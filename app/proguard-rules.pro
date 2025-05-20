# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Общие правила для Android
-keepattributes *Annotation*

# Сохраняем аннотации и поля, нужные Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.solovinyykray.solovinyykray.models.** { *; }

-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.google.gson.** { *; }

# Сохраняем ВСЕ поля и классы, которые используются Gson
-keep class * {
  @com.google.gson.annotations.SerializedName *;
}

# Сохраняем классы с @Expose (если ты используешь его)
-keepclassmembers class * {
    @com.google.gson.annotations.Expose *;
}

-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Firebase
-keep class com.google.firebase.** { *; }
-keepnames class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepnames class com.google.android.gms.** { *; }

# OkHttp
-keep class okhttp3.** { *; }
-keepnames class okhttp3.** { *; }
-keep class okio.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}

-dontwarn com.android.build.api.variant.BuildConfigField
-dontwarn com.android.build.api.variant.Variant
-dontwarn groovy.lang.Closure
-dontwarn groovy.lang.GroovyObject
-dontwarn groovy.lang.MetaClass
-dontwarn groovy.transform.Generated
-dontwarn org.codehaus.groovy.reflection.ClassInfo
-dontwarn org.codehaus.groovy.runtime.GeneratedClosure
-dontwarn org.codehaus.groovy.runtime.ScriptBytecodeAdapter
-dontwarn org.codehaus.groovy.runtime.callsite.CallSite
-dontwarn org.codehaus.groovy.runtime.callsite.CallSiteArray
-dontwarn org.codehaus.groovy.runtime.typehandling.ShortTypeHandling
-dontwarn org.gradle.api.Action
-dontwarn org.gradle.api.DefaultTask
-dontwarn org.gradle.api.GradleException
-dontwarn org.gradle.api.Plugin
-dontwarn org.gradle.api.Project
-dontwarn org.gradle.api.artifacts.Configuration
-dontwarn org.gradle.api.artifacts.ConfigurationContainer
-dontwarn org.gradle.api.artifacts.DependencyResolutionListener
-dontwarn org.gradle.api.artifacts.ResolvableDependencies
-dontwarn org.gradle.api.artifacts.VersionConstraint
-dontwarn org.gradle.api.artifacts.component.ComponentIdentifier
-dontwarn org.gradle.api.artifacts.component.ComponentSelector
-dontwarn org.gradle.api.artifacts.component.ModuleComponentSelector
-dontwarn org.gradle.api.artifacts.result.DependencyResult
-dontwarn org.gradle.api.artifacts.result.ResolutionResult
-dontwarn org.gradle.api.artifacts.result.ResolvedComponentResult
-dontwarn org.gradle.api.file.ConfigurableFileCollection
-dontwarn org.gradle.api.file.FileCollection
-dontwarn org.gradle.api.model.ObjectFactory
-dontwarn org.gradle.api.provider.MapProperty
-dontwarn org.gradle.api.tasks.InputFiles
-dontwarn org.slf4j.Logger
-dontwarn org.slf4j.LoggerFactory