package com.huanchengfly.tieba.post.ui.page.main.notifications

import android.app.NotificationManager
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import com.huanchengfly.tieba.post.im.ImManager
import com.huanchengfly.tieba.post.im.ImStatus
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.services.NotifyJobService
import com.huanchengfly.tieba.post.ui.page.destinations.ConversationsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.chat.rememberImSessions
import com.huanchengfly.tieba.post.ui.page.main.LocalMainBottomInsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.NotificationsPageDestination
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

private const val NOTIFY_PREFS = "flux_notify_seen"

/**
 * 消息中心（贴吧式分类入口）。
 *
 * 结构：四类入口（回复我的 / 提到我的 / 赞我的 / 私信），每行显示最新一条摘要；
 * 右侧红点表示该分类有新消息，点进去即视为已读。
 * 「设置 → 使用习惯 → 屏蔽小红点」开启后，红点与底栏角标都不再显示。
 */
@Composable
fun NotificationsHubPage() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uid = LocalAccount.current?.uid.orEmpty()
    val scope = rememberCoroutineScope()
    val sessions = rememberImSessions()
    val imStatus = if (com.huanchengfly.tieba.post.im.PrivateMessaging.enabled)
        ImManager.imStatus.collectAsState().value else ImStatus.Idle
    val hideDot by LocalContext.current.dataStore.collectPreferenceAsState(booleanPreferencesKey("hideNotificationDot"), false)
    var entries by remember(uid) { mutableStateOf(HubCache.entries[uid] ?: emptyHubEntries()) }
    var loading by remember(uid) { mutableStateOf(false) }
    var loadError by remember(uid) { mutableStateOf(false) }
    var markingRead by remember(uid) { mutableStateOf(false) }
    var refreshTick by remember(uid) { mutableIntStateOf(0) }

    LaunchedEffect(uid, refreshTick) {
        if (uid.isBlank()) return@LaunchedEffect
        if (refreshTick == 0 && System.currentTimeMillis() - (HubCache.loadedAt[uid] ?: 0L) < 60_000L) return@LaunchedEffect
        loading = true
        try {
            val result = loadHubEntries(context, uid, entries)
            entries = result.entries
            loadError = result.failed
            HubCache.entries[uid] = entries
            if (!result.failed) HubCache.loadedAt[uid] = System.currentTimeMillis()
        } finally { loading = false }
    }
    LaunchedEffect(uid) {
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifications.cancel(NotifyJobService.ID_REPLY)
        notifications.cancel(NotifyJobService.ID_AT)
    }
    Scaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Toolbar(
                title = stringResource(R.string.title_notifications),
                navigationIcon = accountNavIconIfCompact(),
                actions = {
                    TextButton(enabled = uid.isNotBlank() && !markingRead, onClick = {
                        markAllRead(context, uid, entries)
                        entries = entries.map { it.copy(hasNew = false) }
                        HubCache.entries[uid] = entries
                        val unread = sessions.filter { it.unreadCount > 0 }
                        markingRead = true
                        scope.launch {
                            try {
                                for (session in unread) {
                                    if (com.huanchengfly.tieba.post.utils.AccountUtil.currentAccount?.uid != uid) break
                                    ImManager.markRead(session.peerUid, session.lastMsgId, uid)
                                }
                            } finally { markingRead = false }
                        }
                    }) {
                        Text(stringResource(R.string.action_mark_all_read), color = ExtendedTheme.colors.textSecondary,
                            style = MaterialTheme.typography.caption)
                    }
                    ActionItem(icon = Icons.Rounded.Refresh, contentDescription = stringResource(R.string.btn_refresh)) {
                        if (!loading) refreshTick++
                    }
                }
            )
        }, modifier = Modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + LocalMainBottomInsets.current)) {
            items(entries, key = { it.type.name }) { entry ->
                HubRow(entry.type.icon(), stringResource(entry.type.titleRes()),
                    if (uid.isBlank()) "登录后查看消息" else entry.preview.ifBlank { if (loading) "加载中…" else stringResource(R.string.tip_hub_no_new_message) },
                    entry.hasNew && !hideDot, onClick = {
                        if (uid.isBlank()) navigator.navigate(LoginPageDestination)
                        else {
                            context.getSharedPreferences(NOTIFY_PREFS, Context.MODE_PRIVATE).edit()
                                .putString(seenKey(uid, entry.type), entry.key).apply()
                            entries = entries.map { if (it.type == entry.type) it.copy(hasNew = false) else it }
                            HubCache.entries[uid] = entries
                            navigator.navigate(NotificationsPageDestination(initialTab = entry.type.ordinal))
                        }
                    })
            }
            if (com.huanchengfly.tieba.post.im.PrivateMessaging.enabled) item(key = "private_messages") {
                val latest = sessions.firstOrNull()
                val preview = when {
                    uid.isBlank() -> "登录后查看私信"
                    latest != null -> (latest.peerName.ifBlank { "用户${latest.peerUid}" }) + "：" + latest.lastMsgPreview
                    imStatus is ImStatus.Error -> "暂未连接，点击查看"
                    imStatus is ImStatus.Connecting -> "连接中…"
                    else -> "暂无私信"
                }
                HubRow(Icons.AutoMirrored.Rounded.Chat, stringResource(R.string.title_chat), preview,
                    !hideDot && sessions.any { it.unreadCount > 0 }) {
                    if (uid.isBlank()) navigator.navigate(LoginPageDestination)
                    else navigator.navigate(ConversationsPageDestination())
                }
            }
            if (loadError) item {
                Text("消息更新失败，已保留上次内容；点击右上角重试", color = ExtendedTheme.colors.textSecondary,
                    style = MaterialTheme.typography.caption, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun HubLoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "加载中…",
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary
        )
    }
}

