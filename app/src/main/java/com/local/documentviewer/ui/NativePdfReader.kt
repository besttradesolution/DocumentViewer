package com.local.documentviewer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.documentviewer.OpenDocument
import com.local.documentviewer.rendering.NativePdfSession
import com.local.documentviewer.rendering.PdfPageSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NativePdfReader(document: OpenDocument,onUseTextViewer:()->Unit) {
    // Resource cleanup waits for in-flight page rendering, off the UI thread.
    val opened by produceState<Result<NativePdfSession>?>(null,document.token) {
        var session: NativePdfSession?=null
        try {
            val result=withContext(Dispatchers.IO) {
                try {Result.success(NativePdfSession.open(document.file).also {session=it})}
                catch(cancelled: CancellationException){throw cancelled}
                catch(error: Exception){Result.failure<NativePdfSession>(error)}
            }
            value=result
            awaitCancellation()
        } finally {withContext(NonCancellable+Dispatchers.IO){session?.close()}}
    }
    val result=opened
    when {
        result==null -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {CircularProgressIndicator()}
        result.isFailure -> PdfReaderError("This PDF may be password protected or unsupported by the page viewer.",onUseTextViewer)
        else -> NativePdfPages(result.getOrThrow(),onUseTextViewer)
    }
}

@Composable
private fun NativePdfPages(session: NativePdfSession,onUseTextViewer:()->Unit) {
    val colors=MaterialTheme.colorScheme
    val list=rememberLazyListState();val horizontal=rememberScrollState();val scope=rememberCoroutineScope()
    var zoom by remember {mutableStateOf(1f)}
    var jump by remember {mutableStateOf(false)};var pageInput by remember {mutableStateOf("")}
    val current by remember {derivedStateOf {(list.firstVisibleItemIndex+1).coerceAtMost(session.pageCount)}}
    Column(Modifier.fillMaxSize().background(colors.background)) {
        Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),shape=RoundedCornerShape(18.dp),color=colors.surface) {
            Row(Modifier.padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                IconButton(onClick={scope.launch {list.scrollToItem((current-2).coerceAtLeast(0))}},enabled=current>1) {ReaderIcon("back","Previous page")}
                TextButton(onClick={pageInput=current.toString();jump=true}) {Text("$current / ${session.pageCount}",fontSize=13.sp)}
                IconButton(onClick={scope.launch {list.scrollToItem(current.coerceAtMost(session.pageCount-1))}},enabled=current<session.pageCount) {ReaderIcon("chevron","Next page")}
                TextButton(onClick={zoom=1f;scope.launch {horizontal.scrollTo(0)}}) {Text("Fit",fontSize=12.sp)}
                IconButton(onClick={zoom=(zoom/1.25f).coerceAtLeast(1f)},enabled=zoom>1f) {Text("−",fontSize=24.sp)}
                IconButton(onClick={zoom=(zoom*1.25f).coerceAtMost(4f)},enabled=zoom<4f) {ReaderIcon("plus","Zoom in")}
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val viewportWidth=maxWidth;val pageWidth=(viewportWidth*zoom-24.dp).coerceAtLeast(1.dp)
            val density=LocalDensity.current
            val requestedWidth=with(density){pageWidth.roundToPx()}.coerceAtLeast(1)
            Box(Modifier.fillMaxSize().pointerInput(session) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed=false)
                    do {
                        val event=awaitPointerEvent(PointerEventPass.Initial)
                        if(event.changes.count {it.pressed}>=2) {
                            val factor=event.calculateZoom()
                            if(factor.isFinite())zoom=(zoom*factor).coerceIn(1f,4f)
                            event.changes.forEach {it.consume()}
                        }
                    } while(event.changes.any {it.pressed})
                }
            }.horizontalScroll(horizontal)) {
                LazyColumn(state=list,modifier=Modifier.width(viewportWidth*zoom).fillMaxHeight(),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    items(session.pageCount,key={it}) {index->NativePdfPage(session,index,pageWidth,requestedWidth,onUseTextViewer)}
                }
            }
        }
    }
    if(jump) AlertDialog(onDismissRequest={jump=false},title={Text("Go to page")},text={OutlinedTextField(pageInput,{pageInput=it.filter(Char::isDigit)},label={Text("Page 1–${session.pageCount}")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))},
        confirmButton={TextButton(onClick={val page=pageInput.toIntOrNull();if(page!=null&&page in 1..session.pageCount){scope.launch {list.scrollToItem(page-1)};jump=false}},enabled=pageInput.toIntOrNull()?.let {it in 1..session.pageCount}==true) {Text("Go")}},
        dismissButton={TextButton(onClick={jump=false}) {Text("Cancel")}})
}

@Composable
private fun NativePdfPage(session: NativePdfSession,index: Int,width: androidx.compose.ui.unit.Dp,requestedWidth: Int,onUseTextViewer:()->Unit) {
    var failed by remember(session,index,requestedWidth) {mutableStateOf(false)}
    val size by produceState<PdfPageSize?>(null,session,index) {
        try {value=withContext(Dispatchers.IO){session.size(index)}}
        catch(cancelled: CancellationException){throw cancelled}
        catch(error: Exception){failed=true}
    }
    val bitmap by produceState<Bitmap?>(null,session,index,requestedWidth) {
        try {value=withContext(Dispatchers.IO){session.render(index,requestedWidth)}}
        catch(cancelled: CancellationException){throw cancelled}
        catch(error: Exception){failed=true}
    }
    val pageSize=size
    val height=if(pageSize==null)width*1.4142f else width*(pageSize.height.toFloat()/pageSize.width)
    Box(Modifier.width(width).height(height).clip(RoundedCornerShape(2.dp)).background(Color.White),contentAlignment=Alignment.Center) {
        when {
            failed -> Column(horizontalAlignment=Alignment.CenterHorizontally) {Text("This page could not be rendered",color=Color(0xFF59516A));TextButton(onClick=onUseTextViewer) {Text("Try Text & search")}}
            bitmap!=null -> Image(bitmap!!.asImageBitmap(),contentDescription="PDF page ${index+1}",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.FillBounds)
            else -> CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp)
        }
    }
}

@Composable
private fun PdfReaderError(message: String,onUseTextViewer:()->Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
        ReaderIcon("document",tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(16.dp));Text(message,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp));Button(onClick=onUseTextViewer) {Text("Open Text & search")}
    }
}
