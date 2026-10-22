# Dawa & Hayah ProGuard Rules
-keepattributes *Annotation*,InnerClasses,Signature
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class *$$serializer { *; }
-dontwarn kotlinx.serialization.**

# Room entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
