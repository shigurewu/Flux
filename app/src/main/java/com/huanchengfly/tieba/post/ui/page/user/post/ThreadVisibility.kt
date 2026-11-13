package com.huanchengfly.tieba.post.ui.page.user.post

internal enum class ThreadVisibility(val label: String) {
    Public("公开"), OnlySelf("仅自己可见"), Unknown("可见范围未确认");
    companion object {
        fun resolve(threadId: Long, publicIds: Set<Long>?, serverHidden: Boolean,
                    confirmedChange: Boolean?, cachedHidden: Boolean?): ThreadVisibility {
            val hidden = confirmedChange ?: publicIds?.let { threadId !in it }
                ?: true.takeIf { serverHidden } ?: cachedHidden
            return when (hidden) { true -> OnlySelf; false -> Public; null -> Unknown }
        }
    }
}

/** Only a complete public index may establish that an own post is absent from public view. */
internal suspend fun collectPublicThreadIds(
    maxPages: Int = 100,
    fetchPage: suspend (Int) -> List<Long>,
): Set<Long> {
    val ids = linkedSetOf<Long>()
    for (page in 1..maxPages) {
        val next = fetchPage(page)
        if (next.isEmpty()) return ids
        check(next.all { it > 0 }) { "公开列表包含无效帖子，无法确认可见范围" }
        val oldSize = ids.size
        ids.addAll(next)
        check(ids.size > oldSize) { "公开列表分页重复，无法确认可见范围" }
    }
    error("公开列表尚未完整加载，请重试")
}
