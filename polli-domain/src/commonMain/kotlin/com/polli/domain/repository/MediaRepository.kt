package com.polli.domain.repository

import com.polli.core.chat.ChatMediaFilter

interface MediaRepository {
    fun messageIdsForFilter(chatId: Int, filter: ChatMediaFilter): IntArray
}
