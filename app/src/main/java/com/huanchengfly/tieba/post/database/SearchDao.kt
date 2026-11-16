package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.models.database.SearchPostHistory

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM searchhistory WHERE owner = :owner ORDER BY timestamp DESC")
    suspend fun getAll(owner: String): List<SearchHistory>

    @Query("DELETE FROM searchhistory WHERE owner = :owner")
    suspend fun deleteAll(owner: String)

    @Query("DELETE FROM searchhistory WHERE owner = :owner AND id = :id")
    suspend fun deleteById(owner: String, id: Long)

    @Query("SELECT * FROM searchhistory WHERE owner = :owner AND content = :content LIMIT 1")
    suspend fun getByContent(owner: String, content: String): SearchHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistory): Long

    @Transaction
    suspend fun upsert(history: SearchHistory) {
        val existing = getByContent(history.owner, history.content)
        if (existing != null) {
            insert(history.copy(id = existing.id, timestamp = System.currentTimeMillis()))
        } else {
            insert(history.copy(timestamp = System.currentTimeMillis()))
        }
    }
}

@Dao
interface SearchPostHistoryDao {
    @Query("SELECT * FROM searchposthistory WHERE owner = :owner ORDER BY timestamp DESC")
    suspend fun getAll(owner: String): List<SearchPostHistory>

    @Query("DELETE FROM searchposthistory WHERE owner = :owner")
    suspend fun deleteAll(owner: String)

    @Query("DELETE FROM searchposthistory WHERE owner = :owner AND id = :id")
    suspend fun deleteById(owner: String, id: Long)

    @Query("SELECT * FROM searchposthistory WHERE owner = :owner AND forumname = :forumName AND content = :content LIMIT 1")
    suspend fun getByContent(owner: String, content: String, forumName: String): SearchPostHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchPostHistory): Long

    @Transaction
    suspend fun upsert(history: SearchPostHistory) {
        val existing = getByContent(history.owner, history.content, history.forumName)
        if (existing != null) {
            insert(history.copy(id = existing.id, timestamp = System.currentTimeMillis()))
        } else {
            insert(history.copy(timestamp = System.currentTimeMillis()))
        }
    }
}
