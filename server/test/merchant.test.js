const test = require('node:test')
const assert = require('node:assert/strict')
const { computeHmacSha256, verifyHmacSignature, normalizeTransactionStatus } = require('../lib/merchant')

test('verifies webhook signatures with HMAC-SHA256', () => {
  const payload = JSON.stringify({ event: 'payment.updated', id: 'pay_123' })
  const secret = 'demo-secret'
  const signature = computeHmacSha256(payload, secret)

  assert.equal(verifyHmacSignature(payload, signature, secret), true)
  assert.equal(verifyHmacSignature(payload, 'bad-signature', secret), false)
})

test('normalizes transaction statuses to the supported set', () => {
  assert.equal(normalizeTransactionStatus('EXECUTED'), 'EXECUTED')
  assert.equal(normalizeTransactionStatus('paid'), 'EXECUTED')
  assert.equal(normalizeTransactionStatus('expired'), 'EXPIRED')
})
