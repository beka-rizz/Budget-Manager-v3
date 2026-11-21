package com.example.pocketbudget.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pocketbudget.di.AppContainer
import com.example.pocketbudget.di.LocalAppContainer

@Composable
inline fun <reified VM : ViewModel> activityViewModel(
    noinline create: (AppContainer) -> VM,
): VM {
    val activity = LocalContext.current as ComponentActivity
    val container = LocalAppContainer.current
    return viewModel(
        viewModelStoreOwner = activity,
        factory = viewModelFactory {
            initializer { create(container) }
        },
    )
}
