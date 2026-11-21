package com.example.pocketbudget.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun formatMoney(amount: Double, withSign: Boolean = false): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        isGroupingUsed = true
    }
    val prefix = when {
        amount < 0 -> "−"
        withSign && amount > 0 -> "+"
        else -> ""
    }
    return "$prefix₸${formatter.format(abs(amount))}"
}

fun formatAmountInput(amount: Double): String {
    return BigDecimal.valueOf(amount)
        .setScale(2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}

fun formatTransactionDate(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val transactionDay = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    if (sameDay(transactionDay, today)) return "Today"
    today.add(Calendar.DAY_OF_YEAR, -1)
    if (sameDay(transactionDay, today)) return "Yesterday"
    return SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochMillis))
}

fun formatExactDateTime(epochMillis: Long): String {
    return SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epochMillis))
}

private fun sameDay(first: Calendar, second: Calendar): Boolean {
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}
