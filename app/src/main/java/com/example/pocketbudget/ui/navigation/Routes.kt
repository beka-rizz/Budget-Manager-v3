package com.example.pocketbudget.ui.navigation

object Routes {
    const val HOME = "home"
    const val GOALS = "goals"
    const val TRANSACTION_NEW = "create-transaction"
    const val TRANSACTION_DETAIL = "transaction/{transactionId}"
    const val TRANSACTION_EDIT = "transaction/{transactionId}/edit"
    const val GOAL_NEW = "create-goal"
    const val GOAL_DETAIL = "goal/{goalId}"
    const val GOAL_EDIT = "goal/{goalId}/edit"
    const val GOAL_MOVE = "goal/{goalId}/move?mode={mode}"

    fun transactionDetail(id: Long) = "transaction/$id"
    fun transactionEdit(id: Long) = "transaction/$id/edit"
    fun goalDetail(id: Long) = "goal/$id"
    fun goalEdit(id: Long) = "goal/$id/edit"
    fun goalMove(id: Long, mode: String) = "goal/$id/move?mode=$mode"
}
