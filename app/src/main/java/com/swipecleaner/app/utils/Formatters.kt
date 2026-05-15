package com.swipecleaner.app.utils

import java.util.Locale

object Formatters {
    fun bytesToHuman(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var idx = 0
        while (value >= 1024 && idx < units.lastIndex) {
            value /= 1024
            idx++
        }
        return String.format(Locale.getDefault(), "%.1f %s", value, units[idx])
    }
}
