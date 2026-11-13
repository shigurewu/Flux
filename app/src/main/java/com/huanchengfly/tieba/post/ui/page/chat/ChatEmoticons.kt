package com.huanchengfly.tieba.post.ui.page.chat

internal data class ChatDraft(val text: String, val cursor: Int)

internal fun insertChatEmoticon(text: String, selectionStart: Int, selectionEnd: Int, name: String): ChatDraft {
    val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val end = maxOf(selectionStart, selectionEnd).coerceIn(start, text.length)
    val token = "#($name)"
    return ChatDraft(text.replaceRange(start, end, token), start + token.length)
}
