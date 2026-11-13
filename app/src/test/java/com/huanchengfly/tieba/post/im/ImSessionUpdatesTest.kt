package com.huanchengfly.tieba.post.im

import com.huanchengfly.tieba.post.models.database.ImMessage
import com.huanchengfly.tieba.post.models.database.ImSession
import org.junit.Assert.*
import org.junit.Test

class ImSessionUpdatesTest {
    private fun message(id: Long, time: Long = id, mine: Boolean = false) = ImMessage(
        uid = "account-a", peerUid = 42, msgId = id, createTime = time,
        content = "message-$id", isMine = mine
    )

    @Test fun incomingMessagesAccumulateUnread() {
        val first = sessionAfterNewMessage(null, message(1), "朋友", "portrait")
        val second = sessionAfterNewMessage(first, message(2))
        assertEquals(2, second.unreadCount)
        assertEquals("朋友", second.peerName)
        assertEquals("message-2", second.lastMsgPreview)
    }

    @Test fun outgoingMessagePreservesPeerAndUnread() {
        val old = sessionAfterNewMessage(null, message(1), "朋友", "portrait")
        val result = sessionAfterNewMessage(old, message(2, mine = true), "我", "my-portrait")
        assertEquals(1, result.unreadCount)
        assertEquals("朋友", result.peerName)
        assertEquals("portrait", result.peerPortrait)
        assertTrue(result.lastMsgFromMe)
    }

    @Test fun outOfOrderDeliveryDoesNotReplaceLatestPreview() {
        val old = sessionAfterNewMessage(null, message(20))
        val result = sessionAfterNewMessage(old, message(10))
        assertEquals("message-20", result.lastMsgPreview)
        assertEquals(20L, result.lastMsgTime)
        assertEquals(2, result.unreadCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun anotherAccountCannotReuseSession() {
        sessionAfterNewMessage(ImSession(uid = "account-b", peerUid = 42), message(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun anotherPeerCannotReuseSession() {
        sessionAfterNewMessage(ImSession(uid = "account-a", peerUid = 99), message(1))
    }
}
