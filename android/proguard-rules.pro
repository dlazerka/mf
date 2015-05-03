# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/dl/opt/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


#Project specifics

-target 1.7

-keepattributes SourceFile,LineNumberTable

# Otherwise DEX fails, taken from http://stackoverflow.com/questions/5701126
-optimizations !code/allocation/variable

-dontobfuscate # does only 100kB reduction
#-dump ./build/proguard_class_files.txt
-printseeds ./build/proguard_seeds.txt
-printusage ./build/proguard_unused.txt
-printmapping ./build/proguard_mapping.txt

-dontwarn org.joda.convert.**
-dontwarn sun.misc.Unsafe
