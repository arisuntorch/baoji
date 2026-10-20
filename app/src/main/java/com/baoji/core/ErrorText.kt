package com.baoji.core

import java.io.IOException

/** A file problem whose message is already written for the user (Chinese, no jargon). */
class FileProblem(message: String) : IOException(message)

/**
 * Short reason to show after "xx失败：". Our own [FileProblem] messages pass through; system exceptions
 * carry English internals (URIs, class names) that mean nothing to the user, so they are mapped or hidden.
 */
fun Throwable.reasonForUser(): String = when (this) {
    is FileProblem -> message.orEmpty()
    is SecurityException -> "这个位置没有读写权限"
    is IOException -> "读写文件时出错"
    else -> "未知错误"
}
