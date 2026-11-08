# 0.3.0 — Native Android PDF page view

The user confirmed that 0.2.1 still showed the PDF layout failure. Browser simulations and CSS fixes did not resolve their device's behavior, and the exact underlying WebView failure remains unconfirmed.

PDFs now default to Android's built-in PdfRenderer and Compose Image views. This path does not instantiate a WebView, use PDF.js's viewer layout, or rely on CSS for page dimensions. Pages render on Dispatchers.IO with serialized renderer access, load through a LazyColumn, and use bitmaps bounded to 2,000,000 pixels / 8192 pixels per edge. Session cleanup closes the renderer off the UI thread after pending work. Portrait/landscape page geometry comes from the platform renderer. The viewer offers scrolling, previous/next, page jumps, fit width, zoom buttons and two-finger pinch zoom.

Text selection, search and PDF.js password entry remain available through **Text & search**, which opens the existing alternate renderer. Native page view is image based and does not offer text selection/search. Password-protected files that the API-28-compatible native renderer cannot open show an explicit alternate-reader action. No additional runtime library, service, network permission or broad storage permission was added.

Version code 4, version name 0.3.0. The toolbar identifies **PDF · Page view** or **PDF · Text & search**, and Settings shows 0.3.0 so the installed build and active rendering path can be verified.

## Verification

- All main Kotlin source files compiled successfully with Kotlin 1.9.24, Compose compiler 1.5.14 and Android SDK 34 APIs, without building an APK.
- The actual PdfBitmapSizing implementation passed five boundary examples and 10,000 deterministic randomized cases checking positive dimensions, maximum pixel area and maximum edge length.
- Added four JVM sizing tests to the normal test suite.
- Added three Android instrumentation tests for visible native PDF pixels, rotated/UserUnit pages and concurrent page request serialization. These tests need a device/emulator and have not run locally.
- Added a manual **Android PDF device checks** GitHub workflow that runs these tests on an Android API 29 emulator. It can be launched from Actions after pushing this change.

No local APK build, physical-device validation or emulator run was performed. The native route is a different implementation, not another claim that the prior browser test reproduced the user's exact device failure.

Platform references: https://developer.android.com/reference/android/graphics/pdf/PdfRenderer and https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page .
