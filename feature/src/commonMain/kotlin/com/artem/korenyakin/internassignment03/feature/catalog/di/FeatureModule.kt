package com.artem.korenyakin.internassignment03.feature.catalog.di

import com.artem.korenyakin.internassignment03.feature.catalog.ProductCatalogViewModel
import com.artem.korenyakin.internassignment03.feature.catalog.data.repository.DummyJsonProductRepository
import com.artem.korenyakin.internassignment03.feature.catalog.domain.SearchProductsUseCase
import com.artem.korenyakin.internassignment03.model.repository.ProductRepository
import org.koin.dsl.module

val featureModule = module {
    single<ProductRepository> { DummyJsonProductRepository(get()) }
    factory { SearchProductsUseCase() }
    factory { ProductCatalogViewModel(get(), get()) }
}
