package com.polli.android.qr

/** SVG post-processing shared by QR-showing screens. */
object QrSvg {
    /**
     * HACK: move avatar-letter down; baseline alignment not working in the core-generated SVG.
     * See https://github.com/deltachat/deltachat-core-rust/pull/2815#issuecomment-978067378
     */
    fun fixSvg(svg: String): String = svg.replace("y=\"281.136\"", "y=\"296\"")
}
