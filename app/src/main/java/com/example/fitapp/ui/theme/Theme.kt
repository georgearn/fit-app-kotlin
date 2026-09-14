package com.example.fitapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppAccent(
    val key: String,
    val title: String,
    val darkPrimary: Color,
    val lightPrimary: Color,
    val secondary: Color
) {
    CYAN("CYAN", "Neon Cyan", NeonCyan, LightPrimary, NeonBlue),
    EMERALD("EMERALD", "Emerald", Color(0xFF00E676), Color(0xFF00897B), Color(0xFF00B0FF)),
    ORANGE("ORANGE", "Coral Orange", Color(0xFFFF9100), Color(0xFFE65100), Color(0xFFFFD600)),
    PURPLE("PURPLE", "Electric Violet", Color(0xFFB388FF), Color(0xFF7C4DFF), Color(0xFF00E5FF)),
    BLUE("BLUE", "Royal Blue", Color(0xFF448AFF), Color(0xFF1976D2), Color(0xFF00E5FF))
}

data class FitThemeColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val secondaryAccent: Color = NeonBlue,
    val muscleHighlight: Color
)

val LocalFitTheme = staticCompositionLocalOf {
    FitThemeColors(
        isDark = true,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        surfaceElevated = DarkSurfaceElevated,
        border = DarkBorder,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textMuted = TextMuted,
        primaryAccent = NeonCyan,
        onPrimaryAccent = DarkBackground,
        secondaryAccent = NeonBlue,
        muscleHighlight = NeonCyan
    )
}

object FitTheme {
    val colors: FitThemeColors
        @Composable
        get() = LocalFitTheme.current
}

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkBackground,
    primaryContainer = NeonCyanDark.copy(alpha = 0.2f),
    onPrimaryContainer = NeonCyan,
    secondary = NeonBlue,
    onSecondary = TextPrimary,
    secondaryContainer = NeonBlue.copy(alpha = 0.2f),
    onSecondaryContainer = TextPrimary,
    tertiary = ElectricEmerald,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Color(0xFF004D56),
    secondary = Color(0xFF1976D2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightBorder.copy(alpha = 0.6f)
)

@Composable
fun FitAppTheme(
    darkTheme: Boolean = true,
    useMaterialYou: Boolean = false,
    accentKey: String = "CYAN",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val chosenAccent = AppAccent.entries.firstOrNull { it.key.equals(accentKey, ignoreCase = true) } ?: AppAccent.CYAN

    val colorScheme = when {
        useMaterialYou && isDynamicAvailable -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            DarkColorScheme.copy(
                primary = chosenAccent.darkPrimary,
                secondary = chosenAccent.secondary
            )
        }
        else -> {
            LightColorScheme.copy(
                primary = chosenAccent.lightPrimary,
                secondary = chosenAccent.secondary
            )
        }
    }

    val activePrimary = colorScheme.primary
    val activeSecondary = colorScheme.secondary

    val fitThemeColors = if (darkTheme) {
        FitThemeColors(
            isDark = true,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            surfaceElevated = DarkSurfaceElevated,
            border = DarkBorder,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            textMuted = TextMuted,
            primaryAccent = activePrimary,
            onPrimaryAccent = if (useMaterialYou && isDynamicAvailable) colorScheme.onPrimary else DarkBackground,
            secondaryAccent = activeSecondary,
            muscleHighlight = activePrimary
        )
    } else {
        FitThemeColors(
            isDark = false,
            background = LightBackground,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            surfaceElevated = LightSurfaceElevated,
            border = LightBorder,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textMuted = LightTextMuted,
            primaryAccent = activePrimary,
            onPrimaryAccent = colorScheme.onPrimary,
            secondaryAccent = activeSecondary,
            muscleHighlight = activePrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bg = if (darkTheme) DarkBackground else LightBackground
            window.statusBarColor = bg.toArgb()
            window.navigationBarColor = bg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFitTheme provides fitThemeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
