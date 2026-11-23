# Keep GeckoView classes and JNI/native entry points intact.
-keep class org.mozilla.** { *; }
-keep interface org.mozilla.** { *; }
-dontwarn org.mozilla.**

# Keep Soul Browser application classes, Compose entry points, and data models
-keep class com.soulbrowser.app.** { *; }
-keep interface com.soulbrowser.app.** { *; }

# Preserve coroutine service-provider names and debug-relevant volatile state.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

-keepattributes SourceFile,LineNumberTable,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod,Signature
