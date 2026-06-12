# Keep Compose runtime
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.chatagent.data.model.**$$serializer { *; }
-keepclassmembers class com.chatagent.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.chatagent.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
