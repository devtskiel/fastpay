const crypto = require('crypto')

function computeHmacSha256(payload, secret) {
  return crypto.createHmac('sha256', secret).update(payload).digest('hex')
}

function verifyHmacSignature(payload, providedSignature, secret) {
  if (!providedSignature || !secret) return false

  const normalized = providedSignature.replace(/^sha256=/i, '').trim()
  const expected = computeHmacSha256(payload, secret)

  if (normalized.length !== expected.length) return false

  try {
    return crypto.timingSafeEqual(Buffer.from(normalized), Buffer.from(expected))
  } catch (_error) {
    return false
  }
}

function normalizeTransactionStatus(status) {
  const normalized = String(status || '').trim().toLowerCase()

  switch (normalized) {
    case 'executed':
    case 'paid':
    case 'success':
    case 'successful':
      return 'EXECUTED'
    case 'canceled':
    case 'cancelled':
    case 'void':
      return 'CANCELED'
    case 'rejected':
    case 'failed':
    case 'declined':
      return 'REJECTED'
    case 'expired':
    case 'timeout':
      return 'EXPIRED'
    default:
      return 'PENDING'
  }
}

module.exports = {
  computeHmacSha256,
  verifyHmacSignature,
  normalizeTransactionStatus
}
