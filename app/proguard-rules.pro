# VidéViewer ProGuard Rules

# Keep application class
-keep class com.videviewer.VidViewerApp { *; }

# Keep all activities
-keep class com.videviewer.activities.** { *; }

# Keep all fragments
-keep class com.videviewer.fragments.** { *; }

# Keep model classes (Parcelable, Room entities)
-keep class com.videviewer.models.** { *; }
-keep class com.videviewer.database.** { *; }

# Keep adapters
-keep class com.videviewer.adapters.** { *; }

# Keep services and receivers
-keep class com.videviewer.services.** { *; }
-keep class com.videviewer.receivers.** { *; }

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Material Design
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Lottie
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# Shimmer
-keep class com.facebook.shimmer.** { *; }

# General rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Prevent stripping of setters/getters used reflectively
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
