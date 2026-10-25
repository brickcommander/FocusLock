package dev.focuslock.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Neutral800,
    onPrimaryContainer = Indigo,
    secondary = Neutral400,
    onSecondary = Neutral950,
    background = Neutral950,
    onBackground = Neutral100,
    surface = Neutral900,
    onSurface = Neutral100,
    surfaceVariant = Neutral850,
    onSurfaceVariant = Neutral400,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = ErrorRed,
    outline = Neutral700,
    outlineVariant = Neutral800,
    inverseSurface = Neutral100,
    inverseOnSurface = Neutral900,
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoMuted,
    onPrimary = Color.White,
    primaryContainer = Neutral100,
    onPrimaryContainer = IndigoMuted,
    secondary = Neutral500,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Neutral900,
    surface = Neutral50,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral500,
    error = StatusLockedMuted,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = StatusLockedMuted,
    outline = Neutral200,
    outlineVariant = Neutral100,
    inverseSurface = Neutral900,
    inverseOnSurface = Neutral100,
)

@Composable
fun FocusLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
