package com.huanchengfly.tieba.post.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SignInReminderTest {
    @Test
    fun opensAtLocalOneAmAndUsesFullCalendarDate() {
        val localTime = Calendar.getInstance(TimeZone.getTimeZone("Asia/Hong_Kong")).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 30, 0, 59, 59)
        }
        assertNull(signInReminderDay(localTime))

        localTime.add(Calendar.SECOND, 1)
        assertEquals(20261130, signInReminderDay(localTime))
        localTime.add(Calendar.HOUR_OF_DAY, 23)
        assertNull(signInReminderDay(localTime))
        localTime.add(Calendar.HOUR_OF_DAY, 1)
        assertEquals(20261201, signInReminderDay(localTime))
    }

    @Test
    fun repeatedOpensAndRecreationPromptOnlyOncePerAccountPerDay() = runBlocking {
        val preferences = TestStore()
        val reminders = SignInReminderStore(preferences)
        val claims = List(2) { async { reminders.claim("123", 20261115) } }.awaitAll()
        assertEquals(1, claims.count { it })
        assertFalse(SignInReminderStore(preferences).claim("123", 20261115))
        assertTrue(reminders.claim("456", 20261115))
        assertTrue(reminders.claim("123", 20261215))
        assertFalse(reminders.claim("123", 20261215))
    }

    private class TestStore : DataStore<Preferences> {
        private val mutex = Mutex()
        override val data = MutableStateFlow(emptyPreferences())

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            mutex.withLock { transform(data.value).also { data.value = it } }
    }
}
