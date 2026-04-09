package com.artem.korenyakin.internassignment03.feature.catalog.di

import com.artem.korenyakin.internassignment03.feature.catalog.ProductCatalogViewModel
import com.artem.korenyakin.internassignment03.feature.catalog.data.repository.CatalogBackendRepository
import com.artem.korenyakin.internassignment03.model.repository.ProductRepository
import org.koin.dsl.module

val featureModule = module {
    single<ProductRepository> { CatalogBackendRepository(get()) }
    factory { ProductCatalogViewModel(get(), get()) }
}
