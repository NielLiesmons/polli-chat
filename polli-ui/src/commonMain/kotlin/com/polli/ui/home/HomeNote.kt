package com.polli.ui.home

data class HomeNote(
    val msgId: Int,
    val title: String,
    val preview: String,
    val body: String,
    val timestamp: Long,
    val hasAttachment: Boolean = false,
)
