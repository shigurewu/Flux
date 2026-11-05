package com.huanchengfly.tieba.post.ui.widgets.compose

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.utils.appPreferences

/**
 * 字号调节对话框。
 *
 * 只在对话框内预览，点“确定”后写入偏好并重启 App 生效——
 * 和原 AppFontSizeActivity 的行为一致，避免运行时改 LocalDensity 带来的兼容问题。
 */
@Composable
fun FontScaleDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val initial = runCatching {
        context.appPreferences.fontScale.coerceIn(0.8f, 1.4f)
    }.getOrDefault(1f)
    var scale by remember { mutableStateOf(initial) }

    fun applyAndRestart() {
        runCatching {
            context.appPreferences.fontScale = scale
            Toast.makeText(
                context,
                R.string.toast_after_change_will_restart,
                Toast.LENGTH_SHORT
            ).show()
            onDismiss()
            App.INSTANCE.removeAllActivity()
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                context.startActivity(it)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.title_custom_font_size)) },
        text = {
            Column {
                Text(text = "${(scale * 100).toInt()}%")
                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    valueRange = 0.8f..1.4f,
                    steps = 5
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { applyAndRestart() }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}
