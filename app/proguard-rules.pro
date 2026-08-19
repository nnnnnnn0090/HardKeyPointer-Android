# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep accessibility service
-keep class com.nnnnnnn0090.hardkeypointer.TapService { *; }

# Keep main activity
-keep class com.nnnnnnn0090.hardkeypointer.MainActivity { *; }

# Keep accessibility utils
-keep class com.nnnnnnn0090.hardkeypointer.AccessibilityUtils { *; }

# Keep license utils
-keep class com.nnnnnnn0090.hardkeypointer.LicenseUtils { *; }

# Keep Kotlin metadata
-keepattributes Annotation,Signature,InnerClasses,EnclosingMethod

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep callback methods
-keepclassmembers class * {
    void *(**On*Event);
}

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
