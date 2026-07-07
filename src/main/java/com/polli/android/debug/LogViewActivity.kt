package com.polli.android.debug

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.polli.android.BaseComposeActivity
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.settings.AppPrefs
import com.polli.android.theme.PolliColors
import com.polli.android.theme.PolliTheme
import com.polli.android.ui.AppInsets
import com.polli.android.ui.RoundBackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.thoughtcrime.securesms.R
import java.io.File

/** Compose debug log viewer — replaces the Java LogViewActivity + LogViewFragment. */
class LogViewActivity : BaseComposeActivity() {

    private var logText by mutableStateOf("")

    private val requestSavePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) saveToDownloads() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logText = getString(R.string.one_moment)

        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) { LogDump.build(this@LogViewActivity) }
            logText = text.ifEmpty {
                "Could not read the log on your device. You can still use ADB to get a debug log instead."
            }
        }

        val prefs = AppPrefs(this)
        setContent {
            PolliTheme(prefs = prefs) {
                LogViewScreen(
                    logText = logText,
                    onBack = { finish() },
                    onSave = { onSaveLog() },
                    onShare = { shareLog() },
                )
            }
        }
    }

    private fun onSaveLog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            saveToDownloads()
        } else {
            requestSavePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun saveToDownloads() {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val success = LogDump.writeLog(dir, logText) != null
        AlertDialog.Builder(this)
            .setMessage(if (success) R.string.pref_saved_log else R.string.pref_save_log_failed)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun shareLog() {
        try {
            val logFile = LogDump.writeLog(externalCacheDir ?: cacheDir, logText) ?: return
            val uri = PlatformLegacyUtil.fileProviderUri(this, logFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.chat_share_with_title)))
        } catch (e: Exception) {
            Log.e(TAG, "failed to share log", e)
        }
    }

    companion object {
        private const val TAG = "LogViewActivity"

        fun intent(context: Context): Intent = Intent(context, LogViewActivity::class.java)
    }
}

@Composable
private fun LogViewScreen(
    logText: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    var textSizeSp by remember { mutableFloatStateOf(12f) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PolliColors.Black)
            .padding(top = AppInsets.statusBarTop() + 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                stringResource(R.string.pref_view_log),
                color = PolliColors.White85,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            LogMenu(
                onSave = onSave,
                onShare = onShare,
                onZoomIn = { textSizeSp += 2f },
                onZoomOut = { if (textSizeSp > 4f) textSizeSp -= 2f },
                onScrollTop = { scope.launch { scrollState.animateScrollTo(0) } },
                onScrollBottom = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            SelectionContainer {
                Text(
                    logText,
                    color = PolliColors.White85,
                    fontFamily = FontFamily.Monospace,
                    fontSize = textSizeSp.sp,
                )
            }
        }
    }
}

@Composable
private fun LogMenu(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = PolliColors.White85)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.menu_share)) }, onClick = { open = false; onShare() })
            DropdownMenuItem(text = { Text(stringResource(R.string.menu_save_log)) }, onClick = { open = false; onSave() })
            DropdownMenuItem(text = { Text(stringResource(R.string.menu_zoom_in)) }, onClick = { open = false; onZoomIn() })
            DropdownMenuItem(text = { Text(stringResource(R.string.menu_zoom_out)) }, onClick = { open = false; onZoomOut() })
            DropdownMenuItem(text = { Text(stringResource(R.string.menu_scroll_to_top)) }, onClick = { open = false; onScrollTop() })
            DropdownMenuItem(text = { Text(stringResource(R.string.menu_scroll_to_bottom)) }, onClick = { open = false; onScrollBottom() })
        }
    }
}
