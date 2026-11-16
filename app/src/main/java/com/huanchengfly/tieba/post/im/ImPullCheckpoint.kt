package com.huanchengfly.tieba.post.im

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first

/** Persist only consumed pull positions; a send acknowledgement is not a pull checkpoint. */
internal class ImPullCheckpoint(private val store: DataStore<Preferences>) {
    private fun key(owner: String, groupId: Long): Preferences.Key<Long> {
        require(owner.isNotBlank() && groupId > 0)
        return longPreferencesKey("im_pull_cursor_v1:$owner:$groupId")
    }

    suspend fun read(owner: String, groupId: Long): Long? = store.data.first()[key(owner, groupId)]

    suspend fun advance(owner: String, groupId: Long, cursor: Long) {
        require(cursor >= 0)
        val key = key(owner, groupId)
        store.edit { prefs ->
            val previous = prefs[key]
            if (previous == null || cursor > previous) prefs[key] = cursor
        }
    }
}

internal fun initialImPullCursor(saved: Long?, latestStoredIncoming: Long?, serverHead: Long): Long =
    saved ?: latestStoredIncoming ?: serverHead.coerceAtLeast(0)
