require('dotenv').config()
const express = require('express')
const bodyParser = require('body-parser')
const cors = require('cors')
const helmet = require('helmet')
const fs = require('fs')
const path = require('path')
const axios = require('axios')

const app = express()
app.use(helmet({ contentSecurityPolicy: false }))
app.use(cors())
app.use(bodyParser.json())

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
      await pgPool.query(`CREATE TABLE IF NOT EXISTS members (
        id TEXT PRIMARY KEY,
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
      console.log('✅ Postgres Production Tables Ready')
    } catch (e) {
      console.error('❌ DB Migration Failed:', e.message)
    }
  })()
}

const MEMBER_FILE = path.join(__dirname, 'members.json')

async function getMembers() {
  if (usePg) {
    const r = await pgPool.query('SELECT * FROM members ORDER BY created_at DESC')
    return r.rows
  }
  try { return JSON.parse(fs.readFileSync(MEMBER_FILE, 'utf8')) } catch (e) { return [] }
}

async function saveMember(m) {
  if (usePg) {
    await pgPool.query(
      'INSERT INTO members(id, name, email, role) VALUES($1, $2, $3, $4)',
      [Date.now().toString(), m.name, m.email, m.role]
    )
    return
  }
  const members = await getMembers()
  members.push({ ...m, id: Date.now().toString() })
  fs.writeFileSync(MEMBER_FILE, JSON.stringify(members, null, 2))
}

// --- SwiftPay Config ---
const API_KEY = process.env.APP_SERVER_KEY || 'dev_key'
const SP_SECRET = process.env.SWIFTPAY_SECRET_KEY
const SP_PUBLIC = process.env.SWIFTPAY_PUBLIC_KEY
const BASE_URL = "https://api.netbank.ph/"

const getAuthHeader = () => ({ 'Authorization': 'Basic ' + Buffer.from(SP_SECRET + ':').toString('base64') })

// Middleware: Production Key Protection
const requireAuth = (req, res, next) => {
    const key = req.headers['x-api-key'] || req.headers['x-app-key']
    if (key === API_KEY) return next()
    res.status(401).json({ error: 'Unauthorized access to merchant gateway.' })
}

// --- Routes ---

app.use(express.static(path.join(__dirname, 'public')))
app.get('/dashboard', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

app.get('/api/swiftpay/balance', requireAuth, async (req, res) => {
    try {
        const resp = await axios.get(`${BASE_URL}v1/account/balance`, { headers: getAuthHeader() })
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: 'SwiftPay API Error' }) }
})

app.get('/api/swiftpay/transactions', requireAuth, async (req, res) => {
    try {
        const resp = await axios.get(`${BASE_URL}v1/collect/payments`, { headers: getAuthHeader() })
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
        const resp = await axios.post(`${BASE_URL}v1/collect/payment-links`, req.body, { headers: getAuthHeader() })
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: 'Failed to create link' }) }
})

app.get('/api/swiftpay/settings', requireAuth, (req, res) => res.json({ publicKey: SP_PUBLIC }))

app.get('/api/swiftpay/members', requireAuth, async (req, res) => res.json(await getMembers()))
app.post('/api/swiftpay/members', requireAuth, async (req, res) => {
    await saveMember(req.body)
    res.json({ status: 'success' })
})

app.get('/api/swiftpay/webhooks', requireAuth, async (req, res) => {
    try {
        const resp = await axios.get(`${BASE_URL}v1/collect/webhooks`, { headers: getAuthHeader() })
        res.json(resp.data)
    } catch (e) { res.json([]) }
})

// Support Mobile App Login Approvals
app.post('/approvals', async (req, res) => {
    const { email, deviceId, deviceName } = req.body
    const entry = { requestId: Math.random().toString(36).slice(2), email, deviceId, deviceName, status: 'APPROVED', createdAt: Date.now() }
    if (usePg) {
        await pgPool.query('INSERT INTO approvals(request_id, email, device_id, device_name, status, created_at) VALUES($1,$2,$3,$4,$5,$6)',
        [entry.requestId, email, deviceId, deviceName, 'APPROVED', entry.createdAt])
    }
    res.json(entry)
})

const PORT = process.env.PORT || 3000
app.listen(PORT, '0.0.0.0', () => console.log('🚀 SwiftPay Production Server listening on port', PORT))
