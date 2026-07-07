package com.polli.android.qr

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/**
 * CaptureManager subclass that keeps the proven zxing scanning engine while letting a Compose host
 * intercept results (for confirm dialogs) and downgrade the framework-bug crash to a toast.
 */
open class InterceptingCaptureManager(
    activity: Activity,
    barcodeView: DecoratedBarcodeView,
) : CaptureManager(activity, barcodeView) {

    /** Called instead of the default finish-on-error; message may be blank. */
    var onFrameworkBug: ((String) -> Unit)? = null

    /** Intercepts a decoded result; call the provided runnable to proceed with the default return. */
    var resultInterceptor: ((BarcodeResult, Runnable) -> Unit)? = null

    override fun displayFrameworkBugMessageAndExit(message: String) {
        val cb = onFrameworkBug
        if (cb != null) cb(message) else super.displayFrameworkBugMessageAndExit(message)
    }

    override fun returnResult(rawResult: BarcodeResult) {
        val interceptor = resultInterceptor
        if (interceptor != null) {
            interceptor(rawResult, Runnable { super.returnResult(rawResult) })
        } else {
            super.returnResult(rawResult)
        }
    }
}

/** Hosts a pre-created zxing [DecoratedBarcodeView] inside Compose. */
@Composable
fun ZxingScannerView(barcodeView: DecoratedBarcodeView, modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier, factory = { barcodeView })
}
