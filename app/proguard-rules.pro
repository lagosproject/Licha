# ==============================================================================
# Licha — ProGuard / R8 Rules
# ==============================================================================
# Hilt 2.x, Compose BOM, OkHttp 4.x, and security-crypto all ship their own
# consumer ProGuard rules inside their AARs. The rules below cover project-
# specific concerns and extra safety for this dependency set.

# ------------------------------------------------------------------------------
# 1. Strip all Android Log.v / Log.d / Log.i calls in release builds
#    (defense-in-depth — ensures no credential leaks even if new Log.d calls
#    are added during development and not caught in review)
# ------------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ------------------------------------------------------------------------------
# 2. OkHttp / Okio — extra safety for reflection-based internals
# ------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.internal.http2.** { *; }

# ------------------------------------------------------------------------------
# 3. Kotlin — preserve metadata for sealed interfaces and data classes
# ------------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlin.jvm.JvmField <fields>;
}

# ------------------------------------------------------------------------------
# 4. Kotlin Coroutines
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 5. EncryptedSharedPreferences / Tink (via security-crypto:1.0.0)
#    security-crypto bundles its own consumer rules; no extra rules needed here.
#    If you upgrade to security-crypto:1.1.0-alpha, add:
#    -keep class com.google.crypto.tink.** { *; }
# ------------------------------------------------------------------------------

# ------------------------------------------------------------------------------
# 6. Hilt — ships its own consumer rules via hilt-android AAR.
#    If you encounter "cannot find class" errors at runtime after enabling
#    minification, add a targeted keep rule here, e.g.:
#    -keep class com.lakescorp.twitchchattts.ChatViewModel { *; }
# ------------------------------------------------------------------------------
