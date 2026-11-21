package com.example.pocketbudget

import com.example.pocketbudget.data.Category
import com.example.pocketbudget.data.ContributionKind
import com.example.pocketbudget.data.GoalStatus
import com.example.pocketbudget.data.SavingsGoal
import com.example.pocketbudget.data.Transaction
import com.example.pocketbudget.data.TransactionGoalLink
import com.example.pocketbudget.data.TransactionType
import com.example.pocketbudget.data.repository.BudgetRepository
import com.example.pocketbudget.data.repository.TransactionDeletion
import com.example.pocketbudget.data.repository.GoalActionResult
import com.example.pocketbudget.data.repository.GoalDeleteResult
import com.example.pocketbudget.data.repository.GoalWriteResult
import com.example.pocketbudget.data.repository.TransactionWriteResult
import com.example.pocketbudget.data.sampleSnapshot
import com.example.pocketbudget.domain.BudgetCalculator
import com.example.pocketbudget.domain.GoalDetails
import com.example.pocketbudget.domain.GoalDirectory
import com.example.pocketbudget.domain.GoalWithProgress
import com.example.pocketbudget.domain.MovePlan
import com.example.pocketbudget.domain.completeError
import com.example.pocketbudget.domain.directoryFrom
import com.example.pocketbudget.domain.pauseError
import com.example.pocketbudget.domain.planDeposit
import com.example.pocketbudget.domain.planWithdraw
import com.example.pocketbudget.domain.reopenError
import com.example.pocketbudget.domain.resumeError
import com.example.pocketbudget.domain.sanitizeAmountInput
import com.example.pocketbudget.domain.statusAfterTargetChange
import com.example.pocketbudget.domain.validateDraft
import com.example.pocketbudget.domain.validateGoalDraft
import com.example.pocketbudget.ui.MessageViewModel
import com.example.pocketbudget.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BudgetCalculatorTest {
    private val transactions = listOf(
        Transaction(1, "Scholarship", 45_000.0, TransactionType.INCOME, Category.SALARY, 3),
        Transaction(2, "Groceries", 10_000.0, TransactionType.EXPENSE, Category.FOOD, 2),
        Transaction(3, "Metro", 2_000.0, TransactionType.EXPENSE, Category.TRANSPORT, 1),
        Transaction(4, "Cafe", 1_500.0, TransactionType.EXPENSE, Category.FOOD, 4),
    )

    @Test
    fun totalsAndCategorySumsComeFromTheFullList() {
        val state = BudgetCalculator.build(transactions, searchQuery = "", selectedCategory = null)

        assertEquals(45_000.0, state.totalIncome, 0.001)
        assertEquals(13_500.0, state.totalExpense, 0.001)
        assertEquals(31_500.0, state.balance, 0.001)
        assertEquals(listOf(4L, 1L, 2L, 3L), state.transactions.map { it.id })
        assertEquals(
            listOf(Category.FOOD, Category.TRANSPORT, Category.SALARY),
            state.categorySums.map { it.category },
        )
        assertEquals(11_500.0, state.categorySums.first().expense, 0.001)
    }

    @Test
    fun categoryFilterChangesTotalsAndSums() {
        val state = BudgetCalculator.build(transactions, "", Category.FOOD)

        assertEquals(0.0, state.totalIncome, 0.001)
        assertEquals(11_500.0, state.totalExpense, 0.001)
        assertEquals(-11_500.0, state.balance, 0.001)
        assertEquals(listOf(Category.FOOD), state.categorySums.map { it.category })
        assertEquals(2, state.transactions.size)
        assertEquals(4, state.sourceCount)
    }

    @Test
    fun searchMatchesTitleAndStillRespectsCategory() {
        val searched = BudgetCalculator.build(transactions, "metro", null)
        assertEquals(listOf(3L), searched.transactions.map { it.id })
        assertEquals(2_000.0, searched.totalExpense, 0.001)

        val both = BudgetCalculator.build(transactions, "a", Category.FOOD)
        assertEquals(listOf("Cafe"), both.transactions.map { it.title })
    }

    @Test
    fun blankSearchDoesNotFilter() {
        val state = BudgetCalculator.build(transactions, "   ", null)
        assertEquals(4, state.transactions.size)
        assertEquals(false, state.hasActiveFilter)
    }
}

