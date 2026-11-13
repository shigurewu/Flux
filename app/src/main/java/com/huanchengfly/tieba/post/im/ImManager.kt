package com.huanchengfly.tieba.post.im

import android.util.Log
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.GlobalEventFlow
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.ImMessage
import com.huanchengfly.tieba.post.models.database.ImSession
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.DatabaseUtil
import com.huanchengfly.tieba.post.utils.UIDUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

/** IM 整体可用状态（连接 + 鉴权） */
sealed interface ImStatus {
    data object Idle : ImStatus
    data object Connecting : ImStatus
    data object Ready : ImStatus
    data class Error(val throwable: Throwable) : ImStatus
}

/**
 * 私信（IM 长连接）业务编排：连接生命周期、鉴权、消息收发、已读、推送驱动的增量拉取与本地持久化。
 *
 * 一期范围：前台收发文本私信；App 启动即建连，无独立进程后台保活（二期）。
 * 协议要点（详见 docs 设计/私信功能调研）：
 * - 1v1 私信共用一个 priv 消息组（groupType=6），会话按对端 uid 划分视图；
 * - recordId = priv 组 lastMsgId * 100 + 1；
 * - 推送（CMD 202006）只带 groupId/msgId，需再按游标增量拉取（CMD 202003）。
 */
object ImManager {
    private const val TAG = "ImManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connection = ImConnection(scope)
    private val networkStatus by lazy { NetworkStatus(App.INSTANCE) }

    private val _imStatus = MutableStateFlow<ImStatus>(ImStatus.Idle)
    val imStatus: StateFlow<ImStatus> = _imStatus.asStateFlow()

    /** 1v1 私信所属的 priv 消息组 id（鉴权响应中 groupType=6 的组） */
    @Volatile
    private var privGid: Long = 0L

    /** 消息组本地拉取游标：groupId → 已拉取的最大 msgId */
    private val gid2LastMsgId = ConcurrentHashMap<Long, Long>()
    private val privateGroups = java.util.Collections.newSetFromMap(ConcurrentHashMap<Long, Boolean>())
    private val initialGroupLatest = ConcurrentHashMap<Long, Long>()
    private val localMessageIds = ImLocalMessageIds()

    /** 本账号数字 uid */
    @Volatile
    private var myUid: Long = 0L

    /** 本账号 uid 字符串（Room 隔离键） */
    @Volatile
    private var accountUid: String = ""

    private val generation = AtomicLong(0L)
    private val pullMutex = Mutex()
    private var monitorJob: Job? = null

    private var accountEventJob: Job? = null

    private var networkEventJob: Job? = null

    private var accountsLoadedJob: Job? = null

    /** App 启动钩子：有登录态则启动（幂等），并跟随切号/退出重启或停止连接 */
    fun onAppStart() {
        if (!PrivateMessaging.enabled) return
        AccountUtil.getLoginInfo()?.let { start(it) }
        // 登录态冷启动的账号改为后台加载：加载完成后再启动一次（start 对同账号幂等）
        if (accountsLoadedJob?.isActive != true) {
            accountsLoadedJob = scope.launch {
                AccountUtil.accountsLoaded.collect { loaded ->
                    if (loaded) AccountUtil.getLoginInfo()?.let { start(it) }
                }
            }
        }
        if (networkEventJob?.isActive != true) {
            networkEventJob = scope.launch {
                var wasOnline = networkStatus.online.value
                networkStatus.online.collect { online ->
                    if (online && !wasOnline && accountUid.isNotEmpty()) {
                        // 网络恢复主动打断旧会话，让保活循环立即重连，而不是等退避结束
                        Log.i(TAG, "网络恢复，立即重连 IM")
                        connection.close()
                    }
                    wasOnline = online
                }
            }
        }
        if (accountEventJob?.isActive != true) {
            accountEventJob = scope.launch {
                GlobalEventFlow.filterIsInstance<GlobalEvent.AccountSwitched>().collect {
                    val account = AccountUtil.getLoginInfo()
                    if (account != null) start(account) else stop()
                }
            }
        }
    }

    /** 登录/切号后调用；同账号重复调用幂等 */
    fun start(account: Account) {
        if (!PrivateMessaging.enabled) { stop(); return }
        if (accountUid == account.uid && monitorJob?.isActive == true) return
        stop()
        if (account.sToken.isEmpty()) {
            Log.w(TAG, "账号缺少 STOKEN，私信长连接不可用")
            _imStatus.value = ImStatus.Error(IllegalStateException("账号缺少 STOKEN，请重新登录后重试"))
            return
        }
        accountUid = account.uid
        myUid = account.uid.toLongOrNull() ?: 0L
        monitorJob = scope.launch { monitorLoop(account) }
    }

