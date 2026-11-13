package com.huanchengfly.tieba.post.ui.page.user.likeforum

internal enum class ForumVisibility(val label: String) {
    Unknown("公开范围未知"), Public("公开"), Friends("好友可见"), Private("仅自己可见");
    fun canView(self: Boolean, friend: Boolean): Boolean = self || when (this) {
        Public, Unknown -> true
        Friends -> friend
        Private -> false
    }
    companion object {
        fun fromServer(value: Int?): ForumVisibility = when (value) {
            1 -> Public
            2 -> Friends
            3 -> Private
            else -> Unknown
        }
    }
}
