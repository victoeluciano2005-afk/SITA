package com.example.ui.theme

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

@Immutable
data class BotanicalExtendedColors(
    val idealContainer: Color,
    val onIdealContainer: Color,
    val cautionContainer: Color,
    val onCautionContainer: Color,
    val avoidContainer: Color,
    val onAvoidContainer: Color,
    val waterAccent: Color,
    val waterContainer: Color
)

private val LightExtendedColors = BotanicalExtendedColors(
    idealContainer = Color(0xFFD8EFE0),
    onIdealContainer = Color(0xFF14472B),
    cautionContainer = OchreSoft,
    onCautionContainer = OchreDeep,
    avoidContainer = ErrorClaySoft,
    onAvoidContainer = ErrorClayDeep,
    waterAccent = Color(0xFF1F6E8C),
    waterContainer = Color(0xFFDCEFF5)
)

private val DarkExtendedColors = BotanicalExtendedColors(
    idealContainer = Color(0xFF1D442C),
    onIdealContainer = Color(0xFFA7E8BC),
    cautionContainer = Color(0xFF4B3712),
    onCautionContainer = Color(0xFFFCD98A),
    avoidContainer = Color(0xFF54201A),
    onAvoidContainer = Color(0xFFFFBDB5),
    waterAccent = Color(0xFF7CCCE6),
    waterContainer = Color(0xFF193C4A)
)

val LocalBotanicalColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = Color.White,
    primaryContainer = SageSoft,
    onPrimaryContainer = ForestDeep,
    secondary = TerracottaPrimary,
    onSecondary = Color.White,
    secondaryContainer = TerracottaSoft,
    onSecondaryContainer = TerracottaDeep,
    tertiary = OchreWarm,
    onTertiary = Color.White,
    tertiaryContainer = OchreSoft,
    onTertiaryContainer = OchreDeep,
    background = ParchmentBg,
    onBackground = BarkInk,
    surface = ParchmentCard,
    onSurface = BarkInk,
    surfaceVariant = ClaySurface,
    onSurfaceVariant = BarkMuted,
    outline = ClayOutline,
    outlineVariant = Color(0xFFE5DDD0),
    error = ErrorClay,
    onError = Color.White,
    errorContainer = ErrorClaySoft,
    onErrorContainer = ErrorClayDeep
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSagePrimary,
    onPrimary = Color(0xFF0A2A17),
    primaryContainer = Color(0xFF1F4930),
    onPrimaryContainer = Color(0xFFD4F4DC),
    secondary = DarkTerracotta,
    onSecondary = Color(0xFF421506),
    secondaryContainer = Color(0xFF5D2713),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = DarkOchre,
    onTertiary = Color(0xFF3E2700),
    tertiaryContainer = Color(0xFF563B0A),
    onTertiaryContainer = Color(0xFFFFE5B0),
    background = DarkForestBg,
    onBackground = DarkInk,
    surface = DarkForestSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkForestCard,
    onSurfaceVariant = DarkInkMuted,
    outline = DarkOutline,
    outlineVariant = Color(0xFF2B3A2F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF7A221B),
    onErrorContainer = Color(0xFFFFDAD6)
)

val SiTaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SiTaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalBotanicalColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SiTaShapes,
            content = content
        )
    }
}
