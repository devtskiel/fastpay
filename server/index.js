require('dotenv').config()
const express = require('express')
const bodyParser = require('body-parser')
const cors = require('cors')
const helmet = require('helmet')
const axios = require('axios')
const bcrypt = require('bcryptjs')
const jwt = require('jsonwebtoken')
const path = require('path')

const app = express()
app.use(helmet({ contentSecurityPolicy: false }))
app.use(cors())
app.use(bodyParser.json())

const JWT_SECRET = process.env.JWT_SECRET || 'fastpay-enterprise-core-2024'

// --- Database Layer ---
const rawDbUrl = process.env.DATABASE_URL
const { Pool } = require('pg')
const pgPool = new Pool({
    connectionString: rawDbUrl ? rawDbUrl.replace('postgresql://', 'postgres://') : undefined,
    ssl: { rejectUnauthorized: false }
})

const cleanKey = (k) => k ? k.split('/')[0].split(' ')[0].trim() : ''

async function startServer() {
    try {
        await pgPool.query(`CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            business_name TEXT,
            shop_url TEXT,
            source_account_number TEXT,
            sp_public_key TEXT,
            sp_secret_key TEXT,
            created_at TIMESTAMPTZ DEFAULT NOW()
        )`)

        const adminEmail = 'admin@fastpay.com'
        const hash = await bcrypt.hash('SwiftPay#Admin#2024', 10)
        const { rows } = await pgPool.query('SELECT id FROM users WHERE email = $1', [adminEmail])

        if (rows.length === 0) {
            await pgPool.query(
                'INSERT INTO users(id, email, password_hash, business_name, sp_public_key, sp_secret_key) VALUES($1, $2, $3, $4, $5, $6)',
                ['ADMIN_01', adminEmail, hash, 'SwiftPay Store', cleanKey(process.env.SWIFTPAY_PUBLIC_KEY), cleanKey(process.env.SWIFTPAY_SECRET_KEY)]
            )
        }
    } catch (e) { console.error('❌ DB ERROR:', e.message) }

    app.listen(process.env.PORT || 3000, '0.0.0.0', () => console.log('🚀 Gateway Live'))
}

const getClient = async (uid) => {
    const r = await pgPool.query('SELECT sp_public_key, sp_secret_key, business_name FROM users WHERE id = $1', [uid])
    const u = r.rows[0]
    if (!u?.sp_secret_key) throw new Error('API keys missing')
    return { pub: cleanKey(u.sp_public_key), sec: cleanKey(u.sp_secret_key), biz: u.business_name }
}

const requireAuth = (req, res, next) => {
    const h = req.headers.authorization
    if (!h?.startsWith('Bearer ')) return res.status(401).json({ error: 'Unauthorized' })
    try {
        req.user = jwt.verify(h.split(' ')[1], JWT_SECRET)
        next()
    } catch (e) { res.status(401).json({ error: 'Expired' }) }
}

// --- API Routes ---

app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body
    try {
        const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase().trim()])
        const user = r.rows[0]
        if (!user || !(await bcrypt.compare(password, user.password_hash))) return res.status(401).json({ error: 'Invalid keys' })
        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name }, JWT_SECRET, { expiresIn: '24h' })
        res.json({ token, user: { businessName: user.business_name || 'Merchant' } })
    } catch (e) { res.status(500).json({ error: 'System Error' }) }
})

app.get('/api/swiftpay/balance', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        const auth = Buffer.from(`${u.pub}:${u.sec}`).toString('base64')
        const resp = await axios.get('https://api.netbank.ph/v1/account/balance', { headers: { 'Authorization': `Basic ${auth}` } })
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: 'Balance Error' }) }
})

app.get('/api/swiftpay/transactions', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        const auth = Buffer.from(`${u.pub}:${u.sec}`).toString('base64')
        const resp = await axios.get('https://api.netbank.ph/v1/collect/payments', { headers: { 'Authorization': `Basic ${auth}` } })
        res.json((resp.data.data || []).map(t => ({ id: t.id, amount: parseFloat(t.amount), status: t.status, date: t.createdAt })))
    } catch (e) { res.json([]) }
})

app.get('/api/swiftpay/banks', requireAuth, async (req, res) => {
    try {
        const resp = await axios.get('https://api.pay.live.swiftpay.ph/api/institutions')
        res.json(resp.data)
    } catch (e) { res.json([]) }
})

app.post('/api/swiftpay/disburse', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        const auth = Buffer.from(`${u.pub}:${u.sec}`).toString('base64')
        const { amount, accountNumber, firstName, lastName, institutionCode } = req.body
        const payload = {
            merchantReferenceNo: 'P' + Date.now(),
            channel: 'INSTAPAY',
            institutionCode,
            creditInformation: { amount: parseFloat(amount).toFixed(2), remarks: 'Payout' },
            recipientInformation: { accountNumber, firstName, lastName }
        }
        await axios.post('https://api.pay.live.swiftpay.ph/api/disbursements/send', payload, { headers: { 'Authorization': `Basic ${auth}` } })
        res.json({ status: 'success' })
    } catch (e) { res.status(500).json({ error: 'Payout Failed' }) }
})

app.get('/api/swiftpay/disbursements', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        const auth = Buffer.from(`${u.pub}:${u.sec}`).toString('base64')
        const resp = await axios.get('https://api.pay.live.swiftpay.ph/api/disbursements', { headers: { 'Authorization': `Basic ${auth}` } })
        res.json(resp.data)
    } catch (e) { res.json([]) }
})

app.get('/api/swiftpay/settings', requireAuth, async (req, res) => {
    const r = await pgPool.query('SELECT business_name, shop_url, source_account_number FROM users WHERE id = $1', [req.user.id])
    res.json(r.rows[0])
})

app.post('/api/swiftpay/profile', requireAuth, async (req, res) => {
    const { businessName, shopUrl, sourceAccountNumber } = req.body
    await pgPool.query('UPDATE users SET business_name = $1, shop_url = $2, source_account_number = $3 WHERE id = $4', [businessName, shopUrl, sourceAccountNumber, req.user.id])
    res.json({ status: 'success' })
})

app.use(express.static(path.join(__dirname, 'public')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

startServer()
