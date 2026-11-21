package com.example.pocketbudget.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pocketbudget.ui.MessageViewModel
import com.example.pocketbudget.ui.activityViewModel
import com.example.pocketbudget.ui.goal.GoalDetailRoute
import com.example.pocketbudget.ui.goal.GoalFormRoute
import com.example.pocketbudget.ui.goal.GoalMoveRoute
import com.example.pocketbudget.ui.goal.GoalsRoute
import com.example.pocketbudget.ui.home.HomeRoute
import com.example.pocketbudget.ui.transaction.TransactionDetailRoute
import com.example.pocketbudget.ui.transaction.TransactionFormRoute

@Composable
fun PocketBudgetApp() {
    val navController = rememberNavController()
    val messages = activityViewModel { MessageViewModel(it.repository) }
    val snackbarHostState = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val topLevel = route == Routes.HOME || route == Routes.GOALS

    LaunchedEffect(messages) {
        messages.messages.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = message.actionLabel,
                duration = if (message.actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                messages.undo(message.id)
            }
        }
    }

    val bottomBar: @Composable () -> Unit = {
        PocketBottomBar(
            goalsSelected = route == Routes.GOALS,
            onLedger = { navController.navigateToTab(Routes.HOME) },
            onGoals = { navController.navigateToTab(Routes.GOALS) },
        )
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize(),
        ) {
                composable(Routes.HOME) {
                    HomeRoute(navController, messages, bottomBar)
                }
                composable(Routes.GOALS) {
                    GoalsRoute(navController, bottomBar)
                }
                composable(
                    route = Routes.TRANSACTION_DETAIL,
                    arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
                ) { entry ->
                    TransactionDetailRoute(
                        transactionId = entry.arguments?.getLong("transactionId") ?: return@composable,
                        navController = navController,
                        messages = messages,
                    )
                }
                composable(Routes.TRANSACTION_NEW) {
                    TransactionFormRoute(transactionId = null, navController = navController, messages = messages)
                }
                composable(
                    route = Routes.TRANSACTION_EDIT,
                    arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
                ) { entry ->
                    TransactionFormRoute(
                        transactionId = entry.arguments?.getLong("transactionId"),
                        navController = navController,
                        messages = messages,
                    )
                }
                composable(Routes.GOAL_NEW) {
                    GoalFormRoute(goalId = null, navController = navController, messages = messages)
                }
                composable(
                    route = Routes.GOAL_DETAIL,
                    arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
                ) { entry ->
                    GoalDetailRoute(
                        goalId = entry.arguments?.getLong("goalId") ?: return@composable,
                        navController = navController,
                        messages = messages,
                    )
                }
                composable(
                    route = Routes.GOAL_EDIT,
                    arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
                ) { entry ->
                    GoalFormRoute(
                        goalId = entry.arguments?.getLong("goalId"),
                        navController = navController,
                        messages = messages,
                    )
                }
                composable(
                    route = Routes.GOAL_MOVE,
                    arguments = listOf(
                        navArgument("goalId") { type = NavType.LongType },
                        navArgument("mode") {
                            type = NavType.StringType
                            defaultValue = "deposit"
                        },
                    ),
                ) { entry ->
                    GoalMoveRoute(
                        goalId = entry.arguments?.getLong("goalId") ?: return@composable,
                        mode = entry.arguments?.getString("mode") ?: "deposit",
                        navController = navController,
                        messages = messages,
                    )
                }
            }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (topLevel) 88.dp else 12.dp)
                .navigationBarsPadding()
                .imePadding(),
        )
    }
}

@Composable
private fun PocketBottomBar(
    goalsSelected: Boolean,
    onLedger: () -> Unit,
    onGoals: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = !goalsSelected,
            onClick = onLedger,
            icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
            label = { Text("Ledger") },
        )
        NavigationBarItem(
            selected = goalsSelected,
            onClick = onGoals,
            icon = { Icon(Icons.Filled.Savings, contentDescription = null) },
            label = { Text("Goals") },
        )
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
