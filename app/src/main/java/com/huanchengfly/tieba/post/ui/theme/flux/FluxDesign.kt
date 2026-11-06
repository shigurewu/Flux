package com.huanchengfly.tieba.post.ui.theme.flux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

/**
 * Flux 设计令牌。
 *
 * 这里的数值是整个应用的唯一来源：间距、圆角、动效时长与字级。
 * 业务组件不要写字面量，统一引用这里，避免出现第四个圆角或第三套间距。
 */
object FluxSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
}

object FluxRadius {
    /** 列表内的密集元素 */
    val sm = 8.dp
    /** 卡片、面板 */
    val md = 14.dp
    /** 大面板、弹层 */
    val lg = 20.dp
    /** 胶囊 */
    val pill = 100.dp
}

object FluxMotion {
    /** 按压、选中反馈 */
    const val quick = 120
    /** 常规进出场 */
    const val base = 200
    /** 页面/大块内容 */
    const val slow = 320
}

object FluxType {
    /** 页面主标题 */
    const val titleSize = 20
    /** 卡片标题 */
    const val cardTitleSize = 16
    /** 正文 */
    const val bodySize = 15
    /** 次级信息（时间、楼层、数量） */
    const val metaSize = 12
    /** 分区标签 */
    const val sectionSize = 12
}

/**
 * 分区标题：左侧 2dp 强调竖线 + 小字级标题。
 * 用于设置页、个人页等需要明确分段的页面。
 */
@Composable
fun FluxSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    val accent = ExtendedTheme.colors.primary
    val textSecondary = ExtendedTheme.colors.textSecondary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = FluxSpacing.lg, end = FluxSpacing.lg, top = FluxSpacing.xl, bottom = FluxSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FluxSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(12.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            text = title,
            fontSize = FluxType.sectionSize.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            color = textSecondary,
        )
    }
}

/** 发丝分隔线：默认左右缩进，避免和内容边缘对齐显得毛躁。 */
@Composable
fun FluxHairline(
    modifier: Modifier = Modifier,
    inset: Int = 16,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = inset.dp)
            .height(0.5.dp)
            .background(ExtendedTheme.colors.divider.copy(alpha = 0.5f))
    )
}

/**
 * 轻标签：胶囊 + 极浅底色，用于「仅自己可见」「置顶」这类状态。
 * 刻意不做描边，避免和卡片描边叠加。
 */
@Composable
fun FluxTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = ExtendedTheme.colors.primary,
        modifier = modifier
            .clip(RoundedCornerShape(FluxRadius.pill))
            .background(ExtendedTheme.colors.primary.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** 列表行的最小内边距，保证长列表节奏一致。 */
@Composable
fun fluxRowPadding() = Modifier.padding(
    horizontal = FluxSpacing.lg,
    vertical = FluxSpacing.md,
)

/** 卡片统一形状。 */
fun fluxCardShape() = RoundedCornerShape(FluxRadius.md)

/** 强调色（跟随主题）。 */
@Composable
fun fluxAccent() = ExtendedTheme.colors.primary
