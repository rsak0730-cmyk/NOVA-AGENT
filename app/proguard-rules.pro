# Keep data models serialized with kotlinx.serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer {
    public static final ** INSTANCE;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable class *;
}

# Keep OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep App Models
-keep class com.editog.novaagent.data.model.** { *; }
