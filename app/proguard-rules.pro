# Project specific ProGuard rules

# General Android rules are already included via getDefaultProguardFile
# Ensure our API models are preserved for JSON serialization
-keep class com.example.myapplication.data.api.** { *; }
-keep class com.example.myapplication.data.model.** { *; }

# Preserve signatures and annotations for serialization libraries
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Security: Remove all Log calls in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Security: Prevent reverse engineering of critical business logic
-keepnames class com.example.myapplication.util.SwiftPaySignatureHelper { *; }

# Retrofit/OkHttp specific security rules
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Security: Obfuscate everything else heavily
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
