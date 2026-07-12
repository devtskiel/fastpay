const test = require('node:test')
const assert = require('node:assert/strict')
const { buildMagpieChargePayload } = require('../lib/magpie')

test('builds a payment-method payload for production QR requests', () => {
  const payload = buildMagpieChargePayload({
    amount: 25.5,
    description: 'Alipay QR',
    referenceNo: 'ALIPAY_001',
    paymentMethod: 'alipay'
  })

  assert.equal(payload.amount, 2550)
  assert.equal(payload.currency, 'php')
  assert.equal(payload.payment_method, 'alipay')
  assert.equal(payload.paymentMethod, 'alipay')
  assert.equal(payload.source, undefined)
})

test('includes a source token for card-based charges when provided', () => {
  const payload = buildMagpieChargePayload({
    amount: 10,
    description: 'Card payment',
    referenceNo: 'CARD_001',
    sourceToken: 'tok_123'
  })

  assert.equal(payload.source, 'tok_123')
  assert.equal(payload.capture, true)
})
