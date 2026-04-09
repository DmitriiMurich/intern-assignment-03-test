package com.artem.korenyakin.internassignment03

import android.app.Application
import com.artem.korenyakin.internassignment03.di.appModule
import com.artem.korenyakin.internassignment03.feature.catalog.di.featureModule
import com.artem.korenyakin.internassignment03.locale.AndroidAppLanguageManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class InternAssignmentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appLanguageManager = AndroidAppLanguageManager(this)
        appLanguageManager.applyCurrentLanguage()

        startKoin {
            androidContext(this@InternAssignmentApp)
            modules(
                appModule(appLanguageManager),
                featureModule,
            )
        }
    }
}
