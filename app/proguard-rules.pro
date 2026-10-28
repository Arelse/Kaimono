-dontobfuscate

-keep,allowoptimization class eu.kanade.**
-keep,allowoptimization class tachiyomi.**
-keep,allowoptimization class mihon.**

# Extensions load against the source API at runtime. R8 must not remove, rename or change the access
# of anything in it: "allowoptimization" lets R8 make methods final, which breaks extensions that
# override them (LinkageError "overrides final method"), and removing unused members causes
# NoSuchMethodError. Plain -keep, with every member kept as written.
-keep class eu.kanade.tachiyomi.animesource.** { *; }
-keep class eu.kanade.tachiyomi.source.** { *; }

# Keep common dependencies used in extensions
-keep,allowoptimization class androidx.preference.** { public protected *; }
-keep,allowoptimization class kotlin.** { public protected *; }
-keep,allowoptimization class kotlinx.coroutines.** { public protected *; }
-keep,allowoptimization class kotlinx.serialization.** { public protected *; }
-keep,allowoptimization class kotlin.time.** { public protected *; }
-keep,allowoptimization class okhttp3.** { public protected *; }
-keep,allowoptimization class okio.** { public protected *; }
-keep,allowoptimization class com.squareup.zstd.** { public protected *; }
-keep,allowoptimization class org.jsoup.** { public protected *; }
-keep,allowoptimization class rx.** { public protected *; }
-keep,allowoptimization class com.dokar.quickjs.** { public protected *; }
-keep class com.dokar.quickjs.MemoryUsage { *; }
-keep,allowoptimization class app.cash.quickjs.** { public protected *; }
-keepclassmembers class kotlin.UByteArray { <init>(...); }
-keep,allowoptimization class uy.kohesive.injekt.** { public protected *; }
-keep,allowoptimization class com.squareup.zstd.** { public protected *; }

# From extensions-lib
# Keep every member as written. "public protected *" skips the synthetic default-argument methods
# (e.g. RequestsKt.get$default), so extensions calling GET(...) with defaults got NoSuchMethodError.
-keep class eu.kanade.tachiyomi.network.** { *; }
-keep class eu.kanade.tachiyomi.AppInfo { *; }

-keepclassmembers class * implements java.io.Serializable {
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

##---------------Begin: proguard configuration for RxJava 1.x  ----------
-dontwarn sun.misc.**

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

-dontnote rx.internal.util.PlatformDependent
##---------------End: proguard configuration for RxJava 1.x  ----------

##---------------Begin: proguard configuration for okhttp  ----------
-keepclasseswithmembers class okhttp3.MultipartBody$Builder { *; }
##---------------End: proguard configuration for okhttp  ----------

##---------------Begin: proguard configuration for kotlinx.serialization  ----------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.** # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class eu.kanade.**$$serializer { *; }
-keepclassmembers class eu.kanade.** {
    *** Companion;
}
-keepclasseswithmembers class eu.kanade.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.** {
    <methods>;
}
##---------------End: proguard configuration for kotlinx.serialization  ----------

# XmlUtil
-keep public enum nl.adaptivity.xmlutil.EventType { *; }

# Firebase
-keep class com.google.firebase.installations.** { *; }
-keep interface com.google.firebase.installations.** { *; }

# WebView JavaScript bridge safety
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class eu.kanade.tachiyomi.ui.reader.viewer.text.webview.NovelWebViewViewer$WebViewInterface { *; }
-keep class eu.kanade.tachiyomi.ui.customsource.ElementSelectorJSInterface { *; }

# KotlinX Datetime
-keep,allowoptimization class kotlinx.datetime.** { public protected *; }

# Methods called by Shizuku only
-keepclassmembers class mihon.app.shizuku.ShellInterface {
    public <init>();
    public void destroy();
}

# mpv's native code looks up these classes and methods by name, so R8 must not rename or strip them
-keep class dev.jdtech.mpv.** { *; }

# Keep Compose Theme from being stripped/mangled by R8
-keep class eu.kanade.presentation.theme.TachiyomiThemeKt { *; }
-keep class eu.kanade.presentation.theme.** { *; }

