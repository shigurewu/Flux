package com.huanchengfly.tieba.post.ui.page.settings.meme

import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.models.database.MemeEmoticon
import com.huanchengfly.tieba.post.repository.MemeRepository
import com.huanchengfly.tieba.post.utils.AccountUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class MemeEmoticonViewModel @Inject constructor() :
    BaseViewModel<MemeEmoticonUiIntent, MemeEmoticonPartialChange, MemeEmoticonUiState, MemeEmoticonUiEvent>() {
    override fun createInitialState() = MemeEmoticonUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<MemeEmoticonUiIntent, MemeEmoticonPartialChange, MemeEmoticonUiState> =
        MemeEmoticonPartialChangeProducer

    override fun dispatchEvent(partialChange: MemeEmoticonPartialChange): UiEvent? = when (partialChange) {
        is MemeEmoticonPartialChange.Load.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
        is MemeEmoticonPartialChange.Sync.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
        is MemeEmoticonPartialChange.Remove.Failure -> CommonUiEvent.Toast(partialChange.error.getErrorMessage())
        else -> null
    }

    private object MemeEmoticonPartialChangeProducer :
        PartialChangeProducer<MemeEmoticonUiIntent, MemeEmoticonPartialChange, MemeEmoticonUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<MemeEmoticonUiIntent>): Flow<MemeEmoticonPartialChange> =
            merge(
                intentFlow.filterIsInstance<MemeEmoticonUiIntent.Load>()
                    .flatMapConcat { produceLoadPartialChange() },
                intentFlow.filterIsInstance<MemeEmoticonUiIntent.Sync>()
                    .flatMapConcat { produceSyncPartialChange() },
                intentFlow.filterIsInstance<MemeEmoticonUiIntent.Remove>()
                    .flatMapConcat { it.producePartialChange() },
            )

        private fun produceLoadPartialChange(): Flow<MemeEmoticonPartialChange> {
            val uid = AccountUtil.getUid()
                ?: return flowOf(MemeEmoticonPartialChange.Load.Success(emptyList()))
            return merge(
                // 持续观察本地数据（同步 / 增删后自动更新 UI）
                MemeRepository.memeFlow(uid)
                    .map<_, MemeEmoticonPartialChange> { MemeEmoticonPartialChange.Load.Success(it) }
                    .onStart { emit(MemeEmoticonPartialChange.Load.Start) }
                    .catch { emit(MemeEmoticonPartialChange.Load.Failure(it)) },
                // 触发一次云同步
                flow<MemeEmoticonPartialChange> {
                    MemeRepository.sync(uid).getOrThrow()
                    emit(MemeEmoticonPartialChange.Sync.Success)
                }
                    .onStart { emit(MemeEmoticonPartialChange.Sync.Start) }
                    .catch { emit(MemeEmoticonPartialChange.Sync.Failure(it)) },
            )
        }

        private fun produceSyncPartialChange(): Flow<MemeEmoticonPartialChange.Sync> {
            val uid = AccountUtil.getUid()
                ?: return flowOf(MemeEmoticonPartialChange.Sync.Failure(IllegalStateException("未登录")))
            return flow<MemeEmoticonPartialChange.Sync> {
                MemeRepository.sync(uid).getOrThrow()
                emit(MemeEmoticonPartialChange.Sync.Success)
            }.onStart { emit(MemeEmoticonPartialChange.Sync.Start) }
                .catch { emit(MemeEmoticonPartialChange.Sync.Failure(it)) }
        }

        private fun MemeEmoticonUiIntent.Remove.producePartialChange(): Flow<MemeEmoticonPartialChange.Remove> {
            val uid = AccountUtil.getUid()
                ?: return flowOf(MemeEmoticonPartialChange.Remove.Failure(IllegalStateException("未登录")))
            return flow<MemeEmoticonPartialChange.Remove> {
                MemeRepository.removeMeme(uid, picId, type).getOrThrow()
                emit(MemeEmoticonPartialChange.Remove.Success(picId, type))
            }.catch { emit(MemeEmoticonPartialChange.Remove.Failure(it)) }
        }
    }
}

sealed interface MemeEmoticonUiIntent : UiIntent {
    /** 进入页面：观察本地数据并触发一次云同步 */
    object Load : MemeEmoticonUiIntent

    /** 手动刷新（从云端拉取） */
    object Sync : MemeEmoticonUiIntent

    /** 删除表情（picId + type 定位） */
    data class Remove(val picId: String, val type: Int) : MemeEmoticonUiIntent
}

sealed interface MemeEmoticonPartialChange : PartialChange<MemeEmoticonUiState> {
    sealed class Load : MemeEmoticonPartialChange {
        override fun reduce(oldState: MemeEmoticonUiState): MemeEmoticonUiState = when (this) {
            is Start -> oldState.copy(isLoading = true)
            is Success -> oldState.copy(isLoading = false, memes = list.toImmutableList())
            is Failure -> oldState.copy(isLoading = false)
        }

        object Start : Load()

        data class Success(val list: List<MemeEmoticon>) : Load()

        data class Failure(val error: Throwable) : Load()
    }

    sealed class Sync : MemeEmoticonPartialChange {
        override fun reduce(oldState: MemeEmoticonUiState): MemeEmoticonUiState = when (this) {
            is Start -> oldState.copy(isSyncing = true)
            is Success -> oldState.copy(isSyncing = false)
            is Failure -> oldState.copy(isSyncing = false)
        }

        object Start : Sync()

        object Success : Sync()

        data class Failure(val error: Throwable) : Sync()
    }

    sealed class Remove : MemeEmoticonPartialChange {
        override fun reduce(oldState: MemeEmoticonUiState): MemeEmoticonUiState = oldState

        data class Success(val picId: String, val type: Int) : Remove()

        data class Failure(val error: Throwable) : Remove()
    }
}

data class MemeEmoticonUiState(
    val isLoggedIn: Boolean = AccountUtil.isLoggedIn(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val memes: ImmutableList<MemeEmoticon> = persistentListOf(),
) : UiState

sealed interface MemeEmoticonUiEvent : UiEvent
