package com.example.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.ImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileUtils {

    fun getImageItemFromUri(context: Context, uri: Uri): ImageItem {
        var name = "image_${System.currentTimeMillis()}"
        var size: Long = 0

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        name = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to default
        }

        return ImageItem(
            id = UUID.randomUUID().toString(),
            uri = uri,
            name = name,
            sizeBytes = size
        )
    }

    fun createTempCameraUri(context: Context): Pair<File, Uri> {
        val tempDir = File(context.cacheDir, "camera_captures")
        if (!tempDir.exists()) tempDir.mkdirs()
        val tempFile = File(tempDir, "IMG_CAPTURE_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(tempFile, uri)
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, pdfFile.name)
            putExtra(Intent.EXTRA_TEXT, "Here is your PDF converted with Nova PDF Converter.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share PDF with...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun openPdf(context: Context, pdfFile: File) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(openIntent, "Open PDF with...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun savePdfToDownloads(context: Context, pdfFile: File, displayName: String): Uri? {
        val finalName = if (displayName.endsWith(".pdf", ignoreCase = true)) displayName else "$displayName.pdf"
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NovaPDF")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(pdfFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                return uri
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val novaDir = File(downloadsDir, "NovaPDF")
            if (!novaDir.exists()) novaDir.mkdirs()
            val destFile = File(novaDir, finalName)
            FileInputStream(pdfFile).use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return Uri.fromFile(destFile)
        }
        return null
    }

    fun savePdfToCustomUri(context: Context, pdfFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                FileInputStream(pdfFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getDrawableUri(context: Context, @DrawableRes drawableResId: Int, fileName: String): Uri {
        val samplesDir = File(context.cacheDir, "sample_images")
        if (!samplesDir.exists()) samplesDir.mkdirs()
        val file = File(samplesDir, fileName)
        if (!file.exists() || file.length() == 0L) {
            context.resources.openRawResource(drawableResId).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun loadSampleImageItems(context: Context): List<ImageItem> = withContext(Dispatchers.IO) {
        val samples = listOf(
            Triple(R.drawable.sample_invoice, "Invoice_Nexus_Solutions.jpg", "Business Invoice Document"),
            Triple(R.drawable.sample_landscape, "Rocky_Mountains_Lake.jpg", "Mountain Scenery Photo"),
            Triple(R.drawable.sample_receipt, "Fresh_Mart_Receipt.jpg", "Store Cashier Receipt")
        )

        samples.map { (resId, fileName, displayName) ->
            val uri = getDrawableUri(context, resId, fileName)
            val file = File(File(context.cacheDir, "sample_images"), fileName)
            ImageItem(
                id = UUID.randomUUID().toString(),
                uri = uri,
                name = displayName,
                sizeBytes = if (file.exists()) file.length() else 350 * 1024L
            )
        }
    }

    suspend fun seedSamplePhotosToMediaStore(context: Context) = withContext(Dispatchers.IO) {
        try {
            val samples = listOf(
                Triple(R.drawable.sample_invoice, "Invoice_Nexus_Solutions.jpg", "image/jpeg"),
                Triple(R.drawable.sample_landscape, "Rocky_Mountains_Lake.jpg", "image/jpeg"),
                Triple(R.drawable.sample_receipt, "Fresh_Mart_Receipt.jpg", "image/jpeg")
            )
            val resolver = context.contentResolver

            for ((resId, name, mime) in samples) {
                // Check if already in MediaStore
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(name)

                val exists = resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { it.count > 0 } ?: false

                if (!exists) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, mime)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NovaPDF")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (itemUri != null) {
                        resolver.openOutputStream(itemUri)?.use { outStream ->
                            context.resources.openRawResource(resId).use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(itemUri, values, null, null)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fail silently if MediaStore is restricted
        }
    }
}
