package com.huanchengfly.tieba.post.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.toJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreadReadMarksTest {
    @Test
    fun sameThreadKeepsSeparatePositionsForEachAccountAndSort() = runBlocking {
        val store = TestStore()
        val accountA = ThreadReadMarks(store, accountUid = "123")
        val accountB = ThreadReadMarks(store, accountUid = "456")
        val first = ThreadReadMark(postId = 11, floor = 2)
        val second = ThreadReadMark(postId = 12, floor = 3)
        val descending = second.copy(isDesc = true)

        accountA.put(99, first)
        assertNull(accountB.getLatestThreadMark(99, isDesc = false))
        accountB.put(99, second)
        accountA.put(99, descending)

        assertEquals(first, ThreadReadMarks(store, accountUid = "123").getLatestThreadMark(99, isDesc = false))
        assertEquals(descending, accountA.getLatestThreadMark(99, isDesc = true))
        assertEquals(second, accountB.getLatestThreadMark(99, isDesc = false))
        assertNull(accountB.getLatestThreadMark(99, isDesc = true))
    }

    @Test
    fun guestAndAccountsNeverReadUnownedLegacyProgress() = runBlocking {
        val store = TestStore()
        val mark = ThreadReadMark(postId = 11, floor = 2)
        store.edit { it[stringPreferencesKey("thread_read_marks")] = mapOf("99:false" to mark).toJson() }
        val guest = ThreadReadMarks(store, accountUid = "")
        val account = ThreadReadMarks(store, accountUid = "123")

        assertNull(guest.getLatestThreadMark(99, isDesc = false))
        assertNull(account.getLatestThreadMark(99, isDesc = false))
        guest.put(99, mark)
        assertEquals(mark, guest.getLatestThreadMark(99, isDesc = false))
        assertNull(account.getLatestThreadMark(99, isDesc = false))
    }

    @Test
    fun onlyTheMostRecentlyReadThreadCanResume() = runBlocking {
        val marks = ThreadReadMarks(TestStore(), accountUid = "123")
        val old = ThreadReadMark(postId = 11, floor = 2, timestamp = 100)
        val latest = ThreadReadMark(postId = 12, floor = 3, timestamp = 200)
        marks.put(99, old)
        marks.put(100, latest.copy(isDesc = true))
        assertNull(marks.getLatestThreadMark(99, isDesc = false))
        assertNull(marks.getLatestThreadMark(100, isDesc = false))
        assertEquals(latest.copy(isDesc = true), marks.getLatestThreadMark(100, isDesc = true))
        marks.put(99, old.copy(timestamp = 300))
        assertEquals(old.copy(timestamp = 300), marks.getLatestThreadMark(99, isDesc = false))
        assertNull(marks.getLatestThreadMark(100, isDesc = true))
    }

    private class TestStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }
}
