package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.Anti
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.widgets.compose.readingPositionOffset
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadPreviousRepliesTest {
    @Test
    fun replyPlacementUsesThirtyPercentOfTheFreeViewport() {
        assertEquals(180, readingPositionOffset(viewportHeight = 1000, itemHeight = 400))
        assertEquals(0, readingPositionOffset(viewportHeight = 800, itemHeight = 1000))
    }

    @Test
    fun previousRepliesPrependAndPreserveTheForwardPage() {
        val state = ThreadUiState(
            data = persistentListOf(reply(30), reply(40)),
            hasPrevious = true,
            currentPageMin = 3,
            currentPageMax = 4,
        )
        val request = ThreadUiIntent.LoadPrevious(threadId = 10, page = 2, postId = 30)
        val loading = ThreadPartialChange.LoadPrevious.Start(request).reduce(state)
        assertTrue(loading.isLoadingPrevious)
        assertFalse(loading.isRefreshing)
        val updated = result(request).reduce(loading)
        assertEquals(listOf(20L, 30L, 40L), updated.data.map { it.post.get { id } })
        assertEquals(2, updated.currentPageMin)
        assertEquals(4, updated.currentPageMax)
        assertFalse(updated.isLoadingPrevious)
    }

    @Test
    fun previousRepliesFromAnOldSortAreDiscarded() {
        val state = ThreadUiState(
            data = persistentListOf(reply(30)),
            hasPrevious = true,
            sortType = ThreadSortType.SORT_TYPE_DESC,
        )
        val request = ThreadUiIntent.LoadPrevious(threadId = 10, page = 2, postId = 30)
        assertEquals(state, result(request).reduce(state))
    }

    @Test
    fun resumeKeepsEveryPageBeforeTheTargetAndTheForwardCursor() = runBlocking {
        val requests = mutableListOf<Int>()
        val result = collectRepliesThroughTarget(page(1, 10), 35) { current, _ ->
            requests += current.currentPage
            when (current.currentPage) {
                1 -> page(2, 10, 20)
                2 -> page(3, 30, 35, 40)
                else -> error("Fetched beyond the target page")
            }
        }
        val loaded = result.reduce(ThreadUiState(isRefreshing = true))

        assertEquals(listOf(1, 2), requests)
        assertEquals(35L, result.postId)
        assertFalse(loaded.isRefreshing)
        assertFalse(loaded.hasPrevious)
        assertEquals(listOf(10L, 20L, 30L, 35L, 40L), loaded.data.map { it.post.get { id } })
        assertEquals(1, loaded.currentPageMin)
        assertEquals(3, loaded.currentPageMax)
        assertEquals(40L, loaded.nextPagePostId)
    }

    @Test
    fun aSkippedPageCannotReplaceTheContiguousPrefix() = runBlocking {
        val result = collectRepliesThroughTarget(page(1, 10), 35) { current, _ ->
            if (current.currentPage == 1) page(2, 20) else page(4, 35)
        }
        assertTrue(result.resumeUnavailable)
        assertTrue(result.resumeInterrupted)
        assertEquals(0L, result.postId)
        assertEquals(2, result.currentPage)
        assertEquals(listOf(10L, 20L), result.data.map { it.post.get { id } })
        assertTrue(result.hasMore)
    }

    @Test
    fun aMissingReplyFallsBackToTheClosestFloorAfterLoadingAllPages() = runBlocking {
        val lastPage = page(2, 30, 40).copy(hasMore = false, data = listOf(
            PostItemData(wrapImmutable(Post(id = 30, floor = 7))),
            PostItemData(wrapImmutable(Post(id = 40, floor = 9))),
        ))
        val result = collectRepliesThroughTarget(page(1, 10), 35, targetFloor = 9) { _, _ ->
            lastPage
        }

        assertEquals(40L, result.postId)
        assertTrue(result.resumeUnavailable)
        assertFalse(result.resumeInterrupted)
        assertEquals(listOf(10L, 30L, 40L), result.data.map { it.post.get { id } })
    }

    @Test
    fun retryContinuesAfterTheLastLoadedPageAndKeepsTheFirstPage() = runBlocking {
        val prefix = page(2, 10, 20).copy(currentPageMin = 1, resumeInterrupted = true)
        val result = collectRepliesThroughTarget(prefix, 35) { current, ids ->
            assertEquals(2, current.currentPage)
            assertEquals(setOf(10L, 20L), ids)
            page(3, 35)
        }
        assertEquals(1, result.currentPageMin)
        assertEquals(listOf(10L, 20L, 35L), result.data.map { it.post.get { id } })
        assertFalse(result.resumeInterrupted)
    }

    private fun page(number: Int, vararg ids: Long) = ThreadPartialChange.Load.Success(
            title = "",
            author = User(),
            user = User(),
            firstPost = null,
            data = ids.map(::reply),
            threadInfo = ThreadInfo(),
            forum = SimpleForum(),
            anti = Anti(),
            currentPage = number,
            totalPage = 5,
            hasMore = true,
            nextPagePostId = ids.last(),
            hasPrevious = number > 1,
            firstPostContentRenders = null,
        )

    private fun result(request: ThreadUiIntent.LoadPrevious) = ThreadPartialChange.LoadPrevious.Success(
        intent = request,
        author = User(),
        data = listOf(reply(20)),
        threadInfo = ThreadInfo(),
        currentPage = 2,
        totalPage = 5,
        hasPrevious = true,
    )

    private fun reply(id: Long) = PostItemData(wrapImmutable(Post(id = id)))
}
