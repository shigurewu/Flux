package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.models.BaseBean

/**
 * 「赞我的」消息（POST /c/u/feed/agreeme）
 *
 * 与「回复我的 / 提到我的」不是同一套结构：这里返回的是 agree_list，
 * 每项包含点赞人 agreeer、所在主题 thread_info、被赞回复 post_info。
 */
class AgreeListBean : BaseBean() {
    @SerializedName("error_code")
    val errorCode: String? = null

    @SerializedName("has_more")
    val hasMore: String? = null

    @SerializedName("agree_list")
    val agreeList: List<AgreeItem>? = null

    class AgreeItem {
        @SerializedName("agreeer")
        val agreeer: UserInfo? = null

        @SerializedName("id")
        val id: Long = 0

        @SerializedName("thread_info")
        val threadInfo: ThreadInfo? = null

        @SerializedName("post_info")
        val postInfo: PostInfo? = null

        /** 1=回复 2=楼中楼 3=主题 */
        @SerializedName("type")
        val type: Int = 0

        @SerializedName("time")
        val time: String? = null
    }

    class UserInfo {
        @SerializedName("id")
        val id: String? = null

        @SerializedName("name")
        val name: String? = null

        @SerializedName("name_show")
        val nameShow: String? = null

        @SerializedName("portrait")
        val portrait: String? = null
    }

    class ThreadInfo {
        @SerializedName("fname")
        val fname: String? = null

        @SerializedName("fid")
        val fid: Long = 0

        @SerializedName("id")
        val id: Long = 0

        @SerializedName("title")
        val title: String? = null
    }

    class PostInfo {
        @SerializedName("id")
        val id: Long = 0

        @SerializedName("content")
        val content: List<Fragment>? = null
    }

    class Fragment {
        @SerializedName("text")
        val text: String? = null
    }
}
