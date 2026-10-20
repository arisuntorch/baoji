package com.baoji.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Reading user-picked files and writing exports. All blocking I/O runs on [Dispatchers.IO]. */
@Singleton
class FileIo @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("无法打开文件")
        stream.use { input ->
            val bytes = input.readNBytesCompat(MAX_IMPORT_BYTES + 1)
            if (bytes.size > MAX_IMPORT_BYTES) throw IOException("文件太大")
            bytes.toString(Charsets.UTF_8)
        }
    }

    suspend fun writeText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("无法写入文件")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    /** Writes into the app cache and returns a share intent granting read access to that one file. */
    suspend fun shareIntent(fileName: String, text: String): Intent = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, fileName)
        file.writeText(text, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Intent.createChooser(send, fileName).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (total < limit) {
            val n = read(buf, 0, minOf(buf.size, limit - total))
            if (n < 0) break
            out.write(buf, 0, n)
            total += n
        }
        return out.toByteArray()
    }

    companion object {
        const val MAX_IMPORT_BYTES = 20 * 1024 * 1024
    }
}
