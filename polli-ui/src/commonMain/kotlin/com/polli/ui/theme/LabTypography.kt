package com.polli.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LabFontFamily = FontFamily.SansSerif

val LabTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = LabFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 21.75.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = LabFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = LabFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.5.sp,
        lineHeight = 21.75.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = LabFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.5.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = LabFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 34.5.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = LabFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 19.5.sp,
    ),
)
