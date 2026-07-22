package com.polli.android.theme

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.polli.android.R

/** Cached Inter typefaces for View-based UI (Compose uses [com.polli.ui.theme.PolliFontFamily]). */
object PolliTypefaces {
    @Volatile private var regular: Typeface? = null
    @Volatile private var bold: Typeface? = null

    fun regular(context: Context): Typeface {
        regular?.let { return it }
        return (ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.SANS_SERIF).also {
            regular = it
        }
    }

    fun bold(context: Context): Typeface {
        bold?.let { return it }
        return (ResourcesCompat.getFont(context, R.font.inter_bold) ?: Typeface.DEFAULT_BOLD).also {
            bold = it
        }
    }
}
