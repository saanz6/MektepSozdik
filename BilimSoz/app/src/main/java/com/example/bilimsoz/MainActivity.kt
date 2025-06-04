//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//

package com.example.bilimsoz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.bilimsoz.ui.theme.BilimSozTheme
import com.example.bilimsoz.ui.navigation.BilimSozNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BilimSozTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BilimSozNavigation(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}