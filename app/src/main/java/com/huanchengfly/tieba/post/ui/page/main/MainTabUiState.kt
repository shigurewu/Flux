package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 底栏需要感知的内容状态。
 *
 * 「动态」标签：列表不在顶部且当前就在动态页时，图标替换为刷新圆圈，
 * 点击后回到顶部并刷新；离开动态页或回到顶部后恢复普通图标。
 */
object MainTabUiState {
    var exploreAtTop by mutableStateOf(true)

    /** 当前选中的底栏标签 id，避免在别的页面也显示刷新/回顶图标 */
    var currentTabId by mutableStateOf("home")

    /** 重复点击底栏的次数，用于图标旋转反馈 */
    var reselectTick by mutableIntStateOf(0)
}
