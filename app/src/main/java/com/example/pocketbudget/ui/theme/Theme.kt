package com.example.pocketbudget.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.pocketbudget.data.Category

@Immutable
data class BudgetColors(
    val income: Color,
    val expense: Color,
    val incomeContainer: Color,
    val expenseContainer: Color,
)

val LocalBudgetColors = staticCompositionLocalOf {
    BudgetColors(
        income = IncomeLight,
        expense = ExpenseLight,
        incomeContainer = IncomeContainerLight,
        expenseContainer = ExpenseContainerLight,
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = OnGreenContainer,
    secondary = Leaf,
    onSecondary = Color.White,
    secondaryContainer = LeafContainer,
    onSecondaryContainer = OnLeafContainer,
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = BackgroundLight,
    onBackground = Ink,
    surface = BackgroundLight,
    onSurface = Ink,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEDF2EE),
    error = ExpenseLight,
    errorContainer = ExpenseContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF0C4F3E),
    onPrimaryContainer = GreenContainer,
    secondary = Color(0xFFB7D0BC),
    onSecondary = Color(0xFF24382A),
    secondaryContainer = Color(0xFF2C4636),
    onSecondaryContainer = LeafContainer,
    tertiary = Color(0xFFF0C48A),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF634014),
    onTertiaryContainer = AmberContainer,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowDark,
    surfaceContainerLow = SurfaceContainerDark,
    error = ExpenseDark,
    errorContainer = ExpenseContainerDark,
)

private val LightBudgetColors = BudgetColors(
    income = IncomeLight,
    expense = ExpenseLight,
    incomeContainer = IncomeContainerLight,
    expenseContainer = ExpenseContainerLight,
)

private val DarkBudgetColors = BudgetColors(
    income = IncomeDark,
    expense = ExpenseDark,
    incomeContainer = IncomeContainerDark,
    expenseContainer = ExpenseContainerDark,
)

@Composable
fun PocketBudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val budgetColors = if (darkTheme) DarkBudgetColors else LightBudgetColors

    CompositionLocalProvider(LocalBudgetColors provides budgetColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

fun Category.accent(): Color = when (this) {
    Category.FOOD -> Color(0xFFC05621)
    Category.TRANSPORT -> Color(0xFF1D6A8A)
    Category.SHOPPING -> Color(0xFF7A4EAB)
    Category.BILLS -> Color(0xFF3E6B4F)
    Category.FUN -> Color(0xFFB23A6A)
    Category.SALARY -> Color(0xFF0F6E56)
    Category.OTHER -> Color(0xFF5C6B73)
}
