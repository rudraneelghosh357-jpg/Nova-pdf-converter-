package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.ImageItem
import com.example.model.ImageQuality
import com.example.model.PageMargin
import com.example.model.PageOrientation
import com.example.model.PageSizeOption
import com.example.model.PdfSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nova PDF Converter", appName)
    }

    @Test
    fun `imageItem rotation cycles correctly`() {
        val uri = Uri.parse("content://media/external/images/media/1")
        val item = ImageItem(
            id = "test-1",
            uri = uri,
            name = "photo.jpg",
            sizeBytes = 2048,
            rotationDegrees = 0
        )

        val rotated90 = item.rotatedClockwise()
        assertEquals(90, rotated90.rotationDegrees)

        val rotated180 = rotated90.rotatedClockwise()
        assertEquals(180, rotated180.rotationDegrees)

        val rotated270 = rotated180.rotatedClockwise()
        assertEquals(270, rotated270.rotationDegrees)

        val rotated360 = rotated270.rotatedClockwise()
        assertEquals(0, rotated360.rotationDegrees)
    }

    @Test
    fun `pdfSettings defaults are sensible`() {
        val settings = PdfSettings()
        assertEquals(PageSizeOption.A4, settings.pageSize)
        assertEquals(PageOrientation.AUTO, settings.orientation)
        assertEquals(PageMargin.SMALL, settings.margin)
        assertEquals(ImageQuality.MEDIUM, settings.quality)
        assertEquals("", settings.customFileName)
    }

    @Test
    fun `imageItem formattedSize calculates KB and MB`() {
        val uri = Uri.parse("content://test/img")
        val itemKb = ImageItem("id1", uri, "img.png", 512 * 1024)
        assertEquals("512 KB", itemKb.formattedSize)

        val itemMb = ImageItem("id2", uri, "img2.png", (2.5 * 1024 * 1024).toLong())
        assertEquals("2.5 MB", itemMb.formattedSize)
    }
}
