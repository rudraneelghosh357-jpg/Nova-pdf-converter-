package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConversionResult
import com.example.ui.components.ConvertButtonSection
import com.example.ui.components.ConvertingDialog
import com.example.ui.components.ImageGridSection
import com.example.ui.components.NovaHeader
import com.example.ui.components.PdfSettingsCard
import com.example.ui.components.ResultSection
import com.example.ui.components.UploadSection
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NovaCyan
import com.example.viewmodel.NovaPdfViewModel
import com.example.viewmodel.UiState

class MainActivity : ComponentActivity() {

    private val viewModel: NovaPdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NovaPdfApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NovaPdfApp(
    viewModel: NovaPdfViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val images by viewModel.images.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-seed sample photos to MediaStore so the system Photo Picker shows device photos
    LaunchedEffect(Unit) {
        viewModel.seedDeviceGallery(context)
    }

    // Intercept back press when in result view to let user return to editing
    BackHandler(enabled = uiState is UiState.Success) {
        viewModel.keepImagesForNewConversion()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("nova_pdf_main_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { NovaHeader() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (val state = uiState) {
                    is UiState.Success -> {
                        item {
                            ResultSection(
                                result = state.result,
                                onConvertMore = { viewModel.resetForMore() }
                            )
                        }
                    }
                    else -> {
                        // 1. Upload & Camera selection with Sample Images support
                        item {
                            UploadSection(
                                onImagesSelected = { uris -> viewModel.addImages(context, uris) },
                                onSingleImageCaptured = { uri -> viewModel.addSingleImage(context, uri) },
                                onLoadSampleImages = { viewModel.loadSampleImages(context) }
                            )
                        }

                        // 2. Thumbnails & Reordering / Rotation (if images exist)
                        if (images.isNotEmpty()) {
                            item {
                                ImageGridSection(
                                    images = images,
                                    onRotateImage = { id -> viewModel.rotateImage(id) },
                                    onMoveUp = { idx -> viewModel.moveImageUp(idx) },
                                    onMoveDown = { idx -> viewModel.moveImageDown(idx) },
                                    onDeleteImage = { id -> viewModel.removeImage(id) },
                                    onClearAll = { viewModel.clearAllImages() },
                                    onReverseOrder = { viewModel.reverseOrder() }
                                )
                            }

                            // 3. PDF Settings (Page size, orientation, margin, quality, filename)
                            item {
                                PdfSettingsCard(
                                    settings = settings,
                                    onPageSizeChange = { viewModel.updatePageSize(it) },
                                    onOrientationChange = { viewModel.updateOrientation(it) },
                                    onMarginChange = { viewModel.updateMargin(it) },
                                    onQualityChange = { viewModel.updateQuality(it) },
                                    onFileNameChange = { viewModel.updateFileName(it) }
                                )
                            }

                            // 4. Convert Button CTA
                            item {
                                ConvertButtonSection(
                                    imageCount = images.size,
                                    onConvertClick = { viewModel.convertToPdf(context) }
                                )
                            }
                        } else {
                            // Empty State / Feature Highlights
                            item {
                                EmptyStateGuide(
                                    onLoadSampleImages = { viewModel.loadSampleImages(context) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Converting Progress Dialog
    if (uiState is UiState.Converting) {
        ConvertingDialog(convertingState = uiState as UiState.Converting)
    }

    // Error Dialog
    if (uiState is UiState.Error) {
        val errorState = uiState as UiState.Error
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conversion Notice")
                }
            },
            text = { Text(errorState.message) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissError() },
                    modifier = Modifier.testTag("dismiss_error_button")
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun EmptyStateGuide(
    onLoadSampleImages: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_state_guide_card"),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NovaCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How Nova PDF Works",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            GuideStep(
                icon = Icons.Default.PhotoLibrary,
                stepNumber = "1",
                title = "Select Any Images",
                description = "Choose photos or camera captures in JPG, PNG, WEBP, or HEIC."
            )

            Spacer(modifier = Modifier.height(12.dp))

            GuideStep(
                icon = Icons.Default.CropRotate,
                stepNumber = "2",
                title = "Rotate & Reorder",
                description = "Rotate individual pages 90° and rearrange the order to your preference."
            )

            Spacer(modifier = Modifier.height(12.dp))

            GuideStep(
                icon = Icons.Default.Compress,
                stepNumber = "3",
                title = "Format & Quality",
                description = "Customize page size (A4, Letter, Fit), orientation, margins & compression."
            )

            Spacer(modifier = Modifier.height(12.dp))

            GuideStep(
                icon = Icons.Default.PictureAsPdf,
                stepNumber = "4",
                title = "Instant On-Device PDF",
                description = "Generates a clean PDF with a reserved 'Created by Nova PDF Converter' footer."
            )

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onLoadSampleImages,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_state_load_samples_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Load Sample Images (Invoice, Scenery, Receipt)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun GuideStep(
    icon: ImageVector,
    stepNumber: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
