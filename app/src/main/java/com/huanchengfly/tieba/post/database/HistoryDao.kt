package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.huanchengfly.tieba.post.models.database.History
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE owner = :owner ORDER BY timestamp DESC, count DESC LIMIT 100")
    suspend fun getAll(owner: String): List<History>

    @Query("SELECT * FROM history WHERE owner = :owner AND type = :type ORDER BY timestamp DESC, count DESC LIMIT :pageSize OFFSET :offset")
    suspend fun getByType(owner: String, type: Int, pageSize: Int = 100, offset: Int = 0): List<History>

    @Query("SELECT * FROM history WHERE owner = :owner AND type = :type ORDER BY timestamp DESC, count DESC LIMIT :pageSize OFFSET :offset")
    fun getFlowByType(owner: String, type: Int, pageSize: Int = 100, offset: Int = 0): Flow<List<History>>

    @Query("SELECT * FROM history WHERE owner = :owner AND type = :type AND data = :data LIMIT 1")
    suspend fun getByData(owner: String, type: Int, data: String): History?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History): Long

    @Update
    suspend fun update(history: History)

    @Query("DELETE FROM history WHERE owner = :owner AND id = :id")
    suspend fun deleteById(owner: String, id: Long)

    @Query("DELETE FROM history WHERE owner = :owner")
    suspend fun deleteAll(owner: String)

    @Transaction
    suspend fun upsert(history: History) {
        val existing = getByData(history.owner, history.type, history.data)
        if (existing != null) {
            update(
                history.copy(
                    id = existing.id,
                    timestamp = System.currentTimeMillis(),
                    title = history.title,
                    extras = history.extras,
                    avatar = history.avatar,
                    username = history.username,
                    count = existing.count + 1,
                )
            )
        } else {
            insert(history.copy(count = 1, timestamp = System.currentTimeMillis()))
        }
    }
}
