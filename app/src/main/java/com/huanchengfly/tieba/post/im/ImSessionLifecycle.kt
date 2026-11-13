package com.huanchengfly.tieba.post.im

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** A SharedFlow never completes: cancel its collector when the socket ends so reconnect can run. */
internal suspend fun <T> collectWhileSessionOpen(
    pushes: Flow<T>, awaitEnd: suspend () -> Unit, onPush: suspend (T) -> Unit
) = coroutineScope {
    val collector = launch { pushes.collect { onPush(it) } }
    try { awaitEnd() } finally { collector.cancel() }
}
