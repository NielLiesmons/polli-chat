package com.polli.ui.bridge

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Platform image loading — Android uses Coil/Glide; iOS/desktop get their own actuals later. */
interface ImageLoader {
    @Composable
    fun AsyncImage(model: Any?, contentDescription: String?, modifier: Modifier)
}

interface MediaPicker {
    fun pickGallery(onResult: (String?) -> Unit)
    fun pickDocument(onResult: (String?) -> Unit)
    fun takePhoto(onResult: (String?) -> Unit)
    fun pickContact(onResult: (Int?) -> Unit)
}

interface WebContent {
    @Composable
    fun WebView(url: String, modifier: Modifier)
}

interface QrScanner {
    @Composable
    fun Scanner(onCode: (String) -> Unit, modifier: Modifier)
}

interface AudioRecorder {
    fun start(): Boolean
    fun stop(send: Boolean): String?
    fun cancel()
}
