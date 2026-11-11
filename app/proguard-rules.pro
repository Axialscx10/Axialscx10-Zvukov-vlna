# Add project specific ProGuard / R8 rules here.
# Keep source file names and line numbers for readable crash stack traces in Google Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Android entry points (Activity, Service, BroadcastReceiver / AppWidgetProvider)
-keep public class com.example.MainActivity { *; }
-keep public class com.example.playback.PlaybackService { *; }
-keep public class com.example.widget.SoundWaveWidgetProvider { *; }

# Keep Media3 / ExoPlayer classes accessed via reflection
-dontwarn androidx.media3.**
-keepclassmembers class androidx.media3.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
