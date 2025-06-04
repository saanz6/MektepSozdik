//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//


package com.example.bilimsoz.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bilimsoz.data.model.AppLanguage
import com.example.bilimsoz.ui.components.TermListItem
import com.example.bilimsoz.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTermDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = when (uiState.selectedLanguage) {
                        AppLanguage.KAZAKH -> "Таңдаулылар"
                        AppLanguage.RUSSIAN -> "Избранное"
                        AppLanguage.ENGLISH -> "Favorites"
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (uiState.favoriteTerms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (uiState.selectedLanguage) {
                            AppLanguage.KAZAKH -> "Әзірше ешқандай сөз таңдалмаған"
                            AppLanguage.RUSSIAN -> "Пока нет избранных слов"
                            AppLanguage.ENGLISH -> "No favorite words yet"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = when (uiState.selectedLanguage) {
                            AppLanguage.KAZAKH -> "Ұнаған сөздерді ❤️ белгісімен белгілеңіз"
                            AppLanguage.RUSSIAN -> "Отмечайте понравившиеся слова значком ❤️"
                            AppLanguage.ENGLISH -> "Mark your favorite words with ❤️"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.favoriteTerms) { term ->
                    TermListItem(
                        term = term,
                        language = uiState.selectedLanguage,
                        onClick = { onNavigateToTermDetail(term.id) }
                    )
                }
            }
        }
    }
}