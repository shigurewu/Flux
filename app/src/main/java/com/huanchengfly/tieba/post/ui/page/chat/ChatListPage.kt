package com.huanchengfly.tieba.post.ui.page.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.PaddingValues
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.utils.DatabaseUtil
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.ui.page.destinations.ConversationsPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.EmoticonText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.panpf.sketch.compose.AsyncImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.ImSession
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ChatPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyIllustration
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getAvatarUrl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 私信会话列表（嵌入「消息」页 pager 的「私信」tab）。
 */
@Composable
fun ChatListPage(folded: Boolean = false) {
    if (!com.huanchengfly.tieba.post.im.PrivateMessaging.enabled) return
    val navigator = LocalNavigator.current
    val uid = AccountUtil.LocalAccount.current?.uid.orEmpty()
    val loggedIn = uid.isNotBlank()
    val sessions = rememberImSessions()
    var retry by remember(uid) { mutableIntStateOf(0) }
    var followFailed by remember(uid) { mutableStateOf(false) }
    val following by produceState<Set<Long>?>(initialValue = null, uid, retry) {
        value = null
        followFailed = false
        if (uid.isBlank()) return@produceState
        try {
            value = loadChatFollowIds(
                isCurrentOwner = { AccountUtil.currentAccount?.uid == uid },
                fetchPage = { TiebaApi.getInstance().followListFlow(it).first() },
            )
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { followFailed = true }
    }
    val groups = remember(sessions, following, uid) {
        following?.let { groupChatSessions(sessions, it, uid) }
    }
    val visibleSessions = if (folded) groups?.folded.orEmpty() else groups?.normal ?: sessions

    Column(Modifier.fillMaxSize()) {
        if (loggedIn && following == null) {
            Text(
                text = if (!followFailed) "正在核对关注关系…"
                    else if (folded) "关注关系加载失败，点此重试"
                    else "关注关系加载失败，暂时显示全部消息，点此重试",
                color = ExtendedTheme.colors.textSecondary, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().clickable { retry++ }.padding(16.dp),
            )
        }
        if (!folded && loggedIn) {
            val strangers = groups?.folded
            Row(
                Modifier.fillMaxWidth().clickable {
                    navigator.navigate(ConversationsPageDestination(folded = true))
                }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("折叠消息", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExtendedTheme.colors.text)
                    Text(strangers?.let { "${it.size} 个陌生人会话" }
                        ?: if (followFailed) "关系未确认，点击重试" else "正在核对关注关系，点击查看",
                        fontSize = 12.sp, color = ExtendedTheme.colors.textSecondary)
                }
                val hideDot by LocalContext.current.dataStore.collectPreferenceAsState(booleanPreferencesKey("hideNotificationDot"), false)
                val unread = strangers?.sumOf { it.unreadCount } ?: 0
                Text(if (unread > 0 && !hideDot) "$unread 条未读 ›" else "›", color = ExtendedTheme.colors.primary)
            }
        }
        if (visibleSessions.isEmpty()) {
            // 外层 Box 负责居中：EmptyState 内容块宽 0.9 屏宽，直接放进 pager 页会贴左
            Box(
                modifier = Modifier
                    .fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = if (folded && loggedIn && following == null) {
                        if (followFailed) "关注关系加载失败" else "正在加载…"
                    } else if (folded && loggedIn) "暂无折叠消息" else stringResource(
                        id = if (loggedIn) R.string.chat_empty_title else R.string.title_empty_login
                    ),
                    subtitle = if (folded && loggedIn) "未关注用户的消息会收在这里" else stringResource(
                        id = if (loggedIn) R.string.chat_empty_subtitle else R.string.chat_empty_login_subtitle
                    ),
                    illustration = EmptyIllustration.Hide,
                    modifier = Modifier.fillMaxWidth(),
                    actions = {
                        if (!loggedIn) {
                            Button(
                                onClick = { navigator.navigate(LoginPageDestination) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(text = stringResource(id = R.string.button_login))
                            }
                        }
                    },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(visibleSessions, key = { "${it.uid}-${it.peerUid}" }) { session ->
                    ChatSessionItem(session = session) {
                        navigator.navigate(
                            ChatPageDestination(
                                peerUid = session.peerUid,
                                peerName = session.peerName,
                                peerPortrait = session.peerPortrait,
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSessionItem(session: ImSession, onClick: () -> Unit) {
    val hideDot by LocalContext.current.dataStore.collectPreferenceAsState(booleanPreferencesKey("hideNotificationDot"), false)
    val hapticFeedback = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            imageUri = getAvatarUrl(session.peerPortrait),
            contentDescription = session.peerName,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ExtendedTheme.colors.chip),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.peerName.ifEmpty { "用户${session.peerUid}" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    color = ExtendedTheme.colors.text,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatChatTime(session.lastMsgTime),
                    fontSize = 12.sp,
                    color = ExtendedTheme.colors.textSecondary,
                )
            }
            EmoticonText(
                text = session.lastMsgPreview,
                memeStickerSize = false,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = ExtendedTheme.colors.textSecondary,
            )
        }
        if (session.unreadCount > 0 && !hideDot) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ExtendedTheme.colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (session.unreadCount > 99) "99+" else session.unreadCount.toString(),
                    fontSize = 11.sp,
                    color = ExtendedTheme.colors.onPrimary,
                )
            }
        }
    }
}

/** 会话时间显示：当日显示时分，跨日显示日期 */
internal fun formatChatTime(timestampSeconds: Long): String {
    if (timestampSeconds <= 0L) return ""
    val date = Date(timestampSeconds * 1000)
    val todayStart = java.util.Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val pattern = if (date.time >= todayStart) "HH:mm" else "yyyy-MM-dd"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

/** Recreate collection state on account changes so another account never flashes on screen. */
@Composable
internal fun rememberImSessions(): List<ImSession> {
    if (!com.huanchengfly.tieba.post.im.PrivateMessaging.enabled) return emptyList()
    val uid = AccountUtil.LocalAccount.current?.uid.orEmpty()
    return key(uid) {
        val sessions by remember(uid) { DatabaseUtil.getImSessionFlow(uid) }.collectAsState(initial = emptyList())
        sessions
    }
}
