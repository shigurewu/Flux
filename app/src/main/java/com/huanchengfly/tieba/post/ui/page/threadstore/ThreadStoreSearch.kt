package com.huanchengfly.tieba.post.ui.page.threadstore

import com.huanchengfly.tieba.post.api.models.ThreadStoreBean.ThreadStoreInfo
import java.io.IOException

internal suspend fun loadSearchableFavorites(
    onPage: (List<ThreadStoreInfo>) -> Unit = {},
    fetchPage: suspend (page: Int) -> List<ThreadStoreInfo>,
): List<ThreadStoreInfo> {
    val favorites = linkedMapOf<String, ThreadStoreInfo>()
    var page = 0
    while (true) {
        val entries = fetchPage(page)
        if (entries.isEmpty()) return favorites.values.toList()
        val oldSize = favorites.size
        entries.forEach { favorite -> favorites.getOrPut(favorite.threadId) { favorite } }
        if (favorites.size == oldSize) throw IOException("收藏列表分页没有继续前进")
        onPage(favorites.values.toList())
        page++
    }
}

internal fun List<ThreadStoreInfo>.filterFavorites(query: String): List<ThreadStoreInfo> {
    val terms = query.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
    return filter { item ->
        val fields = listOf(item.title, item.forumName, item.author.nameShow, item.author.name)
        terms.all { term -> fields.any { it?.contains(term, ignoreCase = true) == true } }
    }
}
