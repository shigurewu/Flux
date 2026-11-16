package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState

internal fun readingPositionOffset(viewportHeight: Int, itemHeight: Int): Int =
    ((viewportHeight - itemHeight).coerceAtLeast(0) * 0.3f).toInt()

internal suspend fun LazyListState.scrollToReadingPosition(index: Int, savedOffsetPx: Int? = null) {
    scrollToItem(index)
    val layout = layoutInfo
    val item = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val desiredTop = layout.viewportStartOffset + (savedOffsetPx?.let {
        -it.coerceIn(1 - layout.viewportSize.height.coerceAtLeast(1), (item.size - 1).coerceAtLeast(0))
    } ?: readingPositionOffset(layout.viewportSize.height, item.size))
    scrollBy((item.offset - desiredTop).toFloat())
}
