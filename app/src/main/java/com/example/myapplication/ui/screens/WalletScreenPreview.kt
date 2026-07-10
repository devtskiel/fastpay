package com.example.myapplication.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.data.api.InternalTransaction
import com.example.myapplication.ui.theme.SwiftPayTheme

import androidx.compose.runtime.CompositionLocalProvider
import com.example.myapplication.LocalNavController
import com.example.myapplication.NavController
import com.example.myapplication.navigation.Route

val MockNavController = object : NavController {
    override fun navigate(route: Route) {}
    override fun pop() {}
    override fun logout() {}
}

@Preview(showBackground = true)
@Composable
fun WalletPreview() {
    SwiftPayTheme {
        CompositionLocalProvider(LocalNavController provides MockNavController) {
            WalletScreen()
        }
    }
}
