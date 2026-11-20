package com.example.ui.theme

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
data class NoorExtendedColors(
    val headerForest: Color,
    val onHeaderForest: Color,
    val goldAccent: Color,
    val goldBorder: Color,
    val cardElevated: Color,
    val activeAyahHighlight: Color,
    val badgeEmeraldBg: Color,
    val badgeEmeraldText: Color
)

private val LightExtendedColors = NoorExtendedColors(
    headerForest = ForestEmerald,
    onHeaderForest = PureWhiteCard,
    goldAccent = MetallicGold,
    goldBorder = GoldBorderLight,
    cardElevated = PureWhiteCard,
    activeAyahHighlight = Color(0x1EB8863B),
    badgeEmeraldBg = Color(0x17283D6B),
    badgeEmeraldText = DeepEmerald
)

private val DarkExtendedColors = NoorExtendedColors(
    headerForest = DarkForestHeader,
    onHeaderForest = IvoryTextDark,
    goldAccent = WarmGoldBright,
    goldBorder = DarkGoldBorder,
    cardElevated = CharcoalCard,
    activeAyahHighlight = Color(0x26D4A95A),
    badgeEmeraldBg = Color(0x298EAAD9),
    badgeEmeraldText = LuminousEmerald
)

val LocalNoorColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = PureWhiteCard,
    primaryContainer = SoftEmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = MetallicGold,
    onSecondary = PureWhiteCard,
    secondaryContainer = SoftGoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = ForestEmerald,
    onTertiary = PureWhiteCard,
    background = CreamCanvas,
    onBackground = DeepCharcoalInk,
    surface = CreamCanvas,
    onSurface = DeepCharcoalInk,
    surfaceVariant = CreamSurfaceContainer,
    onSurfaceVariant = MutedSlateInk,
    surfaceContainerLowest = PureWhiteCard,
    surfaceContainerLow = CreamSurfaceLow,
    surfaceContainer = CreamSurfaceContainer,
    surfaceContainerHigh = CreamSurfaceHigh,
    outline = OutlineMuted,
    outlineVariant = GoldBorderLight,
    error = SemanticError,
    onError = PureWhiteCard
)

private val DarkColorScheme = darkColorScheme(
    primary = LuminousEmerald,
    onPrimary = OnEmeraldContainer,
    primaryContainer = DeepEmerald,
    onPrimaryContainer = IvoryTextDark,
    secondary = WarmGoldBright,
    onSecondary = OnGoldContainer,
    secondaryContainer = Color(0xFF4A3B08),
    onSecondaryContainer = WarmGoldBright,
    tertiary = DarkForestHeader,
    onTertiary = IvoryTextDark,
    background = CharcoalCanvas,
    onBackground = IvoryTextDark,
    surface = CharcoalCanvas,
    onSurface = IvoryTextDark,
    surfaceVariant = CharcoalSurfaceLow,
    onSurfaceVariant = MutedTextDark,
    surfaceContainerLowest = CharcoalCard,
    surfaceContainerLow = CharcoalSurfaceLow,
    surfaceContainer = CharcoalSurfaceHigh,
    surfaceContainerHigh = CharcoalSurfaceHigh,
    outline = MutedTextDark,
    outlineVariant = DarkGoldBorder,
    error = SemanticErrorDark,
    onError = Color(0xFF690005)
)

val NoorShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object NoorTheme {
    val colors: NoorExtendedColors
        @Composable get() = LocalNoorColors.current
}

@Composable
fun NoorSakinahTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(
        LocalNoorColors provides extendedColors,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoorTypography,
            shapes = NoorShapes,
            content = content
        )
    }
}
