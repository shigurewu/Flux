package com.huanchengfly.tieba.post.im

import org.junit.Assert.*
import org.junit.Test
import okio.Buffer

class ImHistoryRequestTest {
    @Test fun initialRequestUsesRecentMessagesWithZeroCursor() {
        val request = buildImHistoryRequest("100", 200, 0, "client", "test-secret")
        assertEquals("/c/s/recentmsg", request.url.encodedPath)
        assertEquals("0", request.url.queryParameter("msg_id"))
        assertEquals("200", request.url.queryParameter("com_id"))
        assertEquals("100", request.url.queryParameter("user_id"))
    }

    @Test fun subsequentRequestUsesHistoryAndExactEarliestId() {
        val request = buildImHistoryRequest("100", 200, 9007199254740993L, "client", "test-secret")
        assertEquals("/c/s/historymsg", request.url.encodedPath)
        assertEquals("9007199254740993", request.url.queryParameter("msg_id"))
    }

    @Test fun postHasEmptyBodyAndCredentialOnlyInCookie() {
        val request = buildImHistoryRequest("100", 200, 0, "client", "test-secret")
        assertEquals("POST", request.method)
        val buffer = Buffer()
        request.body!!.writeTo(buffer)
        assertEquals("", buffer.readUtf8())
        assertEquals("BDUSS=test-secret", request.header("Cookie"))
        assertNull(request.url.queryParameter("BDUSS"))
        assertFalse(request.url.toString().contains("test-secret"))
    }

    @Test fun signatureMatchesLegacyRawValueVectorBeforeUrlEncoding() {
        val request = buildImHistoryRequest("100", 200, 0, "test client+id", "test-secret")
        assertEquals("test client+id", request.url.queryParameter("_client_id"))
        assertEquals("3fb7c8f1f7b97dc2f2bf5ed63169776f", request.url.queryParameter("sign"))
    }

    @Test fun invalidIdsCannotBuildHistoryRequest() {
        assertThrows(IllegalArgumentException::class.java) { buildImHistoryRequest("100", 200, -1, "client", "test-secret") }
        assertThrows(IllegalArgumentException::class.java) { buildImHistoryRequest("invalid", 200, 0, "client", "test-secret") }
    }
}
