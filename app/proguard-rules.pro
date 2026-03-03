# This file is automatically created by Android Studio.
# Specify only the obfuscation level you're interested in.
# By default, Android Studio shows the non-obfuscated class names.
#
# Enabling obfuscation would make the stack trace less readable,
# but it makes reverse engineering your APK much harder.

-keep public class com.agrigo.** { *; }
-keepclassmembers class com.agrigo.** {*;}

# Preserve line numbers
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Firebase
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }

# Preserve Retrofit
-keepattributes Signature
-keep class retrofit2.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Preserve Gson
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# Preserve Material Design
-keep class com.google.android.material.** { *; }
-keep interface com.google.android.material.** { *; }
