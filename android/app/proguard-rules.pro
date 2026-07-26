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
