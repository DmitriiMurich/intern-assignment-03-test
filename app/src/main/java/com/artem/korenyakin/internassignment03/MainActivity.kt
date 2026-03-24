package com.artem.korenyakin.internassignment03

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.artem.korenyakin.internassignment03.feature.catalog.ProductCatalogScreen
import com.artem.korenyakin.internassignment03.ui.theme.Internassignment03Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Internassignment03Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    ProductCatalogScreen()
                }
            }
        }
    }
}
