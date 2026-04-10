package com.artem.korenyakin.internassignment03.feature.catalog.data.mapper

import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogCategoryDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogCurrencyDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogLanguageDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogMoneyDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogProductDetailsDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogProductDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogReviewDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogResponseDto
import com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.Money
import com.artem.korenyakin.internassignment03.model.domain.Product
import com.artem.korenyakin.internassignment03.model.domain.ProductDetails
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogPage
import com.artem.korenyakin.internassignment03.model.domain.ProductCategory
import com.artem.korenyakin.internassignment03.model.domain.ProductReview

internal fun CatalogLanguageDto.toDomain(): CatalogLanguage = CatalogLanguage(
    code = code,
    name = name,
    isSourceLanguage = isSourceLanguage,
)

internal fun CatalogCurrencyDto.toDomain(): CurrencyOption = CurrencyOption(
    code = code,
    name = name,
    symbol = symbol,
    isSourceCurrency = isSourceCurrency,
)

internal fun CatalogCategoryDto.toDomain(): ProductCategory = ProductCategory(
    slug = slug,
    title = title,
)

internal fun CatalogProductDto.toDomain(): Product = Product(
    id = id,
    title = title,
    description = description,
    price = price.toDomain(),
    imageUrl = imageUrl,
    rating = rating,
    category = category.toDomain(),
)

internal fun CatalogReviewDto.toDomain(): ProductReview = ProductReview(
    id = id,
    rating = rating,
    comment = comment,
    date = date,
    reviewerName = reviewerName,
)

internal fun CatalogMoneyDto.toDomain(): Money {
    val currency = CurrencyOption.SupportedCurrencies.firstOrNull { supportedCurrency ->
        supportedCurrency.code == currency
    } ?: CurrencyOption(
        code = currency,
        name = currency,
        symbol = currency,
        isSourceCurrency = currency == CurrencyOption.USD.code,
    )

    return Money(
        amount = amount,
        currency = currency,
    )
}

internal fun CatalogResponseDto.toDomain(
    selectedLanguage: CatalogLanguage,
): ProductCatalogPage = ProductCatalogPage(
    language = selectedLanguage,
    categories = categories.map(CatalogCategoryDto::toDomain),
    products = items.map(CatalogProductDto::toDomain),
    totalProducts = meta.totalProducts,
    currentPage = meta.currentPage,
    pageSize = meta.pageSize,
    totalPages = meta.totalPages,
)

internal fun CatalogProductDetailsDto.toDomain(): ProductDetails = ProductDetails(
    product = product.toDomain(),
    reviews = reviews.map(CatalogReviewDto::toDomain),
)
