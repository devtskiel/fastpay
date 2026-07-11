# FastPay APK Build Instructions

## Quick Start

### Prerequisites
- Java 17 or higher
- Android SDK (API 34)
- Android Build Tools (34.0.0 or higher)

## Build Locally

### Setup Android SDK

1. **Install Android Studio** (recommended):
   ```bash
   # macOS
   brew install android-studio

   # Linux: Download from https://developer.android.com/studio
   ```

2. **Set ANDROID_HOME**:
   ```bash
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
   ```

3. **Install SDK Components**:
   ```bash
   sdkmanager --sdk_root=$ANDROID_HOME "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```

### Build APK

**Debug APK** (for testing):
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

**Release APK** (for production):
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

**Release Bundle** (for Google Play):
```bash
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

## Docker Build

### Build Using Docker

```bash
# Build the Docker image
docker build -f Dockerfile.build -t fastpay-builder .

# Run the build
docker run --rm -v $(pwd):/app fastpay-builder bash -c "cd /app && ./gradlew assembleRelease"

# Extract APK from build directory
ls -lah app/build/outputs/apk/release/
```

### Docker Compose (Optional)

```bash
docker compose -f server/docker-compose.yml up -d
```

## GitHub Actions CI/CD

The repository includes a GitHub Actions workflow (`.github/workflows/build.yml`) that automatically:
- Builds on every push to `main` or `develop`
- Runs on pull requests
- Uploads artifacts for download
- Creates build summaries

### View Workflow Results

1. Go to `Actions` tab on GitHub
2. Select the latest workflow run
3. Download artifacts:
   - `app-debug.apk` - Debug build for testing
   - `app-release.aab` - Release bundle for Play Store

## Signing Release APK

For production releases, sign your APK:

```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore my-release-key.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  alias_name

zipalign -v 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/app-release.apk
```

## Troubleshooting

### "SDK location not found" Error

```bash
# Create local.properties in project root
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### Out of Memory Error

```bash
# Increase Gradle heap size
export GRADLE_OPTS="-Xmx4096m"
./gradlew assembleRelease
```

### Certificate Issues

Clear the build cache:
```bash
./gradlew clean
./gradlew assembleRelease
```

## Build Artifacts Location

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **Release Bundle**: `app/build/outputs/bundle/release/app-release.aab`
- **Build Reports**: `app/build/reports/`

## Next Steps

1. **Install on Device**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Submit to Google Play Store**:
   - Use `app-release.aab` from `bundleRelease` task
   - Follow Google Play Console upload instructions

3. **Create Release**:
   - Tag version: `git tag -a v1.0.0 -m "Version 1.0.0"`
   - Push tags: `git push origin v1.0.0`

## Build Configuration

See `build.gradle.kts` and `gradle.properties` for configuration options.

Key environment variables:
- `ANDROID_HOME`: Path to Android SDK
- `JAVA_HOME`: Path to Java 17 installation
- `GRADLE_OPTS`: Gradle JVM options (e.g., `-Xmx4096m`)
