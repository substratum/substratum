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
-keep class .R
-keep class **.R$* {
    <fields>;
}

# Material sheet FAB
-keep class io.codetail.animation.arcanimator.** { *; }

# AVLoadingIndicatorView
-keep class com.wang.avi.** { *; }
-keep class com.wang.avi.indicators.** { *; }

# OM Library
-dontwarn android.os.ServiceManager

# APK Signer
-dontwarn sun.security.**
-dontwarn javax.naming.**
-dontwarn org.slf4j.impl.**
-dontwarn junit.textui.TestRunner

# Android support libraries
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }
-keep class android.support.v7.widget.RoundRectDrawable { *; }
-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }