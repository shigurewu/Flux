package com.huanchengfly.tieba.post.ui.page.settings.custom.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.settings.custom.SeedPreset
import com.huanchengfly.tieba.post.ui.page.settings.custom.SeedPresets
import com.huanchengfly.tieba.post.utils.parseColorHexOrNull
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * 主题色彩色点网格（参考 FlClash）：
 * [跟随系统] + 预设色 + 自定义色 + [添加]，圆点内三区预览该种子生成的 primary/secondary/tertiary。
 * 自定义色长按删除；当前色不在列表中时在自定义区后补一个只读「当前色」点。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorDotGrid(
    currentSeedHex: String?,
    dynamicSelected: Boolean,
    showFollowSystem: Boolean,
    isNight: Boolean,
    schemeVariant: String,
    customColors: List<String>,
    onSelectFollowSystem: () -> Unit,
    onSelectColor: (String) -> Unit,
    onAddColor: () -> Unit,
    onDeleteColor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    // 选中判定统一用 argb 比较（兼容 "0x..." 与 "#..." 两种存储格式）
    val currentArgb = remember(currentSeedHex) { parseColorHexOrNull(currentSeedHex) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (showFollowSystem) {
            ColorDot(
                selected = dynamicSelected,
                onClick = onSelectFollowSystem,
                content = {
                    Icon(
                        imageVector = Icons.Outlined.Colorize,
                        contentDescription = stringResource(id = R.string.title_seed_follow_system),
                        tint = ExtendedTheme.colors.text,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
        SeedPresets.forEach { preset ->
            SeedDot(
                preset = preset,
                isNight = isNight,
                schemeVariant = schemeVariant,
                selected = !dynamicSelected && currentArgb == parseColorHexOrNull(preset.hex),
                onClick = { onSelectColor(preset.hex) }
            )
        }
        // 当前色不在预设/自定义列表中（如历史遗留值）时补一个只读「当前色」点
        if (!dynamicSelected && currentArgb != null &&
            customColors.none { parseColorHexOrNull(it) == currentArgb } &&
            SeedPresets.none { parseColorHexOrNull(it.hex) == currentArgb }
        ) {
            SeedColorDot(
                seedHex = currentSeedHex ?: "",
                isNight = isNight,
                schemeVariant = schemeVariant,
                selected = true,
                onClick = {},
                onDelete = null
            )
        }
        customColors.forEach { hex ->
            SeedColorDot(
                seedHex = hex,
                isNight = isNight,
                schemeVariant = schemeVariant,
                selected = !dynamicSelected && currentArgb == parseColorHexOrNull(hex),
                onClick = { onSelectColor(hex) },
                onDelete = { onDeleteColor(hex) }
            )
        }
        ColorDot(
            selected = false,
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAddColor()
            },
            border = BorderStroke(1.dp, ExtendedTheme.colors.divider),
            content = {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(id = R.string.desc_add_custom_color),
                    tint = ExtendedTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

/** 预设色点（三区预览由种子实时生成） */
@Composable
private fun SeedDot(
    preset: SeedPreset,
    isNight: Boolean,
    schemeVariant: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SeedColorDot(
        seedHex = preset.hex,
        isNight = isNight,
        schemeVariant = schemeVariant,
        selected = selected,
        onClick = onClick,
        onDelete = null
    )
}

/** 通用色点：圆点 + 三区预览 + 选中环/对勾；onDelete 非空时长按删除 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeedColorDot(
    seedHex: String,
    isNight: Boolean,
    schemeVariant: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val argb = remember(seedHex) { parseColorHexOrNull(seedHex) ?: 0xFF2196F3.toInt() }
    val scheme = remember(argb, isNight, schemeVariant) {
        dynamicColorScheme(
            seedColor = Color(argb),
            isDark = isNight,
            style = runCatching { PaletteStyle.valueOf(schemeVariant) }
                .getOrDefault(PaletteStyle.TonalSpot),
        )
    }
    ColorDot(
        selected = selected,
        onClick = onClick,
        onDelete = onDelete,
        content = {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(scheme.primary)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(scheme.secondary)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(scheme.tertiary)
                    )
                }
            }
        }
    )
}

/**
 * 基础圆形色点：44dp，内容由 [content] 提供（三区预览或图标），
 * 选中态 primary 圆环 + 中央对勾；[onDelete] 非空时支持长按删除。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColorDot(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    border: BorderStroke? = null,
    onDelete: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = if (selected) 2.5.dp else border?.width ?: 1.dp,
                brush = when {
                    selected -> SolidColor(ExtendedTheme.colors.primary)
                    border != null -> border.brush
                    else -> SolidColor(ExtendedTheme.colors.divider)
                },
                shape = CircleShape
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onDelete?.let { handler ->
                    {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        handler()
                    }
                }
            )
            .padding(if (selected) 3.dp else 0.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content?.invoke()
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(id = R.string.desc_checked),
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(4.dp)
            )
        }
    }
}
