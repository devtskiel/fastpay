require('dotenv').config()
const express = require('express')
const bodyParser = require('body-parser')
const cors = require('cors')
const helmet = require('helmet')
const axios = require('axios')
const bcrypt = require('bcryptjs')
const jwt = require('jsonwebtoken')
const path = require('path')
const qs = require('qs')

const app = express()
app.use(helmet({ contentSecurityPolicy: false }))
app.use(cors())
app.use(bodyParser.json())

const JWT_SECRET = process.env.JWT_SECRET || 'fastpay-super-secret-2024'

// --- Persistence Layer ---
const rawDbUrl = process.env.DATABASE_URL
const usePg = !!rawDbUrl
let pgPool = null

if (usePg) {
  const { Pool } = require('pg')
  const connectionString = rawDbUrl.replace('postgresql://', 'postgres://')
  pgPool = new Pool({ connectionString, ssl: { rejectUnauthorized: false } })
}

const cleanKey = (key) => {
    if (!key) return ''
    return key.split('/')[0].split(' ')[0].trim()
}

async function startServer() {
    if (usePg) {
        try {
            await pgPool.query(`CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                business_name TEXT,
                shop_url TEXT,
                sp_public_key TEXT,
                sp_secret_key TEXT,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )`)

            const migrations = [
                'ALTER TABLE users ADD COLUMN IF NOT EXISTS business_name TEXT',
                'ALTER TABLE users ADD COLUMN IF NOT EXISTS shop_url TEXT',
                'ALTER TABLE users ADD COLUMN IF NOT EXISTS sp_public_key TEXT',
                'ALTER TABLE users ADD COLUMN IF NOT EXISTS sp_secret_key TEXT'
            ]
            for (const sql of migrations) await pgPool.query(sql)

            await pgPool.query(`CREATE TABLE IF NOT EXISTS members (
                id TEXT PRIMARY KEY,
                owner_id TEXT NOT NULL,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                role TEXT NOT NULL,
                status TEXT DEFAULT 'ACTIVE',
                created_at TIMESTAMPTZ DEFAULT NOW()
            )`)

            const adminEmail = 'admin@fastpay.com'
            const adminPass = 'SwiftPay#Admin#2024'
            const hash = await bcrypt.hash(adminPass, 10)
            const { rows } = await pgPool.query('SELECT id FROM users WHERE email = $1', [adminEmail])

            if (rows.length === 0) {
                await pgPool.query(
                    'INSERT INTO users(id, email, password_hash, business_name, sp_public_key, sp_secret_key) VALUES($1, $2, $3, $4, $5, $6)',
                    ['ADMIN_01', adminEmail, hash, 'Click Store', cleanKey(process.env.SWIFTPAY_PUBLIC_KEY), cleanKey(process.env.SWIFTPAY_SECRET_KEY)]
                )
            } else {
                await pgPool.query('UPDATE users SET password_hash = $1, sp_public_key = $2, sp_secret_key = $3 WHERE email = $4', [hash, cleanKey(process.env.SWIFTPAY_PUBLIC_KEY), cleanKey(process.env.SWIFTPAY_SECRET_KEY), adminEmail])
            }
        } catch (e) { console.error('❌ DB ERROR:', e.message) }
    }
    app.listen(process.env.PORT || 3000, '0.0.0.0', () => console.log('🚀 Server Live'))
}

// --- Netbank Token Helper ---
const getNetbankToken = async (pub, sec) => {
    try {
        const data = qs.stringify({
            'grant_type': 'client_credentials',
            'client_id': pub,
            'client_secret': sec
        })
        const config = {
            method: 'post',
            url: 'https://auth.netbank.ph/oauth2/token',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            data: data
        }
        const response = await axios(config)
        return response.data.access_token
    } catch (e) {
        console.error('🔑 Token Exchange Failed:', e.response?.data || e.message)
        throw new Error('Authentication with Netbank failed')
    }
}

// --- Auth Middleware ---
const requireAuth = (req, res, next) => {
    const authHeader = req.headers.authorization
    if (!authHeader?.startsWith('Bearer ')) return res.status(401).json({ error: 'Unauthorized' })
    try {
        req.user = jwt.verify(authHeader.split(' ')[1], JWT_SECRET)
        next()
    } catch (e) { res.status(401).json({ error: 'Expired session' }) }
}

// --- Auth Endpoints ---
app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body
    try {
        const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase().trim()])
        const user = r.rows[0]
        if (!user || !(await bcrypt.compare(password, user.password_hash))) return res.status(401).json({ error: 'Invalid credentials' })
        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name }, JWT_SECRET, { expiresIn: '24h' })
        res.json({ token, user: { id: user.id, email: user.email, businessName: user.business_name || 'Merchant', hasKeys: !!user.sp_secret_key } })
    } catch (e) { res.status(500).json({ error: 'Auth error' }) }
})

// --- SwiftPay Proxy ---
app.get('/api/swiftpay/balance', requireAuth, async (req, res) => {
    try {
        const r = await pgPool.query('SELECT sp_secret_key FROM users WHERE id = $1', [req.user.id])
        const secret = cleanKey(r.rows[0]?.sp_secret_key)
        const auth = { 'Authorization': 'Basic ' + Buffer.from(secret + ':').toString('base64') }
        const resp = await axios.get('https://api.netbank.ph/v1/account/balance', { headers: auth })
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: e.message }) }
})

app.get('/api/swiftpay/transactions', requireAuth, async (req, res) => {
    try {
        const r = await pgPool.query('SELECT sp_secret_key FROM users WHERE id = $1', [req.user.id])
        const secret = cleanKey(r.rows[0]?.sp_secret_key)
        const auth = { 'Authorization': 'Basic ' + Buffer.from(secret + ':').toString('base64') }
        const resp = await axios.get('https://api.netbank.ph/v1/collect/payments', { headers: auth })
        res.json((resp.data.data || []).map(t => ({ id: t.id, amount: parseFloat(t.amount), status: t.status, date: t.createdAt })))
    } catch (e) { res.json([]) }
})

app.post('/api/swiftpay/qr', requireAuth, async (req, res) => {
    try {
        const r = await pgPool.query('SELECT sp_public_key, sp_secret_key, business_name FROM users WHERE id = $1', [req.user.id])
        const pub = cleanKey(r.rows[0]?.sp_public_key)
        const sec = cleanKey(r.rows[0]?.sp_secret_key)
        const biz = r.rows[0]?.business_name || 'SwiftPay Merchant'

        // 1. Get OAuth Token
        const accessToken = await getNetbankToken(pub, sec)

        // 2. Generate QR
        const payload = {
            amount: { currency: 'PHP', value: req.body.amount.toFixed(2) },
            merchant_name: biz.substring(0, 25),
            merchant_city: 'Manila',
            reference_no: 'FP_' + Date.now(),
            generate_additional_fields: false
        }

        const resp = await axios.post('https://api.netbank.ph/v1/qrph/generate', payload, {
            headers: { 'Authorization': 'Bearer ' + accessToken, 'Content-Type': 'application/json' }
        })
        res.json(resp.data)
    } catch (e) {
        console.error('🔥 QR Error:', e.response?.data || e.message)
        res.status(500).json({ error: e.message || 'QR Generation Failed' })
    }
})

app.get('/api/swiftpay/settings', requireAuth, async (req, res) => {
    const r = await pgPool.query('SELECT business_name, shop_url, sp_public_key FROM users WHERE id = $1', [req.user.id])
    res.json(r.rows[0])
})

app.use(express.static(path.join(__dirname, 'public')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

startServer()
