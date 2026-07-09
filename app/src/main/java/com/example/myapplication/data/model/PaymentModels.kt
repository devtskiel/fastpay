package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentData(
    val amount: Double = 0.0,
    val currency: String = "PHP",
    val description: String = "Payment"
)
