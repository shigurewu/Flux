package com.huanchengfly.tieba.post.ui.page.user.likeforum

import org.junit.Assert.*
import org.junit.Test

class ForumVisibilityTest {
    @Test fun unknownIsNeverLabeledPublic() {
        assertEquals(ForumVisibility.Unknown, ForumVisibility.fromServer(null))
        assertEquals(ForumVisibility.Unknown, ForumVisibility.fromServer(0))
        assertEquals(ForumVisibility.Unknown, ForumVisibility.fromServer(999))
    }
    @Test fun privateAndFriendsRespectViewer() {
        assertFalse(ForumVisibility.fromServer(3).canView(false, true))
        assertTrue(ForumVisibility.fromServer(3).canView(true, false))
        assertFalse(ForumVisibility.fromServer(2).canView(false, false))
        assertTrue(ForumVisibility.fromServer(2).canView(false, true))
        assertTrue(ForumVisibility.fromServer(1).canView(false, false))
    }
}
