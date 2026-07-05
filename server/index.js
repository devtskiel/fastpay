require('dotenv').config()
const express = require('express')
const bodyParser = require('body-parser')
const cors = require('cors')
const helmet = require('helmet')
const fs = require('fs')
const path = require('path')
const axios = require('axios')
const bcrypt = require('bcryptjs')
const jwt = require('jsonwebtoken')

const app = express()
app.use(helmet({ contentSecurityPolicy: false }))
app.use(cors())
app.use(bodyParser.json())

const JWT_SECRET = process.env.JWT_SECRET || 'fastpay-super-secret-2024'

// --- Persistence Layer ---
const usePg = !!process.env.DATABASE_URL
let pgPool = null

if (usePg) {
  const { Pool } = require('pg')
  pgPool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: { rejectUnauthorized: false }
  })

  ;(async () => {
    try {
      await pgPool.query(`CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        email TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        business_name TEXT,
        sp_public_key TEXT,
        sp_secret_key TEXT,
        created_at TIMESTAMPTZ DEFAULT NOW()
      )`)
      await pgPool.query(`CREATE TABLE IF NOT EXISTS members (
        id TEXT PRIMARY KEY,
        owner_id TEXT NOT NULL,
        name TEXT NOT NULL,
        email TEXT NOT NULL,
        role TEXT NOT NULL,
        status TEXT DEFAULT 'ACTIVE',
        created_at TIMESTAMPTZ DEFAULT NOW()
      )`)
      console.log('✅ Postgres Multi-User Schema Ready')

      // Admin Seeding
      const adminEmail = 'admin@fastpay.com'
      const adminPass = 'SwiftPay#Admin#2024'
      const adminCheck = await pgPool.query('SELECT id FROM users WHERE email = $1', [adminEmail])
      if (adminCheck.rows.length === 0) {
        const hash = await bcrypt.hash(adminPass, 10)
        await pgPool.query(
          'INSERT INTO users(email, password_hash, business_name, sp_public_key, sp_secret_key) VALUES($1, $2, $3, $4, $5)',
          [adminEmail, hash, 'FastPay HQ', process.env.SWIFTPAY_PUBLIC_KEY, process.env.SWIFTPAY_SECRET_KEY]
        )
        console.log('👤 Main Admin Seeded')
      }
    } catch (e) { console.error('❌ DB Error:', e.message) }
  })()
}

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
app.post('/api/auth/signup', async (req, res) => {
    if (!usePg) return res.status(500).json({ error: 'Database not connected' })
    const { email, password, businessName } = req.body
    try {
        const hash = await bcrypt.hash(password, 10)
        await pgPool.query('INSERT INTO users(email, password_hash, business_name) VALUES($1, $2, $3)', [email.toLowerCase(), hash, businessName])
        res.json({ status: 'success' })
    } catch (e) {
        if (e.code === '23505') return res.status(400).json({ error: 'Email already registered' })
        res.status(400).json({ error: 'Account creation failed' })
    }
})

app.post('/api/auth/login', async (req, res) => {
    if (!usePg) return res.status(500).json({ error: 'Database not connected' })
    const { email, password } = req.body
    try {
        const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase()])
        const user = r.rows[0]
        if (!user || !(await bcrypt.compare(password, user.password_hash))) return res.status(401).json({ error: 'Invalid email or password' })

        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name }, JWT_SECRET, { expiresIn: '24h' })
        res.json({ token, user: { email: user.email, businessName: user.business_name, hasKeys: !!user.sp_secret_key } })
    } catch (e) { res.status(500).json({ error: 'Authentication failed' }) }
})

// --- SwiftPay Proxy ---
const getClient = async (uid) => {
    const r = await pgPool.query('SELECT sp_secret_key FROM users WHERE id = $1', [uid])
    const secret = r.rows[0]?.sp_secret_key
    if (!secret) throw new Error('API Keys Missing')
    return axios.create({
        baseURL: 'https://api.netbank.ph/',
        headers: { 'Authorization': 'Basic ' + Buffer.from(secret + ':').toString('base64') }
    })
}

app.get('/api/swiftpay/balance', requireAuth, async (req, res) => {
    try {
        const client = await getClient(req.user.id)
        const resp = await client.get('v1/account/balance')
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: 'API Error' }) }
})

app.get('/api/swiftpay/transactions', requireAuth, async (req, res) => {
    try {
        const client = await getClient(req.user.id)
        const resp = await client.get('v1/collect/payments')
        const data = resp.data.data || resp.data.payments || []
        res.json(data.map(t => ({
            id: t.id,
            amount: parseFloat(t.amount),
            status: (t.status || 'PENDING').toUpperCase(),
            date: t.createdAt || new Date().toISOString(),
            method: t.paymentMethod || 'Wallet'
        })))
    } catch (e) { res.json([]) }
})

app.post('/api/swiftpay/payment-links', requireAuth, async (req, res) => {
    try {
        const client = await getClient(req.user.id)
        const resp = await client.post('v1/collect/payment-links', req.body)
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: 'Failed to create link' }) }
})

app.post('/api/swiftpay/qr', requireAuth, async (req, res) => {
    try {
        const client = await getClient(req.user.id)
        const resp = await client.post('v1/collect/qr/payments', req.body)
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: 'Failed to generate QR' }) }
})

app.post('/api/swiftpay/invoice', requireAuth, async (req, res) => {
    try {
        const client = await getClient(req.user.id)
        const resp = await client.post('v1/collect/invoice', req.body)
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: 'Failed to create invoice' }) }
})

app.post('/api/swiftpay/keys', requireAuth, async (req, res) => {
    const { publicKey, secretKey } = req.body
    await pgPool.query('UPDATE users SET sp_public_key = $1, sp_secret_key = $2 WHERE id = $3', [publicKey, secretKey, req.user.id])
    res.json({ status: 'success' })
})

app.get('/api/swiftpay/settings', requireAuth, async (req, res) => {
    const r = await pgPool.query('SELECT sp_public_key, business_name FROM users WHERE id = $1', [req.user.id])
    res.json(r.rows[0])
})

app.get('/api/swiftpay/members', requireAuth, async (req, res) => {
    const r = await pgPool.query('SELECT * FROM members WHERE owner_id = $1 ORDER BY created_at DESC', [req.user.id])
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

// Static Server
app.use(express.static(path.join(__dirname, 'public')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

const PORT = process.env.PORT || 3000
app.listen(PORT, '0.0.0.0', () => console.log('🚀 SwiftPay Production Server live on', PORT))
