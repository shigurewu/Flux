package com.huanchengfly.tieba.post.im

import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ClientUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Explicit, read-only legacy query. No global account interceptor, logging, redirects or automatic fallback. */
internal object ImHistoryApi {
    private val client = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(owner: String, peer: Long, before: Long): ImHistoryPage {
        check(PrivateMessaging.enabled) { "私信功能暂未开放" }
        val account = AccountUtil.currentAccount?.takeIf { it.uid == owner && it.bduss.isNotBlank() }
            ?: throw CancellationException("账号已切换或退出")
        val credential = account.bduss
        val request = buildImHistoryRequest(owner, peer, before, ClientUtils.clientId.orEmpty(), credential)
        val json = suspendCancellableCoroutine<String> { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(IOException("历史消息请求失败，请检查网络后重试")))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val text = response.use {
                            if (!it.isSuccessful) throw IOException("历史接口 HTTP ${it.code}")
                            val data = it.body ?: throw IOException("历史接口返回空响应")
                            // Bound allocation even for chunked responses.
                            val source = data.source()
                            if (source.request(2L * 1024 * 1024 + 1)) throw IOException("历史响应过大，未导入")
                            source.readUtf8()
                        }
                        continuation.resumeWith(Result.success(text))
                    } catch (_: Exception) {
                        continuation.resumeWith(Result.failure(IOException("历史接口响应异常，未导入")))
                    }
                }
            })
        }
        if (AccountUtil.currentAccount?.uid != owner) throw CancellationException("账号已切换或退出")
        return withContext(Dispatchers.Default) {
            try { parseImHistory(json, owner, peer, before, listOf(credential)) }
            catch (e: IOException) {
                val stage = if (before == 0L) "最近消息初始化失败" else "更早历史读取失败"
                throw IOException("$stage：${e.message}")
            }
        }
    }
}
