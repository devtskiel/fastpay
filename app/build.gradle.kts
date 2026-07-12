import java.util.Properties
import org.gradle.api.GradleException

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

fun secretProperty(name: String, defaultValue: String = "MISSING_KEY"): String {
    return providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: localProps.getProperty(name)
        ?: defaultValue
}

fun optionalProperty(name: String): String {
    return providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: localProps.getProperty(name)
        ?: ""
}

// Read a couple of properties at configuration time so we can enforce them for release builds
val APP_SERVER_URL_PROP: String = providers.gradleProperty("APP_SERVER_URL").orNull
    ?: providers.environmentVariable("APP_SERVER_URL").orNull
    ?: localProps.getProperty("APP_SERVER_URL")
    ?: ""
val APP_SERVER_KEY_PROP: String = providers.gradleProperty("APP_SERVER_KEY").orNull
    ?: providers.environmentVariable("APP_SERVER_KEY").orNull
    ?: localProps.getProperty("APP_SERVER_KEY")
    ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val secretKey = secretProperty("SWIFTPAY_SECRET_KEY")
        val publicKey = secretProperty("SWIFTPAY_PUBLIC_KEY")
        val terminalId = optionalProperty("SWIFTPAY_TERMINAL_ID")
        val qrMid = optionalProperty("SWIFTPAY_QR_MID")
        val cardMid = optionalProperty("SWIFTPAY_CARD_MID")
        val tmkId = optionalProperty("SWIFTPAY_TMK_ID")
        val resendKey = secretProperty("RESEND_API_KEY")
        val otpSender = optionalProperty("OTP_SENDER_EMAIL").takeUnless { it.isBlank() } ?: "onboarding@resend.dev"
        val vaultSuccess = optionalProperty("VAULT_SUCCESS_REDIRECT_URL").takeUnless { it.isBlank() } ?: "https://api.netbank.ph/success"
        val vaultFailure = optionalProperty("VAULT_FAILURE_REDIRECT_URL").takeUnless { it.isBlank() } ?: "https://api.netbank.ph/failure"
        val vaultCancel = optionalProperty("VAULT_CANCEL_REDIRECT_URL").takeUnless { it.isBlank() } ?: "https://api.netbank.ph/cancel"
        val appServerKey = optionalProperty("APP_SERVER_KEY")
        val magpiePubKey = optionalProperty("MAGPIE_PUBLIC_KEY")
        val magpieSecKey = optionalProperty("MAGPIE_SECRET_KEY")

        buildConfigField("String", "SWIFTPAY_SECRET_KEY", "\"$secretKey\"")
        buildConfigField("String", "SWIFTPAY_PUBLIC_KEY", "\"$publicKey\"")
        buildConfigField("String", "SWIFTPAY_TERMINAL_ID", "\"$terminalId\"")
        buildConfigField("String", "SWIFTPAY_QR_MID", "\"$qrMid\"")
        buildConfigField("String", "SWIFTPAY_CARD_MID", "\"$cardMid\"")
        buildConfigField("String", "SWIFTPAY_TMK_ID", "\"$tmkId\"")
        buildConfigField("String", "RESEND_API_KEY", "\"$resendKey\"")
        buildConfigField("String", "OTP_SENDER_EMAIL", "\"$otpSender\"")
        buildConfigField("String", "VAULT_SUCCESS_REDIRECT_URL", "\"$vaultSuccess\"")
        buildConfigField("String", "VAULT_FAILURE_REDIRECT_URL", "\"$vaultFailure\"")
        buildConfigField("String", "VAULT_CANCEL_REDIRECT_URL", "\"$vaultCancel\"")
        buildConfigField("String", "APP_SERVER_URL", "\"${optionalProperty("APP_SERVER_URL")}\"")
        buildConfigField("String", "APP_SERVER_KEY", "\"${appServerKey}\"")
        buildConfigField("String", "MAGPIE_PUBLIC_KEY", "\"$magpiePubKey\"")
        buildConfigField("String", "MAGPIE_SECRET_KEY", "\"$magpieSecKey\"")
    }

    signingConfigs {
        create("release") {
            storeFile = localProps.getProperty("RELEASE_STORE_FILE")?.let { file(it) }
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Enforce presence of critical server config only when release artifacts are actually being built
            val isBuildingRelease = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
            if (isBuildingRelease) {
                if (APP_SERVER_URL_PROP.isBlank()) {
                    throw GradleException("APP_SERVER_URL must be set in local.properties, environment or gradle properties for release builds")
                }
                if (APP_SERVER_KEY_PROP.isBlank()) {
                    throw GradleException("APP_SERVER_KEY must be set in local.properties, environment or gradle properties for release builds")
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.compose.adaptive.layout)
    implementation(libs.androidx.compose.adaptive.navigation)
    implementation(libs.androidx.compose.adaptive.navigation.suite)
    implementation(libs.androidx.compose.adaptive.navigation3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    implementation(libs.converter.moshi)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logging.interceptor)
    implementation(libs.material)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.play.services.location)
    implementation(libs.retrofit)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
}
