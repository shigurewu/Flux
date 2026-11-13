package com.huanchengfly.tieba.post.ui.page.user.followlist

import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.io.IOException

/** Keep failures distinguishable without printing response bodies or login credentials. */
internal fun fansErrorDetail(error: Throwable?): String {
    if (error == null) return "粉丝加载失败，请重试"
    val reason = when (error) {
        is HttpException -> "服务器返回 HTTP ${error.code()}"
        is JsonParseException -> "粉丝数据格式不匹配"
        is IOException -> error.message.orEmpty()
            .replace(Regex("(?i)(BDUSS|STOKEN|COOKIE|AUTHORIZATION)\\s*[:=]\\s*[^\\s,;]+"), "$1=***")
            .take(160).ifBlank { "网络请求失败" }
        else -> "请求或解析异常"
    }
    val location = error.stackTrace.firstOrNull { it.className.startsWith("com.huanchengfly.tieba.post.") }
        ?.let { " · ${it.fileName}:${it.lineNumber}" }.orEmpty()
    return "$reason（${error.javaClass.simpleName}）$location"
}
