package com.budgetquest.app.data.repository

import android.util.Log
import com.budgetquest.app.data.local.dao.BudgetDao
import com.budgetquest.app.data.local.entity.CategoryEntity
import com.budgetquest.app.data.local.entity.ExpenseEntity
import com.budgetquest.app.data.local.entity.QuestEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that mediates all access to expenses, categories and quests.
 *
 * It wraps the [BudgetDao] so the ViewModel layer depends on a clean,
 * intention-revealing API rather than raw database queries. Read operations
 * return Room [kotlinx.coroutines.flow.Flow]s (so the UI updates automatically
 * when the underlying tables change); write operations are suspend functions
 * that run on Room's background executor.
 */
@Singleton
class ExpenseRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    // ---- Expenses ---------------------------------------------------------

    fun getAllExpenses(userId: Int) = budgetDao.getAllExpenses(userId)

    fun getExpensesInRange(userId: Int, start: Long, end: Long) =
        budgetDao.getExpensesInRange(userId, start, end)

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        Log.d(TAG, "insertExpense() amount=${expense.amount} categoryId=${expense.categoryId} userId=${expense.userId}")
        return budgetDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        Log.d(TAG, "updateExpense() id=${expense.id}")
        budgetDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        Log.d(TAG, "deleteExpense() id=${expense.id}")
        budgetDao.deleteExpense(expense)
    }

    // ---- Categories -------------------------------------------------------

    fun getAllCategories(userId: Int) = budgetDao.getAllCategories(userId)

    suspend fun insertCategory(category: CategoryEntity) {
        Log.d(TAG, "insertCategory() name='${category.name}' limit=${category.monthlyLimit}")
        budgetDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        Log.d(TAG, "deleteCategory() id=${category.id} name='${category.name}'")
        budgetDao.deleteCategory(category)
    }

    /** Removes every expense tied to a category — called when a category is deleted
     *  to avoid orphaned rows that would skew spending totals. */
    suspend fun deleteExpensesByCategory(categoryId: Int) {
        Log.d(TAG, "deleteExpensesByCategory() categoryId=$categoryId")
        budgetDao.deleteExpensesByCategory(categoryId)
    }

    // ---- Aggregations (used by the dashboard charts) ----------------------

    fun getTotalSpending(userId: Int, start: Long, end: Long) =
        budgetDao.getTotalSpendingInRange(userId, start, end)

    fun getSpendingByCategory(userId: Int, start: Long, end: Long) =
        budgetDao.getSpendingByCategory(userId, start, end)

    // ---- Quests (gamification) -------------------------------------------

    fun getAllQuests(userId: Int) = budgetDao.getAllQuests(userId)

    suspend fun insertQuest(quest: QuestEntity) {
        Log.d(TAG, "insertQuest() title='${quest.title}'")
        budgetDao.insertQuest(quest)
    }

    suspend fun updateQuest(quest: QuestEntity) {
        Log.d(TAG, "updateQuest() id=${quest.id} progress=${quest.progress}")
        budgetDao.updateQuest(quest)
    }

    suspend fun deleteQuest(quest: QuestEntity) {
        Log.d(TAG, "deleteQuest() id=${quest.id}")
        budgetDao.deleteQuest(quest)
    }

    companion object {
        private const val TAG = "ExpenseRepository"
    }
}
