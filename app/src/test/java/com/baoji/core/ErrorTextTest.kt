package com.baoji.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

class ErrorTextTest {
    @Test
    fun ownFileProblemsPassThrough() {
        assertThat(FileProblem("文件太大").reasonForUser()).isEqualTo("文件太大")
    }

    @Test
    fun permissionErrorsAreTranslatedNotLeaked() {
        val raw = SecurityException("Permission Denial: writing content://com.android.providers.downloads.documents/document/1")
        val reason = raw.reasonForUser()
        assertThat(reason).isEqualTo("这个位置没有读写权限")
        assertThat(reason).doesNotContain("content://")
    }

    @Test
    fun otherSystemErrorsAreHidden() {
        assertThat(IOException("EIO /storage/emulated/0").reasonForUser()).isEqualTo("读写文件时出错")
        assertThat(IllegalStateException("UNIQUE constraint failed: set_log.id").reasonForUser()).isEqualTo("未知错误")
    }
}
