package com.artem.korenyakin.internassignment03.model.domain

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String,
    val isSourceCurrency: Boolean,
) {
    companion object {
        val USD: CurrencyOption = CurrencyOption(
            code = "USD",
            name = "US Dollar",
            symbol = "$",
            isSourceCurrency = true,
        )

        val SupportedCurrencies: List<CurrencyOption> = listOf(
            USD,
            CurrencyOption(code = "EUR", name = "Euro", symbol = "\u20AC", isSourceCurrency = false),
            CurrencyOption(code = "RUB", name = "Russian Ruble", symbol = "\u20BD", isSourceCurrency = false),
            CurrencyOption(code = "GBP", name = "British Pound", symbol = "\u00A3", isSourceCurrency = false),
            CurrencyOption(code = "UAH", name = "Ukrainian Hryvnia", symbol = "\u20B4", isSourceCurrency = false),
            CurrencyOption(code = "TRY", name = "Turkish Lira", symbol = "\u20BA", isSourceCurrency = false),
            CurrencyOption(code = "CNY", name = "Chinese Yuan", symbol = "\u00A5", isSourceCurrency = false),
            CurrencyOption(code = "JPY", name = "Japanese Yen", symbol = "\u00A5", isSourceCurrency = false),
            CurrencyOption(code = "CAD", name = "Canadian Dollar", symbol = "C$", isSourceCurrency = false),
            CurrencyOption(code = "CHF", name = "Swiss Franc", symbol = "CHF", isSourceCurrency = false),
        )
    }
}
