package com.artem.korenyakin.internassignment03.di

import com.artem.korenyakin.internassignment03.BuildConfig
import com.artem.korenyakin.internassignment03.model.repository.AppLanguageManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

internal fun appModule(
    appLanguageManager: AppLanguageManager,
) = module {
    single<AppLanguageManager> { appLanguageManager }
    single {
        HttpClient(Android) {
            defaultRequest {
                url(BuildConfig.BACKEND_BASE_URL)
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }
    }
}
