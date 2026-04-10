package com.artem.korenyakin.internassignment03.model.repository

import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogPage
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogQuery
import com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.ProductDetails

interface ProductRepository {
    suspend fun getLanguages(): List<CatalogLanguage>
    suspend fun getCurrencies(): List<CurrencyOption>

    suspend fun getCatalog(
        query: ProductCatalogQuery,
    ): ProductCatalogPage

    suspend fun getProductDetails(
        productId: String,
        languageCode: String,
        currencyCode: String,
    ): ProductDetails
}
