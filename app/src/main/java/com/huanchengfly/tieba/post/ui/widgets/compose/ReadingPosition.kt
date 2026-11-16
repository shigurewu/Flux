package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs

internal fun readingPositionOffset(viewportHeight: Int, itemHeight: Int): Int =
    ((viewportHeight - itemHeight).coerceAtLeast(0) * 0.3f).toInt()

internal suspend fun LazyListState.scrollToReadingPosition(
    index: Int,
    savedOffsetPx: Int? = null,
    animated: Boolean = false,
) {
    fun topOffset(viewportHeight: Int, itemHeight: Int): Int = savedOffsetPx?.let {
        -it.coerceIn(1 - viewportHeight.coerceAtLeast(1), (itemHeight - 1).coerceAtLeast(0))
    } ?: readingPositionOffset(viewportHeight, itemHeight)

    if (animated) {
        animateCommentJump(index) { viewportHeight, itemHeight -> topOffset(viewportHeight, itemHeight) }
        return
    }

    scrollToItem(index)
    val layout = layoutInfo
    val item = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val desiredTop = layout.viewportStartOffset + topOffset(layout.viewportSize.height, item.size)
    scrollBy((item.offset - desiredTop).toFloat())
}

internal suspend fun LazyListState.animateCommentJump(
    index: Int,
    scrollOffset: Int = 0,
    topOffset: ((viewportHeight: Int, itemHeight: Int) -> Int)? = null,
) {
    val viewportHeight = layoutInfo.viewportSize.height.coerceAtLeast(1)
    val cruiseSpeed = viewportHeight * 12f
    var needsFallback = false
    scroll {
        var previousFrame = withFrameNanos { it }
        var elapsedSeconds = 0f
        var previousDirection = 0
        while (true) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            previousFrame = frame
            if (seconds == 0f) continue
            elapsedSeconds += seconds

            val layout = layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.index == index }
            val remaining = if (item != null) {
                val targetTop = topOffset?.invoke(viewportHeight, item.size) ?: -scrollOffset
                (item.offset - layout.viewportStartOffset - targetTop).toFloat()
            } else if (index <= firstVisibleItemIndex) {
                Float.NEGATIVE_INFINITY
            } else if (index > (layout.visibleItemsInfo.lastOrNull()?.index ?: firstVisibleItemIndex)) {
                Float.POSITIVE_INFINITY
            } else {
                // A zero-height list item can be skipped by visibleItemsInfo.
                needsFallback = true
                break
            }

            if (remaining == 0f || (remaining < 0f && !canScrollBackward) ||
                (remaining > 0f && !canScrollForward)
            ) break

            val direction = if (remaining < 0f) -1 else 1
            if (item == null && previousDirection != 0 && direction != previousDirection) {
                needsFallback = true
                break
            }
            previousDirection = direction

            val speed = cruiseSpeed * (elapsedSeconds / 0.10f).coerceAtMost(1f)
            val approachSpeed = (abs(remaining) * 20f).coerceAtLeast(viewportHeight * 0.75f)
            val step = minOf(abs(remaining), minOf(speed, approachSpeed) * seconds)
            scrollBy(direction * step)
        }
    }
    if (needsFallback) animateScrollToItem(index, scrollOffset)
}
