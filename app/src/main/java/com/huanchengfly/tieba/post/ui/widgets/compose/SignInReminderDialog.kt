package com.huanchengfly.tieba.post.ui.widgets.compose

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.SignInReminderStore
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.signInReminderDay
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.Calendar

@Composable
internal fun SignInReminderDialog(lifecycle: Lifecycle) {
    val context = LocalContext.current
    val uid = AccountUtil.LocalAccount.current?.uid?.takeIf { it.isNotBlank() }
    val reminders = remember(context) { SignInReminderStore(context.dataStore) }
    val dialogState = rememberDialogState()
    var prompt by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(lifecycle, uid) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            SplashState.finished.first { it }
            val day = signInReminderDay(Calendar.getInstance())
            val activePrompt = if (uid != null && day != null) "$uid:$day" else null
            if (prompt != activePrompt) {
                prompt = null
                dialogState.show = false
            }
            if (uid == null || day == null || AccountUtil.currentAccount?.uid != uid) return@repeatOnLifecycle
            val claimed = try {
                reminders.claim(uid, day)
            } catch (_: IOException) {
                false
            }
            if (claimed && AccountUtil.currentAccount?.uid == uid) {
                prompt = activePrompt
                dialogState.show()
            }
        }
    }

    if (uid == null || prompt?.substringBefore(':') != uid) return
    var signRequested by remember(prompt) { mutableStateOf(false) }
    AlertDialog(
        dialogState = dialogState,
        onDismiss = { prompt = null },
        title = { Text(stringResource(R.string.title_sign_in_reminder)) },
        content = { Text(stringResource(R.string.message_sign_in_reminder)) },
        buttons = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    DialogPositiveButton(text = stringResource(R.string.button_sign_in)) {
                        if (!signRequested && AccountUtil.currentAccount?.uid == uid) {
                            signRequested = true
                            runCatching { TiebaUtil.startSign(context, expectedAccountUid = uid) }
                                .onFailure {
                                    Toast.makeText(context, R.string.title_oksign_fail, Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    DialogNegativeButton(text = stringResource(R.string.button_cancel))
                }
            }
        },
    )
}
