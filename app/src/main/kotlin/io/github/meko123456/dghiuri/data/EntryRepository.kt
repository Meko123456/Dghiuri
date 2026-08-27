package io.github.meko123456.dghiuri.data

import kotlinx.coroutines.flow.Flow

/** Thin repository over [EntryDao]; the only place that decides what "saving" means. */
class EntryRepository(private val dao: EntryDao) {

    fun observeAll(): Flow<List<Entry>> = dao.observeAll()

    fun observe(epochDay: Long): Flow<Entry?> = dao.observe(epochDay)

    /** Entries whose markdown contains [query] literally; LIKE wildcards in the text are escaped. */
    fun search(query: String): Flow<List<Entry>> = dao.search(escapeLike(query))

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

    companion object {
        /**
         * Escapes [text] for use inside a `LIKE ... ESCAPE '\\'` pattern so that `%`, `_` and the
         * backslash itself match literally instead of acting as wildcards.
         */
        fun escapeLike(text: String): String = buildString(text.length) {
            for (ch in text) {
                if (ch == '\\' || ch == '%' || ch == '_') append('\\')
                append(ch)
            }
        }
    }
}
