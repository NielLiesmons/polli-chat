package com.polli.android.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.widget.ImageView
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import com.b44t.messenger.DcContact
import com.polli.android.platform.EngineBridge
import com.polli.android.settings.AppPrefs
import com.polli.android.sigil.buildSigilSilhouette
import com.polli.android.ui.AvatarPhoto
import com.polli.core.sigil.MnsSigil
import com.polli.core.sigil.SigilIdentity
import com.polli.ui.theme.ProfileColors
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.roundToInt

/** View bind path for [com.polli.android.ui.PolliAvatar] — DC profile photo or MNS sigil. */
internal object ViewProfileAvatar {
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()

    fun bind(
        imageView: ImageView,
        name: String,
        seed: String,
        contactId: Int,
    ) {
        val context = imageView.context
        val sizePx = min(imageView.layoutParams?.width ?: 0, imageView.layoutParams?.height ?: 0)
            .takeIf { it > 0 }
            ?: ViewChatUi.dp(context, 40f)

        val sigilOnly = AppPrefs(context).sigilOnlyMode
        val photoPath =
            if (!sigilOnly && contactId > 0) {
                AvatarPhoto.storedImagePath(context, chatId = null, contactId = contactId, dcContext = null)
            } else {
                null
            }
        val photoBitmap =
            photoPath?.let {
                try {
                    BitmapFactory.decodeFile(it)
                } catch (_: Throwable) {
                    null
                }
            }
        if (photoBitmap != null) {
            imageView.setImageBitmap(photoBitmap)
            return
        }

        val identity = resolveIdentity(context, seed, contactId)
        val cacheKey = "$identity@$sizePx"
        val sigilBitmap =
            bitmapCache.get(cacheKey) ?: renderSigilBitmap(context, identity, sizePx).also {
                bitmapCache[cacheKey] = it
            }
        imageView.setImageBitmap(sigilBitmap)
    }

    private fun resolveIdentity(context: Context, seed: String, contactId: Int): String {
        try {
            val dc = EngineBridge.getContext(context)
            if (contactId != 0) {
                dc.getContact(contactId).addr?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
            }
        } catch (_: Exception) {
        }
        return seed.ifBlank { nameFallback(contactId) }
    }

    private fun nameFallback(contactId: Int): String =
        if (contactId == DcContact.DC_CONTACT_ID_SELF) "self" else "#"

    private fun renderSigilBitmap(context: Context, identity: String, sizePx: Int): Bitmap {
        val sigil = SigilIdentity.resolve(identity)
        val color = ProfileColors.stringToColor(sigil.name)
        val onColor =
            android.graphics.Color.rgb(
                (color.red * 255).roundToInt(),
                (color.green * 255).roundToInt(),
                (color.blue * 255).roundToInt(),
            )
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFF333333.toInt())
        val grid = MnsSigil.grid(sigil.value)
        val silhouette: Path = buildSigilSilhouette(grid, contentInsetFraction = 0.12f)
        val androidPath: AndroidPath = silhouette.asAndroidPath()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = onColor
        val matrix = android.graphics.Matrix()
        val side = sizePx.toFloat()
        matrix.setScale(side, side)
        androidPath.transform(matrix)
        canvas.drawPath(androidPath, paint)
        return bmp
    }
}
