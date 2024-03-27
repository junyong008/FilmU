package com.yjy.presentation.feature.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yjy.domain.model.GithubRepo
import com.yjy.domain.model.onFailure
import com.yjy.domain.model.onSuccess
import com.yjy.domain.usecase.AddNumberUseCase
import com.yjy.domain.usecase.GetGithubReposUseCase
import com.yjy.domain.usecase.GetNumberUseCase
import com.yjy.domain.usecase.GetRandomJokeUseCase
import com.yjy.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val getNumberUseCase: GetNumberUseCase,
    private val addNumberUseCase: AddNumberUseCase,
    private val getRandomJokeUseCase: GetRandomJokeUseCase,
    private val getGithubReposUseCase: GetGithubReposUseCase
) : ViewModel() {

    val number: StateFlow<Int> = getNumberUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = 0
    )

    fun addNumber() = viewModelScope.launch { addNumberUseCase(1) }

    private val _joke = MutableStateFlow<UiState>(UiState.Ready)
    val joke: StateFlow<UiState> = _joke.asStateFlow()
    fun getRandomJoke() {
        viewModelScope.launch {
            _joke.value = UiState.Loading
            getRandomJokeUseCase()
                .onSuccess {
                    _joke.value = UiState.Success(it)
                }
                .onFailure {
                    _joke.value = UiState.Error(it.safeThrowable().message ?: "")
                    val errorMessage = ExampleMessage.GetJokeFailed(it.safeThrowable().message)
                    postMessage(errorMessage)
                }
        }
    }

    val searchOwnerName = MutableStateFlow("")

    private val updateGithubReposTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun updateGithubRepos() {
        viewModelScope.launch { updateGithubReposTrigger.emit(Unit) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val githubRepos: Flow<PagingData<GithubRepo>> = updateGithubReposTrigger
        .flatMapLatest { getGithubReposUseCase(searchOwnerName.value) }
        .cachedIn(viewModelScope)

    private val _message = MutableSharedFlow<ExampleMessage>(replay = 0)
    val message: SharedFlow<ExampleMessage> = _message

    private suspend fun postMessage(msg: ExampleMessage) {
        _message.emit(msg)
    }

    sealed class ExampleMessage {
        data class GetJokeFailed(val message: String?) : ExampleMessage()
    }
}