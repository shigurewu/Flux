package com.huanchengfly.tieba.post.ui.theme.flux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

/**
 * Flux 材质令牌。
 *
 * 两条规则：
 * 1. **同 Window 的悬浮层**（悬浮底栏、页面内浮层）才允许采样背景做模糊；
 *    透明度、模糊强度、描边统一取这里的值，不允许页面各自调参。
 * 2. **独立 Window 的弹层**（AlertDialog / DropdownMenu / Dialog）无法采样背景，
 *    一律使用不透明或近不透明的实体表面 + 细描边 + 统一圆角，不要伪造折射。
 */
object FluxMaterial {
    /** 玻璃表面底色透明度（采样到背景时） */
    const val glassSurfaceAlpha = 0.42f

    /** 无法模糊时的退化透明度：更实一些，保证可读性 */
    const val glassFallbackAlpha = 0.96f

    /** 玻璃边缘描边透明度 */
    const val glassBorderAlpha = 0.18f

    /** 实体弹层表面透明度（独立 Window） */
    const val solidSurfaceAlpha = 0.99f

    /** 弹层圆角 */
    val dialogCorner = FluxRadius.lg

    /** 弹层描边透明度 */
    const val dialogBorderAlpha = 0.12f
}

/** 玻璃层的边缘描边：与卡片描边同一视觉语言。 */
@Composable
fun Modifier.fluxGlassBorder(
    shape: RoundedCornerShape = RoundedCornerShape(FluxRadius.md),
): Modifier {
    val borderColor = ExtendedTheme.colors.onBottomBarSurface.copy(alpha = FluxMaterial.glassBorderAlpha)
    return this.border(0.5.dp, borderColor, shape)
}

/**
 * 独立 Window 弹层的实体表面：不透明底 + 细描边。
 * 供后续把 AlertDialog 的容器改成统一观感时复用。
 */
@Composable
fun Modifier.fluxSolidSurface(
    shape: RoundedCornerShape = RoundedCornerShape(FluxMaterial.dialogCorner),
): Modifier {
    val surface = ExtendedTheme.colors.card.copy(alpha = FluxMaterial.solidSurfaceAlpha)
    val borderColor = ExtendedTheme.colors.divider.copy(alpha = FluxMaterial.dialogBorderAlpha)
    return this
        .background(surface, shape)
        .border(0.5.dp, borderColor, shape)
}

/** 当前环境下的玻璃底色：能模糊时更透，不能模糊时退化。 */
@Composable
fun fluxGlassSurfaceColor(blurSupported: Boolean): Color {
    val base = ExtendedTheme.colors.bottomBarSurface
    return base.copy(alpha = if (blurSupported) FluxMaterial.glassSurfaceAlpha else FluxMaterial.glassFallbackAlpha)
}
