package com.tarzo.ai.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarzo.ai.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val isSearching: Boolean = false,
    val weatherInfo: WeatherInfo? = null,
    val newsArticles: List<NewsArticle> = emptyList(),
    val lastMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchManager: SearchManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchResult = MutableStateFlow<Result<String>>(Result.Success("Idle"))
    val searchResult: StateFlow<Result<String>> = _searchResult.asStateFlow()

    /**
     * Performs a web search for the given query.
     */
    fun webSearch(query: String) {
        val result = searchManager.webSearch(query)
        _searchResult.value = result
        when (result) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    lastMessage = result.data,
                    error = null
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
            is Result.Loading -> {}
        }
    }

    /**
     * Fetches weather information.
     */
    fun getWeather() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            when (val result = searchManager.getWeather()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        weatherInfo = result.data,
                        lastMessage = result.data.toReadableString()
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Fetches a news briefing.
     */
    fun getNewsBriefing() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            when (val result = searchManager.getNewsBriefing()) {
                is Result.Success -> {
                    val articles = result.data
                    val summary = if (articles.isEmpty()) {
                        "Opening news in browser."
                    } else {
                        articles.take(5).joinToString("\n\n") { it.toReadableString() }
                    }
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        newsArticles = articles,
                        lastMessage = summary
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Gets the current date and time string.
     */
    fun getDateTime(): String {
        val dt = searchManager.getDateTime()
        _uiState.value = _uiState.value.copy(lastMessage = dt)
        return dt
    }

    /**
     * Clears the current error and message.
     */
    fun clearState() {
        _uiState.value = _uiState.value.copy(error = null, lastMessage = null)
    }
}
