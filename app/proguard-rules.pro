# Lambdas
-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*

# Keep tabs class name
-keepnames class projekt.substratum.tabs.*

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Welcome-android
-keepclassmembers class * extends com.stephentuso.welcome.WelcomeActivity {
    public static java.lang.String welcomeKey();
}
-keep class com.stephentuso.welcome.** { *; }

# About libraries
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# Material sheet FAB
-keep class io.codetail.animation.arcanimator.** { *; }

# AVLoadingIndicatorView
-keep class com.wang.avi.** { *; }
-keep class com.wang.avi.indicators.** { *; }

# OM Library
-keep class android.content.** { *; }
-keep class com.android.server.theme.** { *; }
-dontwarn android.os.**
-dontwarn android.content.**
-dontwarn com.android.server.theme.**

# APK Signer
-dontwarn sun.security.**
-dontwarn javax.naming.**
-dontwarn org.slf4j.impl.**
-dontwarn junit.textui.TestRunner


# SLF4J
-keep class org.slf4j.** { *; }

# Don't obfuscate anything
-dontobfuscate

# Warnings are nice but please don't abort build over things I don't control
-ignorewarnings
