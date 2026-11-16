package com.huanchengfly.tieba.post.ui.page.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.im.ImManager
import com.huanchengfly.tieba.post.im.ImStatus
import com.huanchengfly.tieba.post.utils.AccountUtil
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
fun ConversationsPage(navigator: DestinationsNavigator, folded: Boolean = false) {
    if (!com.huanchengfly.tieba.post.im.PrivateMessaging.enabled) {
        Text("私信功能暂未开放"); return
    }
    val uid = AccountUtil.LocalAccount.current?.uid.orEmpty()
    val status by ImManager.imStatus.collectAsState()
    ProvideNavigator(navigator) {
        MyScaffold(topBar = {
            TitleCentredToolbar(
                title = { Text(if (folded) "折叠消息" else stringResource(R.string.title_chat)) },
                navigationIcon = { BackNavigationIcon { navigator.navigateUp() } }
            )
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (uid.isNotBlank() && status != ImStatus.Ready) {
                    Text(when {
                        status is ImStatus.Connecting -> "正在连接私信…"
                        status is ImStatus.Error -> "私信连接暂不可用，正在重连"
                        else -> "等待网络连接…"
                    }, modifier = Modifier.padding(16.dp))
                }
                Box(Modifier.weight(1f)) { ChatListPage(folded = folded) }
            }
        }
    }
}
