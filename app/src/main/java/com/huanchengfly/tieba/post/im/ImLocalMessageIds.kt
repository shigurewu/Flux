package com.huanchengfly.tieba.post.im

/** Reserve once per send, including uncertain failures; never reuse a local id. */
internal class ImLocalMessageIds {
    private val lastByPeer = mutableMapOf<Long, Long>()

    @Synchronized fun next(peer: Long, serverMsgId: Long): Long {
        require(serverMsgId in 0..Long.MAX_VALUE / 100) { "消息序号超出范围" }
        val base = maxOf(lastByPeer[peer] ?: 0L, serverMsgId * 100)
        check(base < Long.MAX_VALUE) { "消息序号超出范围" }
        return (base + 1).also { lastByPeer[peer] = it }
    }

    @Synchronized fun clear() { lastByPeer.clear() }
}
