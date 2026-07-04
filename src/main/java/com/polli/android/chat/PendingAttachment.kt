package com.polli.android.chat

import android.net.Uri

/** Attachment staged in the composer before send — mirrors legacy slide-deck draft flow. */
data class PendingAttachment(
    val uri: Uri,
    val mimeType: String,
    val label: String,
    val isImage: Boolean = false,
)
