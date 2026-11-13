package com.huanchengfly.tieba.post.im

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest

/** Match the legacy client's recent -> earlier-history sequence and query signing contract. */
internal fun buildImHistoryRequest(owner: String, peer: Long, before: Long, clientId: String, credential: String): Request {
    require(owner.toLongOrNull()?.let { it > 0 } == true && peer > 0 && before >= 0)
    require(credential.isNotBlank())
    val parameters = sortedMapOf(
        "_client_id" to clientId.ifBlank { "wappc_history" },
        "_client_type" to "2", "_client_version" to "4.2.7",
        "_phone_imei" to "000000000000000", "com_id" to peer.toString(),
        "msg_id" to before.toString(), "net_type" to "1", "user_id" to owner,
    )
    val raw = parameters.entries.joinToString("") { "${it.key}=${it.value}" } + "tiebaclient!!!"
    val sign = MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8))
        .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    val endpoint = if (before == 0L) "recentmsg" else "historymsg"
    val url = "https://tieba.baidu.com/c/s/$endpoint".toHttpUrl().newBuilder().apply {
        parameters.forEach { (key, value) -> addQueryParameter(key, value) }
        addQueryParameter("sign", sign)
    }.build()
    // Login travels in the Cookie header only, as in the reference browser client.
    return Request.Builder().url(url).header("Cookie", "BDUSS=$credential")
        .post(ByteArray(0).toRequestBody()).build()
}
