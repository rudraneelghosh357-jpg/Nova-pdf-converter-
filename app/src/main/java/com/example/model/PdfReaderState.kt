package com.example.model

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

sealed interface PdfReaderUiState {
    data object Closed : PdfReaderUiState
    data class Loading(val uri: Uri, val title: String) : PdfReaderUiState
    data class Viewing(
        val uri: Uri,
        val file: File,
        val title: String,
        val pageCount: Int,
        val pageDimensions: List<Pair<Int, Int>>
    ) : PdfReaderUiState
    data class Error(val uri: Uri, val title: String, val message: String) : PdfReaderUiState
}
