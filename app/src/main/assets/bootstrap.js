// A classic script keeps module-loading failures visible on older Android WebViews.
window.viewerStartupFailure = function () {
  const status = document.getElementById('status');
  const pdf = document.getElementById('pdfContainer');
  if (pdf) pdf.hidden = true;
  if (status) {
    status.dataset.state = 'error';
    status.textContent = 'The reader could not start. Update Android System WebView, then reopen the document.';
  }
};
window.addEventListener('error', event => {
  if (event.target instanceof HTMLScriptElement || (event.filename && event.filename.includes('/vendor/pdf/'))) window.viewerStartupFailure();
}, true);
window.addEventListener('unhandledrejection', () => window.viewerStartupFailure());
