package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// SwiftPay Enterprise Palette - PH Banking Standards
val SwiftPayPrimary = Color(0xFF1F4BA8) // Authority Blue
val SwiftPayPrimaryHover = Color(0xFF163E8C)
val SwiftPayBackground = Color(0xFF0A0E14) // Deep Professional Background
val SwiftPaySurface = Color(0xFF121820) // Surface Elevation
val SwiftPayCard = Color(0xFF1C242E) // Card/Component Background
val SwiftPayBorder = Color(0xFF2E3A47) // Subtle Professional Border

val SwiftPayTextPrimary = Color(0xFFFFFFFF)
val SwiftPayTextSecondary = Color(0xFF94A3B8)
val SwiftPayTextDim = Color(0xFF64748B)

val SwiftPaySuccess = Color(0xFF2ECC71) // Banking Success Green
val SwiftPayError = Color(0xFFE74C3C) // Banking Error Red
val SwiftPayWarning = Color(0xFFF1C40F)

// Light Theme mapping (Enterprise Standards)
val primaryLight = SwiftPayPrimary
val onPrimaryLight = Color.White
val primaryContainerLight = Color(0xFFE3E8FF)
val onPrimaryContainerLight = SwiftPayPrimary
val backgroundLight = Color(0xFFF1F5F9)
val onBackgroundLight = Color(0xFF0F172A)
val surfaceLight = Color.White
val onSurfaceLight = Color(0xFF0F172A)
val outlineLight = Color(0xFFCBD5E1)

// Dark Theme mapping (Enterprise Dark Mode)
val primaryDark = SwiftPayPrimary
val onPrimaryDark = Color.White
val primaryContainerDark = Color(0xFF163E8C)
val onPrimaryContainerDark = Color(0xFFE3E8FF)
val backgroundDark = SwiftPayBackground
val onBackgroundDark = SwiftPayTextPrimary
val surfaceDark = SwiftPaySurface
val onSurfaceDark = SwiftPayTextPrimary
val outlineDark = SwiftPayBorder

// Theme-aware helpers
@Composable
fun getSwiftPayBackground() = MaterialTheme.colorScheme.background

@Composable
fun getSwiftPaySurface() = MaterialTheme.colorScheme.surface

@Composable
fun getSwiftPayTextPrimary() = if (MaterialTheme.colorScheme.background == backgroundDark) Color.White else Color(0xFF0F172A)

@Composable
fun getSwiftPayTextSecondary() = if (MaterialTheme.colorScheme.background == backgroundDark) Color.LightGray else Color.Gray

// SwiftPay Aliases
val SwiftPayNavy = SwiftPayPrimary
val SwiftPayBlue = SwiftPayPrimary
val SwiftPayActionIcon = SwiftPayPrimary
