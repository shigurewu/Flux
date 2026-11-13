package com.huanchengfly.tieba.post.im

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Assert.*
import org.junit.Test
import java.io.Closeable
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ImWebSocketHandshakeTest {
    @Test fun originalHeaderIsRejectedBeforeNetwork() {
        val client = OkHttpClient()
        val error = CompletableFuture<Throwable>()
        client.newWebSocket(Request.Builder().url("ws://127.0.0.1:1")
            .header("Sec-WebSocket-Extensions", ImConstants.HEADER_IM_VERSION).build(),
            object : WebSocketListener() {
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { error.complete(t) }
            })
        try { assertTrue(error.get(5, TimeUnit.SECONDS).message.orEmpty().contains("Request header not permitted")) }
        // OkHttp never creates a Call after this preflight rejection; cancel() itself
        // dereferences that missing Call in 5.3.2. No socket exists to cancel here.
        finally { client.dispatcher.executorService.shutdownNow() }
    }

    @Test fun versionMarkerAndBinaryMessagesWork() = checkUpgrade(ImConstants.HEADER_IM_VERSION)
    @Test fun serverWithoutExtensionAndBinaryMessagesWork() = checkUpgrade(null)

    private fun checkUpgrade(extension: String?) {
        Peer(extension).use { peer ->
            val client = OkHttpClient.Builder().addInterceptor(ImWebSocketHandshake).build()
            val received = CompletableFuture<ByteArray>()
            val ws = client.newWebSocket(Request.Builder().url(peer.url).build(), object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) { received.complete(bytes.toByteArray()) }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { received.completeExceptionally(t) }
            })
            try {
                assertArrayEquals(byteArrayOf(1, 2, 3), received.get(5, TimeUnit.SECONDS))
                val headers = peer.headers.get(5, TimeUnit.SECONDS)
                assertEquals(ImConstants.HEADER_IM_VERSION, headers["sec-websocket-extensions"])
                assertFalse(headers["sec-websocket-extensions"].orEmpty().contains("permessage-deflate"))
            } finally { ws.cancel(); client.dispatcher.executorService.shutdownNow() }
        }
    }

    @Test fun unsolicitedCompressionIsRejected() {
        Peer("permessage-deflate").use { peer ->
            val client = OkHttpClient.Builder().addInterceptor(ImWebSocketHandshake).build()
            val failure = CompletableFuture<Throwable>()
            val ws = client.newWebSocket(Request.Builder().url(peer.url).build(), object : WebSocketListener() {
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { failure.complete(t) }
            })
            try { assertTrue(failure.get(5, TimeUnit.SECONDS).message.orEmpty().contains("不支持的 WebSocket 扩展")) }
            finally { ws.cancel(); client.dispatcher.executorService.shutdownNow() }
        }
    }

    @Test fun realConnectionPairsRequestAndResponse(): Unit = runBlocking {
        Peer(null, replyToRequest = true).use { peer ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val connection = ImConnection(scope, peer.url)
            try {
                withTimeout(5000) {
                    connection.connect()
                    assertEquals(ImConnectionState.Connected, connection.state.value)
                    assertArrayEquals(byteArrayOf(9, 8), connection.request(1001, byteArrayOf(7), encrypt = false))
                }
                peer.completed.get(5, TimeUnit.SECONDS)
            } finally { connection.close(); scope.cancel() }
        }
    }

    /** Real TCP upgrade and uncompressed binary frames; no mock of OkHttp internals. */
    private class Peer(extension: String?, replyToRequest: Boolean = false) : Closeable {
        private val server = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        private val executor = Executors.newSingleThreadExecutor()
        private val release = CountDownLatch(1)
        private var socket: Socket? = null
        val headers = CompletableFuture<Map<String, String>>()
        val completed = CompletableFuture<Unit>()
        val url = "ws://127.0.0.1:${server.localPort}/"
        init {
            executor.submit {
                try {
                    server.accept().use { peer ->
                        socket = peer
                        peer.soTimeout = 5000
                        val input = peer.getInputStream()
                        val raw = StringBuilder()
                        while (!raw.endsWith("\r\n\r\n")) {
                            val next = input.read()
                            check(next >= 0) { "Upgrade ended early" }
                            raw.append(next.toChar())
                        }
                        val values = raw.toString().split("\r\n").drop(1).filter { ':' in it }
                            .associate { it.substringBefore(':').lowercase() to it.substringAfter(':').trim() }
                        headers.complete(values)
                        val accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                            .digest((values.getValue("sec-websocket-key") + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))
                        val output = peer.getOutputStream()
                        output.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n" +
                            "Sec-WebSocket-Accept: $accept\r\n" +
                            (extension?.let { "Sec-WebSocket-Extensions: $it\r\n" } ?: "") + "\r\n").toByteArray())
                        val payload = if (replyToRequest) {
                            assertEquals(0x82, input.read())
                            val length = input.read()
                            assertTrue(length and 0x80 != 0)
                            val mask = input.readNBytes(4)
                            val body = input.readNBytes(length and 0x7f)
                            body.indices.forEach { body[it] = (body[it].toInt() xor mask[it % 4].toInt()).toByte() }
                            val request = ImFrame.parse(body, null)
                            assertEquals(1001, request.cmd)
                            assertArrayEquals(byteArrayOf(7), request.payload)
                            ImFrame.pack(byteArrayOf(9, 8), request.cmd, request.reqId, null, encrypt = false)
                        } else byteArrayOf(1, 2, 3)
                        output.write(byteArrayOf(0x82.toByte(), payload.size.toByte()) + payload)
                        output.flush()
                        completed.complete(Unit)
                        release.await(5, TimeUnit.SECONDS)
                    }
                } catch (t: Throwable) { headers.completeExceptionally(t); completed.completeExceptionally(t) }
            }
        }
        override fun close() { release.countDown(); socket?.close(); server.close(); executor.shutdownNow() }
    }
}
