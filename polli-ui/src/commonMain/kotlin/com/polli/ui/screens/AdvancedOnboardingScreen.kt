package com.polli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.polli.ui.components.PolliPrimaryButton
import com.polli.ui.components.ProfileAvatar
import com.polli.ui.components.RoundBackButton
import com.polli.ui.theme.LabColors
import com.polli.ui.theme.accent

@Composable
fun AdvancedOnboardingScreen(
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onShowOtherOptions: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialDisplayName: String = "",
    providerHost: String = "",
    isDcLogin: Boolean = false,
    invitationMessage: String? = null,
    avatarBitmap: ImageBitmap? = null,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 24.dp,
    externalBusy: Boolean = false,
    displayName: String = initialDisplayName,
    onDisplayNameChange: (String) -> Unit = {},
) {
    val busy = externalBusy
    val name = displayName
    val display = name.ifBlank { "?" }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(LabColors.Black)
                .verticalScroll(rememberScrollState())
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
                text = "Your profile",
                color = LabColors.White85,
                style = MaterialTheme.typography.titleLarge,
            )
            if (!invitationMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = invitationMessage,
                    color = LabColors.White66,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.padding(20.dp))
            if (avatarBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = avatarBitmap,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(88.dp)
                            .clickable(onClick = onPickAvatar),
                )
            } else {
                ProfileAvatar(
                    name = display,
                    seed = display,
                    size = 88.dp,
                    modifier = Modifier.clickable(onClick = onPickAvatar),
                )
            }
            Text(
                text = "Tap to change photo",
                color = LabColors.White33,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.padding(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onDisplayNameChange,
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
            if (providerHost.isNotBlank() && !isDcLogin) {
                Spacer(modifier = Modifier.padding(12.dp))
                TextButton(onClick = onPrivacyPolicyClick, enabled = !busy) {
                    Text(
                        text = "Privacy policy for $providerHost",
                        color = accent().light,
                    )
                }
            }
            if (isDcLogin && providerHost.isNotBlank()) {
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = "Log in to $providerHost",
                    color = LabColors.White66,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.padding(16.dp))
            TextButton(onClick = onShowOtherOptions, enabled = !busy) {
                Text("Other server options", color = LabColors.White66)
            }
            Spacer(modifier = Modifier.padding(16.dp))
            if (busy) {
                CircularProgressIndicator(
                    color = accent().solid,
                    modifier = Modifier.size(32.dp),
                )
            } else {
                PolliPrimaryButton(
                    label = if (isDcLogin) "Log in" else "Agree and create profile",
                    onClick = { onCreate(name.trim()) },
                )
            }
        }
    }
}
