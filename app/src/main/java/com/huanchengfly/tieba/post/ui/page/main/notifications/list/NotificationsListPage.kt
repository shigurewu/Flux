package com.huanchengfly.tieba.post.ui.page.main.notifications.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.main.LocalMainBottomInsets
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.EmoticonText
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.PullRefreshLogoIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NotificationsListPage(
    type: NotificationsType,
    viewModel: NotificationsListViewModel = when (type) {
        NotificationsType.ReplyMe -> pageViewModel<NotificationsListUiIntent, ReplyMeListViewModel>()
        NotificationsType.AtMe -> pageViewModel<NotificationsListUiIntent, AtMeListViewModel>()
        NotificationsType.AgreeMe -> pageViewModel<NotificationsListUiIntent, AgreeMeListViewModel>()
    }
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(NotificationsListUiIntent.Refresh)
        viewModel.initialized = true
    }
    val navigator = LocalNavigator.current
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::hasMore,
        initial = true
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::data,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = NotificationsListUiState::currentPage,
        initial = 1
    )
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.send(NotificationsListUiIntent.Refresh) }
    )
    val lazyListState = rememberLazyListState()
    Box(
        modifier = Modifier.pullRefresh(pullRefreshState)
    ) {
        LoadMoreLayout(
            isLoading = isLoadingMore,
            onLoadMore = { viewModel.send(NotificationsListUiIntent.LoadMore(currentPage + 1)) },
            loadEnd = !hasMore,
            lazyListState = lazyListState,
            // 真悬浮底栏胶囊浮于内容之上,加载提示胶囊需停在呼吸位上(经典贴底模式为 0)
            indicatorBottomInset = LocalMainBottomInsets.current,
        ) {
            MyLazyColumn(
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = 4.dp + LocalMainBottomInsets.current
                ),
                state = lazyListState,
            ) {
                items(
                    items = data,
                    key = { "${it.info.postId}_${it.info.replyer?.id}_${it.info.time}" },
                ) { (info, blocked) ->
                    Container {
                        BlockableContent(
                            blocked = blocked,
                            blockedTip = {
                                BlockTip {
                                    Text(
                                        text = stringResource(id = R.string.tip_blocked_message)
                                    )
                                }
                            },
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                NotificationMessageItem(
                                    info = info,
                                    type = type,
                                    onClick = {
                                        val threadId = info.threadId?.toLongOrNull()
                                        val postId = info.postId?.toLongOrNull()
                                        if (threadId != null && postId != null) {
                                            if (info.isFloor == "1") {
                                                navigator.navigate(
                                                    SubPostsPageDestination(
                                                        threadId = threadId,
                                                        // quotePid 引用不确定，先传 0，在子楼页面获取正确的 postId
                                                        postId = 0,
                                                        subPostId = postId,
                                                        loadFromSubPost = true
                                                    )
                                                )
                                            } else {
                                                navigator.navigate(
                                                    ThreadPageDestination(
                                                        threadId = threadId,
                                                        postId = postId
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                                // 细分隔线，贴吧式列表
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(ExtendedTheme.colors.divider)
                                )
                            }
                        }
                    }
                }
            }
        }

        PullRefreshLogoIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
            contentColor = ExtendedTheme.colors.onPrimary,
        )
    }
}

/**
 * 贴吧式消息行：头像 + 昵称/时间 → 动作文案 → 内容块 → 吧名。
 * 「赞我的」内容块展示被赞的原回复；「回复我/@我」正文直接展示，引用块单独显示。
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NotificationMessageItem(
    info: MessageListBean.MessageInfoBean,
    type: NotificationsType,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val actionText = when (type) {
        NotificationsType.ReplyMe -> stringResource(id = R.string.msg_action_reply_you)
        NotificationsType.AtMe -> stringResource(id = R.string.msg_action_mention_you)
        NotificationsType.AgreeMe -> stringResource(id = R.string.msg_action_agree_you)
    }
    val actionColor = if (type == NotificationsType.AgreeMe) {
        ExtendedTheme.colors.text
    } else {
        ExtendedTheme.colors.text
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (info.replyer != null) {
                Avatar(
                    data = StringUtil.getAvatarUrl(info.replyer.portrait),
                    size = Sizes.Small,
                    contentDescription = null,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            info.replyer.id?.toLongOrNull()?.let {
                                navigator.navigate(UserProfilePageDestination(it))
                            }
                        }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ExtendedTheme.colors.chip)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = info.replyer?.nameShow ?: info.replyer?.name.orEmpty(),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = ExtendedTheme.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                info.time?.let { time ->
                    Text(
                        text = DateTimeUtils.getRelativeTimeString(context, time),
                        style = MaterialTheme.typography.caption,
                        color = ExtendedTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Text(
            text = actionText,
            style = MaterialTheme.typography.body1,
            fontSize = 15.sp,
            color = actionColor
        )

        when (type) {
            NotificationsType.AgreeMe -> {
                MessageQuoteBlock(text = info.content.orEmpty())
            }

            else -> {
                if (!info.content.isNullOrBlank()) {
                    EmoticonText(text = info.content)
                }
                val quoteText = if (type == NotificationsType.ReplyMe && info.isFloor != "1") {
                    stringResource(id = R.string.text_message_list_item_reply_my_thread, info.title ?: "")
                } else {
                    info.quoteContent ?: if (type == NotificationsType.AtMe) info.title else null
                }
                if (!quoteText.isNullOrBlank()) {
                    MessageQuoteBlock(text = quoteText)
                }
            }
        }

        if (!info.forumName.isNullOrBlank()) {
            Text(
                text = info.forumName,
                style = MaterialTheme.typography.caption,
                color = ExtendedTheme.colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MessageQuoteBlock(text: String) {
    if (text.isBlank()) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ExtendedTheme.colors.chip)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        EmoticonText(
            text = text,
            color = ExtendedTheme.colors.onChip,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
