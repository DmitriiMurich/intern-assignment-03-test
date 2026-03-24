package com.artem.korenyakin.internassignment03.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MarketplaceBlueSoft,
    onPrimary = InkDark,
    primaryContainer = MarketplaceBlueDark,
    onPrimaryContainer = SurfaceWhite,
    secondary = MarketplaceOrangeSoft,
    onSecondary = InkDark,
    secondaryContainer = MarketplaceOrange,
    onSecondaryContainer = InkDark,
    tertiary = MarketplaceBlue,
    onTertiary = SurfaceWhite,
    background = Color(0xFF0F1320),
    onBackground = SurfaceWhite,
    surface = Color(0xFF171D2D),
    onSurface = SurfaceWhite,
    surfaceVariant = Color(0xFF20283A),
    onSurfaceVariant = Color(0xFFB5C1D8),
    outline = Color(0xFF364055),
)

private val LightColorScheme = lightColorScheme(
    primary = MarketplaceBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = MarketplaceBlueSoft,
    onPrimaryContainer = MarketplaceBlueDark,
    secondary = MarketplaceOrange,
    onSecondary = SurfaceWhite,
    secondaryContainer = MarketplaceOrangeSoft,
    onSecondaryContainer = InkDark,
    tertiary = MarketplaceBlueDark,
    onTertiary = SurfaceWhite,
    background = PageBackground,
    onBackground = InkDark,
    surface = SurfaceWhite,
    onSurface = InkDark,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkSoft,
    outline = Divider,
)

@Composable
fun Internassignment03Theme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}