# Payment Link Redirect Helper

Tiny Express server that receives SwiftPay payment redirects (web) and forwards them to your app deep link (fastpay-app://...). Useful when you want SwiftPay to redirect to a web URL that then opens the app, avoiding broken flows when the app is not installed.

Usage

1. Install dependencies:

```powershell
cd payment_link_redirect
npm install
```

2. Start server:

```powershell
npm start
```

3. Configure your SwiftPay payment link `redirectUrl.success` to point to:

```
https://your-domain.example/return
```

The helper will forward to `fastpay-app://payment/success?linkId=...&status=...` which your Android app should handle via an intent-filter.
