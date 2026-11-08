package com.local.documentviewer.rendering

import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

data class PdfBitmapSize(val width: Int, val height: Int)

/** Keeps page images bounded independently of PDF dimensions, device density and zoom. */
object PdfBitmapSizing {
    const val MAX_PIXELS = 2_000_000
    const val MAX_EDGE = 8192
    fun calculate(pageWidth: Int, pageHeight: Int, requestedWidth: Int): PdfBitmapSize {
        require(pageWidth>0 && pageHeight>0 && requestedWidth>0)
        val width=pageWidth.toDouble();val height=pageHeight.toDouble()
        val scale=min(requestedWidth/width,min(MAX_EDGE/width,min(MAX_EDGE/height,sqrt(MAX_PIXELS/(width*height)))))
        return PdfBitmapSize(floor(width*scale).toInt().coerceAtLeast(1),floor(height*scale).toInt().coerceAtLeast(1))
    }
}
