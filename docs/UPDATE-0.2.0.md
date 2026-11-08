# 0.2.0 — PDF startup fix and interface refresh

## PDF fix

AndroidX's asset handler guesses MIME types from Android's OS-dependent extension table. Unknown extensions fall back to text/plain. PDF.js's .mjs modules and worker need a JavaScript MIME type; the previous browser harness supplied that MIME type itself and missed the Android failure mode. AssetMimeTypes now explicitly maps .mjs/.js to application/javascript and .wasm to application/wasm, applied only to already-approved local asset responses. Network/file access restrictions remain in place.

The bounded local document snapshot now has a Content-Length header and PDFs load without HTTP range/stream assumptions. PDF load listeners are registered before setDocument. Loading feedback stays visible until a page actually renders; module/render failures show an error. The native search box now uses PDF.js's full-document find controller for PDFs. Viewport resizing reapplies fit-width.

## Design

Native Compose library: violet gradient open action, custom bundled outline icons, distinct file-type accents, compact recent rows, styled empty state, dark palette, and a settings bottom sheet with a device-theme reset. Reader controls: floating rounded toolbar, a collapsible PDF search panel, soft page shadows, and dark UI chrome that preserves original document colors. No new runtime dependencies, permissions or remote assets. Version code 2, version name 0.2.0.

## Checks

- tests/pdf-android-regression.cjs reproduces rejection of text/plain modules and checks a visible error. Correct module responses produce real PDF canvas pixels, full-document search highlights and successful viewport resizing. This is an Android transport simulation in desktop Chromium, not an on-device test.
- Existing renderer suite: 12 passed, one known PPTX effects/SmartArt fixture remains unsupported.
- Added three Kotlin MIME policy tests for CI; not run locally.
- JavaScript syntax checks passed. Native Compose changes require the next GitHub build and a device check. No local APK build was attempted for this update, following the user's earlier instruction to build on GitHub.

Reference: https://android.googlesource.com/platform/frameworks/support/+/androidx-main/webkit/webkit/src/main/java/androidx/webkit/internal/AssetHelper.java
