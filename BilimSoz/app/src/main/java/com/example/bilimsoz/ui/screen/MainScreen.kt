//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//

package com.example.bilimsoz.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bilimsoz.data.model.AppLanguage
import com.example.bilimsoz.ui.components.LanguageSelector
import com.example.bilimsoz.ui.components.RandomWordCard
import com.example.bilimsoz.ui.components.SearchBar
import com.example.bilimsoz.ui.components.TermListItem
import com.example.bilimsoz.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToDictionary: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToTermDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LanguageSelector(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageSelected = viewModel::setLanguage
            )
        }

        item {
            SearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    viewModel.updateSearchQuery(it)
                },
                placeholder = when (uiState.selectedLanguage) {
                    AppLanguage.KAZAKH -> "Сөз іздеу"
                    AppLanguage.RUSSIAN -> "Поиск слова"
                    AppLanguage.ENGLISH -> "Search a word"
                }
            )
        }

        if (searchQuery.isNotEmpty()) {
            items(uiState.searchResults) { term ->
                TermListItem(
                    term = term,
                    language = uiState.selectedLanguage,
                    onClick = { onNavigateToTermDetail(term.id) }
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        onClick = onNavigateToDictionary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (uiState.selectedLanguage) {
                                    AppLanguage.KAZAKH -> "Сөздік"
                                    AppLanguage.RUSSIAN -> "Словарь"
                                    AppLanguage.ENGLISH -> "Dictionary"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp),
                        onClick = onNavigateToFavorites,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (uiState.selectedLanguage) {
                                    AppLanguage.KAZAKH -> "Таңдаулылар"
                                    AppLanguage.RUSSIAN -> "Избранное"
                                    AppLanguage.ENGLISH -> "Favorites"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            item {
                uiState.randomTerm?.let { term ->
                    RandomWordCard(
                        term = term,
                        language = uiState.selectedLanguage,
                        onClick = { onNavigateToTermDetail(term.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(200.dp))
            }

            item {
                Text(
                    text = "Powered by Neo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    }