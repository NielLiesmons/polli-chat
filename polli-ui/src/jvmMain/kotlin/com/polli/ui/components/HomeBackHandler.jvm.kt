package com.polli.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun HomeBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop window close is handled by the host; no system back gesture.
}
