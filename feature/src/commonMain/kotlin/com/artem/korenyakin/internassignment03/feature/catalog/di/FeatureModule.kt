package com.artem.korenyakin.internassignment03.feature.catalog.di

import com.artem.korenyakin.internassignment03.feature.catalog.ProductCatalogViewModel
import com.artem.korenyakin.internassignment03.feature.catalog.domain.SearchProductsUseCase
import org.koin.dsl.module

public val featureModule = module {
    factory { SearchProductsUseCase() }
    factory { ProductCatalogViewModel(get()) }
}
