package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NovaCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class AspectRatioPreset(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    PORTRAIT_4_3("4:3", 3f / 4f),
    A4("A4", 1f / 1.414f)
}

private enum class DragMode {
    NONE, MOVE_ALL, TL, TR, BL, BR, TOP, BOTTOM, LEFT, RIGHT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    imageUri: Uri,
    onCropDone: (Uri) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var selectedPreset by remember { mutableStateOf(AspectRatioPreset.FREE) }
    var isLoading by remember { mutableStateOf(true) }

    // Normalized coordinates (0f to 1f) relative to bitmap bounds
    var cropLeft by remember { mutableFloatStateOf(0.1f) }
    var cropTop by remember { mutableFloatStateOf(0.1f) }
    var cropRight by remember { mutableFloatStateOf(0.9f) }
    var cropBottom by remember { mutableFloatStateOf(0.9f) }

    var dragMode by remember { mutableStateOf(DragMode.NONE) }

    BackHandler {
        onCancel()
    }

    LaunchedEffect(imageUri) {
        isLoading = true
        val bmp = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }
        originalBitmap = bmp
        displayedBitmap = bmp
        isLoading = false
    }

    fun rotateBitmap() {
        originalBitmap?.let { bmp ->
            rotationAngle = (rotationAngle + 90f) % 360f
            val matrix = Matrix().apply { postRotate(90f) }
            displayedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            cropLeft = 0.1f
            cropTop = 0.1f
            cropRight = 0.9f
            cropBottom = 0.9f
        }
    }

    fun resetCrop() {
        cropLeft = 0.05f
        cropTop = 0.05f
        cropRight = 0.95f
        cropBottom = 0.95f
        selectedPreset = AspectRatioPreset.FREE
    }

    fun applyPreset(preset: AspectRatioPreset) {
        selectedPreset = preset
        if (preset.ratio != null) {
            val width = 0.8f
            val height = width / preset.ratio
            cropLeft = 0.1f
            cropTop = ((1f - height) / 2f).coerceIn(0.05f, 0.8f)
            cropRight = (cropLeft + width).coerceAtMost(0.95f)
            cropBottom = (cropTop + height).coerceAtMost(0.95f)
        } else {
            resetCrop()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Crop Image", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("crop_cancel_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(onClick = { rotateBitmap() }, modifier = Modifier.testTag("crop_rotate_button")) {
                        Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Rotate 90°")
                    }
                    IconButton(onClick = { resetCrop() }, modifier = Modifier.testTag("crop_reset_button")) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Crop")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Aspect ratio preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AspectRatioPreset.entries.forEach { preset ->
                            val isSelected = selectedPreset == preset
                            FilledTonalButton(
                                onClick = { applyPreset(preset) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NovaCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Done Button
                    Button(
                        onClick = {
                            displayedBitmap?.let { bmp ->
                                val x = (cropLeft * bmp.width).toInt().coerceIn(0, bmp.width - 10)
                                val y = (cropTop * bmp.height).toInt().coerceIn(0, bmp.height - 10)
                                val w = ((cropRight - cropLeft) * bmp.width).toInt().coerceIn(10, bmp.width - x)
                                val h = ((cropBottom - cropTop) * bmp.height).toInt().coerceIn(10, bmp.height - y)

                                try {
                                    val croppedBmp = Bitmap.createBitmap(bmp, x, y, w, h)
                                    val cacheDir = File(context.cacheDir, "cropped_images")
                                    if (!cacheDir.exists()) cacheDir.mkdirs()
                                    val file = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(file).use { out ->
                                        croppedBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                    }
                                    onCropDone(Uri.fromFile(file))
                                } catch (_: Exception) {
                                    onCropDone(imageUri)
                                }
                            } ?: onCancel()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("crop_done_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Crop", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF090D16)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading || displayedBitmap == null) {
                CircularProgressIndicator(color = NovaCyan)
            } else {
                val bitmap = displayedBitmap!!
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Crop Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )

                    // Fully adjustable touch crop canvas with corner & edge handles
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(selectedPreset) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val canvasW = size.width.toFloat()
                                        val canvasH = size.height.toFloat()
                                        if (canvasW <= 0f || canvasH <= 0f) return@detectDragGestures

                                        val bmpW = bitmap.width.toFloat()
                                        val bmpH = bitmap.height.toFloat()
                                        val scale = minOf(canvasW / bmpW, canvasH / bmpH)
                                        val drawW = bmpW * scale
                                        val drawH = bmpH * scale
                                        val startX = (canvasW - drawW) / 2f
                                        val startY = (canvasH - drawH) / 2f

                                        val leftPx = startX + cropLeft * drawW
                                        val topPx = startY + cropTop * drawH
                                        val rightPx = startX + cropRight * drawW
                                        val bottomPx = startY + cropBottom * drawH

                                        val touchTolerance = 60f // generous touch radius for handles

                                        dragMode = when {
                                            // Corners
                                            (offset - Offset(leftPx, topPx)).getDistance() < touchTolerance -> DragMode.TL
                                            (offset - Offset(rightPx, topPx)).getDistance() < touchTolerance -> DragMode.TR
                                            (offset - Offset(leftPx, bottomPx)).getDistance() < touchTolerance -> DragMode.BL
                                            (offset - Offset(rightPx, bottomPx)).getDistance() < touchTolerance -> DragMode.BR
                                            // Edges
                                            kotlin.math.abs(offset.x - leftPx) < touchTolerance && offset.y in topPx..bottomPx -> DragMode.LEFT
                                            kotlin.math.abs(offset.x - rightPx) < touchTolerance && offset.y in topPx..bottomPx -> DragMode.RIGHT
                                            kotlin.math.abs(offset.y - topPx) < touchTolerance && offset.x in leftPx..rightPx -> DragMode.TOP
                                            kotlin.math.abs(offset.y - bottomPx) < touchTolerance && offset.x in leftPx..rightPx -> DragMode.BOTTOM
                                            // Inside box -> move all
                                            offset.x in leftPx..rightPx && offset.y in topPx..bottomPx -> DragMode.MOVE_ALL
                                            else -> DragMode.NONE
                                        }
                                    },
                                    onDragEnd = { dragMode = DragMode.NONE },
                                    onDragCancel = { dragMode = DragMode.NONE },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val canvasW = size.width.toFloat()
                                        val canvasH = size.height.toFloat()
                                        if (canvasW <= 0f || canvasH <= 0f || dragMode == DragMode.NONE) return@detectDragGestures

                                        val bmpW = bitmap.width.toFloat()
                                        val bmpH = bitmap.height.toFloat()
                                        val scale = minOf(canvasW / bmpW, canvasH / bmpH)
                                        val drawW = bmpW * scale
                                        val drawH = bmpH * scale

                                        val dx = dragAmount.x / drawW
                                        val dy = dragAmount.y / drawH
                                        val minSize = 0.15f

                                        when (dragMode) {
                                            DragMode.MOVE_ALL -> {
                                                val width = cropRight - cropLeft
                                                val height = cropBottom - cropTop
                                                var newLeft = (cropLeft + dx).coerceIn(0f, 1f - width)
                                                var newTop = (cropTop + dy).coerceIn(0f, 1f - height)
                                                cropLeft = newLeft
                                                cropTop = newTop
                                                cropRight = newLeft + width
                                                cropBottom = newTop + height
                                            }
                                            DragMode.TL -> {
                                                if (selectedPreset.ratio == null) {
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                                } else {
                                                    val ratio = selectedPreset.ratio!!
                                                    val newL = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                                    val newW = cropRight - newL
                                                    val newH = newW * ratio
                                                    if (cropBottom - newH >= 0f) {
                                                        cropLeft = newL
                                                        cropTop = cropBottom - newH
                                                    }
                                                }
                                            }
                                            DragMode.TR -> {
                                                if (selectedPreset.ratio == null) {
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                                    cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                                } else {
                                                    val ratio = selectedPreset.ratio!!
                                                    val newR = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                                    val newW = newR - cropLeft
                                                    val newH = newW * ratio
                                                    if (cropBottom - newH >= 0f) {
                                                        cropRight = newR
                                                        cropTop = cropBottom - newH
                                                    }
                                                }
                                            }
                                            DragMode.BL -> {
                                                if (selectedPreset.ratio == null) {
                                                    cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                                } else {
                                                    val ratio = selectedPreset.ratio!!
                                                    val newL = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                                    val newW = cropRight - newL
                                                    val newH = newW * ratio
                                                    if (cropTop + newH <= 1f) {
                                                        cropLeft = newL
                                                        cropBottom = cropTop + newH
                                                    }
                                                }
                                            }
                                            DragMode.BR -> {
                                                if (selectedPreset.ratio == null) {
                                                    cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                                    cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                                } else {
                                                    val ratio = selectedPreset.ratio!!
                                                    val newR = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                                    val newW = newR - cropLeft
                                                    val newH = newW * ratio
                                                    if (cropTop + newH <= 1f) {
                                                        cropRight = newR
                                                        cropBottom = cropTop + newH
                                                    }
                                                }
                                            }
                                            DragMode.LEFT -> {
                                                cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                            }
                                            DragMode.RIGHT -> {
                                                cropRight = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                            }
                                            DragMode.TOP -> {
                                                cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                            }
                                            DragMode.BOTTOM -> {
                                                cropBottom = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                            }
                                            else -> {}
                                        }
                                    }
                                )
                            }
                    ) {
                        val canvasW = size.width
                        val canvasH = size.height

                        val bmpW = bitmap.width.toFloat()
                        val bmpH = bitmap.height.toFloat()
                        val scale = minOf(canvasW / bmpW, canvasH / bmpH)
                        val drawW = bmpW * scale
                        val drawH = bmpH * scale
                        val startX = (canvasW - drawW) / 2f
                        val startY = (canvasH - drawH) / 2f

                        val left = startX + cropLeft * drawW
                        val top = startY + cropTop * drawH
                        val right = startX + cropRight * drawW
                        val bottom = startY + cropBottom * drawH

                        // Dimmed overlay outside crop rect
                        val path = Path().apply {
                            addRect(Rect(0f, 0f, canvasW, canvasH))
                            addRect(Rect(left, top, right, bottom))
                        }
                        drawPath(path, color = Color.Black.copy(alpha = 0.7f))

                        // Crop border
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Rule of thirds grid lines
                        val thirdW = (right - left) / 3f
                        val thirdH = (bottom - top) / 3f
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(left + thirdW, top), Offset(left + thirdW, bottom), 1.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(left + 2 * thirdW, top), Offset(left + 2 * thirdW, bottom), 1.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(left, top + thirdH), Offset(right, top + thirdH), 1.dp.toPx())
                        drawLine(Color.White.copy(alpha = 0.5f), Offset(left, top + 2 * thirdH), Offset(right, top + 2 * thirdH), 1.dp.toPx())

                        // Corner handles (large touch targets / visual indicators)
                        val handleLength = 24.dp.toPx()
                        val handleStroke = 4.dp.toPx()
                        val handleColor = NovaCyan

                        // Top-Left corner handle
                        drawLine(handleColor, Offset(left, top), Offset(left + handleLength, top), handleStroke)
                        drawLine(handleColor, Offset(left, top), Offset(left, top + handleLength), handleStroke)

                        // Top-Right corner handle
                        drawLine(handleColor, Offset(right, top), Offset(right - handleLength, top), handleStroke)
                        drawLine(handleColor, Offset(right, top), Offset(right, top + handleLength), handleStroke)

                        // Bottom-Left corner handle
                        drawLine(handleColor, Offset(left, bottom), Offset(left + handleLength, bottom), handleStroke)
                        drawLine(handleColor, Offset(left, bottom), Offset(left, bottom - handleLength), handleStroke)

                        // Bottom-Right corner handle
                        drawLine(handleColor, Offset(right, bottom), Offset(right - handleLength, bottom), handleStroke)
                        drawLine(handleColor, Offset(right, bottom), Offset(right, bottom - handleLength), handleStroke)

                        // Edge handle indicator pills
                        val pillRadius = 6.dp.toPx()
                        drawCircle(handleColor, pillRadius, Offset((left + right) / 2f, top))
                        drawCircle(handleColor, pillRadius, Offset((left + right) / 2f, bottom))
                        drawCircle(handleColor, pillRadius, Offset(left, (top + bottom) / 2f))
                        drawCircle(handleColor, pillRadius, Offset(right, (top + bottom) / 2f))
                    }
                }
            }
        }
    }
}