    /** 主动停止：同时断开旧连接，唤醒可能正在等待网络恢复的保活循环 */
    fun stop() {
        generation.incrementAndGet()
        monitorJob?.cancel()
        monitorJob = null
        accountUid = ""
        myUid = 0L
        connection.close()
        privGid = 0L
        gid2LastMsgId.clear()
        privateGroups.clear()
        initialGroupLatest.clear()
        localMessageIds.clear()
        _imStatus.value = ImStatus.Idle
    }

    /**
     * 连接保活循环：connect → 鉴权 → 收推送，断开后指数退避重连。
     */
    private suspend fun monitorLoop(account: Account) {
        var backoff = ImConstants.RECONNECT_BASE_DELAY_MS
        while (currentCoroutineContext().isActive) {
            var phase = "连接私信服务器"
            try {
                if (!networkStatus.online.value) {
                    _imStatus.value = ImStatus.Idle
                    // 离线时不重试握手，等待 NetworkCallback 恢复信号
                    networkStatus.online.first { it }
                }
                _imStatus.value = ImStatus.Connecting
                connection.connect()
                phase = "私信账号鉴权"
                auth(account)
                _imStatus.value = ImStatus.Ready
                backoff = ImConstants.RECONNECT_BASE_DELAY_MS
                phase = "接收私信"
                // 推送与「会话结束」并行等待：SharedFlow 不会自行结束，断线必须靠 sessionEnd 感知
                collectWhileSessionOpen(connection.pushFlow, { connection.awaitSessionEnd() }, ::onPush)
                Log.i(TAG, "IM 连接已断开，准备重连")
                _imStatus.value = ImStatus.Error(IllegalStateException("IM 连接已断开"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "IM 连接异常：${e.message}")
                _imStatus.value = ImStatus.Error(java.io.IOException("$phase 失败：${e.message ?: "请稍后重试"}", e))
            }
            connection.close()
            delay(backoff)
            backoff = minOf(backoff * 2, ImConstants.RECONNECT_MAX_DELAY_MS)
        }
    }

    /** CMD 1001 上线鉴权：上报密钥换取消息组列表，初始化游标与会话 */
    private suspend fun auth(account: Account) {
        val secKey = connection.newSessionKey()
        val body = connection.request(
            ImConstants.CMD_UPDATE_CLIENT_INFO,
            ImApi.buildUpdateClientInfo(
                bduss = account.bduss,
                stoken = account.sToken,
                // IM 的 cuid 用官方形态 baidutiebaapp{uuid}；session/device 里再带 cuid_galaxy2
                cuid = UIDUtil.newCUID,
                cuidGalaxy2 = CuidUtils.getNewCuid(),
                secKey = secKey,
            ),
            encrypt = false,
        )
        val groups = ImApi.parseUpdateClientInfo(body, account.uid.toLongOrNull() ?: 0L)
        privGid = groups.firstOrNull { it.groupType == ImConstants.GROUP_TYPE_PRIVATE_MSG }?.groupId ?: 0L
        check(privGid > 0L) { "服务器未返回私信消息组，暂不能收发私信" }
        groups.filter { it.groupType == ImConstants.GROUP_TYPE_PRIVATE_MSG && it.groupId > 0 }.forEach {
            privateGroups.add(it.groupId)
            // Start from zero once per account session: using the server's latest cursor
            // discarded messages received before startup, including unknown senders.
            gid2LastMsgId.putIfAbsent(it.groupId, 0L)
            initialGroupLatest.putIfAbsent(it.groupId, it.lastMsgId)
        }
        pullMessages()
        Log.i(TAG, "IM 鉴权成功：privGid=$privGid, groups=${groups.size}")
    }

    /** 服务端推送（CMD 202006）：按 groupType 过滤私信推送并触发增量拉取 */
    private suspend fun onPush(frame: ImFrame.Frame) {
        val notifies = try {
            ImApi.parsePushNotify(frame.payload)
        } catch (e: Exception) {
            Log.w(TAG, "推送解析失败", e)
            return
        }
        val gids = notifies
            .filter { it.third == ImConstants.GROUP_TYPE_PRIVATE_MSG }
            .map { it.first }
            .distinct()
        if (gids.isEmpty()) return
        privateGroups.addAll(gids)
        try { pullMessages(gids) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { Log.w(TAG, "推送拉取失败", e) }
    }

    /**
     * 按游标增量拉取并持久化（推送驱动 / 聊天页主动刷新共用）。
     */
    suspend fun pullMessages(groupIds: List<Long> = privateGroups.toList()) = pullMutex.withLock {
        if (!PrivateMessaging.enabled) return@withLock
        val owner = accountUid
        val epoch = generation.get()
        val ownId = myUid
        val privateGroup = privGid
        if (owner.isEmpty() || groupIds.isEmpty()) return@withLock
        // Drain bounded batches so a busy group does not leave other peers permanently unseen.
        repeat(10) {
            val pairs = groupIds.distinct().map { it to (gid2LastMsgId[it] ?: 0L) }
            val body = connection.request(ImConstants.CMD_GET_GROUP_MSG, ImApi.buildGetGroupMsg(UIDUtil.newCUID, pairs))
            if (epoch != generation.get()) return@withLock
            var progressed = false
            for (result in ImApi.parseGetGroupMsg(body)) {
                val gid = result.groupId.takeIf { it > 0L } ?: privateGroup
                var maxMsgId = gid2LastMsgId[gid] ?: 0L
                for (msg in result.messages) {
                    if (epoch != generation.get()) return@withLock
                    maxMsgId = maxOf(maxMsgId, msg.msgId)
                    val isMine = msg.fromUid == ownId
                    if (msg.msgType == ImConstants.MSG_TYPE_READED && !isMine) {
                        DatabaseUtil.advanceImPeerRead(owner, msg.fromUid, msg.msgId)
                    } else if (msg.msgType == ImConstants.MSG_TYPE_TEXT) {
                        val peer = if (isMine) msg.toUid else msg.fromUid
                        if (peer <= 0L || msg.msgId <= 0L) continue
                        DatabaseUtil.storeImMessage(ImMessage(
                            uid = owner, peerUid = peer, groupId = gid, msgId = msg.msgId,
                            msgType = msg.msgType, content = msg.content, createTime = msg.createTime,
                            fromUid = msg.fromUid, isMine = isMine
                        ), msg.fromName, msg.fromPortrait,
                            countAsUnread = msg.msgId > (initialGroupLatest[gid] ?: 0L))
                    }
                }
                if (epoch == generation.get()) {
                    if (maxMsgId > (gid2LastMsgId[gid] ?: 0L)) progressed = true
                    gid2LastMsgId[gid] = maxMsgId
                }
            }
            if (!progressed) return@withLock
        }
    }

    /**
     * 发送文本私信。返回服务端分配的 msgId；被屏蔽时抛 [ImException]。
     */
    suspend fun sendMessage(toUid: Long, content: String): Long {
        check(PrivateMessaging.enabled) { "私信功能暂未开放" }
        check(_imStatus.value == ImStatus.Ready && privGid > 0L) { "私信服务未连接" }
        val owner = accountUid
        val ownId = myUid
        val group = privGid
        val epoch = generation.get()
        check(owner.isNotBlank() && owner == AccountUtil.currentAccount?.uid) { "账号正在切换，请稍后重试" }
        val recordId = localMessageIds.next(toUid, maxOf(gid2LastMsgId[group] ?: 0L, initialGroupLatest[group] ?: 0L))
        val body = connection.request(ImConstants.CMD_COMMIT_PERSONAL_MSG,
            ImApi.buildCommitPersonalMsg(toUid, content, recordId))
        val msgId = ImApi.parseCommitPersonalMsg(body)
        check(epoch == generation.get()) { "账号已切换，请在原账号确认发送结果" }
        if (msgId > 0L) DatabaseUtil.storeImMessage(ImMessage(
            uid = owner, peerUid = toUid, groupId = group, msgId = msgId,
            msgType = ImConstants.MSG_TYPE_TEXT, content = content,
            createTime = System.currentTimeMillis() / 1000, fromUid = ownId, isMine = true
        ))
        return msgId
    }

    /**
     * 标记某会话已读（对端为 peerUid，读到 msgId），并清零本地未读。
     */
    suspend fun markRead(peerUid: Long, msgId: Long, owner: String = AccountUtil.currentAccount?.uid.orEmpty()) {
        if (!PrivateMessaging.enabled) return
        if (owner.isEmpty() || msgId <= 0L) return
        val group = privGid
        val epoch = generation.get()
        DatabaseUtil.readImThrough(owner, peerUid, msgId)
        if (owner != accountUid || _imStatus.value != ImStatus.Ready || group <= 0L || epoch != generation.get()) return
        try {
            val body = connection.request(ImConstants.CMD_COMMIT_RECEIVED_PMSG,
                ImApi.buildCommitReceivedPmsg(group, peerUid, msgId))
            ImApi.parseCommitReceivedPmsg(body)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Log.w(TAG, "已本地标记已读，回执发送失败", e) }
    }
}
