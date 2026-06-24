package com.polli.domain.repository

interface ChatRepository {
    fun observeInbox(onUpdate: () -> Unit): AutoCloseable
    fun getFreshMessageCount(chatId: Int): Int
}
