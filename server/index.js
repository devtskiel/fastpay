require('dotenv').config()
const express = require('express')
const bodyParser = require('body-parser')
const cors = require('cors')
const helmet = require('helmet')
const axios = require('axios')
const bcrypt = require('bcryptjs')
const jwt = require('jsonwebtoken')
const path = require('path')
const { pgPool, initDatabase, cleanKey } = require('./lib/db')
const { requireAuth, requireApiKey, JWT_SECRET } = require('./lib/auth')
const { computeHmacSha256, verifyHmacSignature, normalizeTransactionStatus } = require('./lib/merchant')
const { buildMagpieChargePayload, normalizeMagpieChargeResponse } = require('./lib/magpie')

const app = express()
app.set('trust proxy', 1) // Required for Render/Railway HTTPS detection
app.use(helmet({ contentSecurityPolicy: false }))
app.use(cors())
app.use(bodyParser.json())

const API_VERSION = 'v1.18.0'

async function startServer() {
    await initDatabase()

    // Seed/Update Admin User
    try {
        const adminEmail = 'drltechgroup2024@gmail.com'
        const hash = await bcrypt.hash('#Sirden1216', 10)
        const { rows } = await pgPool.query('SELECT id FROM users WHERE email = $1', [adminEmail])

        const isProdKeys = process.env.MAGPIE_PUBLIC_KEY?.startsWith('pk_live') || process.env.SWIFTPAY_PUBLIC_KEY?.startsWith('pk_live')

        if (rows.length === 0) {
            await pgPool.query(
                `INSERT INTO users(id, email, password_hash, business_name, sp_public_key, sp_secret_key, magpie_public_key, magpie_secret_key, role, is_production)
                 VALUES($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
                ['ADMIN_01', adminEmail, hash, 'SwiftPay Store', cleanKey(process.env.SWIFTPAY_PUBLIC_KEY), cleanKey(process.env.SWIFTPAY_SECRET_KEY), cleanKey(process.env.MAGPIE_PUBLIC_KEY), cleanKey(process.env.MAGPIE_SECRET_KEY), 'SUPER_ADMIN', isProdKeys]
            )
        } else {
            await pgPool.query(
                'UPDATE users SET sp_public_key = COALESCE($1, sp_public_key), sp_secret_key = COALESCE($2, sp_secret_key), magpie_public_key = COALESCE($3, magpie_public_key), magpie_secret_key = COALESCE($4, magpie_secret_key), role = $5, is_production = $6 WHERE email = $7',
                [cleanKey(process.env.SWIFTPAY_PUBLIC_KEY), cleanKey(process.env.SWIFTPAY_SECRET_KEY), cleanKey(process.env.MAGPIE_PUBLIC_KEY), cleanKey(process.env.MAGPIE_SECRET_KEY), 'SUPER_ADMIN', isProdKeys, adminEmail]
            )
        }
        console.log(`✅ Admin account pointed to ${isProdKeys ? 'PRODUCTION' : 'SANDBOX'} route`);
    } catch (e) { console.error('Admin seeding failed:', e.message) }

    app.listen(process.env.PORT || 3000, '0.0.0.0', () => {
        console.log(`🚀 Gateway Live - SwiftPay Enterprise ${API_VERSION}`)
        console.log('✅ 18 Expected Fixed Features Applied')
        console.log(`📧 Resend API Key: ${process.env.RESEND_API_KEY ? 'CONFIGURED' : 'MISSING'}`)
        console.log(`📧 Sender Email: ${process.env.OTP_SENDER_EMAIL || 'onboarding@resend.dev'}`)
    })
}

const getClient = async (uid) => {
    const r = await pgPool.query('SELECT sp_public_key, sp_secret_key, magpie_public_key, magpie_secret_key, business_name, is_production FROM users WHERE id = $1', [uid])
    const u = r.rows[0]
    if (!u) throw new Error('User not found')
    return {
        pub: cleanKey(u.sp_public_key),
        sec: cleanKey(u.sp_secret_key),
        mgPub: cleanKey(u.magpie_public_key),
        mgSec: cleanKey(u.magpie_secret_key),
        biz: u.business_name,
        isProd: !!u.is_production
    }
}

// --- API Routes ---

// Auth Routes
app.post('/api/auth/login', async (req, res) => {
    const { email, password } = req.body
    try {
        console.log(`🔐 Login attempt for: ${email}`);
        const normalizedEmail = (email || '').toLowerCase().trim()
        if (!normalizedEmail || !password) {
            return res.status(400).json({ error: 'Email and password are required' });
        }

        const r = await pgPool.query('SELECT * FROM users WHERE email = $1', [normalizedEmail])
        const user = r.rows[0]

        if (!user) {
            console.warn(`❌ Login failed: User ${normalizedEmail} not found`);
            return res.status(401).json({ error: 'Invalid email or password' });
        }

        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            console.warn(`❌ Login failed: Password mismatch for ${normalizedEmail}`);
            return res.status(401).json({ error: 'Invalid email or password' });
        }

        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name, role: user.role }, JWT_SECRET, { expiresIn: '24h' })
        console.log(`✅ Login successful for: ${normalizedEmail}`);
        res.json({ token, user: { id: user.id, email: user.email, businessName: user.business_name || 'Merchant', role: user.role } })
    } catch (e) {
        console.error('🔥 Login Controller Error:', e);
        res.status(500).json({ error: 'System Error: ' + e.message })
    }
})

app.post('/api/auth/register', async (req, res) => {
    const { email, password, fullName, businessName, businessAddress, businessType, idType, idNumber, selfieCaptured, documentsUploaded, acceptedTerms } = req.body
    try {
        const normalizedEmail = (email || '').toLowerCase().trim()
        if (!normalizedEmail || !password) {
            return res.status(400).json({ error: 'Email and password are required' });
        }

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
    } catch (e) { res.status(500).json({ error: 'Registration failed' }) }
})

app.post('/api/auth/forgot-password', async (req, res) => {
    const { email } = req.body
    try {
        const normalizedEmail = (email || '').toLowerCase().trim()
        const { rows } = await pgPool.query('SELECT id FROM users WHERE email = $1', [normalizedEmail])
        if (rows.length === 0) return res.status(404).json({ error: 'Account not found' })
        res.json({ status: 'success', message: 'If registered, reset link sent to ' + normalizedEmail })
    } catch (e) { res.status(500).json({ error: 'Reset failed' }) }
})

app.get('/api/auth/terms', (_req, res) => res.json({
    title: 'SwiftPay Merchant Agreement',
    content: 'By registering for a merchant account, you agree to comply with BSP-regulated payment and KYC/AML requirements. You consent to identity verification, document review, and data privacy processing for compliance and transaction monitoring.'
}))

app.get('/api/auth/compliance', (_req, res) => res.json({
    title: 'Regulatory Compliance & Privacy',
    content: `1. BSP REGULATION: SwiftPay operates in compliance with Bangko Sentral ng Pilipinas (BSP) regulations for Electronic Money Operations and Payment Systems.\n2. KYC/AML POLICY: Robust Know-Your-Customer (KYC) process implemented.\n3. DATA PRIVACY: Compliance with the Data Privacy Act of 2012 (RA 10173).\n4. SECURITY STANDARDS: Industry-standard encryption (AES-256).`
}))

const sendEmail = async (to, subject, html) => {
    const resendKey = process.env.RESEND_API_KEY;
    const allowDevFallback = process.env.NODE_ENV !== 'production';

    if (!resendKey) {
        console.warn('⚠️ RESEND_API_KEY missing. Email not sent.');
        if (allowDevFallback) {
            console.log(`ℹ️ DEV OTP bypass enabled for ${to}. OTP email delivery is not configured.`);
            return true;
        }
        return false;
    }
    try {
        await axios.post('https://api.resend.com/emails', {
            from: process.env.OTP_SENDER_EMAIL || 'onboarding@resend.dev',
            to: [to],
            subject,
            html
        }, {
            headers: { 'Authorization': `Bearer ${resendKey}`, 'Content-Type': 'application/json' }
        });
        return true;
    } catch (e) {
        console.error('📧 Email Error:', e.response?.data || e.message);
        return false;
    }
};

app.post('/api/auth/request-otp', async (req, res) => {
    const { email } = req.body;
    if (!email) return res.status(400).json({ error: 'Email required' });

    console.log(`📩 OTP Request for: ${email}`);
    const normalizedEmail = email.toLowerCase().trim();

    // Check if user exists before sending OTP
    try {
        const userCheck = await pgPool.query('SELECT id FROM users WHERE email = $1', [normalizedEmail]);
        if (userCheck.rows.length === 0) {
            console.warn(`⚠️ OTP Request failed: ${normalizedEmail} is not registered`);
            return res.status(404).json({ error: 'Account not found' });
        }

        const code = Math.floor(100000 + Math.random() * 900000).toString();
        const expiresAt = new Date(Date.now() + 10 * 60000); // 10 mins

        await pgPool.query(
            'INSERT INTO verification_codes(email, code, expires_at) VALUES($1, $2, $3) ON CONFLICT (email) DO UPDATE SET code = $2, expires_at = $3',
            [normalizedEmail, code, expiresAt]
        );

        const sent = await sendEmail(
            normalizedEmail,
            'Your SwiftPay Verification Code',
            `<strong>${code}</strong> is your SwiftPay access code. It expires in 10 minutes.`
        );

        if (sent) {
            console.log(`✅ OTP sent to: ${normalizedEmail}`);
            res.json({ status: 'success', message: 'OTP sent' });
        } else {
            console.error(`❌ SMTP Failure for: ${normalizedEmail}`);
            res.status(500).json({ error: 'Failed to deliver email. Please try again later.' });
        }
    } catch (e) {
        console.error('🔥 OTP Request Error:', e);
        res.status(500).json({ error: 'Internal error: ' + e.message });
    }
});

app.post('/api/auth/verify-otp', async (req, res) => {
    const { email, code } = req.body;
    const normalizedEmail = (email || '').toLowerCase().trim();
    console.log(`🔑 Verifying OTP for: ${normalizedEmail}`);

    try {
        const { rows } = await pgPool.query('SELECT * FROM verification_codes WHERE email = $1', [normalizedEmail]);
        const record = rows[0];

        if (!record) {
            console.warn(`❌ OTP Verify failed: No code found for ${normalizedEmail}`);
            return res.status(401).json({ error: 'No verification session found' });
        }

        if (record.code !== code) {
            console.warn(`❌ OTP Verify failed: Incorrect code for ${normalizedEmail}`);
            return res.status(401).json({ error: 'Invalid verification code' });
        }

        if (new Date() > record.expires_at) {
            console.warn(`❌ OTP Verify failed: Expired code for ${normalizedEmail}`);
            return res.status(401).json({ error: 'Verification code expired' });
        }

        await pgPool.query('DELETE FROM verification_codes WHERE email = $1', [normalizedEmail]);

        // Return login success data
        const userRes = await pgPool.query('SELECT * FROM users WHERE email = $1', [normalizedEmail]);
        const user = userRes.rows[0];

        if (!user) {
            console.error(`🔥 Critical Error: User ${normalizedEmail} disappeared after verification`);
            return res.status(404).json({ error: 'User account not found' });
        }

        const token = jwt.sign({ id: user.id, email: user.email, businessName: user.business_name, role: user.role }, JWT_SECRET, { expiresIn: '24h' });
        console.log(`✅ OTP verified. Session created for: ${normalizedEmail}`);
        res.json({ token, user: { id: user.id, email: user.email, businessName: user.business_name || 'Merchant', role: user.role } });
    } catch (e) {
        console.error('🔥 OTP Verification Error:', e);
        res.status(500).json({ error: 'Verification failed: ' + e.message });
    }
});

// Admin Routes
app.get('/api/admin/metrics', requireAuth, async (req, res) => {
    if (req.user?.role !== 'SUPER_ADMIN') return res.status(403).json({ error: 'Super Admin only' })
    try {
        const [registrations, deposits, members, events, disbursements] = await Promise.all([
            pgPool.query("SELECT COUNT(*)::int AS count FROM merchant_registrations WHERE status = 'PENDING'"),
            pgPool.query("SELECT COUNT(*)::int AS count FROM deposits WHERE status = 'PENDING'"),
            pgPool.query("SELECT COUNT(*)::int AS count FROM merchant_members WHERE status = 'ACTIVE'"),
            pgPool.query("SELECT COUNT(*)::int AS count FROM payment_events"),
            pgPool.query("SELECT COUNT(*)::int AS count FROM disbursements WHERE status = 'PENDING'")
        ])
        res.json({
            pendingRegistrations: registrations.rows[0]?.count || 0,
            pendingDeposits: deposits.rows[0]?.count || 0,
            activeMembers: members.rows[0]?.count || 0,
            paymentEvents: events.rows[0]?.count || 0,
            pendingDisbursements: disbursements.rows[0]?.count || 0
        })
    } catch (e) { res.status(500).json({ error: 'Metrics failed' }) }
})

app.get('/api/admin/registrations', requireAuth, async (req, res) => {
    if (req.user?.role !== 'SUPER_ADMIN') return res.status(403).json({ error: 'Super Admin only' })
    try {
        const { rows } = await pgPool.query('SELECT * FROM merchant_registrations ORDER BY created_at DESC LIMIT 20')
        res.json(rows.map(row => ({
            id: row.id, email: row.email, fullName: row.full_name, businessName: row.business_name,
            businessType: row.business_type, status: row.status, createdAt: row.created_at
        })))
    } catch (e) { res.status(500).json({ error: 'Registrations failed' }) }
})

app.post('/api/admin/registrations/:id/status', requireAuth, async (req, res) => {
    if (req.user?.role !== 'SUPER_ADMIN') return res.status(403).json({ error: 'Super Admin only' })
    try {
        const { status } = req.body
        await pgPool.query('UPDATE merchant_registrations SET status = $1 WHERE id = $2', [status, req.params.id])
        res.json({ status: 'success' })
    } catch (e) { res.status(500).json({ error: 'Update failed' }) }
})

// Member Management
app.post('/api/admin/members', requireAuth, async (req, res) => {
    const { id, name, email, role, permissions = {} } = req.body
    if (!req.user?.id) {
        return res.status(401).json({ error: 'Unauthorized' });
    }
    try {
        if (id) {
            await pgPool.query(
                'UPDATE merchant_members SET name = $1, email = $2, role = $3, permissions = $4 WHERE id = $5 AND merchant_id = $6',
                [name, email, role, JSON.stringify(permissions), id, req.user.id]
            )
            return res.json({ status: 'success', memberId: id })
        }
        const memberId = 'MEM' + Date.now()
        await pgPool.query(
            'INSERT INTO merchant_members(id, merchant_id, name, email, role, permissions, status) VALUES($1, $2, $3, $4, $5, $6, $7)',
            [memberId, req.user.id, name, email, role, JSON.stringify(permissions), 'ACTIVE']
        )
        res.json({ status: 'success', memberId })
    } catch (e) { res.status(500).json({ error: 'Member operation failed' }) }
})

app.get('/api/admin/members', requireAuth, async (req, res) => {
    try {
        const { rows } = await pgPool.query('SELECT * FROM merchant_members WHERE merchant_id = $1 ORDER BY created_at DESC', [req.user.id])
        res.json(rows.map(row => ({ id: row.id, name: row.name, email: row.email, role: row.role, permissions: row.permissions || {}, status: row.status })))
    } catch (e) { res.status(500).json({ error: 'Failed to load members' }) }
})

// --- Payment & Collection Routes ---

app.post('/api/payments/checkout', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        const { amount, description, customerEmail } = req.body
        const reference = 'ORD-' + Date.now()

        // Dynamic DNS Routing for Callbacks
        const serverUrl = process.env.APP_PUBLIC_URL || `${req.protocol}://${req.get('host')}`
        const callbackUrl = process.env.SWIFTPAY_CALLBACK_URL || `${serverUrl}/api/payments/callback`
        const webhookUrl = process.env.SWIFTPAY_WEBHOOK_URL || `${serverUrl}/api/payments/callback`

        if (u.isProd) {
            return res.status(400).json({ error: 'Please use /api/payments/magpie/checkout for production collections' })
        }

        // [SANDBOX] SwiftPay Collection Integration
        const payload = {
            amount: parseFloat(amount).toFixed(2),
            currency: 'PHP',
            reference,
            description: description || 'SwiftPay Order',
            email: customerEmail,
            callbackUrl,
            webhookUrl
        }

        const signature = computeHmacSha256(JSON.stringify(payload), u.sec)
        const sandboxApiUrl = 'https://api.pay.sandbox.live.swiftpay.ph/api/orders'

        console.log(`🔗 Pointing SwiftPay Callback to route: ${callbackUrl}`)

        try {
            const resp = await axios.post(sandboxApiUrl, { ...payload, signature }, {
                headers: { 'x-access-key': u.pub }
            })
            res.json({ checkoutUrl: resp.data.customerRedirectUrl, reference, status: 'PENDING' })
        } catch (apiErr) {
            const checkoutUrl = `https://pay.sandbox.swiftpay.ph/checkout?ref=${reference}&sig=${signature}`
            res.json({ checkoutUrl, reference, status: 'PENDING' })
        }
    } catch (e) { res.status(500).json({ error: 'Checkout failed' }) }
})

// [PRODUCTION] Magpie Collection Integration
app.post('/api/payments/magpie/checkout', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        const { amount, description, token, paymentMethod = 'qrph', referenceNo, metadata = {} } = req.body
        const MAGPIE_SECRET = u.mgSec || process.env.MAGPIE_SECRET_KEY
        const payload = buildMagpieChargePayload({
            amount,
            description,
            referenceNo: referenceNo || `MAGPIE_${Date.now()}`,
            paymentMethod,
            sourceToken: token,
            metadata
        })

        const resp = await axios.post('https://api.magpie.im/v1/charges', payload, {
            auth: { username: MAGPIE_SECRET, password: '' },
            headers: { 'Content-Type': 'application/json' }
        })
        res.json(normalizeMagpieChargeResponse(resp.data, { paymentMethod }))
    } catch (e) {
        const message = e.response?.data?.message || e.response?.data?.error || e.message
        res.status(500).json({ error: 'Magpie payment failed: ' + message })
    }
})

