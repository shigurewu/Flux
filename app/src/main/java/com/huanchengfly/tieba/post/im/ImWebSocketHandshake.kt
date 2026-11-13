package com.huanchengfly.tieba.post.im

import okhttp3.Interceptor
import okhttp3.Response
import java.net.ProtocolException

/**
 * Tieba uses the extension header for its application protocol version, without RFC 7692
 * compression. OkHttp owns this header and rejects it on the original WebSocket request.
 * This application interceptor runs after OkHttp builds the upgrade request. It is installed
 * only on the dedicated IM client; ordinary HTTP clients are not affected.
 */
internal object ImWebSocketHandshake : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.header("Upgrade").equals("websocket", ignoreCase = true)) {
            return chain.proceed(request)
        }
        val response = chain.proceed(request.newBuilder()
            .header("Sec-WebSocket-Extensions", ImConstants.HEADER_IM_VERSION)
            .build())
        // No compression was offered. Only acknowledge the known, framing-neutral version
        // marker; never hide an unknown extension or an unsolicited compression negotiation.
        if (response.code != 101) return response
        val extensions = response.headers.values("Sec-WebSocket-Extensions")
            .flatMap { it.split(',') }.map { it.trim() }.filter { it.isNotEmpty() }
        if (extensions.any { it != ImConstants.HEADER_IM_VERSION }) {
            response.close()
            throw ProtocolException("私信服务器返回了不支持的 WebSocket 扩展")
        }
        return response.newBuilder().removeHeader("Sec-WebSocket-Extensions").build()
    }
}
