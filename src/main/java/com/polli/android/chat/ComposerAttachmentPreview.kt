package com.polli.android.chat

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.polli.android.icons.PolliIcon
import com.polli.android.icons.PolliIconName
import com.polli.android.theme.PolliColors

@Composable
fun ComposerAttachmentPreview(
    attachment: PendingAttachment,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PolliColors.White16)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (attachment.isImage) {
            AndroidView(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { view ->
                    Glide.with(view).load(attachment.uri).centerCrop().into(view)
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PolliColors.White16),
                contentAlignment = Alignment.Center,
            ) {
                PolliIcon(PolliIconName.Options, 20.dp, PolliColors.White66)
            }
        }
        Text(
            text = attachment.label,
            color = PolliColors.White85,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onClear),
            contentAlignment = Alignment.Center,
        ) {
            PolliIcon(PolliIconName.Cross, 14.dp, PolliColors.White33)
        }
    }
}