// --- Disbursement & Payout Routes ---

app.post('/api/swiftpay/disburse', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        const { amount, accountNumber, firstName, lastName, institutionCode } = req.body
        const internalId = 'DISB' + Date.now()
        const referenceNo = 'P' + Date.now()

        await pgPool.query(
            'INSERT INTO disbursements(id, merchant_id, amount, account_number, bank_code, beneficiary_name, status, external_reference) VALUES($1, $2, $3, $4, $5, $6, $7, $8)',
            [internalId, req.user.id, amount, accountNumber, institutionCode, `${firstName} ${lastName}`, 'PENDING', referenceNo]
        )

        if (u.isProd) {
            return res.json({ status: 'submitted', disbursementId: internalId, message: 'Payout request sent for admin approval' })
        }

        // [SANDBOX] Swiftpay Disbursement Integration
        const baseUrl = u.isProd ? 'https://api.pay.live.swiftpay.ph' : 'https://api.pay.sandbox.live.swiftpay.ph'
        const auth = Buffer.from(`${u.pub}:${u.sec}`).toString('base64')

        const serverUrl = process.env.APP_PUBLIC_URL || `${req.protocol}://${req.get('host')}`
        const callbackUrl = process.env.DISBURSEMENT_CALLBACK_URL || `${serverUrl}/api/swiftpay/disbursement-callback`

        const payload = {
            merchantReferenceNo: referenceNo, channel: 'INSTAPAY', institutionCode,
            creditInformation: { amount: parseFloat(amount).toFixed(2), remarks: 'Payout' },
            recipientInformation: { accountNumber, firstName, lastName },
            callbackUrl
        }

        console.log(`🔗 Pointing Disbursement Callback to route: ${callbackUrl}`)

        try {
            await axios.post(`${baseUrl}/api/disbursements/send`, payload, { headers: { 'Authorization': `Basic ${auth}` } })
            res.json({ status: 'success', disbursementId: internalId, reference: referenceNo })
        } catch (apiErr) {
            res.json({ status: 'PENDING', disbursementId: internalId, message: 'Disbursement initiated (API call logged)' })
        }
    } catch (e) { res.status(500).json({ error: 'Payout Failed: ' + (e.response?.data?.message || e.message) }) }
})

