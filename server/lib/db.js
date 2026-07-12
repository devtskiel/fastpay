const { Pool } = require('pg');
require('dotenv').config();

const rawDbUrl = process.env.DATABASE_URL;

// Fix: Log the database route being used (masked for security)
if (rawDbUrl) {
    const dbHost = rawDbUrl.split('@')[1]?.split('/')[0] || 'unknown';
    console.log(`📡 Pointing Database to route: ${dbHost}`);
} else {
    console.warn('⚠️ No DATABASE_URL found. Database is not pointed to any route.');
}

const pgPool = new Pool({
    connectionString: rawDbUrl ? rawDbUrl.replace('postgresql://', 'postgres://') : undefined,
    ssl: { rejectUnauthorized: false },
    // Optimized for production stability
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 10000,
});

const cleanKey = (k) => k ? k.split('/')[0].split(' ')[0].trim() : '';

async function initDatabase() {
    try {
        await pgPool.query(`CREATE TABLE IF NOT EXISTS users (
            id TEXT PRIMARY KEY,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            business_name TEXT,
            business_address TEXT,
            contact_number TEXT,
            shop_url TEXT,
            logo_url TEXT,
            source_account_number TEXT,
            sp_public_key TEXT,
            sp_secret_key TEXT,
            magpie_public_key TEXT,
            magpie_secret_key TEXT,
            is_production BOOLEAN DEFAULT FALSE,
            redirect_url TEXT,
            webhook_url TEXT,
            role TEXT DEFAULT 'MERCHANT',
            created_at TIMESTAMPTZ DEFAULT NOW()
        )`);

        const cols = [
            'magpie_public_key TEXT', 'magpie_secret_key TEXT',
            'is_production BOOLEAN DEFAULT FALSE', 'redirect_url TEXT', 'webhook_url TEXT'
        ];
        for (const col of cols) {
            try {
                await pgPool.query(`ALTER TABLE users ADD COLUMN ${col}`);
            } catch (e) {}
        }

        await pgPool.query(`CREATE TABLE IF NOT EXISTS approvals (
            request_id TEXT PRIMARY KEY,
            email TEXT NOT NULL,
            device_id TEXT NOT NULL,
            device_name TEXT NOT NULL,
            status TEXT DEFAULT 'PENDING',
            created_at BIGINT,
            expires_at BIGINT
        )`);

        await pgPool.query(`CREATE TABLE IF NOT EXISTS deposits (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            user_email TEXT NOT NULL,
            amount DECIMAL(12,2) NOT NULL,
            reference_number TEXT,
            bank_name TEXT,
            status TEXT DEFAULT 'PENDING',
            created_at TIMESTAMPTZ DEFAULT NOW()
        )`);

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
        )`);

        await pgPool.query(`CREATE TABLE IF NOT EXISTS merchant_members (
            id TEXT PRIMARY KEY,
            merchant_id TEXT NOT NULL,
            name TEXT NOT NULL,
            email TEXT NOT NULL,
            role TEXT NOT NULL,
            permissions JSONB DEFAULT '{}'::jsonb,
            status TEXT DEFAULT 'ACTIVE',
            created_at TIMESTAMPTZ DEFAULT NOW()
        )`);

        await pgPool.query(`CREATE TABLE IF NOT EXISTS payment_events (
            id TEXT PRIMARY KEY,
            merchant_id TEXT NOT NULL,
            source TEXT NOT NULL,
            external_reference TEXT,
            status TEXT NOT NULL,
            amount DECIMAL(12,2) DEFAULT 0,
            payload JSONB DEFAULT '{}'::jsonb,
            created_at TIMESTAMPTZ DEFAULT NOW()
        )`);

        await pgPool.query(`CREATE TABLE IF NOT EXISTS disbursements (
            id TEXT PRIMARY KEY,
            merchant_id TEXT NOT NULL,
            amount DECIMAL(12,2) NOT NULL,
            account_number TEXT NOT NULL,
            bank_code TEXT NOT NULL,
            beneficiary_name TEXT NOT NULL,
            status TEXT DEFAULT 'PENDING',
            external_reference TEXT,
            created_at TIMESTAMPTZ DEFAULT NOW(),
            updated_at TIMESTAMPTZ DEFAULT NOW()
        )`);

        console.log('✅ Database Schema Verified & Connected to Route');
    } catch (e) {
        console.error('❌ DB CONNECTION ERROR:', e.message);
        throw e;
    }
}

module.exports = {
    pgPool,
    initDatabase,
    cleanKey
};
