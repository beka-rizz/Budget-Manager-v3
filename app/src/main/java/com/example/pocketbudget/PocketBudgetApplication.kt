package com.example.pocketbudget

import android.app.Application
import com.example.pocketbudget.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class PocketBudgetApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        runBlocking(Dispatchers.IO) {
            container.repository.seedIfNeeded()
        }
    }
}
