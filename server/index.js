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

async function startServer() {
    if (usePg) {
        try {
            console.log('🏗️  Starting Database Maintenance...')

            // Check if users table exists and its ID type
            const tableCheck = await pgPool.query("SELECT data_type FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'id'")

            if (tableCheck.rows.length > 0 && tableCheck.rows[0].data_type === 'uuid') {
                console.log('⚠️  Existing users table has strict UUID type. Recreating for flexibility...')
                await pgPool.query('DROP TABLE IF EXISTS users CASCADE')
            }

            // 1. Create Users Table with TEXT ID
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

            // 2. Create Members Table
            await pgPool.query(`CREATE TABLE IF NOT EXISTS members (
                id TEXT PRIMARY KEY,
                owner_id TEXT NOT NULL,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                role TEXT NOT NULL,
                status TEXT DEFAULT 'ACTIVE',
                created_at TIMESTAMPTZ DEFAULT NOW()
            )`)

            // 3. Seed Admin Account
            const adminEmail = 'admin@fastpay.com'
            const adminPass = 'SwiftPay#Admin#2024'
            const hash = await bcrypt.hash(adminPass, 10)

            const { rows } = await pgPool.query('SELECT id FROM users WHERE email = $1', [adminEmail])

            if (rows.length === 0) {
                await pgPool.query(
                    'INSERT INTO users(id, email, password_hash, business_name, sp_public_key, sp_secret_key) VALUES($1, $2, $3, $4, $5, $6)',
                    ['ADMIN_01', adminEmail, hash, 'Click Store', process.env.SWIFTPAY_PUBLIC_KEY, process.env.SWIFTPAY_SECRET_KEY]
                )
                console.log('👤 Admin Created: admin@fastpay.com')
            } else {
                await pgPool.query('UPDATE users SET password_hash = $1 WHERE email = $2', [hash, adminEmail])
                console.log('👤 Admin Password Resynced')
            }
            console.log('✅ Database is Healthy and Ready')
        } catch (e) {
            console.error('❌ DB ERROR:', e.message)
        }
    }

    const PORT = process.env.PORT || 3000
    app.listen(PORT, '0.0.0.0', () => console.log('🚀 Merchant Portal Live on', PORT))
}

// --- Status Endpoint ---
app.get('/api/status', (req, res) => res.json({ database: usePg ? 'connected' : 'disconnected' }))

// --- Auth Middleware ---
const requireAuth = (req, res, next) => {
    const authHeader = req.headers.authorization
    if (!authHeader?.startsWith('Bearer ')) return res.status(401).json({ error: 'Unauthorized' })
    try {
        req.user = jwt.verify(authHeader.split(' ')[1], JWT_SECRET)
        next()
    } catch (e) { res.status(401).json({ error: 'Session Expired' }) }
}

// --- Auth Endpoints ---
app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body
    if (!usePg) return res.status(500).json({ error: 'DB not connected' })
    try {
        const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase().trim()])
        const user = r.rows[0]
        if (!user || !(await bcrypt.compare(password, user.password_hash))) {
            return res.status(401).json({ error: 'Invalid credentials' })
        }
        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name }, JWT_SECRET, { expiresIn: '24h' })
        res.json({ token, user: { id: user.id, email: user.email, businessName: user.business_name || 'Merchant', hasKeys: !!user.sp_secret_key } })
    } catch (e) { res.status(500).json({ error: 'Server Error' }) }
})

app.post('/api/auth/signup', async (req, res) => {
    const { email, password, businessName } = req.body
    try {
        const hash = await bcrypt.hash(password, 10)
        await pgPool.query(
            'INSERT INTO users(id, email, password_hash, business_name) VALUES($1, $2, $3, $4)',
            ['USER_' + Date.now(), email.toLowerCase().trim(), hash, businessName]
        )
        res.json({ status: 'success' })
    } catch (e) { res.status(400).json({ error: e.message }) }
})

