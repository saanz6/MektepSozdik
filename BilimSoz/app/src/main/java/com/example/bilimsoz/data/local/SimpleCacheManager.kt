//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//
package com.example.bilimsoz.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.bilimsoz.data.model.Subject
import com.example.bilimsoz.data.model.Term
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SimpleCacheManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("terms_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    // Время кэша - 24 часа
    private val cacheExpirationTime = 24 * 60 * 60 * 1000L

    suspend fun saveTermsForSubject(subject: Subject, terms: List<Term>) {
        withContext(Dispatchers.IO) {
            val editor = prefs.edit()
            val termsJson = json.encodeToString(terms)
            val timestamp = System.currentTimeMillis()

            editor.putString("terms_${subject.name}", termsJson)
            editor.putLong("timestamp_${subject.name}", timestamp)
            editor.apply()
        }
    }

    suspend fun getTermsForSubject(subject: Subject): List<Term>? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = prefs.getLong("timestamp_${subject.name}", 0)
                if (System.currentTimeMillis() - timestamp > cacheExpirationTime) {
                    return@withContext null // Кэш устарел
                }

                val termsJson = prefs.getString("terms_${subject.name}", null)
                if (termsJson != null) {
                    json.decodeFromString<List<Term>>(termsJson)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getAllCachedTerms(): List<Term> {
        return withContext(Dispatchers.IO) {
            val allTerms = mutableListOf<Term>()
            Subject.values().forEach { subject ->
                getTermsForSubject(subject)?.let { terms ->
                    allTerms.addAll(terms)
                }
            }
            allTerms
        }
    }

    suspend fun searchCachedTerms(query: String): List<Term> {
        return withContext(Dispatchers.IO) {
            val allTerms = getAllCachedTerms()
            allTerms.filter { term ->
                term.kazakh.contains(query, ignoreCase = true) ||
                        term.russian.contains(query, ignoreCase = true) ||
                        term.english.contains(query, ignoreCase = true) ||
                        term.description.contains(query, ignoreCase = true)
            }
        }
    }

    suspend fun getTermById(termId: String): Term? {
        return withContext(Dispatchers.IO) {
            getAllCachedTerms().find { it.id == termId }
        }
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            val editor = prefs.edit()
            editor.clear()
            editor.apply()
        }
    }

    suspend fun getCacheInfo(): SimpleCacheInfo {
        return withContext(Dispatchers.IO) {
            val subjectCounts = mutableMapOf<Subject, Int>()
            val lastSyncTimes = mutableMapOf<String, Long>()
            var totalTerms = 0

            Subject.values().forEach { subject ->
                val terms = getTermsForSubject(subject)
                val count = terms?.size ?: 0
                subjectCounts[subject] = count
                totalTerms += count

                val timestamp = prefs.getLong("timestamp_${subject.name}", 0)
                if (timestamp > 0) {
                    lastSyncTimes[subject.nameRu] = timestamp
                }
            }

            SimpleCacheInfo(
                totalTerms = totalTerms,
                subjectCounts = subjectCounts,
                lastSyncTimes = lastSyncTimes
            )
        }
    }
}

data class SimpleCacheInfo(
    val totalTerms: Int,
    val subjectCounts: Map<Subject, Int>,
    val lastSyncTimes: Map<String, Long>
)