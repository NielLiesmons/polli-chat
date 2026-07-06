package com.polli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.polli.ui.components.PolliPrimaryButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.ui.components.ProfileAvatar
import com.polli.ui.components.RoundBackButton
import com.polli.ui.theme.LabColors
import com.polli.ui.theme.accent

@Composable
fun AccountSetupScreen(
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialDisplayName: String = "",
    topInset: Dp = 0.dp,
    bottomInset: Dp = 24.dp,
    externalBusy: Boolean = false,
) {
    var name by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
    var internalBusy by remember { mutableStateOf(false) }
    val busy = externalBusy || internalBusy
    val display = name.ifBlank { "?" }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(LabColors.Black)
                .padding(top = topInset + 8.dp)
                .padding(horizontal = 24.dp)
                .padding(bottom = bottomInset),
    ) {
        RoundBackButton(onClick = onBack)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.padding(16.dp))
            Text(
                text = "Create account",
                color = LabColors.White85,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.padding(20.dp))
            ProfileAvatar(name = display, seed = display, size = 88.dp)
            Spacer(modifier = Modifier.padding(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                colors =
                    TextFieldDefaults.colors(
                        focusedTextColor = LabColors.White85,
                        unfocusedTextColor = LabColors.White85,
                        focusedContainerColor = LabColors.Gray33,
                        unfocusedContainerColor = LabColors.Gray33,
                    ),
            )
            Spacer(modifier = Modifier.padding(24.dp))
            if (busy) {
                CircularProgressIndicator(
                    color = accent().solid,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                PolliPrimaryButton(
                    label = "Create",
                    onClick = {
                        internalBusy = true
                        onCreate(name.trim())
                    },
                )
            }
        }
    }
}
