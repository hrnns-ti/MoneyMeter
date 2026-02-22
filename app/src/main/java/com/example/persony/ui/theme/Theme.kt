package com.example.persony.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Konfigurasi Dark Mode (Deep Navy/GitHub Dark Style)
private val DarkColorScheme = darkColorScheme(
    primary = MainPurple,
    secondary = LightPurple,
    tertiary = SuccessGreen,
    error = ErrorRed,
    background = Color(0xFF0D1117), // Latar belakang gelap
    surface = Color(0xFF161B22),    // Warna kartu saat dark mode
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

// Konfigurasi Light Mode (Sesuai desain gambar)
private val LightColorScheme = lightColorScheme(
    primary = MainPurple,
    secondary = DarkBlue,
    tertiary = SuccessGreen,
    error = ErrorRed,
    background = GitHubBg,
    surface = GitHubWhite,
    onPrimary = GitHubWhite,
    onBackground = GitHubTextPrimary,
    onSurface = GitHubTextPrimary,
)

@Composable
fun PersonyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor ke false agar warna ungu kustom kita tidak tertimpa warna sistem
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}