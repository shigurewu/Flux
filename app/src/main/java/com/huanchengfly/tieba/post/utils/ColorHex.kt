package com.huanchengfly.tieba.post.utils

import android.graphics.Color
import androidx.annotation.ColorInt

/** 默认种子色（Material 蓝），解析失败时的统一兜底 */
val DEFAULT_SEED_COLOR: Int = 0xFF2196F3.toInt()

/**
 * 解析十六进制颜色，兼容 "#AARRGGBB" / "#RRGGBB" / "0xAARRGGBB" / "0xRRGGBB" 四种存储格式。
 * 历史数据两种前缀并存（色卡写 "0x"、自定义弹窗写 "#"），Color.parseColor 只认 "#"，
 * 统一经此函数解析，失败返回 null 不抛异常。
 */
fun parseColorHexOrNull(hex: String?): Int? {
    if (hex.isNullOrBlank()) return null
    val normalized = if (hex.startsWith("0x", ignoreCase = true)) {
        "#" + hex.substring(2)
    } else {
        hex
    }
    return runCatching { Color.parseColor(normalized) }.getOrNull()
}

/** 统一输出 "#AARRGGBB" 格式（设置项写入用） */
fun formatColorHex(@ColorInt argb: Int): String = String.format("#%08X", argb)
