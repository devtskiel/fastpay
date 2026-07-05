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
const rawDbUrl = process.env.DATABASE_URL
const usePg = !!rawDbUrl
let pgPool = null

if (usePg) {
  const { Pool } = require('pg')
  const connectionString = rawDbUrl.replace('postgresql://', 'postgres://')

  pgPool = new Pool({
    connectionString: connectionString,
    ssl: { rejectUnauthorized: false }
  })

  const initDb = async () => {
    try {
      await pgPool.query(`CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        email TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        business_name TEXT,
        sp_public_key TEXT,
        sp_secret_key TEXT,
        created_at TIMESTAMPTZ DEFAULT NOW()
      )`)

      // Migrations
      await pgPool.query(`ALTER TABLE users ADD COLUMN IF NOT EXISTS business_name TEXT`)
      await pgPool.query(`ALTER TABLE users ADD COLUMN IF NOT EXISTS sp_public_key TEXT`)
      await pgPool.query(`ALTER TABLE users ADD COLUMN IF NOT EXISTS sp_secret_key TEXT`)

      await pgPool.query(`CREATE TABLE IF NOT EXISTS members (
        id TEXT PRIMARY KEY,
        owner_id TEXT NOT NULL,
        name TEXT NOT NULL,
        email TEXT NOT NULL,
        role TEXT NOT NULL,
        status TEXT DEFAULT 'ACTIVE',
        created_at TIMESTAMPTZ DEFAULT NOW()
      )`)

      // Seed/Reset Admin
      const adminEmail = 'admin@fastpay.com'
      const adminPass = 'SwiftPay#Admin#2024'
      const hash = await bcrypt.hash(adminPass, 10)

      const { rows } = await pgPool.query('SELECT id FROM users WHERE email = $1', [adminEmail])

      if (rows.length === 0) {
        await pgPool.query(
          'INSERT INTO users(id, email, password_hash, business_name, sp_public_key, sp_secret_key) VALUES($1, $2, $3, $4, $5, $6)',
          ['ADMIN_ROOT', adminEmail, hash, 'Click Store', process.env.SWIFTPAY_PUBLIC_KEY, process.env.SWIFTPAY_SECRET_KEY]
        )
      } else {
        // Force update password to ensure it matches the provided one
        await pgPool.query('UPDATE users SET password_hash = $1 WHERE email = $2', [hash, adminEmail])
      }
      console.log('✅ System Database Ready & Admin Synced')
    } catch (e) {
      console.error('❌ DB SYNC ERROR:', e.message)
    }
  }
  initDb()
}

app.get('/api/status', (req, res) => res.json({ database: usePg ? 'connected' : 'disconnected', time: Date.now() }))

app.post('/api/auth/login', async (req, res) => {
    if (!usePg) return res.status(500).json({ error: 'DB not connected' })
    const { email, password } = req.body
    try {
        const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase().trim()])
        const user = r.rows[0]

        if (!user) return res.status(401).json({ error: 'Account not found. Please Sign Up.' })

        const valid = await bcrypt.compare(password, user.password_hash)
        if (!valid) return res.status(401).json({ error: 'Incorrect password' })

        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name }, JWT_SECRET, { expiresIn: '24h' })
        res.json({ token, user: { email: user.email, businessName: user.business_name || 'Merchant', hasKeys: !!user.sp_secret_key } })
    } catch (e) {
        console.error('Login Error:', e.message)
        res.status(500).json({ error: 'Internal Server Error' })
    }
})

app.post('/api/auth/signup', async (req, res) => {
    if (!usePg) return res.status(500).json({ error: 'DB not connected' })
    const { email, password, businessName } = req.body
    try {
        const hash = await bcrypt.hash(password, 10)
        await pgPool.query(
            'INSERT INTO users(id, email, password_hash, business_name) VALUES($1, $2, $3, $4)',
            ['USER_' + Date.now(), email.toLowerCase().trim(), hash, businessName]
        )
        res.json({ status: 'success' })
    } catch (e) {
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

const getClient = async (uid) => {
    const r = await pgPool.query('SELECT sp_secret_key FROM users WHERE id = $1', [uid])
    const secret = r.rows[0]?.sp_secret_key
    if (!secret) throw new Error('API Keys Missing')
    return axios.create({ baseURL: 'https://api.netbank.ph/', headers: { 'Authorization': 'Basic ' + Buffer.from(secret.trim() + ':').toString('base64') } })
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
        const data = resp.data.data || []
        res.json(data.map(t => ({ id: t.id, amount: parseFloat(t.amount), status: t.status, date: t.createdAt, method: 'SwiftPay' })))
    } catch (e) { res.json([]) }
})

app.post('/api/swiftpay/keys', requireAuth, async (req, res) => {
    await pgPool.query('UPDATE users SET sp_public_key = $1, sp_secret_key = $2 WHERE id = $3', [req.body.publicKey, req.body.secretKey, req.user.id])
    res.json({ status: 'success' })
})

app.get('/api/swiftpay/settings', requireAuth, async (req, res) => {
    const r = await pgPool.query('SELECT sp_public_key, business_name FROM users WHERE id = $1', [req.user.id])
    res.json(r.rows[0])
})

app.get('/api/swiftpay/members', requireAuth, async (req, res) => {
    const r = await pgPool.query('SELECT * FROM members WHERE owner_id = $1', [req.user.id])
    res.json(r.rows)
})

app.post('/api/swiftpay/members', requireAuth, async (req, res) => {
    await pgPool.query('INSERT INTO members(id, owner_id, name, email, role) VALUES($1, $2, $3, $4, $5)', [Date.now().toString(), req.user.id, req.body.name, req.body.email, req.body.role])
    res.json({ status: 'success' })
})

app.use(express.static(path.join(__dirname, 'public')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

const PORT = process.env.PORT || 3000
app.listen(PORT, '0.0.0.0', () => console.log('🚀 Server live on', PORT))
