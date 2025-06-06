//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//


package com.example.bilimsoz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bilimsoz.data.model.Subject
import com.example.bilimsoz.ui.screen.DictionaryScreen
import com.example.bilimsoz.ui.screen.FavoritesScreen
import com.example.bilimsoz.ui.screen.MainScreen
import com.example.bilimsoz.ui.screen.SubjectTermsScreen
import com.example.bilimsoz.ui.screen.TermDetailScreen
import com.example.bilimsoz.ui.viewmodel.MainViewModel

@Composable
fun BilimSozNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = modifier
    ) {
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onNavigateToDictionary = { navController.navigate("dictionary") },
                onNavigateToFavorites = { navController.navigate("favorites") },
                onNavigateToTermDetail = { termId ->
                    navController.navigate("term_detail/$termId")
                }
            )
        }

        composable("dictionary") {
            DictionaryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubject = { subject: Subject ->
                    navController.navigate("subject/${subject.name}")
                }
            )
        }

        composable("subject/{subjectName}") { backStackEntry ->
            val subjectName = backStackEntry.arguments?.getString("subjectName")
            val subject = Subject.values().find { it.name == subjectName }
            if (subject != null) {
                SubjectTermsScreen(
                    subject = subject,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTermDetail = { termId: String ->
                        navController.navigate("term_detail/$termId")
                    }
                )
            }
        }

        composable("favorites") {
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTermDetail = { termId: String ->
                    navController.navigate("term_detail/$termId")
                }
            )
        }

        composable("term_detail/{termId}") { backStackEntry ->
            val termId = backStackEntry.arguments?.getString("termId") ?: ""
            TermDetailScreen(
                termId = termId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}