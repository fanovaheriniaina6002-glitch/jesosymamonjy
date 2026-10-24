package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val LocalIsDarkMode = compositionLocalOf { false }
val LocalThemeMode = compositionLocalOf { "jour" }

/**
 * 0ms Instant Solid Bento background brush:
 * Light: #FAF8F9 to #F6E9EC
 * Dark: Gris-Grenat profond #120A0B
 * Zero color animation (0 ms).
 */
@Composable
fun rememberSolidBentoBrush(isDark: Boolean): Brush {
    val startColor = if (isDark) DarkBackgroundPure else LightBackgroundStart
    val endColor = if (isDark) DarkBackgroundDeep else LightBackgroundEnd
    return Brush.verticalGradient(listOf(startColor, endColor))
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkTextAccent,            // #D4AF37 (Or mat — icônes actives & onglet sélectionné)
    onPrimary = Color(0xFF120A0B),
    primaryContainer = Color(0xFF3A161E),
    onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = DarkSurfaceSecondary,
    onSecondary = DarkTextPrimary,
    background = DarkBackgroundDeep,     // #120A0B (Gris-Grenat extrêmement profond)
    onBackground = DarkTextPrimary,      // #F5E6E8 (Blanc Cassé très doux)
    surface = DarkSurface,               // #1E1214 (Gris-Rouge chaud et élégant — Cartes, TopBar, NavBar)
    surfaceVariant = DarkSurfaceSecondary, // #28181B (Surface secondaire distincte pour les badges)
    onSurface = DarkTextPrimary,         // #F5E6E8 (Blanc Cassé très doux)
    onSurfaceVariant = DarkTextPrimary,  // #F5E6E8 (Parfaitement lisible sur surfaceVariant en Mode Nuit)
    outline = DarkBorder,                // #382528 (Bordures 1.dp)
    outlineVariant = DarkBorder          // #382528
)

private val LightColorScheme = lightColorScheme(
    primary = LightTextAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF9E8EA),
    onPrimaryContainer = Color(0xFF781C2E),
    secondary = LightSurfaceSecondary,
    onSecondary = LightTextPrimary,
    background = LightBackgroundStart,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    surfaceVariant = Color(0xFFF5E6E8),
    onSurface = LightTextPrimary,
    onSurfaceVariant = Color(0xFF564243),
    outline = LightBorder,
    outlineVariant = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: String = if (darkTheme) "nuit" else "jour",
    content: @Composable () -> Unit
) {
    // 0ms Instant color switch (Zero animation)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalIsDarkMode provides darkTheme,
        LocalThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
