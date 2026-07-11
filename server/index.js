require('dotenv').config()
const express = require('express')
const bodyParser = require('body-parser')
const cors = require('cors')
const helmet = require('helmet')
const axios = require('axios')
const bcrypt = require('bcryptjs')
const jwt = require('jsonwebtoken')
const path = require('path')
const { computeHmacSha256, verifyHmacSignature, normalizeTransactionStatus } = require('./lib/merchant')

const app = express()
app.use(helmet({ contentSecurityPolicy: false }))
app.use(cors())
app.use(bodyParser.json())

const JWT_SECRET = process.env.JWT_SECRET || 'swiftpay-enterprise-core-2024'
const APP_SERVER_KEY = process.env.APP_SERVER_KEY || 'my-secret-key'

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

        await pgPool.query(`CREATE TABLE IF NOT EXISTS approvals (
            request_id TEXT PRIMARY KEY,
            email TEXT NOT NULL,
            device_id TEXT NOT NULL,
            device_name TEXT NOT NULL,
            status TEXT DEFAULT 'PENDING',
            created_at BIGINT,
            expires_at BIGINT
        )`)

        await pgPool.query(`CREATE TABLE IF NOT EXISTS deposits (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            user_email TEXT NOT NULL,
            amount DECIMAL(12,2) NOT NULL,
            reference_number TEXT,
            bank_name TEXT,
            status TEXT DEFAULT 'PENDING',
            created_at TIMESTAMPTZ DEFAULT NOW()
        )`)

        await pgPool.query(`CREATE TABLE IF NOT EXISTS merchant_registrations (
            id TEXT PRIMARY KEY,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            full_name TEXT NOT NULL,
            business_name TEXT NOT NULL,
            business_address TEXT,
            business_type TEXT,
            id_type TEXT,
            id_number TEXT,
            selfie_captured BOOLEAN DEFAULT FALSE,
            documents_uploaded BOOLEAN DEFAULT FALSE,
            accepted_terms BOOLEAN DEFAULT FALSE,
            status TEXT DEFAULT 'PENDING',
            created_at TIMESTAMPTZ DEFAULT NOW()
        )`)

        await pgPool.query(`CREATE TABLE IF NOT EXISTS merchant_members (
            id TEXT PRIMARY KEY,
            merchant_id TEXT NOT NULL,
            name TEXT NOT NULL,
            email TEXT NOT NULL,
            role TEXT NOT NULL,
            permissions JSONB DEFAULT '{}'::jsonb,
            status TEXT DEFAULT 'ACTIVE',
            created_at TIMESTAMPTZ DEFAULT NOW()
        )`)

        await pgPool.query(`CREATE TABLE IF NOT EXISTS payment_events (
            id TEXT PRIMARY KEY,
            merchant_id TEXT NOT NULL,
            source TEXT NOT NULL,
            external_reference TEXT,
            status TEXT NOT NULL,
            amount DECIMAL(12,2) DEFAULT 0,
            payload JSONB DEFAULT '{}'::jsonb,
            created_at TIMESTAMPTZ DEFAULT NOW()
        )`)

        const adminEmail = 'drltechgroup2024@gmail.com'
        const hash = await bcrypt.hash('#Sirden1216', 10)
        const { rows } = await pgPool.query('SELECT id FROM users WHERE email = $1', [adminEmail])

        if (rows.length === 0) {
            await pgPool.query(
                'INSERT INTO users(id, email, password_hash, business_name, sp_public_key, sp_secret_key) VALUES($1, $2, $3, $4, $5, $6)',
                ['ADMIN_01', adminEmail, hash, 'SwiftPay Store', cleanKey(process.env.SWIFTPAY_PUBLIC_KEY), cleanKey(process.env.SWIFTPAY_SECRET_KEY)]
            )
        } else {
            // Update keys if env vars are present to ensure sync
            if (process.env.SWIFTPAY_PUBLIC_KEY || process.env.SWIFTPAY_SECRET_KEY) {
                await pgPool.query(
                    'UPDATE users SET sp_public_key = $1, sp_secret_key = $2 WHERE email = $3',
                    [cleanKey(process.env.SWIFTPAY_PUBLIC_KEY), cleanKey(process.env.SWIFTPAY_SECRET_KEY), adminEmail]
                )
            }
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

const requireApiKey = (req, res, next) => {
    const key = req.headers['x-api-key']
    if (key !== APP_SERVER_KEY) return res.status(401).json({ error: 'Forbidden' })
    next()
}

// --- API Routes ---

app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body
    try {
        const normalizedEmail = (email || '').toLowerCase().trim()
        const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [normalizedEmail])
        const user = r.rows[0]
        if (!user || !(await bcrypt.compare(password, user.password_hash))) return res.status(401).json({ error: 'Invalid keys' })
        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name, role: 'SUPER_ADMIN' }, JWT_SECRET, { expiresIn: '24h' })
        res.json({ token, user: { businessName: user.business_name || 'Merchant', role: 'SUPER_ADMIN' } })
    } catch (e) { res.status(500).json({ error: 'System Error' }) }
})

