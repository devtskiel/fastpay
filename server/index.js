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
            // 1. Ensure Table Exists
            await pgPool.query(`CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TIMESTAMPTZ DEFAULT NOW()
            )`)

            // 2. CRITICAL: Force convert ID column to TEXT if it was created as UUID by mistake
            const colCheck = await pgPool.query("SELECT data_type FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'id'")
            if (colCheck.rows.length > 0 && colCheck.rows[0].data_type === 'uuid') {
                console.log('🔄 Converting UUID column to TEXT for compatibility...')
                await pgPool.query('ALTER TABLE users ALTER COLUMN id TYPE TEXT USING id::text')
            }

            // 3. Ensure Migrations
            await pgPool.query(`ALTER TABLE users ADD COLUMN IF NOT EXISTS business_name TEXT`)
            await pgPool.query(`ALTER TABLE users ADD COLUMN IF NOT EXISTS sp_public_key TEXT`)
            await pgPool.query(`ALTER TABLE users ADD COLUMN IF NOT EXISTS sp_secret_key TEXT`)

            // 4. Seed/Reset Admin (Use a valid UUID string just in case conversion fails)
            const adminEmail = 'admin@fastpay.com'
            const adminPass = 'SwiftPay#Admin#2024'
            const hash = await bcrypt.hash(adminPass, 10)
            const { rows } = await pgPool.query('SELECT id FROM users WHERE email = $1', [adminEmail])

            const adminId = '00000000-0000-0000-0000-000000000000'
            if (rows.length === 0) {
                await pgPool.query(
                    'INSERT INTO users(id, email, password_hash, business_name, sp_public_key, sp_secret_key) VALUES($1, $2, $3, $4, $5, $6)',
                    [adminId, adminEmail, hash, 'Click Store', process.env.SWIFTPAY_PUBLIC_KEY, process.env.SWIFTPAY_SECRET_KEY]
                )
            } else {
                await pgPool.query('UPDATE users SET password_hash = $1 WHERE email = $2', [hash, adminEmail])
            }
            console.log('✅ Database Ready - Admin: admin@fastpay.com / SwiftPay#Admin#2024')
        } catch (e) { console.error('❌ DB ERROR:', e.message) }
    }

    const PORT = process.env.PORT || 3000
    app.listen(PORT, '0.0.0.0', () => console.log('🚀 Merchant Portal Live on', PORT))
}

// --- Auth Endpoints ---
app.get('/api/status', (req, res) => res.json({ database: usePg ? 'connected' : 'disconnected' }))

app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body
    if (!usePg) return res.status(500).json({ error: 'DB not connected' })
    try {
        const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase().trim()])
        const user = r.rows[0]
        if (!user) return res.status(401).json({ error: 'Account not found' })
        const valid = await bcrypt.compare(password, user.password_hash)
        if (!valid) return res.status(401).json({ error: 'Incorrect password' })
        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name }, JWT_SECRET, { expiresIn: '24h' })
        res.json({ token, user: { email: user.email, businessName: user.business_name || 'Merchant', hasKeys: !!user.sp_secret_key } })
    } catch (e) { res.status(500).json({ error: 'Server Error' }) }
})

app.post('/api/auth/signup', async (req, res) => {
    const { email, password, businessName } = req.body
    try {
        const hash = await bcrypt.hash(password, 10)
        // Use a clean hex string for ID to avoid UUID syntax errors
        const cleanId = require('crypto').randomBytes(16).toString('hex')
        await pgPool.query(
            'INSERT INTO users(id, email, password_hash, business_name) VALUES($1, $2, $3, $4)',
            [cleanId, email.toLowerCase().trim(), hash, businessName]
        )
        res.json({ status: 'success' })
    } catch (e) { res.status(400).json({ error: e.message }) }
})

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
    } catch (e) { res.status(500).json({ error: 'API Error' }) }
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

app.use(express.static(path.join(__dirname, 'public')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

startServer()
