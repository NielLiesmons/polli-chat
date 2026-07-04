package com.polli.domain.model

import com.polli.core.chat.ChatCategory

data class InboxItem(
    val chatId: Int,
    val name: String,
    val preview: String,
    val previewAuthor: String?,
    val updatedAt: Long,
    val unreadCount: Int,
    val category: ChatCategory,
    val profileImage: String?,
    val colorSeed: String,
)
