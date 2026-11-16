package com.huanchengfly.tieba.post.ui.page.threadstore

import com.huanchengfly.tieba.post.api.models.ThreadStoreBean.AuthorInfo
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean.ThreadStoreInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class ThreadStoreSearchTest {
    @Test
    fun findsFavoritesOnLaterPagesAndKeepsFirstOccurrence() = runBlocking {
        val first = favorite("1", "收藏整理")
        val later = favorite("2", "Android 相册")
        val pages = listOf(listOf(first), listOf(first.copy(title = "旧标题"), later), emptyList())
        val requested = mutableListOf<Int>()
        val results = loadSearchableFavorites { page ->
            requested.add(page)
            pages[page]
        }
        assertEquals(listOf(0, 1, 2), requested)
        assertEquals(listOf(first, later), results)
        assertEquals(listOf(later), results.filterFavorites("  android 作者  "))
        assertEquals(listOf(first, later), results.filterFavorites("摄影"))
        assertEquals(emptyList<ThreadStoreInfo>(), results.filterFavorites("无匹配"))
    }

    @Test
    fun repeatedPageFailsInsteadOfLoopingOrReturningIncompleteResults() = runBlocking {
        try {
            loadSearchableFavorites { listOf(favorite("1", "收藏整理")) }
            fail("Repeated page should fail")
        } catch (_: IOException) {
        }
    }

    private fun favorite(id: String, title: String) = ThreadStoreInfo(
        threadId = id,
        title = title,
        forumName = "摄影",
        author = AuthorInfo(name = "账号", nameShow = "作者"),
        media = emptyList(),
        isDeleted = "0",
        lastTime = "",
        type = "",
        status = "",
        maxPid = "0",
        minPid = "0",
        markPid = "0",
        markStatus = "",
        postNo = "0",
        postNoMsg = "",
        count = "0",
    )
}
