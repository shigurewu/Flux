package com.huanchengfly.tieba.post.ui.widgets.compose

/** Admission is synchronous; completed is observable even when loading true is conflated away. */
internal class RefreshCycle {
    var inFlight = false
        private set
    private var completion = 0L
    private var loading = false
    fun tryStart(): Boolean {
        if (inFlight || loading) return false
        inFlight = true
        return true
    }
    fun observe(isLoading: Boolean, completed: Long): Boolean {
        val finished = completed != completion || (loading && !isLoading)
        completion = completed
        loading = isLoading
        if (finished) inFlight = false
        return finished
    }
    fun cancel() { inFlight = false }
}
