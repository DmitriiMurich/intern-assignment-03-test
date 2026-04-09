package com.artem.korenyakin.internassignment03.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.artem.korenyakin.internassignment03.model.repository.AppLanguageManager
import java.util.Locale

internal class AndroidAppLanguageManager(
    context: Context,
) : AppLanguageManager {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE,
    )

    override fun getCurrentLanguageCode(): String {
        val savedLanguageCode = preferences.getString(LanguageCodeKey, null)
        if (!savedLanguageCode.isNullOrBlank()) {
            return normalizeLanguageCode(savedLanguageCode)
        }

        val appLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (appLocales.isNotBlank()) {
            return normalizeLanguageCode(appLocales.substringBefore(','))
        }

        val systemLanguage = applicationContext.resources.configuration.locales[0]?.language
            ?: Locale.getDefault().language
        return normalizeLanguageCode(systemLanguage)
    }

    override fun updateLanguage(languageCode: String) {
        val normalizedLanguageCode = normalizeLanguageCode(languageCode)
        preferences.edit()
            .putString(LanguageCodeKey, normalizedLanguageCode)
            .apply()
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalizedLanguageCode),
        )
    }

    override fun applyCurrentLanguage() {
        val savedLanguageCode = preferences.getString(LanguageCodeKey, null)
            ?.takeIf { languageCode -> languageCode.isNotBlank() }
            ?: return

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalizeLanguageCode(savedLanguageCode)),
        )
    }

    private fun normalizeLanguageCode(languageCode: String): String {
        return languageCode.substringBefore('-').lowercase(Locale.ROOT)
    }

    private companion object {
        private const val PreferencesName: String = "app_language_preferences"
        private const val LanguageCodeKey: String = "language_code"
    }
}
