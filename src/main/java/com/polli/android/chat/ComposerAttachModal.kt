package com.polli.android.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.ui.AppModal
import com.polli.android.ui.ModalSectionLabel
import dev.chrisbanes.haze.HazeState

@Composable
fun ComposerAttachModal(
    onClose: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onBrowse: () -> Unit,
    onVideo: () -> Unit = {},
    onContact: () -> Unit = {},
    onLocation: () -> Unit = {},
    hazeState: HazeState? = null,
) {
    AppModal(onDismiss = onClose, hazeState = hazeState) {
        ModalSectionLabel("Share")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AttachActionRow(icon = LabIconName.Camera, label = "Camera", onClick = onCamera)
            AttachActionRow(icon = LabIconName.Plus, label = "Gallery", onClick = onGallery)
            AttachActionRow(icon = LabIconName.Options, label = "File", onClick = onBrowse)
            AttachActionRow(icon = LabIconName.Play, label = "Video", onClick = onVideo)
            AttachActionRow(icon = LabIconName.EmojiFill, label = "Contact", onClick = onContact)
            AttachActionRow(icon = LabIconName.ArrowUp, label = "Location", onClick = onLocation)
        }
    }
}

@Composable
private fun AttachActionRow(
    icon: LabIconName,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LabColors.Gray33)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LabIcon(icon, 18.dp, LabColors.White66)
        Text(label, color = LabColors.White85, fontSize = 15.sp)
    }
}
