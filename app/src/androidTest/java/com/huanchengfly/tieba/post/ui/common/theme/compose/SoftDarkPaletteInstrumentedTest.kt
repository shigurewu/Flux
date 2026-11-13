package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The Material palette calls Android Color.colorToHSV through the existing darken helper. */
@RunWith(AndroidJUnit4::class)
class SoftDarkPaletteInstrumentedTest {
    @Test fun materialSurfacesFollowSoftDarkText() {
        val extended = ExtendedColors("dark", true,
            accent = Color.Cyan, primary = Color.Cyan, text = Color.White,
            background = Color.Black, card = Color.Black)
            .withSoftDarkText(enabled = true, dark = true)
        val material = getColorPalette(true, extended)
        assertEquals(SoftDarkText.primary, material.onSurface)
        assertEquals(SoftDarkText.primary, material.onBackground)
    }
}
