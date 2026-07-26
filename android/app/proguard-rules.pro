# Ghost Cart release rules. Models are stored explicitly as JSON and do not
# require reflection-based serialization keeps.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager's internal Room database is instantiated via
# Class.forName(RoomDatabase::class.canonicalName + "_Impl") - a raw string
# lookup R8 can't see, so it renames/strips the generated *_Impl class and
# crashes at first-provider-init with "Failed to create an instance of class
# ... WorkDatabase". Keeping Room's generated implementation classes intact
# fixes this for WorkManager and any future direct Room usage.
-keep class * extends androidx.room.RoomDatabase
-keep class **_Impl { *; }

# Navigation3's NavKey hierarchy (NavigationKeys.kt) is dozens of small
# @Serializable data classes/objects, several with zero fields. Without
# this, R8's class-merging optimization can collapse two structurally
# identical NavKey types into one physical class, and Navigation3's
# internal entry registry (keyed by class) then sees a duplicate
# registration and crashes on first launch: "An entry with the same clazz
# has already been added: p." (reproduced on-device, versionCode 68).
# Keeping every NavKey implementor whole - unmerged, unrenamed - fixes it.
-keep class * implements androidx.navigation3.runtime.NavKey { *; }
