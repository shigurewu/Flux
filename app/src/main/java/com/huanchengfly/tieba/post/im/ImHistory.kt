package com.huanchengfly.tieba.post.im

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.huanchengfly.tieba.post.models.database.ImMessage
import java.io.IOException

/** Legacy history IDs are kept outside the live IM ID space; never send them as read receipts. */
internal const val HISTORY_GROUP_ID = -1L

internal data class ImHistoryPage(
    val messages: List<ImMessage>,
    val nextBefore: Long,
    val hasMore: Boolean,
    val unsupported: Int,
)

internal fun parseImHistory(json: String, owner: String, peer: Long, before: Long,
    privateValues: List<String> = emptyList()): ImHistoryPage {
    require(owner.toLongOrNull()?.let { it > 0 } == true && peer > 0 && before >= 0)
    try {
        val root = JsonParser.parseString(json).asJsonObject
        val code = root.long("error_code")
        if (code != 0L) throw historyError(code, root.get("error_msg"), privateValues)
        root.get("error")?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            if (it.long("errno") != 0L) throw historyError(it.long("errno"), it.get("errmsg"), privateValues)
        }
        val rows = root.get("message")?.takeIf { it.isJsonArray }?.asJsonArray
            ?: throw IOException("历史消息格式不匹配，未导入")
        val more = root.long("has_more")
        if (more !in 0L..1L) throw IOException("历史分页标记不匹配，未导入")
        var next = before
        var unsupported = 0
        val messages = rows.map { element ->
            val row = element.asJsonObject
            val id = row.long("msg_id")
            val time = row.long("time")
            val from = row.long("from")
            if (id <= 0 || time <= 0 || from !in 0L..1L || (before > 0 && id >= before)) {
                throw IOException("历史消息游标或发送方不匹配，未导入")
            }
            next = if (next == 0L) id else minOf(next, id)
            val content = row.get("content")?.takeIf { it.isJsonArray }?.asJsonArray
                ?: throw IOException("历史消息内容格式不匹配，未导入")
            // Only plain text and inline face tokens are supported. Never mistake image/voice URLs for text.
            val text = content.mapNotNull { part ->
                val obj = part.asJsonObject
                val type = obj.get("type")?.asString
                if (type != null && type != "0") null
                else obj.get("text")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
            }.joinToString("")
            if (text.isEmpty()) { unsupported++; null }
            else ImMessage(uid = owner, peerUid = peer, groupId = HISTORY_GROUP_ID,
                msgId = Long.MIN_VALUE + id, msgType = ImConstants.MSG_TYPE_TEXT,
                content = text, createTime = time, fromUid = if (from == 1L) owner.toLong() else peer,
                isMine = from == 1L)
        }.filterNotNull().distinctBy { it.msgId }
        if (more == 1L && (rows.isEmpty || next == before)) throw IOException("历史接口未返回更早的消息，请稍后重试")
        return ImHistoryPage(messages, next, more == 1L, unsupported)
    } catch (e: IOException) { throw e }
    catch (_: Exception) { throw IOException("历史消息格式不匹配，未导入") }
}

private fun JsonObject.long(name: String): Long = get(name)?.takeIf { it.isJsonPrimitive }
    ?.asString?.toLongOrNull() ?: throw IOException("历史消息缺少有效字段：$name")

private fun historyError(code: Long, value: com.google.gson.JsonElement?, privateValues: List<String>): IOException {
    var detail = value?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString.orEmpty()
    // Only show the error explanation, never the response or request. Mask known credentials first.
    privateValues.filter { it.isNotEmpty() }.sortedByDescending { it.length }.forEach {
        detail = detail.replace(it, "[已隐藏]")
    }
    detail = detail.replace(Regex("[\\p{Cc}\\p{Cf}]"), " ")
        .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "[链接已隐藏]")
        .replace(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[邮箱已隐藏]")
        .replace(Regex("[A-Za-z0-9_%+./=-]{24,}"), "[已隐藏]")
        .replace(Regex("\\s+"), " ").trim().take(160)
    val title = if (code == 1L) "历史接口未接受当前登录态（1）" else "历史接口返回错误（$code）"
    return IOException(title + if (detail.isNotEmpty()) "：$detail" else "；服务端未提供具体说明")
}

/** Only hide a cross-protocol duplicate if its ID, sender, timestamp and text all agree. */
internal fun visibleImMessages(messages: List<ImMessage>): List<ImMessage> {
    val live = messages.filter { it.msgId > 0 }.associateBy { it.msgId }
    return messages.filter { message ->
        if (message.groupId != HISTORY_GROUP_ID || message.msgId >= 0) true
        else {
            val match = live[message.msgId - Long.MIN_VALUE]
            match == null || match.uid != message.uid || match.peerUid != message.peerUid ||
                match.fromUid != message.fromUid || match.createTime != message.createTime || match.content != message.content
        }
    }
}
