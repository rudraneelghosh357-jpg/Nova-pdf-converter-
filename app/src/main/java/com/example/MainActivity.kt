package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConversionResult
import com.example.model.ImageItem
import com.example.model.PdfReaderUiState
import com.example.model.ScanFilter
import com.example.ui.components.ContinuousCameraScreen
import com.example.ui.components.ConvertButtonSection
import com.example.ui.components.ConvertingDialog
import com.example.ui.components.CropScreen
import com.example.ui.components.FilterScreen
import com.example.ui.components.ImageGridSection
import com.example.ui.components.NovaHeader
import com.example.ui.components.PdfReaderScreen
import com.example.ui.components.PdfSettingsCard
import com.example.ui.components.ResultSection
import com.example.ui.components.SplashScreen
import com.example.ui.components.UploadSection
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NovaCyan
import com.example.viewmodel.NovaPdfViewModel
import com.example.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.components.ScrollDownHint

class MainActivity : ComponentActivity() {

    private val viewModel: NovaPdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize AndroidX SplashScreen API
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for incoming intent to open PDF
        handleIncomingPdfIntent(intent)

        setContent {
            MyApplicationTheme {
                var isSplashActive by remember { mutableStateOf(true) }
                val context = LocalContext.current
                val pdfReaderState by viewModel.pdfReaderState.collectAsStateWithLifecycle()
                val isCameraActive by viewModel.isCameraActive.collectAsStateWithLifecycle()
                val activeCropItem by viewModel.activeCropItem.collectAsStateWithLifecycle()
                val activeFilterItem by viewModel.activeFilterItem.collectAsStateWithLifecycle()

                // Splash screen 2-second timer then smooth fade-out and open pending intent if any
                LaunchedEffect(Unit) {
                    delay(2000L)
                    isSplashActive = false
                    viewModel.pendingOpenUri.value?.let { uri ->
                        viewModel.openPdfForViewing(context, uri)
                        viewModel.setPendingOpenUri(null)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NovaPdfApp(
                        viewModel = viewModel,
                        onOpenPdfRequest = { uri ->
                            viewModel.openPdfForViewing(context, uri)
                        }
                    )

                    // Continuous Camera Overlay
                    if (isCameraActive) {
                        ContinuousCameraScreen(
                            onFinished = { uris ->
                                viewModel.addImages(context, uris)
                                viewModel.setCameraActive(false)
                            },
                            onFilterShot = { uri ->
                                val tempItem = ImageItem(
                                    id = "temp_${System.currentTimeMillis()}",
                                    uri = uri,
                                    name = "Camera Scan",
                                    sizeBytes = 0
                                )
                                viewModel.openFilter(tempItem)
                            },
                            onCancel = {
                                viewModel.setCameraActive(false)
                            }
                        )
                    }

                    // Crop Screen Overlay
                    if (activeCropItem != null) {
                        CropScreen(
                            imageUri = activeCropItem!!.uri,
                            onCropDone = { newUri ->
                                viewModel.updateCroppedImage(context, activeCropItem!!.id, newUri)
                            },
                            onCancel = {
                                viewModel.closeCrop()
                            }
                        )
                    }

                    // Filter Screen Overlay
                    if (activeFilterItem != null) {
                        FilterScreen(
                            item = activeFilterItem!!,
                            onApply = { item, applyToAll ->
                                viewModel.applyFilter(item, applyToAll)
                            },
                            onCancel = {
                                viewModel.closeFilter()
                            }
                        )
                    }

                    // PDF Reader Fullscreen Overlay
                    if (pdfReaderState !is PdfReaderUiState.Closed) {
                        PdfReaderScreen(
                            state = pdfReaderState,
                            onClose = { viewModel.closePdfReader() }
                        )
                    }

                    // Splash Screen Overlay with Smooth Fade Out
                    AnimatedVisibility(
                        visible = isSplashActive,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        SplashScreen()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingPdfIntent(intent)
    }

    private fun handleIncomingPdfIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {
                        // Not all content URIs support persistable permissions
                    }
                }
                viewModel.setPendingOpenUri(uri)
                // If app is already running past splash screen, open immediately
                if (viewModel.pdfReaderState.value is PdfReaderUiState.Closed) {
                    viewModel.openPdfForViewing(this, uri)
                    viewModel.setPendingOpenUri(null)
                }
            }
        }
    }
}

@Composable
fun NovaPdfApp(
    viewModel: NovaPdfViewModel,
    onOpenPdfRequest: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val images by viewModel.images.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // PDF file picker launcher for "Open PDF" button
    val openPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            onOpenPdfRequest(it)
        }
    }

    // Auto-seed sample photos to MediaStore so the system Photo Picker shows device photos
    LaunchedEffect(Unit) {
        viewModel.seedDeviceGallery(context)
    }

    // Intercept back press when in result view to let user return to editing
    BackHandler(enabled = uiState is UiState.Success) {
        viewModel.keepImagesForNewConversion()
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var hintDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 150) {
            hintDismissed = true
        }
    }
    LaunchedEffect(images.isEmpty()) {
        if (images.isEmpty()) hintDismissed = false
    }

    val showScrollHint by remember {
        derivedStateOf {
            images.isNotEmpty() &&
                uiState !is UiState.Success &&
                !hintDismissed &&
                listState.firstVisibleItemIndex == 0 &&
                listState.canScrollForward
        }
    }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("nova_pdf_main_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NovaHeader(
                onOpenPdfClick = {
                    openPdfPickerLauncher.launch(arrayOf("application/pdf"))
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .navigationBarsPadding()
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
                        // 1. Upload, Continuous Camera, and Open PDF selection
                        item {
                            UploadSection(
                                onImagesSelected = { uris -> viewModel.addImages(context, uris) },
                                onCameraClick = { viewModel.setCameraActive(true) },
                                onLoadSampleImages = { viewModel.loadSampleImages(context) },
                                onOpenPdf = {
                                    openPdfPickerLauncher.launch(arrayOf("application/pdf"))
                                }
                            )
                        }

                        // 2. Thumbnails & Reordering / Rotation / Crop (if images exist)
                        if (images.isNotEmpty()) {
                            item {
                                ImageGridSection(
                                    images = images,
                                    onRotateImage = { id -> viewModel.rotateImage(id) },
                                    onCropImage = { id ->
                                        images.find { it.id == id }?.let { viewModel.openCrop(it) }
                                    },
                                    onFilterImage = { id ->
                                        images.find { it.id == id }?.let { viewModel.openFilter(it) }
                                    },
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
                                    onLoadSampleImages = { viewModel.loadSampleImages(context) },
                                    onOpenPdf = {
                                        openPdfPickerLauncher.launch(arrayOf("application/pdf"))
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            ScrollDownHint(
                visible = showScrollHint,
                onClick = { scope.launch { listState.animateScrollToItem(2) } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
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
    onOpenPdf: () -> Unit,
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
                description = "Choose photos or continuous multi-shot camera scans in JPG, PNG, WEBP."
            )

            Spacer(modifier = Modifier.height(12.dp))

            GuideStep(
                icon = Icons.Default.CropRotate,
                stepNumber = "2",
                title = "Crop, Rotate & Reorder",
                description = "Crop images, rotate 90°, and rearrange pages to your preference."
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
                title = "Instant On-Device PDF & Reader",
                description = "Generates clean PDFs with reserved footers, or view any PDF from your device."
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onLoadSampleImages,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("empty_state_load_samples_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Try Samples",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                FilledTonalButton(
                    onClick = onOpenPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("empty_state_open_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Open PDF",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
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
