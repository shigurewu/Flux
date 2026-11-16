package com.huanchengfly.tieba.post.im

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ImPullCheckpointTest {
    @Test fun restartUsesConsumedPositionInsteadOfNewServerHead() = runBlocking {
        val disk = TestStore()
        ImPullCheckpoint(disk).advance("100", 9, 42)
        val reopened = ImPullCheckpoint(disk)
        assertEquals(42L, initialImPullCursor(reopened.read("100", 9), 44, 50))
        reopened.advance("100", 9, 50)
        reopened.advance("100", 9, 42)
        assertEquals(50L, ImPullCheckpoint(disk).read("100", 9))
    }

    @Test fun positionsAreIsolatedByAccountAndGroup() = runBlocking {
        val checkpoint = ImPullCheckpoint(TestStore())
        checkpoint.advance("100", 9, 42)
        assertNull(checkpoint.read("200", 9))
        assertNull(checkpoint.read("100", 10))
    }

    @Test fun upgradeUsesExistingIncomingPositionAndFreshInstallKeepsOriginalBaseline() {
        assertEquals(42L, initialImPullCursor(null, 42, 50))
        assertEquals(50L, initialImPullCursor(null, null, 50))
    }

    private class TestStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it }
    }
}
