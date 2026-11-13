package com.huanchengfly.tieba.post.im

import org.junit.Assert.*
import org.junit.Test

class ImLocalMessageIdsTest {
    @Test fun consecutiveSendsDoNotReuseIdsWhileServerCursorIsUnchanged() {
        val ids = ImLocalMessageIds()
        assertEquals(4201L, ids.next(1, 42))
        assertEquals(4202L, ids.next(1, 42))
    }
    @Test fun oldBackfillDoesNotMoveIdsBackward() {
        val ids = ImLocalMessageIds()
        assertEquals(4201L, ids.next(1, 42))
        assertEquals(4202L, ids.next(1, 2))
        assertEquals(5001L, ids.next(1, 50))
    }
    @Test fun peerAndAccountResetAreIndependent() {
        val ids = ImLocalMessageIds()
        ids.next(1, 42)
        ids.next(1, 42)
        assertEquals(4201L, ids.next(2, 42))
        ids.clear()
        assertEquals(4201L, ids.next(1, 42))
    }
}
