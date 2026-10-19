package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import com.example.model.ConversionResult
import com.example.model.ImageItem
import com.example.model.ImageQuality
import com.example.model.PageMargin
import com.example.model.PageOrientation
import com.example.model.PageSizeOption
import com.example.model.PdfSettings
import com.example.model.ScanFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PdfGenerator {

    private const val FOOTER_TEXT = "Created by Nova PDF Converter"
    private const val FOOTER_RESERVED_HEIGHT_PT = 22f

    suspend fun generatePdf(
        context: Context,
        images: List<ImageItem>,
        settings: PdfSettings,
        onProgress: (current: Int, total: Int, message: String) -> Unit
    ): ConversionResult = withContext(Dispatchers.IO) {
        if (images.isEmpty()) {
            throw IllegalArgumentException("No images to convert")
        }

        val pdfDir = File(context.cacheDir, "generated_pdfs")
        if (!pdfDir.exists()) pdfDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val rawName = if (settings.customFileName.isNotBlank()) {
            settings.customFileName.trim()
        } else {
            "Nova_$timeStamp"
        }
        val safeName = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val finalFileName = if (safeName.endsWith(".pdf", ignoreCase = true)) safeName else "$safeName.pdf"
        val outputFile = File(pdfDir, finalFileName)

        val document = PdfDocument()
        val totalImages = images.size

        try {
            for (index in images.indices) {
                yield()
                val imageItem = images[index]
                onProgress(index + 1, totalImages, "Processing image ${index + 1} of $totalImages...")

                // 1. Decode & rotate bitmap efficiently
                val processedBitmap = loadAndProcessBitmap(context, imageItem, settings.quality)

                try {
                    // 2. Determine Page Dimensions in points (72 pt/inch)
                    val (pageWidth, pageHeight) = calculatePageDimensions(
                        processedBitmap.width,
                        processedBitmap.height,
                        settings.pageSize,
                        settings.orientation,
                        settings.margin
                    )

                    // 3. Create PDF Page
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        pageWidth.toInt(),
                        pageHeight.toInt(),
                        index + 1
                    ).create()

                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    // 4. Draw Crisp White Page Background
                    canvas.drawColor(android.graphics.Color.WHITE)

                    // 5. Calculate Image Positioning without overlapping the reserved footer
                    val marginPt = settings.margin.points
                    val availableWidth = pageWidth - (2 * marginPt)
                    val availableHeight = pageHeight - (2 * marginPt) - FOOTER_RESERVED_HEIGHT_PT

                    val scale = min(
                        availableWidth / processedBitmap.width.toFloat(),
                        availableHeight / processedBitmap.height.toFloat()
                    )

                    val drawnWidth = processedBitmap.width * scale
                    val drawnHeight = processedBitmap.height * scale

                    // Center within the reserved image area
                    val leftOffset = marginPt + (availableWidth - drawnWidth) / 2f
                    val topOffset = marginPt + (availableHeight - drawnHeight) / 2f

                    val bitmapPaint = Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                        isDither = true
                    }

                    val destRect = RectF(
                        leftOffset,
                        topOffset,
                        leftOffset + drawnWidth,
                        topOffset + drawnHeight
                    )
                    canvas.drawBitmap(processedBitmap, null, destRect, bitmapPaint)

                    // 6. Draw MANDATORY Footer: "Created by Nova PDF Converter" at bottom right
                    val footerPaint = Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.rgb(128, 128, 128)
                        textSize = 8.5f
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    }

                    val textWidth = footerPaint.measureText(FOOTER_TEXT)
                    val textRightPadding = max(marginPt, 14f)
                    val textBottomPadding = max(marginPt, 8f)

                    val textX = pageWidth - textRightPadding - textWidth
                    val textY = pageHeight - textBottomPadding - 2f

                    canvas.drawText(FOOTER_TEXT, textX, textY, footerPaint)

                    document.finishPage(page)
                } finally {
                    processedBitmap.recycle()
                }
            }

            onProgress(totalImages, totalImages, "Writing PDF document to disk...")
            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }
        } finally {
            document.close()
        }

        val previews = PdfPreviewHelper.renderPreviews(outputFile, maxPages = 6)

        ConversionResult(
            file = outputFile,
            fileName = finalFileName,
            pageCount = totalImages,
            fileSizeBytes = outputFile.length(),
            previewBitmaps = previews
        )
    }

    private fun calculatePageDimensions(
        bitmapWidth: Int,
        bitmapHeight: Int,
        pageSizeOption: PageSizeOption,
        orientation: PageOrientation,
        margin: PageMargin
    ): Pair<Float, Float> {
        val m = margin.points

        return when (pageSizeOption) {
            PageSizeOption.A4 -> {
                val baseW = 595f
                val baseH = 842f
                applyOrientation(baseW, baseH, bitmapWidth, bitmapHeight, orientation)
            }
            PageSizeOption.LETTER -> {
                val baseW = 612f
                val baseH = 792f
                applyOrientation(baseW, baseH, bitmapWidth, bitmapHeight, orientation)
            }
            PageSizeOption.FIT_TO_IMAGE -> {
                // Fit to Image: standardize width to 595pt and dynamically adjust height
                val targetW = 595f
                val availableW = targetW - (2 * m)
                val aspect = bitmapHeight.toFloat() / bitmapWidth.toFloat()
                val imageH = availableW * aspect
                val targetH = imageH + (2 * m) + FOOTER_RESERVED_HEIGHT_PT
                Pair(targetW, targetH)
            }
        }
    }

    private fun applyOrientation(
        baseW: Float,
        baseH: Float,
        bitmapWidth: Int,
        bitmapHeight: Int,
        orientation: PageOrientation
    ): Pair<Float, Float> {
        val minDim = min(baseW, baseH)
        val maxDim = max(baseW, baseH)

        return when (orientation) {
            PageOrientation.PORTRAIT -> Pair(minDim, maxDim)
            PageOrientation.LANDSCAPE -> Pair(maxDim, minDim)
            PageOrientation.AUTO -> {
                if (bitmapWidth > bitmapHeight) {
                    Pair(maxDim, minDim)
                } else {
                    Pair(minDim, maxDim)
                }
            }
        }
    }

    private fun loadAndProcessBitmap(
        context: Context,
        imageItem: ImageItem,
        quality: ImageQuality
    ): Bitmap {
        // 1. Measure dimensions without full loading
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(imageItem.uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val rawW = boundsOptions.outWidth
        val rawH = boundsOptions.outHeight

        // 2. Compute inSampleSize
        val maxDim = quality.maxDimension
        var sampleSize = 1
        var currW = rawW
        var currH = rawH
        while (currW / 2 >= maxDim || currH / 2 >= maxDim) {
            currW /= 2
            currH /= 2
            sampleSize *= 2
        }

        // 3. Decode scaled
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodedBitmap: Bitmap = context.contentResolver.openInputStream(imageItem.uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Unable to decode image: ${imageItem.name}")

        // 4. Read EXIF rotation
        var exifDegrees = 0
        try {
            context.contentResolver.openInputStream(imageItem.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                exifDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {
            // Ignore EXIF errors
        }

        val totalRotation = (imageItem.rotationDegrees + exifDegrees) % 360

        // 5. Apply Rotation & MaxDimension Scaling
        val matrix = Matrix()
        if (totalRotation != 0) {
            matrix.postRotate(totalRotation.toFloat())
        }

        val transformedBitmap = if (totalRotation != 0) {
            val rotated = Bitmap.createBitmap(
                decodedBitmap,
                0,
                0,
                decodedBitmap.width,
                decodedBitmap.height,
                matrix,
                true
            )
            if (rotated != decodedBitmap) {
                decodedBitmap.recycle()
            }
            rotated
        } else {
            decodedBitmap
        }

        // Apply Filter
        val finalBitmap = if (imageItem.filterMode != com.example.model.ScanFilter.ORIGINAL || imageItem.brightness != 0f || imageItem.contrast != 1f) {
            val paint = Paint().apply {
                colorFilter = FilterHelper.getColorFilter(imageItem.filterMode, imageItem.brightness, imageItem.contrast)
            }
            val filteredBitmap = Bitmap.createBitmap(transformedBitmap.width, transformedBitmap.height, Bitmap.Config.ARGB_8888)
            Canvas(filteredBitmap).drawBitmap(transformedBitmap, 0f, 0f, paint)
            transformedBitmap.recycle()
            filteredBitmap
        } else {
            transformedBitmap
        }

        // 6. Compress with quality setting to optimize PDF size
        if (quality.compressionQuality < 90) {
            val byteStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality.compressionQuality, byteStream)
            val compressedBytes = byteStream.toByteArray()
            val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
            finalBitmap.recycle()
            return compressedBitmap
        }

        return finalBitmap
    }
}
