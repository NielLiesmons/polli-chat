package com.polli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.ui.components.PolliGhostButton
import com.polli.ui.components.PolliPrimaryButton
import com.polli.ui.theme.LabColors

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onImportQr: () -> Unit,
    onLinkSecondDevice: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 32.dp,
    onAdvancedSetup: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(LabColors.Black)
                .padding(top = topInset)
                .padding(horizontal = 24.dp)
                .padding(bottom = bottomInset),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Polli",
            color = LabColors.White,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.padding(12.dp))
        Text(
            text = "Secure messaging for everyone",
            color = LabColors.White33,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.padding(32.dp))
        WelcomeButton("Create new account", onCreateAccount)
        Spacer(modifier = Modifier.padding(8.dp))
        WelcomeButton("Scan QR / import", onImportQr)
        Spacer(modifier = Modifier.padding(8.dp))
        WelcomeButton("Link second device", onLinkSecondDevice)
        if (onAdvancedSetup != null) {
            Spacer(modifier = Modifier.padding(8.dp))
            PolliGhostButton(label = "Advanced setup", onClick = onAdvancedSetup)
        }
    }
}

@Composable
private fun WelcomeButton(label: String, onClick: () -> Unit) {
    PolliPrimaryButton(label = label, onClick = onClick)
}
