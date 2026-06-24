package com.polli.domain.repository

interface SettingsRepository {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)
}
