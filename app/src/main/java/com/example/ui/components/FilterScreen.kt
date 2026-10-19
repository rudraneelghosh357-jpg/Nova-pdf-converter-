package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ImageItem
import com.example.model.ScanFilter
import com.example.util.FilterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    item: ImageItem,
    onApply: (ImageItem, Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var filterMode by remember { mutableStateOf(item.filterMode) }
    var brightness by remember { mutableFloatStateOf(item.brightness) }
    var contrast by remember { mutableFloatStateOf(item.contrast) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(item.uri) {
        isLoading = true
        originalBitmap = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(item.uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) { null }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filters") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel") }
                },
                actions = {
                    var applyToAll by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Apply to all", style = MaterialTheme.typography.labelSmall)
                        Checkbox(checked = applyToAll, onCheckedChange = { applyToAll = it })
                    }
                    IconButton(onClick = {
                        onApply(item.copy(filterMode = filterMode, brightness = brightness, contrast = contrast), applyToAll)
                    }) { Icon(Icons.Default.Check, "Apply") }
                }
            )
        }
    ) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Preview
            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Gray)) {
                if (originalBitmap != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                colorFilter = FilterHelper.getColorFilter(filterMode, brightness, contrast)
                            }
                            canvas.nativeCanvas.drawBitmap(originalBitmap!!, 0f, 0f, paint)
                        }
                    }
                } else if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Thumbnails
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ScanFilter.entries) { mode ->
                    FilterThumbnail(mode = mode, isSelected = filterMode == mode, onClick = { filterMode = mode })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sliders
            Text("Brightness")
            Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -100f..100f)
            Text("Contrast")
            Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.5f..2f)
        }
    }
}

@Composable
fun FilterThumbnail(mode: ScanFilter, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(mode.label, textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        }
    }
}
