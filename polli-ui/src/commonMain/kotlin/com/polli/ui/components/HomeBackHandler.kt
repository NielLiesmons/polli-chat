package com.polli.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun HomeBackHandler(enabled: Boolean, onBack: () -> Unit)
