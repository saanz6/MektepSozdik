//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//
package com.example.bilimsoz.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilimsoz.data.local.NetworkChecker
import com.example.bilimsoz.data.local.PreferencesManager
import com.example.bilimsoz.data.local.SimpleCacheInfo
import com.example.bilimsoz.data.model.AppLanguage
import com.example.bilimsoz.data.model.Subject
import com.example.bilimsoz.data.model.Term
import com.example.bilimsoz.data.repository.TermRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "MainViewModel"
        const val SEARCH_DEBOUNCE_TIME = 300L
        const val OPERATION_TIMEOUT = 30_000L // 30 секунд
    }

    private val repository = TermRepository(application)
    private val preferencesManager = PreferencesManager(application)
    private val networkChecker = NetworkChecker(application)

    // Exception handler для корутин
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught exception in ViewModel", throwable)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isSyncing = false,
            error = "Системная ошибка: ${throwable.message}"
        )
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var searchJob: Job? = null
    private var dataLoadingJob: Job? = null
    private var syncJob: Job? = null

    init {
        Log.d(TAG, "MainViewModel initialized")
        initializeViewModel()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "MainViewModel cleared")
        // Отменяем все активные корутины
        cancelAllJobs()
    }

    private fun initializeViewModel() {
        viewModelScope.launch(exceptionHandler) {
            try {
                observeLanguageAndFavorites()
                loadAllTerms()
                observeSearchResults()
                checkNetworkStatus()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ViewModel", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка инициализации: ${e.message}"
                )
            }
        }
    }

    private fun observeLanguageAndFavorites() {
        viewModelScope.launch(exceptionHandler) {
            try {
                combine(
                    preferencesManager.selectedLanguage,
                    preferencesManager.favoriteTermIds,
                    repository.getAllTermsFlow()
                ) { language, favoriteIds, allTerms ->
                    Triple(language, favoriteIds, allTerms)
                }
                    .catch { exception ->
                        Log.e(TAG, "Error in language/favorites flow", exception)
                        emit(Triple(AppLanguage.RUSSIAN, emptySet(), emptyList()))
                    }
                    .collect { (language, favoriteIds, allTerms) ->
                        try {
                            val favoriteTerms = allTerms.filter { it.id in favoriteIds }

                            // Получаем слово дня (не зависит от языка!)
                            val wordOfDay = getWordOfDay(allTerms)

                            _uiState.value = _uiState.value.copy(
                                selectedLanguage = language,
                                favoriteTermIds = favoriteIds,
                                favoriteTerms = favoriteTerms,
                                randomTerm = wordOfDay, // Слово дня остается тем же!
                                isLoading = false
                            )
                            Log.d(TAG, "Updated UI state: ${allTerms.size} terms, ${favoriteTerms.size} favorites, word of day: ${wordOfDay?.kazakh}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating UI state", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up language/favorites observer", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки настроек: ${e.message}"
                )
            }
        }
    }

    private suspend fun getWordOfDay(allTerms: List<Term>): Term? {
        return try {
            if (allTerms.isEmpty()) {
                Log.d(TAG, "No terms available for word of day")
                return null
            }

            // Проверяем, нужно ли обновить слово дня
            val shouldUpdate = preferencesManager.shouldUpdateWordOfDay()
            val (savedWordId, _) = preferencesManager.getWordOfDay()

            if (!shouldUpdate && savedWordId != null) {
                // Ищем сохраненное слово дня
                val savedTerm = allTerms.find { it.id == savedWordId }
                if (savedTerm != null) {
                    Log.d(TAG, "Using saved word of day: ${savedTerm.kazakh}")
                    return savedTerm
                } else {
                    Log.w(TAG, "Saved word of day not found in current terms, selecting new one")
                }
            }

            // Выбираем новое слово дня
            Log.d(TAG, "Selecting new word of day")
            val newWordOfDay = selectRandomTermDeterministic(allTerms)
            preferencesManager.setWordOfDay(newWordOfDay.id)
            Log.d(TAG, "New word of day selected: ${newWordOfDay.kazakh}")

            newWordOfDay
        } catch (e: Exception) {
            Log.e(TAG, "Error getting word of day", e)
            allTerms.randomOrNull()
        }
    }

    private fun selectRandomTermDeterministic(terms: List<Term>): Term {
        // Используем детерминистический алгоритм на основе текущей даты
        val calendar = java.util.Calendar.getInstance()
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val year = calendar.get(java.util.Calendar.YEAR)

        // Создаем seed на основе года и дня в году
        val seed = (year * 1000L + dayOfYear).toLong()
        val random = java.util.Random(seed)

        val index = random.nextInt(terms.size)
        return terms[index]
    }

    private fun observeSearchResults() {
        viewModelScope.launch(exceptionHandler) {
            _searchQuery
                .catch { exception ->
                    Log.e(TAG, "Error in search flow", exception)
                }
                .collect { query ->
                    try {
                        if (query.isBlank()) {
                            _uiState.value = _uiState.value.copy(searchResults = emptyList())
                        } else {
                            searchJob?.cancel()
                            searchJob = viewModelScope.launch(exceptionHandler) {
                                try {
                                    delay(SEARCH_DEBOUNCE_TIME)

                                    val results = withTimeoutOrNull(OPERATION_TIMEOUT) {
                                        repository.searchTerms(query)
                                    } ?: emptyList()

                                    _uiState.value = _uiState.value.copy(searchResults = results)
                                    Log.d(TAG, "Search completed for '$query': ${results.size} results")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error during search", e)
                                    _uiState.value = _uiState.value.copy(
                                        searchResults = emptyList(),
                                        error = "Ошибка поиска: ${e.message}"
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing search query", e)
                    }
                }
        }
    }

    private fun checkNetworkStatus() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val isOnline = networkChecker.isNetworkAvailable()
                _uiState.value = _uiState.value.copy(isOnline = isOnline)
                Log.d(TAG, "Network status: $isOnline")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking network status", e)
                _uiState.value = _uiState.value.copy(isOnline = false)
            }
        }
    }

    private fun loadAllTerms() {
        dataLoadingJob?.cancel()
        dataLoadingJob = viewModelScope.launch(exceptionHandler) {
            try {
                Log.d(TAG, "Starting to load all terms")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val terms = withTimeoutOrNull(OPERATION_TIMEOUT) {
                    repository.getAllTerms()
                } ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
                updateCacheInfo()
                Log.d(TAG, "Successfully loaded ${terms.size} terms")
            } catch (exception: Exception) {
                Log.e(TAG, "Error loading all terms", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки данных: ${exception.message}"
                )
            }
        }
    }

    fun loadTermsForSubject(subject: Subject, forceRefresh: Boolean = false) {
        viewModelScope.launch(exceptionHandler) {
            try {
                Log.d(TAG, "Loading terms for subject: ${subject.name}")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val terms = withTimeoutOrNull(OPERATION_TIMEOUT) {
                    repository.getTermsForSubject(subject, forceRefresh)
                } ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    subjectTerms = terms,
                    isLoading = false,
                    error = null
                )
                Log.d(TAG, "Loaded ${terms.size} terms for ${subject.name}")
            } catch (exception: Exception) {
                Log.e(TAG, "Error loading terms for subject", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки предмета: ${exception.message}"
                )
            }
        }
    }

    fun syncData() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch(exceptionHandler) {
            try {
                Log.d(TAG, "Starting data sync")
                checkNetworkStatus()

                if (!_uiState.value.isOnline) {
                    _uiState.value = _uiState.value.copy(
                        error = "Нет подключения к интернету. Синхронизация невозможна."
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(isSyncing = true, error = null)

                val success = withTimeoutOrNull(OPERATION_TIMEOUT * 2) { // Больше времени для синхронизации
                    repository.syncAllSubjects()
                } ?: false

                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis(),
                    error = if (!success) "Не удалось синхронизировать все данные" else null
                )
                updateCacheInfo()
                Log.d(TAG, "Sync completed, success: $success")
            } catch (exception: Exception) {
                Log.e(TAG, "Error during sync", exception)
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = "Ошибка синхронизации: ${exception.message}"
                )
            }
        }
    }

    fun refreshSubject(subject: Subject) {
        viewModelScope.launch(exceptionHandler) {
            try {
                checkNetworkStatus()
                if (_uiState.value.isOnline) {
                    loadTermsForSubject(subject, forceRefresh = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Нет подключения к интернету. Обновление невозможно."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing subject", e)
            }
        }
    }

    private fun updateCacheInfo() {
        viewModelScope.launch(exceptionHandler) {
            try {
                val cacheInfo = withTimeoutOrNull(OPERATION_TIMEOUT) {
                    repository.getCacheInfo()
                }
                if (cacheInfo != null) {
                    _uiState.value = _uiState.value.copy(cacheInfo = cacheInfo)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error updating cache info", e)
                // Не показываем ошибку пользователю для cache info
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(exceptionHandler) {
            try {
                Log.d(TAG, "Clearing cache")
                repository.clearCache()
                _uiState.value = _uiState.value.copy(
                    cacheInfo = null,
                    error = null
                )
                loadAllTerms()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка очистки кэша: ${e.message}"
                )
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch(exceptionHandler) {
            try {
                preferencesManager.setLanguage(language)
                Log.d(TAG, "Language changed to: $language")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting language", e)
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка смены языка: ${e.message}"
                )
            }
        }
    }

    fun toggleFavorite(term: Term) {
        viewModelScope.launch(exceptionHandler) {
            try {
                val currentFavorites = _uiState.value.favoriteTermIds
                if (term.id in currentFavorites) {
                    preferencesManager.removeFromFavorites(term.id)
                    Log.d(TAG, "Removed from favorites: ${term.id}")
                } else {
                    preferencesManager.addToFavorites(term.id)
                    Log.d(TAG, "Added to favorites: ${term.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite", e)
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка обновления избранного: ${e.message}"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        try {
            _searchQuery.value = query
        } catch (e: Exception) {
            Log.e(TAG, "Error updating search query", e)
        }
    }

    fun clearSearch() {
        try {
            _searchQuery.value = ""
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing search", e)
        }
    }

    suspend fun getTermById(termId: String): Term? {
        return try {
            Log.d(TAG, "Getting term by id: $termId")

            // Сначала ищем в текущем состоянии ViewModel
            val currentTerm = _uiState.value.let { state ->
                state.favoriteTerms.find { it.id == termId }
                    ?: state.subjectTerms.find { it.id == termId }
                    ?: state.searchResults.find { it.id == termId }
            }

            if (currentTerm != null) {
                Log.d(TAG, "Found term in current state: ${currentTerm.kazakh}")
                return currentTerm
            }

            // Если не найден, ищем в репозитории (кэше)
            Log.d(TAG, "Term not found in current state, checking repository")
            val repoTerm = withTimeoutOrNull(OPERATION_TIMEOUT) {
                repository.getTermById(termId)
            }

            if (repoTerm != null) {
                Log.d(TAG, "Found term in repository: ${repoTerm.kazakh}")
                return repoTerm
            }

            // Если всё ещё не найден, пробуем загрузить все термины заново
            Log.w(TAG, "Term not found anywhere, reloading all terms")
            withTimeoutOrNull(OPERATION_TIMEOUT) {
                repository.getAllTerms(forceRefresh = false)
                repository.getTermById(termId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting term by id", e)
            null
        }
    }

    fun dismissError() {
        try {
            _uiState.value = _uiState.value.copy(error = null)
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing error", e)
        }
    }

    fun retryLastAction() {
        viewModelScope.launch(exceptionHandler) {
            try {
                checkNetworkStatus()
                if (_uiState.value.isOnline) {
                    loadAllTerms()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Подключитесь к интернету для обновления данных"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error retrying last action", e)
            }
        }
    }

    private fun cancelAllJobs() {
        searchJob?.cancel()
        dataLoadingJob?.cancel()
        syncJob?.cancel()
    }
}

data class MainUiState(
    val selectedLanguage: AppLanguage = AppLanguage.RUSSIAN,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val isOnline: Boolean = true,
    val searchResults: List<Term> = emptyList(),
    val subjectTerms: List<Term> = emptyList(),
    val favoriteTerms: List<Term> = emptyList(),
    val favoriteTermIds: Set<String> = emptySet(),
    val randomTerm: Term? = null,
    val error: String? = null,
    val cacheInfo: SimpleCacheInfo? = null,
    val lastSyncTime: Long = 0L
)