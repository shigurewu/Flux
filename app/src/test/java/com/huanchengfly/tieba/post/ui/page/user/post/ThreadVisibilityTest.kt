package com.huanchengfly.tieba.post.ui.page.user.post

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ThreadVisibilityTest {
    @Test fun defaultZeroAndMissingCacheAreNotEvidenceOfPublic() {
        assertEquals(ThreadVisibility.Unknown, ThreadVisibility.resolve(8, null, false, null, null))
        assertEquals(ThreadVisibility.OnlySelf, ThreadVisibility.resolve(8, null, true, null, null))
    }
    @Test fun completePublicIndexOverridesStaleLocalChanges() {
        assertEquals(ThreadVisibility.OnlySelf, ThreadVisibility.resolve(8, setOf(9), false, null, false))
        assertEquals(ThreadVisibility.Public, ThreadVisibility.resolve(8, setOf(8), false, null, true))
    }
    @Test fun successfulChangeIsImmediatelyVisibleBeforeNextServerSync() {
        assertEquals(ThreadVisibility.OnlySelf, ThreadVisibility.resolve(8, setOf(8), false, true, null))
        assertEquals(ThreadVisibility.Public, ThreadVisibility.resolve(8, emptySet(), false, false, null))
    }
    @Test fun publicIndexContinuesPastShortPagesAndCollectsAllPages() = runBlocking {
        val requests = mutableListOf<Int>()
        val ids = collectPublicThreadIds { page ->
            requests += page
            when (page) { 1 -> listOf(1, 2); 2 -> listOf(3); 3 -> listOf(4, 5); else -> emptyList() }
        }
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L), ids)
        assertEquals(listOf(1, 2, 3, 4), requests)
    }
    @Test fun failedOrRepeatedPageCannotProducePartialPrivacyIndex() = runBlocking {
        try {
            collectPublicThreadIds { page -> if (page == 1) listOf(1L) else error("network") }
            fail("must not return a partial index")
        } catch (expected: IllegalStateException) { assertEquals("network", expected.message) }
        try {
            collectPublicThreadIds { listOf(1L) }
            fail("repeated pagination must fail")
        } catch (_: IllegalStateException) { }
    }
    @Test fun pageLimitCannotLabelUnseenPublicThreadsAsPrivate() = runBlocking {
        try {
            collectPublicThreadIds(maxPages = 2) { listOf(it.toLong()) }
            fail("incomplete pagination must fail")
        } catch (_: IllegalStateException) { }
    }
    @Test fun emptyCompleteIndexIsValid() = runBlocking {
        assertEquals(emptySet<Long>(), collectPublicThreadIds { emptyList() })
    }
}
