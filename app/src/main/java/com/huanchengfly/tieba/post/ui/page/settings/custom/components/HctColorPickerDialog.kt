package com.huanchengfly.tieba.post.ui.page.settings.custom.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.utils.formatColorHex
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.hct.Hct

/**
 * HCT 三段式取色器（参考 FlClash 的 Palette）：
 * 色相滑块（0-360 渐变轨道）+ 彩度滑块（0 ~ 该色相下最大彩度）+ 明度色带（11 格点选），
 * 三色实时预览该种子在当前配色风格下生成的 primary/secondary/tertiary。
 */
@Composable
internal fun HctColorPickerDialog(
    dialogState: DialogState,
    initialColor: Color,
    isNight: Boolean,
    schemeVariant: String,
    onConfirm: (Color) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val initialHct = Hct.fromInt(initialColor.toArgb())
    var hue by remember { mutableFloatStateOf(initialHct.hue.toFloat()) }
    var chroma by remember { mutableFloatStateOf(initialHct.chroma.toFloat()) }
    var tone by remember { mutableFloatStateOf(initialHct.tone.toFloat()) }
    // 每次打开重置为当前主题色
    LaunchedEffect(dialogState.show) {
        if (dialogState.show) {
            val hct = Hct.fromInt(initialColor.toArgb())
            hue = hct.hue.toFloat()
            chroma = hct.chroma.toFloat()
            tone = hct.tone.toFloat()
        }
    }
    // 该色相下可达到的最大彩度（tone 60 处最鲜艳）
    val chromaMax = remember(hue) {
        Hct.from(hue.toDouble(), 200.0, 60.0).chroma.toFloat().coerceAtLeast(1f)
    }
    LaunchedEffect(chromaMax) {
        if (chroma > chromaMax) chroma = chromaMax
    }

    val previewSeed = remember(hue, chroma, tone) {
        Color(Hct.from(hue.toDouble(), chroma.toDouble(), tone.toDouble()).toInt())
    }
    val previewScheme = remember(previewSeed, isNight, schemeVariant) {
        dynamicColorScheme(
            seedColor = previewSeed,
            isDark = isNight,
            style = runCatching { PaletteStyle.valueOf(schemeVariant) }
                .getOrDefault(PaletteStyle.TonalSpot),
        )
    }

    Dialog(
        dialogState = dialogState,
        title = { Text(text = stringResource(id = R.string.title_custom_seed_color)) },
        buttons = {
            DialogPositiveButton(
                text = stringResource(id = R.string.button_finish),
                onClick = { onConfirm(previewSeed) }
            )
            DialogNegativeButton(text = stringResource(id = R.string.button_cancel))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 三色实时预览：主色大块 + 副色/三色小块
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewScheme.primary)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewScheme.secondary)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewScheme.tertiary)
                    )
                }
            }

            GradientSlider(
                label = stringResource(id = R.string.label_color_hue),
                value = hue,
                onValueChange = { hue = it },
                valueRange = 0f..360f,
                trackColors = List(25) { Color(Hct.from(it * 15.0, 100.0, 60.0).toInt()) }
            )
            GradientSlider(
                label = stringResource(id = R.string.label_color_chroma),
                value = chroma,
                onValueChange = { chroma = it },
                valueRange = 0f..chromaMax,
                trackColors = List(11) {
                    Color(Hct.from(hue.toDouble(), chromaMax * it / 10.0, 60.0).toInt())
                }
            )

            ToneStrip(
                hue = hue,
                chroma = chroma,
                tone = tone,
                onToneChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tone = it
                }
            )

            Text(
                text = formatColorHex(previewSeed.toArgb()),
                style = MaterialTheme.typography.caption,
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/** 带标签的渐变轨道滑块：分段纯色块拼接轨道（规避部分软件渲染下渐变 shader 崩溃）+ 透明轨道 Slider 叠加 */
@Composable
private fun GradientSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    trackColors: List<Color>,
) {
    Column {
        Text(
            text = label,
            fontSize = 13.sp,
            color = ExtendedTheme.colors.textSecondary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
            ) {
                trackColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(color)
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = ExtendedTheme.colors.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 明度色带：11 格（0,10,...,100）点选，当前格 primary 描边 */
@Composable
private fun ToneStrip(
    hue: Float,
    chroma: Float,
    tone: Float,
    onToneChange: (Float) -> Unit,
) {
    Text(
        text = stringResource(id = R.string.label_color_tone),
        fontSize = 13.sp,
        color = ExtendedTheme.colors.textSecondary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        repeat(11) { i ->
            val t = i * 10f
            val selected = kotlin.math.abs(tone - t) < 5f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color(Hct.from(hue.toDouble(), chroma.toDouble(), t.toDouble()).toInt()))
                    .border(
                        width = if (selected) 2.dp else 0.dp,
                        color = ExtendedTheme.colors.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToneChange(t) }
            )
        }
    }
}
