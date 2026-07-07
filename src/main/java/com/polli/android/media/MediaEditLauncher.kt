package com.polli.android.media

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Launches the Compose [MediaEditorActivity] and returns the baked result URI.
 * Handles image crop/rotate, avatar (square) crop, and video trim/crop.
 */
class MediaEditLauncher(
    private val activity: ComponentActivity,
    private val onEdited: (Uri) -> Unit,
    private val onCancelled: () -> Unit = {},
) {
    private val launcher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) onEdited(uri) else onCancelled()
            } else {
                onCancelled()
            }
        }

    fun launchImage(source: Uri, cropAvatar: Boolean = false) {
        val mode = if (cropAvatar) MediaEditorActivity.Mode.AVATAR else MediaEditorActivity.Mode.IMAGE
        launcher.launch(MediaEditorActivity.intent(activity, source, mode))
    }

    fun launchVideo(source: Uri) {
        launcher.launch(MediaEditorActivity.intent(activity, source, MediaEditorActivity.Mode.VIDEO))
    }
}
