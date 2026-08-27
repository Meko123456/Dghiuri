package io.github.meko123456.dghiuri.data

import kotlinx.coroutines.flow.Flow

/** Thin repository over [EntryDao]; the only place that decides what "saving" means. */
class EntryRepository(private val dao: EntryDao) {

    fun observeAll(): Flow<List<Entry>> = dao.observeAll()

    fun observe(epochDay: Long): Flow<Entry?> = dao.observe(epochDay)

    fun search(query: String): Flow<List<Entry>> = dao.search(query)

    /**
     * Saves the day's text. An entry whose markdown is blank and has no mood is deleted rather
     * than stored, so an accidentally opened day never counts toward the streak.
     */
    suspend fun save(epochDay: Long, markdown: String, mood: Int?, now: Long = System.currentTimeMillis()) {
        val existing = dao.get(epochDay)
        if (markdown.isBlank() && mood == null) {
            existing?.let { dao.delete(it) }
            return
        }
        dao.upsert(Entry(epochDay = epochDay, markdown = markdown, mood = mood, updatedAt = now))
    }

    suspend fun delete(epochDay: Long) {
        dao.get(epochDay)?.let { dao.delete(it) }
    }
}
