package com.huanchengfly.tieba.post.ui.page.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.im.ImManager
import com.huanchengfly.tieba.post.im.ImStatus
import com.huanchengfly.tieba.post.im.ImHistoryApi
import com.huanchengfly.tieba.post.im.visibleImMessages
import com.huanchengfly.tieba.post.models.database.ImMessage
import com.huanchengfly.tieba.post.models.database.ImSession
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.DatabaseUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChatUiIntent {
    data class Load(val peerUid: Long) : ChatUiIntent
    data class SyncHistory(val peerUid: Long, val peerName: String, val peerPortrait: String) : ChatUiIntent
    data class Send(val peerUid: Long, val content: String, val peerName: String, val peerPortrait: String) : ChatUiIntent
}

sealed interface ChatUiEvent {
    val ownerUid: String
    data class SendFailed(override val ownerUid: String, val message: String) : ChatUiEvent
    data class Sent(override val ownerUid: String, val content: String) : ChatUiEvent
}

data class ChatUiState(
    val ownerUid: String = "",
    val messages: List<ImMessage> = emptyList(),
    val session: ImSession? = null,
    val sending: Boolean = false,
    val syncingHistory: Boolean = false,
    val historyStarted: Boolean = false,
    val historyHasMore: Boolean = true,
    val historyFeedback: String = "",
)

/** Private text never passes through the generic ViewModel's intent/state log statements. */
@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    private val state = MutableStateFlow(ChatUiState())
    val uiState = state.asStateFlow()
    private val events = Channel<ChatUiEvent>(Channel.BUFFERED)
    val uiEventFlow = events.receiveAsFlow()
    private var loadJob: Job? = null
    private var historyJob: Job? = null
    private var historyBefore = 0L
    private var loadedPeer = 0L
    private var loadGeneration = 0L

    fun send(intent: ChatUiIntent) {
        if (!com.huanchengfly.tieba.post.im.PrivateMessaging.enabled) return
        val uid = AccountUtil.currentAccount?.uid.orEmpty()
        when (intent) {
            is ChatUiIntent.Load -> {
                loadJob?.cancel()
                historyJob?.cancel()
                loadGeneration++
                loadedPeer = intent.peerUid
                historyBefore = 0L
                state.value = ChatUiState(ownerUid = uid)
                if (uid.isBlank()) return
                loadJob = viewModelScope.launch {
                    coroutineScope {
                        launch {
                            ImManager.imStatus.collect { status ->
                                if (status == ImStatus.Ready) {
                                    try { ImManager.pullMessages() }
                                    catch (e: CancellationException) { throw e }
                                    catch (_: Exception) { /* keep local history while reconnecting */ }
                                }
                            }
                        }
                        var lastRead = 0L
                        combine(DatabaseUtil.getImMessageFlow(uid, intent.peerUid), DatabaseUtil.getImSessionFlow(uid)) {
                            messages, sessions -> messages to sessions.firstOrNull { it.peerUid == intent.peerUid }
                        }.collect { (messages, session) ->
                            if (AccountUtil.currentAccount?.uid != uid) return@collect
                            state.update { it.copy(messages = visibleImMessages(messages), session = session) }
                            val lastIncoming = messages.lastOrNull { !it.isMine && it.msgId > 0 }
                            if (lastIncoming != null && lastIncoming.msgId > lastRead) {
                                lastRead = lastIncoming.msgId
                                ImManager.markRead(intent.peerUid, lastRead, uid)
                            }
                        }
                    }
                }
            }
            is ChatUiIntent.SyncHistory -> {
                if (uid.isBlank() || uid != state.value.ownerUid || intent.peerUid != loadedPeer ||
                    state.value.syncingHistory || !state.value.historyHasMore) return
                val generation = loadGeneration
                state.update { it.copy(syncingHistory = true, historyFeedback = "正在尝试读取服务端历史消息…") }
                historyJob = viewModelScope.launch {
                    try {
                        val page = ImHistoryApi.fetch(uid, intent.peerUid, historyBefore)
                        if (generation != loadGeneration || AccountUtil.currentAccount?.uid != uid) return@launch
                        // Historical reads do not alter session previews, unread totals or live cursors.
                        val inserted = DatabaseUtil.importImHistory(uid, intent.peerUid, page.messages,
                            intent.peerName, intent.peerPortrait)
                        if (generation != loadGeneration || AccountUtil.currentAccount?.uid != uid) return@launch
                        historyBefore = page.nextBefore
                        state.update { it.copy(historyStarted = true, historyHasMore = page.hasMore,
                            historyFeedback = "新增 $inserted 条历史记录" +
                                (if (page.unsupported > 0) "，${page.unsupported} 条非文本消息暂不支持" else "") +
                                (if (page.hasMore) "；可继续同步更早消息" else "；服务端本次未提供更多历史")) }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        if (generation == loadGeneration && AccountUtil.currentAccount?.uid == uid)
                            state.update { it.copy(historyFeedback = e.message ?: "历史同步失败，请重试") }
                    } finally {
                        if (generation == loadGeneration) state.update { it.copy(syncingHistory = false) }
                    }
                }
            }
            is ChatUiIntent.Send -> {
                if (uid.isBlank() || state.value.sending || intent.content.isBlank()) return
                state.update { it.copy(sending = true) }
                viewModelScope.launch {
                    try {
                        ImManager.sendMessage(intent.peerUid, intent.content)
                        DatabaseUtil.updateImPeer(uid, intent.peerUid, intent.peerName, intent.peerPortrait)
                        events.send(ChatUiEvent.Sent(uid, intent.content))
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { events.send(ChatUiEvent.SendFailed(uid, e.message ?: "请稍后重试")) }
                    finally { if (state.value.ownerUid == uid) state.update { it.copy(sending = false) } }
                }
            }
        }
    }
}
