package com.example.pocketbudget.domain

import java.math.BigDecimal
import java.math.RoundingMode

fun toCents(value: Double): Long {
    return BigDecimal.valueOf(value)
        .setScale(2, RoundingMode.HALF_UP)
        .movePointRight(2)
        .longValueExact()
}

fun centsToAmount(cents: Long): Double {
    return BigDecimal.valueOf(cents).movePointLeft(2).toDouble()
}

fun sumMoney(values: Iterable<Double>): Double {
    return centsToAmount(values.sumOf { toCents(it) })
}
