package com.example.model

enum class PageSizeOption(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4", 595, 842),
    LETTER("Letter", 612, 792),
    FIT_TO_IMAGE("Fit to Image", 0, 0)
}

enum class PageOrientation(val label: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    AUTO("Auto")
}

enum class PageMargin(val label: String, val points: Float) {
    NONE("None", 0f),
    SMALL("Small", 18f), // 0.25 inch
    MEDIUM("Medium", 36f) // 0.5 inch
}

enum class ImageQuality(val label: String, val compressionQuality: Int, val maxDimension: Int) {
    LOW("Low", 50, 1280),
    MEDIUM("Medium", 75, 1920),
    HIGH("High", 95, 2560)
}

data class PdfSettings(
    val pageSize: PageSizeOption = PageSizeOption.A4,
    val orientation: PageOrientation = PageOrientation.AUTO,
    val margin: PageMargin = PageMargin.SMALL,
    val quality: ImageQuality = ImageQuality.MEDIUM,
    val customFileName: String = ""
)