class DraftValidationTest {
    @Test
    fun rejectsBlankTitleAndNonPositiveAmount() {
        val result = validateDraft("  ", "0")
        assertEquals("Enter a title", result.titleError)
        assertEquals("Enter an amount greater than zero", result.amountError)
        assertEquals(false, result.isValid)
    }

    @Test
    fun acceptsACommaDecimalAfterSanitizing() {
        val amount = sanitizeAmountInput("1,5")
        assertEquals("1.5", amount)
        val result = validateDraft("Coffee", amount)
        assertNull(result.titleError)
        assertNull(result.amountError)
        assertEquals(1.5, result.amount!!, 0.001)
    }

    @Test
    fun goalTargetMustCoverMoneyAlreadySaved() {
        val result = validateGoalDraft("Laptop", "100", "", saved = 250.0)
        assertEquals("Target must cover the amount already saved", result.targetError)
        assertEquals(false, result.isValid)
    }
}

class GoalRulesTest {
    private fun goal(status: GoalStatus, target: Double = 100.0) = SavingsGoal(
        id = 1,
        name = "Laptop",
        targetAmount = target,
        category = Category.SHOPPING,
        note = "",
        status = status,
        createdAtMillis = 0,
        completedAtMillis = null,
    )

    @Test
    fun sampleGoalsMatchTheirContributions() {
        val snapshot = sampleSnapshot(now = 1_700_000_000_000)
        val directory = directoryFrom(snapshot.goals, snapshot.contributions)
        val laptop = directory.goals.first { it.goal.name == "New laptop" }
        val trip = directory.goals.first { it.goal.name == "Summer trip" }
        val emergency = directory.goals.first { it.goal.name == "Emergency fund" }

        assertEquals(40_000.0, laptop.saved, 0.001)
        assertEquals(false, laptop.isFunded)
        assertEquals(15_000.0, trip.saved, 0.001)
        assertEquals(GoalStatus.PAUSED, trip.goal.status)
        assertEquals(50_000.0, emergency.saved, 0.001)
        assertEquals(true, emergency.isFunded)
        assertEquals("New laptop", directory.goalNameByTransactionId[12])
    }

