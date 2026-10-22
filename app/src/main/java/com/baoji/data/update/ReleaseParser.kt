package com.baoji.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/** An update problem whose message is already written for the user. */
class UpdateProblem(message: String) : IOException(message)

/** The latest GitHub release, reduced to what the updater needs. */
data class ReleaseInfo(val versionName: String, val notes: String, val apkUrl: String, val apkSize: Long)

object ReleaseParser {

    const val REPO = "arisuntorch/baoji"
    const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"

    /** Only APKs attached to this repo's releases are accepted; GitHub redirects these to its own CDN. */
    private const val DOWNLOAD_PREFIX = "https://github.com/$REPO/releases/download/"

    private val JSON = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Release(
        @SerialName("tag_name") val tag: String,
        val body: String? = null,
        val assets: List<Asset> = emptyList(),
    )

    @Serializable
    private data class Asset(
        val name: String,
        val size: Long,
        @SerialName("browser_download_url") val url: String,
    )

    fun parse(json: String): ReleaseInfo {
        val release = try {
            JSON.decodeFromString<Release>(json)
        } catch (e: SerializationException) {
            throw UpdateProblem("读不懂 GitHub 返回的内容")
        } catch (e: IllegalArgumentException) {
            throw UpdateProblem("读不懂 GitHub 返回的内容")
        }
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: throw UpdateProblem("新版本没有安装包")
        if (!apk.url.startsWith(DOWNLOAD_PREFIX)) throw UpdateProblem("安装包地址不对，已忽略")
        return ReleaseInfo(release.tag.removePrefix("v"), plainNotes(release.body.orEmpty()), apk.url, apk.size)
    }

    /** Release notes are Markdown; the sheet shows plain text, so drop the symbols and keep the words. */
    fun plainNotes(markdown: String): String = markdown
        .replace("\r\n", "\n")
        .lines()
        .joinToString("\n") { line ->
            line.trim()
                .replace(HEADING, "")
                .replace(BULLET, "· ")
                .replace(LINK, "$1")
                .replace("**", "")
                .replace("`", "")
        }
        .replace(BLANK_RUN, "\n\n")
        .trim()

    private val HEADING = Regex("^#{1,6}\\s+")
    private val BULLET = Regex("^[-*]\\s+")
    private val LINK = Regex("\\[([^\\]]*)]\\([^)]*\\)")
    private val BLANK_RUN = Regex("\n{3,}")
}

object Versions {
    /** Dotted numeric versions compared part by part; anything unreadable is never "newer". */
    fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote) ?: return false
        val l = parts(local) ?: return false
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parts(v: String): List<Int>? =
        v.trim().split('.').map { it.toIntOrNull() ?: return null }.takeIf { it.isNotEmpty() }
}
