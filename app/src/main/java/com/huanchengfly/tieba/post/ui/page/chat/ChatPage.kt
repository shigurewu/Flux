package com.huanchengfly.tieba.post.ui.page.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.im.ImManager
import com.huanchengfly.tieba.post.im.ImStatus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.panpf.sketch.compose.AsyncImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.ImMessage
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.StringUtil.getAvatarUrl
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/**
 * 1v1 私信聊天页（一期：文本收发）。
 */
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://chat/{peerUid}")
    ]
)
@Composable
fun ChatPage(
    navigator: DestinationsNavigator,
    peerUid: Long,
    peerName: String = "",
    peerPortrait: String = "",
    chatViewModel: ChatViewModel = viewModel(),
) {
    if (!com.huanchengfly.tieba.post.im.PrivateMessaging.enabled) {
        Text("私信功能暂未开放", modifier = Modifier.padding(24.dp)); return
    }
    val context = LocalContext.current
    val uid = AccountUtil.LocalAccount.current?.uid.orEmpty()
    val latestUid by rememberUpdatedState(uid)
    val state by chatViewModel.uiState.collectAsState()
    val uiState = if (state.ownerUid == uid) state else ChatUiState(ownerUid = uid)
    val status by ImManager.imStatus.collectAsState()
    var input by rememberSaveable(uid, peerUid) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(chatViewModel, peerUid, uid) {
        chatViewModel.send(ChatUiIntent.Load(peerUid))
    }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }
    LaunchedEffect(chatViewModel, uid, peerUid) {
        chatViewModel.uiEventFlow.collect { event ->
            if (event.ownerUid == latestUid) when (event) {
                is ChatUiEvent.Sent -> if (input.trim() == event.content) input = ""
                is ChatUiEvent.SendFailed -> Toast.makeText(context,
                    context.getString(R.string.toast_chat_send_failed, event.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    val displayName = uiState.session?.peerName?.ifEmpty { peerName }
        ?.ifEmpty { "用户$peerUid" } ?: peerName.ifEmpty { "用户$peerUid" }
    val displayPortrait = uiState.session?.peerPortrait?.ifEmpty { peerPortrait } ?: peerPortrait

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    BackNavigationIcon {
                        navigator.navigateUp()
                    }
                },
                actions = {
                    if (displayPortrait.isNotEmpty()) {
                        AsyncImage(
                            imageUri = getAvatarUrl(displayPortrait),
                            contentDescription = displayName,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(32.dp)
                                .clip(CircleShape),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
        ) {
            if (status != ImStatus.Ready || uid.isBlank()) {
                Text(
                    text = when {
                        uid.isBlank() -> "登录后即可发送私信"
                        status is ImStatus.Error -> "私信暂不可用：" + ((status as ImStatus.Error).throwable.message ?: "正在重连")
                        status is ImStatus.Connecting -> "正在连接私信…"
                        else -> "等待网络连接…"
                    },
                    color = ExtendedTheme.colors.textSecondary, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (uiState.messages.isEmpty()) {
                Text("仅支持文本私信；历史消息暂不回填", color = ExtendedTheme.colors.textSecondary,
                    fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.messages, key = { "${it.peerUid}-${it.msgId}" }) { message ->
                    ChatMessageItem(
                        message = message,
                        peerName = displayName,
                        peerPortrait = displayPortrait,
                    )
                }
            }
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                enabled = !uiState.sending,
                canSend = status == ImStatus.Ready && uid.isNotBlank(),
                onSend = {
                    val content = input.trim()
                    if (content.isEmpty() || uiState.sending) return@ChatInputBar
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    chatViewModel.send(ChatUiIntent.Send(peerUid, content, displayName, displayPortrait))
                },
            )
        }
    }
}

@Composable
private fun ChatMessageItem(message: ImMessage, peerName: String, peerPortrait: String) {
    val isMine = message.isMine
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isMine && peerPortrait.isNotEmpty()) {
            AsyncImage(
                imageUri = getAvatarUrl(peerPortrait),
                contentDescription = peerName,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape),
            )
        }
        Text(
            text = message.content,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isMine) 16.dp else 4.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp,
                    )
                )
                .background(if (isMine && ExtendedTheme.colors.primaryContainer != Color.Unspecified) ExtendedTheme.colors.primaryContainer else ExtendedTheme.colors.chip)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (isMine && ExtendedTheme.colors.onPrimaryContainer != Color.Unspecified) ExtendedTheme.colors.onPrimaryContainer else ExtendedTheme.colors.text,
        )
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    canSend: Boolean,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(ExtendedTheme.colors.chip)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = ExtendedTheme.colors.text,
            ),
            cursorBrush = SolidColor(ExtendedTheme.colors.primary),
            maxLines = 4,
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.chat_input_hint),
                            fontSize = 15.sp,
                            color = ExtendedTheme.colors.textSecondary,
                        )
                    }
                    innerTextField()
                }
            },
        )
        IconButton(onClick = onSend, enabled = enabled && canSend && value.isNotBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = stringResource(id = R.string.chat_btn_send_desc),
                tint = if (value.isBlank()) {
                    ExtendedTheme.colors.textDisabled
                } else {
                    ExtendedTheme.colors.primary
                },
            )
        }
    }
}
