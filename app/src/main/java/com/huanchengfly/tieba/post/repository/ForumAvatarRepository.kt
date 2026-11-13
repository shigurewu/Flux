package com.huanchengfly.tieba.post.repository

import androidx.compose.runtime.*
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.retrofit.getData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** Forum identity is public. Coalesce missing-avatar lookups and cache failures briefly. */
private object ForumAvatarRepository {
    data class Entry(val avatar: String?, val expires: Long)
    private val cache = ConcurrentHashMap<String, Entry>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    suspend fun get(name: String): String? = locks.getOrPut(name) { Mutex() }.withLock {
        cache[name]?.takeIf { it.expires > System.currentTimeMillis() }?.let { return@withLock it.avatar }
        val avatar = try { TiebaApi.getInstance().forumPageAsync(name).getData().forum?.avatar }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { null }
        if (cache.size > 300) cache.clear()
        cache[name] = Entry(avatar, System.currentTimeMillis() + if (avatar.isNullOrBlank()) 30_000 else 600_000)
        avatar
    }
}

@Composable
internal fun rememberForumAvatar(name: String, supplied: String?): String? {
    val avatar by produceState(supplied, name, supplied) {
        value = supplied?.takeIf { it.isNotBlank() } ?: name.takeIf { it.isNotBlank() }?.let { ForumAvatarRepository.get(it) }
    }
    return avatar?.let { if (it.startsWith("//")) "https:$it" else it }
}
