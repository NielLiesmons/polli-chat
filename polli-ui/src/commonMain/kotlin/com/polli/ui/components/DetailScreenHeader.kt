package com.polli.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.polli.ui.theme.PolliColors

@Composable
fun DetailScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    backButton: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (backButton != null) {
            backButton()
        }
        Text(
            text = title,
            color = PolliColors.White85,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = if (backButton != null) 12.dp else 0.dp),
        )
    }
}
