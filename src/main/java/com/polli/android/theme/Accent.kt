package com.polli.android.theme

import androidx.compose.runtime.Composable
import com.polli.ui.theme.AccentPalette
import com.polli.ui.theme.accent as uiAccent

/** App-layer alias for the current accent palette. */
@Composable
fun accent(): AccentPalette = uiAccent()
