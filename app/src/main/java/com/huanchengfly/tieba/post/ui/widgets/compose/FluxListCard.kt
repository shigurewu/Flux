package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Composable
internal fun Modifier.fluxListCard(): Modifier {
    val shape = RoundedCornerShape(14.dp)
    return fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .background(ExtendedTheme.colors.card, shape)
        .border(0.5.dp, ExtendedTheme.colors.divider.copy(alpha = 0.45f), shape)
}
