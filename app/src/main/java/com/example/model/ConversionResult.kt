package com.example.model

import android.graphics.Bitmap
import java.io.File

data class ConversionResult(
    val file: File,
    val fileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val previewBitmaps: List<Bitmap> = emptyList()
) {
    val formattedSize: String
        get() {
            val kb = fileSizeBytes / 1024.0
            return if (kb > 1024) {
                String.format("%.2f MB", kb / 1024.0)
            } else {
                String.format("%.1f KB", kb)
            }
        }
}
