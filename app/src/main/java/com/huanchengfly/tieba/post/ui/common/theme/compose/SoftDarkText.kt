package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Shared colors for Compose and legacy Views. The 1.3.13 palette is unchanged. */
object SoftDarkText {
    const val PRIMARY_ARGB: Int = 0xFFA8A8A8.toInt()
    const val SECONDARY_ARGB: Int = 0xFF8A8A8A.toInt()
    val primary = Color(PRIMARY_ARGB)
    val secondary = Color(SECONDARY_ARGB)
}

internal fun ExtendedColors.withSoftDarkText(enabled: Boolean, dark: Boolean): ExtendedColors {
    if (!enabled || !dark) return this
    // Colored buttons can have light backgrounds even in dark mode. Preserve their contrast.
    fun onDarkSurface(surface: Color, original: Color, replacement: Color): Color =
        if (surface != Color.Unspecified && surface.luminance() < 0.18f) replacement else original
    return copy(
        text = SoftDarkText.primary,
        textSecondary = SoftDarkText.secondary,
        textDisabled = SoftDarkText.primary.copy(alpha = 0.38f),
        onTopBar = onDarkSurface(topBar, onTopBar, SoftDarkText.primary),
        onTopBarSecondary = onDarkSurface(topBar, onTopBarSecondary, SoftDarkText.secondary),
        onTopBarActive = if (onTopBarActive != Color.Unspecified &&
            maxOf(onTopBarActive.red, onTopBarActive.green, onTopBarActive.blue) -
            minOf(onTopBarActive.red, onTopBarActive.green, onTopBarActive.blue) < 0.12f
        ) onDarkSurface(topBar, onTopBarActive, SoftDarkText.primary) else onTopBarActive,
        onTopBarSurface = onDarkSurface(topBarSurface, onTopBarSurface, SoftDarkText.primary),
        onBottomBarSurface = onDarkSurface(bottomBarSurface, onBottomBarSurface, SoftDarkText.primary),
        onChip = onDarkSurface(chip, onChip, SoftDarkText.secondary),
        onPrimaryContainer = onDarkSurface(primaryContainer, onPrimaryContainer, SoftDarkText.primary),
        onSecondaryContainer = onDarkSurface(secondaryContainer, onSecondaryContainer, SoftDarkText.primary),
        unselected = SoftDarkText.secondary,
    )
}
