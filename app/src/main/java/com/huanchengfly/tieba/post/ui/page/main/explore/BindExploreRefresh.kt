package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.ui.page.main.LocalTabReselect
import com.huanchengfly.tieba.post.ui.page.main.MainTabUiState
import com.huanchengfly.tieba.post.ui.widgets.compose.GlassPullRefreshState

val LocalExploreFeedActive = staticCompositionLocalOf { false }

/** One visible feed handles every reselect/global trigger; offscreen pager pages only acknowledge ticks. */
@Composable
internal fun BindExploreRefresh(id: String, state: GlassPullRefreshState, list: LazyListState) {
    val active = LocalExploreFeedActive.current && MainTabUiState.currentTabId == "explore"
    val currentActive by rememberUpdatedState(active)
    DisposableEffect(active, state) {
        if (active) MainTabUiState.exploreRefreshState = state
        onDispose {
            if (MainTabUiState.exploreRefreshState === state) MainTabUiState.exploreRefreshState = null
        }
    }
    fun refresh() { state.requestRefresh { list.animateScrollToItem(0) } }
    onGlobalEvent<GlobalEvent.Refresh>(filter = { it.key == id && currentActive }) { refresh() }
    val signal = LocalTabReselect.current
    var handled by remember { mutableIntStateOf(signal.tick) }
    LaunchedEffect(signal.tick, signal.tabId, active) {
        if (signal.tick == handled) return@LaunchedEffect
        handled = signal.tick
        if (active && signal.tabId == "explore") refresh()
    }
    LaunchedEffect(active, list) {
        if (active) snapshotFlow {
            list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0
        }.collect { MainTabUiState.exploreAtTop = it }
    }
}