app.post('/api/auth/register', async (req, res) => {
    const { email, password, fullName, businessName, businessAddress, businessType, idType, idNumber, selfieCaptured, documentsUploaded, acceptedTerms } = req.body
    try {
        const normalizedEmail = (email || '').toLowerCase().trim()
        const existing = await pgPool.query('SELECT id FROM merchant_registrations WHERE email = $1', [normalizedEmail])
        if (existing.rows.length > 0) return res.status(409).json({ error: 'Email already registered' })

        const passwordHash = await bcrypt.hash(password, 10)
        const registrationId = 'REG' + Date.now()
        await pgPool.query(
            `INSERT INTO merchant_registrations(id, email, password_hash, full_name, business_name, business_address, business_type, id_type, id_number, selfie_captured, documents_uploaded, accepted_terms, status)
             VALUES($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)`,
            [registrationId, normalizedEmail, passwordHash, fullName, businessName, businessAddress, businessType, idType, idNumber, !!selfieCaptured, !!documentsUploaded, !!acceptedTerms, 'PENDING']
        )

        const userId = 'MERCHANT_' + Date.now()
        await pgPool.query(
            `INSERT INTO users(id, email, password_hash, business_name, sp_public_key, sp_secret_key) VALUES($1, $2, $3, $4, $5, $6)`,
            [userId, normalizedEmail, passwordHash, businessName, cleanKey(process.env.SWIFTPAY_PUBLIC_KEY), cleanKey(process.env.SWIFTPAY_SECRET_KEY)]
        )

        res.json({ status: 'success', registrationId, email: normalizedEmail })
    } catch (e) {
        res.status(500).json({ error: 'Registration failed' })
    }
})

app.post('/api/auth/forgot-password', async (req, res) => {
    const { email } = req.body
    try {
        const normalizedEmail = (email || '').toLowerCase().trim()
        const { rows } = await pgPool.query('SELECT id FROM users WHERE email = $1', [normalizedEmail])
        if (rows.length === 0) return res.status(404).json({ error: 'No account found' })
        res.json({ status: 'success', message: 'Password reset instructions were sent.' })
    } catch (e) {
        res.status(500).json({ error: 'Reset request failed' })
    }
})

app.get('/api/auth/terms', (_req, res) => {
    res.json({
        title: 'SwiftPay Merchant Agreement',
        content: 'By registering for a merchant account, you agree to comply with BSP-regulated payment and KYC/AML requirements. You consent to identity verification, document review, and data privacy processing for compliance and transaction monitoring.'
    })
})

app.get('/api/auth/compliance', (_req, res) => {
    res.json({
        title: 'Regulatory Compliance',
        content: 'SwiftPay is designed for BSP-regulated electronic money and payment services. We follow KYC/AML, data privacy, and secure transaction handling standards.'
    })
})

app.post('/api/admin/members', requireAuth, async (req, res) => {
    const { name, email, role, permissions = {} } = req.body
    try {
        const memberId = 'MEM' + Date.now()
        await pgPool.query(
            'INSERT INTO merchant_members(id, merchant_id, name, email, role, permissions, status) VALUES($1, $2, $3, $4, $5, $6, $7)',
            [memberId, req.user.id, name, email, role, JSON.stringify(permissions), 'ACTIVE']
        )
        res.json({ status: 'success', memberId })
    } catch (e) {
        res.status(500).json({ error: 'Member invite failed' })
    }
})

app.get('/api/admin/members', requireAuth, async (req, res) => {
    try {
        const { rows } = await pgPool.query('SELECT * FROM merchant_members WHERE merchant_id = $1 ORDER BY created_at DESC', [req.user.id])
        res.json(rows.map(row => ({ id: row.id, name: row.name, email: row.email, role: row.role, permissions: row.permissions || {}, status: row.status })))
    } catch (e) {
        res.status(500).json({ error: 'Failed to load members' })
    }
})

