package io.github.redballoonx.fableverse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.redballoonx.fableverse.data.local.dao.AudiobookDao
import io.github.redballoonx.fableverse.data.local.entity.AuthorEntity
import io.github.redballoonx.fableverse.data.local.entity.AudiobookEntity
import io.github.redballoonx.fableverse.data.local.entity.ChapterEntity

@Database(
    entities = [
        AuthorEntity::class,
        AudiobookEntity::class,  // ← Wird verwendet, aber Import fehlt
        ChapterEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AudiobookDatabase : RoomDatabase() {

    abstract fun audiobookDao(): AudiobookDao

    companion object {
        @Volatile
        private var INSTANCE: AudiobookDatabase? = null

        fun getDatabase(context: Context): AudiobookDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AudiobookDatabase::class.java,
                    "audiobook_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}