    @Test
    fun fundingPausingAndCompletingFollowTheRules() {
        val active = goal(GoalStatus.ACTIVE)
        assertNull(pauseError(active, saved = 40.0))
        assertEquals(
            "Add the remaining amount before marking this goal complete.",
            completeError(active, saved = 40.0),
        )

        val deposit = planDeposit(active, saved = 40.0, amount = 60.0, note = "  rest  ")
        assertTrue(deposit is MovePlan.Ready)
        val ready = deposit as MovePlan.Ready
        assertEquals(true, ready.fundedAfter)
        assertEquals(ContributionKind.DEPOSIT, ready.kind)
        assertEquals(TransactionType.EXPENSE, ready.type)
        assertEquals("rest", ready.note)
        assertEquals("Saved for Laptop", ready.title)

        assertNull(completeError(active, saved = 100.0))
        assertEquals(
            "This goal is funded. Mark it complete instead of pausing it.",
            pauseError(active, saved = 100.0),
        )

        val over = planDeposit(active, saved = 40.0, amount = 70.0, note = "")
        assertTrue(over is MovePlan.Rejected)

        val paused = active.copy(status = GoalStatus.PAUSED)
        assertEquals("Only an active goal can be marked complete.", completeError(paused, saved = 100.0))
        assertNull(resumeError(paused))
        assertEquals("Only a paused goal can be resumed.", resumeError(active))

        val done = active.copy(status = GoalStatus.COMPLETED)
        val blocked = planWithdraw(done, saved = 100.0, amount = 10.0, note = "")
        assertTrue(blocked is MovePlan.Rejected)
        assertNull(reopenError(done))
        assertEquals(
            "Only a completed goal can be reopened.",
            reopenError(active),
        )

        val withdraw = planWithdraw(active, saved = 100.0, amount = 25.0, note = "")
        assertTrue(withdraw is MovePlan.Ready)
        assertEquals(TransactionType.INCOME, (withdraw as MovePlan.Ready).type)
        assertEquals(GoalStatus.ACTIVE, statusAfterTargetChange(GoalStatus.COMPLETED, 150.0, 100.0))
        assertEquals(GoalStatus.COMPLETED, statusAfterTargetChange(GoalStatus.COMPLETED, 100.0, 100.0))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchFilterDeleteAndUndoUpdateTheLedger() {
        val repository = FakeBudgetRepository(
            listOf(
                Transaction(1, "Lunch", 2_500.0, TransactionType.EXPENSE, Category.FOOD, 2),
                Transaction(2, "Metro", 800.0, TransactionType.EXPENSE, Category.TRANSPORT, 1),
            ),
        )
        val messages = MessageViewModel(repository)
        val viewModel = HomeViewModel(repository, messages)

        assertEquals(2, viewModel.uiState.value.sourceCount)
        assertEquals(-3_300.0, viewModel.uiState.value.balance, 0.001)

        viewModel.onSearchQueryChange("lun")
        assertEquals(listOf("Lunch"), viewModel.uiState.value.transactions.map { it.title })
        viewModel.onCategorySelected(Category.TRANSPORT)
        assertEquals(0, viewModel.uiState.value.transactions.size)
        assertEquals(2, viewModel.uiState.value.sourceCount)

        viewModel.clearFilters()
        viewModel.onTransactionSaved(
            Transaction(9, "Dinner", 1.0, TransactionType.EXPENSE, Category.FUN, 3),
        )
        assertEquals("", viewModel.uiState.value.searchQuery)

        viewModel.deleteTransaction(1)
        assertEquals(1, viewModel.uiState.value.sourceCount)
        messages.undo(messages.lastUndoMessageId!!)
        assertEquals(2, viewModel.uiState.value.sourceCount)
        assertEquals("Lunch", viewModel.uiState.value.transactions.first { it.id == 1L }.title)
    }
}

private class FakeBudgetRepository(
    initial: List<Transaction>,
) : BudgetRepository {
    private val transactions = MutableStateFlow(initial)

    override fun observeTransactions(): Flow<List<Transaction>> = transactions

    override fun observeGoalDirectory(): Flow<GoalDirectory> {
        return MutableStateFlow(GoalDirectory(emptyList(), emptyMap()))
    }

    override suspend fun deleteTransaction(id: Long): TransactionDeletion? {
        val transaction = transactions.value.firstOrNull { it.id == id } ?: return null
        transactions.value = transactions.value.filterNot { it.id == id }
        return TransactionDeletion(transaction = transaction, goalName = null, contribution = null)
    }

    override suspend fun restoreTransaction(deletion: TransactionDeletion) {
        if (transactions.value.any { it.id == deletion.transaction.id }) return
        transactions.value = transactions.value + deletion.transaction
    }

    override fun observeTransaction(id: Long): Flow<Transaction?> = unused()
    override fun observeTransactionLink(transactionId: Long): Flow<TransactionGoalLink?> = unused()
    override fun observeGoal(id: Long): Flow<GoalDetails?> = unused()
    override suspend fun getTransaction(id: Long): Transaction? = unused()
    override suspend fun getTransactionLink(transactionId: Long): TransactionGoalLink? = unused()
    override suspend fun getGoalProgress(id: Long): GoalWithProgress? = unused()
    override suspend fun saveTransaction(
        id: Long?,
        title: String,
        amountText: String,
        type: TransactionType,
        category: Category,
    ): TransactionWriteResult = unused()

    override suspend fun saveGoal(
        id: Long?,
        name: String,
        targetText: String,
        category: Category,
        note: String,
    ): GoalWriteResult = unused()

    override suspend fun deleteGoal(id: Long, releaseSavings: Boolean): GoalDeleteResult = unused()
    override suspend fun deposit(goalId: Long, amount: Double, note: String): GoalActionResult = unused()
    override suspend fun withdraw(goalId: Long, amount: Double, note: String): GoalActionResult = unused()
    override suspend fun pauseGoal(id: Long): GoalActionResult = unused()
    override suspend fun resumeGoal(id: Long): GoalActionResult = unused()
    override suspend fun completeGoal(id: Long): GoalActionResult = unused()
    override suspend fun reopenGoal(id: Long): GoalActionResult = unused()

    private fun unused(): Nothing = error("not used in this test")
}
