package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlin.math.roundToInt

/**
 * 下拉刷新指示器：位置完全由 [GlassPullRefreshState.offsetPx] 决定，
 * 不做第二套 offset 动画，松手后与状态共用同一条回弹曲线。
 */
@Composable
fun GlassRefreshIndicator(
    state: GlassPullRefreshState,
    modifier: Modifier = Modifier,
) {
    val progress = state.progress
    val refreshing = state.refreshing
    if (!refreshing && progress <= 0.01f) return

    val arcColor = ExtendedTheme.colors.primary
    val chipColor = ExtendedTheme.colors.chip.copy(alpha = 0.85f)
    val borderColor = ExtendedTheme.colors.divider.copy(alpha = 0.45f)
    val diameterPx = with(LocalDensity.current) { 34.dp.toPx() }

    val spin = rememberInfiniteTransition(label = "glassRefreshSpin")
    val spinAngle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing)
        ),
        label = "glassRefreshSpinAngle"
    )

    val diameter = 34.dp
    val scale = if (refreshing) 1f else 0.6f + 0.4f * progress
    val arcSweep = if (refreshing) 280f else 300f * progress
    val arcRotation = if (refreshing) spinAngle else -90f

    Box(
        modifier = modifier
            .offset { IntOffset(0, (state.offsetPx - diameterPx).roundToInt()) }
            .size(diameter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(chipColor)
                .border(1.dp, borderColor, CircleShape)
        )
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = 2.2.dp.toPx()
            drawArc(
                color = arcColor,
                startAngle = arcRotation,
                sweepAngle = arcSweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(stroke, stroke),
                size = androidx.compose.ui.geometry.Size(
                    size.width - stroke * 2,
                    size.height - stroke * 2
                )
            )
        }
    }
}

/**
 * 底栏「刷新/回顶」圆圈：列表不在顶部时替换动态图标。
 * 点击时做一次轻微旋转反馈，闲置时不转。
 */
@Composable
fun RefreshCircleIcon(
    spinSignal: Int,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinSignal) {
        if (spinSignal > 0) {
            rotation.snapTo(0f)
            rotation.animateTo(360f, tween(durationMillis = 420))
            rotation.snapTo(0f)
        }
    }
    Box(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer { rotationZ = rotation.value }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "刷新",
            modifier = Modifier.size(14.dp),
            tint = Color.White.copy(alpha = 0.92f)
        )
    }
}
