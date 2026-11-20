package org.rayista.subobus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.rayista.subobus.i18n.LocalStrings
import org.rayista.subobus.vm.UiState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

private data class ScreenActions(
    val announce: () -> Unit,
    val stop: () -> Unit,
    val repeat: () -> Unit,
    val settings: () -> Unit,
    val direction: () -> Unit,
    val key: (String) -> Unit,
    val prevStation: () -> Unit,
    val nextStation: () -> Unit
)

@Composable
fun MainScreen(
    state: UiState,
    onAnnounce: () -> Unit,
    onStop: () -> Unit,
    onRepeat: () -> Unit,
    onSettings: () -> Unit,
    onDirection: () -> Unit,
    onKey: (String) -> Unit,
    onPrevStation: () -> Unit,
    onNextStation: () -> Unit,
    onOverwriteLocation: () -> Unit
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var pressedKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pressedKey) {
        if (pressedKey != null) {
            delay(120)
            pressedKey = null
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    val clock = DateTimeFormatter
        .ofPattern("yyyy-MM-dd EEEE HH:mm:ss", Locale.CHINA)
        .format(now)

    // 所有主页按键统一触发一次短震动（可在设置里关闭）
    val haptic = LocalHapticFeedback.current
    fun tap(action: () -> Unit): () -> Unit = {
        if (state.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        action()
    }

    val actions = ScreenActions(
        announce = tap(onAnnounce),
        stop = tap(onStop),
        repeat = tap(onRepeat),
        settings = tap(onSettings),
        direction = tap(onDirection),
        key = { key ->
            if (state.hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onKey(key)
        },
        prevStation = tap(onPrevStation),
        nextStation = tap(onNextStation)
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        if (maxWidth > maxHeight) {
            val s = (maxHeight.value / 700f).coerceIn(0.9f, 1.4f)
            LandscapeLayout(state, s, clock, actions, pressedKey, onOverwriteLocation) { pressedKey = it }
        } else {
            val s = min(maxWidth.value / 420f, maxHeight.value / 760f).coerceIn(0.6f, 1.35f)
            PortraitLayout(state, s, clock, actions, pressedKey, onOverwriteLocation) { pressedKey = it }
        }
    }
}

/* ============================ 竖屏 ============================ */
@Composable
private fun PortraitLayout(
    state: UiState,
    s: Float,
    clock: String,
    actions: ScreenActions,
    pressedKey: String?,
    onOverwriteLocation: () -> Unit,
    onFlash: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding((8 * s).dp),
        verticalArrangement = Arrangement.spacedBy((5 * s).dp)
    ) {
        TopBar(state.gps, clock, s, Modifier.fillMaxWidth())
        RouteBar(state, s, actions.direction, Modifier.fillMaxWidth())
        StationRow(
            state, s, Modifier.fillMaxWidth().weight(1.35f),
            onDoubleTap = if (state.doubleTapOverwrite) onOverwriteLocation else null
        )
        ActionArea(s, actions, Modifier.fillMaxWidth().weight(0.9f))
        KeypadArea(s, actions, pressedKey, onFlash, Modifier.fillMaxWidth().weight(2.2f))
        SpeedTextBar(state, s, Modifier.fillMaxWidth())
    }
}

/* ============================ 横屏 ============================ */
@Composable
private fun LandscapeLayout(
    state: UiState,
    s: Float,
    clock: String,
    actions: ScreenActions,
    pressedKey: String?,
    onOverwriteLocation: () -> Unit,
    onFlash: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding((8 * s).dp),
        verticalArrangement = Arrangement.spacedBy((5 * s).dp)
    ) {
        HeaderStrip(state, clock, s, actions.direction, Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy((8 * s).dp)
        ) {
            Column(
                modifier = Modifier.weight(1.15f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy((5 * s).dp)
            ) {
                StationRow(
                    state, s, Modifier.fillMaxWidth().weight(1f), compact = true,
                    onDoubleTap = if (state.doubleTapOverwrite) onOverwriteLocation else null
                )
                ActionArea(
                    s, actions,
                    Modifier.fillMaxWidth().height((92 * s).dp),
                    compact = true
                )
            }
            Column(modifier = Modifier.weight(0.95f).fillMaxHeight()) {
                KeypadArea(s, actions, pressedKey, onFlash, Modifier.fillMaxWidth().weight(1f))
                SpeedTextBar(state, s, Modifier.fillMaxWidth(), compact = true)
            }
        }
    }
}

@Composable
private fun HeaderStrip(
    state: UiState,
    clock: String,
    s: Float,
    onDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val t = LocalStrings.current
    Row(
        modifier = modifier.background(cs.appTopBar).padding(horizontal = (10 * s).dp, vertical = (6 * s).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "GPS " + t.gpsLevel(state.gps),
            color = gpsColor(state.gps),
            fontWeight = FontWeight.Bold,
            fontSize = (15 * s).sp,
            modifier = Modifier
                .border(1.dp, cs.appPanelBorder, MaterialTheme.shapes.small)
                .padding(horizontal = (9 * s).dp, vertical = (3 * s).dp)
        )
        Spacer(Modifier.width((10 * s).dp))
        Button(
            onClick = onDirection,
            enabled = state.canSwitch,
            modifier = Modifier.height((42 * s).dp),
            contentPadding = PaddingValues(horizontal = (12 * s).dp)
        ) {
            Text("⇅ ${state.routeMode}", fontSize = (15 * s).sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width((10 * s).dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = routeLabel(state.route, t),
                color = cs.appOnTopBar,
                fontSize = (23 * s).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "${state.startName} → ${state.endName}",
                color = cs.appOnTopBar,
                fontSize = (14 * s).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(text = clock, color = cs.appOnTopBar, fontSize = (13 * s).sp, maxLines = 1)
    }
}

@Composable
private fun TopBar(gps: String, clock: String, s: Float, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val t = LocalStrings.current
    Row(
        modifier = modifier.background(cs.appTopBar).padding(horizontal = (10 * s).dp, vertical = (7 * s).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "GPS " + t.gpsLevel(gps),
            color = gpsColor(gps),
            fontWeight = FontWeight.Bold,
            fontSize = (16 * s).sp,
            modifier = Modifier
                .border(1.dp, cs.appPanelBorder, MaterialTheme.shapes.small)
                .padding(horizontal = (10 * s).dp, vertical = (4 * s).dp)
        )
        Text(text = clock, color = cs.appOnTopBar, fontSize = (12 * s).sp, maxLines = 1)
    }
}

@Composable
private fun RouteBar(
    state: UiState,
    s: Float,
    onDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val t = LocalStrings.current
    Row(
        modifier = modifier.background(cs.appPanel).padding((9 * s).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onDirection,
            enabled = state.canSwitch,
            modifier = Modifier.width((96 * s).dp).height((44 * s).dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("⇅ ${state.routeMode}", fontSize = (15 * s).sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width((8 * s).dp))
        Column {
            Text(
                text = routeLabel(state.route, t),
                color = cs.appAccent,
                fontSize = (24 * s).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "${state.startName} → ${state.endName}",
                color = cs.appText,
                fontSize = (13 * s).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StationRow(
    state: UiState,
    s: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onDoubleTap: (() -> Unit)? = null
) {
    val t = LocalStrings.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((7 * s).dp)) {
        StationPanel(
            label = t.currentStop,
            name = state.currentName,
            coordinate = coordinateText(state.currentLat, state.currentLon),
            distance = "",
            big = true,
            s = s,
            compact = compact,
            onDoubleTap = onDoubleTap,
            modifier = Modifier.weight(1.15f).fillMaxHeight()
        )
        StationPanel(
            label = t.nextStop,
            name = state.nextName,
            coordinate = coordinateText(state.nextLat, state.nextLon),
            distance = distanceText(state, t),
            big = false,
            s = s,
            compact = compact,
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
    }
}

private fun coordinateText(lat: Double, lon: Double): String {
    if (lat == 0.0 && lon == 0.0) return ""
    return String.format(Locale.CHINA, "%.4f°N  %.4f°E", lat, lon)
}

private fun distanceText(state: UiState, t: org.rayista.subobus.i18n.AppStrings): String {
    if (state.nextLat == 0.0 && state.nextLon == 0.0) return ""
    return if (state.currentLat != 0.0 || state.currentLon != 0.0) {
        t.distance(state.distanceToNext)
    } else {
        t.distanceUnknown
    }
}

@Composable
private fun StationPanel(
    label: String,
    name: String,
    coordinate: String,
    distance: String,
    big: Boolean,
    s: Float,
    compact: Boolean,
    onDoubleTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val pad = if (compact) 9f else 12f
    val nameSize = if (compact) (if (big) 24f else 19f) else (if (big) 32f else 24f)
    val labelSize = if (compact) (if (big) 13f else 15f) else (if (big) 15f else 17f)
    val distSize = if (compact) 14f else (if (big) 15f else 17f)
    val coordSize = if (compact) 9f else 10f

    val tapHandler = onDoubleTap
    val gesture = if (tapHandler != null) {
        Modifier.pointerInput(tapHandler) {
            detectTapGestures(onDoubleTap = { tapHandler() })
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .then(gesture)
            .background(cs.appPanel, MaterialTheme.shapes.small)
            .border(1.dp, cs.appPanelBorder, MaterialTheme.shapes.small)
            .padding((pad * s).dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = if (big) cs.appMuted else cs.appAccent,
            fontSize = (labelSize * s).sp,
            fontWeight = if (big) FontWeight.Normal else FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height((3 * s).dp))
        Text(
            text = name,
            color = cs.appText,
            fontSize = (nameSize * s).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (distance.isNotBlank()) {
            Spacer(Modifier.height((3 * s).dp))
            Text(
                text = distance,
                color = cs.appDistance,
                fontSize = (distSize * s).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        if (coordinate.isNotBlank()) {
            Spacer(Modifier.height((2 * s).dp))
            Text(
                text = coordinate,
                color = cs.appMuted,
                fontSize = (coordSize * s).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionArea(
    s: Float,
    actions: ScreenActions,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val t = LocalStrings.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy((5 * s).dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy((7 * s).dp)) {
            ActionButton(t.announce, "primary", actions.announce, s, compact, Modifier.weight(1f).fillMaxHeight())
            ActionButton(t.stopPlayback, "stop", actions.stop, s, compact, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy((7 * s).dp)) {
            ActionButton(t.repeat, "normal", actions.repeat, s, compact, Modifier.weight(1f).fillMaxHeight())
            ActionButton(t.settings, "normal", actions.settings, s, compact, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    kind: String,
    onClick: () -> Unit,
    s: Float,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val colors = when (kind) {
        "primary" -> ButtonDefaults.buttonColors(cs.appButtonPrimary, cs.appOnButtonPrimary)
        "stop" -> ButtonDefaults.buttonColors(cs.appButtonStop, cs.appOnButtonStop)
        else -> ButtonDefaults.buttonColors(cs.appButtonNormal, cs.appOnButtonNormal)
    }
    Button(
        onClick = onClick,
        colors = colors,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, fontSize = ((if (compact) 15f else 18f) * s).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KeypadArea(
    s: Float,
    actions: ScreenActions,
    pressedKey: String?,
    onFlash: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val t = LocalStrings.current
    Column(modifier.background(cs.surface, MaterialTheme.shapes.small).border(1.dp, cs.appPanelBorder, MaterialTheme.shapes.small).padding((8 * s).dp)) {
        Text(t.keypad, color = cs.appMuted, fontSize = (11 * s).sp)
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy((5 * s).dp)
        ) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9")
            ).forEach { line ->
                Row(
                    Modifier.weight(1f).fillMaxWidth().heightIn(min = (40 * s).dp),
                    horizontalArrangement = Arrangement.spacedBy((5 * s).dp)
                ) {
                    line.forEach { key ->
                        KeyButton(
                            key, pressedKey == key,
                            { onFlash(key); actions.key(key) },
                            s, Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
            Row(
                Modifier.weight(1f).fillMaxWidth().heightIn(min = (40 * s).dp),
                horizontalArrangement = Arrangement.spacedBy((5 * s).dp)
            ) {
                KeyButton("▲", pressedKey == "prev", { onFlash("prev"); actions.prevStation() },
                    s, Modifier.weight(1f).fillMaxHeight())
                KeyButton("0", pressedKey == "0", { onFlash("0"); actions.key("0") },
                    s, Modifier.weight(1f).fillMaxHeight())
                KeyButton("▼", pressedKey == "next", { onFlash("next"); actions.nextStation() },
                    s, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    s: Float,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) cs.appKeyPressed else cs.surfaceVariant,
            contentColor = if (selected) cs.appOnKeyPressed else cs.onSurfaceVariant
        ),
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = (18 * s).sp)
    }
}

/** 纯文字速度/限速：竖屏通栏；横屏只在右栏键盘下方 */
@Composable
private fun SpeedTextBar(
    state: UiState,
    s: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    val t = LocalStrings.current
    val size = if (compact) 12f else 14f
    Row(
        modifier = modifier.background(cs.appTopBar).padding(horizontal = (10 * s).dp, vertical = (7 * s).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = t.speedLine(state.speed),
            color = cs.appOnTopBar,
            fontSize = (size * s).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = t.limitLine(state.limit),
            color = cs.error,
            fontSize = (size * s).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun gpsColor(level: String): Color {
    val dark = LocalAppTheme.current.isDark
    return when (level) {
        "强" -> if (dark) Color(0xFF8FF0B1) else Color(0xFF1B6E31)
        "中" -> if (dark) Color(0xFFD5EF8A) else Color(0xFF4F6B00)
        "弱" -> if (dark) Color(0xFFFFD27C) else Color(0xFF8A5300)
        else -> if (dark) Color(0xFFFF9D9D) else Color(0xFFB3261E)
    }
}
