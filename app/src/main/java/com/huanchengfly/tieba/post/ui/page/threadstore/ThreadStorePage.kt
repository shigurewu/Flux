package com.huanchengfly.tieba.post.ui.page.threadstore

import com.huanchengfly.tieba.post.ui.widgets.compose.fluxListCard

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchBox
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.ThreadStoreBean
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.pxToSp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFromStoreExtra
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.PullRefreshLogoIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import androidx.compose.material.TextButton
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getUsernameAnnotatedString
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException

private val UpdateTipTextStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp)

@OptIn(ExperimentalMaterialApi::class, ExperimentalTextApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://favorite")
    ]
)
@Composable
fun ThreadStorePage(
    navigator: DestinationsNavigator,
    viewModel: ThreadStoreViewModel = pageViewModel(key = "favorites_${AccountUtil.LocalAccount.current?.uid}")
) {
    val uid = AccountUtil.LocalAccount.current?.uid
    var searchOpen by rememberSaveable(uid) { mutableStateOf(false) }
    var query by rememberSaveable(uid) { mutableStateOf("") }
    var lastPositionedQuery by rememberSaveable(uid) { mutableStateOf("") }
    var searchGeneration by remember(uid) { mutableIntStateOf(0) }
    var searchResult by remember(uid) { mutableStateOf<Result<List<ThreadStoreBean.ThreadStoreInfo>>?>(null) }
    var indexedFavorites by remember(uid) { mutableStateOf(emptyList<ThreadStoreBean.ThreadStoreInfo>()) }
    val hasQuery = searchOpen && uid != null && query.isNotBlank()
    val matchingFavorites = remember(indexedFavorites, query) {
        indexedFavorites.filterFavorites(query)
    }
    val focusManager = LocalFocusManager.current
    val normalListState = rememberLazyListState()
    val searchListState = rememberLazyListState()

    fun closeSearch() {
        focusManager.clearFocus()
        searchOpen = false
        query = ""
        lastPositionedQuery = ""
        searchResult = null
        indexedFavorites = emptyList()
    }

    BackHandler(enabled = searchOpen) { closeSearch() }
    LaunchedEffect(uid, hasQuery, searchGeneration) {
        if (!hasQuery || searchResult != null) return@LaunchedEffect
        try {
            val favorites = loadSearchableFavorites(onPage = { indexedFavorites = it }) { page ->
                if (AccountUtil.currentAccount?.uid != uid) throw CancellationException()
                val response = TiebaApi.getInstance().threadStoreFlow(page).first()
                if (AccountUtil.currentAccount?.uid != uid) throw CancellationException()
                response.storeThread ?: throw IOException("收藏列表加载失败")
            }
            indexedFavorites = favorites
            searchResult = Result.success(favorites)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            searchResult = Result.failure(e)
        }
    }

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ThreadStoreUiIntent.Refresh)
        viewModel.initialized = true
    }
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::hasMore,
        initial = true
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::currentPage,
        initial = 0
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::data,
        initial = emptyList()
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ThreadStoreUiState::error,
        initial = null
    )
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    viewModel.onEvent<ThreadStoreUiEvent.Delete.Failure> {
        scaffoldState.snackbarHostState.showSnackbar(
            context.getString(
                R.string.delete_store_failure,
                it.errorMsg
            )
        )
    }
    viewModel.onEvent<ThreadStoreUiEvent.Delete.Success> {
        if (searchOpen) {
            searchResult = null
            searchGeneration++
        }
        scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.delete_store_success))
    }
    MyScaffold(
        backgroundColor = Color.Transparent,
        scaffoldState = scaffoldState,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_my_collect),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { if (searchOpen) closeSearch() else navigator.navigateUp() })
                },
                actions = {
                    if (uid != null && !searchOpen) {
                        ActionItem(Icons.Rounded.Search, stringResource(R.string.hint_search_favorites)) {
                            searchOpen = true
                        }
                    }
                },
                content = {
                    if (searchOpen) {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
                        SearchBox(
                            keyword = query,
                            onKeywordChange = { query = it },
                            onKeywordSubmit = { focusManager.clearFocus() },
                            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                                .widthIn(max = 760.dp).fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                            focusRequester = focusRequester,
                            placeholder = { Text(stringResource(R.string.hint_search_favorites_fields)) },
                            shape = RoundedCornerShape(12.dp),
                            color = ExtendedTheme.colors.topBarSurface,
                            showClearWhenUnfocused = true,
                        )
                    }
                },
            )
        }
    ) { contentPaddings ->
        val visibleFavorites = if (hasQuery) matchingFavorites else data
        val visibleError = if (hasQuery) searchResult?.exceptionOrNull() else error?.let { (e) -> e }
        val refreshing = if (hasQuery) searchResult == null else isRefreshing
        val refresh: () -> Unit = {
            if (hasQuery) {
                searchResult = null
                searchGeneration++
            } else viewModel.send(ThreadStoreUiIntent.Refresh)
        }
        StateScreen(
            isEmpty = visibleFavorites.isEmpty(),
            isError = visibleError != null && (!hasQuery || visibleFavorites.isEmpty()),
            isLoading = refreshing && (!hasQuery || visibleFavorites.isEmpty()),
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPaddings)
                .wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 760.dp).fillMaxWidth(),
            onReload = refresh,
            errorScreen = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.tip_favorites_load_failed))
                    TextButton(onClick = refresh) { Text(stringResource(R.string.btn_reload)) }
                }
            },
            emptyScreen = {
                if (hasQuery) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.tip_search_favorites_empty_query, query.trim()), color = ExtendedTheme.colors.textSecondary)
                    }
                } else {
                    Text(stringResource(R.string.tip_no_favorites), color = ExtendedTheme.colors.textSecondary)
                }
            }
        ) {
            val pullRefreshState = rememberPullRefreshState(
                refreshing = refreshing,
                onRefresh = refresh
            )

            val lazyListState = if (hasQuery) searchListState else normalListState
            LaunchedEffect(query) {
                if (query != lastPositionedQuery) {
                    searchListState.scrollToItem(0)
                    lastPositionedQuery = query
                }
            }

            Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
                LoadMoreLayout(
                    isLoading = !hasQuery && isLoadingMore,
                    onLoadMore = {
                        if (!hasQuery) viewModel.send(ThreadStoreUiIntent.LoadMore(currentPage + 1))
                    },
                    enableLoadMore = !hasQuery,
                    loadEnd = !hasMore,
                    lazyListState = lazyListState
                ) {
                    MyLazyColumn(state = lazyListState) {
                        if (hasQuery && (refreshing || visibleError != null)) item("favorites_search_progress") {
                            Text(
                                text = if (visibleError == null) stringResource(R.string.message_favorites_indexing, indexedFavorites.size)
                                else stringResource(R.string.message_favorites_search_retry, indexedFavorites.size),
                                color = ExtendedTheme.colors.textSecondary,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.fillMaxWidth()
                                    .then(if (visibleError != null) Modifier.clickable(onClick = refresh) else Modifier)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                        items(
                            items = visibleFavorites,
                            key = { it.threadId }
                        ) { info ->
                            StoreItem(
                                info = info,
                                onUserClick = {
                                    info.author.lzUid?.let {
                                        navigator.navigate(UserProfilePageDestination(it.toLong()))
                                    }
                                },
                                onClick = {
                                    navigator.navigate(
                                        ThreadPageDestination(
                                            threadId = info.threadId.toLong(),
                                            postId = info.markPid.toLong(),
                                            seeLz = context.appPreferences.collectThreadSeeLz,
                                            sortType = if(context.appPreferences.collectThreadDescSort) ThreadSortType.SORT_TYPE_DESC else ThreadSortType.SORT_TYPE_DEFAULT,
                                            from = ThreadPageFrom.FROM_STORE,
                                            extra = ThreadPageFromStoreExtra(
                                                maxPid = info.maxPid.toLong(),
                                                maxFloor = info.postNo.toInt()
                                            )
                                        )
                                    )
                                },
                                onDelete = {
                                    viewModel.send(
                                        ThreadStoreUiIntent.Delete(
                                            info.threadId
                                        )
                                    )
                                }
                            )
                        }
                        if (!hasQuery && visibleError != null && !refreshing) {
                            item("favorites_retry") {
                                TextButton(onClick = refresh, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.tip_favorites_retry))
                                }
                            }
                        }
                    }
                }

                PullRefreshLogoIndicator(
                    refreshing = refreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                    contentColor = ExtendedTheme.colors.onPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun StoreItem(
    info: ThreadStoreBean.ThreadStoreInfo,
    onUserClick: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUpdate = remember(info) { info.count != "0" && info.postNo != "0" }
    val isDeleted = remember(info) { info.isDeleted == "1" }
    val textMeasurer = rememberTextMeasurer()
    LongClickMenu(
        modifier = Modifier.fluxListCard(),
        menuContent = {
            DropdownMenuItem(onClick = onDelete) {
                Text(text = stringResource(id = R.string.title_collect_on))
            }
        },
        onClick = onClick
    ) {
        Column(
            modifier = modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserHeader(
                avatar = {
                    Avatar(
                        data = StringUtil.getAvatarUrl(info.author.userPortrait),
                        size = Sizes.Small,
                        contentDescription = null
                    )
                },
                name = {
                    Text(
                        text = getUsernameAnnotatedString(
                            LocalContext.current,
                            info.author.name ?: "",
                            info.author.nameShow,
                            LocalContentColor.current
                        )
                    )
                },
                onClick = onUserClick,
            )
            val title = remember(info, hasUpdate) {
                buildAnnotatedString {
                    append(info.title)
                    if (hasUpdate) {
                        append(" ")
                        appendInlineContent("Update", info.postNo)
                    }
                }
            }
            val updateTip = stringResource(
                id = R.string.tip_thread_store_update,
                info.postNo
            )
            val result = remember {
                textMeasurer.measure(
                    AnnotatedString(updateTip),
                    style = UpdateTipTextStyle
                ).size
            }
            val width = result.width.pxToSp() + 12F.dpToPxFloat().pxToSp() * 2 + 1
            val height = result.height.pxToSp() + 4F.dpToPxFloat().pxToSp() * 2
            Text(
                text = title,
                fontSize = 15.sp,
                color = if (isDeleted) ExtendedTheme.colors.textDisabled else ExtendedTheme.colors.text,
                inlineContent = mapOf(
                    "Update" to InlineTextContent(
                        placeholder = Placeholder(
                            width = width.sp,
                            height = height.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        ),
                        children = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = ExtendedTheme.colors.chip,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .padding(vertical = 4.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = updateTip,
                                    style = UpdateTipTextStyle,
                                    color = ExtendedTheme.colors.onChip,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    )
                )
            )
            if (isDeleted) {
                Text(
                    text = stringResource(id = R.string.tip_thread_store_deleted),
                    fontSize = 12.sp,
                    color = ExtendedTheme.colors.textDisabled
                )
            }
        }
    }
}
