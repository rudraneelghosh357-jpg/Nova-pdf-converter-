package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ConversionResult
import com.example.model.ImageItem
import com.example.model.ImageQuality
import com.example.model.PageMargin
import com.example.model.PageOrientation
import com.example.model.PageSizeOption
import com.example.model.PdfReaderUiState
import com.example.model.PdfSettings
import com.example.util.FileUtils
import com.example.util.PdfGenerator
import com.example.util.PdfReaderHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState {
    data object Idle : UiState
    data class Converting(
        val current: Int,
        val total: Int,
        val message: String,
        val progress: Float
    ) : UiState
    data class Success(val result: ConversionResult) : UiState
    data class Error(val message: String) : UiState
}

class NovaPdfViewModel : ViewModel() {

    private val _images = MutableStateFlow<List<ImageItem>>(emptyList())
    val images: StateFlow<List<ImageItem>> = _images.asStateFlow()

    private val _settings = MutableStateFlow(PdfSettings())
    val settings: StateFlow<PdfSettings> = _settings.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _pdfReaderState = MutableStateFlow<PdfReaderUiState>(PdfReaderUiState.Closed)
    val pdfReaderState: StateFlow<PdfReaderUiState> = _pdfReaderState.asStateFlow()

    private val _pendingOpenUri = MutableStateFlow<Uri?>(null)
    val pendingOpenUri: StateFlow<Uri?> = _pendingOpenUri.asStateFlow()

    fun setPendingOpenUri(uri: Uri?) {
        _pendingOpenUri.value = uri
    }

    fun openPdfForViewing(context: Context, uri: Uri) {
        val title = PdfReaderHelper.getFileName(context, uri)
        _pdfReaderState.value = PdfReaderUiState.Loading(uri, title)

        viewModelScope.launch {
            try {
                val (file, extractedTitle, dimensions) = PdfReaderHelper.preparePdfForViewing(context, uri)
                _pdfReaderState.value = PdfReaderUiState.Viewing(
                    uri = uri,
                    file = file,
                    title = extractedTitle,
                    pageCount = dimensions.size,
                    pageDimensions = dimensions
                )
            } catch (se: SecurityException) {
                _pdfReaderState.value = PdfReaderUiState.Error(
                    uri = uri,
                    title = title,
                    message = se.localizedMessage ?: "This PDF is password-protected and cannot be opened."
                )
            } catch (e: Exception) {
                _pdfReaderState.value = PdfReaderUiState.Error(
                    uri = uri,
                    title = title,
                    message = e.localizedMessage ?: "This PDF file is corrupted or unsupported."
                )
            }
        }
    }

    fun closePdfReader() {
        _pdfReaderState.value = PdfReaderUiState.Closed
    }

    fun addImages(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val newItems = uris.map { uri ->
                FileUtils.getImageItemFromUri(context, uri)
            }
            _images.update { current -> current + newItems }
        }
    }

    fun addSingleImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            val item = FileUtils.getImageItemFromUri(context, uri)
            _images.update { current -> current + item }
        }
    }

    fun loadSampleImages(context: Context) {
        viewModelScope.launch {
            val samples = FileUtils.loadSampleImageItems(context)
            _images.update { current -> current + samples }
        }
    }

    fun seedDeviceGallery(context: Context) {
        viewModelScope.launch {
            FileUtils.seedSamplePhotosToMediaStore(context)
        }
    }

    fun removeImage(id: String) {
        _images.update { list -> list.filterNot { it.id == id } }
    }

    fun rotateImage(id: String) {
        _images.update { list ->
            list.map { item ->
                if (item.id == id) item.rotatedClockwise() else item
            }
        }
    }

    fun moveImageUp(index: Int) {
        if (index <= 0) return
        _images.update { list ->
            val mutable = list.toMutableList()
            val temp = mutable[index]
            mutable[index] = mutable[index - 1]
            mutable[index - 1] = temp
            mutable
        }
    }

    fun moveImageDown(index: Int) {
        val current = _images.value
        if (index >= current.size - 1) return
        _images.update { list ->
            val mutable = list.toMutableList()
            val temp = mutable[index]
            mutable[index] = mutable[index + 1]
            mutable[index + 1] = temp
            mutable
        }
    }

    fun reverseOrder() {
        _images.update { it.reversed() }
    }

    fun clearAllImages() {
        _images.value = emptyList()
    }

    fun updatePageSize(option: PageSizeOption) {
        _settings.update { it.copy(pageSize = option) }
    }

    fun updateOrientation(orientation: PageOrientation) {
        _settings.update { it.copy(orientation = orientation) }
    }

    fun updateMargin(margin: PageMargin) {
        _settings.update { it.copy(margin = margin) }
    }

    fun updateQuality(quality: ImageQuality) {
        _settings.update { it.copy(quality = quality) }
    }

    fun updateFileName(name: String) {
        _settings.update { it.copy(customFileName = name) }
    }

    fun convertToPdf(context: Context) {
        val currentImages = _images.value
        if (currentImages.isEmpty()) {
            _uiState.value = UiState.Error("Please add at least one image before converting.")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Converting(
                current = 0,
                total = currentImages.size,
                message = "Preparing conversion...",
                progress = 0f
            )

            try {
                val result = PdfGenerator.generatePdf(
                    context = context.applicationContext,
                    images = currentImages,
                    settings = _settings.value,
                    onProgress = { current, total, message ->
                        val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
                        _uiState.value = UiState.Converting(
                            current = current,
                            total = total,
                            message = message,
                            progress = progress
                        )
                    }
                )
                _uiState.value = UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to convert images to PDF")
            }
        }
    }

    fun resetForMore() {
        _uiState.value = UiState.Idle
        _images.value = emptyList()
    }

    fun keepImagesForNewConversion() {
        _uiState.value = UiState.Idle
    }

    fun dismissError() {
        _uiState.value = UiState.Idle
    }
}
