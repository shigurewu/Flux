package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.huanchengfly.tieba.post.models.database.MemeEmoticon
import kotlinx.coroutines.flow.Flow

@Dao
interface MemeEmoticonDao {
    @Query("SELECT * FROM meme_emoticon WHERE uid = :uid ORDER BY type ASC, orderId ASC, addedTime DESC, id DESC")
    fun getFlowByUid(uid: String): Flow<List<MemeEmoticon>>

    @Query("SELECT * FROM meme_emoticon WHERE uid = :uid ORDER BY type ASC, orderId ASC, addedTime DESC, id DESC")
    suspend fun getByUid(uid: String): List<MemeEmoticon>

    @Query("SELECT * FROM meme_emoticon WHERE uid = :uid AND type = :type ORDER BY orderId ASC, addedTime DESC, id DESC")
    suspend fun getByUidAndType(uid: String, type: Int): List<MemeEmoticon>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MemeEmoticon>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MemeEmoticon)

    @Query("DELETE FROM meme_emoticon WHERE uid = :uid AND picId = :picId AND type = :type")
    suspend fun delete(uid: String, picId: String, type: Int)

    @Query("DELETE FROM meme_emoticon WHERE uid = :uid")
    suspend fun deleteAllByUid(uid: String)

    @Transaction
    suspend fun replaceAll(uid: String, items: List<MemeEmoticon>) {
        deleteAllByUid(uid)
        insertAll(items)
    }
}
