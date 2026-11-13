package com.huanchengfly.tieba.post.ui.page.user.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.getOrNull
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SubPostsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.PullRefreshLogoIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyIllustration
import com.huanchengfly.tieba.post.ui.widgets.compose.EmptyState
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserPostPage(
    uid: Long,
    authorName: String = "",
    authorPortrait: String = "",
    isThread: Boolean = true,
    fluid: Boolean = false,
    enablePullRefresh: Boolean = false,
    viewModel: UserPostViewModel = pageViewModel(key = if (isThread) "user_thread_$uid" else "user_post_$uid"),
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(UserPostUiIntent.Refresh(uid, isThread))
        viewModel.initialized = true
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isLoadingMore,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::error,
        initial = null
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::hasMore,
        initial = false
    )
    val posts by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::posts,
        initial = persistentListOf()
    )
    val hidePost by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::hidePost,
        initial = false
    )

    val viewerUid = AccountUtil.LocalAccount.current?.uid.orEmpty()
    val canManage = viewerUid == uid.toString()
    val refreshCompleted by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::refreshCompleted, initial = 0L)
    var publicIds by remember(uid, viewerUid) { mutableStateOf<Set<Long>?>(null) }
    var visibilityRevision by remember(uid, viewerUid) { mutableLongStateOf(0L) }
    var visibilitySyncing by remember(uid, viewerUid) { mutableStateOf(false) }
    var visibilityError by remember(uid, viewerUid) { mutableStateOf<String?>(null) }
    var visibilityRetry by remember(uid, viewerUid) { mutableIntStateOf(0) }
    LaunchedEffect(uid, viewerUid, isThread, isRefreshing, refreshCompleted, visibilityRetry) {
        if (!isThread || !canManage || isRefreshing) return@LaunchedEffect
        visibilitySyncing = true
        visibilityError = null
        try {
            val result = withTimeout(45_000) { withContext(Dispatchers.IO) { loadPublicThreadIndex(uid) } }
            if (AccountUtil.currentAccount?.uid == viewerUid) {
                publicIds = result
                visibilityRevision++
            }
        } catch (_: TimeoutCancellationException) {
            visibilityError = "可见范围同步超时，未确认的帖子不会标为公开"
        } catch (cancel: CancellationException) { throw cancel
        } catch (error: Exception) {
            visibilityError = "可见范围同步失败，可点击重试"
        } finally { visibilitySyncing = false }
    }

    val isEmpty by remember {
        derivedStateOf { posts.isEmpty() }
    }
    val isError by remember {
        derivedStateOf { error != null }
    }

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "user_profile" }
    ) {
        viewModel.send(UserPostUiIntent.Refresh(uid, isThread))
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = {
            viewModel.send(UserPostUiIntent.Refresh(uid, isThread))
        },
        loadingScreen = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) {
                        FeedCardPlaceholder()
                    }
                }
            }
        },
        errorScreen = { ErrorScreen(error = error.getOrNull()) },
        emptyScreen = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (hidePost) {
                    EmptyState(
                        title = stringResource(id = R.string.title_user_hide_post),
                        illustration = EmptyIllustration.Hide,
                        scrollable = false,
                    )
                } else {
                    EmptyState(
                        title = stringResource(id = R.string.title_empty),
                        illustration = EmptyIllustration.Box,
                        actions = {
                            if (canReload) {
                                Button(onClick = { reload() }) {
                                    Text(text = stringResource(id = R.string.btn_refresh))
                                }
                            }
                        },
                        scrollable = false,
                    )
                }
            }
        },
    ) {
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = ::reload
        )

        val lazyListState = rememberLazyListState()

        val pullRefreshModifier =
            if (enablePullRefresh) Modifier.pullRefresh(pullRefreshState) else Modifier

        Box(modifier = pullRefreshModifier) {
            LoadMoreLayout(
                isLoading = isLoadingMore,
                onLoadMore = {
                    viewModel.send(UserPostUiIntent.LoadMore(uid, isThread, currentPage))
                },
                loadEnd = !hasMore,
                lazyListState = lazyListState
            ) {
                UserPostList(
                    data = posts,
                    canManage = canManage,
                    publicIds = publicIds,
                    visibilityRevision = visibilityRevision,
                    visibilitySyncing = visibilitySyncing,
                    visibilityError = visibilityError,
                    onRetryVisibility = { visibilityRetry++ },
                    authorName = authorName,
                    authorPortrait = authorPortrait,
                    fluid = fluid,
                    lazyListState = lazyListState,
                    onTogglePrivacy = { fid, tid, pid, hide, onDone ->
                        scope.launch {
                            try {
                                check(AccountUtil.currentAccount?.uid == uid.toString()) { "账号已切换" }
                                TiebaApi.getInstance()
                                    .setThreadPrivacyFlow(fid, tid, pid, hide)
                                    .collect { response ->
                                        check(response.errorCode == 0) { response.errorMsg.ifBlank { "操作未成功" } }
                                        onDone(true)
                                        Toast.makeText(
                                            context,
                                            if (hide) "已设为仅自己可见" else "已设为公开",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        viewModel.send(UserPostUiIntent.Refresh(uid, isThread))
                                    }
                            } catch (cancel: CancellationException) { throw cancel
                            } catch (e: Exception) {
                                onDone(false)
                                Toast.makeText(
                                    context,
                                    "设置失败：${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onDeleteThread = { fid, tid, name, onDone ->
                        scope.launch {
                            try {
                                check(AccountUtil.currentAccount?.uid == uid.toString()) { "账号已切换" }
                                TiebaApi.getInstance()
                                    .delThreadFlow(
                                        forumId = fid,
                                        forumName = name,
                                        threadId = tid,
                                        tbs = AccountUtil.getLoginInfo()?.tbs,
                                        delMyThread = true,
                                        isHide = false
                                    )
                                    .collect { response ->
                                        check(response.errorCode == 0) { response.errorMsg.ifBlank { "操作未成功" } }
                                        ThreadPrivacyCache.remove(context, tid)
                                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                        viewModel.send(UserPostUiIntent.Refresh(uid, isThread))
                                    }
                            } catch (cancel: CancellationException) { throw cancel
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "删除失败：${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally { onDone() }
                        }
                    },
                    onClickItem = { threadId, postId, isSubPost ->
                        if (postId == null) {
                            navigator.navigate(ThreadPageDestination(threadId))
                        } else {
                            if (isSubPost) {
                                navigator.navigate(
                                    SubPostsPageDestination(
                                        threadId = threadId,
                                        subPostId = postId,
                                        loadFromSubPost = true
                                    )
                                )
                            } else {
                                navigator.navigate(
                                    ThreadPageDestination(
                                        threadId,
                                        postId = postId,
                                        scrollToReply = true
                                    )
                                )
                            }
                        }
                    },
                    onAgreeItem = {
                        viewModel.send(
                            UserPostUiIntent.Agree(
                                it.thread_id,
                                it.post_id,
                                it.agree?.hasAgree ?: 0
                            )
                        )
                    },
                    onClickReply = {
                        navigator.navigate(
                            ThreadPageDestination(
                                it.thread_id,
                                it.forum_id,
                                scrollToReply = true
                            )
                        )
                    },
                    onClickUser = {
                        navigator.navigate(UserProfilePageDestination(it))
                    },
                    onClickForum = {
                        navigator.navigate(ForumPageDestination(it))
                    },
                    onClickOriginThread = {
                        navigator.navigate(ThreadPageDestination(it))
                    },
                )
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
}

@Composable
private fun UserPostList(
    publicIds: Set<Long>?,
    visibilityRevision: Long,
    visibilitySyncing: Boolean,
    visibilityError: String?,
    onRetryVisibility: () -> Unit,
    canManage: Boolean,
    authorName: String,
    authorPortrait: String,
    data: ImmutableList<PostListItemData>,
    fluid: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    onClickItem: (threadId: Long, postId: Long?, isSubPost: Boolean) -> Unit = { _, _, _ -> },
    onAgreeItem: (PostInfoList) -> Unit = {},
    onClickReply: (PostInfoList) -> Unit = {},
    onClickUser: (id: Long) -> Unit = {},
    onClickForum: (name: String) -> Unit = {},
    onClickOriginThread: (threadId: Long) -> Unit = {},
    onTogglePrivacy: (fid: Long, tid: Long, pid: Long, hide: Boolean, onDone: (Boolean) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onDeleteThread: (fid: Long, tid: Long, forumName: String, onDone: () -> Unit) -> Unit = { _, _, _, _ -> },
) {
    MyLazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (canManage && (visibilitySyncing || visibilityError != null)) item(key = "visibility_status") {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(if (visibilitySyncing) "正在核对帖子可见范围…" else visibilityError.orEmpty(),
                    style = MaterialTheme.typography.caption, color = ExtendedTheme.colors.textSecondary,
                    modifier = Modifier.weight(1f))
                if (!visibilitySyncing) TextButton(onClick = onRetryVisibility) { Text("重试") }
            }
        }
        items(
            items = data,
            key = {
                "${it.data.get { thread_id }}_${it.data.get { post_id }}"
            }
        ) { itemData ->
            Container(fluid = fluid) {
                UserPostItem(
                    post = itemData,
                    canManage = canManage,
                    publicIds = publicIds,
                    visibilityRevision = visibilityRevision,
                    visibilitySyncing = visibilitySyncing,
                    authorName = authorName,
                    authorPortrait = authorPortrait,
                    onClick = onClickItem,
                    onAgree = onAgreeItem,
                    onClickReply = onClickReply,
                    onClickUser = onClickUser,
                    onClickForum = onClickForum,
                    onClickOriginThread = onClickOriginThread,
                    onTogglePrivacy = onTogglePrivacy,
                    onDeleteThread = onDeleteThread,
                )
            }
        }
    }
}

@Composable
fun UserPostItem(
    publicIds: Set<Long>? = null,
    visibilityRevision: Long = 0L,
    visibilitySyncing: Boolean = false,
    canManage: Boolean = false,
    authorName: String = "",
    authorPortrait: String = "",
    post: PostListItemData,
    onAgree: (PostInfoList) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (threadId: Long, postId: Long?, isSubPost: Boolean) -> Unit = { _, _, _ -> },
    onClickReply: (PostInfoList) -> Unit = {},
    onClickUser: (id: Long) -> Unit = {},
    onClickForum: (name: String) -> Unit = {},
    onClickOriginThread: (threadId: Long) -> Unit = {},
    onTogglePrivacy: (fid: Long, tid: Long, pid: Long, hide: Boolean, onDone: (Boolean) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onDeleteThread: (fid: Long, tid: Long, forumName: String, onDone: () -> Unit) -> Unit = { _, _, _, _ -> },
) {
    val item = post.data
    if (post.isThread) {
        val tid = item.get { thread_id }
        val itemContext = LocalContext.current
        val cachedHidden = remember(tid, canManage) {
            if (canManage) ThreadPrivacyCache.getHidden(itemContext, tid) else null
        }
        var confirmedChange by remember(tid, canManage, visibilityRevision) { mutableStateOf<Boolean?>(null) }
        val visibility = ThreadVisibility.resolve(tid, publicIds, item.get { hide_post } == 1,
            confirmedChange, cachedHidden)
        val hidden = visibility == ThreadVisibility.OnlySelf
        var menu by remember(tid) { mutableStateOf(false) }
        var privacyDialog by remember(tid) { mutableStateOf(false) }
        var targetHidden by remember(tid) { mutableStateOf(hidden) }
        var busy by remember(tid) { mutableStateOf(false) }
        var showDeleteConfirm by remember(tid) { mutableStateOf(false) }

        if (privacyDialog && canManage) {
            AlertDialog(
                onDismissRequest = { privacyDialog = false },
                title = { Text("帖子可见范围") },
                text = {
                    Column {
                        listOf(false to "公开", true to "仅自己可见").forEach { (value, label) ->
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { targetHidden = value }
                            ) {
                                androidx.compose.material.RadioButton(selected = targetHidden == value,
                                    onClick = { targetHidden = value })
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = !visibilitySyncing && !busy, onClick = {
                        privacyDialog = false
                        if (!busy) {
                            busy = true
                            val target = targetHidden
                            onTogglePrivacy(item.get { forum_id }, tid, item.get { post_id }, target) { ok ->
                                busy = false
                                if (ok) {
                                    ThreadPrivacyCache.setHidden(itemContext, tid, target)
                                    confirmedChange = target
                                }
                            }
                        }
                    }) { Text("保存") }
                },
                dismissButton = { TextButton(onClick = { privacyDialog = false }) { Text("取消") } }
            )
        }
        if (showDeleteConfirm && canManage) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("删除帖子") },
                text = { Text("确定删除这条帖子吗？删除后无法在这里恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        if (!busy) {
                            busy = true
                            onDeleteThread(item.get { forum_id }, tid, item.get { forum_name }) { busy = false }
                        }
                    }) { Text("删除", color = MaterialTheme.colors.error) }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
            )
        }
        FeedCard(
            item = item,
            profileCard = true,
            profileAuthorName = authorName,
            profileAuthorPortrait = authorPortrait,
            onClick = { onClick(it.thread_id, null, false) },
            onAgree = onAgree,
            modifier = modifier,
            onClickReply = onClickReply,
            onClickUser = onClickUser,
            onClickForum = onClickForum,
            onClickOriginThread = { onClickOriginThread(it.tid.toLong()) },
            headerAction = {
                if (canManage) Text(
                    text = if (visibility == ThreadVisibility.Unknown && visibilitySyncing) "确认中…" else visibility.label,
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.textSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(100)).background(ExtendedTheme.colors.chip)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                if (canManage) Box {
                    androidx.compose.material.IconButton(onClick = { menu = true }, enabled = !busy && !visibilitySyncing) {
                        androidx.compose.material.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Rounded.MoreHoriz,
                            contentDescription = "帖子更多操作", tint = ExtendedTheme.colors.textSecondary)
                    }
                    androidx.compose.material.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        androidx.compose.material.DropdownMenuItem(onClick = {
                            menu = false; targetHidden = hidden; privacyDialog = true
                        }) { Text("可见范围：" + visibility.label) }
                        androidx.compose.material.DropdownMenuItem(onClick = {
                            menu = false; showDeleteConfirm = true
                        }) { Text("删除帖子", color = MaterialTheme.colors.error) }
                    }
                }
            },
        )
    } else {
        Card(
            header = {
                UserHeader(
                    nameProvider = { item.get { user_name } },
                    nameShowProvider = { item.get { name_show } },
                    portraitProvider = { item.get { user_portrait } },
                    onClick = {
                        onClickUser(item.get { user_id })
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            },
            content = {
                Column {
                    post.contents.fastForEach {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClick(
                                        item.get { thread_id },
                                        it.postId,
                                        it.isSubPost
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = it.contentText,
                                style = MaterialTheme.typography.body1,
                                color = ExtendedTheme.colors.text,
                            )

                            Text(
                                text = DateTimeUtils.getRelativeTimeString(
                                    LocalContext.current,
                                    it.createTime
                                ),
                                style = MaterialTheme.typography.caption,
                                color = ExtendedTheme.colors.textSecondary,
                            )
                        }
                    }
                }

                Text(
                    text = item.get { title },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ExtendedTheme.colors.floorCard)
                        .clickable {
                            onClickOriginThread(item.get { thread_id })
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.body2,
                )
            },
            modifier = modifier,
            contentPadding = PaddingValues(0.dp),
        )
    }
}
