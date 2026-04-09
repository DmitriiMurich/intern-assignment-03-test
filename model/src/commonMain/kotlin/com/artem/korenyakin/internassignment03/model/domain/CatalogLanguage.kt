package com.artem.korenyakin.internassignment03.model.domain

data class CatalogLanguage(
    val code: String,
    val name: String,
    val isSourceLanguage: Boolean,
) {
    companion object {
        val ENGLISH: CatalogLanguage = CatalogLanguage(
            code = "en",
            name = "English",
            isSourceLanguage = true,
        )

        val SupportedLanguages: List<CatalogLanguage> = listOf(
            ENGLISH,
            CatalogLanguage(code = "ru", name = "Russian", isSourceLanguage = false),
            CatalogLanguage(code = "de", name = "German", isSourceLanguage = false),
            CatalogLanguage(code = "fr", name = "French", isSourceLanguage = false),
            CatalogLanguage(code = "es", name = "Spanish", isSourceLanguage = false),
            CatalogLanguage(code = "it", name = "Italian", isSourceLanguage = false),
            CatalogLanguage(code = "pt", name = "Portuguese", isSourceLanguage = false),
            CatalogLanguage(code = "tr", name = "Turkish", isSourceLanguage = false),
            CatalogLanguage(code = "uk", name = "Ukrainian", isSourceLanguage = false),
            CatalogLanguage(code = "zh", name = "Chinese", isSourceLanguage = false),
        )
    }
}
