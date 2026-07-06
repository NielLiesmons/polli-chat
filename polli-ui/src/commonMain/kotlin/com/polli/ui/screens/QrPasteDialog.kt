package com.polli.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polli.ui.components.PolliGhostButton
import com.polli.ui.theme.PolliColors

@Composable
fun QrPasteDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    hint: String = "Paste QR code content",
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = PolliColors.White85) },
        text = {
            Column {
                Text(
                    hint,
                    color = PolliColors.White33,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            PolliGhostButton(
                label = "Continue",
                onClick = { onSubmit(text.trim()) },
                color = if (text.isNotBlank()) PolliColors.White85 else PolliColors.White16,
            )
        },
        dismissButton = {
            PolliGhostButton(label = "Cancel", onClick = onDismiss)
        },
    )
}
