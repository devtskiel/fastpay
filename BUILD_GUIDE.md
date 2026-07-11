# FastPay App - Build & Deployment Guide

## 📊 Project Status

The FastPay application is **fully feature-complete** and **production-ready** from a code perspective. All core features have been implemented and verified with zero compilation errors.

### ✅ Completed Features

#### Authentication & Session Management
- Email/password login with OTP verification
- Multi-device session management (30-day expiry)
- Automatic session restoration on app launch
- Forgot password flow with backend integration
- Session token clearing on logout

#### Admin & Member Management
- Team member invitation and management
- Role-based access control (SUPER_ADMIN, ADMIN, DEVELOPER, MEMBER)
- Member add/delete with persistent storage
- Permissions matrix by role

#### Payment Processing
- SwiftPay payment gateway integration
- Payout/disbursement processing
- Transaction tracking with status updates
- Multi-currency support
- Virtual Card Account (VCA) management

#### Transactions & Analytics
- Transaction filtering by date, status, type
- CSV export for transaction data
- Real-time balance updates
- Settlement ledger display
- Audit logging of all operations

#### Admin Dashboard
- Deposit approval workflow
- Admin deposits with SUBMITTED/APPROVED/REJECTED status
- Webhook management and testing
- API keys configuration
- Invoice management
- Audit logs for compliance

#### User Features
- Cash-in deposits with tracking
- Wallet management
- Settlement tracking
- Profile management
- Settings and preferences

## 🏗️ Architecture

**Stack:**
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: DataStore (preference-based storage)
- **Networking**: Retrofit with Kotlinx Serialization
- **Async**: Coroutines & Flow
- **Build System**: Gradle with Kotlin DSL

**Project Structure:**
```
app/
├── src/main/java/
│   └── com/example/myapplication/
│       ├── MainActivity.kt          # App entry point & navigation
│       ├── domain/usecase/          # Business logic
│       ├── data/                    # Data layer
│       │   ├── SwiftPayService.kt  # API integration
│       │   ├── SettingsManager.kt  # Preferences
│       │   └── SessionManager.kt   # Session handling
│       ├── ui/screens/             # UI screens (25+ screens)
│       ├── navigation/             # Route definitions
│       └── di/                     # Dependency injection
└── build.gradle.kts                # Build configuration
```

## 🔨 Build Setup

### Prerequisites

1. **Java 17+**
   ```bash
   java -version  # Should show Java 17 or higher
   ```

2. **Android SDK**
   - Download from: https://developer.android.com/studio
   - Or use Android Studio to install automatically

3. **Set Environment Variables**
   ```bash
   export ANDROID_HOME=/path/to/android/sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
   ```

### Build Commands

**Using the provided build script (recommended):**
```bash
# Debug build (for testing)
./build.sh debug

# Release build (for production)
./build.sh release

# Release bundle (for Google Play Store)
./build.sh bundle
```

**Using Gradle directly:**
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# Release Bundle
./gradlew bundleRelease
```

### Build Outputs

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Release Bundle**: `app/build/outputs/bundle/release/app-release.aab`

## 📦 CI/CD with GitHub Actions

The repository includes automated build workflows (`.github/workflows/build.yml`):

**Triggered on:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`

**Outputs:**
- Debug APK artifact
- Release Bundle artifact
- Build summary

**Access builds:**
1. Go to repository → Actions tab
2. Select latest workflow run
3. Download artifacts

## 🚀 Deployment Steps

### Local Testing

1. **Build Debug APK:**
   ```bash
   ./build.sh debug
   ```

2. **Install on Device/Emulator:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Run the App:**
   - Launch from home screen or:
   ```bash
   adb shell am start -n com.example.myapplication/.MainActivity
   ```

### Production Deployment (Google Play)

1. **Build Release Bundle:**
   ```bash
   ./build.sh bundle
   ```

2. **Sign the Bundle** (if using local signing):
   ```bash
   jarsigner -keystore my-release-key.jks \
     app/build/outputs/bundle/release/app-release-unsigned.aab \
     alias_name
   ```

3. **Upload to Google Play Console:**
   - Sign in to [Google Play Console](https://play.google.com/console)
   - Create/select app
   - Go to Release → Production
   - Upload `app-release.aab`
   - Follow review and deployment process

### Manual Installation

```bash
# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Install multiple APKs from bundle (requires bundletool)
bundletool install-apks \
  --apks=app/build/outputs/bundle/release/app.apks \
  --adb=/path/to/adb
```

## 🐳 Docker Build (Alternative)

For environments without Android SDK:

```bash
# Build Docker image with Android SDK
docker build -f Dockerfile.build -t fastpay-builder .

# Build APK inside container
docker run --rm -v $(pwd):/app fastpay-builder \
  bash -c "cd /app && ./gradlew assembleRelease"

# APK available at: app/build/outputs/apk/release/app-release.apk
```

## 📋 Environment Configuration

### API Configuration

Set in `app/build.gradle.kts` or `gradle.properties`:

```properties
# For debug builds
ANDROID_APP_SERVER_URL=https://api-dev.fastpay.com
ANDROID_APP_SERVER_KEY=your_dev_key

# For release builds
ANDROID_APP_SERVER_URL=https://api.fastpay.com
ANDROID_APP_SERVER_KEY=your_production_key
```

### SwiftPay Integration

Configure in the app settings or via `.env`:
```properties
SWIFTPAY_PUBLIC_KEY=your_public_key
SWIFTPAY_SECRET_KEY=your_secret_key
SWIFTPAY_ENVIRONMENT=PRODUCTION  # or SANDBOX
```

## 🐛 Troubleshooting

### Build Fails with "SDK location not found"

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./build.sh debug
```

### Out of Memory During Build

```bash
export GRADLE_OPTS="-Xmx4096m -XX:MaxPermSize=1024m"
./build.sh release
```

### Gradle Daemon Issues

```bash
./gradlew --stop
./gradlew clean build
```

### Certificate/Signature Issues

```bash
# Clear all build cache
./gradlew clean
rm -rf ~/.gradle/caches/
./build.sh release
```

## 📱 Device Requirements

- **Minimum SDK**: Android 10 (API 29)
- **Target SDK**: Android 14 (API 34)
- **RAM**: 2GB minimum, 4GB recommended
- **Storage**: 50MB minimum

## 🔒 Security Notes

- All API keys should be stored securely (not in source code)
- Use environment variables for sensitive data
- Implement proper SSL/TLS certificate pinning
- Sanitize all user inputs
- Never commit `.keystore` files or signing keys

## 📞 Support & Issues

For build-related issues:
1. Check `BUILD.md` for detailed instructions
2. Review GitHub Actions logs (Actions tab)
3. Check Gradle cache: `./gradlew --info`
4. Update Gradle: `./gradlew wrapper --gradle-version=latest`

## 📄 Additional Resources

- [Android Developer Docs](https://developer.android.com/docs)
- [Gradle Documentation](https://gradle.org/documentation)
- [Kotlin Documentation](https://kotlinlang.org/docs)
- [Jetpack Compose Guide](https://developer.android.com/jetpack/compose)

---

**Last Updated**: 2026-07-11  
**Gradle Version**: 8.x  
**Kotlin Version**: 1.9.x  
**Target SDK**: 34  
**Minimum SDK**: 29
