package com.huanchengfly.tieba.post.ui.page.user.likeforum

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
internal fun ForumPrivacyHeader(visibility: ForumVisibility, self: Boolean) {
    var help by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("关注吧 · " + visibility.label, style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary)
        if (self) TextButton(onClick = { help = true }) { Text("管理隐私") }
    }
    if (help) AlertDialog(
        onDismissRequest = { help = false },
        title = { Text("关注吧的可见范围") },
        text = { Text("当前范围：" + visibility.label + "。\n\n此项跟随贴吧账号的隐私设置。请在官方贴吧的「设置 → 隐私设置」中调整关注吧的可见范围，返回后刷新个人主页。") },
        confirmButton = { TextButton(onClick = {
            help = false
            try {
                val intent = context.packageManager.getLaunchIntentForPackage("com.baidu.tieba")
                if (intent != null) context.startActivity(intent)
                else Toast.makeText(context, "未找到官方贴吧，请在官方客户端修改隐私设置", Toast.LENGTH_LONG).show()
            } catch (_: Exception) { Toast.makeText(context, "无法打开官方贴吧", Toast.LENGTH_SHORT).show() }
        }) { Text("打开官方贴吧") } },
        dismissButton = { TextButton(onClick = { help = false }) { Text("关闭") } }
    )
}
