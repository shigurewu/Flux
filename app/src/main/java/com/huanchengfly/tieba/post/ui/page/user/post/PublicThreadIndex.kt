package com.huanchengfly.tieba.post.ui.page.user.post

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

internal suspend fun loadPublicThreadIndex(uid: Long): Set<Long> = collectPublicThreadIds { page ->
    if (AccountUtil.currentAccount?.uid != uid.toString()) throw CancellationException("账号已切换")
    val response = TiebaApi.getInstance().userPostFlow(uid, page, isThread = true, publicOnly = true).first()
    check((response.error?.error_code ?: 0) == 0) { response.error?.error_msg ?: "公开范围同步失败" }
    val data = checkNotNull(response.data_) { "公开列表返回为空，无法确认可见范围" }
    data.post_list.map { it.thread_id }
}
