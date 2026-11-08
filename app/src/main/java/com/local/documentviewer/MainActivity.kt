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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.local.documentviewer.ui.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class MainActivity: ComponentActivity() {
    private var incoming by mutableStateOf<Uri?>(null)
    private var restoreUri: String?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        val colors=readerColors(dark)
        var document by remember { mutableStateOf<OpenDocument?>(null) }
        var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
        var settings by remember { mutableStateOf(false) }; var web by remember { mutableStateOf<WebView?>(null) }
        var search by remember { mutableStateOf(false) }; var query by remember { mutableStateOf("") }
        fun find(text: String, again: Boolean=false) {
            if(document?.type=="pdf") web?.evaluateJavascript("window.documentViewerFind && window.documentViewerFind(${JSONObject.quote(text)}, $again);",null)
            else if(again) web?.findNext(true) else web?.findAllAsync(text)
        }
        fun close() { web?.stopLoading(); web?.destroy(); web=null; document?.file?.delete(); document=null; search=false; query=""; restoreUri=null }
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
        val choose={ picker.launch(arrayOf("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.openxmlformats-officedocument.presentationml.presentation","text/plain","text/csv")) }
        LaunchedEffect(incoming) { incoming?.let { open(it); incoming=null } }
        DisposableEffect(Unit) { onDispose { web?.destroy(); document?.file?.delete() } }
        BackHandler(document!=null || settings || search) { if(settings) settings=false else if(search) {search=false;query="";find("")} else close() }
        SideEffect {
            window.statusBarColor=colors.background.toArgb()
            window.navigationBarColor=colors.background.toArgb()
            WindowCompat.getInsetsController(window,window.decorView).apply {isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark}
        }
        MaterialTheme(colorScheme=colors) {
            Scaffold(containerColor=colors.background,topBar={
                if(document!=null) TopAppBar(
                    title={Column {Text(document?.name.orEmpty(),maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=15.sp,fontWeight=FontWeight.SemiBold);Text("${document?.type?.uppercase()} document",fontSize=10.sp,color=colors.onSurfaceVariant)}},
                    navigationIcon={IconButton(onClick={close()}) {ReaderIcon("back","Back to library")}},
                    actions={IconButton(onClick={search=!search;if(!search){query="";find("")}}) {ReaderIcon("search","Find in document")};IconButton(onClick={settings=true}) {ReaderIcon("settings","Settings")}},
                    colors=TopAppBarDefaults.topAppBarColors(containerColor=colors.background))
            }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if(loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp),color=colors.primary,trackColor=colors.primaryContainer)
                    if(search) Row(Modifier.padding(horizontal=16.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(query,{query=it;find(it)},Modifier.weight(1f),placeholder={Text("Find in document",fontSize=13.sp)},singleLine=true,shape=RoundedCornerShape(16.dp),leadingIcon={ReaderIcon("search",tint=colors.onSurfaceVariant)})
                        TextButton(onClick={find(query,true)}) {Text("Next")}
                        IconButton(onClick={search=false;query="";find("")}) {ReaderIcon("close","Close search")}
                    }
                    val current=document
                    if(current!=null) key(current.token) {
                        AndroidView(factory={secureWebView(it,current,dark) {message ->close();error=message}.also {v->web=v}},
                            update={view->view.evaluateJavascript("document.documentElement.dataset.theme='${if(dark) "dark" else "light"}';document.querySelector('iframe')?.contentWindow.postMessage({appearance:'${if(dark) "dark" else "light"}'},'*');",null)},modifier=Modifier.fillMaxSize())
                    } else LibraryHome(recents,loading,onOpen=choose,onRecent={open(Uri.parse(it.uri))},onSettings={settings=true})
                }
            }
            error?.let { message -> AlertDialog(onDismissRequest={error=null},icon={ReaderIcon("document",tint=colors.primary)},title={Text("Let's try that again")},text={Text(message)},confirmButton={TextButton(onClick={error=null}) {Text("Got it")}},shape=RoundedCornerShape(28.dp)) }
            if(settings) ModalBottomSheet(onDismissRequest={settings=false},containerColor=colors.surface,shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp)) {
                Column(Modifier.padding(start=24.dp,end=24.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    Text("Make it yours",fontSize=26.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.8).sp)
                    Text("A comfortable place to read.",fontSize=13.sp,color=colors.onSurfaceVariant)
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {Text("Dark appearance",fontSize=15.sp,fontWeight=FontWeight.Medium);Text(if(darkOverride==null) "Following your device" else "Interface only; document colors stay original",fontSize=11.sp,color=colors.onSurfaceVariant)}
                        Switch(dark,{value->scope.launch {preferences.edit {it[darkKey]=value}}})
                    }
                    if(darkOverride!=null) TextButton(onClick={scope.launch {preferences.edit {it.remove(darkKey)}}}) {Text("Use device appearance")}
                    HorizontalDivider(color=colors.outlineVariant)
                    TextButton(onClick={scope.launch {store.clear()}}) {ReaderIcon("trash",modifier=Modifier.size(18.dp));Spacer(Modifier.width(10.dp));Text("Clear recent documents")}
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {ReaderIcon("shield",tint=colors.primary);Column {Text("Your files stay with you",fontSize=13.sp,fontWeight=FontWeight.Medium);Text("Offline. No accounts. No tracking.",fontSize=11.sp,color=colors.onSurfaceVariant)}}
                    Text("Document Viewer 0.2.1 · 50 MB file limit\nPowerPoint preview is experimental.",fontSize=10.sp,lineHeight=16.sp,color=colors.onSurfaceVariant)
                }
            }
        }
    }
}