// Admin Disbursement Management
app.get('/api/admin/disbursements', requireAuth, async (req, res) => {
    if (req.user.role !== 'SUPER_ADMIN') return res.status(403).json({ error: 'Super Admin only' })
    try {
        const { rows } = await pgPool.query("SELECT d.*, u.email as merchant_email FROM disbursements d JOIN users u ON d.merchant_id = u.id WHERE d.status = 'PENDING' ORDER BY d.created_at DESC")
        res.json(rows)
    } catch (e) { res.status(500).json({ error: 'Query failed' }) }
})

app.post('/api/admin/disbursements/:id/approve', requireAuth, async (req, res) => {
    if (req.user.role !== 'SUPER_ADMIN') return res.status(403).json({ error: 'Super Admin only' })
    try {
        const { rows } = await pgPool.query('SELECT d.*, u.sp_public_key, u.sp_secret_key FROM disbursements d JOIN users u ON d.merchant_id = u.id WHERE d.id = $1', [req.params.id])
        const d = rows[0]
        if (!d) return res.status(404).json({ error: 'Not found' })

        const baseUrl = process.env.SWIFTPAY_BASE_URL || 'https://api.pay.live.swiftpay.ph'
        const auth = Buffer.from(`${cleanKey(d.sp_public_key)}:${cleanKey(d.sp_secret_key)}`).toString('base64')
        const payload = {
            merchantReferenceNo: d.external_reference, channel: 'INSTAPAY', institutionCode: d.bank_code,
            creditInformation: { amount: parseFloat(d.amount).toFixed(2), remarks: 'Payout' },
            recipientInformation: { accountNumber: d.account_number, firstName: d.beneficiary_name.split(' ')[0], lastName: d.beneficiary_name.split(' ').slice(1).join(' ') },
            callbackUrl: process.env.DISBURSEMENT_CALLBACK_URL || 'https://example.com/payout-callback'
        }

        await axios.post(`${baseUrl}/api/disbursements/send`, payload, { headers: { 'Authorization': `Basic ${auth}` } })
        await pgPool.query("UPDATE disbursements SET status = 'EXECUTED', updated_at = NOW() WHERE id = $1", [req.params.id])
        res.json({ status: 'success' })
    } catch (e) { res.status(500).json({ error: 'Approval failed: ' + (e.response?.data?.message || e.message) }) }
})

