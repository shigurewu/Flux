package com.huanchengfly.tieba.post.im

import com.huanchengfly.tieba.post.models.database.ImMessage
import com.huanchengfly.tieba.post.models.database.ImSession

/** Called only after a successful INSERT, inside the same Room transaction. */
internal fun sessionAfterNewMessage(
    previous: ImSession?, message: ImMessage, peerName: String = "", peerPortrait: String = "",
    countAsUnread: Boolean = true,
): ImSession {
    require(previous == null || (previous.uid == message.uid && previous.peerUid == message.peerUid))
    val session = previous ?: ImSession(uid = message.uid, peerUid = message.peerUid)
    val newest = message.createTime > session.lastMsgTime ||
        (message.createTime == session.lastMsgTime && message.msgId >= session.lastMsgId)
    return session.copy(
        peerName = if (!message.isMine) peerName.ifBlank { session.peerName } else session.peerName,
        peerPortrait = if (!message.isMine) peerPortrait.ifBlank { session.peerPortrait } else session.peerPortrait,
        lastMsgId = maxOf(session.lastMsgId, message.msgId),
        lastMsgTime = maxOf(session.lastMsgTime, message.createTime),
        lastMsgPreview = if (newest) message.content else session.lastMsgPreview,
        lastMsgFromMe = if (newest) message.isMine else session.lastMsgFromMe,
        unreadCount = session.unreadCount + if (message.isMine || !countAsUnread) 0 else 1,
    )
}
