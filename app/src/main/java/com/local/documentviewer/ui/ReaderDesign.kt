package com.local.documentviewer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.documentviewer.Recent
import java.text.DateFormat
import java.util.Date

val Violet = Color(0xFF7059E8)
fun readerColors(dark: Boolean): ColorScheme = if(dark) darkColorScheme(
    primary=Color(0xFFC4B8FF),onPrimary=Color(0xFF27145D),primaryContainer=Color(0xFF30264C),
    background=Color(0xFF14141D),surface=Color(0xFF1C1C28),surfaceVariant=Color(0xFF292937),
    onSurface=Color(0xFFF1EFFA),onSurfaceVariant=Color(0xFFAAA6BD),outlineVariant=Color(0xFF34323F)
) else lightColorScheme(
    primary=Violet,onPrimary=Color.White,primaryContainer=Color(0xFFEFEAFF),
    background=Color(0xFFF7F7FC),surface=Color.White,surfaceVariant=Color(0xFFF0EFF7),
    onSurface=Color(0xFF242337),onSurfaceVariant=Color(0xFF838096),outlineVariant=Color(0xFFE9E6F1)
)

/** Small bundled outline glyphs; no icon font or runtime downloads. */
@Composable
fun ReaderIcon(name: String, description: String?=null, modifier: Modifier=Modifier, tint: Color=LocalContentColor.current) {
    val vector=remember(name) { ImageVector.Builder(name,24.dp,24.dp,24f,24f).apply {
        path(fill=null,stroke=SolidColor(Color.Black),strokeLineWidth=1.7f,strokeLineCap=StrokeCap.Round,strokeLineJoin=StrokeJoin.Round) {
            when(name) {
                "back" -> {moveTo(14f,5f);lineTo(7f,12f);lineTo(14f,19f)}
                "arrow" -> {moveTo(5f,12f);lineTo(19f,12f);moveTo(13f,6f);lineTo(19f,12f);lineTo(13f,18f)}
                "chevron" -> {moveTo(9f,7f);lineTo(14f,12f);lineTo(9f,17f)}
                "search" -> {moveTo(16f,10f);curveTo(16f,18f,4f,18f,4f,10f);curveTo(4f,2f,16f,2f,16f,10f);moveTo(15f,15f);lineTo(21f,21f)}
                "close" -> {moveTo(6f,6f);lineTo(18f,18f);moveTo(18f,6f);lineTo(6f,18f)}
                "plus" -> {moveTo(12f,5f);lineTo(12f,19f);moveTo(5f,12f);lineTo(19f,12f)}
                "settings" -> {moveTo(4f,7f);lineTo(20f,7f);moveTo(4f,17f);lineTo(20f,17f);moveTo(8f,4f);lineTo(8f,10f);moveTo(16f,14f);lineTo(16f,20f)}
                "shield" -> {moveTo(12f,3f);lineTo(20f,6f);lineTo(20f,12f);curveTo(20f,17f,16f,20f,12f,22f);curveTo(8f,20f,4f,17f,4f,12f);lineTo(4f,6f);close();moveTo(8f,12f);lineTo(11f,15f);lineTo(16f,10f)}
                "folder" -> {moveTo(3f,8f);lineTo(3f,5f);lineTo(9f,5f);lineTo(12f,8f);lineTo(21f,8f);lineTo(21f,19f);lineTo(3f,19f);close()}
                "trash" -> {moveTo(4f,6f);lineTo(20f,6f);moveTo(9f,6f);lineTo(9f,3f);lineTo(15f,3f);lineTo(15f,6f);moveTo(6f,6f);lineTo(7f,21f);lineTo(17f,21f);lineTo(18f,6f);moveTo(10f,10f);lineTo(10f,17f);moveTo(14f,10f);lineTo(14f,17f)}
                "check" -> {moveTo(5f,12f);lineTo(10f,17f);lineTo(19f,7f)}
                else -> {moveTo(6f,3f);lineTo(14f,3f);lineTo(19f,8f);lineTo(19f,21f);lineTo(5f,21f);lineTo(5f,3f);close();moveTo(14f,3f);lineTo(14f,8f);lineTo(19f,8f);moveTo(9f,12f);lineTo(15f,12f);moveTo(9f,16f);lineTo(13f,16f)}
            }
        }
    }.build() }
    Icon(vector,description,modifier.size(24.dp),tint)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryHome(recents: List<Recent>, loading: Boolean, onOpen:()->Unit, onRecent:(Recent)->Unit, onSettings:()->Unit) {
    val colors=MaterialTheme.colorScheme
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=24.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        item { Row(Modifier.widthIn(max=600.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(colors.primaryContainer),contentAlignment=Alignment.Center) { ReaderIcon("document",tint=colors.primary) }
            Column(Modifier.weight(1f).padding(start=12.dp)) { Text("Document Viewer",fontWeight=FontWeight.SemiBold,fontSize=18.sp,letterSpacing=(-.5).sp);Text("Your offline library",fontSize=12.sp,color=colors.onSurfaceVariant) }
            IconButton(onClick=onSettings) {ReaderIcon("settings","Settings",tint=colors.onSurface)}
        } }
        item { Column(Modifier.widthIn(max=600.dp).fillMaxWidth()) {
            Text("A LITTLE LESS NOISE",fontSize=10.sp,letterSpacing=2.sp,fontWeight=FontWeight.SemiBold,color=colors.primary)
            Spacer(Modifier.height(10.dp))
            Text("Room to read.",fontSize=36.sp,lineHeight=42.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1.5).sp)
            Spacer(Modifier.height(8.dp));Text("Your files, a little more focused.",color=colors.onSurfaceVariant,fontSize=14.sp)
        } }
        item { Card(onClick=onOpen,enabled=!loading,shape=RoundedCornerShape(28.dp),modifier=Modifier.widthIn(max=600.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Violet)) {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF7963EE),Color(0xFF5341B2))))) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(alpha=.045f),radius=size.width*.55f,center=Offset(size.width,size.height*.2f))
                    drawCircle(Color.White.copy(alpha=.045f),radius=size.width*.33f,center=Offset(size.width,size.height*.2f))
                }
                Column(Modifier.padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha=.16f)),contentAlignment=Alignment.Center) {ReaderIcon("folder",tint=Color.White)}
                        Surface(shape=RoundedCornerShape(50),color=Color.White.copy(alpha=.13f)) {Row(Modifier.padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {ReaderIcon("shield",modifier=Modifier.size(13.dp),tint=Color.White.copy(alpha=.9f));Text("100% offline",color=Color.White.copy(alpha=.9f),fontSize=10.sp,fontWeight=FontWeight.Medium)}}
                    }
                    Spacer(Modifier.height(24.dp));Text("Open a document",fontSize=24.sp,fontWeight=FontWeight.SemiBold,color=Color.White,letterSpacing=(-.5).sp)
                    Spacer(Modifier.height(6.dp));Text("Pick a file. Make yourself comfortable.",fontSize=12.sp,color=Color.White.copy(alpha=.72f))
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {Text("Browse files",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f));Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White),contentAlignment=Alignment.Center) {ReaderIcon("arrow",tint=Color(0xFF6751CA),modifier=Modifier.size(18.dp))}}
                }
            }
        } }
        item { Row(Modifier.widthIn(max=600.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            listOf("PDF" to Color(0xFFE27B77),"WORD" to Color(0xFF7396DE),"EXCEL" to Color(0xFF65AA92),"SLIDES*" to Color(0xFFCC9C65)).forEach { (label,tint)->
                Surface(Modifier.weight(1f),shape=RoundedCornerShape(12.dp),color=colors.surface) {Row(Modifier.padding(vertical=10.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {Box(Modifier.size(5.dp).clip(CircleShape).background(tint));Spacer(Modifier.width(5.dp));Text(label,fontSize=9.sp,fontWeight=FontWeight.SemiBold,letterSpacing=.5.sp,color=colors.onSurfaceVariant)}}
            }
        } }
        item { Row(Modifier.widthIn(max=600.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("Recently opened",fontWeight=FontWeight.SemiBold,fontSize=18.sp,letterSpacing=(-.4).sp,modifier=Modifier.weight(1f));if(recents.isNotEmpty())Text("${recents.size} files",fontSize=12.sp,color=colors.onSurfaceVariant) } }
        if(recents.isEmpty()) item { Surface(Modifier.widthIn(max=600.dp).fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=colors.surface) {
            Column(Modifier.padding(horizontal=24.dp,vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(20.dp)).background(colors.primaryContainer),contentAlignment=Alignment.Center) {ReaderIcon("document",tint=colors.primary,modifier=Modifier.size(28.dp))}
                Spacer(Modifier.height(16.dp));Text("A fresh page",fontWeight=FontWeight.SemiBold,fontSize=16.sp);Spacer(Modifier.height(6.dp));Text("Open your first document to get started.",color=colors.onSurfaceVariant,fontSize=12.sp)
            }
        } }
        items(recents,key={it.uri}) { recent -> RecentDocumentRow(recent,!loading) {onRecent(recent)} }
        item { Column(Modifier.padding(bottom=16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {ReaderIcon("shield",modifier=Modifier.size(13.dp),tint=colors.onSurfaceVariant);Text("On your device. Always yours.",fontSize=11.sp,color=colors.onSurfaceVariant)}
            Spacer(Modifier.height(8.dp));Text("* PowerPoint preview is experimental",fontSize=10.sp,color=colors.onSurfaceVariant.copy(alpha=.7f))
        } }
    }
}

@Composable
private fun RecentDocumentRow(recent: Recent,enabled: Boolean,onClick:()->Unit) {
    val colors=MaterialTheme.colorScheme
    val extension=recent.name.substringAfterLast('.',"FILE").uppercase(java.util.Locale.ROOT)
    val accent=when(extension) {"PDF"->Color(0xFFDC7776);"DOCX"->Color(0xFF7294D5);"XLSX","CSV"->Color(0xFF60A58C);"PPTX"->Color(0xFFCC9A5B);else->Violet}
    Surface(Modifier.widthIn(max=600.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(enabled=enabled,onClick=onClick),shape=RoundedCornerShape(20.dp),color=colors.surface) {
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha=.12f)),contentAlignment=Alignment.Center) {ReaderIcon("document",tint=accent)}
            Column(Modifier.weight(1f).padding(horizontal=14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(recent.name,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=14.sp,fontWeight=FontWeight.Medium)
                Text("$extension  ·  ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(recent.time))}",fontSize=10.sp,color=colors.onSurfaceVariant)
            }
            ReaderIcon("chevron",modifier=Modifier.size(17.dp),tint=colors.onSurfaceVariant.copy(alpha=.5f))
        }
    }
}
