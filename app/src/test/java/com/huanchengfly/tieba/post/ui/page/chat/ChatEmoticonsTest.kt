package com.huanchengfly.tieba.post.ui.page.chat

import org.junit.Assert.*
import org.junit.Test

class ChatEmoticonsTest {
    @Test fun insertingAtCursorPreservesSurroundingText() {
        val result = insertChatEmoticon("你好世界", 2, 2, "吐舌")
        assertEquals("你好#(吐舌)世界", result.text)
        assertEquals("你好#(吐舌)".length, result.cursor)
    }
    @Test fun reversedSelectionIsReplaced() {
        assertEquals("a#(滑稽)d", insertChatEmoticon("abcd", 3, 1, "滑稽").text)
    }
    @Test fun consecutiveEmoticonsRemainSeparateProtocolTokens() {
        val first = insertChatEmoticon("", 0, 0, "吐舌")
        assertEquals("#(吐舌)#(吐舌)", insertChatEmoticon(first.text, first.cursor, first.cursor, "吐舌").text)
    }
}
