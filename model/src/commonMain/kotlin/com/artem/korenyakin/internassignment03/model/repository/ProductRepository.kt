package com.artem.korenyakin.internassignment03.model.repository

import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogPage
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogQuery
import com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption

interface ProductRepository {
    suspend fun getLanguages(): List<CatalogLanguage>
    suspend fun getCurrencies(): List<CurrencyOption>

    suspend fun getCatalog(
        query: ProductCatalogQuery,
    ): ProductCatalogPage
}
