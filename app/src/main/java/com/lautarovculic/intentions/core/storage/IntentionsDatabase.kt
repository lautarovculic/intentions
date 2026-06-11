package com.lautarovculic.intentions.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        IntentRunEntity::class,
        PresetEntity::class,
        NoteEntity::class,
        CaptureSessionEntity::class,
        CapturedIntentEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class IntentionsDatabase : RoomDatabase() {
    abstract fun intentRunDao(): IntentRunDao
    abstract fun presetDao(): PresetDao
    abstract fun noteDao(): NoteDao
    abstract fun captureDao(): CaptureDao

    companion object {
        @Volatile
        private var instance: IntentionsDatabase? = null

        fun get(context: Context): IntentionsDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                IntentionsDatabase::class.java,
                "intentions.db",
            ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
        }
    }
}
