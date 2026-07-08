package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF9800),       // Vibrant Amber
    onPrimary = Color(0xFF0F0F12),
    primaryContainer = Color(0x26FF9800),
    secondary = Color(0xFFFF5722),     // Deep Orange Accent
    onSecondary = Color.White,
    tertiary = Color(0xFFFFD54F),      // Soft Gold
    background = Color(0xFF000000),    // Sleek Pure Black OLED Canvas
    surface = Color(0xFF0D0D12),       // Premium Card Dark
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF16161C),
    onSurfaceVariant = Color(0xFFC4C4C4)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFF57C00),       // Darker Amber for Accessibility Contrast
    onPrimary = Color.White,
    primaryContainer = Color(0x1AFF9800),
    secondary = Color(0xFFE64A19),     // Deep Orange Accent
    onSecondary = Color.White,
    tertiary = Color(0xFFFFB300),      // Premium Gold
    background = Color(0xFFF8F9FC),    // Crisp off-white canvas
    surface = Color(0xFFFFFFFF),       // Pure White Card
    onBackground = Color(0xFF1E1E24),
    onSurface = Color(0xFF1E1E24),
    surfaceVariant = Color(0xFFF1F1F5),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor option, but default to false to prioritize our customized premium brand theme
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

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

