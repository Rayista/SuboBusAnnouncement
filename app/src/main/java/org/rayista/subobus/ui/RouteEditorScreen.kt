package org.rayista.subobus.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.rayista.subobus.i18n.AppStrings
import org.rayista.subobus.i18n.LocalStrings
import org.rayista.subobus.vm.EditableStop
import org.rayista.subobus.vm.RouteEditorViewModel
import kotlin.math.roundToInt

private data class EditTarget(val index: Int, val isNew: Boolean, val stop: EditableStop)

private val StationRowHeight = 64.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RouteEditorScreen(
    vm: RouteEditorViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val state = vm.state
    val t = LocalStrings.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val edgePx = with(density) { 8.dp.toPx() }
    val rowHeightPx = with(density) { StationRowHeight.toPx() }

    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var nameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var showBackConfirm by remember { mutableStateOf(false) }
    var showCopyConfirm by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    var draggingUid by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.isNew, state.route) {
        if (state.isNew && state.route.isBlank() && !nameDialog) {
            nameInput = ""
            nameDialog = true
        }
    }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearToast()
        }
    }

    fun onDragStart(uid: Long) {
        draggingUid = uid
        dragOffsetY = 0f
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // 行高固定，所以按“跨过半行就换位”的步进算法重排：
    // 首/尾再往外拖时只给阻尼，不会越拖越远导致飞掉。
    fun reorderStep(uid: Long) {
        val stops = vm.state.stops
        val from = stops.indexOfFirst { it.uid == uid }
        if (from < 0) return
        val step = when {
            dragOffsetY > rowHeightPx / 2f -> 1
            dragOffsetY < -rowHeightPx / 2f -> -1
            else -> return
        }
        val to = (from + step).coerceIn(0, stops.lastIndex)
        if (to != from) {
            vm.moveStation(from, to)
            // 行槽位整体移动了一行，补偿偏移保持跟手
            dragOffsetY -= (to - from) * rowHeightPx
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            dragOffsetY = dragOffsetY.coerceIn(-rowHeightPx * 0.7f, rowHeightPx * 0.7f)
        }
    }

    fun onDragDelta(uid: Long, delta: Float) {
        dragOffsetY += delta
        reorderStep(uid)
    }

    fun onDragEnd() {
        draggingUid = null
        dragOffsetY = 0f
    }

    // 只有真的把拖动行拖出视口边缘时才自动滚动；
    // 并且只补偿“实际滚动掉的距离”，列表到顶/到底时不再施加位移，
    // 避免第一站/最后一站一开始拖就被持续拉扯。
    LaunchedEffect(draggingUid) {
        val uid = draggingUid ?: return@LaunchedEffect
        while (true) {
            val layout = listState.layoutInfo
            val info = layout.visibleItemsInfo.firstOrNull { it.key == uid }
            if (info == null) {
                delay(32)
                continue
            }
            val visualTop = info.offset + dragOffsetY
            val visualBottom = visualTop + info.size
            val wantScroll = when {
                visualTop < layout.viewportStartOffset + edgePx && listState.canScrollBackward -> -14f
                visualBottom > layout.viewportEndOffset - edgePx && listState.canScrollForward -> 14f
                else -> 0f
            }
            if (wantScroll != 0f) {
                val consumed = listState.scrollBy(wantScroll)
                if (consumed != 0f) {
                    dragOffsetY += consumed
                    reorderStep(uid)
                }
                delay(16)
            } else {
                delay(32)
            }
        }
    }

    fun attemptBack() {
        if (state.dirty) showBackConfirm = true else onBack()
    }

    BackHandler { attemptBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        EditorTopBar(
            up = state.up,
            onBack = { attemptBack() },
            onSave = {
                if (vm.save()) onSaved(vm.state.route)
            },
            onAdd = { editing = EditTarget(state.stops.size, true, EditableStop()) },
            onMenu = { menuOpen = true },
            menuOpen = menuOpen,
            onDismissMenu = { menuOpen = false },
            onRename = {
                menuOpen = false
                nameInput = state.route
                nameDialog = true
            },
            onSwitchDirection = {
                menuOpen = false
                vm.switchDirection()
            },
            onCopyUpToDown = {
                menuOpen = false
                if (state.downStops.isNotEmpty()) showCopyConfirm = true
                else vm.copyUpToDown()
            }
        )

        if (state.stops.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = if (state.up) LocalStrings.current.emptyUp else LocalStrings.current.emptyDown,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                item { HintStrip(count = state.stops.size) }
                itemsIndexed(vm.state.stops, key = { _, stop -> stop.uid }) { index, stop ->
                    StationRow(
                        index = index,
                        stop = stop,
                        isDragging = draggingUid == stop.uid,
                        dragOffsetY = if (draggingUid == stop.uid) dragOffsetY else 0f,
                        onClick = { editing = EditTarget(index, false, stop) },
                        onDelete = { vm.deleteStation(index) },
                        onInsertAbove = { editing = EditTarget(index, true, EditableStop()) },
                        onDragStart = { onDragStart(stop.uid) },
                        onDragDelta = { delta -> onDragDelta(stop.uid, delta) },
                        onDragEnd = { onDragEnd() },
                        modifier = if (draggingUid == stop.uid) {
                            Modifier
                        } else {
                            Modifier.animateItemPlacement()
                        }
                    )
                }
            }
        }
    }

    editing?.let { target ->
        StationEditDialog(
            target = target,
            onDismiss = { editing = null },
            onConfirm = { stop ->
                if (target.isNew) vm.addStation(target.index, stop)
                else vm.updateStation(target.index, stop)
                editing = null
            }
        )
    }

    if (nameDialog) {
        AlertDialog(
            onDismissRequest = {
                nameDialog = false
                if (state.isNew && state.route.isBlank()) onBack()
            },
            title = { Text(if (state.route.isBlank()) t.newRouteTitle else t.routeNameLabel) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(t.routeNameLabel) },
                    singleLine = true,
                    colors = editorFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nameInput.isNotBlank(),
                    onClick = {
                        vm.renameRoute(nameInput)
                        if (vm.state.route.isNotBlank()) nameDialog = false
                    }
                ) { Text(t.confirm) }
            },
            dismissButton = {
                TextButton(onClick = {
                    nameDialog = false
                    if (state.isNew && state.route.isBlank()) onBack()
                }) { Text(t.cancel) }
            }
        )
    }

    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            title = { Text(t.unsavedTitle) },
            text = { Text(t.unsavedText) },
            // 取消做成实心主按钮（留在编辑器），确定弱化为文字按钮，避免误点丢修改
            confirmButton = {
                Button(onClick = { showBackConfirm = false }) { Text(t.stay) }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirm = false; onBack() }) {
                    Text(t.discard, color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (showCopyConfirm) {
        AlertDialog(
            onDismissRequest = { showCopyConfirm = false },
            title = { Text(t.overwriteTitle) },
            text = { Text(t.overwriteText) },
            confirmButton = {
                TextButton(onClick = {
                    showCopyConfirm = false
                    vm.copyUpToDown()
                }) { Text(t.overwriteContinue) }
            },
            dismissButton = {
                TextButton(onClick = { showCopyConfirm = false }) { Text(t.cancel) }
            }
        )
    }
}

@Composable
private fun EditorTopBar(
    up: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onAdd: () -> Unit,
    onMenu: () -> Unit,
    menuOpen: Boolean,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onSwitchDirection: () -> Unit,
    onCopyUpToDown: () -> Unit
) {
    val t = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回用 TextButton，避免和“保存”抢视觉焦点
        TextButton(onClick = onBack) { Text(t.back) }
        Spacer(Modifier.width(4.dp))
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { Text(t.save) }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 14.dp)) {
            Text("＋", fontSize = 18.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = if (up) t.up else t.down,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Box {
            IconButton(onClick = onMenu) {
                Text("⋮", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(text = { Text(t.routeNameLabel) }, onClick = onRename)
                DropdownMenuItem(
                    text = { Text(if (up) t.down else t.up) },
                    onClick = onSwitchDirection
                )
                DropdownMenuItem(text = { Text(t.copyUpToDown) }, onClick = onCopyUpToDown)
            }
        }
    }
}

@Composable
private fun HintStrip(count: Int) {
    val cs = MaterialTheme.colorScheme
    val t = LocalStrings.current
    Text(
        text = t.editorHint(count),
        color = cs.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(cs.surfaceVariant, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun StationRow(
    index: Int,
    stop: EditableStop,
    isDragging: Boolean,
    dragOffsetY: Float,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onInsertAbove: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val t = LocalStrings.current
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 160.dp.toPx() }
    val triggerPx = with(density) { 72.dp.toPx() }

    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(StationRowHeight)
            .zIndex(if (isDragging) 1f else 0f)
    ) {
        if (offsetX < 0f) {
            Box(
                Modifier.fillMaxSize().background(cs.error),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    t.deleteAction,
                    color = cs.onError,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(end = 28.dp)
                )
            }
        } else if (offsetX > 0f) {
            Box(
                Modifier.fillMaxSize().background(cs.primaryContainer),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    t.insertAbove,
                    color = cs.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(offsetX.roundToInt(), if (isDragging) dragOffsetY.roundToInt() else 0)
                }
                .graphicsLayer {
                    if (isDragging) {
                        shadowElevation = 6.dp.toPx()
                        scaleX = 1.02f
                        scaleY = 1.02f
                        alpha = 0.98f
                    }
                }
                .background(if (isDragging) cs.surfaceVariant else cs.surface)
                .pointerInput(stop.uid) {
                    detectHorizontalDragGestures(
                        onDragStart = { offsetX = 0f },
                        onHorizontalDrag = { change, amount ->
                            if (!isDragging) {
                                change.consume()
                                offsetX = (offsetX + amount).coerceIn(-maxSwipePx, maxSwipePx)
                            }
                        },
                        onDragEnd = {
                            if (!isDragging) {
                                if (offsetX <= -triggerPx) onDelete()
                                else if (offsetX >= triggerPx) onInsertAbove()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f }
                    )
                }
                .pointerInput(stop.uid) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, amount ->
                            change.consume()
                            onDragDelta(amount.y)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(cs.primaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    color = cs.onPrimaryContainer,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stop.name.ifBlank { t.untitledStop },
                    color = cs.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(stop.voice.ifBlank { t.noVoice })
                        if (stop.big.isNotBlank()) append(" · ").append(typeLabel(stop.big, t))
                        if (stop.limit.isNotBlank()) append(" · ").append(t.limitShort).append(" ").append(stop.limit)
                    },
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 拖拽手柄：同时支持“按住手柄直接拖”
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(stop.uid) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { change, amount ->
                                change.consume()
                                onDragDelta(amount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("≡", color = cs.onSurfaceVariant, fontSize = 20.sp)
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(cs.outlineVariant)
        )
    }
}

@Composable
private fun StationEditDialog(
    target: EditTarget,
    onDismiss: () -> Unit,
    onConfirm: (EditableStop) -> Unit
) {
    var draft by remember(target) { mutableStateOf(target.stop) }
    var error by remember(target) { mutableStateOf<String?>(null) }
    val cs = MaterialTheme.colorScheme
    val t = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (target.isNew) t.addStopTitle else t.editStopTitle) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorField(t.fVoice, draft.voice) { draft = draft.copy(voice = it) }
                EditorField(t.fName, draft.name) { draft = draft.copy(name = it) }
                EditorField(t.fLon, draft.lon, decimal = true) {
                    draft = draft.copy(lon = it)
                }
                EditorField(t.fLat, draft.lat, decimal = true) {
                    draft = draft.copy(lat = it)
                }
                EditorField(t.fAngle, draft.angle, decimal = true) { draft = draft.copy(angle = it) }
                EditorField(t.fAdIn, draft.adIn) { draft = draft.copy(adIn = it) }
                EditorField(t.fAdOut, draft.adOut) { draft = draft.copy(adOut = it) }
                EditorField(t.fTipIn, draft.tipIn) { draft = draft.copy(tipIn = it) }
                EditorField(t.fTipOut, draft.tipOut) { draft = draft.copy(tipOut = it) }
                EditorField(t.fExtIn, draft.extIn) { draft = draft.copy(extIn = it) }
                EditorField(t.fExtOut, draft.extOut) { draft = draft.copy(extOut = it) }
                EditorField(t.fLimit, draft.limit, number = true) { draft = draft.copy(limit = it) }
                EditorField(t.fApproach, draft.before, number = true) { draft = draft.copy(before = it) }

                Text(t.stopType, color = cs.onSurface, style = MaterialTheme.typography.labelLarge)
                BigStationOptions(draft.big) { draft = draft.copy(big = it) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        t.externalAudio,
                        color = cs.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(if (draft.external) t.yes else t.no, color = cs.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = draft.external,
                        onCheckedChange = { draft = draft.copy(external = it) }
                    )
                }

                error?.let { Text(it, color = cs.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (draft.voice.isBlank() || draft.name.isBlank()) {
                    error = t.requiredError
                } else {
                    onConfirm(draft)
                }
            }) { Text(t.confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t.cancel) }
        }
    )
}

@Composable
private fun BigStationOptions(selected: String, onSelect: (String) -> Unit) {
    val t = LocalStrings.current
    val options = listOf("起点站", "大站", "小站", "终点站")
    Column {
        options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == option, onClick = { onSelect(option) })
                        Text(typeLabel(option, t), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    number: Boolean = false,
    decimal: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                number -> KeyboardType.Number
                decimal -> KeyboardType.Decimal
                else -> KeyboardType.Text
            }
        ),
        colors = editorFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = MaterialTheme.colorScheme.primary
)

private fun typeLabel(canonical: String, t: AppStrings): String = when (canonical) {
    "起点站" -> t.typeStart
    "大站" -> t.typeMajor
    "小站" -> t.typeMinor
    "终点站" -> t.typeEnd
    else -> canonical
}
