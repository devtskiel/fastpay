package com.example.myapplication.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Login : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Wallet : Route

    @Serializable
    data object Payout : Route

    @Serializable
    data object Hub : Route

    @Serializable
    data object Invoices : Route

    @Serializable
    data object Webhooks : Route

    @Serializable
    data object Vca : Route

    @Serializable
    data class Payment(val amount: Double) : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data object ApiKeys : Route

    @Serializable
    data object ApiDocs : Route

    @Serializable
    data object Approvals : Route
}
