# Keep GeckoView classes and JNI/native entry points intact.
-keep class org.mozilla.geckoview.** { *; }
-keep interface org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**
-keepattributes SourceFile,LineNumberTable
