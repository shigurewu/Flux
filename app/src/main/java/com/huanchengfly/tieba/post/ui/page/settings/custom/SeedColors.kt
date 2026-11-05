package com.huanchengfly.tieba.post.ui.page.settings.custom

import androidx.annotation.StringRes
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.toJson
import com.huanchengfly.tieba.post.utils.parseColorHexOrNull

/** 预设种子色 */
internal data class SeedPreset(
    val hex: String,
    @StringRes val nameRes: Int,
)

internal val SeedPresets: List<SeedPreset> = listOf(
    SeedPreset("0xFF2196F3", R.string.seed_default),
    SeedPreset("0xFF116682", R.string.seed_ocean),
    SeedPreset("0xFF8E4955", R.string.seed_sakura),
    SeedPreset("0xFF4C662B", R.string.seed_spring),
    SeedPreset("0xFF735C0C", R.string.seed_autumn),
    SeedPreset("0xFF9C27B0", R.string.seed_purple),
    SeedPreset("0xFF4CAF50", R.string.seed_green),
    SeedPreset("0xFFFF9800", R.string.seed_orange),
    SeedPreset("0xFFE91E63", R.string.seed_pink),
    SeedPreset("0xFF009688", R.string.seed_teal),
    SeedPreset("0xFFF44336", R.string.seed_red),
    SeedPreset("0xFF3F51B5", R.string.seed_indigo),
    SeedPreset("0xFFFFC107", R.string.seed_amber),
    SeedPreset("0xFF00BCD4", R.string.seed_cyan),
)

/** 自定义种子色列表存储 key（string，JSON 数组，如 ["#FF2196F3",...]） */
const val KEY_CUSTOM_SEED_COLORS = "custom_primary_colors"

/** 自定义色列表上限 */
const val MAX_CUSTOM_SEED_COLORS = 20

/** 解析自定义种子色列表，失败/为空回退空列表 */
internal fun parseCustomSeedColors(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        raw.fromJson<List<String>>()
            .filter { parseColorHexOrNull(it) != null }
            .distinct()
    }.getOrDefault(emptyList())
}

/** 序列化自定义种子色列表（去重、上限截断，保留最新） */
internal fun serializeCustomSeedColors(colors: List<String>): String =
    colors.distinct().takeLast(MAX_CUSTOM_SEED_COLORS).toJson()
