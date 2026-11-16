package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.models.database.History
import kotlinx.coroutines.flow.Flow

object HistoryUtil {
    const val PAGE_SIZE = 100
    const val TYPE_FORUM = 1
    const val TYPE_THREAD = 2

    suspend fun deleteAll(owner: String) {
        DatabaseUtil.deleteAllHistory(owner)
    }

    suspend fun saveHistory(history: History) {
        DatabaseUtil.upsertHistory(history)
    }

    suspend fun getAll(owner: String): List<History> = DatabaseUtil.getAllHistory(owner)

    suspend fun get(owner: String, type: Int, page: Int): List<History> =
        DatabaseUtil.getHistoryByType(owner, type, PAGE_SIZE, page * PAGE_SIZE)

    fun getFlow(owner: String, type: Int, page: Int): Flow<List<History>> =
        DatabaseUtil.getHistoryFlowByType(owner, type, PAGE_SIZE, page * PAGE_SIZE)
}