app.get('/api/admin/metrics', requireAuth, async (req, res) => {
    if (req.user.email !== 'drltechgroup2024@gmail.com') return res.status(403).json({ error: 'Admin only' })
    try {
        const [registrations, deposits, members, events] = await Promise.all([
            pgPool.query("SELECT COUNT(*)::int AS count FROM merchant_registrations WHERE status = 'PENDING'"),
            pgPool.query("SELECT COUNT(*)::int AS count FROM deposits WHERE status = 'PENDING'"),
            pgPool.query("SELECT COUNT(*)::int AS count FROM merchant_members WHERE status = 'ACTIVE'"),
            pgPool.query("SELECT COUNT(*)::int AS count FROM payment_events")
        ])
        res.json({
            pendingRegistrations: registrations.rows[0]?.count || 0,
            pendingDeposits: deposits.rows[0]?.count || 0,
            activeMembers: members.rows[0]?.count || 0,
            paymentEvents: events.rows[0]?.count || 0
        })
    } catch (e) {
        res.status(500).json({ error: 'Metrics failed' })
    }
})

app.get('/api/admin/registrations', requireAuth, async (req, res) => {
    if (req.user.email !== 'drltechgroup2024@gmail.com') return res.status(403).json({ error: 'Admin only' })
    try {
        const { rows } = await pgPool.query('SELECT * FROM merchant_registrations ORDER BY created_at DESC LIMIT 20')
        res.json(rows.map(row => ({
            id: row.id,
            email: row.email,
            fullName: row.full_name,
            businessName: row.business_name,
            businessType: row.business_type,
            status: row.status,
            createdAt: row.created_at
        })))
    } catch (e) {
        res.status(500).json({ error: 'Registrations failed' })
    }
})

app.post('/api/admin/registrations/:id/status', requireAuth, async (req, res) => {
    if (req.user.email !== 'drltechgroup2024@gmail.com') return res.status(403).json({ error: 'Admin only' })
    try {
        const { status } = req.body
        await pgPool.query('UPDATE merchant_registrations SET status = $1 WHERE id = $2', [status, req.params.id])
        res.json({ status: 'success' })
    } catch (e) {
        res.status(500).json({ error: 'Update failed' })
    }
})

app.post('/api/admin/members/:id/status', requireAuth, async (req, res) => {
    const { status } = req.body
    try {
        await pgPool.query('UPDATE merchant_members SET status = $1 WHERE id = $2 AND merchant_id = $3', [status, req.params.id, req.user.id])
        res.json({ status: 'success' })
    } catch (e) {
        res.status(500).json({ error: 'Update failed' })
    }
})

app.get('/health', (req, res) => res.json({ status: 'UP', timestamp: new Date().toISOString() }))

// --- Approvals (Legacy/Multi-device) ---
app.post('/approvals', requireApiKey, async (req, res) => {
    const { email, deviceId, deviceName } = req.body
    const requestId = 'REQ' + Math.random().toString(36).substr(2, 9).toUpperCase()
    const now = Date.now()
    const expires = now + 300000 // 5 mins
    await pgPool.query(
        'INSERT INTO approvals(request_id, email, device_id, device_name, status, created_at, expires_at) VALUES($1, $2, $3, $4, $5, $6, $7)',
        [requestId, email, deviceId, deviceName, 'PENDING', now, expires]
    )
    res.json({ requestId, email, deviceId, deviceName, status: 'PENDING', createdAt: now, expiresAt: expires })
})

app.get('/approvals/:id', requireApiKey, async (req, res) => {
    const { rows } = await pgPool.query('SELECT * FROM approvals WHERE request_id = $1', [req.params.id])
    if (rows[0]) {
        const r = rows[0]
        res.json({ requestId: r.request_id, email: r.email, deviceId: r.device_id, deviceName: r.device_name, status: r.status, createdAt: parseInt(r.created_at), expiresAt: parseInt(r.expires_at) })
    } else res.status(404).json({ error: 'Not Found' })
})

app.get('/approvals', requireApiKey, async (req, res) => {
    const { email } = req.query
    const { rows } = await pgPool.query('SELECT * FROM approvals WHERE email = $1 AND status = $2', [email, 'PENDING'])
    res.json(rows.map(r => ({ requestId: r.request_id, email: r.email, deviceId: r.device_id, deviceName: r.device_name, status: r.status, createdAt: parseInt(r.created_at), expiresAt: parseInt(r.expires_at) })))
})

app.post('/approvals/:id/approve', requireApiKey, async (req, res) => {
    await pgPool.query('UPDATE approvals SET status = $1 WHERE request_id = $2', ['APPROVED', req.params.id])
    const { rows } = await pgPool.query('SELECT * FROM approvals WHERE request_id = $1', [req.params.id])
    res.json(rows[0])
})

