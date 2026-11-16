package com.huanchengfly.tieba.post.ui.page.history

import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.localDataOwner
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.key
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListPage
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://history")
    ]
)
@Composable
fun HistoryPage(
    navigator: DestinationsNavigator
) {
    val pagerState = rememberPagerState { 2 }
    val owner = localDataOwner(LocalAccount.current?.uid)
    key(owner) {
        val coroutineScope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()
        val context = LocalContext.current
        val confirmClear = rememberDialogState()
        ConfirmDialog(
            dialogState = confirmClear,
            title = { Text(stringResource(R.string.title_history_delete)) },
            content = { Text(stringResource(R.string.message_clear_browsing_history)) },
            onConfirm = {
                if (localDataOwner(AccountUtil.currentAccount?.uid) == owner) coroutineScope.launch {
                    try {
                        HistoryUtil.deleteAll(owner)
                        if (localDataOwner(AccountUtil.currentAccount?.uid) == owner) {
                            emitGlobalEvent(HistoryListUiEvent.DeleteAll)
                            scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_success))
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_failure, error.localizedMessage.orEmpty()))
                    }
                }
            },
        )
        MyScaffold(
            backgroundColor = Color.Transparent,
            scaffoldState = scaffoldState,
            topBar = {
                TitleCentredToolbar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.title_history),
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                        )
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                    },
                    actions = {
                        IconButton(onClick = { confirmClear.show() }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(id = R.string.title_history_delete),
                                tint = ExtendedTheme.colors.onTopBar
                            )
                        }
                    },
                    content = {
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            indicator = { tabPositions ->
                                PagerTabIndicator(
                                    pagerState = pagerState,
                                    tabPositions = tabPositions
                                )
                            },
                            divider = {},
                            backgroundColor = Color.Transparent,
                            contentColor = ExtendedTheme.colors.primary,
                            modifier = Modifier
                                .width(100.dp * 2)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Tab(
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.title_history_thread),
                                        fontSize = 13.sp
                                    )
                                },
                                selected = pagerState.currentPage == 0,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                                selectedContentColor = ExtendedTheme.colors.onTopBar,
                                unselectedContentColor = ExtendedTheme.colors.onTopBarSecondary
                            )
                            Tab(
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.title_history_forum),
                                        fontSize = 13.sp
                                    )
                                },
                                selected = pagerState.currentPage == 1,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                selectedContentColor = ExtendedTheme.colors.onTopBar,
                                unselectedContentColor = ExtendedTheme.colors.onTopBarSecondary
                            )
                        }
                    }
                )
            }
        ) {
            ProvideNavigator(navigator = navigator) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = true,
                ) {
                    if (it == 0) {
                        HistoryListPage(type = HistoryUtil.TYPE_THREAD)
                    } else {
                        HistoryListPage(type = HistoryUtil.TYPE_FORUM)
                    }
                }
            }
        }
    }
}
