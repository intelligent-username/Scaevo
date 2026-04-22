package com.scaevo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GithubBlue = Color(0xFF58A6FF)
private val GithubDarkBg = Color(0xFF0D1117)
private val GithubDarkSurface = Color(0xFF161B22)
private val GithubDarkSurfaceVariant = Color(0xFF21262D)
private val GithubDarkText = Color(0xFFC9D1D9)
private val GithubDarkMuted = Color(0xFF8B949E)
private val GithubDarkBorder = Color(0xFF30363D)
private val GithubLightBg = Color(0xFFFFFFFF)
private val GithubLightSurface = Color(0xFFF6F8FA)
private val GithubLightSurfaceVariant = Color(0xFFEAEEF2)
private val GithubLightText = Color(0xFF24292F)
private val GithubLightMuted = Color(0xFF57606A)
private val GithubLightBorder = Color(0xFFD0D7DE)

private val DarkColorScheme = darkColorScheme(
    primary = GithubBlue,
    onPrimary = Color(0xFF0D1117),
    primaryContainer = Color(0xFF1F6FEB),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF7D8590),
    onSecondary = Color.White,
    background = GithubDarkBg,
    onBackground = GithubDarkText,
    surface = GithubDarkSurface,
    onSurface = GithubDarkText,
    surfaceVariant = GithubDarkSurfaceVariant,
    onSurfaceVariant = GithubDarkMuted,
    outline = GithubDarkBorder,
    error = Color(0xFFF85149),
    onError = Color(0xFF0D1117)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF4FF),
    onPrimaryContainer = Color(0xFF0550AE),
    secondary = Color(0xFF57606A),
    onSecondary = Color.White,
    background = GithubLightBg,
    onBackground = GithubLightText,
    surface = GithubLightSurface,
    onSurface = GithubLightText,
    surfaceVariant = GithubLightSurfaceVariant,
    onSurfaceVariant = GithubLightMuted,
    outline = GithubLightBorder,
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
