# Room
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.Database { *; }

# Hilt
-keep class com.google.dagger.hilt.** { *; }
-keep class * implements dagger.hilt.** { *; }

# CameraX
-keep class androidx.camera.** { *; }

# TensorFlow Lite
-keep class org.tensorflow.** { *; }
-keep class com.google.mediapipe.** { *; }

# AndroidX
-keep class androidx.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }