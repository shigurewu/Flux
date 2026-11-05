package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "meme_emoticon",
    indices = [Index(value = ["uid", "picId", "pkgId", "type"], unique = true)]
)
data class MemeEmoticon(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val picId: String,
    val pkgId: String = "",
    val picUrl: String,
    val thumbnail: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val type: Int = TYPE_COLLECT,
    val orderId: Int = 0,
    val addedTime: Long = System.currentTimeMillis(),
) {
    companion object {
        const val TYPE_COLLECT = 0
        const val TYPE_DIY = 1
    }
}
