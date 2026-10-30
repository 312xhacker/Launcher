package com.custom.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

@Composable
fun HomeScreen(vm: LauncherViewModel, onDrawer: () -> Unit, onSettings: () -> Unit) {
    val s by vm.settings.collectAsState()
    val dock by vm.dock.collectAsState()
    val ctx = LocalContext.current
    var drag by remember { mutableFloatStateOf(0f) }
    val hour = LocalTime.now().hour
    val greet = when { hour < 12 -> "Good Morning"; hour < 17 -> "Good Afternoon"; else -> "Good Evening" }

    Column(
        Modifier.fillMaxSize().background(Color(0x99EAF0FA)).systemBarsPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = { if (drag < -120f) onDrawer() },
                    onVerticalDrag = { _, dy -> drag += dy })
            }
    ) {
        Column {
            Text("Hello!", fontFamily = FontFamily.Cursive, fontSize = 22.sp, color = Accent)
            Text(s.name, fontSize = 46.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(greet, fontSize = 13.sp, color = Accent)
            Text("Have a nice day!", fontSize = 12.sp, color = Muted)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NavPill(Icons.Default.Home, "Home") {}
                NavPill(Icons.Default.Person, "Profile", onSettings)
                NavPill(Icons.Default.Settings, "Settings", onSettings)
                NavPill(Icons.Default.Menu, "Apps", onDrawer)
            }
            Spacer(Modifier.weight(1f))
            ClockDial()
        }
        MusicCard(vm)
        Spacer(Modifier.height(14.dp))
        // Dock
        Row(Modifier.fillMaxWidth().neu(34.dp).padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            dock.forEach { AppIcon(vm, it, 52.dp) { vm.launch(ctx, it) } }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().neu(26.dp).clickable(remember { MutableInteractionSource() }, null) { onDrawer() }
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = Muted, modifier = Modifier.size(18.dp))
            Text("  Search", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
fun DrawerScreen(vm: LauncherViewModel, onClose: () -> Unit) {
    val apps by vm.apps.collectAsState()
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val list = remember(apps, q) { if (q.isBlank()) apps else apps.filter { it.label.contains(q, true) } }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Color.White.copy(s.glassAlpha)).systemBarsPadding().padding(16.dp)
        .pointerInput(Unit) { var d = 0f
            detectVerticalDragGestures(onDragStart = { d = 0f }, onDragEnd = { if (d > 160f) onClose() }, onVerticalDrag = { _, dy -> d += dy }) }) {
        Box(Modifier.fillMaxWidth().glass(s.glassAlpha, 24.dp).padding(16.dp)) {
            if (q.isEmpty()) Text("Search apps", color = Muted, fontSize = 14.sp)
            BasicTextField(q, { q = it }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Ink),
                modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list, key = { it.component.flattenToString() }) { a ->
                Column(Modifier.clickable(remember { MutableInteractionSource() }, null) { vm.launch(ctx, a); onClose() },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(vm, a, 56.dp) { vm.launch(ctx, a); onClose() }
                    Text(a.label, fontSize = 11.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: LauncherViewModel, onClose: () -> Unit) {
    val s by vm.settings.collectAsState()
    val packs by vm.packs.collectAsState()
    var blur by remember(s.blur) { mutableFloatStateOf(s.blur.toFloat()) }
    var alpha by remember(s.glassAlpha) { mutableFloatStateOf(s.glassAlpha) }
    var name by remember(s.name) { mutableStateOf(s.name) }
    BackHandler(onBack = onClose)
    LazyColumn(Modifier.fillMaxSize().background(Color(0xE6EAF0FA)).systemBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink) }
        item {
            Text("Your name", color = Muted, fontSize = 13.sp)
            OutlinedTextField(name, { name = it; vm.setName(it) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            Text("Blur strength: ${blur.toInt()}", color = Muted, fontSize = 13.sp)
            Slider(blur, { blur = it }, onValueChangeFinished = { vm.setBlur(blur.toInt()) }, valueRange = 0f..100f)
        }
        item {
            Text("Glass opacity: ${(alpha * 100).toInt()}%", color = Muted, fontSize = 13.sp)
            Slider(alpha, { alpha = it }, onValueChangeFinished = { vm.setAlpha(alpha) }, valueRange = 0f..0.8f)
        }
        item { Text("Icon pack", color = Muted, fontSize = 13.sp) }
        item { PackRow("System default", s.iconPack.isEmpty()) { vm.setPack("") } }
        items(packs, key = { it.pkg }) { p -> PackRow(p.label, s.iconPack == p.pkg) { vm.setPack(p.pkg) } }
        if (packs.isEmpty()) item { Text("Koi icon pack installed nahi mila. Play Store se install karo (e.g. Pixel, Delta).", fontSize = 12.sp, color = Muted) }
    }
}

@Composable
private fun PackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().neu(18.dp).clickable(remember { MutableInteractionSource() }, null) { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Ink, fontSize = 14.sp)
        if (selected) Icon(Icons.Default.CheckCircle, null, tint = Accent)
    }
}

@Composable
fun MusicCard(vm: LauncherViewModel) {
    val music = vm.music
    val t by music.track.collectAsState()
    val access by music.hasAccess.collectAsState()
    val progress by produceState(0f, t) {
        while (true) {
            val d = t?.duration ?: 0L
            value = if (d > 0) (music.position().toFloat() / d).coerceIn(0f, 1f) else 0f
            delay(1000)
        }
    }
    Row(Modifier.fillMaxWidth().neu(34.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        val tr = t
        when {
            !access -> Text("Music card ke liye notification access allow karo (tap)", fontSize = 12.sp, color = Muted,
                modifier = Modifier.fillMaxWidth().clickable { music.openAccessSettings() }.padding(8.dp))
            tr == null -> Text("Abhi kuch play nahi ho raha", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(8.dp))
            else -> {
                Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(Ink), Alignment.Center) {
                    if (tr.art != null) Image(tr.art, null, Modifier.fillMaxSize()) else Text("♪", color = Color.White, fontSize = 22.sp)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(tr.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(tr.artist, fontSize = 11.sp, color = Accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { progress }, color = Accent, trackColor = Color(0x22000000),
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape))
                }
                Icon(Icons.Default.KeyboardArrowLeft, "Previous", tint = Ink, modifier = Modifier.size(30.dp).clickable { music.prev() })
                Box(Modifier.padding(horizontal = 4.dp).size(38.dp).clip(CircleShape).background(Ink).clickable { music.toggle() }, Alignment.Center) {
                    if (tr.playing) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.size(4.dp, 14.dp).background(Color.White)); Box(Modifier.size(4.dp, 14.dp).background(Color.White))
                    } else Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Icon(Icons.Default.KeyboardArrowRight, "Next", tint = Ink, modifier = Modifier.size(30.dp).clickable { music.next() })
            }
        }
    }
}
