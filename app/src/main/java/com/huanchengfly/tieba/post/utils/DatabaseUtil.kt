package com.huanchengfly.tieba.post.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.room.withTransaction
import com.huanchengfly.tieba.post.im.sessionAfterNewMessage
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.database.AppDatabase
import com.huanchengfly.tieba.post.database.AppDatabaseEntryPoint
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Draft
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.ImMessage
import com.huanchengfly.tieba.post.models.database.ImSession
import com.huanchengfly.tieba.post.models.database.MemeEmoticon
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import com.huanchengfly.tieba.post.models.database.TopForum
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow

internal fun localDataOwner(uid: String?): String =
    if (uid.isNullOrBlank()) "guest" else "uid:$uid"

internal fun replyDraftKey(uid: String?, forumId: Long, threadId: Long, postId: Long?, subPostId: Long?): String {
    val owner = localDataOwner(uid)
    val targetForum = if (threadId == 0L) forumId else 0L
    val targetPost = postId ?: 0L
    val targetSubPost = subPostId ?: 0L
    return "reply:$owner:$targetForum:$threadId:$targetPost:$targetSubPost"
}

object DatabaseUtil {
    private val appDatabase: AppDatabase by lazy {
        EntryPointAccessors.fromApplication(
            App.INSTANCE,
            AppDatabaseEntryPoint::class.java,
        ).appDatabase()
    }

    // ── Account ─────────────────────────────────────────────────
    suspend fun getAllAccounts(): List<Account> = appDatabase.accountDao().getAll()

    suspend fun getAccountById(id: Int): Account? = appDatabase.accountDao().getById(id)

    suspend fun getAccountByUid(uid: String): Account? = appDatabase.accountDao().getByUid(uid)

    suspend fun getAccountByBduss(bduss: String): Account? = appDatabase.accountDao().getByBduss(bduss)

    suspend fun upsertAccountByUid(account: Account) = appDatabase.accountDao().upsertByUid(account)

    suspend fun updateAccount(account: Account) = appDatabase.accountDao().update(account)

    suspend fun deleteAccount(account: Account) = appDatabase.accountDao().delete(account)

    // ── History ─────────────────────────────────────────────────
    suspend fun getAllHistory(owner: String): List<History> = appDatabase.historyDao().getAll(owner)

    suspend fun getHistoryByType(owner: String, type: Int, pageSize: Int = 100, offset: Int = 0): List<History> =
        appDatabase.historyDao().getByType(owner, type, pageSize, offset)

    fun getHistoryFlowByType(owner: String, type: Int, pageSize: Int = 100, offset: Int = 0): Flow<List<History>> =
        appDatabase.historyDao().getFlowByType(owner, type, pageSize, offset)

    suspend fun upsertHistory(history: History) = appDatabase.historyDao().upsert(history)

    suspend fun deleteHistoryById(owner: String, id: Long) = appDatabase.historyDao().deleteById(owner, id)

    suspend fun deleteAllHistory(owner: String) = appDatabase.historyDao().deleteAll(owner)

    // ── Block ───────────────────────────────────────────────────
    suspend fun getAllBlocks(): List<Block> = appDatabase.blockDao().getAll()

    fun getAllBlocksFlow(): Flow<List<Block>> = appDatabase.blockDao().getAllFlow()

    suspend fun insertBlock(block: Block): Long = appDatabase.blockDao().insert(block)

    suspend fun deleteBlockById(id: Long) = appDatabase.blockDao().deleteById(id)

    // ── Draft ───────────────────────────────────────────────────
    private val draftScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val draftLock = Mutex()

    suspend fun getDraft(hash: String): Draft? = draftLock.withLock {
        appDatabase.draftDao().getByHash(hash)
    }

    // Finish pending edits even if their editor is closed immediately.
    fun updateDraft(hash: String, content: String) {
        draftScope.launch {
            draftLock.withLock {
                try {
                    if (content.isEmpty()) appDatabase.draftDao().deleteByHash(hash)
                    else appDatabase.draftDao().upsert(Draft(hash = hash, content = content))
                } catch (error: android.database.SQLException) {
                    Log.e("ReplyDraft", "Could not save reply draft", error)
                }
            }
        }
    }

    // ── TopForum ────────────────────────────────────────────────
    suspend fun getTopForums(): List<TopForum> = appDatabase.topForumDao().getAll()

    suspend fun addTopForum(forumId: String) {
        appDatabase.topForumDao().insertOrReplace(TopForum(forumId = forumId))
    }

    suspend fun deleteTopForum(forumId: String) = appDatabase.topForumDao().deleteByForumId(forumId)

    // ── SearchHistory ───────────────────────────────────────────
    suspend fun getAllSearchHistories(owner: String): List<SearchHistory> = appDatabase.searchHistoryDao().getAll(owner)

    suspend fun saveSearchHistory(owner: String, content: String) {
        appDatabase.searchHistoryDao().upsert(SearchHistory(owner = owner, content = content))
    }

    suspend fun deleteSearchHistory(owner: String, id: Long) = appDatabase.searchHistoryDao().deleteById(owner, id)

    suspend fun clearSearchHistory(owner: String) = appDatabase.searchHistoryDao().deleteAll(owner)

    // ── SearchPostHistory ───────────────────────────────────────
    suspend fun getAllSearchPostHistories(owner: String): List<SearchPostHistory> = appDatabase.searchPostHistoryDao().getAll(owner)

