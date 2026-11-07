package com.huanchengfly.tieba.post.ui.page.subposts

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.bawuType
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.threadBottomBar
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.CopyTextDialogPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.reply.ReplyArgs
import com.huanchengfly.tieba.post.ui.page.reply.ReplyDialog
import com.huanchengfly.tieba.post.ui.page.thread.PostAgreeBtn
import com.huanchengfly.tieba.post.ui.page.thread.PostCard
import com.huanchengfly.tieba.post.ui.page.thread.UserNameText
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockTip
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.states.SubPostsSkeleton
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyleBottomSheet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Destination
@Composable
fun SubPostsPage(
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long = 0L,
    postId: Long = 0L,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            loadFromSubPost = loadFromSubPost,
            onNavigateUp = { navigator.navigateUp() }
        )
    }
}

@Destination(
    style = DestinationStyleBottomSheet::class
)
@Composable
fun SubPostsSheetPage(
    navigator: DestinationsNavigator,
    threadId: Long,
    forumId: Long = 0L,
    postId: Long = 0L,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostId = subPostId,
            loadFromSubPost = loadFromSubPost,
            isSheet = true,
            onNavigateUp = { navigator.navigateUp() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
internal fun SubPostsContent(
    viewModel: SubPostsViewModel,
    forumId: Long,
    threadId: Long,
    postId: Long,
    subPostId: Long = 0L,
    loadFromSubPost: Boolean = false,
    isSheet: Boolean = false,
    onNavigateUp: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val account = LocalAccount.current

    LazyLoad(key = viewModel, loaded = viewModel.initialized) {
        viewModel.send(
            SubPostsUiIntent.Load(
                forumId,
                threadId,
                postId,
                subPostId.takeIf { loadFromSubPost } ?: 0L
            )
        )
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isRefreshing,
        initial = false
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isLoading,
        initial = false
    )
    val anti by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::anti,
        initial = null
    )
    val forum by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::forum,
        initial = null
    )
    val thread by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::thread,
        initial = null
    )
    val post by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::post,
        initial = null
    )
    val postContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::postContentRenders,
        initial = persistentListOf()
    )
    val subPosts by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::subPosts,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::currentPage,
        initial = 1
    )
    val totalCount by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::totalCount,
        initial = 0
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::hasMore,
        initial = true
    )

    val lazyListState = rememberLazyListState()
    var highlightSubPostId by remember { mutableStateOf(0L) }
    var hasCentered by remember { mutableStateOf(false) }

    // ── 高亮：与滚动解耦，只要数据里出现目标回复就亮，2.5s 后自动清除 ──
    LaunchedEffect(subPosts, subPostId) {
        if (subPostId == 0L) return@LaunchedEffect
        if (subPosts.none { it.id == subPostId }) return@LaunchedEffect
        Log.e(
            "SubPostsLocate",
            "[定位排查 1] 接收到的 targetReplyId=$subPostId，数据条数=${subPosts.size}，列表已就绪，开始高亮"
        )
        highlightSubPostId = subPostId
        delay(2500)
        highlightSubPostId = 0L
    }

    // ── 滚动：轮询等待列表就绪 → 粗定位 → 量高度 → 像素级居中 ──
    LaunchedEffect(subPosts, subPostId) {
        if (hasCentered || subPostId == 0L) return@LaunchedEffect

        Log.e(
            "SubPostsLocate",
            "[定位排查 1] 定位流程启动 targetReplyId=$subPostId，当前数据条数=${subPosts.size}"
        )

        // 每 100ms 轮询一次；目标不在当前页时自动向后加载，最多多拉 2 页（总时长约 8 秒）
        var targetIndex = -1
        var loadPage = currentPage + 1
        var extraLoads = 0
        for (attempt in 1..80) {
            val idx = subPosts.indexOfFirst { it.id == subPostId }
            val total = lazyListState.layoutInfo.totalItemsCount
            if (idx >= 0 && total > idx + 2) {
                targetIndex = idx + 2
                Log.e(
                    "SubPostsLocate",
                    "[定位排查 2] 第 $attempt 次轮询命中：数据下标=$idx，列表下标=$targetIndex，totalItems=$total"
                )
                break
            }
            // 每 1 秒还没找到，就认为目标在后续页，主动加载下一页
            if (attempt % 10 == 0 && extraLoads < 2) {
                extraLoads++
                Log.e(
                    "SubPostsLocate",
                    "[定位排查 2] 目标不在已加载数据中，自动加载第 $loadPage 页（第 $extraLoads 次）"
                )
                viewModel.send(
                    SubPostsUiIntent.LoadMore(
                        forumId = forumId,
                        threadId = threadId,
                        postId = postId,
                        subPostId = subPostId,
                        page = loadPage,
                    )
                )
                loadPage++
            }
            Log.e(
                "SubPostsLocate",
                "[定位排查 2] 第 $attempt 次轮询未就绪：数据下标=$idx，totalItems=$total"
            )
            delay(100)
        }
        if (targetIndex < 0) {
            Log.e("SubPostsLocate", "[定位排查 2] 轮询 20 次仍未找到目标，放弃定位")
            return@LaunchedEffect
        }

        // 先把目标项滚入可视区（粗定位），否则无法测量它的高度
        lazyListState.scrollToItem(targetIndex)

        // 再轮询等它被测量出来
        var measured = false
        for (attempt in 1..20) {
            val info = lazyListState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == targetIndex }
            if (item != null && item.size > 0) {
                measured = true
                break
            }
            delay(100)
        }
        if (!measured) {
            Log.e("SubPostsLocate", "[定位排查 3] 目标项未完成测量，放弃精确居中")
            return@LaunchedEffect
        }

        val info = lazyListState.layoutInfo
        val viewport = info.viewportSize.height
        val item = info.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        if (item == null || viewport <= 0) {
            Log.e("SubPostsLocate", "[定位排查 3] viewport=$viewport item=$item，无法居中")
            return@LaunchedEffect
        }

        // 中间偏上的“黄金阅读区”:目标项顶边落在 (viewport - itemHeight) * 0.3 处
        val desiredTop = ((viewport - item.size) * 0.3f).toInt()
        val delta = item.offset - desiredTop
        Log.e(
            "SubPostsLocate",
            "[定位排查 3] viewport=$viewport itemHeight=${item.size} itemOffset=${item.offset} " +
                    "desiredTop=$desiredTop delta=$delta"
        )

        // 像素级修正：正数向前滚（内容上移），负数向后滚（内容下移）
        lazyListState.scrollBy(delta.toFloat())
        hasCentered = true
        Log.e("SubPostsLocate", "[定位排查 3] 居中完成，已滚动 delta=$delta")
    }

    val confirmDeleteDialogState = rememberDialogState()
    var deleteSubPost by remember { mutableStateOf<ImmutableHolder<SubPostList>?>(null) }
    ConfirmDialog(
        dialogState = confirmDeleteDialogState,
        onConfirm = {
            if (deleteSubPost == null) {
                val isSelfPost =
                    if (post?.get { author_id } == 0L) post?.get { author?.id } == account?.uid?.toLongOrNull() else post?.get { author_id } == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forum?.get { id } ?: forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = post?.get { id } ?: postId,
                        deleteMyPost = isSelfPost,
                        tbs = anti?.get { tbs } ?: account?.tbs,
                    )
                )
            } else {
                val isSelfSubPost =
                    if (deleteSubPost!!.get { author_id } == 0L) deleteSubPost!!.get { author?.id } == account?.uid?.toLongOrNull() else
                        deleteSubPost!!.get { author_id } == account?.uid?.toLongOrNull()
                viewModel.send(
                    SubPostsUiIntent.DeletePost(
                        forumId = forum?.get { id } ?: forumId,
                        forumName = forum?.get { name }.orEmpty(),
                        threadId = threadId,
                        postId = post?.get { id } ?: postId,
                        subPostId = deleteSubPost!!.get { id },
                        deleteMyPost = isSelfSubPost,
                        tbs = anti?.get { tbs } ?: account?.tbs,
                    )
                )
            }
        }
    ) {
        Text(
            text = stringResource(
                id = R.string.message_confirm_delete,
                if (deleteSubPost == null && post != null) stringResource(
                    id = R.string.tip_post_floor,
                    post!!.get { floor })
                else stringResource(id = R.string.this_reply)
            )
        )
    }

    val replyDialogState = rememberDialogState()
    var currentReplyArgs by remember { mutableStateOf<ReplyArgs?>(null) }
    if (currentReplyArgs != null) {
        ReplyDialog(args = currentReplyArgs!!, state = replyDialogState)
    }