@Composable
private fun HubRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showDot: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ExtendedTheme.colors.chip),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ExtendedTheme.colors.text,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = ExtendedTheme.colors.text
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.caption,
                color = ExtendedTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ExtendedTheme.colors.accent)
            )
        }
    }
}

private data class HubEntry(
    val type: NotificationsType,
    val preview: String,
    /** 用于判断「是否已读过」的最新一条的唯一标识 */
    val key: String,
    val hasNew: Boolean,
)

/** 进程内缓存：切回消息 Tab 不重复请求，只有点刷新才重新拉取 */
private object HubCache {
    val entries = mutableMapOf<String, List<HubEntry>>()
    val loadedAt = mutableMapOf<String, Long>()
}

internal fun NotificationsType.titleRes(): Int = when (this) {
    NotificationsType.ReplyMe -> R.string.title_reply_me
    NotificationsType.AtMe -> R.string.title_at_me
    NotificationsType.AgreeMe -> R.string.title_agree_me
}

private fun NotificationsType.icon(): ImageVector = when (this) {
    NotificationsType.ReplyMe -> Icons.AutoMirrored.Rounded.Reply
    NotificationsType.AtMe -> Icons.Rounded.AlternateEmail
    NotificationsType.AgreeMe -> Icons.Rounded.ThumbUp
}

private fun seenKey(uid: String, type: NotificationsType) = "seen_${uid}_${type.name}"

private fun MessageListBean.MessageInfoBean.previewText(): String {
    val who = replyer?.nameShow?.takeIf { it.isNotBlank() }
        ?: replyer?.name.orEmpty()
    val body = content.orEmpty().replace('\n', ' ').trim()
    return listOf(who, body).filter { it.isNotBlank() }.joinToString("：")
}

private fun emptyHubEntries() = NotificationsType.values().map { HubEntry(it, "", "", false) }
private data class HubResult(val entries: List<HubEntry>, val failed: Boolean)
private suspend fun <T> hubRequest(block: suspend () -> T): Result<T> = try { Result.success(block()) }
catch (e: CancellationException) { throw e }
catch (e: Exception) { Result.failure(e) }

private suspend fun loadHubEntries(context: Context, uid: String, previous: List<HubEntry>): HubResult = coroutineScope {
    val prefs = context.getSharedPreferences(NOTIFY_PREFS, Context.MODE_PRIVATE)
    val api = TiebaApi.getInstance()

    val replyDeferred = async { hubRequest { api.replyMeFlow().first() } }
    val atDeferred = async { hubRequest { api.atMeFlow().first() } }
    val agreeDeferred = async { hubRequest { api.agreeMeFlow(0L, 20).first() } }

    val replyResult = replyDeferred.await()
    val replyItem = replyResult.getOrNull()?.replyList?.firstOrNull()
    val atResult = atDeferred.await()
    val atItem = atResult.getOrNull()?.atList?.firstOrNull()
    val agreeResult = agreeDeferred.await()
    val agreeItem = agreeResult.getOrNull()?.agreeList?.firstOrNull()

    fun entry(type: NotificationsType, preview: String, key: String): HubEntry {
        val failed = when (type) {
            NotificationsType.ReplyMe -> replyResult.isFailure
            NotificationsType.AtMe -> atResult.isFailure
            NotificationsType.AgreeMe -> agreeResult.isFailure
        }
        if (failed) return previous.firstOrNull { it.type == type } ?: HubEntry(type, "", "", false)
        val seen = prefs.getString(seenKey(uid, type), null).orEmpty()
        return HubEntry(
            type = type,
            preview = preview,
            key = key,
            hasNew = key.isNotBlank() && key != seen
        )
    }

    HubResult(listOf(
        entry(
            type = NotificationsType.ReplyMe,
            preview = replyItem?.previewText().orEmpty(),
            key = replyItem?.postId ?: replyItem?.time.orEmpty()
        ),
        entry(
            type = NotificationsType.AtMe,
            preview = atItem?.previewText().orEmpty(),
            key = atItem?.postId ?: atItem?.time.orEmpty()
        ),
        entry(
            type = NotificationsType.AgreeMe,
            preview = agreeItem?.let {
                val who = it.agreeer?.nameShow?.takeIf { name -> name.isNotBlank() }
                if (who != null) "$who 赞了你" else "有人赞了你的帖子"
            }.orEmpty(),
            key = agreeItem?.id?.toString().orEmpty()
        ),
    ), replyResult.isFailure || atResult.isFailure || agreeResult.isFailure)
}

private fun markAllRead(context: Context, uid: String, entries: List<HubEntry>) {
    val editor = context.getSharedPreferences(NOTIFY_PREFS, Context.MODE_PRIVATE).edit()
    entries.forEach { editor.putString(seenKey(uid, it.type), it.key) }
    editor.apply()
}
