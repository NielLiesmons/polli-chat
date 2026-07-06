package com.polli.ui.home

import com.polli.ui.components.polliClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.polli.ui.components.LabIcon
import com.polli.ui.components.LabIconName
import com.polli.ui.components.ShellDivider
import com.polli.ui.theme.AppInsets
import com.polli.ui.theme.LabColors
import com.polli.ui.theme.LabDimens
import java.text.DateFormat
import java.util.Date

private val NoteRowHeight = 72.dp
private const val PreviewPlaceholder = "No additional text"

@Composable
fun NotesTab(
    notes: List<HomeNote>,
    onNewNote: () -> Unit,
    onOpenNote: (Int) -> Unit,
) {
    val bottomPad = AppInsets.navigationBarBottom() + LabDimens.TabContentBottomPad

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPad),
    ) {
        item(key = "new-note-cta") {
            NewNoteCta(onClick = onNewNote)
            ShellDivider(screenPad = 0.dp)
        }

        if (notes.isEmpty()) {
            item(key = "notes-empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LabDimens.HomeBarPadding, vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No notes yet.\nSaved messages and new notes appear here.",
                        color = LabColors.White33,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            items(notes, key = { it.msgId }) { note ->
                NoteListRow(note = note, onClick = { onOpenNote(note.msgId) })
                ShellDivider(screenPad = 0.dp)
            }
        }
    }
}

@Composable
private fun NewNoteCta(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .polliClickable(onClick = onClick)
            .padding(horizontal = LabDimens.HomeBarPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LabIcon(LabIconName.Plus, 16.dp, LabColors.White33)
        Text(
            text = "New note",
            color = LabColors.White33,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun NoteListRow(note: HomeNote, onClick: () -> Unit) {
    val dateLabel = rememberDateLabel(note.timestamp)
    val previewText = when {
        note.preview.isNotBlank() -> note.preview
        note.hasAttachment -> "Attachment"
        else -> PreviewPlaceholder
    }
    val previewColor = if (
        note.preview.isNotBlank() || note.hasAttachment
    ) {
        LabColors.White33
    } else {
        LabColors.White16
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(NoteRowHeight)
            .polliClickable(onClick = onClick)
            .padding(horizontal = LabDimens.HomeBarPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = note.title,
                color = LabColors.White85,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (dateLabel.isNotBlank()) {
                Text(
                    text = dateLabel,
                    color = LabColors.White16,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        Text(
            text = previewText,
            color = previewColor,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun rememberDateLabel(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val millis = if (timestamp > 1_000_000_000_000L) timestamp else timestamp * 1000
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
}
