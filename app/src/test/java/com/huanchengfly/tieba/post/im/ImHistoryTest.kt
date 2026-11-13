package com.huanchengfly.tieba.post.im

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class ImHistoryTest {
    private fun row(id: String = "123", from: String = "0", content: String = """[{"type":"0","text":"hello #(吐舌)"}]""") =
        """{"msg_id":"$id","time":"1700000000","from":"$from","content":$content}"""
    private fun page(rows: String = row(), more: String = "1") =
        """{"error_code":"0","message":[$rows],"has_more":"$more"}"""

    @Test fun mapsSendersAndKeepsCursorSeparateFromLiveIds() {
        val result = parseImHistory(page(row() + "," + row("122", "1")), "100", 200, 0)
        assertEquals(122L, result.nextBefore)
        assertTrue(result.hasMore)
        assertEquals(200L, result.messages[0].fromUid)
        assertEquals(100L, result.messages[1].fromUid)
        assertTrue(result.messages[1].isMine)
        assertEquals(Long.MIN_VALUE + 123, result.messages[0].msgId)
        assertEquals("hello #(吐舌)", result.messages[0].content)
    }

    @Test fun idsAboveDoublePrecisionStayExact() {
        val result = parseImHistory(page(row("9007199254740993")), "100", 200, 0)
        assertEquals(9007199254740993L, result.nextBefore)
    }

    @Test fun loginFailureNeverBecomesEmptyHistory() {
        assertThrows(IOException::class.java) { parseImHistory("""{"error_code":"1","info":[]}""", "100", 200, 0) }
    }

    @Test fun missingAndMalformedPayloadsNeverReportSuccess() {
        for (json in listOf("<html>error</html>", "{}", """{"error_code":0,"message":{},"has_more":0}""", page(row(from="2")))) {
            assertThrows(IOException::class.java) { parseImHistory(json, "100", 200, 0) }
        }
    }

    @Test fun duplicateOrForwardCursorIsRejected() {
        assertThrows(IOException::class.java) { parseImHistory(page(), "100", 200, 123) }
        assertThrows(IOException::class.java) { parseImHistory(page(""), "100", 200, 123) }
    }

    @Test fun unsupportedMediaAdvancesCursorWithoutImportingUrl() {
        val result = parseImHistory(page(row(content="""[{"type":"3","text":"https://image.invalid/a"}]"""), "0"), "100", 200, 0)
        assertEquals(123L, result.nextBefore)
        assertEquals(1, result.unsupported)
        assertTrue(result.messages.isEmpty())
        assertFalse(result.hasMore)
    }

    @Test fun repeatedPageHasStableIdsAndPreservesDistinctMessages() {
        val a = parseImHistory(page(row() + "," + row("122")), "100", 200, 0)
        val b = parseImHistory(page(row() + "," + row("122")), "100", 200, 0)
        assertEquals(a.messages, b.messages)
        assertEquals(2, a.messages.size)
    }

    @Test fun crossProtocolDuplicateRequiresAllFieldsToMatch() {
        val history = parseImHistory(page(), "100", 200, 0).messages.single()
        val live = history.copy(groupId=9, msgId=123)
        assertEquals(listOf(live), visibleImMessages(listOf(history, live)))
        assertEquals(2, visibleImMessages(listOf(history, live.copy(content="different"))).size)
        assertEquals(2, visibleImMessages(listOf(history, live.copy(uid="other"))).size)
        assertEquals(2, visibleImMessages(listOf(history, live.copy(peerUid=300))).size)
    }

    @Test fun emptyHistoryIsOnlyAcceptedWithExplicitEndFlag() {
        assertFalse(parseImHistory(page("", "0"), "100", 200, 0).hasMore)
    }

    @Test fun topLevelServerExplanationIsPreservedWithoutGuessingCodeMeaning() {
        val error = assertThrows(IOException::class.java) {
            parseImHistory("""{"error_code":"380006","error_msg":"示例错误说明"}""", "100", 200, 0)
        }
        assertEquals("历史接口返回错误（380006）：示例错误说明", error.message)
    }

    @Test fun nestedServerExplanationIsPreserved() {
        val error = assertThrows(IOException::class.java) {
            parseImHistory("""{"error_code":0,"error":{"errno":380006,"errmsg":"示例嵌套说明"}}""", "100", 200, 0)
        }
        assertTrue(error.message!!.contains("示例嵌套说明"))
    }

    @Test fun serverExplanationCannotEchoCredentialsOrUrls() {
        val error = assertThrows(IOException::class.java) {
            parseImHistory("""{"error_code":380006,"error_msg":"示例 abc-secret https://example.com/?BDUSS=abc-secret somebody@example.com abcdefghijklmnopqrstuvwxyz1234\n重试"}""",
                "100", 200, 0, listOf("abc-secret"))
        }
        assertFalse(error.message!!.contains("abc-secret"))
        assertFalse(error.message!!.contains("https://"))
        assertFalse(error.message!!.contains("somebody@"))
        assertFalse(error.message!!.contains("abcdefghijklmnopqrstuvwxyz"))
        assertFalse(error.message!!.contains("\n"))
        assertTrue(error.message!!.contains("重试"))
    }

    @Test fun malformedExplanationStillPreservesServerErrorCode() {
        val error = assertThrows(IOException::class.java) {
            parseImHistory("""{"error_code":380006,"error_msg":{"unexpected":"value"}}""", "100", 200, 0)
        }
        assertEquals("历史接口返回错误（380006）；服务端未提供具体说明", error.message)
    }
}
