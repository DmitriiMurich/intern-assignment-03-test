package com.artem.korenyakin.internassignment03.feature.catalog.data.repository

import com.artem.korenyakin.internassignment03.feature.catalog.data.mapper.toDomain
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogCurrenciesResponseDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogLanguagesResponseDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogProductDetailsDto
import com.artem.korenyakin.internassignment03.feature.catalog.data.remote.dto.CatalogResponseDto
import com.artem.korenyakin.internassignment03.model.domain.CatalogLanguage
import com.artem.korenyakin.internassignment03.model.domain.CurrencyOption
import com.artem.korenyakin.internassignment03.model.domain.ProductDetails
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogPage
import com.artem.korenyakin.internassignment03.model.domain.ProductCatalogQuery
import com.artem.korenyakin.internassignment03.model.repository.ProductRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.ConnectException
import java.net.SocketTimeoutException

internal class CatalogBackendRepository(
    private val httpClient: HttpClient,
) : ProductRepository {
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getLanguages(): List<CatalogLanguage> {
        return getDecoded<CatalogLanguagesResponseDto>("api/v1/languages")
            .items
            .map { languageDto -> languageDto.toDomain() }
    }

    override suspend fun getCurrencies(): List<CurrencyOption> {
        return getDecoded<CatalogCurrenciesResponseDto>("api/v1/currencies")
            .items
            .map { currencyDto -> currencyDto.toDomain() }
    }

    override suspend fun getCatalog(
        query: ProductCatalogQuery,
    ): ProductCatalogPage {
        val selectedLanguage = CatalogLanguage(
            code = query.languageCode,
            name = query.languageCode.uppercase(),
            isSourceLanguage = query.languageCode == CatalogLanguage.ENGLISH.code,
        )

        return getDecoded<CatalogResponseDto>("api/v1/catalog") {
            parameter("lang", query.languageCode)
            parameter("currency", query.currencyCode)
            parameter("page", query.page)
            parameter("pageSize", query.pageSize)
            parameter("query", query.searchQuery)
            parameter("sort", query.sortOption.apiValue)
            query.categorySlug?.let { categorySlug ->
                parameter("category", categorySlug)
            }
        }
            .toDomain(selectedLanguage = selectedLanguage)
    }

    override suspend fun getProductDetails(
        productId: String,
        languageCode: String,
        currencyCode: String,
    ): ProductDetails {
        return getDecoded<CatalogProductDetailsDto>("api/v1/catalog/$productId") {
            parameter("lang", languageCode)
            parameter("currency", currencyCode)
        }.toDomain()
    }

    private suspend inline fun <reified T> getDecoded(
        path: String,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = executeRequest(path = path, block = block)

        if (response.status.isSuccess()) {
            return response.body()
        }

        val errorBody = response.bodyAsText()
        val backendMessage = runCatching {
            json.decodeFromString<ApiErrorDto>(errorBody).message
        }.getOrNull()

        throw IllegalStateException(
            backendMessage?.takeIf { message -> message.isNotBlank() }
                ?: "Request failed with status ${response.status.value}",
        )
    }

    private suspend fun executeRequest(
        path: String,
        block: HttpRequestBuilder.() -> Unit,
    ) = try {
        httpClient.get(path, block)
    } catch (_: ConnectException) {
        throwCatalogConnectionException()
    } catch (_: SocketTimeoutException) {
        throwCatalogConnectionException()
    } catch (_: UnresolvedAddressException) {
        throwCatalogConnectionException()
    }

    private fun throwCatalogConnectionException(): Nothing = throw CatalogConnectionException()
}

@Serializable
internal data class ApiErrorDto(
    val statusCode: Int,
    val error: String,
    val message: String,
)
