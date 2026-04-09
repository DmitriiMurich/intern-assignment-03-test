package com.artem.korenyakin.internassignment03.model.repository

interface AppLanguageManager {
    fun getCurrentLanguageCode(): String

    fun updateLanguage(languageCode: String)

    fun applyCurrentLanguage()
}
