package com.baoji.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/** Manual update check against GitHub releases. Nothing here runs unless the user presses the button. */
@Singleton
class UpdateRepository @Inject constructor(@ApplicationContext private val context: Context) {

    val currentVersion: String by lazy {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    private val currentVersionCode: Long by lazy {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    }

    /** The latest release if it is newer than this install, else null. */
    suspend fun checkNewer(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val conn = open(ReleaseParser.LATEST_URL).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            when (conn.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> throw UpdateProblem("仓库还没有发布版本")
                HttpURLConnection.HTTP_FORBIDDEN, 429 -> throw UpdateProblem("GitHub 访问太频繁，过一会儿再试")
                else -> throw UpdateProblem("GitHub 暂时不可用（${conn.responseCode}）")
            }
            val info = ReleaseParser.parse(conn.inputStream.bufferedReader().use { it.readText() })
            info.takeIf { Versions.isNewer(it.versionName, currentVersion) }
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads the APK into the cache, reporting progress 0..1, and checks it really is a newer build of this app. */
    suspend fun download(info: ReleaseInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        if (info.apkSize !in 1..MAX_APK_BYTES) throw UpdateProblem("安装包大小不对")
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        // A unique name per attempt, so a cancelled download still winding down cannot delete a newer one.
        val file = File(dir, "baoji-${info.versionName}-${System.nanoTime()}.apk")
        val conn = open(info.apkUrl)
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) throw UpdateProblem("下载失败（${conn.responseCode}）")
            var done = 0L
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        done += n
                        if (done > info.apkSize) throw UpdateProblem("安装包大小不对")
                        out.write(buf, 0, n)
                        onProgress(done.toFloat() / info.apkSize)
                    }
                }
            }
            if (done != info.apkSize) throw UpdateProblem("下载不完整，请重试")
            verify(file)
            file
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally {
            conn.disconnect()
        }
    }

    private fun verify(file: File) {
        val pkg = context.packageManager.getPackageArchiveInfo(file.path, 0)
            ?: throw UpdateProblem("安装包已损坏，请重试")
        if (pkg.packageName != context.packageName) throw UpdateProblem("这不是薄肌的安装包")
        if (PackageInfoCompat.getLongVersionCode(pkg) <= currentVersionCode) throw UpdateProblem("安装包版本不比当前新")
    }

    /** Android asks once per app for permission to install packages; until granted we send the user to that switch. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "baoji/$currentVersion")
        }

    companion object {
        const val MAX_APK_BYTES = 100L * 1024 * 1024
    }
}
