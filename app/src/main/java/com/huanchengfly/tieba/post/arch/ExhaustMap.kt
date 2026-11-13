package com.huanchengfly.tieba.post.arch

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Drop overlapping refresh intents instead of queueing them for later. */
internal fun <T, R> Flow<T>.exhaustMap(transform: (T) -> Flow<R>): Flow<R> = channelFlow {
    val busy = AtomicBoolean(false)
    collect { value ->
        if (busy.compareAndSet(false, true)) launch(start = CoroutineStart.UNDISPATCHED) {
            try { transform(value).collect { send(it) } }
            finally { busy.set(false) }
        }
    }
}
