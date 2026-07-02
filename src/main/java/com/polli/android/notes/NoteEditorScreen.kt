package com.polli.android.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import com.polli.android.ui.ShellDivider

@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    BackHandler {
        if (viewModel.hasChanges) viewModel.save()
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LabColors.Black)
            .imePadding(),
    ) {
        NoteEditorTopBar(
            title = if (viewModel.isNew) "New note" else "Note",
            showPreview = viewModel.showPreview,
            canDelete = !viewModel.isNew,
            onBack = {
                if (viewModel.hasChanges) viewModel.save()
                onBack()
            },
            onTogglePreview = viewModel::togglePreview,
            onDelete = {
                viewModel.delete()
                onDeleted()
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LabDimens.HomeBarPadding)
                .padding(bottom = AppInsets.navigationBarBottom() + 16.dp),
        ) {
            BasicTextField(
                value = viewModel.body,
                onValueChange = viewModel::updateBody,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                textStyle = TextStyle(
                    color = LabColors.White85,
                    fontFamily = FontFamily.Monospace,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                ),
                cursorBrush = SolidColor(LabColors.White),
                decorationBox = { inner ->
                    Box {
                        if (viewModel.body.isEmpty()) {
                            Text(
                                "# Title\n\nWrite markdown here…",
                                color = LabColors.White16,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        inner()
                    }
                },
            )

            if (viewModel.showPreview && viewModel.body.isNotBlank()) {
                ShellDivider(screenPad = 0.dp)
                Text(
                    "Preview",
                    color = LabColors.White33,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LabColors.Gray33)
                        .padding(14.dp),
                ) {
                    NoteMarkdownPreview(markdown = viewModel.body)
                }
            }
        }
    }
}

@Composable
private fun NoteEditorTopBar(
    title: String,
    showPreview: Boolean,
    canDelete: Boolean,
    onBack: () -> Unit,
    onTogglePreview: () -> Unit,
    onDelete: () -> Unit,
) {
    val top = AppInsets.statusBarTop() + 6.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = top, start = LabDimens.HomeBarPadding, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundBackButton(onClick = onBack)
        Text(
            text = title,
            color = LabColors.White85,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        TextButton(onClick = onTogglePreview) {
            Text(
                if (showPreview) "Hide preview" else "Preview",
                color = LabColors.White66,
            )
        }
        if (canDelete) {
            TextButton(onClick = onDelete) {
                Text("Delete", color = LabColors.White33)
            }
        }
    }
}
