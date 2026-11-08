package com.local.documentviewer

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.local.documentviewer.rendering.NativePdfSession
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativePdfRendererTest {
    private suspend fun withPdf(name: String,block: suspend (NativePdfSession)->Unit) {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val file=File.createTempFile("native-pdf-test-", ".pdf",instrumentation.targetContext.cacheDir)
        var session: NativePdfSession?=null
        try {
            instrumentation.context.assets.open(name).use {input->file.outputStream().use {input.copyTo(it)}}
            val opened=NativePdfSession.open(file);session=opened
            block(opened)
        } finally {session?.close();file.delete()}
    }
    private fun assertVisibleContent(bitmap: Bitmap) {
        assertTrue(bitmap.width>100);assertTrue(bitmap.height>100)
        val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        assertTrue("Native page must contain actual ink, not a blank image",pixels.count {(it and 0x00ffffff) < 0x00dddddd}>20)
    }
    @Test fun rendersSelectedPdfWithoutWebView()=runBlocking(Dispatchers.IO) {
        withPdf("native-sample.pdf") {session->
            assertEquals(3,session.pageCount)
            val bitmap=session.render(0,720)
            try {assertVisibleContent(bitmap);assertTrue(bitmap.height>bitmap.width)}finally{bitmap.recycle()}
        }
    }
    @Test fun rendersRotatedPagesAndUserUnits()=runBlocking(Dispatchers.IO) {
        withPdf("native-rotations.pdf") {session->
            assertEquals(4,session.pageCount)
            for(index in 0 until session.pageCount){val bitmap=session.render(index,720);try {assertVisibleContent(bitmap)}finally {bitmap.recycle()}}
        }
    }
    @Test fun serializesParallelPageRequests()=runBlocking(Dispatchers.IO) {
        withPdf("native-sample.pdf") {session->
            val bitmaps=(0..5).map {index->async {session.render(index%session.pageCount,320)}}.awaitAll()
            try {bitmaps.forEach(::assertVisibleContent)}finally{bitmaps.forEach {it.recycle()}}
        }
    }
}
