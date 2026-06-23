package com.jbcbros.qbitremote.util

fun formatBytes(bytes: Long, decimals: Int = 2): String {
    if (bytes <= 0) return "0 B"
    val k = 1024.0
    val dm = if (decimals < 0) 0 else decimals
    val sizes = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    val i = (Math.log(bytes.toDouble()) / Math.log(k)).toInt().coerceAtMost(sizes.size - 1)
    return "${(bytes / Math.pow(k, i.toDouble())).toFixed(dm)} ${sizes[i]}"
}

fun formatSpeed(bytesPerSec: Long): String = "${formatBytes(bytesPerSec, 1)}/s"

private fun Double.toFixed(decimals: Int): String {
    return String.format(java.util.Locale.US, "%.${decimals}f", this)
}
