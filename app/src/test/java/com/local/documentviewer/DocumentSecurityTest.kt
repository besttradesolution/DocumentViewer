package com.local.documentviewer

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocumentSecurityTest {
    private fun archive(vararg paths: String, check:(File)->Unit) {
        val file=File.createTempFile("office-test", ".zip")
        try { ZipOutputStream(file.outputStream()).use {zip -> paths.forEach {path ->zip.putNextEntry(ZipEntry(path));zip.write("<test/>".toByteArray());zip.closeEntry()} };check(file) }
        finally {file.delete()}
    }
    @Test fun unicodeNamesAndCase() {assertEquals("docx",DocumentTypeDetector.detect("報告 final.DOCX"))}
    @Test(expected=IllegalArgumentException::class) fun rejectLegacy() {DocumentTypeDetector.detect("document.doc")}
    @Test(expected=IllegalArgumentException::class) fun rejectDisguisedExtension() {DocumentTypeDetector.detect("report.pdf.exe")}
    @Test fun acceptsCorrectPackage() {archive("word/document.xml") {OfficePackageValidator.validate(it,"docx")}}
    @Test(expected=IllegalArgumentException::class) fun rejectTraversal() {archive("../outside","word/document.xml") {OfficePackageValidator.validate(it,"docx")}}
    @Test(expected=IllegalArgumentException::class) fun rejectMacro() {archive("word/document.xml","word/vbaProject.bin") {OfficePackageValidator.validate(it,"docx")}}
    @Test(expected=IllegalArgumentException::class) fun rejectActiveX() {archive("word/document.xml","word/activeX/control.xml") {OfficePackageValidator.validate(it,"docx")}}
    @Test(expected=IllegalArgumentException::class) fun rejectEmbeddedHtml() {archive("word/document.xml","word/chunk.html") {OfficePackageValidator.validate(it,"docx")}}
    @Test(expected=IllegalArgumentException::class) fun rejectWrongPackage() {archive("xl/workbook.xml") {OfficePackageValidator.validate(it,"docx")}}
    @Test(expected=IllegalArgumentException::class) fun rejectEmptyPackage() {archive {OfficePackageValidator.validate(it,"pptx")}}
    @Test(expected=IllegalArgumentException::class) fun rejectEntryFlood() {archive(*(0..4096).map {"entry$it"}.toTypedArray()) {OfficePackageValidator.validate(it,"xlsx")}}
}
