package com.local.documentviewer

/** Android's extension table varies by OS release and often doesn't recognize ES modules. */
object AssetMimeTypes {
    fun forPath(path: String?): String? = when(path?.substringAfterLast('.')?.lowercase(java.util.Locale.ROOT)) {
        "mjs", "js" -> "application/javascript"
        "wasm" -> "application/wasm"
        "css" -> "text/css"
        "html" -> "text/html"
        else -> null
    }
}
