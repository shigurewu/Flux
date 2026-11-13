package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class SoftDarkTextTest {
    private fun palette() = ExtendedColors("dark", true,
        text = Color.White, textSecondary = Color.LightGray,
        topBar = Color.Black, onTopBar = Color.White, onTopBarActive = Color.White,
        topBarSurface = Color.Black, onTopBarSurface = Color.White,
        bottomBarSurface = Color.Black, onBottomBarSurface = Color.White,
        chip = Color.Black, onChip = Color.White,
        primary = Color.Cyan, onPrimary = Color.Black, accent = Color.Cyan,
        primaryContainer = Color.Black, onPrimaryContainer = Color.White,
        secondaryContainer = Color.Black, onSecondaryContainer = Color.White)

    @Test fun coversReadingBarsChipsAndChatContainers() {
        val result = palette().withSoftDarkText(true, true)
        assertEquals(SoftDarkText.primary, result.text)
        assertEquals(SoftDarkText.secondary, result.textSecondary)
        assertEquals(SoftDarkText.primary, result.onTopBar)
        assertEquals(SoftDarkText.primary, result.onTopBarActive)
        assertEquals(SoftDarkText.primary, result.onTopBarSurface)
        assertEquals(SoftDarkText.primary, result.onBottomBarSurface)
        assertEquals(SoftDarkText.secondary, result.onChip)
        assertEquals(SoftDarkText.primary, result.onPrimaryContainer)
        assertEquals(SoftDarkText.primary, result.onSecondaryContainer)
    }

    @Test fun offAndLightModeKeepOriginalColors() {
        val original = palette()
        assertSame(original, original.withSoftDarkText(false, true))
        assertSame(original, original.withSoftDarkText(true, false))
    }

    @Test fun coloredButtonAndLightContainerRemainReadable() {
        val result = palette().copy(primaryContainer = Color.White, onPrimaryContainer = Color.Black)
            .withSoftDarkText(true, true)
        assertEquals(Color.Black, result.onPrimaryContainer)
        assertEquals(Color.Black, result.onPrimary)
        assertEquals(Color.Cyan, result.accent)
    }
}
