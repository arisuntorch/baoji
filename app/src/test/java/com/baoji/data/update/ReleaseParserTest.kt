package com.baoji.data.update

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ReleaseParserTest {

    private fun release(tag: String = "v0.1.3", assets: String = apk("baoji-0.1.3.apk")) = """
        {"tag_name":"$tag","name":"薄肌 $tag","body":"## 更新\n- 改了东西","draft":false,"prerelease":false,
         "assets":[$assets],"author":{"login":"arisuntorch"}}
    """.trimIndent()

    private fun apk(name: String, url: String = "https://github.com/arisuntorch/baoji/releases/download/v0.1.3/$name") =
        """{"name":"$name","size":1972832,"browser_download_url":"$url","content_type":"application/vnd.android.package-archive"}"""

    @Test
    fun parsesTagNotesAndApkAsset() {
        val info = ReleaseParser.parse(release())
        assertThat(info.versionName).isEqualTo("0.1.3")
        assertThat(info.notes).contains("改了东西")
        assertThat(info.apkUrl).endsWith("/v0.1.3/baoji-0.1.3.apk")
        assertThat(info.apkSize).isEqualTo(1972832)
    }

    @Test
    fun notesLoseMarkdownSymbols() {
        val md = "## 训练页调整\r\n\r\n- **去掉打卡**：直接点 `圆形按钮`\n- [指南](https://x.y/z) 里补充\n\n\n## 安装"
        assertThat(ReleaseParser.plainNotes(md)).isEqualTo("训练页调整\n\n· 去掉打卡：直接点 圆形按钮\n· 指南 里补充\n\n安装")
    }

    @Test
    fun picksTheApkAmongOtherAssets() {
        val assets = """{"name":"icons-0.1.3.png","size":10,"browser_download_url":"https://github.com/arisuntorch/baoji/releases/download/v0.1.3/icons-0.1.3.png"},""" +
            apk("baoji-0.1.3.apk")
        assertThat(ReleaseParser.parse(release(assets = assets)).apkUrl).endsWith(".apk")
    }

    @Test
    fun releaseWithoutApkIsAnError() {
        val e = assertThrows(UpdateProblem::class.java) { ReleaseParser.parse(release(assets = "")) }
        assertThat(e.message).isEqualTo("新版本没有安装包")
    }

    @Test
    fun apkFromAnotherHostIsRejected() {
        val bad = apk("baoji-0.1.3.apk", url = "https://evil.example.com/baoji-0.1.3.apk")
        assertThrows(UpdateProblem::class.java) { ReleaseParser.parse(release(assets = bad)) }
        val otherRepo = apk("baoji-0.1.3.apk", url = "https://github.com/someone/baoji/releases/download/v0.1.3/baoji-0.1.3.apk")
        assertThrows(UpdateProblem::class.java) { ReleaseParser.parse(release(assets = otherRepo)) }
    }

    @Test
    fun garbageIsAnError() {
        assertThrows(UpdateProblem::class.java) { ReleaseParser.parse("<html>rate limited</html>") }
    }

    @Test
    fun versionComparisonIsNumericPerPart() {
        assertThat(Versions.isNewer("0.1.3", "0.1.2")).isTrue()
        assertThat(Versions.isNewer("0.1.10", "0.1.9")).isTrue()
        assertThat(Versions.isNewer("0.2", "0.1.9")).isTrue()
        assertThat(Versions.isNewer("1.0.0", "0.9.9")).isTrue()
        assertThat(Versions.isNewer("0.1.2", "0.1.2")).isFalse()
        assertThat(Versions.isNewer("0.1.2", "0.1.3")).isFalse()
        assertThat(Versions.isNewer("0.1.2.0", "0.1.2")).isFalse()
    }

    @Test
    fun unreadableVersionIsNeverNewer() {
        assertThat(Versions.isNewer("nightly", "0.1.2")).isFalse()
        assertThat(Versions.isNewer("", "0.1.2")).isFalse()
    }
}
