package com.example.pocketbudget.ui.theme

import androidx.annotation.DrawableRes
import com.example.pocketbudget.R
import com.example.pocketbudget.data.Category

@DrawableRes
fun Category.iconRes(): Int = when (this) {
    Category.FOOD -> R.drawable.ic_category_food
    Category.TRANSPORT -> R.drawable.ic_category_transport
    Category.SHOPPING -> R.drawable.ic_category_shopping
    Category.BILLS -> R.drawable.ic_category_bills
    Category.FUN -> R.drawable.ic_category_fun
    Category.SALARY -> R.drawable.ic_category_salary
    Category.OTHER -> R.drawable.ic_category_other
}
