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

// --- Auth Endpoints ---
app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body
    if (!usePg) return res.status(500).json({ error: 'DB not connected' })
    try {
        console.log(`🔑 Login attempt: ${email}`)
        const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase().trim()])
        const user = r.rows[0]

        if (!user) {
            console.log('❌ User not found')
            return res.status(401).json({ error: 'Account not found. Please Sign Up.' })
        }

        const valid = await bcrypt.compare(password, user.password_hash)
        if (!valid) {
            console.log('❌ Incorrect password')
            return res.status(401).json({ error: 'Incorrect password' })
        }

        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name }, JWT_SECRET, { expiresIn: '24h' })
        console.log('✅ Login successful')
        res.json({ token, user: { email: user.email, businessName: user.business_name || 'Merchant', hasKeys: !!user.sp_secret_key } })
    } catch (e) {
        console.error('🔥 Login Error:', e.message)
        res.status(500).json({ error: 'Server Error: ' + e.message })
    }
})

app.post('/api/auth/signup', async (req, res) => {
    const { email, password, businessName } = req.body
    try {
        console.log(`📝 Signup attempt: ${email}`)
        const hash = await bcrypt.hash(password, 10)
        const id = 'USER_' + Date.now()
        await pgPool.query(
            'INSERT INTO users(id, email, password_hash, business_name) VALUES($1, $2, $3, $4)',
            [id, email.toLowerCase().trim(), hash, businessName]
        )
        console.log('✅ Signup successful')
        res.json({ status: 'success' })
    } catch (e) {
        console.error('🔥 Signup Error:', e.message)
        res.status(400).json({ error: e.message })
    }
})

// --- SwiftPay Proxy ---
const requireAuth = (req, res, next) => {
    const authHeader = req.headers.authorization
    if (!authHeader?.startsWith('Bearer ')) return res.status(401).json({ error: 'Unauthorized' })
    try {
        req.user = jwt.verify(authHeader.split(' ')[1], JWT_SECRET)
        next()
    } catch (e) { res.status(401).json({ error: 'Session Expired' }) }
}

app.get('/api/swiftpay/balance', requireAuth, async (req, res) => {
    try {
        const r = await pgPool.query('SELECT sp_secret_key FROM users WHERE id = $1', [req.user.id])
        const secret = r.rows[0]?.sp_secret_key
        if (!secret) return res.status(400).json({ error: 'API Keys Missing' })
        const auth = { 'Authorization': 'Basic ' + Buffer.from(secret.trim() + ':').toString('base64') }
        const resp = await axios.get('https://api.netbank.ph/v1/account/balance', { headers: auth })
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: 'SwiftPay API Error' }) }
})

app.get('/api/swiftpay/transactions', requireAuth, async (req, res) => {
    try {
        const r = await pgPool.query('SELECT sp_secret_key FROM users WHERE id = $1', [req.user.id])
        const secret = r.rows[0]?.sp_secret_key
        if (!secret) return res.json([])
        const auth = { 'Authorization': 'Basic ' + Buffer.from(secret.trim() + ':').toString('base64') }
        const resp = await axios.get('https://api.netbank.ph/v1/collect/payments', { headers: auth })
        res.json((resp.data.data || []).map(t => ({ id: t.id, amount: parseFloat(t.amount), status: t.status, date: t.createdAt })))
    } catch (e) { res.json([]) }
})

// --- Webhook Handler ---
app.post('/api/swiftpay/webhook', async (req, res) => {
    console.log('🔔 Received SwiftPay Webhook:', JSON.stringify(req.body, null, 2))
    // Add logic here to process payment status changes (e.g., notify Android app via FCM or update DB)
    res.status(200).json({ status: 'received' })
})

app.use(express.static(path.join(__dirname, 'public')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

startServer()
