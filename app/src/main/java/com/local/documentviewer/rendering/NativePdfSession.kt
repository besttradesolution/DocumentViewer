package com.local.documentviewer.rendering

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PdfPageSize(val width: Int,val height: Int)

/** Serializes access: PdfRenderer permits only one open page at a time. */
class NativePdfSession private constructor(private val renderer: PdfRenderer) {
    val pageCount=renderer.pageCount
    private val lock=Mutex()
    private var closed=false
    suspend fun size(index: Int): PdfPageSize = lock.withLock {
        check(!closed)
        renderer.openPage(index).use {PdfPageSize(it.width,it.height)}
    }
    suspend fun render(index: Int,requestedWidth: Int): Bitmap = lock.withLock {
        check(!closed)
        renderer.openPage(index).use {page ->
            val size=PdfBitmapSizing.calculate(page.width,page.height,requestedWidth)
            val bitmap=Bitmap.createBitmap(size.width,size.height,Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.WHITE)
                val matrix=Matrix().apply {setScale(size.width.toFloat()/page.width,size.height.toFloat()/page.height)}
                page.render(bitmap,null,matrix,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } catch(error: Throwable) {bitmap.recycle();throw error}
        }
    }
    suspend fun close(): Unit =lock.withLock {if(!closed){closed=true;renderer.close()};Unit}
    companion object {
        fun open(file: File): NativePdfSession {
            val descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                val renderer=PdfRenderer(descriptor)
                if(renderer.pageCount<1){renderer.close();error("The PDF has no pages.")}
                return NativePdfSession(renderer)
            }
            catch(error: Throwable){descriptor.close();throw error}
            // PdfRenderer owns and closes the descriptor after a successful open.
        }
    }
}
