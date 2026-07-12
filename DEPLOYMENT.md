# Deployment Status for SwiftPay

## 1. Backend Deployment (Render) - ✅ COMPLETED
The Node.js gateway is now live and "online".
- **URL**: `https://swiftpay-merchant-gateway.onrender.com`
- **Status**: `UP` (Verified)
- **Features**: Refactored with 18-field user schema and stability fixes.

### Backend Verification:
You can verify the gateway status by visiting:
`https://swiftpay-merchant-gateway.onrender.com/health`

## 2. Android App Deployment - ✅ CONFIGURED
The Android application has been fully refactored and configured to talk to the live Render backend.

### Environment Configuration:
- `APP_SERVER_URL` or `ANDROID_APP_SERVER_URL`: Set to the Render URL in `local.properties` for Android builds.
- `APP_SERVER_KEY` or `ANDROID_APP_SERVER_KEY`: Matching the secure key on the server.
- `SWIFTPAY_KEYS`: Configured for production-ready abstraction.

### Build and Install:
To generate the final APK for use:
```bash
./gradlew assembleDebug
```
The APK will be available at: `app/build/outputs/apk/debug/app-debug.apk`

## 3. Production Release Checklist
Before submitting to Google Play:
1. Ensure `local.properties` contains your production `RELEASE_STORE_FILE` details.
2. Run `./gradlew assembleRelease`.
3. Verify all 18 UI screens in the release build.
