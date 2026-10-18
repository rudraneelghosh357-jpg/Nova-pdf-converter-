package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

object PdfPreviewHelper {

    fun renderPreviews(pdfFile: File, maxPages: Int = 8): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pagesToRender = minOf(renderer.pageCount, maxPages)

            for (i in 0 until pagesToRender) {
                val page = renderer.openPage(i)
                // Render at a clear display resolution (e.g. 1.25x or fixed target width)
                val targetWidth = 720
                val scale = targetWidth.toFloat() / page.width.toFloat()
                val targetHeight = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bitmap)
            }
        } catch (_: Exception) {
            // Silently fallback if preview rendering fails
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {
            }
        }

        return bitmaps
    }
}
