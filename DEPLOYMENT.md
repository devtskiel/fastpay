# Deployment Guide for SwiftPay

This project is configured for production deployment on **Render** and **Railway**.

## 1. Backend Deployment (Node.js Gateway)

The `server/` directory contains a Node.js gateway that handles:
- Multi-device login approvals.
- Proxying sensitive SwiftPay operations.
- Merchant settings and profile management.

### Option A: Railway (Recommended)
1. Connect this repository to your **Railway.app** account.
2. In the Railway Dashboard, go to your Service settings and set the **Root Directory** to `server`.
3. Railway will then detect the `server/railway.json` file and use the `server/Dockerfile`.
4. Add a **PostgreSQL** service to your Railway project.
4. **Environment Variables**: Railway will automatically link `DATABASE_URL`. You MUST manually set:
   - `APP_SERVER_KEY`: A secure random string.
   - `JWT_SECRET`: A secure random string.
   - `SWIFTPAY_PUBLIC_KEY`: Your SwiftPay Public Key.
   - `SWIFTPAY_SECRET_KEY`: Your SwiftPay Secret Key.

### Option B: Render
1. Connect this repository to your **Render.com** account.
2. Render will automatically detect the `render.yaml` file.
3. It will create:
   - A Web Service (`swiftpay-merchant-gateway`)
   - A PostgreSQL Database (`swiftpay-db`)
4. **Environment Variables**: In the Render UI, you MUST set the following for the `swiftpay-merchant-gateway` service:
   - `APP_SERVER_KEY`: A secure random string used as an API key for the app.
   - `JWT_SECRET`: A secure random string for signing login tokens.
   - `SWIFTPAY_PUBLIC_KEY`: Your SwiftPay Public Key.
   - `SWIFTPAY_SECRET_KEY`: Your SwiftPay Secret Key.

## 2. Android App Production Readiness

### Configuration:
1. Open `local.properties` (or set environment variables in your CI/CD).
2. Update `APP_SERVER_URL` to your Render service URL (e.g., `https://swiftpay-merchant-gateway.onrender.com`).
3. Update `APP_SERVER_KEY` to match the one set in Render.

### Security:
- ProGuard is enabled for release builds to obfuscate code.
- Sensitive keys are excluded from the binary when using the Gateway (in progress).

## 3. Production Build
To generate a production APK:
```bash
./gradlew assembleRelease
```
The release build is configured to use the signing credentials defined in `local.properties`.
