package com.huanchengfly.tieba.post.api.retrofit.interfaces

import com.huanchengfly.tieba.post.api.Header
import com.huanchengfly.tieba.post.api.models.MemeResponse
import com.huanchengfly.tieba.post.api.models.Pic2IdResponse
import kotlinx.coroutines.flow.Flow
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface OfficialMemeApi {
    /**
     * 拉取全量云表情。
     *
     * 注意：`/c/e/meme/` 系接口要求 `cmd` 作为表单参数参与签名（仅放 URL query 会返回
     * error_code=110001），因此所有接口都显式携带 cmd（URL query 中同样保留，两者值一致）。
     */
    @Headers("${Header.FORCE_LOGIN}: ${Header.FORCE_LOGIN_TRUE}")
    @POST("/c/e/meme/getAllMeme?cmd=1003351")
    @FormUrlEncoded
    fun getAllMemeFlow(@Field("cmd") cmd: String): Flow<MemeResponse>

    /**
     * 图片 URL 转表情 pic_id（收藏前必须调用，否则 collect 会因未知 pic_id 失败）。
     *
     * @param cmd 固定 1003353（必须参与签名，否则服务端返回 110001）
     * @param picUrls JSON 数组字符串，如 `["http://.../xxx.jpg"]`
     */
    @Headers("${Header.FORCE_LOGIN}: ${Header.FORCE_LOGIN_TRUE}")
    @POST("/c/e/meme/pic2idJson?cmd=1003353")
    @FormUrlEncoded
    fun pic2IdFlow(
        @Field("cmd") cmd: String,
        @Field("pic_urls") picUrls: String,
    ): Flow<Pic2IdResponse>

    @Headers("${Header.FORCE_LOGIN}: ${Header.FORCE_LOGIN_TRUE}")
    @POST("/c/e/meme/collect?cmd=1003339")
    @FormUrlEncoded
    fun collectFlow(
        @Field("cmd") cmd: String,
        @Field("pics") pics: String,
        @Field("pic_update_time") picUpdateTime: Long,
    ): Flow<MemeResponse>

    @Headers("${Header.FORCE_LOGIN}: ${Header.FORCE_LOGIN_TRUE}")
    @POST("/c/e/meme/collect?cmd=1003532")
    @FormUrlEncoded
    fun collectDiyFlow(
        @Field("cmd") cmd: String,
        @Field("pics") pics: String,
        @Field("is_diy") isDiy: Int,
        @Field("diy_pic_update_time") diyPicUpdateTime: Long,
    ): Flow<MemeResponse>
}
