package com.huanchengfly.tieba.post.arch

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ExhaustMapTest {
    @Test fun overlappingIntentsAreDroppedInsteadOfQueued() = runBlocking {
        withTimeout(2000) {
            val finish = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            val calls = mutableListOf<Int>()
            val result = async {
                flow { emit(1); emit(2); emit(3) }.exhaustMap { value -> flow {
                    calls += value
                    entered.complete(Unit)
                    finish.await()
                    emit(value)
                } }.toList()
            }
            entered.await()
            yield()
            assertEquals(listOf(1), calls)
            finish.complete(Unit)
            assertEquals(listOf(1), result.await())
            assertEquals(listOf(1), calls)
        }
    }
}
