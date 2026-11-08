# 0.2.1 — Collapsed PDF page layout

The screenshot showing the correct page count and a horizontal white strip indicates that parsing succeeded but the page's displayed dimensions collapsed. The 0.2.0 MIME fix did not address that failure mode.

The bundled PDF.js 6.5.438 setLayerDimensions uses CSS round() unconditionally. On WebViews without that feature, its var()-containing dimension declarations become invalid at computed-value time, and the page/canvas can collapse to zero height. Restored the feature detection and calc() fallback present in PDF.js 5.7.284, preserving the current PDF engine, dynamic zoom, rotation and UserUnit scaling.

Verification:

- Reproduced the old behavior with CSS round() disabled: count remains present; page and canvas have displayed height 0, even though the PDF bitmap exists.
- After the fix, modern CSS page size is 319 × 414 CSS pixels; legacy fallback is 320 × 414.109375. Both preserve the PDF aspect ratio.
- tests/pdf-layout-compat.cjs checks actual page and canvas display dimensions, zoom, fit-width after viewport changes, page navigation and search highlights for both modern and emulated older CSS support.
- tests/pdf-android-regression.cjs now also rejects a zero-height displayed canvas. The viewer reports layout failure instead of silently clearing loading feedback if a future compatibility problem collapses a page.
- Version code 3 / version name 0.2.1 distinguishes the new APK from the earlier update. No local APK build or physical-device verification was performed; build and install the new GitHub artifact.

This test simulates unsupported CSS math in desktop Chromium. It does not identify the user's exact WebView version, which was not supplied.
