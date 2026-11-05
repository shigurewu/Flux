package com.huanchengfly.tieba.post.ui.page.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.rememberPreferenceAsMutableState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Switch

/**
 * 设置页统一组件（卡片五件套）：
 * 分组标题 [SectionLabel] + 分组卡片 [AppearanceCard] + 卡内分隔线 [CardDivider] +
 * 行式条目 [SettingRow] / [SwitchSettingRow]。
 * 全部设置页（主页 / 外观 / 使用习惯 / 屏蔽 / 签到 / 账号 / 关于）统一使用。
 */

@Composable
internal fun SectionLabel(text: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ExtendedTheme.colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ExtendedTheme.colors.primary
        )
    }
}

/** Bettbox 风格分组卡片：圆角 20，组内行间细分隔线 */
@Composable
internal fun AppearanceCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color = ExtendedTheme.colors.card),
        content = content
    )
}

@Composable
internal fun CardDivider() {
    Divider(
        thickness = 1.dp,
        color = ExtendedTheme.colors.divider.copy(alpha = if (ExtendedTheme.colors.isNightMode) 0.45f else 0.6f),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
    )
}

@Composable
internal fun SettingRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled && onClick != null,
                onClick = { onClick?.invoke() }
            )
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.subtitle1)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.body2,
                    color = ExtendedTheme.colors.textSecondary
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
internal fun SwitchSettingRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    summaryOn: String? = null,
    summaryOff: String? = null,
    key: String,
    defaultValue: Boolean,
    enabled: Boolean = true,
    summaryProvider: ((Boolean) -> String)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    var checked by rememberPreferenceAsMutableState(
        key = booleanPreferencesKey(key),
        defaultValue = defaultValue
    )
    SettingRow(
        icon = icon,
        title = title,
        summary = summaryProvider?.invoke(checked)
            ?: if (checked) summaryOn ?: summary else summaryOff ?: summary,
        enabled = enabled,
        onClick = {
            checked = !checked
            onCheckedChange?.invoke(checked)
        },
        trailing = {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    )
}
