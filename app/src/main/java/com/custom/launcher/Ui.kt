package com.custom.launcher

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val Ink = Color(0xFF050810)
val Accent = Color(0xFF5B8DEF)
val Surface = Color(0xFFE6ECF7)
val Muted = Color(0xFF5A6478)

/** Soft neumorphic raised surface (light + dark shadow). */
fun Modifier.neu(radius: Dp = 24.dp, base: Color = Surface) = drawBehind {
    val r = radius.toPx()
    drawIntoCanvas { c ->
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = base.toArgb() }
        p.setShadowLayer(18f, -9f, -9f, 0xFFFFFFFF.toInt())
        c.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, p)
        p.setShadowLayer(18f, 9f, 9f, 0x40597AB8)
        c.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, p)
    }
}

/** Frosted-glass look: translucent tint + light rim. Real blur comes from window blur (see MainActivity). */
fun Modifier.glass(alpha: Float, radius: Dp = 28.dp) = this
    .clip(RoundedCornerShape(radius))
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha + 0.12f), Color.White.copy(alpha))))
    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.7f), Color.White.copy(0.1f))), RoundedCornerShape(radius))

@Composable
fun AppIcon(vm: LauncherViewModel, app: AppInfo, size: Dp = 56.dp, onClick: () -> Unit) {
    val v by vm.iconVersion.collectAsState()
    val icon by produceState<ImageBitmap?>(null, app, v) { value = vm.load(app) }
    Box(Modifier.size(size).clickable(remember { MutableInteractionSource() }, null) { onClick() }) {
        icon?.let { Image(it, app.label, Modifier.fillMaxSize()) }
    }
}

@Composable
fun NavPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.neu(26.dp).clickable(remember { MutableInteractionSource() }, null) { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Ink), Alignment.Center) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(label, fontSize = 10.sp, color = Ink, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ClockDial() {
    val now by produceState(LocalDateTime.now()) { while (true) { value = LocalDateTime.now(); delay(1000) } }
    Box(Modifier.size(250.dp).neu(125.dp), Alignment.Center) {
        Canvas(Modifier.size(236.dp)) {
            drawArc(Accent, -90f, 360f * now.second / 60f, false, style = Stroke(5f, cap = StrokeCap.Round),
                topLeft = Offset.Zero, size = Size(size.width, size.height))
        }
        Box(Modifier.size(200.dp).clip(CircleShape).background(Ink), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(now.format(DateTimeFormatter.ofPattern("EEEE")), color = Color(0xFFB8C4DC), fontFamily = FontFamily.Cursive, fontSize = 16.sp)
                Text(now.format(DateTimeFormatter.ofPattern("hh")), color = Accent, fontSize = 58.sp, fontWeight = FontWeight.Bold)
                Text(now.format(DateTimeFormatter.ofPattern("mm")), color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Bold)
                Text(now.format(DateTimeFormatter.ofPattern("a  dd-MM-yyyy")), color = Color(0xFFB8C4DC), fontSize = 11.sp)
            }
        }
    }
}
