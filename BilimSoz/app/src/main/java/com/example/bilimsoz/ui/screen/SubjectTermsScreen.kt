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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bilimsoz.data.model.Subject
import com.example.bilimsoz.ui.components.SearchBar
import com.example.bilimsoz.ui.components.TermListItem
import com.example.bilimsoz.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectTermsScreen(
    subject: Subject,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTermDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(subject) {
        viewModel.loadTermsForSubject(subject)
    }

    val filteredTerms = remember(uiState.subjectTerms, searchQuery) {
        if (searchQuery.isEmpty()) {
            uiState.subjectTerms
        } else {
            uiState.subjectTerms.filter { term ->
                term.kazakh.contains(searchQuery, true) ||
                        term.russian.contains(searchQuery, true) ||
                        term.english.contains(searchQuery, true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(text = subject.getDisplayName(uiState.selectedLanguage))
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = when (uiState.selectedLanguage) {
                    com.example.bilimsoz.data.model.AppLanguage.KAZAKH -> "Сөз іздеу"
                    com.example.bilimsoz.data.model.AppLanguage.RUSSIAN -> "Поиск слова"
                    com.example.bilimsoz.data.model.AppLanguage.ENGLISH -> "Search a word"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTerms) { term ->
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
}