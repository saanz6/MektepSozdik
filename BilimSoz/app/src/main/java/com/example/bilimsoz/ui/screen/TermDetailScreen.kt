//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//

package com.example.bilimsoz.ui.screen

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bilimsoz.data.model.AppLanguage
import com.example.bilimsoz.data.model.Term
import com.example.bilimsoz.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermDetailScreen(
    termId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Состояние для термина
    var term by remember { mutableStateOf<Term?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Загружаем термин при создании экрана
    LaunchedEffect(termId) {
        try {
            isLoading = true
            error = null
            Log.d("TermDetailScreen", "Loading term with id: $termId")

            val foundTerm = viewModel.getTermById(termId)
            if (foundTerm != null) {
                term = foundTerm
                Log.d("TermDetailScreen", "Term loaded: ${foundTerm.kazakh}")
            } else {
                error = when (uiState.selectedLanguage) {
                    AppLanguage.KAZAKH -> "Термин табылмады"
                    AppLanguage.RUSSIAN -> "Термин не найден"
                    AppLanguage.ENGLISH -> "Term not found"
                }
                Log.w("TermDetailScreen", "Term not found for id: $termId")
            }
        } catch (e: Exception) {
            error = "Ошибка загрузки: ${e.message}"
            Log.e("TermDetailScreen", "Error loading term", e)
        } finally {
            isLoading = false
        }
    }

    val isFavorite = term?.id in uiState.favoriteTermIds

    // Показываем индикатор загрузки
    if (isLoading) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = when (uiState.selectedLanguage) {
                            AppLanguage.KAZAKH -> "Жүктелуде..."
                            AppLanguage.RUSSIAN -> "Загрузка..."
                            AppLanguage.ENGLISH -> "Loading..."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        return
    }

    // Показываем ошибку
    if (error != null || term == null) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = error ?: when (uiState.selectedLanguage) {
                            AppLanguage.KAZAKH -> "Термин табылмады"
                            AppLanguage.RUSSIAN -> "Термин не найден"
                            AppLanguage.ENGLISH -> "Term not found"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when (uiState.selectedLanguage) {
                            AppLanguage.KAZAKH -> "Термин жүктелмеді немесе табылмады"
                            AppLanguage.RUSSIAN -> "Термин не загрузился или не найден"
                            AppLanguage.ENGLISH -> "Term failed to load or not found"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            // Пробуем перезагрузить
                            isLoading = true
                            error = null
                        }
                    ) {
                        Text(
                            text = when (uiState.selectedLanguage) {
                                AppLanguage.KAZAKH -> "Қайталау"
                                AppLanguage.RUSSIAN -> "Повторить"
                                AppLanguage.ENGLISH -> "Retry"
                            }
                        )
                    }
                }
            }
        }
        return
    }

    // Показываем детали термина
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        term?.let { viewModel.toggleFavorite(it) }
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        term?.let { currentTerm ->
                            val shareText = buildString {
                                appendLine(currentTerm.getDisplayName(uiState.selectedLanguage))
                                appendLine()
                                when (uiState.selectedLanguage) {
                                    AppLanguage.KAZAKH -> {
                                        appendLine("Орысша: ${currentTerm.russian}")
                                        appendLine("English: ${currentTerm.english}")
                                    }
                                    AppLanguage.RUSSIAN -> {
                                        appendLine("Қазақша: ${currentTerm.kazakh}")
                                        appendLine("English: ${currentTerm.english}")
                                    }
                                    AppLanguage.ENGLISH -> {
                                        appendLine("Қазақша: ${currentTerm.kazakh}")
                                        appendLine("Русский: ${currentTerm.russian}")
                                    }
                                }
                                appendLine()
                                appendLine(currentTerm.description)
                                appendLine()
                                appendLine("Shared from BilimSoz")
                            }

                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }

                            try {
                                context.startActivity(Intent.createChooser(intent, when (uiState.selectedLanguage) {
                                    AppLanguage.KAZAKH -> "Бөлісу"
                                    AppLanguage.RUSSIAN -> "Поделиться"
                                    AppLanguage.ENGLISH -> "Share"
                                }))
                            } catch (e: Exception) {
                                Log.e("TermDetailScreen", "Error sharing", e)
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Основной термин
            Text(
                text = term!!.getDisplayName(uiState.selectedLanguage),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Переводы
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when (uiState.selectedLanguage) {
                            AppLanguage.KAZAKH -> "Аудармалар"
                            AppLanguage.RUSSIAN -> "Переводы"
                            AppLanguage.ENGLISH -> "Translations"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    when (uiState.selectedLanguage) {
                        AppLanguage.KAZAKH -> {
                            LanguageRow("Орысша", term!!.russian)
                            LanguageRow("English", term!!.english)
                        }
                        AppLanguage.RUSSIAN -> {
                            LanguageRow("Қазақша", term!!.kazakh)
                            LanguageRow("English", term!!.english)
                        }
                        AppLanguage.ENGLISH -> {
                            LanguageRow("Қазақша", term!!.kazakh)
                            LanguageRow("Русский", term!!.russian)
                        }
                    }
                }
            }

            // Описание
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when (uiState.selectedLanguage) {
                            AppLanguage.KAZAKH -> "Сипаттама"
                            AppLanguage.RUSSIAN -> "Описание"
                            AppLanguage.ENGLISH -> "Description"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = term!!.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LanguageRow(
    language: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$language:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}