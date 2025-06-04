//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//
package com.example.bilimsoz.data.repository

import android.content.Context
import android.util.Log
import com.example.bilimsoz.BuildConfig
import com.example.bilimsoz.data.local.NetworkChecker
import com.example.bilimsoz.data.local.SimpleCacheManager
import com.example.bilimsoz.data.local.SimpleCacheInfo
import com.example.bilimsoz.data.model.GoogleSheetsResponse
import com.example.bilimsoz.data.model.Subject
import com.example.bilimsoz.data.model.Term
import com.example.bilimsoz.data.network.GoogleSheetsApi
import com.example.bilimsoz.di.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class TermRepository(context: Context) {
    private val api: GoogleSheetsApi = NetworkModule.googleSheetsApi
    private val cacheManager = SimpleCacheManager(context)
    private val networkChecker = NetworkChecker(context)

    private val spreadsheetId = "1I9KZKiU5A51_iEd1Mmr4Acdr8zo8XIbvlfpINuUpn9U"
    private val apiKey = BuildConfig.GOOGLE_SHEETS_API_KEY

    // StateFlow для отслеживания всех терминов
    private val _allTermsFlow = MutableStateFlow<List<Term>>(emptyList())
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Таймауты для предотвращения зависания
    private companion object {
        const val NETWORK_TIMEOUT = 10_000L // 10 секунд
        const val CACHE_TIMEOUT = 5_000L    // 5 секунд
        const val TAG = "TermRepository"
    }

    suspend fun getTermsForSubject(subject: Subject, forceRefresh: Boolean = false): List<Term> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting terms for subject: ${subject.name}, forceRefresh: $forceRefresh")

                val isOnline = safeNetworkCheck()

                // Проверяем кэш сначала с таймаутом
                val cachedTerms = if (forceRefresh) {
                    null
                } else {
                    withTimeoutOrNull(CACHE_TIMEOUT) {
                        cacheManager.getTermsForSubject(subject)
                    }
                }

                when {
                    cachedTerms != null && !forceRefresh -> {
                        Log.d(TAG, "Returning cached terms for ${subject.name}: ${cachedTerms.size} items")
                        cachedTerms
                    }
                    isOnline -> {
                        Log.d(TAG, "Loading terms from network for ${subject.name}")
                        loadFromNetwork(subject)
                    }
                    else -> {
                        Log.w(TAG, "No network, trying cached data for ${subject.name}")
                        cachedTerms ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting terms for subject ${subject.name}", e)
                // Fallback: попробуем получить из кэша
                try {
                    cacheManager.getTermsForSubject(subject) ?: emptyList()
                } catch (cacheException: Exception) {
                    Log.e(TAG, "Cache fallback failed", cacheException)
                    emptyList()
                }
            }
        }
    }

    private suspend fun loadFromNetwork(subject: Subject): List<Term> {
        return try {
            withTimeoutOrNull(NETWORK_TIMEOUT) {
                val range = "${subject.nameRu}!A2:D"
                val response = api.getSheetData(spreadsheetId, range, apiKey)
                val terms = parseTermsFromResponse(response, subject)

                Log.d(TAG, "Loaded ${terms.size} terms from network for ${subject.name}")

                // Сохраняем в кэш асинхронно
                repositoryScope.launch {
                    try {
                        cacheManager.saveTermsForSubject(subject, terms)
                        updateAllTermsFlow()
                        Log.d(TAG, "Cached terms for ${subject.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error caching terms for ${subject.name}", e)
                    }
                }

                terms
            } ?: run {
                Log.w(TAG, "Network timeout for ${subject.name}, falling back to cache")
                cacheManager.getTermsForSubject(subject) ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error for ${subject.name}", e)
            cacheManager.getTermsForSubject(subject) ?: emptyList()
        }
    }

    suspend fun getAllTerms(forceRefresh: Boolean = false): List<Term> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting all terms, forceRefresh: $forceRefresh")

                val allTerms = mutableListOf<Term>()
                var hasErrors = false

                Subject.entries.forEach { subject ->
                    try {
                        val terms = getTermsForSubject(subject, forceRefresh)
                        allTerms.addAll(terms)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading subject ${subject.name}", e)
                        hasErrors = true
                    }
                }

                // Обновляем Flow только если есть данные
                if (allTerms.isNotEmpty()) {
                    _allTermsFlow.value = allTerms
                    Log.d(TAG, "Updated flow with ${allTerms.size} total terms")
                }

                if (hasErrors && allTerms.isEmpty()) {
                    Log.w(TAG, "No terms loaded, trying cached data")
                    // Fallback: загружаем все из кэша
                    val cachedTerms = cacheManager.getAllCachedTerms()
                    _allTermsFlow.value = cachedTerms
                    cachedTerms
                } else {
                    allTerms
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting all terms", e)
                try {
                    cacheManager.getAllCachedTerms()
                } catch (cacheException: Exception) {
                    Log.e(TAG, "Cache fallback failed", cacheException)
                    emptyList()
                }
            }
        }
    }

    suspend fun searchTerms(query: String): List<Term> {
        return try {
            withTimeoutOrNull(CACHE_TIMEOUT) {
                cacheManager.searchCachedTerms(query)
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error searching terms", e)
            emptyList()
        }
    }

    // Получение Flow всех терминов для реактивного UI
    fun getAllTermsFlow(): StateFlow<List<Term>> {
        // Загружаем кэшированные данные асинхронно при первом обращении
        if (_allTermsFlow.value.isEmpty()) {
            repositoryScope.launch {
                try {
                    loadCachedTermsToFlow()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading cached terms to flow", e)
                }
            }
        }
        return _allTermsFlow
    }

    suspend fun getTermById(termId: String): Term? {
        return try {
            withTimeoutOrNull(CACHE_TIMEOUT) {
                cacheManager.getTermById(termId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting term by id", e)
            null
        }
    }

    suspend fun syncAllSubjects(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting sync all subjects")

                if (!safeNetworkCheck()) {
                    Log.w(TAG, "No network for sync")
                    return@withContext false
                }

                var allSuccess = true
                Subject.entries.forEach { subject ->
                    try {
                        getTermsForSubject(subject, forceRefresh = true)
                        Log.d(TAG, "Synced ${subject.name}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync ${subject.name}", e)
                        allSuccess = false
                    }
                }

                Log.d(TAG, "Sync completed, success: $allSuccess")
                allSuccess
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing all subjects", e)
                false
            }
        }
    }

    suspend fun getCacheInfo(): SimpleCacheInfo {
        return try {
            withTimeoutOrNull(CACHE_TIMEOUT) {
                cacheManager.getCacheInfo()
            } ?: SimpleCacheInfo(0, emptyMap(), emptyMap())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache info", e)
            SimpleCacheInfo(0, emptyMap(), emptyMap())
        }
    }

    suspend fun clearCache() {
        try {
            Log.d(TAG, "Clearing cache")
            cacheManager.clearCache()
            _allTermsFlow.value = emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }

    private suspend fun loadCachedTermsToFlow() {
        try {
            val cachedTerms = cacheManager.getAllCachedTerms()
            _allTermsFlow.value = cachedTerms
            Log.d(TAG, "Loaded ${cachedTerms.size} cached terms to flow")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached terms to flow", e)
        }
    }

    private suspend fun updateAllTermsFlow() {
        try {
            val allTerms = cacheManager.getAllCachedTerms()
            _allTermsFlow.value = allTerms
            Log.d(TAG, "Updated flow with ${allTerms.size} terms")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating all terms flow", e)
        }
    }

    private fun safeNetworkCheck(): Boolean {
        return try {
            networkChecker.isNetworkAvailable()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network", e)
            false
        }
    }

    private fun parseTermsFromResponse(response: GoogleSheetsResponse, subject: Subject): List<Term> {
        return try {
            response.values.mapNotNull { row ->
                try {
                    if (row.size >= 4 && row.all { it.isNotBlank() }) {
                        Term(
                            id = "${subject.name}_${row[0].hashCode()}_${System.currentTimeMillis()}",
                            kazakh = row[0].trim(),
                            russian = row[1].trim(),
                            english = row[2].trim(),
                            description = row[3].trim(),
                            subject = subject.nameRu
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing row: $row", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            emptyList()
        }
    }
}