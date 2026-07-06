package com.polli.android.media

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.polli.android.platform.LegacyScribbleActivity

/**
 * Kotlin bridge to Delta Chat's image editor ([LegacyScribbleActivity]).
 * Keeps crop/draw/blur/sticker tooling until a Compose editor exists.
 */
class ImageEditLauncher(
    private val activity: ComponentActivity,
    private val onEdited: (Uri) -> Unit,
    private val onCancelled: () -> Unit = {},
) {
    private val launcher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    onEdited(uri)
                } else {
                    onCancelled()
                }
            } else {
                onCancelled()
            }
        }

    fun launch(source: Uri, cropAvatar: Boolean = false) {
        val intent = Intent(activity, LegacyScribbleActivity::class.java).apply {
            setDataAndType(source, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (cropAvatar) {
                putExtra(LegacyScribbleActivity.CROP_AVATAR, true)
            }
        }
        launcher.launch(intent)
    }
}
