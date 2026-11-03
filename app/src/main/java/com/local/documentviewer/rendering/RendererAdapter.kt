package com.local.documentviewer.rendering

import android.net.Uri

/** Native entry points to the independently bundled rendering engines. */
interface RendererAdapter {
    val format: String
    fun viewerUrl(documentPath: String): String = "https://appassets.androidplatform.net/assets/viewer.html?type=$format&doc=${Uri.encode(documentPath)}"
}
object PdfRendererAdapter: RendererAdapter { override val format="pdf" }
object WordRendererAdapter: RendererAdapter { override val format="docx" }
object ExcelRendererAdapter: RendererAdapter { override val format="xlsx" }
object PowerPointRendererAdapter: RendererAdapter { override val format="pptx" }
object TextRendererAdapter: RendererAdapter { override val format="txt" }
object CsvRendererAdapter: RendererAdapter { override val format="csv" }
object RendererRegistry {
    fun forType(type: String): RendererAdapter = when(type) {
        "pdf" -> PdfRendererAdapter; "docx" -> WordRendererAdapter; "xlsx" -> ExcelRendererAdapter
        "pptx" -> PowerPointRendererAdapter; "txt" -> TextRendererAdapter; "csv" -> CsvRendererAdapter
        else -> error("Unsupported document format.")
    }
}
