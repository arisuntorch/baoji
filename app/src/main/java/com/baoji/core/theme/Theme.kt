package com.baoji.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Apple-style greys only. No accent colour anywhere. */
@Immutable
data class BaojiColors(
    val background: Color,
    val card: Color,
    val fill: Color,
    val separator: Color,
    val secondaryText: Color,
    val primaryText: Color,
    val watermark: Color,
)

val LightColors = BaojiColors(
    background = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    fill = Color(0xFFE5E5EA),
    separator = Color(0xFFC6C6C8),
    secondaryText = Color(0xFF8E8E93),
    primaryText = Color(0xFF000000),
    watermark = Color(0xFFE5E5EA),
)

val DarkColors = BaojiColors(
    background = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    fill = Color(0xFF2C2C2E),
    separator = Color(0xFF38383A),
    secondaryText = Color(0xFF8E8E93),
    primaryText = Color(0xFFFFFFFF),
    watermark = Color(0xFF1C1C1E),
)

val LocalBaojiColors = staticCompositionLocalOf { LightColors }

object Baoji {
    val colors: BaojiColors
        @Composable get() = LocalBaojiColors.current
}

@Composable
fun BaojiTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.primaryText,
        onPrimary = c.background,
        secondary = c.secondaryText,
        background = c.background,
        onBackground = c.primaryText,
        surface = c.card,
        onSurface = c.primaryText,
        surfaceVariant = c.fill,
        onSurfaceVariant = c.secondaryText,
        surfaceContainerLow = c.card,
        surfaceContainer = c.card,
        surfaceContainerHigh = c.card,
        outline = c.separator,
        outlineVariant = c.separator,
        error = c.primaryText,
    )
    CompositionLocalProvider(LocalBaojiColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = BaojiTypography, content = content)
    }
}
