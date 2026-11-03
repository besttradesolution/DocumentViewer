# Third-party notices

Full license texts are bundled in `app/src/main/assets/licenses/` and included in the APK. Their original copyright notices apply. SHA-256 hashes for shipped renderer files are recorded in `docs/vendor-inventory.json`.

| Package | Version / source | License |
|---|---|---|
| Mozilla PDF.js | 6.5.438, npm pdfjs-dist legacy build | Apache-2.0 |
| docx-preview | 0.4.1, VolodymyrBaydalka/docxjs | Apache-2.0 |
| JSZip | 3.10.1 | MIT (selected from MIT/GPL dual license) |
| DOMPurify | 3.4.17 | Apache-2.0 option selected |
| SheetJS Community Edition | 0.20.3, official standalone script | Apache-2.0 |
| PPTXjs | 1.21.1, meshesha/PPTXjs commit 1a9260b2062f89ba822aeb54da792236780af8e7 | MIT |
| jQuery | 3.7.1 | MIT |
| JSZip (PPTXjs only) | upstream bundled 2.x distribution | MIT option selected; includes MIT-licensed pako |
| D3 | bundled 3.5.10 | BSD-3-Clause |
| NVD3 | bundled 1.8.1-dev (2015-09-10) | Apache-2.0 |
| FileReader.js | bundled 0.99 | MIT |
| dingbat mapping / PPTXjs CSS | same pinned PPTXjs repository | repository MIT license |
| Paperwren test fixtures | public main branch fetched 2026-11-02 | MIT |
| AndroidX, Compose, Android build tools and Kotlin | versions in Gradle files | Apache-2.0 and associated transitive notices |

Sources: https://github.com/mozilla/pdf.js , https://github.com/VolodymyrBaydalka/docxjs , https://github.com/Stuk/jszip , https://github.com/cure53/DOMPurify , https://docs.sheetjs.com/ , https://github.com/meshesha/PPTXjs , https://github.com/jquery/jquery , https://github.com/d3/d3 , https://github.com/novus/nvd3 , https://github.com/bgrins/filereader.js , https://github.com/Razee4315/Paperwren .

## Local PPTXjs hardening changes

`vendor/pptx/pptxjs.js` differs from upstream only in its slide insertion and generated stylesheet insertion: slide HTML is passed through DOMPurify before jQuery insertion; stylesheet content uses a style element's textContent. The host supplies a modern jQuery rather than the upstream 1.11.3 copy, disables media, disables global script evaluation and runs the renderer in an opaque-origin sandbox iframe with CSP. These changes do not constitute a full security audit of the old engine. No pptx-preview code is distributed.

The PDF assets retain upstream license comments. Other bundled code retains upstream headers where supplied. NVD3 and D3 license texts accompany their bundled distributions; their headers identify the precise historical builds. Android dependencies are resolved by Gradle and their packaged metadata is preserved.
