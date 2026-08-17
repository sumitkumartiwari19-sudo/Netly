package com.netly.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.netly.app.domain.model.VideoInfo
import com.netly.app.domain.repository.ExtractorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<VideoInfo>) : SearchUiState
    object Empty : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val extractorRepository: ExtractorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _recentSearches = MutableStateFlow(
        listOf("lofi music", "free fire highlights", "coding tutorial", "podcast episode")
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            searchJob?.cancel()
            _uiState.value = SearchUiState.Idle
        }
    }

    fun search(queryToSearch: String? = null) {
        val q = (queryToSearch ?: _query.value).trim()
        if (q.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        _query.value = q
        addRecentSearch(q)

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            val result = extractorRepository.searchVideos(q)
            result.fold(
                onSuccess = { items ->
                    if (items.isEmpty()) {
                        _uiState.value = SearchUiState.Empty
                    } else {
                        _uiState.value = SearchUiState.Success(items)
                    }
                },
                onFailure = { error ->
                    val userMessage = when (error) {
                        is com.netly.app.domain.exception.YouTubeExtractionException.BotRestricted ->
                            "YouTube temporarily restricted this request. Please try again later."
                        is com.netly.app.domain.exception.YouTubeExtractionException.NetworkError ->
                            "No internet connection. Please check your network connection and try again."
                        else -> {
                            val msg = error.message ?: ""
                            if (msg.contains("bot", true) || msg.contains("LOGIN_REQUIRED", true)) {
                                "YouTube temporarily restricted this request. Please try again later."
                            } else {
                                "Failed to load search results. Please try again."
                            }
                        }
                    }
                    _uiState.value = SearchUiState.Error(userMessage)
                }
            )
        }
    }

    private fun addRecentSearch(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        _recentSearches.value = current.take(8)
    }

    fun clearQuery() {
        _query.value = ""
        searchJob?.cancel()
        _uiState.value = SearchUiState.Idle
    }

    fun retry() {
        if (_query.value.isNotBlank()) {
            search(_query.value)
        }
    }

    companion object {
        fun factory(extractorRepository: ExtractorRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(extractorRepository) as T
                }
            }
    }
}
