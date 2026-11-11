package com.mukeshprajapat.kharcha.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Transactions : Screen("transactions", "History", Icons.AutoMirrored.Filled.ReceiptLong)
    data object AddEdit : Screen("add_edit", "Add", Icons.Default.AddCircle)
    data object Insights : Screen("insights", "Insights", Icons.Default.PieChart)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Home, Transactions, AddEdit, Insights, Settings)
    }
}