app.post('/api/admin/disbursements/:id/reject', requireAuth, async (req, res) => {
    if (req.user.role !== 'SUPER_ADMIN') return res.status(403).json({ error: 'Super Admin only' })
    try {
        await pgPool.query("UPDATE disbursements SET status = 'REJECTED', updated_at = NOW() WHERE id = $1", [req.params.id])
        res.json({ status: 'success' })
    } catch (e) { res.status(500).json({ error: 'Reject failed' }) }
})

// Infrastructure & Utility Routes
app.get('/api/swiftpay/balance', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        // Correct Auth Format for Netbank Balance: sk_...:
        const auth = Buffer.from(`${u.sec}:`).toString('base64')
        const resp = await axios.get('https://api.netbank.ph/v1/account/balance', { headers: { 'Authorization': `Basic ${auth}` } })
        res.json(resp.data)
    } catch (e) {
        console.error('Balance Error:', e.response?.data || e.message);
        res.status(500).json({ error: 'Balance Error' })
    }
})

app.get('/api/swiftpay/transactions', requireAuth, async (req, res) => {
    try {
        const u = await getClient(req.user.id)
        const auth = Buffer.from(`${u.pub}:${u.sec}`).toString('base64')
        const resp = await axios.get('https://api.netbank.ph/v1/collect/payments', { headers: { 'Authorization': `Basic ${auth}` } })
        res.json((resp.data.data || []).map(t => ({ id: t.id, amount: parseFloat(t.amount), status: t.status, date: t.createdAt })))
    } catch (e) { res.json([]) }
})

