package com.example.model

import android.net.Uri

data class ImageItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val rotationDegrees: Int = 0,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val filterMode: ScanFilter = ScanFilter.ORIGINAL,
    val brightness: Float = 0f, // -255 to 255
    val contrast: Float = 1f    // 0 to 2
) {
    fun rotatedClockwise(): ImageItem {
        val nextDegrees = (rotationDegrees + 90) % 360
        return copy(rotationDegrees = nextDegrees)
    }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return ""
            val kb = sizeBytes / 1024.0
            return if (kb > 1024) {
                String.format("%.1f MB", kb / 1024.0)
            } else {
                String.format("%.0f KB", kb)
            }
        }
}
