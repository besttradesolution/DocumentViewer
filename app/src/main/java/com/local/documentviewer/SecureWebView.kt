package com.local.documentviewer

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import com.local.documentviewer.rendering.RendererRegistry

@SuppressLint("SetJavaScriptEnabled")
fun secureWebView(context: Context, document: OpenDocument, dark: Boolean=false, onFailure: (String)->Unit): WebView {
    val loader=WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context)).build()
    val docPath="/document/${document.token}"
    return WebView(context).apply {
        settings.apply {
            javaScriptEnabled=true; domStorageEnabled=false; allowFileAccess=false; allowContentAccess=false
            blockNetworkLoads=true; mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically=false; setSupportMultipleWindows(false)
            setSupportZoom(true); builtInZoomControls=true; displayZoomControls=false
            mediaPlaybackRequiresUserGesture=true; cacheMode=WebSettings.LOAD_NO_CACHE
        }
        CookieManager.getInstance().setAcceptCookie(false)
        webViewClient=object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest)=true
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                view.post { onFailure("The document renderer stopped. The file may be too complex for this device.") }
                return true
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                val uri=request.url
                if(request.method == "GET" && uri.scheme=="https" && uri.host=="appassets.androidplatform.net" && uri.port == -1) {
                    if(uri.path==docPath) return runCatching {
                        WebResourceResponse(if(document.type=="pdf") "application/pdf" else "application/octet-stream", null, 200, "OK",
                            mapOf("Cache-Control" to "no-store", "Content-Length" to document.file.length().toString(), "Access-Control-Allow-Origin" to "*"), document.file.inputStream())
                    }.getOrElse { blockedResponse() }
                    if(uri.path?.startsWith("/assets/")==true) loader.shouldInterceptRequest(uri)?.let { response ->
                        // Modules are subject to strict MIME validation. AssetsPathHandler's OS-dependent
                        // guess can be text/plain for .mjs, which prevents PDF.js and its worker loading.
                        AssetMimeTypes.forPath(uri.path)?.let { response.mimeType=it }
                        response.responseHeaders=(response.responseHeaders ?: emptyMap()) + mapOf("X-Content-Type-Options" to "nosniff", "Cache-Control" to "no-store")
                        return response
                    }
                }
                return blockedResponse()
            }
        }
        webChromeClient=object: WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) { callback.invoke(origin,false,false) }
        }
        setDownloadListener { _,_,_,_,_ -> }
        loadUrl(RendererRegistry.forType(document.type).viewerUrl(docPath)+"&theme="+if(dark) "dark" else "light")
    }
}

private fun blockedResponse() = WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
