package com.artem.korenyakin.internassignment03.feature.catalog.data.repository

import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogQuery
import com.artem.korenyakin.internassignment03.model.domain.SortOption
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class CatalogBackendRepositoryTest {
    @Test
    fun shouldRequestLanguagesFromBackend() = runTest {
        val httpClient = createHttpClient { request ->
            assertEquals("/api/v1/languages", request.url.encodedPath)
            respondJson(
                body = """
                    {
                      "items": [
                        { "code": "en", "name": "English", "isSourceLanguage": true },
                        { "code": "ru", "name": "Russian", "isSourceLanguage": false }
                      ]
                    }
                """.trimIndent(),
            )
        }
        val repository = CatalogBackendRepository(httpClient)

        val languages = repository.getLanguages()

        assertEquals(2, languages.size)
        assertEquals("en", languages.first().code)
        assertEquals("Russian", languages.last().name)
    }

    @Test
    fun shouldRequestCatalogWithBackendQueryParameters() = runTest {
        val httpClient = createHttpClient { request ->
            assertEquals("/api/v1/catalog", request.url.encodedPath)
            assertEquals("ru", request.url.parameters["lang"])
            assertEquals("EUR", request.url.parameters["currency"])
            assertEquals("2", request.url.parameters["page"])
            assertEquals("10", request.url.parameters["pageSize"])
            assertEquals("phone", request.url.parameters["query"])
            assertEquals("smartphones", request.url.parameters["category"])
            assertEquals("price_desc", request.url.parameters["sort"])

            respondJson(
                body = """
                    {
                      "language": "ru",
                      "currency": "EUR",
                      "categories": [
                        { "slug": "smartphones", "title": "Smartphones" }
                      ],
                      "items": [
                        {
                          "id": "1",
                          "title": "Телефон",
                          "description": "Описание",
                          "price": {
                            "amount": 999.0,
                            "currency": "EUR"
                          },
                          "rating": 4.8,
                          "imageUrl": "https://example.com/phone.png",
                          "category": {
                            "slug": "smartphones",
                            "title": "Smartphones"
                          }
                        }
                      ],
                      "meta": {
                        "totalProducts": 12,
                        "totalCategories": 1,
                        "currentPage": 2,
                        "pageSize": 10,
                        "totalPages": 2,
                        "query": "phone",
                        "category": "smartphones",
                        "sort": "price_desc",
                        "sourceLanguage": "en",
                        "sourceCurrency": "USD",
                        "exchangeRateProvider": "frankfurter"
                      }
                    }
                """.trimIndent(),
            )
        }
        val repository = CatalogBackendRepository(httpClient)

        val page = repository.getCatalog(
            ProductCatalogQuery(
                page = 2,
                pageSize = 10,
                searchQuery = "phone",
                categorySlug = "smartphones",
                sortOption = SortOption.PRICE_DESC,
                languageCode = "ru",
                currencyCode = "EUR",
            ),
        )

        assertEquals(1, page.products.size)
        assertEquals("Телефон", page.products.first().title)
        assertEquals("EUR", page.products.first().price.currency.code)
        assertEquals(12, page.totalProducts)
        assertEquals(2, page.currentPage)
        assertEquals(2, page.totalPages)
        assertTrue(page.categories.isNotEmpty())
    }

    @Test
    fun shouldRequestProductDetailsWithLocalizedReviews() = runTest {
        val httpClient = createHttpClient { request ->
            assertEquals("/api/v1/catalog/10", request.url.encodedPath)
            assertEquals("ru", request.url.parameters["lang"])
            assertEquals("EUR", request.url.parameters["currency"])

            respondJson(
                body = """
                    {
                      "language": "ru",
                      "currency": "EUR",
                      "product": {
                        "id": "10",
                        "title": "Night Serum",
                        "description": "Localized description",
                        "price": {
                          "amount": 88.5,
                          "currency": "EUR"
                        },
                        "rating": 4.9,
                        "imageUrl": "https://example.com/product.png",
                        "category": {
                          "slug": "beauty",
                          "title": "Beauty"
                        }
                      },
                      "reviews": [
                        {
                          "id": "review-1",
                          "rating": 5.0,
                          "comment": "Localized review comment",
                          "date": "2026-04-10T10:15:00.000Z",
                          "reviewerName": "Emma Wilson"
                        }
                      ],
                      "meta": {
                        "sourceLanguage": "en",
                        "sourceCurrency": "USD",
                        "exchangeRateProvider": "frankfurter"
                      }
                    }
                """.trimIndent(),
            )
        }
        val repository = CatalogBackendRepository(httpClient)

        val details = repository.getProductDetails(
            productId = "10",
            languageCode = "ru",
            currencyCode = "EUR",
        )

        assertEquals("10", details.product.id)
        assertEquals("Night Serum", details.product.title)
        assertEquals("EUR", details.product.price.currency.code)
        assertEquals(1, details.reviews.size)
        assertEquals("Localized review comment", details.reviews.first().comment)
    }

    @Test
    fun shouldExposeBackendErrorMessageWhenCatalogRequestFails() = runTest {
        val httpClient = createHttpClient {
            respond(
                content = """
                    {
                      "statusCode": 502,
                      "error": "YANDEX_TRANSLATION_REQUEST_FAILED",
                      "message": "Yandex access denied for the configured folder"
                    }
                """.trimIndent(),
                status = HttpStatusCode.BadGateway,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString(),
                ),
            )
        }
        val repository = CatalogBackendRepository(httpClient)

        val error = assertFailsWith<IllegalStateException> {
            repository.getCatalog(
                ProductCatalogQuery(
                    page = 1,
                    pageSize = 20,
                    searchQuery = "",
                    categorySlug = null,
                    sortOption = SortOption.PRICE_ASC,
                    languageCode = "ru",
                    currencyCode = "USD",
                ),
            )
        }

        assertEquals("Yandex access denied for the configured folder", error.message)
    }

    @Test
    fun shouldNormalizeConnectionFailureMessage() = runTest {
        val httpClient = createHttpClient {
            throw UnresolvedAddressException()
        }
        val repository = CatalogBackendRepository(httpClient)

        val error = assertFailsWith<CatalogConnectionException> {
            repository.getCatalog(
                ProductCatalogQuery(
                    page = 1,
                    pageSize = 20,
                    searchQuery = "",
                    categorySlug = null,
                    sortOption = SortOption.PRICE_ASC,
                    languageCode = "en",
                    currencyCode = "USD",
                ),
            )
        }

        assertEquals(null, error.message)
    }

    private fun createHttpClient(
        handler: MockRequestHandler,
    ): HttpClient = HttpClient(
        MockEngine(handler),
    ) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
    }

    private fun MockRequestHandleScope.respondJson(
        body: String,
    ) = respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = headersOf(
            HttpHeaders.ContentType,
            ContentType.Application.Json.toString(),
        ),
    )
}
