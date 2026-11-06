package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 轻量下拉刷新状态。
 *
 * 自己接管嵌套滚动的位移，避免 Material pullRefresh 与指示器各自做一套动画造成位移叠加：
 * - 下拉时列表不位移，只有指示器按 [offsetPx] 跟随手指；
 * - 反向上滑时先用位移抵消下拉，再交给列表滚动（[cancelDrag]）；
 * - 松手后只有一套回弹动画（[release] / [finishRefresh]）。
 */
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

    private var settleJob: Job? = null

    /** 0f~1f，达到 1 即到达触发阈值 */
    val progress: Float
        get() = (offsetPx / thresholdPx).coerceIn(0f, 1f)

    val isReady: Boolean
        get() = offsetPx >= thresholdPx

    private val settleSpec = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = Spring.StiffnessMediumLow
    )

    internal fun dragBy(delta: Float) {
        settleJob?.cancel()
        offsetPx = (offsetPx + delta).coerceIn(0f, maxOffsetPx)
    }

    /** 反向（上滑）时先吃掉下拉位移，返回被消费的负值 */
    internal fun cancelDrag(delta: Float): Float {
        if (offsetPx <= 0f) return 0f
        settleJob?.cancel()
        val consumed = minOf(offsetPx, -delta)
        offsetPx -= consumed
        return -consumed
    }

    internal fun release() {
        settleJob?.cancel()
        settleJob = scope.launch {
            if (isReady) {
                animateTo(thresholdPx)
                if (!refreshing) onRefresh()
            } else {
                animateTo(0f)
            }
        }
    }

    internal fun finishRefresh() {
        settleJob?.cancel()
        settleJob = scope.launch { animateTo(0f) }
    }

    /**
     * 由底栏「动态」按钮触发的刷新：
     * 圆圈从顶部平滑拉下来到阈值位置，随后触发一次真实刷新。
     *
     * 与手动下拉共用同一份 offset / refreshing 状态，所以不会出现两套动画抢位移；
     * 被取消（再次点击、页面销毁）时会把圆圈收回，不会停在半空。
     */
    suspend fun animateRefresh() {
        if (refreshing) return
        settleJob?.cancel()
        try {
            animate(
                initialValue = offsetPx,
                targetValue = thresholdPx,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            ) { value, _ -> offsetPx = value }
            if (!refreshing) onRefresh()
        } catch (cancel: CancellationException) {
            finishRefresh()
            throw cancel
        }
    }

    internal fun setRefreshing(value: Boolean) {
        refreshing = value
    }

    private suspend fun animateTo(target: Float) {
        val from = offsetPx
        if (from == target) return
        animate(from, target, animationSpec = settleSpec) { value, _ -> offsetPx = value }
    }
}

@Composable
fun rememberGlassPullRefreshState(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    threshold: Dp = 72.dp,
    maxOffset: Dp = 150.dp,
): GlassPullRefreshState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val state = remember(threshold, maxOffset) {
        GlassPullRefreshState(
            thresholdPx = with(density) { threshold.toPx() },
            maxOffsetPx = with(density) { maxOffset.toPx() },
            scope = scope,
        )
    }
    state.onRefresh = onRefresh
    state.setRefreshing(refreshing)
    LaunchedEffect(refreshing) {
        if (!refreshing) state.finishRefresh()
    }
    return state
}

fun Modifier.glassPullRefresh(state: GlassPullRefreshState): Modifier =
    nestedScroll(state.nestedScrollConnection)

private val GlassPullRefreshState.nestedScrollConnection: NestedScrollConnection
    get() = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.Drag) return Offset.Zero
            // 上滑：先抵消尚未释放的下拉位移，剩余距离才交给列表
            if (available.y < 0f) {
                val consumed = cancelDrag(available.y)
                if (consumed != 0f) return Offset(0f, consumed)
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (source != NestedScrollSource.Drag) return Offset.Zero
            // 列表已经到顶、没有可滚动距离时，剩余下拉交给指示器（带阻尼）
            if (available.y > 0f && !refreshing && consumed.y <= 0.01f) {
                dragBy(available.y * 0.55f)
                return available
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offsetPx > 0f) release()
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (offsetPx > 0f) release()
            return Velocity.Zero
        }
    }



