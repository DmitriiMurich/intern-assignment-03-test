package com.artem.korenyakin.internassignment03

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.artem.korenyakin.internassignment03.feature.catalog.di.featureModule

class InternAssignmentApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@InternAssignmentApp)
            modules(featureModule)
        }
    }
}
