package com.example.pocketbudget.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.pocketbudget.data.local.PocketBudgetDatabase
import com.example.pocketbudget.data.repository.BudgetRepository
import com.example.pocketbudget.data.repository.RoomBudgetRepository

class AppContainer(context: Context) {
    private val database = PocketBudgetDatabase.get(context)
    val repository: BudgetRepository = RoomBudgetRepository(database)
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not available")
}
