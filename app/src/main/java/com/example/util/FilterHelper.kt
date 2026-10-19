package com.example.util

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

object FilterHelper {
    fun getColorFilter(mode: com.example.model.ScanFilter, brightness: Float, contrast: Float): ColorMatrixColorFilter {
        val matrix = ColorMatrix()

        // Apply mode-specific matrix
        when (mode) {
            com.example.model.ScanFilter.ORIGINAL -> {}
            com.example.model.ScanFilter.MAGIC -> {
                matrix.setSaturation(1.3f)
                matrix.postConcat(ColorMatrix().apply { setScale(1f, 1f, 1f, 1f) }) // placeholder
            }
            com.example.model.ScanFilter.BW -> {
                matrix.setSaturation(0f)
                val bwMatrix = floatArrayOf(
                    1.5f, 0f, 0f, 0f, -100f,
                    0f, 1.5f, 0f, 0f, -100f,
                    0f, 0f, 1.5f, 0f, -100f,
                    0f, 0f, 0f, 1f, 0f
                )
                matrix.postConcat(ColorMatrix(bwMatrix))
            }
            com.example.model.ScanFilter.GRAYSCALE -> matrix.setSaturation(0f)
            com.example.model.ScanFilter.VIBRANT -> matrix.setSaturation(1.5f)
            com.example.model.ScanFilter.WARM -> {
                matrix.set(floatArrayOf(
                    1.1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 0.9f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            com.example.model.ScanFilter.COOL -> {
                matrix.set(floatArrayOf(
                    0.9f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1.1f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            com.example.model.ScanFilter.SHARPEN -> {} // Needs convolution, will handle differently
        }

        // Apply Brightness/Contrast
        // Contrast: (1-c)*128 + c*pixel
        val contrastScale = contrast // e.g. 1.0f
        val contrastOffset = (1 - contrastScale) * 128f
        matrix.postConcat(ColorMatrix(floatArrayOf(
            contrastScale, 0f, 0f, 0f, brightness + contrastOffset,
            0f, contrastScale, 0f, 0f, brightness + contrastOffset,
            0f, 0f, contrastScale, 0f, brightness + contrastOffset,
            0f, 0f, 0f, 1f, 0f
        )))

        return ColorMatrixColorFilter(matrix)
    }
}
