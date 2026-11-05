package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.runtime.compositionLocalOf

/**
 * 再次点击当前底栏标签时发出的信号。
 *
 * tick 每次自增，页面用 LaunchedEffect(tick) 触发自己的刷新逻辑，
 * 不依赖 GlobalEvent 的转发链，避免嵌套页面收不到事件。
 */
data class TabReselectSignal(
    val tabId: String = "",
    val tick: Int = 0,
)

val LocalTabReselect = compositionLocalOf { TabReselectSignal() }
