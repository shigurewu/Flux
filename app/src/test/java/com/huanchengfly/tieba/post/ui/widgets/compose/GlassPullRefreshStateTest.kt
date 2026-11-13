package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class GlassPullRefreshStateTest {
    private class Frames : MonotonicFrameClock {
        private var time = 0L
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(also { time += 16_666_667 }.time)
    }
    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined + Frames())

    @Test fun locksBeforeScrollAndDropsTapsUntilFastCompletion() = runBlocking {
        val scope = scope()
        try {
            val state = GlassPullRefreshState(72f, 150f, scope)
            val scroll = CompletableDeferred<Unit>()
            var requests = 0
            state.onRefresh = { requests++ }
            state.requestRefresh { scroll.await() }
            repeat(10) { state.requestRefresh() }
            assertTrue(state.isBusy)
            assertEquals(0, requests)
            scroll.complete(Unit)
            yield()
            assertEquals(1, requests)
            state.requestRefresh()
            assertEquals(1, requests)
            // Loading true never reaches Compose, but the completion counter does.
            state.updateRequest(false, 1L)
            yield()
            assertEquals(0f, state.offsetPx, 0.001f)
            assertFalse(state.isBusy)
            state.requestRefresh()
            assertEquals(2, requests)
        } finally { scope.cancel() }
    }

    @Test fun horizontalMotionIsUntouchedAndReverseDragRetractsBeforeList() {
        val scope = scope()
        try {
            val state = GlassPullRefreshState(72f, 150f, scope)
            val connection = state.connection
            assertEquals(Offset.Zero, connection.onPostScroll(Offset.Zero, Offset(100f, 10f), NestedScrollSource.UserInput))
            assertEquals(0f, state.offsetPx, 0f)
            val pulled = connection.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.UserInput)
            assertEquals(0f, pulled.x, 0f)
            assertEquals(55f, state.offsetPx, 0.001f)
            assertEquals(Offset(0f, -20f), connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput))
            assertEquals(35f, state.offsetPx, 0.001f)
            assertEquals(Offset(0f, -35f), connection.onPreScroll(Offset(0f, -70f), NestedScrollSource.UserInput))
            assertEquals(0f, state.offsetPx, 0f)
        } finally { scope.cancel() }
    }

    @Test fun releaseOnlyDispatchesOnceAndFailureRetracts() = runBlocking {
        val scope = scope()
        try {
            val state = GlassPullRefreshState(72f, 150f, scope)
            var requests = 0
            state.onRefresh = { requests++ }
            state.dragBy(90f)
            state.connection.onPreFling(Velocity(100f, 100f))
            state.connection.onPostFling(Velocity.Zero, Velocity.Zero)
            state.release()
            assertEquals(1, requests)
            state.updateRequest(true, 0L)
            state.updateRequest(false, 1L)
            yield()
            assertFalse(state.isBusy)
            assertEquals(0f, state.offsetPx, 0f)
        } finally { scope.cancel() }
    }

    @Test fun reverseDragCanInterruptRetractionWithoutJumpingOrSticking() {
        val pausedFrames = object : MonotonicFrameClock {
            override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = awaitCancellation()
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined + pausedFrames)
        try {
            val state = GlassPullRefreshState(72f, 150f, scope)
            state.dragBy(55f)
            state.release()
            assertTrue(state.isBusy)
            val consumed = state.connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)
            assertEquals(Offset(0f, -20f), consumed)
            assertEquals(35f, state.offsetPx, 0.001f)
            assertFalse(state.isBusy)
            // The remaining indicator must retract on the next release, even with no new downward drag.
            state.release()
            assertTrue(state.isBusy)
        } finally { scope.cancel() }
    }
}
