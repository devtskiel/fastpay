package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.MayaService
import com.example.myapplication.data.SettingsManager
import com.example.myapplication.data.TransactionStore
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.QrRepository
import com.example.myapplication.data.repository.TransactionRepository
import com.example.myapplication.domain.usecase.AuthenticateUseCase
import com.example.myapplication.domain.usecase.GenerateQrUseCase
import com.example.myapplication.domain.usecase.ProcessPaymentUseCase

/**
 * Simple dependency injection container.
 * Creates and provides instances for repositories and use cases.
 */
object DIContainer {

    private var context: Context? = null
    private var mayaService: MayaService? = null
    private var settingsManager: SettingsManager? = null
    private var transactionStore: TransactionStore? = null

    /**
     * Initialize the DI container with application context
     */
    fun initialize(appContext: Context) {
        context = appContext
        settingsManager = SettingsManager(appContext)
        transactionStore = TransactionStore(appContext)
    }

    /**
     * Get or create MayaService instance
     */
    fun provideMayaService(): MayaService {
        if (mayaService == null) {
            mayaService = MayaService()
        }
        return mayaService!!
    }

    /**
     * Get or create SettingsManager instance
     */
    fun provideSettingsManager(): SettingsManager {
        return settingsManager ?: throw IllegalStateException("DIContainer not initialized")
    }

    /**
     * Get or create TransactionStore instance
     */
    fun provideTransactionStore(): TransactionStore {
        return transactionStore ?: throw IllegalStateException("DIContainer not initialized")
    }

    // Repositories

    fun providePaymentRepository(): PaymentRepository =
        PaymentRepository(provideMayaService())

    fun provideAuthRepository(): AuthRepository =
        AuthRepository(provideMayaService())

    fun provideQrRepository(): QrRepository =
        QrRepository(provideMayaService())

    fun provideTransactionRepository(): TransactionRepository =
        TransactionRepository(provideMayaService(), provideTransactionStore())

    // Use Cases

    fun provideProcessPaymentUseCase(): ProcessPaymentUseCase =
        ProcessPaymentUseCase(providePaymentRepository())

    fun provideGenerateQrUseCase(): GenerateQrUseCase =
        GenerateQrUseCase(provideQrRepository())

    fun provideAuthenticateUseCase(): AuthenticateUseCase =
        AuthenticateUseCase(provideAuthRepository(), provideSettingsManager())
}

