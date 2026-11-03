# Verification record

## Android build status

No APK produced. Local AAPT2 resource linking repeatedly crashed with Windows access-violation exit code -1073741819, including a minimal diagnostic invocation. Kotlin compilation and the 11 JVM unit tests have therefore **not run**. Local build work stopped at the user's request; `.github/workflows/android.yml` builds and tests on GitHub's Ubuntu runner. That workflow has not been executed in this session. It may reveal compilation or runtime issues requiring further changes. Release signing and device validation remain outstanding.

## Rendering harness

Desktop Chromium 145-family local installation, 412 × 850 viewport. The test harness fulfills packaged asset and document requests locally; all other hosts are blocked. These are smoke tests for renderer behavior, not pixel-level comparisons against Microsoft Office or Paperwren.

12 cases passed; one known PowerPoint fidelity limitation is explicitly recorded as unsupported. Raw results: `tests/renderer-results.json`. Representative screenshots accompany the tests.

| Case | Result |
|---|---|
| Basic PDF, canvas rendering, zoom and find commands | Passed |
| Encrypted PDF, wrong password then correct password `wren` | Passed |
| CJK PDF with local CMaps | Passed |
| Basic DOCX text and emphasis | Passed |
| Three-page DOCX | Passed |
| DOCX lists | Passed |
| XLSX visible cells | Passed |
| XLSX merged cells | Passed |
| Styled XLSX smoke test | Passed; complete Excel style fidelity is not asserted |
| Basic three-slide PPTX, slide navigation | Passed; small text observed in fit-to-width screenshot |
| Complex effects/SmartArt PPTX | Unsupported: upstream renderer throws on missing relationship target; app reports a rendering error |
| Malformed PDF | Rejected with visible error |
| Unicode-named TXT | Passed |

DOMPurify removal of script/event-handler markup is also asserted in rendered cases. This is not a full malicious-document security audit.

## Device checks still required

No Android device or emulator was connected in the build environment. SAF provider interoperability, Open With and Share intent routing, rotation, Android dark mode, Android WebView compatibility, accessibility, large/scanned documents, memory pressure and physical-device opening times remain unverified. No measured comparison with Paperwren is available. No claim of production readiness, complete Office support, sub-20-MB release size or superior performance is made.

## Reproduction

Build: `gradlew.bat assembleDebug testDebugUnitTest` with Java 17 and SDK 34. Browser checks: `node tests/renderers.cjs` with Playwright available through NODE_PATH; optional CHROMIUM_PATH selects a local Chromium executable. Opening times from this harness would include browser setup and are not reported as Android performance numbers.
