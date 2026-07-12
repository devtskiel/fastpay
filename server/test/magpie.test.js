const test = require('node:test')
const assert = require('node:assert/strict')
const { buildMagpieChargePayload, normalizeMagpieChargeResponse, normalizeWalletBalanceResponse } = require('../lib/magpie')

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

test('normalizes production wallet balances for the app', () => {
  const normalized = normalizeWalletBalanceResponse({
    balance: 1250.5,
    availableBalance: 1250.5,
    currency: 'PHP'
  })

  assert.equal(normalized.balance, 1250.5)
  assert.equal(normalized.availableBalance, 1250.5)
  assert.equal(normalized.currency, 'PHP')
})

test('preserves production payment links from magpie responses', () => {
  const normalized = normalizeMagpieChargeResponse({
    paymentLinkUrl: 'https://pay.example.test/checkout/123',
    status: 'PENDING'
  })

  assert.equal(normalized.paymentLinkUrl, 'https://pay.example.test/checkout/123')
  assert.equal(normalized.status, 'PENDING')
})
