package com.example.pocketbudget.domain

import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.Transaction
import com.example.pocketbudget.data.TransactionType

data class CategorySum(
    val category: Category,
    val income: Double,
    val expense: Double,
)

data class PocketBudgetUiState(
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val transactions: List<Transaction> = emptyList(),
    val sourceCount: Int = 0,
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categorySums: List<CategorySum> = emptyList(),
) {
    val hasActiveFilter: Boolean
        get() = selectedCategory != null || searchQuery.isNotBlank()
}

data class DraftValidation(
    val titleError: String?,
    val amountError: String?,
    val title: String,
    val amount: Double?,
) {
    val isValid: Boolean
        get() = titleError == null && amountError == null && amount != null
}

object BudgetCalculator {
    fun build(
        transactions: List<Transaction>,
        searchQuery: String,
        selectedCategory: Category?,
    ): PocketBudgetUiState {
        val query = searchQuery.trim()
        val searched = if (query.isEmpty()) {
            transactions
        } else {
            transactions.filter { it.title.contains(query, ignoreCase = true) }
        }
        val visible = if (selectedCategory == null) {
            searched
        } else {
            searched.filter { it.category == selectedCategory }
        }
        val income = visible
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val expense = visible
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val categorySums = visible
            .groupBy { it.category }
            .map { (category, items) ->
                CategorySum(
                    category = category,
                    income = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                    expense = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                )
            }
            .sortedWith(compareByDescending<CategorySum> { it.expense }.thenByDescending { it.income })

        return PocketBudgetUiState(
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            transactions = visible.sortedWith(
                compareByDescending<Transaction> { it.createdAtMillis }.thenByDescending { it.id },
            ),
            sourceCount = transactions.size,
            balance = income - expense,
            totalIncome = income,
            totalExpense = expense,
            categorySums = categorySums,
        )
    }
}

fun sanitizeAmountInput(raw: String): String {
    val normalized = raw.replace(',', '.')
    val builder = StringBuilder()
    var dotSeen = false
    var digitsBeforeDot = 0
    var digitsAfterDot = 0
    for (ch in normalized) {
        when {
            ch.isDigit() && !dotSeen && digitsBeforeDot < 9 -> {
                builder.append(ch)
                digitsBeforeDot++
            }
            ch == '.' && !dotSeen -> {
                if (builder.isEmpty()) builder.append('0')
                builder.append('.')
                dotSeen = true
            }
            ch.isDigit() && dotSeen && digitsAfterDot < 2 -> {
                builder.append(ch)
                digitsAfterDot++
            }
        }
    }
    return builder.toString()
}

fun parseAmount(text: String): Double? {
    val value = text.toDoubleOrNull() ?: return null
    if (value <= 0.0 || value > 99_999_999.99) return null
    return value
}

fun validateTitle(title: String): Pair<String, String?> {
    val cleanTitle = title.trim()
    val titleError = when {
        cleanTitle.isEmpty() -> "Enter a title"
        cleanTitle.length > 40 -> "Keep the title under 40 characters"
        else -> null
    }
    return cleanTitle to titleError
}

fun validateDraft(title: String, amountText: String): DraftValidation {
    val (cleanTitle, titleError) = validateTitle(title)
    val amount = parseAmount(amountText)
    val amountError = if (amount == null) "Enter an amount greater than zero" else null
    return DraftValidation(
        titleError = titleError,
        amountError = amountError,
        title = cleanTitle,
        amount = amount,
    )
}
