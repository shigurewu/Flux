package com.huanchengfly.tieba.post.ui.page.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

/** 私信作为消息中心的第四个分类，独立进入，返回回到原消息列表。 */
@Destination
@Composable
fun ConversationsPage(navigator: DestinationsNavigator) {
    if (!com.huanchengfly.tieba.post.im.PrivateMessaging.enabled) {
        Text("私信功能暂未开放"); return
    }
    ProvideNavigator(navigator) {
        MyScaffold(topBar = {
            TitleCentredToolbar(
                title = { Text(stringResource(R.string.title_chat)) },
                navigationIcon = { BackNavigationIcon { navigator.navigateUp() } }
            )
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) { ChatListPage() }
        }
    }
}
