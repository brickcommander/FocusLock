package dev.focuslock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SageLight,
    onPrimary = Graphite,
    primaryContainer = SageTintDark,
    onPrimaryContainer = SageLight,
    secondary = ChalkMuted,
    onSecondary = Graphite,
    background = Graphite,
    onBackground = Chalk,
    surface = GraphiteRaised,
    onSurface = Chalk,
    surfaceVariant = GraphiteSunken,
    onSurfaceVariant = ChalkMuted,
    surfaceContainerHigh = GraphiteRaised,
    surfaceContainerHighest = GraphiteSunken,
    error = ClayLight,
    onError = Graphite,
    errorContainer = ClayTintDark,
    onErrorContainer = ClayLight,
    outline = ChalkFaint,
    outlineVariant = GraphiteHairline,
    inverseSurface = Chalk,
    inverseOnSurface = Graphite,
)

private val LightColorScheme = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SageTintLight,
    onPrimaryContainer = Sage,
    secondary = InkMuted,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = PaperSunken,
    onSurfaceVariant = InkMuted,
    surfaceContainerHigh = PaperRaised,
    surfaceContainerHighest = PaperSunken,
    error = Clay,
    onError = Color.White,
    errorContainer = ClayTintLight,
    onErrorContainer = Clay,
    outline = InkFaint,
    outlineVariant = Hairline,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
)

/** State colours that Material has no slot for: locked (clay), free (sage), focus (dusk). */
@Immutable
data class StatusColors(
    val locked: Color,
    val lockedTint: Color,
    val free: Color,
    val freeTint: Color,
    val focus: Color,
    val focusTint: Color,
)

private val LightStatus = StatusColors(Clay, ClayTintLight, Sage, SageTintLight, Dusk, DuskTintLight)
private val DarkStatus = StatusColors(ClayLight, ClayTintDark, SageLight, SageTintDark, DuskLight, DuskTintDark)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FocusLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
