package io.github.meko123456.dghiuri.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Entry::class], version = 1, exportSchema = false)
abstract class DghiuriDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile private var instance: DghiuriDatabase? = null

        fun get(context: Context): DghiuriDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                DghiuriDatabase::class.java,
                "dghiuri.db",
            ).build().also { instance = it }
        }
    }
}
