package com.mukeshprajapat.kharcha.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mukeshprajapat.kharcha.ui.theme.MaterialThemeSpacing

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val spacing = MaterialThemeSpacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(spacing.regular),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Settings & Allowlist",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
