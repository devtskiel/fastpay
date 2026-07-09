package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

// SwiftPay Enterprise Palette
val SwiftPayPrimary = Color(0xFFF46119) // Primary Action Orange
val SwiftPayPrimaryHover = Color(0xFFD94E00)
val SwiftPayBackground = Color(0xFF0B0C0E) // Deep Fintech Background
val SwiftPaySurface = Color(0xFF14161A) // Surface Elevation
val SwiftPayCard = Color(0xFF1C1F26) // Card/Component Background
val SwiftPayBorder = Color(0xFF2D3139) // Subtle Border

val SwiftPayTextPrimary = Color(0xFFFFFFFF)
val SwiftPayTextSecondary = Color(0xFF94A3B8)
val SwiftPayTextDim = Color(0xFF64748B)

val SwiftPaySuccess = Color(0xFF10B981)
val SwiftPayError = Color(0xFFEF4444)
val SwiftPayWarning = Color(0xFFF59E0B)

// Light Theme mapping (Refined for Fintech standards)
val primaryLight = SwiftPayPrimary
val onPrimaryLight = Color.White
val primaryContainerLight = Color(0xFFFFE7D9)
val onPrimaryContainerLight = SwiftPayPrimaryHover
val backgroundLight = Color(0xFFF8FAFC)
val onBackgroundLight = Color(0xFF0F172A)
val surfaceLight = Color.White
val onSurfaceLight = Color(0xFF0F172A)
val outlineLight = Color(0xFFE2E8F0)

// Dark Theme mapping (Enterprise Dark Mode)
val primaryDark = SwiftPayPrimary
val onPrimaryDark = Color.White
val primaryContainerDark = Color(0xFF4A1E00)
val onPrimaryContainerDark = Color(0xFFFFDBCF)
val backgroundDark = SwiftPayBackground
val onBackgroundDark = SwiftPayTextPrimary
val surfaceDark = SwiftPaySurface
val onSurfaceDark = SwiftPayTextPrimary
val outlineDark = SwiftPayBorder

// Legacy Aliases for compatibility
val FastPayNavy = SwiftPayPrimary
val FastPayBlue = SwiftPayPrimary
val FastPayAccent = SwiftPayPrimary
val FastPayBlack = SwiftPayBackground
val FastPayDarkGray = SwiftPaySurface
val FastPayDarkNavy = SwiftPayBackground
val FastPaySurface = SwiftPaySurface
val FastPayTextPrimary = SwiftPayTextPrimary
val FastPayTextSecondary = SwiftPayTextSecondary
val FastPayTextDim = SwiftPayTextDim
val FastPaySuccess = SwiftPaySuccess
val FastPayError = SwiftPayError
val FastPayWarning = SwiftPayWarning
val FastPayTextField = SwiftPayCard
val FastPayLightGray = SwiftPayBorder
val FastPayMeshBlue = SwiftPayPrimary.copy(alpha = 0.2f)
val FastPayMeshTeal = SwiftPaySuccess.copy(alpha = 0.2f)
val FastPayGradient = listOf(SwiftPayPrimary, SwiftPayPrimary)
val FastPayActionIcon = SwiftPayPrimary
