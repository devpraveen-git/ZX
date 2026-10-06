# Proguard / R8 rules for CallScribe

# Keep all app classes and do not obfuscate names
-dontobfuscate
-dontoptimize
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep all CallScribe code
-keep class com.example.callscribe.** { *; }

# Keep Android components
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep Hilt and Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.** { *; }
-keep class * implements dagger.** { *; }
-dontwarn dagger.**
-dontwarn com.google.errorprone.annotations.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Media3 & Crypto
-keep class androidx.media3.** { *; }
-keep class androidx.security.crypto.** { *; }