//    onGlobalEvent<GlobalEvent.ReplySuccess>(
//        filter = { it.threadId == threadId && it.postId == postId }
//    ) { event ->
//        viewModel.send(
//            SubPostsUiIntent.Load(
//                forumId,
//                threadId,
//                postId,
//                subPostId.takeIf { loadFromSubPost } ?: 0L
//            )
//        )
//    }

    fun showReplyDialog(args: ReplyArgs) {
        currentReplyArgs = args
        replyDialogState.show()
    }

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = subPosts.isEmpty(),
        isError = false,
        isLoading = isRefreshing,
        loadingScreen = { SubPostsSkeleton() }
    ) {
        MyScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(text = post?.let {
                            stringResource(
                                id = R.string.title_sub_posts,
                                it.get { floor })
                        } ?: stringResource(id = R.string.title_sub_posts_default),
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = if (isSheet) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.btn_close)
                            )
                        }
                    },
                    actions = {
                        if (!isSheet) {
                            IconButton(onClick = {
                                navigator.navigate(
                                    ThreadPageDestination(
                                        forumId = forum?.get { id } ?: forumId,
                                        threadId = threadId,
                                        postId = post?.get { id } ?: postId
                                    )
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInBrowser,
                                    contentDescription = stringResource(id = R.string.btn_open_origin_thread)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (account != null && !LocalContext.current.appPreferences.hideReply) {
                    Column(
                        modifier = Modifier.background(ExtendedTheme.colors.threadBottomBar)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Avatar(
                                data = StringUtil.getAvatarUrl(account.portrait),
                                size = Sizes.Tiny,
                                contentDescription = account.name,
                            )
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ExtendedTheme.colors.bottomBarSurface)
//                                    .clickable {
//                                        val fid = forum?.get { id } ?: forumId
//                                        val forumName = forum?.get { name }
//                                        if (fid != 0L) {
//                                            showReplyDialog(
//                                                ReplyArgs(
//                                                    forumId = fid,
//                                                    forumName = forumName.toString(),
//                                                    threadId = threadId,
//                                                    postId = postId,
//                                                )
//                                            )
//                                        }
//                                    }
                                    .debounceClickable(onClick = {
                                        val fid = forum?.get { id } ?: forumId
                                        val forumName = forum?.get { name }
                                        if (fid != 0L) {
                                            showReplyDialog(
                                                ReplyArgs(
                                                    forumId = fid,
                                                    forumName = forumName.toString(),
                                                    threadId = threadId,
                                                    postId = post?.get { id } ?: postId,
                                                )
                                            )
                                        }
                                    })
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.tip_reply_thread),
                                    style = MaterialTheme.typography.caption,
                                    color = ExtendedTheme.colors.onBottomBarSurface,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .requiredHeightIn(min = if (LocalContext.current.appPreferences.liftUpBottomBar) 16.dp else 0.dp)
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LoadMoreLayout(
                modifier = Modifier.padding(paddingValues),
                // 有回复栏时容器底缘即栏上沿(胶囊从栏后滑出、停在栏上方 8dp);
                // 无回复栏时容器直达屏幕底,需额外避开系统导航栏
                indicatorBottomInset = if (account != null && !LocalContext.current.appPreferences.hideReply) {
                    8.dp
                } else {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
                },
                isLoading = isLoading,
                onLoadMore = {
                    viewModel.send(
                        SubPostsUiIntent.LoadMore(
                            forumId,
                            threadId,
                            postId,
//                            subPostId,
                            page = currentPage + 1,
                        )
                    )
                },
                loadEnd = !hasMore,
                lazyListState = lazyListState,
                isEmpty = post == null && subPosts.isEmpty(),
            ) {
                MyLazyColumn(state = lazyListState) {
                    item(key = "Post$postId") {
                        post?.let {
                            Column {
                                PostCard(
                                    postHolder = it,
                                    contentRenders = postContentRenders,
                                    canDelete = { it.author_id == account?.uid?.toLongOrNull() || it.author?.id == account?.uid?.toLongOrNull() || thread?.get { author?.id } ==  account?.uid?.toLongOrNull() },
                                        showSubPosts = false,
                                    onUserClick = {
                                        navigator.navigate(UserProfilePageDestination(it.id))
                                    },
                                    onAgree = {
                                        val hasAgreed = it.get { agree?.hasAgree != 0 }
                                        viewModel.send(
                                            SubPostsUiIntent.Agree(
                                                forumId,
                                                threadId,
                                                post?.get { id } ?: postId,
                                                agree = !hasAgreed
                                            )
                                        )
                                    },
                                    onReplyClick = {
                                        val fid = forum?.get { id } ?: forumId
                                        if (fid != 0L) {
                                            showReplyDialog(
                                                ReplyArgs(
                                                    forumId = fid,
                                                    forumName = forum?.get { name } ?: "",
                                                    threadId = threadId,
                                                    postId =  post?.get { id } ?: postId,
                                                    replyUserId = it.author?.id ?: it.author_id,
                                                    replyUserName = it.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                                        ?: it.author?.name,
                                                    replyUserPortrait = it.author?.portrait,
                                                )
                                            )
                                        }
                                    },
                                    onMenuCopyClick = {
                                        navigator.navigate(
                                            CopyTextDialogPageDestination(it)
                                        )
                                    },
                                ) {
                                    deleteSubPost = null
                                    confirmDeleteDialogState.show()
                                }
                            }
                        }
                    }
                    stickyHeader(key = "SubPostsHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExtendedTheme.colors.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.title_sub_posts_header,
                                    totalCount
                                ),
                                style = MaterialTheme.typography.subtitle1
                            )
                        }
                    }
                    itemsIndexed(
                        items = subPosts,
                        key = { _, subPost -> subPost.id }
                    ) { index, item ->
                        SubPostItem(
                            item = item,
                            highlighted = item.id == highlightSubPostId,
                            canDelete = { it.author_id == account?.uid?.toLongOrNull() || it.author?.id == account?.uid?.toLongOrNull() || thread?.get { author?.id } ==  account?.uid?.toLongOrNull() },
                            threadAuthorId = thread?.get { author?.id },
                            onUserClick = {
                                navigator.navigate(UserProfilePageDestination(it.id))
                            },
                            onAgree = {
                                val hasAgreed = it.agree?.hasAgree != 0
                                viewModel.send(
                                    SubPostsUiIntent.Agree(
                                        forumId,
                                        threadId,
                                        post?.get { id } ?: postId,
                                        subPostId = it.id,
                                        agree = !hasAgreed
                                    )
                                )
                            },
                            onReplyClick = {
                                val fid = forum?.get { id } ?: forumId
                                if (fid != 0L) {
                                    showReplyDialog(
                                        ReplyArgs(
                                            forumId = fid,
                                            forumName = forum?.get { name } ?: "",
                                            threadId = threadId,
                                            postId = post?.get { id } ?: postId,
                                            subPostId = it.id,
                                            replyUserId = it.author?.id ?: it.author_id,
                                            replyUserName = it.author?.nameShow.takeIf { name -> !name.isNullOrEmpty() }
                                                ?: it.author?.name,
                                            replyUserPortrait = it.author?.portrait,
                                        )
                                    )
                                }
                            },
                            onMenuCopyClick = {
                                navigator.navigate(
                                    CopyTextDialogPageDestination(it)
                                )
//                                TiebaUtil.copyText(context, it)
                            },
                            onMenuDeleteClick = {
                                deleteSubPost = it.wrapImmutable()
                                confirmDeleteDialogState.show()
                            },
                        )
                        if (index < subPosts.lastIndex) {
                            // 扁平列表用发丝线分隔:高密度,替代卡片分块
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .height(0.5.dp)
                                    .background(ExtendedTheme.colors.divider.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getDescText(
    time: Long?,
    ipAddress: String?
): String {
    val texts = listOfNotNull(
        time?.let { DateTimeUtils.getRelativeTimeString(App.INSTANCE, it) },
        ipAddress?.takeIf { it.isNotBlank() }?.let { App.INSTANCE.getString(R.string.text_ip_location, it) }
    )
    if (texts.isEmpty()) return ""
    return texts.joinToString(" ")
}

@Composable
private fun SubPostItem(
    item: SubPostItemData,
    highlighted: Boolean = false,
    threadAuthorId: Long? = null,
    canDelete: (SubPostList) -> Boolean = { false },
    onUserClick: (User) -> Unit = {},
    onAgree: (SubPostList) -> Unit = {},
    onReplyClick: (SubPostList) -> Unit = {},
    onMenuCopyClick: ((String) -> Unit)? = null,
    onMenuDeleteClick: ((SubPostList) -> Unit)? = null,
) {
    val (subPost, contentRenders, blocked) = item
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val account = LocalAccount.current
    val coroutineScope = rememberCoroutineScope()
    val author = remember(subPost) { subPost.get { author }?.wrapImmutable() }
    val hasAgreed = remember(subPost) {
        subPost.get { agree?.hasAgree == 1 }
    }
    val agreeNum = remember(subPost) {
        subPost.get { agree?.diffAgreeNum ?: 0L }
    }
    // 楼中楼图片：内容里的 render 不带 photoViewData 时，手动构造一份，
    // 否则缩略图没有点击目标，点击会冒泡到「回复」上
    val subPostPics = remember(contentRenders) {
        contentRenders.filterIsInstance<PicContentRender>()
    }
    val subPostPhotoViewData = remember(subPostPics, subPost) {
        val postId = subPost.get { id }
        PhotoViewData(
            picItems = subPostPics.mapIndexed { index, pic ->
                PicItem(
                    picId = pic.picId,
                    picIndex = index,
                    url = pic.picUrl,
                    originUrl = pic.originUrl,
                    showOriginBtn = pic.showOriginBtn,
                    originSize = pic.originSize,
                    postId = postId,
                    width = pic.width,
                    height = pic.height,
                )
            },
            index = 0
        )
    }
    val menuState = rememberMenuState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        BlockableContent(
            blocked = blocked,
            blockedTip = { BlockTip(text = { Text(text = stringResource(id = R.string.tip_blocked_sub_post)) }) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
        LongClickMenu(
            menuState = menuState,
            indication = null,
            menuContent = {
                if (onMenuCopyClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuCopyClick(contentRenders.joinToString("\n") { it.toString() })
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.menu_copy))
                    }
                }
                if (account != null) {
                    if (!context.appPreferences.hideReply) {
                        DropdownMenuItem(
                            onClick = {
                                onReplyClick(subPost.get())
                                menuState.expanded = false
                            }
                        ) {
                            Text(text = stringResource(id = R.string.btn_reply))
                        }
                    }
                    DropdownMenuItem(
                        onClick = {
                            coroutineScope.launch {
                                TiebaUtil.reportPost(
                                    context,
                                    navigator,
                                    subPost.get { id }.toString()
                                )
                            }
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_report))
                    }
                }
                if (canDelete(subPost.get()) && onMenuDeleteClick != null) {
                    DropdownMenuItem(
                        onClick = {
                            onMenuDeleteClick(subPost.get())
                            menuState.expanded = false
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_delete))
                    }
                }
            },
            onClick = { onReplyClick(subPost.get()) }.takeUnless { context.appPreferences.hideReply || account == null }
        ) {
            // 扁平回复行:无卡片底,发丝线分隔(由列表项之间绘制),高密度对话流
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                author?.let {
                    UserHeader(
                        avatar = {
                            Avatar(
                                data = StringUtil.getAvatarUrl(it.get { portrait }),
                                size = Sizes.Small,
                                contentDescription = null
                            )
                        },
                        name = {
                            UserNameText(
                                userName = StringUtil.getUsernameAnnotatedString(
                                    LocalContext.current,
                                    it.get { name },
                                    it.get { nameShow }
                                ),
                                userLevel = it.get { level_id },
                                isLz = it.get { id } == threadAuthorId,
                                bawuType = it.get { bawuType },
                            )
                        },
                        desc = {
                            Text(
                                text = getDescText(
                                    subPost.get { time }.toLong(),
                                    it.get { ip_address })
                            )
                        },
                        onClick = {
                            onUserClick(it.get())
                        }
                    ) {
                        PostAgreeBtn(
                            hasAgreed = hasAgreed,
                            agreeNum = agreeNum,
                            onClick = { onAgree(subPost.get()) }
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 8.dp, start = Sizes.Small + 8.dp)
                        .fillMaxWidth()
                ) {
                    contentRenders.fastForEach { render ->
                        if (render is PicContentRender) {
                            // 楼中楼图片按原图比例缩略：高度固定 120dp，宽度按比例并限制在 60~200dp
                            val ratio = if (render.width > 0 && render.height > 0) {
                                render.width.toFloat() / render.height
                            } else 1f
                            val thumbWidth = (120f * ratio).coerceIn(60f, 200f)
                            NetworkImage(
                                imageUri = render.picUrl,
                                contentDescription = stringResource(id = R.string.desc_image),
                                modifier = Modifier
                                    .height(120.dp)
                                    .width(thumbWidth.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                photoViewData = render.photoViewData
                                    ?: subPostPhotoViewData.copy(
                                        index = subPostPics.indexOfFirst { it === render }
                                            .coerceAtLeast(0)
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            render.Render()
                        }
                    }
                }
            }
        }
        }

        // 绝对定位遮罩层：盖在整条回复最上层，不受子元素背景/裁剪影响
        if (highlighted) {
            HighlightOverlay(modifier = Modifier.matchParentSize())
        }
    }
}

/**
 * 0.8s 单次呼吸遮罩：中性灰细边框 + 极淡贴合底色 + 轻微投影，
 * 渐显到 0.35 处最亮，随后柔和消失（结束后自身透明，相当于移除）。
 */
@Composable
private fun HighlightOverlay(modifier: Modifier = Modifier) {
    val breath = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        breath.snapTo(0f)
        breath.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 800
                0f at 0 using LinearEasing
                1f at 280 using LinearEasing
                0f at 800 using LinearEasing
            }
        )
    }
    // 起始轻微缩小 0.995，随呼吸回到 1.0
    val scale = 0.995f + 0.005f * breath.value.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(10f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = Color(0x26787880),
                spotColor = Color(0x26787880)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x14787880))
            .border(1.5.dp, Color(0x73787880), RoundedCornerShape(8.dp))
            .alpha(breath.value)
    )
}