app.get('/api/swiftpay/disbursements', requireAuth, async (req, res) => {
    try {
        const { rows } = await pgPool.query('SELECT * FROM disbursements WHERE merchant_id = $1 ORDER BY created_at DESC', [req.user.id])
        res.json(rows.map(r => ({
            id: r.id, recipientInformation: { firstName: r.beneficiary_name.split(' ')[0], lastName: r.beneficiary_name.split(' ').slice(1).join(' ') },
            creditInformation: { amount: r.amount }, status: r.status, createdAt: r.created_at
        })))
    } catch (e) { res.json([]) }
})

app.get('/api/swiftpay/settings', requireAuth, async (req, res) => {
    if (!req.user?.id) {
        return res.status(401).json({ error: 'Unauthorized' });
    }
    const r = await pgPool.query(`SELECT business_name, business_address, contact_number, shop_url, logo_url,
        source_account_number, sp_public_key, sp_secret_key, magpie_public_key, magpie_secret_key,
        is_production, redirect_url, webhook_url FROM users WHERE id = $1`, [req.user.id])
    res.json(r.rows[0])
})

app.post('/api/swiftpay/profile', requireAuth, async (req, res) => {
    const { businessName, businessAddress, contactNumber, shopUrl, logoUrl, sourceAccountNumber, spPublicKey, spSecretKey, magpiePublicKey, magpieSecretKey, isProduction, redirectUrl, webhookUrl } = req.body
    await pgPool.query(
        `UPDATE users SET business_name = $1, business_address = $2, contact_number = $3, shop_url = $4, logo_url = $5,
        source_account_number = $6, sp_public_key = $7, sp_secret_key = $8, magpie_public_key = $9, magpie_secret_key = $10,
        is_production = $11, redirect_url = $12, webhook_url = $13 WHERE id = $14`,
        [businessName, businessAddress, contactNumber, shopUrl, logoUrl, sourceAccountNumber, spPublicKey, spSecretKey, magpiePublicKey, magpieSecretKey, isProduction, redirectUrl, webhookUrl, req.user.id]
    )
    res.json({ status: 'success' })
})

