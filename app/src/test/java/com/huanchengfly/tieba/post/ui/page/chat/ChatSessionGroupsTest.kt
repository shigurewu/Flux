package com.huanchengfly.tieba.post.ui.page.chat

import com.huanchengfly.tieba.post.api.models.FollowListBean
import com.huanchengfly.tieba.post.models.database.ImSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ChatSessionGroupsTest {
    @Test fun foldedRowUsesMostRecentStrangerTimeAmongDirectChats() {
        val older = ImSession(uid = "a", peerUid = 1, lastMsgTime = 100)
        val newer = ImSession(uid = "a", peerUid = 2, lastMsgTime = 300)
        val strangers = listOf(
            ImSession(uid = "a", peerUid = 3, lastMsgTime = 50),
            ImSession(uid = "a", peerUid = 4, lastMsgTime = 200),
        )
        val rows = ChatSessionGroups(listOf(older, newer), strangers).inboxConversations()
        assertEquals(
            listOf(InboxConversation.Direct(newer), InboxConversation.Folded(strangers), InboxConversation.Direct(older)),
            rows,
        )
        val latestStranger = strangers.last().copy(lastMsgTime = 400)
        assertEquals("folded", ChatSessionGroups(listOf(older, newer), listOf(latestStranger)).inboxConversations().first().key)
    }

    @Test fun emptyFoldedRowStaysBelowEveryDirectChat() {
        val session = ImSession(uid = "a", peerUid = 1)
        assertEquals(
            listOf(InboxConversation.Direct(session), InboxConversation.Folded(emptyList())),
            ChatSessionGroups(listOf(session), emptyList()).inboxConversations(),
        )
    }

    @Test fun fullFollowingListKeepsPageTwoFriendOutOfFoldedMessages(): Unit = runBlocking {
        val ids = loadChatFollowIds({ true }) { page ->
            FollowListBean(hasMore = if (page == 1) 1 else 0,
                followList = listOf(FollowListBean.FollowUserBean(id = page.toLong())))
        }
        val sessions = listOf(ImSession(uid = "a", peerUid = 2), ImSession(uid = "a", peerUid = 3))
        val grouped = groupChatSessions(sessions, ids, "a")
        assertEquals(listOf(2L), grouped.normal.map { it.peerUid })
        assertEquals(listOf(3L), grouped.folded.map { it.peerUid })
    }
    @Test fun anotherAccountsSessionsAreNeverShown() {
        val result = groupChatSessions(listOf(ImSession(uid = "b", peerUid = 3)), emptySet(), "a")
        assertTrue(result.normal.isEmpty() && result.folded.isEmpty())
    }
    @Test fun repeatedPageDoesNotProducePartialRelationshipSnapshot(): Unit = runBlocking {
        try {
            loadChatFollowIds({ true }) { FollowListBean(hasMore = 1, followList = listOf(FollowListBean.FollowUserBean(id = 1))) }
            fail("Repeated page must fail")
        } catch (_: IOException) { }
    }
    @Test fun accountSwitchDiscardsRelationshipResult(): Unit = runBlocking {
        var current = true
        try {
            loadChatFollowIds({ current }) { current = false; FollowListBean() }
            fail("Stale account must be cancelled")
        } catch (_: CancellationException) { }
    }
}
