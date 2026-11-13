package com.huanchengfly.tieba.post.ui.page.user.followlist

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class FansErrorDetailTest {
    @Test fun diagnosticsDoNotExposeCredentialValues() {
        val detail = fansErrorDetail(IOException("BDUSS=fake-session; STOKEN=fake-token; 请求失败"))
        assertFalse(detail.contains("fake-session"))
        assertFalse(detail.contains("fake-token"))
        assertTrue(detail.contains("请求失败"))
    }
}
