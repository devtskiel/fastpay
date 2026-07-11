const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');

const JWT_SECRET = process.env.JWT_SECRET || 'swiftpay-enterprise-core-2024';
const APP_SERVER_KEY = process.env.APP_SERVER_KEY || 'my-secret-key';

const requireAuth = (req, res, next) => {
    const h = req.headers.authorization;
    if (!h?.startsWith('Bearer ')) return res.status(401).json({ error: 'Unauthorized' });
    try {
        req.user = jwt.verify(h.split(' ')[1], JWT_SECRET);
        next();
    } catch (e) { res.status(401).json({ error: 'Expired' }); }
};

const requireApiKey = (req, res, next) => {
    const key = req.headers['x-api-key'];
    if (key !== APP_SERVER_KEY) return res.status(401).json({ error: 'Forbidden' });
    next();
};

module.exports = {
    JWT_SECRET,
    APP_SERVER_KEY,
    requireAuth,
    requireApiKey
};
