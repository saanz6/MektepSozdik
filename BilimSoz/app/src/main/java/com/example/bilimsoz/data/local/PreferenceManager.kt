//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//
package com.example.bilimsoz.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bilimsoz.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesManager(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
        private val WORD_OF_DAY_KEY = stringPreferencesKey("word_of_day_id")
        private val WORD_OF_DAY_DATE_KEY = longPreferencesKey("word_of_day_date")
    }

    val selectedLanguage: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        val languageCode = preferences[LANGUAGE_KEY] ?: AppLanguage.RUSSIAN.code
        AppLanguage.entries.find { it.code == languageCode } ?: AppLanguage.RUSSIAN
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }
    }

    val favoriteTermIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITES_KEY] ?: emptySet()
    }

    suspend fun addToFavorites(termId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = currentFavorites + termId
        }
    }

    suspend fun removeFromFavorites(termId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = currentFavorites - termId
        }
    }

    // Новые методы для "Слово дня"
    suspend fun getWordOfDay(): Pair<String?, Long> {
        return context.dataStore.data.map { preferences ->
            val wordId = preferences[WORD_OF_DAY_KEY]
            val date = preferences[WORD_OF_DAY_DATE_KEY] ?: 0L
            Pair(wordId, date)
        }.first()
    }

    suspend fun setWordOfDay(termId: String) {
        context.dataStore.edit { preferences ->
            preferences[WORD_OF_DAY_KEY] = termId
            preferences[WORD_OF_DAY_DATE_KEY] = getCurrentDayTimestamp()
        }
    }

    suspend fun shouldUpdateWordOfDay(): Boolean {
        val (_, savedDate) = getWordOfDay()
        val currentDayTimestamp = getCurrentDayTimestamp()
        return savedDate != currentDayTimestamp
    }

    private fun getCurrentDayTimestamp(): Long {
        // Возвращаем timestamp начала текущего дня (00:00:00)
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}