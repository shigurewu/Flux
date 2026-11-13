package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import kotlin.math.abs

@Stable
class GlassPullRefreshState internal constructor(
    val thresholdPx: Float,
    val maxOffsetPx: Float,
    private val scope: CoroutineScope,
) {
    var offsetPx by mutableFloatStateOf(0f)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var onRefresh: () -> Unit = {}
    private val cycle = RefreshCycle()
    private var settling by mutableStateOf(false)
    private var job: Job? = null
    private var dragged = false
    val isBusy: Boolean get() = refreshing || cycle.inFlight || settling
    val progress: Float get() = (offsetPx / thresholdPx).coerceIn(0f, 1f)
    val isReady: Boolean get() = offsetPx >= thresholdPx

    internal fun dragBy(delta: Float) {
        if (refreshing || cycle.inFlight) return
        if (settling) { job?.cancel(); settling = false }
        offsetPx = (offsetPx + delta).coerceIn(0f, maxOffsetPx)
        dragged = true
    }
    internal fun cancelDrag(delta: Float): Float {
        if (refreshing || cycle.inFlight || offsetPx <= 0f || delta >= 0f) return 0f
        if (settling) { job?.cancel(); settling = false }
        val consumed = minOf(offsetPx, -delta)
        offsetPx -= consumed
        dragged = true
        return -consumed
    }
    internal fun release() {
        if (!dragged) return
        dragged = false
        if (isBusy) return
        if (isReady) requestRefresh() else finishRefresh()
    }
    /** Lock before launching the scroll/animation, including the wait for request state. */
    fun requestRefresh(beforeRefresh: suspend () -> Unit = {}) {
        if (isBusy || !cycle.tryStart()) return
        refreshing = true
        job?.cancel()
        job = scope.launch {
            try {
                beforeRefresh()
                animate(offsetPx, thresholdPx,
                    animationSpec = tween(280, easing = FastOutSlowInEasing)) { value, _ -> offsetPx = value }
                onRefresh()
            } catch (cancel: CancellationException) {
                cycle.cancel(); refreshing = false; offsetPx = 0f
                throw cancel
            } catch (error: Exception) {
                cycle.cancel(); refreshing = false; finishRefresh()
                throw error
            }
        }
    }
    suspend fun animateRefresh() { requestRefresh() }
    internal fun updateRequest(loading: Boolean, completed: Long) {
        val finished = cycle.observe(loading, completed)
        refreshing = loading || cycle.inFlight
        if (finished && !loading) finishRefresh()
    }
    internal fun finishRefresh() {
        if (refreshing || cycle.inFlight) return
        job?.cancel()
        settling = offsetPx > 0f
        job = scope.launch {
            try {
                animate(offsetPx, 0f, animationSpec = spring<Float>(
                    dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) { value, _ -> offsetPx = value }
                offsetPx = 0f
            } finally { settling = false }
        }
    }
    internal val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || abs(available.x) > abs(available.y)) return Offset.Zero
            return if (available.y < 0f) Offset(0f, cancelDrag(available.y)) else Offset.Zero
        }
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || refreshing || cycle.inFlight ||
                abs(available.x) > abs(available.y) || abs(consumed.x) > abs(available.y)) return Offset.Zero
            if (available.y > 0f && consumed.y <= 0.01f) {
                val before = offsetPx
                dragBy(available.y * 0.55f)
                return Offset(0f, ((offsetPx - before) / 0.55f).coerceAtMost(available.y))
            }
            return Offset.Zero
        }
        override suspend fun onPreFling(available: Velocity): Velocity {
            val pulled = dragged && offsetPx > 0f
            release()
            return if (pulled) Velocity(0f, available.y) else Velocity.Zero
        }
    }
}

@Composable
fun rememberGlassPullRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    threshold: Dp = 72.dp,
    maxOffset: Dp = 150.dp,
    refreshCompleted: Long = 0L,
): GlassPullRefreshState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val state = remember(threshold, maxOffset, density) {
        GlassPullRefreshState(with(density) { threshold.toPx() }, with(density) { maxOffset.toPx() }, scope)
    }
    state.onRefresh = onRefresh
    LaunchedEffect(refreshing, refreshCompleted) { state.updateRequest(refreshing, refreshCompleted) }
    return state
}
fun Modifier.glassPullRefresh(state: GlassPullRefreshState): Modifier = nestedScroll(state.connection)
