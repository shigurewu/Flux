package com.huanchengfly.tieba.post.ui.page.chat

import com.huanchengfly.tieba.post.api.models.FollowListBean
import com.huanchengfly.tieba.post.models.database.ImSession
import kotlinx.coroutines.CancellationException
import java.io.IOException

internal data class ChatSessionGroups(val normal: List<ImSession>, val folded: List<ImSession>)

internal fun groupChatSessions(sessions: List<ImSession>, followed: Set<Long>, owner: String): ChatSessionGroups {
    val (normal, folded) = sessions.filter { it.uid == owner }.partition { it.peerUid in followed }
    return ChatSessionGroups(normal, folded)
}

/** Never classify strangers from an incomplete following list. */
internal suspend fun loadChatFollowIds(
    isCurrentOwner: () -> Boolean,
    fetchPage: suspend (Int) -> FollowListBean,
): Set<Long> {
    val ids = mutableSetOf<Long>()
    val pages = mutableSetOf<List<Long>>()
    for (page in 1..100) {
        if (!isCurrentOwner()) throw CancellationException("Account changed")
        val result = fetchPage(page)
        if (!isCurrentOwner()) throw CancellationException("Account changed")
        if (result.errorCode != 0) throw IOException("关注关系加载失败")
        val current = result.followList.map { it.id }.filter { it > 0L }
        if (result.hasMore == 1 && (current.isEmpty() || !pages.add(current.sorted()))) {
            throw IOException("关注列表分页未完整返回")
        }
        ids.addAll(current)
        if (result.hasMore != 1) return ids
    }
    throw IOException("关注列表分页超限")
}
