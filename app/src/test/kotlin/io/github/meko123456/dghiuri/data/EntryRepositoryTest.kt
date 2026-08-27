package io.github.meko123456.dghiuri.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryRepositoryTest {

    /** Records the LIKE pattern the repository hands to the DAO. */
    private class RecordingDao : EntryDao {
        var lastPattern: String? = null

        override fun observeAll(): Flow<List<Entry>> = flowOf(emptyList())
        override fun observe(epochDay: Long): Flow<Entry?> = flowOf(null)
        override suspend fun get(epochDay: Long): Entry? = null
        override fun search(pattern: String): Flow<List<Entry>> {
            lastPattern = pattern
            return flowOf(emptyList())
        }
        override suspend fun upsert(entry: Entry) = Unit
        override suspend fun delete(entry: Entry) = Unit
    }

    @Test
    fun `plain text is passed through unchanged`() {
        assertEquals("push-ups today", EntryRepository.escapeLike("push-ups today"))
        assertEquals("", EntryRepository.escapeLike(""))
    }

    @Test
    fun `percent and underscore are escaped so they match literally`() {
        assertEquals("50\\%", EntryRepository.escapeLike("50%"))
        assertEquals("\\_", EntryRepository.escapeLike("_"))
        assertEquals("snake\\_case\\_name", EntryRepository.escapeLike("snake_case_name"))
        assertEquals("\\%\\_\\%", EntryRepository.escapeLike("%_%"))
    }

    @Test
    fun `the escape character itself is escaped first`() {
        assertEquals("a\\\\b", EntryRepository.escapeLike("a\\b"))
        assertEquals("\\\\\\%", EntryRepository.escapeLike("\\%"))
    }

    @Test
    fun `search hands the escaped pattern to the dao`() {
        val dao = RecordingDao()
        EntryRepository(dao).search("100%_done")
        assertEquals("100\\%\\_done", dao.lastPattern)
    }
}
