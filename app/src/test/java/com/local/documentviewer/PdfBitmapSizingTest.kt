package com.local.documentviewer

import com.local.documentviewer.rendering.PdfBitmapSizing
import org.junit.Assert.*
import org.junit.Test

class PdfBitmapSizingTest {
    @Test fun normalPortraitPageKeepsAspectRatio() {val s=PdfBitmapSizing.calculate(612,792,1080);assertTrue(s.height>1000);assertEquals(792.0/612,s.height.toDouble()/s.width,.002)}
    @Test fun landscapePageKeepsAspectRatio() {val s=PdfBitmapSizing.calculate(792,612,1080);assertTrue(s.width>s.height);assertEquals(612.0/792,s.height.toDouble()/s.width,.002)}
    @Test fun hugeZoomStaysWithinMemoryBudget() {
        for((w,h) in listOf(612 to 792,792 to 612,1 to 100000,100000 to 1,Int.MAX_VALUE to Int.MAX_VALUE)) {
            val s=PdfBitmapSizing.calculate(w,h,Int.MAX_VALUE)
            assertTrue(s.width>0&&s.height>0);assertTrue(s.width.toLong()*s.height<=PdfBitmapSizing.MAX_PIXELS)
            assertTrue(s.width<=PdfBitmapSizing.MAX_EDGE&&s.height<=PdfBitmapSizing.MAX_EDGE)
        }
    }
    @Test(expected=IllegalArgumentException::class) fun rejectInvalidPageGeometry() {PdfBitmapSizing.calculate(0,792,1080)}
}
