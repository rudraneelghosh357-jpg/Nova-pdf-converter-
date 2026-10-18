package com.example

import com.example.model.PageMargin
import com.example.model.PageSizeOption
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun pageSize_standardDimensions_areAccurate() {
        assertEquals(595, PageSizeOption.A4.widthPt)
        assertEquals(842, PageSizeOption.A4.heightPt)
        assertEquals(612, PageSizeOption.LETTER.widthPt)
        assertEquals(792, PageSizeOption.LETTER.heightPt)
    }

    @Test
    fun pageMargin_points_areAccurate() {
        assertEquals(0f, PageMargin.NONE.points, 0.01f)
        assertEquals(18f, PageMargin.SMALL.points, 0.01f)
        assertEquals(36f, PageMargin.MEDIUM.points, 0.01f)
    }
}