app.post('/approvals/:id/deny', requireApiKey, async (req, res) => {
    await pgPool.query('UPDATE approvals SET status = $1 WHERE request_id = $2', ['DENIED', req.params.id])
    const { rows } = await pgPool.query('SELECT * FROM approvals WHERE request_id = $1', [req.params.id])
    res.json(rows[0])
})

// --- SwiftPay Gateway ---
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

app.post('/api/swiftpay/qr', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        const auth = Buffer.from(`${u.pub}:${u.sec}`).toString('base64')
        const { amount } = req.body
        const payload = {
            totalAmount: { value: parseFloat(amount), currency: 'PHP' },
            requestReferenceNumber: 'QR' + Date.now(),
            type: 'DYNAMIC'
        }
        const resp = await axios.post('https://api.netbank.ph/v1/collect/qr/payments', payload, { headers: { 'Authorization': `Basic ${auth}` } })
        res.json(resp.data)
    } catch (e) { res.status(500).json({ error: 'QR Error' }) }
})

// --- Deposits ---
app.post('/api/swiftpay/deposit', requireAuth, async (req, res) => {
    const { amount, referenceNumber, bankName } = req.body
    const id = 'DEP' + Date.now()
    try {
        await pgPool.query(
            'INSERT INTO deposits(id, user_id, user_email, amount, reference_number, bank_name) VALUES($1, $2, $3, $4, $5, $6)',
            [id, req.user.id, req.user.email, amount, referenceNumber, bankName]
        )
        res.json({ status: 'success', depositId: id })
    } catch (e) { res.status(500).json({ error: 'Deposit failed' }) }
})

app.post('/api/payments/callback', express.json({ type: '*/*' }), async (req, res) => {
    try {
        const payload = typeof req.body === 'string' ? req.body : JSON.stringify(req.body)
        const signature = req.headers['x-signature'] || req.headers['x-swiftpay-signature'] || ''
        const webhookSecret = process.env.SWIFTPAY_WEBHOOK_SECRET || 'demo-secret'
        const isValid = verifyHmacSignature(payload, signature, webhookSecret)
        if (!isValid) return res.status(401).json({ error: 'Invalid signature' })

        const event = req.body?.event || req.body?.status || 'payment.updated'
        const externalReference = req.body?.id || req.body?.paymentId || req.body?.referenceNo || 'unknown'
        const amount = Number(req.body?.amount || req.body?.data?.amount || 0)
        const normalizedStatus = normalizeTransactionStatus(req.body?.status || req.body?.data?.status || event)
        const paymentId = 'PAY' + Date.now()

        await pgPool.query(
            'INSERT INTO payment_events(id, merchant_id, source, external_reference, status, amount, payload) VALUES($1, $2, $3, $4, $5, $6, $7)',
            [paymentId, 'DEFAULT', 'SWIFTPAY', externalReference, normalizedStatus, amount, JSON.stringify(req.body)]
        )

        res.json({ status: 'accepted', paymentId, status: normalizedStatus })
    } catch (e) {
        res.status(500).json({ error: 'Callback failed' })
    }
})

app.get('/api/payments/events', requireAuth, async (req, res) => {
    try {
        const { rows } = await pgPool.query('SELECT * FROM payment_events WHERE merchant_id = $1 ORDER BY created_at DESC LIMIT 20', [req.user.id])
        res.json(rows.map(row => ({ id: row.id, externalReference: row.external_reference, status: row.status, amount: parseFloat(row.amount), source: row.source })))
    } catch (e) {
        res.status(500).json({ error: 'Failed to load events' })
    }
})

app.get('/api/admin/deposits', requireAuth, async (req, res) => {
    // Basic admin check (could be more robust)
    if (req.user.email !== 'drltechgroup2024@gmail.com') return res.status(403).json({ error: 'Admin only' })
    try {
        const { rows } = await pgPool.query('SELECT * FROM deposits ORDER BY created_at DESC')
        res.json(rows)
    } catch (e) { res.status(500).json({ error: 'Query failed' }) }
})

app.post('/api/admin/deposits/:id/status', requireAuth, async (req, res) => {
    if (req.user.email !== 'drltechgroup2024@gmail.com') return res.status(403).json({ error: 'Admin only' })
    const { status } = req.body
    try {
        await pgPool.query('UPDATE deposits SET status = $1 WHERE id = $2', [status, req.params.id])
        res.json({ status: 'success' })
    } catch (e) { res.status(500).json({ error: 'Update failed' }) }
})

app.use(express.static(path.join(__dirname, 'public')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

startServer()
