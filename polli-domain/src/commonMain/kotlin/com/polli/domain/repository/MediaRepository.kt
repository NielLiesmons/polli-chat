package com.polli.domain.repository

interface MediaRepository {
    fun sendMedia(chatId: Int, uri: String, mimeType: String?)
}
