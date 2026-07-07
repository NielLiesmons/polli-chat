package com.polli.android.debug

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.View
import androidx.core.content.PermissionChecker
import chat.delta.rpc.RpcException
import com.polli.android.platform.EngineBridge
import com.polli.android.platform.PlatformLegacyUtil
import com.polli.android.BuildConfig
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds the diagnostic report + grabs logcat for the debug log viewer. */
object LogDump {

    fun build(context: Context): String =
        "**This log may contain sensitive information. If you want to post it publicly you may " +
            "examine and edit it beforehand.**\n\n" +
            buildDescription(context) +
            "\n" +
            grabLogcat()

    /** Writes [text] to [outputDir], returning the file or null on failure / empty text. */
    fun writeLog(outputDir: File, text: String): File? {
        if (text.isBlank()) return null
        val name = "deltachat-log-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".txt"
        return try {
            val logFile = File(outputDir, name)
            if (!logFile.exists()) logFile.createNewFile()
            BufferedWriter(FileWriter(logFile, false)).use { it.write(text) }
            logFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun grabLogcat(): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -v threadtime -d -t 10000 *:I")
            val log = StringBuilder()
            val separator = System.lineSeparator()
            process.inputStream.bufferedReader().forEachLine { raw ->
                val line = raw
                    .replaceFirst(Regex(" (\\d+) E "), " $1 \uD83D\uDD34 ")
                    .replaceFirst(Regex(" (\\d+) W "), " $1 \uD83D\uDFE0 ")
                    .replaceFirst(Regex(" (\\d+) I "), " $1 \uD83D\uDD35 ")
                    .replaceFirst(Regex(" (\\d+) D "), " $1 \uD83D\uDFE2 ")
                log.append(line).append(separator)
            }
            log.toString()
        } catch (e: Exception) {
            "Error grabbing log: $e"
        }
    }

    private fun memoryUsage(): String {
        val info = Runtime.getRuntime()
        return String.format(
            Locale.ENGLISH,
            "%dM (%.2f%% free, %dM max)",
            info.totalMemory().megs(),
            info.freeMemory().toFloat() / info.totalMemory() * 100f,
            info.maxMemory().megs(),
        )
    }

    private fun memoryClass(context: Context): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val lowMem = if (am.isLowRamDevice) ", low-mem device" else ""
        return "${am.memoryClass}$lowMem"
    }

    private fun Long.megs(): Long = this / 1_048_576L

    private fun buildDescription(context: Context): String {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val pm = context.packageManager
        val b = StringBuilder()

        b.append("device=").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
            .append(" (").append(Build.PRODUCT).append(")\n")
        b.append("android=").append(Build.VERSION.RELEASE).append(" (")
            .append(Build.VERSION.INCREMENTAL).append(", ").append(Build.DISPLAY).append(")\n")
        b.append("sdk=").append(Build.VERSION.SDK_INT).append("\n")
        b.append("memory=").append(memoryUsage()).append("\n")
        b.append("memoryClass=").append(memoryClass(context)).append("\n")
        b.append("host=").append(Build.HOST).append("\n")
        b.append("applicationId=").append(BuildConfig.APPLICATION_ID).append("\n")
        b.append("app=")
        try {
            val pkg = context.packageName
            b.append(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0))).append(" ")
                .append(pm.getPackageInfo(pkg, 0).versionName).append("-")
                .append(BuildConfig.FLAVOR).append(if (BuildConfig.DEBUG) "-debug" else "").append("\n")
            @Suppress("DEPRECATION")
            b.append("versionCode=").append(pm.getPackageInfo(pkg, 0).versionCode).append("\n")
            @Suppress("DEPRECATION")
            b.append("installer=").append(pm.getInstallerPackageName(pkg)).append("\n")
            b.append("ignoreBatteryOptimizations=")
                .append(powerManager.isIgnoringBatteryOptimizations(pkg)).append("\n")
            b.append("reliableService=").append(PlatformLegacyUtil.reliableService(context)).append("\n")

            val config = context.resources.configuration
            b.append("lang=").append(Locale.getDefault().toString()).append("\n")
            val isRtl = config.layoutDirection == View.LAYOUT_DIRECTION_RTL
            b.append("rtl=").append(isRtl).append("\n")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = PermissionChecker.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS,
                ) == PermissionChecker.PERMISSION_GRANTED
                b.append("post-notifications-granted=").append(granted).append("\n")
            } else {
                b.append("post-notifications-granted=<not needed>\n")
            }

            val token = PlatformLegacyUtil.pushToken()
            b.append("push-enabled=").append(PlatformLegacyUtil.isPushEnabled(context)).append("\n")
            b.append("push-token=").append(token ?: "<empty>").append("\n")
        } catch (e: Exception) {
            b.append("Unknown\n")
        }

        val rpc = EngineBridge.getRpc(context)
        val accId = EngineBridge.getContext(context).accountId
        b.append("\n")
        try {
            b.append(rpc.getStorageUsageReportString(accId))
        } catch (e: RpcException) {
            b.append(e)
        }
        b.append(EngineBridge.getContext(context).info)
        return b.toString()
    }
}
