package com.huanchengfly.tieba.post.ui.page.user.post

import android.content.Context

/**
 * 本地记录「仅自己可见」的帖子。
 *
 * 用户帖子列表接口的 hide_post 字段长期为 0，无法可靠反映真实可见性，
 * 这里用 SharedPreferences 做本地兜底，保证切换后离开页面再回来仍能显示正确状态。
 */
object ThreadPrivacyCache {
    private const val PREF_NAME = "thread_privacy_cache"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isHidden(context: Context, threadId: Long): Boolean =
        prefs(context).getBoolean(threadId.toString(), false)

    fun setHidden(context: Context, threadId: Long, hidden: Boolean) {
        prefs(context).edit().putBoolean(threadId.toString(), hidden).apply()
    }

    fun remove(context: Context, threadId: Long) {
        prefs(context).edit().remove(threadId.toString()).apply()
    }
}
