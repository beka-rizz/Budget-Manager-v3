package com.example.pocketbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.example.pocketbudget.di.LocalAppContainer
import com.example.pocketbudget.ui.navigation.PocketBudgetApp
import com.example.pocketbudget.ui.theme.PocketBudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PocketBudgetApplication).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                PocketBudgetTheme {
                    PocketBudgetApp()
                }
            }
        }
    }
}
