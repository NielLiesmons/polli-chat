package com.polli.domain.repository

interface AccountRepository {
    val isConfigured: Boolean
    val selectedAccountId: Int
}
