# Document Viewer — Android

PDFs now open in native Android **Page view**, which bypasses WebView layout. Use **Text & search** in the toolbar for PDF.js text selection, search or password entry. Native page view is image based; password-protected PDFs may require the alternate reader. Version 0.3.0 includes a manual **Android PDF device checks** workflow under GitHub Actions. See docs/UPDATE-0.3.0.md for verification and limits.

An offline, read-only native Android document viewer. Kotlin, Compose Material 3, SAF and bundled local renderers. No INTERNET permission, ads, accounts, trackers or broad storage permission.

This is a development build. PowerPoint support is experimental. See docs/ARCHITECTURE.md and docs/TEST_RESULTS.md for actual checks and limitations; do not interpret parsing support as complete Office fidelity.

## Build with GitHub Actions

Upload the **contents** of this project folder to the root of your GitHub repository, including `.github`, `gradle`, `gradlew` and `gradlew.bat`. Open **Actions → Build Android APK → Run workflow**. After a successful run, download **DocumentViewer-debug-apk** from that run's Artifacts and unzip it to get `app-debug.apk`. The workflow also runs the security unit tests and saves their report. Pushes to main/master and pull requests trigger the same build automatically.

No APK is included in this delivery. Local build attempts encountered an Android resource-tool crash before Kotlin compilation; the GitHub workflow has been prepared but has not run. Review the workflow result before treating the Android application as build-verified.

## Install after a successful build

Transfer app-debug.apk to an Android 9+ device and open it. Allow installation from the app you use to open the APK if Android prompts. Use **Open Document**, Android's **Open with**, or **Share → Document Viewer**. Keep Android System WebView updated; the PDF engine requires a modern WebView. The app itself never uses the network.

## Build

Open this folder in Android Studio with Java 17 and Android SDK 34 / build-tools 34.0.0 installed. Set `sdk.dir` in local.properties to your SDK directory. Run `gradlew.bat assembleDebug testDebugUnitTest` on Windows, or `./gradlew assembleDebug testDebugUnitTest` elsewhere. First build downloads Gradle/Maven dependencies. Bundled document engines require no npm install or runtime download.

For this machine the existing Gamebox SDK is at `E:/GameBox/work/android-sdk`. local.properties is machine-specific and excluded from the source archive.

## Release signing

Use Android Studio **Build → Generate Signed App Bundle / APK → APK**, select the release variant, and create or choose your own private signing key. Keep the key and passwords outside the project. The release build enables R8 and resource shrinking. No signing credentials or shared release key are shipped. The debug APK is for evaluation and is signed with a development key, not a production identity.

## Scope

PDF: lazy rendering, selection, in-document search, page navigation, fit width, zoom and password prompt. Word: common DOCX layout via docx-preview. Excel: worksheets, formatted cached values, merges and dimensions, 200-row incremental display. PPTX: positioned slides via sandboxed PPTXjs, experimental. TXT/CSV are secondary previews. Native **Find** searches currently displayed WebView text; PDF's own search reaches the entire PDF. No formula recalculation, editing, macros or external data refresh.

Office files above 50 MB, decompressed packages above 128 MB, macro/ActiveX packages, embedded HTML and encrypted Office files are rejected. PDF metadata/annotations/forms/scripting are not interactive. Legacy .doc/.xls/.ppt and OpenDocument formats are not advertised. PDF annotations are disabled for security. Not all Excel styles, frozen panes or presentation features are preserved.

Recent documents use persisted URI permissions where Android/providers allow it; shared temporary URIs may stop working later. Reopen unavailable documents with the picker. Settings can clear recents and release URI grants. Documents are copied into private temporary storage for rendering, removed on close or next cold start. Physical secure erasure cannot be guaranteed on flash storage.

## Tests

`tests/renderers.cjs` exercises bundled engines in desktop Chromium through a local asset interception harness. It requires Playwright externally and runs with `node tests/renderers.cjs`. Fixtures come from MIT-licensed Paperwren; upstream attribution and commit information are in research/ and THIRD_PARTY_NOTICES.md. Browser tests do not replace Android device testing. No connected device was assumed.
