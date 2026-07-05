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

const JWT_SECRET = process.env.JWT_SECRET || 'fastpay-super-secret'

// --- Persistence Layer ---
const usePg = !!process.env.DATABASE_URL
let pgPool = null

if (usePg) {
  const { Pool } = require('pg')
  pgPool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: { rejectUnauthorized: false }
  })

  // Auto-migrate tables for Production
  ;(async () => {
    try {
      await pgPool.query(`CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        email TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
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
      await pgPool.query(`CREATE TABLE IF NOT EXISTS approvals (
        request_id TEXT PRIMARY KEY,
        email TEXT NOT NULL,
        device_id TEXT,
        device_name TEXT,
        status TEXT NOT NULL,
        created_at BIGINT
      )`)
      console.log('✅ Postgres Production Tables Ready with Multi-User Support')

      // --- Admin Seeding ---
      const adminEmail = 'admin@fastpay.com'
      const adminPass = 'SwiftPay#Admin#2024'
      const adminCheck = await pgPool.query('SELECT id FROM users WHERE email = $1', [adminEmail])

      if (adminCheck.rows.length === 0) {
        const hash = await bcrypt.hash(adminPass, 10)
        await pgPool.query(
          'INSERT INTO users(email, password_hash, sp_public_key, sp_secret_key) VALUES($1, $2, $3, $4)',
          [adminEmail, hash, process.env.SWIFTPAY_PUBLIC_KEY, process.env.SWIFTPAY_SECRET_KEY]
        )
        console.log('👤 Main Admin Account Seeded Successfully')
      }
    } catch (e) {
      console.error('❌ DB Migration Failed:', e.message)
    }
  })()
}

// --- Auth Middleware ---
const requireAuth = (req, res, next) => {
    const authHeader = req.headers.authorization
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'Unauthorized. Please login.' })
    }
    const token = authHeader.split(' ')[1]
    try {
        const decoded = jwt.verify(token, JWT_SECRET)
        req.user = decoded
        next()
    } catch (e) {
        res.status(401).json({ error: 'Invalid or expired token.' })
    }
}

// --- Auth Routes ---
app.post('/api/auth/signup', async (req, res) => {
    const { email, password } = req.body
    if (!email || !password) return res.status(400).json({ error: 'Email and password required' })

    try {
        const hash = await bcrypt.hash(password, 10)
        if (usePg) {
            await pgPool.query('INSERT INTO users(email, password_hash) VALUES($1, $2)', [email.toLowerCase(), hash])
        } else {
            return res.status(500).json({ error: 'Signup requires DATABASE_URL configured' })
        }
        res.json({ status: 'success', message: 'User created' })
    } catch (e) {
        if (e.code === '23505') return res.status(400).json({ error: 'Email already exists' })
        res.status(500).json({ error: e.message })
    }
})

app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body
    try {
        let user = null
        if (usePg) {
            const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase()])
            user = r.rows[0]
        }
        if (!user || !(await bcrypt.compare(password, user.password_hash))) {
            return res.status(401).json({ error: 'Invalid email or password' })
        }
        const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '24h' })
        res.json({ token, user: { email: user.email, id: user.id, hasKeys: !!user.sp_secret_key } })
    } catch (e) { res.status(500).json({ error: e.message }) }
})

// --- SwiftPay Proxy Endpoints ---

const getSwiftPayClient = async (userId) => {
    const user = await pgPool.query('SELECT sp_secret_key FROM users WHERE id = $1', [userId])
    const secret = user.rows[0]?.sp_secret_key
    if (!secret) throw new Error('Keys not configured')
    return axios.create({
        baseURL: 'https://api.netbank.ph/',
        headers: { 'Authorization': 'Basic ' + Buffer.from(secret + ':').toString('base64') }
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
        const data = resp.data.data || resp.data.payments || []
        res.json(data.map(t => ({
            transactionId: t.id,
            amount: parseFloat(t.amount),
            status: (t.status || 'PENDING').toUpperCase(),
            date: t.createdAt || new Date().toISOString()
        })))
    } catch (e) { res.json([]) }
})

app.post('/api/swiftpay/payment-links', requireAuth, async (req, res) => {
    try {
        const client = await getSwiftPayClient(req.user.id)
        const resp = await client.post('v1/collect/payment-links', req.body)
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: e.message }) }
})

app.post('/api/swiftpay/generate_dynamic_qr', requireAuth, async (req, res) => {
    try {
        const client = await getSwiftPayClient(req.user.id)
        const resp = await client.post('v1/collect/qr/payments', req.body)
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: e.message }) }
})

app.post('/api/swiftpay/create_invoice', requireAuth, async (req, res) => {
    try {
        const client = await getSwiftPayClient(req.user.id)
        const resp = await client.post('v1/collect/invoice', req.body)
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: e.message }) }
})

app.post('/api/swiftpay/keys', requireAuth, async (req, res) => {
    const { publicKey, secretKey } = req.body
    try {
        await pgPool.query('UPDATE users SET sp_public_key = $1, sp_secret_key = $2 WHERE id = $3', [publicKey, secretKey, req.user.id])
        res.json({ status: 'success' })
    } catch (e) { res.status(500).json({ error: e.message }) }
})

app.get('/api/swiftpay/settings', requireAuth, async (req, res) => {
    const user = await pgPool.query('SELECT sp_public_key, sp_secret_key FROM users WHERE id = $1', [req.user.id])
    res.json({
        publicKey: user.rows[0]?.sp_public_key || '',
        hasSecret: !!user.rows[0]?.sp_secret_key
    })
})

app.get('/api/swiftpay/members', requireAuth, async (req, res) => {
    const r = await pgPool.query('SELECT * FROM members WHERE owner_id = $1 ORDER BY created_at DESC', [req.user.id])
    res.json(r.rows)
})

app.post('/api/swiftpay/members', requireAuth, async (req, res) => {
    const { name, email, role } = req.body
    await pgPool.query(
      'INSERT INTO members(id, owner_id, name, email, role) VALUES($1, $2, $3, $4, $5)',
      [Date.now().toString(), req.user.id, name, email, role]
    )
    res.json({ status: 'success' })
})

app.delete('/api/swiftpay/members/:id', requireAuth, async (req, res) => {
    await pgPool.query('DELETE FROM members WHERE id = $1 AND owner_id = $2', [req.params.id, req.user.id])
    res.json({ status: 'success' })
})

// --- UI & Static ---
app.use(express.static(path.join(__dirname, 'public')))
app.get('/', (req, res) => res.redirect('/dashboard'))
app.get('/dashboard', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

const PORT = process.env.PORT || 3000
app.listen(PORT, '0.0.0.0', () => console.log('🚀 SwiftPay Multi-User Server listening on port', PORT))
