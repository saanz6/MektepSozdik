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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.bilimsoz.data.model.AppLanguage
import com.example.bilimsoz.data.model.Subject
import com.example.bilimsoz.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubject: (Subject) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = when (uiState.selectedLanguage) {
                        AppLanguage.KAZAKH -> "Пәндер"
                        AppLanguage.RUSSIAN -> "Предметы"
                        AppLanguage.ENGLISH -> "Subjects"
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Subject.values()) { subject ->
                SubjectCard(
                    subject = subject,
                    language = uiState.selectedLanguage,
                    onClick = { onNavigateToSubject(subject) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectCard(
    subject: Subject,
    language: AppLanguage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = getSubjectIcon(subject),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = subject.getDisplayName(language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getSubjectIcon(subject: Subject): ImageVector {
    return when (subject) {
        Subject.MATHEMATICS -> Icons.Default.Calculate
        Subject.ALGEBRA -> Icons.Default.Functions
        Subject.GEOMETRY -> Icons.Default.Pentagon
        Subject.PHYSICS -> Icons.Default.Science
        Subject.BIOLOGY -> Icons.Default.Eco
        Subject.CHEMISTRY -> Icons.Default.Biotech
        Subject.COMPUTER_SCIENCE -> Icons.Default.Computer
        Subject.GEOGRAPHY -> Icons.Default.Public
        Subject.NATURAL_SCIENCE -> Icons.Default.Nature
    }
}