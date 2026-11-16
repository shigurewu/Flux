package com.huanchengfly.tieba.post.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.toJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * 记录一种排序下最后阅读的楼层，供动态页的回复入口定位。
 */
data class ThreadReadMark(
    val postId: Long = 0,
    val floor: Int = 0,
    /** 记录时的排序：true = 倒序 */
    val isDesc: Boolean = false,
    val timestamp: Long = 0,
    val offsetDp: Float? = null,
)

internal class ThreadReadMarks(
    private val store: DataStore<Preferences>,
    accountUid: String,
) {
    // The old shared key has no owner metadata. Leave it unread for every account.
    private val key = stringPreferencesKey(
        if (accountUid.isEmpty()) "thread_read_marks_guest" else "thread_read_marks_uid_$accountUid"
    )

    private companion object {
        const val MAX_ENTRIES = 300
    }

    suspend fun getLatestThreadMark(threadId: Long, isDesc: Boolean): ThreadReadMark? =
        runCatching {
            val marks = currentMarks(store.data.first())
            val latest = marks.maxByOrNull { it.value.timestamp }
            if (latest?.key?.substringBefore(':') == threadId.toString()) {
                marks["$threadId:$isDesc"]
            } else null
        }.onFailure { if (it is CancellationException) throw it }.getOrNull()

    suspend fun put(threadId: Long, mark: ThreadReadMark) {
        runCatching {
            store.edit { prefs ->
                val marks = currentMarks(prefs)
                marks["$threadId:${mark.isDesc}"] = mark
                if (marks.size > MAX_ENTRIES) {
                    // LRU 截断：只保留最近访问的条目
                    val kept = marks.entries
                        .sortedByDescending { it.value.timestamp }
                        .take(MAX_ENTRIES)
                        .associate { it.key to it.value }
                    marks.clear()
                    marks.putAll(kept)
                }
                prefs[key] = marks.toJson()
            }
        }.onFailure { if (it is CancellationException) throw it }
    }

    private fun currentMarks(prefs: Preferences): MutableMap<String, ThreadReadMark> =
        prefs[key]?.fromJson<MutableMap<String, ThreadReadMark>>() ?: mutableMapOf()
}
