# Architecture decision — 2026-11-02

Native Kotlin/Compose shell, Android SAF content URIs, DataStore recents and settings, private bounded document snapshots, AndroidX WebViewAssetLoader. No internet permission, native JavaScript interface, account, analytics or service. All rendering assets ship in the APK. Build-time downloads are separate from runtime operation.

## Source review

Paperwren's public main/package.json uses PDF.js 4.10.38, docx-preview ^0.4.0, SheetJS 0.20.3 and fflate. Its src/lib/pptx/parse.ts and Slides viewer implement a custom positioned HTML/SVG renderer; it does **not** use PPTXjs. This project does not claim fidelity or performance parity. Sources: https://github.com/Razee4315/Paperwren and https://github.com/Razee4315/Paperwren/blob/main/package.json . Raw source snapshots are retained in research/.

## Selected dependencies

| Component | Pinned version | License | Decision |
|---|---|---|---|
| Android Gradle plugin | 8.5.2 | Apache-2.0 | Existing machine toolchain; build compatibility baseline, not latest |
| Kotlin / Compose compiler | 1.9.24 / 1.5.14 | Apache-2.0 | Compatible paired baseline |
| Compose BOM | 2024.06.00 | Apache-2.0 | Conservative build baseline |
| Activity / WebKit / DataStore / lifecycle | 1.9.0 / 1.11.0 / 1.1.1 / 2.8.3 | Apache-2.0 | Native shell |
| PDF.js | 6.5.438 legacy build | Apache-2.0 | Current fetched distribution; requires modern updated System WebView |
| docx-preview | 0.4.1 | Apache-2.0 | Layout renderer, not a text extractor |
| JSZip | 3.10.1 | MIT | docx-preview dependency |
| DOMPurify | See vendor inventory | Apache-2.0 OR MPL-2.0 | Sanitizes detached rendered Office HTML; Apache option selected |
| SheetJS CE | 0.20.3 | Apache-2.0 | Official vendored distribution; cached formula values only |
| PPTXjs | 1.21.1, exact upstream commit in vendor inventory | MIT | Experimental only: old engine, not actively maintained |
| PPTXjs runtime dependencies | See vendor inventory and bundled notices | MIT/BSD/GPL dual license | jQuery 3.7.1, JSZip 2.x (MIT option), D3 3.x, NVD3, FileReader, dingbat |

The requested combination of maintained, permissively licensed, small, high-fidelity PowerPoint engine was not established. pptx-preview 1.0.7's npm ISC metadata conflicts with README restrictions on source reuse and paid source access; rejected. PPTXjs has clearer licensing and actual positioned rendering, but maintenance/security debt means PowerPoint is experimental and is not production approved. No custom text extraction fallback.

## Limits and tradeoffs

PDF.js viewer provides lazy canvas rendering, selection, search, navigation, zoom and password callbacks. DOCX supports common paragraphs, tables, images, headers and footers, but not Word's complete pagination, fields, tracked changes, SmartArt or exact font metrics. Missing fonts substitute device fonts. SheetJS CE does not preserve full Excel styling, charts or all frozen panes; cached results may be stale, missing formula caches are explicitly labeled. Sheet rendering is chunked; workbook parsing uses a worker. PPTXjs does not guarantee charts, SmartArt, transitions, effects or accurate master inheritance. Media is disabled. Legacy formats and OpenDocument are not advertised.

Files above 50 MiB are rejected; Office ZIP expansion above 128 MiB or 4096 entries is rejected. Macro-bearing packages, ZIP path traversal and embedded HTML parts are rejected. These are practical bounds, not a proof against every parser exploit. Office rendering still consumes WebView memory; native preflight does not make third-party parsers memory-safe.

The WebView serves only packaged assets and the active document via a random session URL, blocks all other requests/navigation, file/content access, popups and mixed content. No native JS bridge exists. CSP blocks inline scripts, external connections and objects. Office output is sanitized before it enters the visible DOM. PPTXjs executes in a sandboxed opaque-origin iframe. Inline document styles are necessary for fidelity. Temporary snapshots are unlinked when closed; secure physical erasure on flash storage cannot be guaranteed. Recents retain URI metadata until cleared; clearing recents releases stored permissions.

Expected APK range before measurement: 12–25 MB including fonts/CMaps/WASM. Actual size and build results belong in TEST_RESULTS.md. No production readiness, device performance or Paperwren superiority claim is made without device measurements.
