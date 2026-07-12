const buildMagpieChargePayload = ({
    amount,
    description,
    referenceNo,
    paymentMethod = 'qrph',
    sourceToken,
    capture = true,
    metadata = {}
}) => {
    const normalizedAmount = Number(amount || 0)
    const cents = Math.round(normalizedAmount * 100)
    const payload = {
        amount: cents,
        currency: 'php',
        description: description || 'SwiftPay Payment',
        reference_no: referenceNo || `MAGPIE_${Date.now()}`,
        metadata: {
            source: 'swiftpay_app',
            ...metadata
        },
        capture,
        payment_method: paymentMethod,
        paymentMethod
    }

    if (sourceToken) {
        payload.source = sourceToken
    }

    return payload
}

const normalizeMagpieChargeResponse = (response = {}, { paymentMethod = 'qrph' } = {}) => {
    const raw = response || {}
    const paymentLink = raw.paymentLinkUrl || raw.payment_link_url || raw.checkout_url || raw.url || raw.paymentUrl || raw.payment_url || null
    const qrCode = raw.qrCode || raw.qr_code || raw.qrcode || raw.qrCodeBody || raw.qr_code_body || null
    const qrCodeBody = qrCode || raw.qrCodeBody || raw.qr_code_body || null

    return {
        id: raw.id || raw.paymentId || raw.chargeId || null,
        paymentId: raw.paymentId || raw.id || raw.chargeId || null,
        paymentLinkUrl: paymentLink,
        shortCode: raw.shortCode || raw.short_code || null,
        status: raw.status || raw.state || 'PENDING',
        description: raw.description || raw.memo || null,
        totalAmount: raw.totalAmount || raw.amount || null,
        qrCode,
        qrCodeBody,
        redirectUrl: raw.redirectUrl || raw.redirect_url || null,
        metadata: raw.metadata || {},
        paymentMethod
    }
}

module.exports = {
    buildMagpieChargePayload,
    normalizeMagpieChargeResponse
}
