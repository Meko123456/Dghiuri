package io.github.meko123456.dghiuri.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE epochDay = :epochDay")
    fun observe(epochDay: Long): Flow<Entry?>

    @Query("SELECT * FROM entries WHERE epochDay = :epochDay")
    suspend fun get(epochDay: Long): Entry?

    /**
     * Substring match on the raw markdown. [pattern] must already have `%`, `_` and `\\` escaped
     * with a backslash (see [EntryRepository.escapeLike]) so they match literally.
     */
    @Query("SELECT * FROM entries WHERE markdown LIKE '%' || :pattern || '%' ESCAPE '\\' ORDER BY epochDay DESC")
    fun search(pattern: String): Flow<List<Entry>>

    @Upsert
    suspend fun upsert(entry: Entry)

    @Delete
    suspend fun delete(entry: Entry)
}
