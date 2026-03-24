package com.artem.korenyakin.internassignment03.feature.catalog.data.repository

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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class DummyJsonProductRepositoryTest {
    @Test
    fun shouldRequestProductsUsingPageAndPageSize() = runTest {
        val httpClient = createHttpClient { request ->
            assertEquals("/products", request.url.encodedPath)
            assertEquals("10", request.url.parameters["limit"])
            assertEquals("20", request.url.parameters["skip"])

            respondJson(
                body = """
                    {
                      "products": [
                        {
                          "id": 1,
                          "title": "Phone Case",
                          "description": "Protective case",
                          "category": "mobile-accessories",
                          "price": 12.5,
                          "rating": 4.7,
                          "images": ["https://example.com/image-1.png"],
                          "thumbnail": "https://example.com/thumb.png"
                        }
                      ],
                      "total": 1,
                      "skip": 20,
                      "limit": 10
                    }
                """.trimIndent(),
            )
        }
        val repository = DummyJsonProductRepository(httpClient)

        val products = repository.getProducts(
            page = 2,
            pageSize = 10,
        )

        assertEquals(1, products.size)
        assertEquals("Phone Case", products.first().title)
        assertEquals("https://example.com/thumb.png", products.first().imageUrl)
    }

    @Test
    fun shouldClampNegativePageToZero() = runTest {
        val httpClient = createHttpClient { request ->
            assertEquals("0", request.url.parameters["skip"])
            respondJson(
                body = """
                    {
                      "products": [],
                      "total": 0,
                      "skip": 0,
                      "limit": 5
                    }
                """.trimIndent(),
            )
        }
        val repository = DummyJsonProductRepository(httpClient)

        val products = repository.getProducts(
            page = -5,
            pageSize = 5,
        )

        assertTrue(products.isEmpty())
    }

    @Test
    fun shouldReturnEmptyProductsWhenPageSizeIsNotPositive() = runTest {
        var requestCount = 0
        val httpClient = createHttpClient {
            requestCount += 1
            respondJson(
                body = """
                    {
                      "products": [],
                      "total": 0,
                      "skip": 0,
                      "limit": 0
                    }
                """.trimIndent(),
            )
        }
        val repository = DummyJsonProductRepository(httpClient)

        val products = repository.getProducts(
            page = 0,
            pageSize = 0,
        )

        assertTrue(products.isEmpty())
        assertEquals(0, requestCount)
    }

    @Test
    fun shouldReturnCategoriesWithAllOptionFirst() = runTest {
        val httpClient = createHttpClient { request ->
            assertEquals("/products/category-list", request.url.encodedPath)
            respondJson(
                body = """
                    ["beauty", "mobile-accessories", "home-decoration"]
                """.trimIndent(),
            )
        }
        val repository = DummyJsonProductRepository(httpClient)

        val categories = repository.getCategories()

        assertEquals("All", categories.first().title)
        assertEquals(
            listOf("All", "Beauty", "Mobile Accessories", "Home Decoration"),
            categories.map { category -> category.title },
        )
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