    suspend fun saveSearchPostHistory(owner: String, content: String, forumName: String) {
        appDatabase.searchPostHistoryDao().upsert(
            SearchPostHistory(owner = owner, content = content, forumName = forumName)
        )
    }

    suspend fun deleteSearchPostHistory(owner: String, id: Long) = appDatabase.searchPostHistoryDao().deleteById(owner, id)

    suspend fun clearSearchPostHistory(owner: String) = appDatabase.searchPostHistoryDao().deleteAll(owner)

    // ── MemeEmoticon ────────────────────────────────────────────
    fun getMemeEmoticonFlow(uid: String): Flow<List<MemeEmoticon>> =
        appDatabase.memeEmoticonDao().getFlowByUid(uid)

    suspend fun getMemeEmoticons(uid: String): List<MemeEmoticon> =
        appDatabase.memeEmoticonDao().getByUid(uid)

    suspend fun getMemeEmoticons(uid: String, type: Int): List<MemeEmoticon> =
        appDatabase.memeEmoticonDao().getByUidAndType(uid, type)

    suspend fun upsertMemeEmoticon(item: MemeEmoticon) =
        appDatabase.memeEmoticonDao().insert(item)

    suspend fun upsertMemeEmoticons(items: List<MemeEmoticon>) =
        appDatabase.memeEmoticonDao().insertAll(items)

    suspend fun deleteMemeEmoticon(uid: String, picId: String, type: Int) =
        appDatabase.memeEmoticonDao().delete(uid, picId, type)

    suspend fun replaceMemeEmoticons(uid: String, items: List<MemeEmoticon>) =
        appDatabase.memeEmoticonDao().replaceAll(uid, items)

    // ── IM 私信 ─────────────────────────────────────────────────
    fun getImSessionFlow(uid: String): Flow<List<ImSession>> =
        appDatabase.imSessionDao().getFlowByUid(uid)

    fun getImUnreadCountFlow(uid: String): Flow<Int?> =
        appDatabase.imSessionDao().getUnreadCountFlow(uid)

    fun getImMessageFlow(uid: String, peerUid: Long): Flow<List<ImMessage>> =
        appDatabase.imMessageDao().getFlowByPeer(uid, peerUid)

    suspend fun getImSession(uid: String, peerUid: Long): ImSession? =
        appDatabase.imSessionDao().getByPeerUid(uid, peerUid)

    suspend fun upsertImSession(session: ImSession) =
        appDatabase.imSessionDao().upsert(session)

    suspend fun clearImUnread(uid: String, peerUid: Long) =
        appDatabase.imSessionDao().clearUnread(uid, peerUid)

    suspend fun insertImMessages(messages: List<ImMessage>): List<Long> =
        appDatabase.imMessageDao().insertAll(messages)

    /** Only seed a missing session; an existing live preview and unread state are untouched. */
    suspend fun importImHistory(uid: String, peerUid: Long, messages: List<ImMessage>, name: String, portrait: String): Int =
        appDatabase.withTransaction {
            require(messages.all { it.uid == uid && it.peerUid == peerUid && it.groupId == -1L && it.msgId < 0 })
            val inserted = appDatabase.imMessageDao().insertAll(messages).count { it != -1L }
            if (messages.isNotEmpty() && appDatabase.imSessionDao().getByPeerUid(uid, peerUid) == null) {
                val latest = messages.maxWith(compareBy<ImMessage> { it.createTime }.thenBy { it.msgId })
                appDatabase.imSessionDao().insert(ImSession(uid = uid, peerUid = peerUid,
                    peerName = name, peerPortrait = portrait, lastMsgTime = latest.createTime,
                    lastMsgPreview = latest.content, lastMsgFromMe = latest.isMine))
            }
            inserted
        }

    suspend fun getLatestImMessage(uid: String, peerUid: Long): ImMessage? =
        appDatabase.imMessageDao().getLatestByPeer(uid, peerUid)

    suspend fun getLatestIncomingImId(uid: String, groupId: Long): Long? =
        appDatabase.imMessageDao().getLatestIncomingId(uid, groupId)

    /** Message insertion and unread count are atomic; replayed messages have no effect. */
    suspend fun storeImMessage(message: ImMessage, peerName: String = "", peerPortrait: String = "", countAsUnread: Boolean = true) =
        appDatabase.withTransaction {
            val inserted = appDatabase.imMessageDao().insertAll(listOf(message)).first() != -1L
            if (inserted) {
                val dao = appDatabase.imSessionDao()
                val old = dao.getByPeerUid(message.uid, message.peerUid)
                val updated = sessionAfterNewMessage(old, message, peerName, peerPortrait, countAsUnread)
                if (old == null) dao.insert(updated) else dao.update(updated)
            }
        }

    suspend fun readImThrough(uid: String, peerUid: Long, msgId: Long) =
        appDatabase.imSessionDao().clearUnreadThrough(uid, peerUid, msgId)

    suspend fun advanceImPeerRead(uid: String, peerUid: Long, msgId: Long) =
        appDatabase.imSessionDao().advancePeerRead(uid, peerUid, msgId)

    suspend fun updateImPeer(uid: String, peerUid: Long, name: String, portrait: String) =
        appDatabase.imSessionDao().updatePeer(uid, peerUid, name, portrait)
}
