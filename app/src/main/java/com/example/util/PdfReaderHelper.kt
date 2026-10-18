package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfReaderHelper {

    suspend fun preparePdfForViewing(
        context: Context,
        uri: Uri
    ): Triple<File, String, List<Pair<Int, Int>>> = withContext(Dispatchers.IO) {
        val title = getFileName(context, uri)
        val cacheDir = File(context.cacheDir, "opened_pdfs")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        // Clean previous temp file
        val tempFile = File(cacheDir, "view_${System.currentTimeMillis()}.pdf")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Could not read the PDF file.")

        if (tempFile.length() == 0L) {
            tempFile.delete()
            throw IOException("The selected PDF file is empty.")
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val dimensions = mutableListOf<Pair<Int, Int>>()

        try {
            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = try {
                PdfRenderer(pfd)
            } catch (se: SecurityException) {
                throw SecurityException("This PDF is password-protected and cannot be opened.", se)
            } catch (e: Exception) {
                throw IOException("This PDF file appears to be corrupted or invalid.", e)
            }

            val pageCount = renderer.pageCount
            if (pageCount == 0) {
                throw IOException("This PDF does not contain any readable pages.")
            }

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                dimensions.add(Pair(page.width, page.height))
                page.close()
            }
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {
            }
        }

        Triple(tempFile, title, dimensions)
    }

    suspend fun renderPageBitmap(
        file: File,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            val page = renderer.openPage(pageIndex)
            val scale = targetWidth.toFloat() / page.width.toFloat()
            val targetHeight = (page.height * scale).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (_: Exception) {
            null
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {
            }
        }
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "Document.pdf"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {
            uri.lastPathSegment?.let { segment ->
                name = segment.substringAfterLast('/')
            }
        }
        if (!name.endsWith(".pdf", ignoreCase = true)) {
            name = "$name.pdf"
        }
        return name
    }
}
