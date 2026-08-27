package io.github.meko123456.dghiuri

import android.app.Application
import android.content.Context
import io.github.meko123456.dghiuri.data.DghiuriDatabase
import io.github.meko123456.dghiuri.data.EntryRepository

/** Application-scoped object graph. Small app, no DI framework needed. */
class DghiuriApp : Application() {
    val repository: EntryRepository by lazy { EntryRepository(DghiuriDatabase.get(this).entryDao()) }
}

val Context.dghiuriApp: DghiuriApp
    get() = applicationContext as DghiuriApp
