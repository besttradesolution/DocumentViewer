package com.local.documentviewer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream

val Context.preferences by preferencesDataStore("document_viewer")
data class Recent(val uri: String, val name: String, val time: Long)
data class OpenDocument(val file: File, val name: String, val type: String, val token: String = UUID.randomUUID().toString())

object DocumentTypeDetector {
    fun detect(name: String): String = name.substringAfterLast('.', "").lowercase().also {
        require(it in setOf("pdf", "docx", "xlsx", "pptx", "txt", "csv")) { "This file format is not supported. Choose PDF, DOCX, XLSX, PPTX, TXT or CSV." }
    }
}

class RecentDocumentsStore(private val context: Context) {
    private val key = stringPreferencesKey("recents")
    val recents = context.preferences.data.map { prefs ->
        runCatching { val array = JSONArray(prefs[key] ?: "[]"); (0 until array.length()).map { val o = array.getJSONObject(it); Recent(o.getString("uri"), o.getString("name"), o.getLong("time")) } }.getOrDefault(emptyList())
    }
    suspend fun add(uri: Uri, name: String) {
        context.preferences.edit { prefs ->
            val old = runCatching { JSONArray(prefs[key] ?: "[]") }.getOrDefault(JSONArray())
            val next = JSONArray().put(JSONObject().put("uri", uri.toString()).put("name", name).put("time", System.currentTimeMillis()))
            for (i in 0 until old.length()) { val o = old.getJSONObject(i); if (o.getString("uri") != uri.toString() && next.length() < 20) next.put(o) }
            prefs[key] = next.toString()
        }
    }
    suspend fun clear() {
        context.contentResolver.persistedUriPermissions.forEach { runCatching { context.contentResolver.releasePersistableUriPermission(it.uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        context.preferences.edit { it.remove(key) }
    }
}

class DocumentLoader(private val context: Context) {
    suspend fun load(uri: Uri): OpenDocument = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Choose a document with Android's file picker." }
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if(c.moveToFirst()) c.getString(0) else null } ?: "document"
        val type = DocumentTypeDetector.detect(name)
        val file = File.createTempFile("document-", ".$type", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { out ->
                val buffer = ByteArray(65536); var total = 0L
                while(true) { val n = input.read(buffer); if(n < 0) break; total += n; require(total <= 50L * 1024 * 1024) { "This document exceeds the 50 MB limit." }; out.write(buffer, 0, n) }
                require(total > 0) { "This file is empty." }
            } } ?: error("The document is unavailable. Open it again with the file picker.")
            if(type in setOf("docx", "xlsx", "pptx")) OfficePackageValidator.validate(file, type)
            if(type == "pdf") require(file.inputStream().use { val header=ByteArray(5); it.read(header); String(header, Charsets.US_ASCII) } == "%PDF-") { "This is not a valid PDF." }
            OpenDocument(file, name.take(250), type)
        } catch(e: Exception) { file.delete(); throw e }
    }
}
object OfficePackageValidator {
    fun validate(file: File, type: String) {
        var count=0; var expanded=0L; var expected=false
        val entryPoint=mapOf("docx" to "word/document.xml", "xlsx" to "xl/workbook.xml", "pptx" to "ppt/presentation.xml").getValue(type)
        ZipInputStream(file.inputStream()).use { zip ->
            val buffer=ByteArray(65536)
            while(true) { val entry=zip.nextEntry ?: break; val path=entry.name.lowercase()
                require(++count <= 4096 && !path.startsWith('/') && !path.contains("..") && !path.contains('\\')) { "Unsafe or oversized Office package." }
                require(!path.endsWith("vbaproject.bin") && !path.endsWith(".html") && !path.endsWith(".htm") && !path.contains("activex/")) { "Documents containing macros, ActiveX or embedded HTML are not supported." }
                if(path == entryPoint) expected=true
                while(true) { val n=zip.read(buffer); if(n<0) break; expanded+=n; require(expanded<=128L*1024*1024) { "Office package expands beyond the safety limit." } }
                zip.closeEntry()
            }
        }
        require(expected) { "This Office document is encrypted, corrupted or has the wrong extension." }
    }
}
