package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemeResponse(
    @SerialName("error_code")
    @SerializedName("error_code")
    val errorCode: Int = 0,
    @SerialName("error_msg")
    @SerializedName("error_msg")
    val errorMsg: String = "",
    @SerialName("data")
    @SerializedName("data")
    val data: MemeData? = null,
)

@Serializable
data class MemeData(
    @SerialName("pic_update_time")
    @SerializedName("pic_update_time")
    val picUpdateTime: Long = 0,
    @SerialName("diy_pic_update_time")
    @SerializedName("diy_pic_update_time")
    val diyPicUpdateTime: Long = 0,
    @SerialName("pkg_update_time")
    @SerializedName("pkg_update_time")
    val pkgUpdateTime: Long = 0,
    @SerialName("pic_ids")
    @SerializedName("pic_ids")
    val picIds: List<MemePic> = emptyList(),
    @SerialName("diy_pic_ids")
    @SerializedName("diy_pic_ids")
    val diyPicIds: List<MemePic> = emptyList(),
    @SerialName("package_ids")
    @SerializedName("package_ids")
    val packageIds: String = "",
)

@Serializable
data class MemePic(
    @SerialName("pic_id")
    @SerializedName("pic_id")
    val picId: String,
    @SerialName("pic_url")
    @SerializedName("pic_url")
    val picUrl: String = "",
    @SerialName("thumbnail")
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
    @SerialName("width")
    @SerializedName("width")
    val width: Int = 0,
    @SerialName("height")
    @SerializedName("height")
    val height: Int = 0,
    @SerialName("pck_id")
    @SerializedName("pck_id")
    val pckId: String? = null,
)

/**
 * 图片 URL 转表情 ID（`c/e/meme/pic2idJson`，cmd=1003353）的响应。
 *
 * 官方解析时直接在响应顶层取 `pics` 数组，这里同时兼容 `data.pics` 结构。
 */
@Serializable
data class Pic2IdResponse(
    @SerialName("error_code")
    @SerializedName("error_code")
    val errorCode: Int = 0,
    @SerialName("error_msg")
    @SerializedName("error_msg")
    val errorMsg: String = "",
    @SerialName("pics")
    @SerializedName("pics")
    val pics: List<MemePic> = emptyList(),
    @SerialName("data")
    @SerializedName("data")
    val data: Pic2IdData? = null,
) {
    /** 实际的表情信息列表（优先顶层 `pics`，其次 `data.pics`） */
    val emotionPics: List<MemePic>
        get() = pics.ifEmpty { data?.pics.orEmpty() }
}

@Serializable
data class Pic2IdData(
    @SerialName("pics")
    @SerializedName("pics")
    val pics: List<MemePic> = emptyList(),
)
