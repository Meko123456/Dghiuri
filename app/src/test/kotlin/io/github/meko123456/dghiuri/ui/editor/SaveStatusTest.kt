package io.github.meko123456.dghiuri.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The editor footer's three words. Small, but it is the only thing on screen telling you whether
 * what you wrote is on disk, so it has to be true rather than reassuring.
 */
class SaveStatusTest {

    @Test
    fun `an unsaved edit is still saving`() {
        assertEquals("saving…", saveStatus(dirty = true, exists = false))
        assertEquals("saving…", saveStatus(dirty = true, exists = true))
    }

    @Test
    fun `a day with a row is saved`() {
        assertEquals("saved", saveStatus(dirty = false, exists = true))
    }

    @Test
    fun `a day that was never written has nothing to save`() {
        // The bug this replaces: opening today's empty editor said "0 words · saved" before a
        // key had been pressed, about a row that did not exist.
        assertEquals("nothing to save", saveStatus(dirty = false, exists = false))
    }

    @Test
    fun `dirty wins over exists — the pending edit is the unsaved thing`() {
        // An existing entry being edited is "saving…", not "saved" — the row on disk is stale.
        assertEquals("saving…", saveStatus(dirty = true, exists = true))
    }
}
