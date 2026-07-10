package com.example.myapplication.data.repository

import com.example.myapplication.data.SwiftPayService
import com.example.myapplication.data.TransactionStore
import com.example.myapplication.data.api.InternalTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository for transaction operations.
 * Manages transaction history, storage, and retrieval.
 */
class TransactionRepository(
    private val swiftPayService: SwiftPayService,
    private val transactionStore: TransactionStore
) {

    /**
     * Get all transactions as a Flow
     */
    fun getAllTransactions(): Flow<List<InternalTransaction>> =
        transactionStore.transactions

    /**
     * Save a new transaction
     */
    suspend fun saveTransaction(transaction: InternalTransaction) {
        transactionStore.record(transaction)
    }

    /**
     * Sync local transactions with the remote API
     */
    suspend fun syncWithApi(): Result<List<InternalTransaction>> {
        return swiftPayService.getInternalTransactions().onSuccess { remote ->
            remote.forEach { tx ->
                transactionStore.record(tx)
            }
        }
    }
}