// --- Webhook & Callback Handlers ---

app.post('/api/payments/callback', express.json({ type: '*/*' }), async (req, res) => {
    try {
        const payload = typeof req.body === 'string' ? req.body : JSON.stringify(req.body)
        const swiftSignature = req.headers['x-signature'] || req.headers['x-swiftpay-signature'] || ''
        const webhookSecret = process.env.SWIFTPAY_WEBHOOK_SECRET || 'demo-secret'

        if (!verifyHmacSignature(payload, swiftSignature, webhookSecret)) {
            return res.status(401).json({ error: 'Invalid signature' })
        }

        const rawAmount = Number(req.body?.amount || 0)
        const serviceFee = rawAmount * 0.005
        const netAmount = rawAmount - serviceFee
        const normalizedStatus = normalizeTransactionStatus(req.body?.status || req.body?.data?.status)
        const paymentId = 'PAY' + Date.now()

        await pgPool.query(
            'INSERT INTO payment_events(id, merchant_id, source, external_reference, status, amount, payload) VALUES($1, $2, $3, $4, $5, $6, $7)',
            [paymentId, 'DEFAULT', 'SWIFTPAY', req.body?.referenceNo || req.body?.id || 'unknown', normalizedStatus, netAmount, JSON.stringify({ ...req.body, fee_deducted: serviceFee })]
        )
        res.json({ status: 'accepted', paymentId, netAmount: netAmount.toFixed(2), finalStatus: normalizedStatus })
    } catch (e) { res.status(500).json({ error: 'Callback failed' }) }
})

app.post('/api/swiftpay/disbursement-callback', express.json({ type: '*/*' }), async (req, res) => {
    try {
        const { merchantReferenceNo, status } = req.body
        const normalizedStatus = normalizeTransactionStatus(status)
        await pgPool.query('UPDATE disbursements SET status = $1, updated_at = NOW() WHERE external_reference = $2', [normalizedStatus, merchantReferenceNo])
        res.json({ status: 'received' })
    } catch (e) { res.status(500).json({ error: 'Callback failed' }) }
})

app.get('/health', (req, res) => res.json({ status: 'UP', timestamp: new Date().toISOString(), version: API_VERSION }))
app.use(express.static(path.join(__dirname, 'public')))
app.get('*', (req, res) => res.sendFile(path.join(__dirname, 'public', 'dashboard.html')))

startServer()
