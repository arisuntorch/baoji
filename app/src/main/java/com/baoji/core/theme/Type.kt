package com.baoji.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** System font everywhere. Digits are tabular so rolling numbers don't jitter. */
private const val TNUM = "tnum"

private fun style(size: Int, weight: FontWeight = FontWeight.Normal, line: Int = size + 6) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontFeatureSettings = TNUM,
)

val BaojiTypography = Typography(
    displayLarge = style(56, FontWeight.Light, 62),
    displayMedium = style(44, FontWeight.Light, 50),
    headlineMedium = style(28, FontWeight.SemiBold),
    titleLarge = style(22, FontWeight.SemiBold),
    titleMedium = style(17, FontWeight.Medium),
    bodyLarge = style(17),
    bodyMedium = style(15),
    bodySmall = style(13),
    labelLarge = style(15, FontWeight.Medium),
    labelMedium = style(13),
    labelSmall = style(11),
)
