# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/tomas/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add project specific keep rules here:

# SnakeYAML and address-formatter - fix for NullPointerException in Package.getName()
-keep class org.yaml.snakeyaml.** { *; }
-keepnames class org.yaml.snakeyaml.** { *; }
-keeppackagenames org.yaml.snakeyaml.**
-keep class org.microg.addressformatter.** { *; }
-keepnames class org.microg.addressformatter.** { *; }
-keeppackagenames org.microg.addressformatter.**

# Retain attributes required for reflection and package discovery
-keepattributes SourceFile,LineNumberTable,EnclosingMethod,InnerClasses,Signature,RuntimeVisibleAnnotations,AnnotationDefault,Package

# Keep packages for libraries that might use reflection for package info (like android-async-http)
-keeppackagenames com.loopj.android.http.**
-keeppackagenames org.osmdroid.**

-dontwarn org.conscrypt.**
-dontwarn org.osmdroid.**

-keep class org.osmdroid.** { *; }
-keep interface org.osmdroid.** { *; }


# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# more convenient stack trace analysis.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-keepattributes SourceFile

# Fix for "Missing classes detected while running R8"
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

# Keep all fragments in settings package as they are instantiated via reflection
-keep class org.thosp.yourlocalweather.settings.fragments.** { *; }

# Keep all fragments used in the app
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.preference.PreferenceFragmentCompat

# MPAndroidChart rules (repackaged/forked as org.thosp.charting)
-keep class org.thosp.charting.** { *; }
-dontwarn org.thosp.charting.**

# Glide rules
-keep public class * extends com.github.bumptech.glide.module.AppGlideModule
-keep public class * extends com.github.bumptech.glide.module.LibraryGlideModule
-keep class com.github.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep public enum com.github.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# AmbilWarna Color Picker
-keep class yuku.ambilwarna.** { *; }

# android-file-chooser
-keep class com.github.hedzr.** { *; }

# android-async-http
-keep class com.loopj.android.http.** { *; }
-keeppackagenames com.loopj.android.http.**
