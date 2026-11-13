package com.huanchengfly.tieba.post.im

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class ImSessionLifecycleTest {
    @Test fun disconnectReleasesInfinitePushCollector() = runBlocking {
        withTimeout(2000) {
            val pushes = MutableSharedFlow<Int>()
            val ended = CompletableDeferred<Unit>()
            val received = CompletableDeferred<Int>()
            val session = async { collectWhileSessionOpen(pushes, { ended.await() }) { received.complete(it) } }
            while (pushes.subscriptionCount.value == 0) yield()
            pushes.emit(7)
            assertEquals(7, received.await())
            ended.complete(Unit)
            session.await()
            assertEquals(0, pushes.subscriptionCount.value)
        }
    }

    @Test fun accountCancellationRemovesOldCollector() = runBlocking {
        withTimeout(2000) {
            val pushes = MutableSharedFlow<Int>()
            val ended = CompletableDeferred<Unit>()
            val session = async { collectWhileSessionOpen(pushes, { ended.await() }) { } }
            while (pushes.subscriptionCount.value == 0) yield()
            session.cancelAndJoin()
            assertEquals(0, pushes.subscriptionCount.value)
        }
    }
}
