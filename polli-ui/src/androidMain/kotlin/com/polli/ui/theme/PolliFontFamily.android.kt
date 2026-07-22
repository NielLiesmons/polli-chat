package com.polli.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.polli.ui.R

actual val PolliFontFamily: FontFamily =
    FontFamily(
        Font(R.font.inter_light, FontWeight.Light),
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.inter_bold, FontWeight.Bold),
        // Medium/SemiBold map onto Regular/Bold — Inter Medium file not shipped yet.
        Font(R.font.inter_regular, FontWeight.Medium),
        Font(R.font.inter_bold, FontWeight.SemiBold),
    )
