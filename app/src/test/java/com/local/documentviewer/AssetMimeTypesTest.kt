package com.local.documentviewer

import org.junit.Assert.*
import org.junit.Test

class AssetMimeTypesTest {
    @Test fun pdfModulesHaveJavaScriptMimeType() {
        for(path in listOf("/assets/vendor/pdf/pdf.mjs", "/assets/vendor/pdf/pdf.worker.mjs", "/assets/vendor/pdf/pdf_viewer.mjs", "/assets/viewer.js"))
            assertEquals("application/javascript", AssetMimeTypes.forPath(path))
    }
    @Test fun wasmIsBinaryRatherThanPlainText() { assertEquals("application/wasm",AssetMimeTypes.forPath("/assets/vendor/pdf/wasm/openjpeg.wasm")) }
    @Test fun unrelatedAssetsKeepTheirOriginalType() { assertNull(AssetMimeTypes.forPath("/assets/vendor/pdf/cmaps/UniJIS-UTF16-H.bcmap"));assertNull(AssetMimeTypes.forPath(null)) }
}
