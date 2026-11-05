package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.MemePic
import com.huanchengfly.tieba.post.models.database.MemeEmoticon
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DatabaseUtil
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.MD5Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * 贴吧个人表情包（收藏 / DIY）业务仓库。
 *
 * 协议要点：
 * - 拉取：`c/e/meme/getAllMeme`（cmd=1003351），返回收藏与 DIY 的全量数据
 * - 上传：`c/e/meme/collect`（收藏 cmd=1003339 / DIY cmd=1003532），全量覆盖，空列表传 "all_delete"
 * - 本地 sharpText：`#(meme,{collect_|diy_}{pkgId_}{picId},{w},{h},{md5})`
 * - 发送格式：官方客户端发帖前会将 `meme,collect_` / `meme,diy_` 替换为 `meme,`（lf6.c），
 *   即服务端只接受 `#(meme,{pkgId_}{picId},{w},{h},{md5})`（无类型前缀），md5 不变
 *   其中 md5 = MD5("meme,{idPart},{w},{h},7S6wbXjEKL9N").toLowerCase()
 */
object MemeRepository {
    private const val SIGN_SALT = "7S6wbXjEKL9N"
    private const val MEME_PREFIX = "#(meme,"

    /** 生成表情的本地标识（与 [MemeEmoticon] 的 type/pkgId/picId 对应），用于渲染时查找 URL */
    fun sharpKeyOf(m: MemeEmoticon): String {
        val idPart = idPartOf(m)
        return if (m.type == MemeEmoticon.TYPE_DIY) "diy_$idPart" else "collect_$idPart"
    }

    /** 构造发送用的表情文本（混入帖子 content 字段，无 collect_/diy_ 前缀） */
    fun buildSharpText(m: MemeEmoticon): String {
        val idPart = idPartOf(m)
        val md5 = MD5Util.toMd5("meme,$idPart,${m.width},${m.height},$SIGN_SALT").lowercase()
        return "$MEME_PREFIX$idPart,${m.width},${m.height},$md5)"
    }

    /** 当前账号的表情列表 Flow（收集时自动刷新 EmoticonManager 内存映射） */
    fun memeFlow(uid: String): Flow<List<MemeEmoticon>> =
        DatabaseUtil.getMemeEmoticonFlow(uid)
            .onEach { EmoticonManager.updateMemeEmoticons(it) }

    /** 启动 / 切号时从本地数据库载入表情到内存映射 */
    suspend fun ensureLoaded() {
        val uid = AccountUtil.getUid() ?: return
        runCatching {
            EmoticonManager.updateMemeEmoticons(DatabaseUtil.getMemeEmoticons(uid))
        }
    }

    /** 从云端拉取全量表情并覆盖本地 */
    suspend fun sync(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val data = TiebaApi.getInstance().getAllMemeFlow().first().data
                ?: return@runCatching
            val items = buildList {
                data.picIds.forEachIndexed { index, pic ->
                    add(pic.toMemeEmoticon(uid, MemeEmoticon.TYPE_COLLECT, index))
                }
                data.diyPicIds.forEachIndexed { index, pic ->
                    add(pic.toMemeEmoticon(uid, MemeEmoticon.TYPE_DIY, index))
                }
            }
            DatabaseUtil.replaceMemeEmoticons(uid, items)
            EmoticonManager.updateMemeEmoticons(DatabaseUtil.getMemeEmoticons(uid))
        }
    }

    /**
     * 添加收藏表情：先经 `c/e/meme/pic2idJson` 将图片 URL 换取表情 pic_id
     * （直接提交帖子图片的 picId 会因服务端不认识而失败），
     * 再上传（含新条目的全量列表），成功后落库。
     */
    suspend fun addMeme(
        uid: String,
        picUrl: String,
        thumbnail: String?,
        width: Int,
        height: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val picInfo = TiebaApi.getInstance().pic2IdFlow(listOf(picUrl)).first()
                .emotionPics.firstOrNull()
                ?: throw IllegalStateException("获取表情信息失败")
            val item = MemeEmoticon(
                uid = uid,
                picId = picInfo.picId,
                picUrl = picInfo.picUrl.ifEmpty { picUrl },
                thumbnail = picInfo.thumbnail ?: thumbnail,
                width = picInfo.width.takeIf { it > 0 } ?: width,
                height = picInfo.height.takeIf { it > 0 } ?: height,
                type = MemeEmoticon.TYPE_COLLECT,
                orderId = -1,
            )
            val current = DatabaseUtil.getMemeEmoticons(uid, MemeEmoticon.TYPE_COLLECT)
            val newList = listOf(item) + current.filterNot { it.picId == item.picId }
            uploadList(uid, MemeEmoticon.TYPE_COLLECT, newList)
            DatabaseUtil.upsertMemeEmoticon(item)
            EmoticonManager.updateMemeEmoticons(DatabaseUtil.getMemeEmoticons(uid))
        }
    }

    /** 删除表情：先上传（不含被删条目的全量列表），成功后删除本地 */
    suspend fun removeMeme(uid: String, picId: String, type: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val current = DatabaseUtil.getMemeEmoticons(uid, type)
                val newList = current.filterNot { it.picId == picId }
                uploadList(uid, type, newList)
                DatabaseUtil.deleteMemeEmoticon(uid, picId, type)
                EmoticonManager.updateMemeEmoticons(DatabaseUtil.getMemeEmoticons(uid))
            }
        }

    private suspend fun uploadList(uid: String, type: Int, items: List<MemeEmoticon>) {
        val picIds = items.map { it.picId }
        if (type == MemeEmoticon.TYPE_DIY) {
            TiebaApi.getInstance().collectDiyMemeFlow(picIds).first()
        } else {
            TiebaApi.getInstance().collectMemeFlow(picIds).first()
        }
    }

    private fun idPartOf(m: MemeEmoticon): String =
        if (m.pkgId.isEmpty()) m.picId else "${m.pkgId}_${m.picId}"

    private fun MemePic.toMemeEmoticon(uid: String, type: Int, orderId: Int) = MemeEmoticon(
        uid = uid,
        picId = picId,
        pkgId = pckId.takeIf { !it.isNullOrEmpty() && it != "0" }.orEmpty(),
        picUrl = picUrl,
        thumbnail = thumbnail,
        width = width,
        height = height,
        type = type,
        orderId = orderId,
    )
}
