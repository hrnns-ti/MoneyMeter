package com.example.persony

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class PersonyViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "persony_db")
        .fallbackToDestructiveMigration()
        .build()
    private val dao = db.dao()

    val transactions = dao.getAllTransactions()
    val bills = dao.getAllBills()
    val savings = dao.getAllSavings()
    val userPrefs = dao.getUserPrefs().map { it ?: UserPrefs() }

    fun addTransaction(title: String, amount: Long, isExpense: Boolean) {
        viewModelScope.launch {
            val prefs = userPrefs.first()
            val newBalance = if (isExpense) prefs.totalBalance - amount else prefs.totalBalance + amount

            dao.insertTransaction(TransactionEntity(
                title = title,
                date = "Hari ini",
                amount = formatRupiah(amount),
                isExpense = isExpense
            ))
            dao.updateUserPrefs(prefs.copy(totalBalance = newBalance))
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            val amountLong = tx.amount.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
            val prefs = userPrefs.first()
            val newBalance = if (tx.isExpense) prefs.totalBalance + amountLong else prefs.totalBalance - amountLong

            dao.deleteTransaction(tx)
            dao.updateUserPrefs(prefs.copy(totalBalance = newBalance))
        }
    }

    fun saveBill(title: String, amount: Long, date: String, rec: String, id: String?) {
        viewModelScope.launch {
            dao.upsertBill(BillEntity(id ?: UUID.randomUUID().toString(), title, "Rp ${formatRupiah(amount)}", date, rec))
        }
    }

    fun deleteBill(id: String) {
        viewModelScope.launch {
            val billList = bills.first()
            val bill = billList.find { it.id == id }
            bill?.let { dao.deleteBill(it) }
        }
    }

    fun addSaving(name: String, target: Long, location: String, iconName: String) {
        viewModelScope.launch {
            dao.upsertSaving(SavingEntity(UUID.randomUUID().toString(), name, target, 0L, location, iconName))
        }
    }

    fun depositSaving(id: String, amount: Long) {
        viewModelScope.launch {
            val savingList = savings.first()
            val plan = savingList.find { it.id == id } ?: return@launch
            val prefs = userPrefs.first()
            if (prefs.totalBalance >= amount) {
                dao.upsertSaving(plan.copy(currentAmount = plan.currentAmount + amount))
                addTransaction("Simpan ke ${plan.name}", amount, true)
            }
        }
    }

    fun withdrawSaving(id: String, amount: Long) {
        viewModelScope.launch {
            val savingList = savings.first()
            val plan = savingList.find { it.id == id } ?: return@launch
            if (plan.currentAmount >= amount) {
                dao.upsertSaving(plan.copy(currentAmount = plan.currentAmount - amount))
                addTransaction("Tarik dari ${plan.name}", amount, false)
            }
        }
    }

    fun updateSaving(plan: SavingEntity) {
        viewModelScope.launch { dao.upsertSaving(plan) }
    }

    fun deleteSaving(id: String) {
        viewModelScope.launch { dao.deleteSaving(id) }
    }

    fun updateBudget(newBudget: Long) {
        viewModelScope.launch {
            val prefs = userPrefs.first()
            dao.updateUserPrefs(prefs.copy(dailyBudget = newBudget))
        }
    }

    fun updateName(newName: String) {
        viewModelScope.launch {
            val prefs = userPrefs.first()
            dao.updateUserPrefs(prefs.copy(userName = newName))
        }
    }

    fun resetData() {
        viewModelScope.launch {
            dao.clearTransactions()
            dao.clearBills()
            dao.clearSavings()
            dao.updateUserPrefs(UserPrefs(totalBalance = 0L))
        }
    }
}
