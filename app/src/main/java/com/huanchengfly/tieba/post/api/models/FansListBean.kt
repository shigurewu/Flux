package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName
import java.io.IOException

/** /c/u/fans/page uses user_list + nested page, unlike followList. */
data class FansListBean(
    @SerializedName("error_code") val errorCode: Int = 0,
    @SerializedName("error_msg") val errorMsg: String? = null,
    @SerializedName("user_list") val users: List<Fan?>? = null,
    val page: Page? = null,
) {
    // The fans endpoint is not the followList endpoint. Ignore unrelated metadata whose
    // JSON shape differs between them (e.g. an empty array instead of a metadata object).
    data class Fan(
        val id: Long = 0L,
        val name: String? = null,
        @SerializedName("name_show") val nameShow: String? = null,
        val portrait: String? = null,
    )
    data class Page(
        @SerializedName("current_page") val currentPage: Int = 0,
        @SerializedName("total_count") val totalCount: Int = 0,
        @SerializedName("has_more") val hasMore: Int = 0,
    )

    fun toFollowListBean(): FollowListBean {
        if (errorCode != 0) throw IOException(errorMsg?.takeIf { it.isNotBlank() } ?: "粉丝列表加载失败（$errorCode）")
        val pageInfo = page ?: throw IOException("粉丝列表缺少分页信息，请稍后重试")
        val entries = users ?: throw IOException("粉丝列表响应不完整，请稍后重试")
        if (pageInfo.currentPage < 1) {
            throw IOException("粉丝列表数据无效，请稍后重试")
        }
        val availableUsers = entries.filterNotNull().filter { it.id > 0L }.distinctBy { it.id }
        if (entries.isNotEmpty() && availableUsers.isEmpty()) {
            throw IOException("本页粉丝账号暂不可用，请稍后重试")
        }
        return FollowListBean(
            pageNum = pageInfo.currentPage,
            hasMore = pageInfo.hasMore,
            totalFollowNum = pageInfo.totalCount,
            followList = availableUsers.map {
                FollowListBean.FollowUserBean(id = it.id, name = it.name, nameShow = it.nameShow, portrait = it.portrait)
            },
        )
    }
}
