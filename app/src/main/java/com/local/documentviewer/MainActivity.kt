package com.local.documentviewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.text.DateFormat
import java.util.Date

class MainActivity: ComponentActivity() {
    private var incoming by mutableStateOf<Uri?>(null)
    private var restoreUri: String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No document snapshots survive process startup.
        cacheDir.listFiles()?.filter { it.name.startsWith("document-") }?.forEach { it.delete() }
        incoming=savedInstanceState?.getString("documentUri")?.let(Uri::parse) ?: readIntent(intent)
        setContent { ViewerApp() }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); incoming=readIntent(intent) }
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("documentUri",restoreUri); super.onSaveInstanceState(outState) }
    @Suppress("DEPRECATION")
    private fun readIntent(intent: Intent): Uri? = when(intent.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        else -> null
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun ViewerApp() {
        val store=remember { RecentDocumentsStore(this) }; val scope=rememberCoroutineScope()
        val recents by store.recents.collectAsState(initial=emptyList())
        val darkKey=remember { booleanPreferencesKey("dark") }
        val darkOverride by remember { preferences.data.map { it[darkKey] } }.collectAsState(initial=null)
        val dark=darkOverride ?: isSystemInDarkTheme()
        var document by remember { mutableStateOf<OpenDocument?>(null) }
        var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
        var settings by remember { mutableStateOf(false) }; var web by remember { mutableStateOf<WebView?>(null) }
        var search by remember { mutableStateOf(false) }; var query by remember { mutableStateOf("") }
        fun close() { web?.stopLoading(); web?.destroy(); web=null; document?.file?.delete(); document=null; search=false; restoreUri=null }
        fun open(uri: Uri) {
            if(loading) return
            loading=true; error=null
            scope.launch {
                try {
                    val next=DocumentLoader(this@MainActivity).load(uri)
                    close(); document=next; restoreUri=uri.toString()
                    runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    store.add(uri,next.name)
                } catch(e: Exception) { error=when(e) { is SecurityException -> "Access was revoked. Open the document again with the file picker."; else -> e.message ?: "Could not open this document." } }
                finally { loading=false }
            }
        }
        val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri -> open(uri) } }
        LaunchedEffect(incoming) { incoming?.let { open(it); incoming=null } }
        DisposableEffect(Unit) { onDispose { web?.destroy(); document?.file?.delete() } }
        BackHandler(document!=null || settings) { if(settings) settings=false else close() }
        MaterialTheme(colorScheme=if(dark) darkColorScheme(primary=Color(0xFFA9C9FF)) else lightColorScheme(primary=Color(0xFF244E78), background=Color(0xFFF8F9FC))) {
            Scaffold(topBar={ TopAppBar(title={ Text(document?.name ?: "Document Viewer", maxLines=1,overflow=TextOverflow.Ellipsis) },
                navigationIcon={ if(document!=null) TextButton(onClick={close()}) { Text("Back") } },
                actions={ if(document!=null) TextButton(onClick={search=!search}) { Text("Find") }; TextButton(onClick={settings=true}) { Text("Settings") } }) }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if(search) Row(Modifier.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                        OutlinedTextField(query,{query=it; web?.findAllAsync(it)},Modifier.weight(1f),label={Text("Find in displayed text")},singleLine=true)
                        TextButton(onClick={web?.findNext(true)}) { Text("Next") }
                    }
                    val current=document
                    if(current!=null) key(current.token) {
                        AndroidView(factory={ secureWebView(it,current) { message -> close(); error=message }.also { v->web=v } }, modifier=Modifier.fillMaxSize())
                    } else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        item { Spacer(Modifier.height(22.dp)); Text("Your documents.\nJust open and read.",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.SemiBold) }
                        item { Text("Private by design. Works entirely offline.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        item { Button(onClick={picker.launch(arrayOf("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.openxmlformats-officedocument.presentationml.presentation","text/plain","text/csv"))},modifier=Modifier.fillMaxWidth().height(60.dp),enabled=!loading) { Text("Open Document",style=MaterialTheme.typography.titleMedium) } }
                        item { Text("PDF · Word · Excel · Text\nPowerPoint preview is experimental",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        item { Spacer(Modifier.height(12.dp)); Text("Recent documents",style=MaterialTheme.typography.titleLarge) }
                        if(recents.isEmpty()) item { Text("Documents you open will appear here.",color=MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(recents,key={it.uri}) { recent ->
                            ElevatedCard(Modifier.fillMaxWidth().clickable(enabled=!loading) { open(Uri.parse(recent.uri)) }) {
                                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Text(recent.name.substringAfterLast('.',"FILE").uppercase().take(4),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary,modifier=Modifier.width(52.dp))
                                    Column { Text(recent.name,maxLines=2,overflow=TextOverflow.Ellipsis); Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(recent.time)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            }
                        }
                    }
                }
            }
            error?.let { message -> AlertDialog(onDismissRequest={error=null},title={Text("Unable to open document")},text={Text(message)},confirmButton={TextButton(onClick={error=null}) {Text("OK")}}) }
            if(settings) AlertDialog(onDismissRequest={settings=false},title={Text("Settings")},text={ Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) { Text("Dark interface",Modifier.weight(1f)); Switch(dark,{value->scope.launch {preferences.edit {it[darkKey]=value}}}) }
                Text("Document colors stay unchanged. No ads, accounts or network permission.")
                TextButton(onClick={scope.launch {store.clear()}}) {Text("Clear recent documents")}
                Text("50 MB file limit. Office layout may differ from Microsoft Office. PowerPoint is experimental. Version 0.1.0",style=MaterialTheme.typography.bodySmall)
            } },confirmButton={TextButton(onClick={settings=false}) {Text("Done")}})
        }
    }
}