// --- SwiftPay Proxy Endpoints ---

const getSwiftPayClient = async (userId) => {
    const r = await pgPool.query('SELECT sp_secret_key FROM users WHERE id = $1', [userId])
    const secret = r.rows[0]?.sp_secret_key
    if (!secret) throw new Error('API Keys Missing')
    return axios.create({
        baseURL: 'https://api.netbank.ph/',
        headers: { 'Authorization': 'Basic ' + Buffer.from(secret.trim() + ':').toString('base64') }
    })
}

app.get('/api/swiftpay/balance', requireAuth, async (req, res) => {
    try {
        const client = await getSwiftPayClient(req.user.id)
        const resp = await client.get('v1/account/balance')
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: e.message }) }
})

app.get('/api/swiftpay/transactions', requireAuth, async (req, res) => {
    try {
        const client = await getSwiftPayClient(req.user.id)
        const resp = await client.get('v1/collect/payments')
        res.json((resp.data.data || []).map(t => ({ id: t.id, amount: parseFloat(t.amount), status: t.status, date: t.createdAt })))
    } catch (e) { res.json([]) }
})

app.post('/api/swiftpay/paylinks', requireAuth, async (req, res) => {
    try {
        const client = await getSwiftPayClient(req.user.id)
        const resp = await client.post('v1/collect/payment-links', req.body)
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: e.message }) }
})

app.get('/api/swiftpay/paylinks', requireAuth, async (req, res) => {
    try {
        const client = await getSwiftPayClient(req.user.id)
        const resp = await client.get('v1/collect/payment-links')
        res.json(resp.data.data || [])
    } catch (e) { res.json([]) }
})

app.post('/api/swiftpay/qr', requireAuth, async (req, res) => {
    try {
        const client = await getSwiftPayClient(req.user.id)
        const resp = await client.post('v1/collect/qr/payments', req.body)
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: e.message }) }
})

// --- Merchant Settings ---
app.get('/api/swiftpay/settings', requireAuth, async (req, res) => {
    const r = await pgPool.query('SELECT business_name, shop_url, sp_public_key FROM users WHERE id = $1', [req.user.id])
    res.json(r.rows[0])
})

app.post('/api/swiftpay/profile', requireAuth, async (req, res) => {
    const { businessName, shopUrl } = req.body
    await pgPool.query('UPDATE users SET business_name = $1, shop_url = $2 WHERE id = $3', [businessName, shopUrl, req.user.id])
    res.json({ status: 'success' })
})

app.post('/api/swiftpay/keys', requireAuth, async (req, res) => {
    const { publicKey, secretKey } = req.body
    await pgPool.query('UPDATE users SET sp_public_key = $1, sp_secret_key = $2 WHERE id = $3', [publicKey, secretKey, req.user.id])
    res.json({ status: 'success' })
})

app.get('/api/swiftpay/members', requireAuth, async (req, res) => {
    const r = await pgPool.query('SELECT * FROM members WHERE owner_id = $1', [req.user.id])
    res.json(r.rows)
})

app.post('/api/swiftpay/members', requireAuth, async (req, res) => {
    const { name, email, role } = req.body
    await pgPool.query('INSERT INTO members(id, owner_id, name, email, role) VALUES($1, $2, $3, $4, $5)', [Date.now().toString(), req.user.id, name, email, role])
    res.json({ status: 'success' })
})

app.delete('/api/swiftpay/members/:id', requireAuth, async (req, res) => {
    await pgPool.query('DELETE FROM members WHERE id = $1 AND owner_id = $2', [req.params.id, req.user.id])
    res.json({ status: 'success' })
})

app.post('/api/swiftpay/webhook', async (req, res) => {
    console.log('🔔 Webhook:', req.body)
    res.status(200).send('OK')
})

app.use(express.static(path.join(__dirname, 'public')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

startServer()
