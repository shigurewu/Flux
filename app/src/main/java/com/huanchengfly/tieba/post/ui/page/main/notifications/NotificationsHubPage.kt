package com.huanchengfly.tieba.post.ui.page.main.notifications

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
 * 结构：三类入口（回复我的 / 提到我的 / 赞我的），每行显示最新一条摘要；
 * 右侧红点表示该分类有新消息，点进去即视为已读。
 * 「设置 → 使用习惯 → 屏蔽小红点」开启后，红点与底栏角标都不再显示。
 */
@Composable
fun NotificationsHubPage() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val hideDot by rememberPreferenceAsState(
        key = booleanPreferencesKey("hideNotificationDot"),
        defaultValue = false
    )

    var entries by remember { mutableStateOf<List<HubEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTick) {
        // 首次进入才走网络；切回消息 Tab 直接用进程内缓存，避免每次进来都刷一遍
        val cached = if (refreshTick == 0) HubCache.entries else null
        if (cached != null) {
            entries = cached
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val loaded = loadHubEntries(context)
        entries = loaded
        HubCache.entries = loaded
        loading = false
    }

    Scaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.title_notifications),
                navigationIcon = accountNavIconIfCompact(),
                actions = {
                    TextButton(
                        onClick = {
                            markAllRead(context, entries)
                            entries = entries.map { it.copy(hasNew = false) }
                            HubCache.entries = entries
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.action_mark_all_read),
                            color = ExtendedTheme.colors.textSecondary,
                            style = MaterialTheme.typography.caption
                        )
                    }
                    ActionItem(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = stringResource(id = R.string.btn_refresh)
                    ) {
                        refreshTick++
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(entries, key = { it.type.name }) { entry ->
                HubRow(
                    icon = entry.type.icon(),
                    title = stringResource(id = entry.type.titleRes()),
                    subtitle = entry.preview.ifBlank {
                        stringResource(id = R.string.tip_hub_no_new_message)
                    },
                    showDot = entry.hasNew && !hideDot,
                    onClick = {
                        context.getSharedPreferences(NOTIFY_PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putString(seenKey(entry.type), entry.key)
                            .apply()
                        entries = entries.map {
                            if (it.type == entry.type) it.copy(hasNew = false) else it
                        }
                        HubCache.entries = entries
                        navigator.navigate(
                            NotificationsPageDestination(initialTab = entry.type.ordinal)
                        )
                    }
                )
            }
            if (loading && entries.isEmpty()) {
                item { HubLoadingRow() }
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
    var entries: List<HubEntry>? = null
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

private fun seenKey(type: NotificationsType) = "seen_${type.name}"

private fun MessageListBean.MessageInfoBean.previewText(): String {
    val who = replyer?.nameShow?.takeIf { it.isNotBlank() }
        ?: replyer?.name.orEmpty()
    val body = content.orEmpty().replace('\n', ' ').trim()
    return listOf(who, body).filter { it.isNotBlank() }.joinToString("：")
}

private suspend fun loadHubEntries(context: Context): List<HubEntry> = coroutineScope {
    val prefs = context.getSharedPreferences(NOTIFY_PREFS, Context.MODE_PRIVATE)
    val api = TiebaApi.getInstance()

    val replyDeferred = async { runCatching { api.replyMeFlow().first() }.getOrNull() }
    val atDeferred = async { runCatching { api.atMeFlow().first() }.getOrNull() }
    val agreeDeferred = async { runCatching { api.agreeMeFlow(0L, 20).first() }.getOrNull() }

    val replyItem = replyDeferred.await()?.replyList?.firstOrNull()
    val atItem = atDeferred.await()?.atList?.firstOrNull()
    val agreeItem = agreeDeferred.await()?.agreeList?.firstOrNull()

    fun entry(type: NotificationsType, preview: String, key: String): HubEntry {
        val seen = prefs.getString(seenKey(type), null).orEmpty()
        return HubEntry(
            type = type,
            preview = preview,
            key = key,
            hasNew = key.isNotBlank() && key != seen
        )
    }

    listOf(
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
    )
}

private fun markAllRead(context: Context, entries: List<HubEntry>) {
    val editor = context.getSharedPreferences(NOTIFY_PREFS, Context.MODE_PRIVATE).edit()
    entries.forEach { editor.putString(seenKey(it.type), it.key) }
    editor.apply()
}
