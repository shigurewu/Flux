package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "searchhistory")
data class SearchHistory(
    @ColumnInfo(defaultValue = "'legacy'") val owner: String = "legacy",
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)
