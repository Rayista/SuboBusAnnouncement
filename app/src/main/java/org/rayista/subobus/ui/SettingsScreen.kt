package org.rayista.subobus.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.rayista.subobus.i18n.AppLanguage
import org.rayista.subobus.i18n.AppStrings
import org.rayista.subobus.i18n.LocalStrings
import org.rayista.subobus.vm.UiState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class SettingsActions(
    val back: () -> Unit,
    val routesZip: (Uri) -> Unit,
    val audioZip: (Uri) -> Unit,
    val rules: (Uri) -> Unit,
    val allZip: (Uri) -> Unit,
    val exportAll: () -> Unit,
    val selectRoute: (String) -> Unit,
    val editRoute: (String) -> Unit,
    val newRoute: () -> Unit,
    val limitOverride: (Boolean) -> Unit,
    val manualLimit: (Int) -> Unit,
    val overspeedEnabled: (Boolean) -> Unit,
    val overspeedRepeat: (Int) -> Unit,
    val overspeedInterval: (Int) -> Unit,
    val sensitivityOverride: (Boolean) -> Unit,
    val arrivalSensitivity: (Int) -> Unit,
    val overlapSensitivity: (Int) -> Unit,
    val departureSensitivity: (Int) -> Unit,
    val doubleTapOverwrite: (Boolean) -> Unit,
    val ruleOverride: (Boolean) -> Unit,
    val ruleFlag: (String, Boolean) -> Unit,
    val separateChannels: (Boolean) -> Unit,
    val themeId: (String) -> Unit,
    val keepScreenOn: (Boolean) -> Unit,
    val hapticEnabled: (Boolean) -> Unit,
    val loudnessNormalization: (Boolean) -> Unit,
    val language: (String) -> Unit,
    val backgroundRun: (Boolean) -> Unit
)

@Composable
fun SettingsScreen(
    state: UiState,
    onBack: () -> Unit,
    onImportRoutesZip: (Uri) -> Unit,
    onImportAudioZip: (Uri) -> Unit,
    onImportRules: (Uri) -> Unit,
    onImportAllZip: (Uri) -> Unit,
    onExportAll: (Uri) -> Unit,
    onSelectRoute: (String) -> Unit,
    onEditRoute: (String) -> Unit,
    onNewRoute: () -> Unit,
    onLimitOverride: (Boolean) -> Unit,
    onManualLimit: (Int) -> Unit,
    onOverspeedEnabled: (Boolean) -> Unit,
    onOverspeedRepeat: (Int) -> Unit,
    onOverspeedInterval: (Int) -> Unit,
    onSensitivityOverride: (Boolean) -> Unit,
    onArrivalSensitivity: (Int) -> Unit,
    onOverlapSensitivity: (Int) -> Unit,
    onDepartureSensitivity: (Int) -> Unit,
    onDoubleTapOverwrite: (Boolean) -> Unit,
    onRuleOverride: (Boolean) -> Unit,
    onRuleFlag: (String, Boolean) -> Unit,
    onSeparateChannels: (Boolean) -> Unit,
    onThemeId: (String) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onHapticEnabled: (Boolean) -> Unit,
    onLoudnessNormalization: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onBackgroundRun: (Boolean) -> Unit
) {
    // 必须用 ACTION_OPEN_DOCUMENT（OpenDocument），不能用 ACTION_GET_CONTENT：
    // 部分车机/系统的“下载”提供方（如 DownloadStorageProvider）只对 OPEN_DOCUMENT 授权，
    // 用 GET_CONTENT 拿到的 URI 在读取时会报 Permission Denial。
    val routesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportRoutesZip(uri)
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportAudioZip(uri)
    }
    val rulesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportRules(uri)
    }
    val allPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportAllZip(uri)
    }
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) onExportAll(uri)
    }

    val actions = SettingsActions(
        back = onBack,
        routesZip = onImportRoutesZip,
        audioZip = onImportAudioZip,
        rules = onImportRules,
        allZip = onImportAllZip,
        exportAll = {
            val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").format(LocalDateTime.now())
            exportPicker.launch("subo-backup-$stamp.zip")
        },
        selectRoute = onSelectRoute,
        editRoute = onEditRoute,
        newRoute = onNewRoute,
        limitOverride = onLimitOverride,
        manualLimit = onManualLimit,
        overspeedEnabled = onOverspeedEnabled,
        overspeedRepeat = onOverspeedRepeat,
        overspeedInterval = onOverspeedInterval,
        sensitivityOverride = onSensitivityOverride,
        arrivalSensitivity = onArrivalSensitivity,
        overlapSensitivity = onOverlapSensitivity,
        departureSensitivity = onDepartureSensitivity,
        doubleTapOverwrite = onDoubleTapOverwrite,
        ruleOverride = onRuleOverride,
        ruleFlag = onRuleFlag,
        separateChannels = onSeparateChannels,
        themeId = onThemeId,
        keepScreenOn = onKeepScreenOn,
        hapticEnabled = onHapticEnabled,
        loudnessNormalization = onLoudnessNormalization,
        language = onLanguage,
        backgroundRun = onBackgroundRun
    )
    val launchers = SettingsLaunchers(
        { routesPicker.launch(arrayOf("*/*")) },
        { audioPicker.launch(arrayOf("*/*")) },
        { rulesPicker.launch(arrayOf("*/*")) },
        { allPicker.launch(arrayOf("*/*")) }
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (maxWidth > maxHeight) {
            LandscapeSettings(state, actions, launchers)
        } else {
            PortraitSettings(state, actions, launchers)
        }
    }
}

private data class SettingsLaunchers(
    val pickRoutes: () -> Unit,
    val pickAudio: () -> Unit,
    val pickRules: () -> Unit,
    val pickAll: () -> Unit
)

/** 设置页顺序：主题 → 通用 → 报站功能 → 实验性 → 选择线路 → 数据导入导出 */
@Composable
private fun PortraitSettings(
    state: UiState,
    actions: SettingsActions,
    launchers: SettingsLaunchers
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TitleRow(actions.back)
        ThemeSection(state, actions.themeId)
        GeneralSettings(state, actions)
        AnnounceSettings(state, actions)
        ExperimentalSettings(state, actions)
        RouteSection(state, actions.selectRoute, actions.editRoute, actions.newRoute)
        DataSection(launchers, actions.exportAll)
        FeedbackText(state.feedback)
    }
}

@Composable
private fun LandscapeSettings(
    state: UiState,
    actions: SettingsActions,
    launchers: SettingsLaunchers
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TitleRow(actions.back)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeSection(state, actions.themeId)
                GeneralSettings(state, actions)
                AnnounceSettings(state, actions)
                ExperimentalSettings(state, actions)
                DataSection(launchers, actions.exportAll)
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RouteSection(state, actions.selectRoute, actions.editRoute, actions.newRoute)
            Spacer(Modifier.weight(1f))
            FeedbackText(state.feedback)
        }
    }
}

@Composable
private fun TitleRow(onBack: () -> Unit) {
    val t = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onBack) { Text(t.back) }
        Spacer(Modifier.width(8.dp))
        Text(
            t.settingsTitle,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ---------------- 主题配色 ---------------- */
@Composable
private fun ThemeSection(state: UiState, onSelect: (String) -> Unit) {
    val t = LocalStrings.current
    GroupTitle(t.themeTitle)
    appThemeOptions.chunked(4).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { theme ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(theme.scheme.primary)
                            .border(
                                if (theme.id == state.themeId) 3.dp else 1.dp,
                                if (theme.id == state.themeId) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                CircleShape
                            )
                            .clickable { onSelect(theme.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (theme.id == state.themeId) {
                            Text("✓", color = theme.scheme.onPrimary, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = themeLabel(theme.id, t),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/* ---------------- 通用 ---------------- */
@Composable
private fun GeneralSettings(state: UiState, actions: SettingsActions) {
    val t = LocalStrings.current
    GroupTitle(t.general)
    SwitchRow(
        title = t.keepScreenOn,
        checked = state.keepScreenOn,
        onChange = actions.keepScreenOn
    )
    SwitchRow(
        title = t.haptic,
        checked = state.hapticEnabled,
        onChange = actions.hapticEnabled
    )
    SwitchRow(
        title = t.loudness,
        checked = state.loudnessNormalization,
        onChange = actions.loudnessNormalization
    )
    SwitchRow(
        title = t.backgroundRun,
        checked = state.backgroundRun,
        onChange = actions.backgroundRun
    )
    LanguageRow(current = state.language, onSelect = actions.language)
}

@Composable
private fun LanguageRow(current: String, onSelect: (String) -> Unit) {
    val t = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    val language = AppLanguage.fromId(current)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            t.languageLabel,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Box {
            OutlinedButton(onClick = { open = true }) {
                Text(language.label + "  ▾", color = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                AppLanguage.entries.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (item == language) item.label + "  ✓" else item.label,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            open = false
                            onSelect(item.id)
                        }
                    )
                }
            }
        }
    }
}

private fun themeLabel(id: String, t: AppStrings): String = when (id) {
    "light_blue" -> t.themeSkyBlue
    "light_teal" -> t.themeTeal
    "light_amber" -> t.themeAmber
    "light_violet" -> t.themeViolet
    "dark_blue" -> t.themeNightBlue
    "dark_forest" -> t.themeForest
    "dark_coffee" -> t.themeCoffee
    "pure_black" -> t.themePureBlack
    else -> id
}

/* ---------------- 报站功能设置 ---------------- */
@Composable
private fun AnnounceSettings(state: UiState, actions: SettingsActions) {
    val t = LocalStrings.current
    GroupTitle(t.announceSettings)

    SubTitle(t.limitTitle)
    SwitchRow(
        title = t.limitOverride,
        checked = state.limitOverride,
        onChange = actions.limitOverride
    )
    if (state.limitOverride) {
        NumberField(t.manualLimit, state.manualLimit, t.kmh, actions.manualLimit)
    }
    CheckRow(
        title = t.overspeed,
        checked = state.overspeedEnabled,
        onChange = actions.overspeedEnabled
    )
    if (state.overspeedEnabled) {
        NumberField(t.overspeedRepeat, state.overspeedRepeat, t.timesUnit, actions.overspeedRepeat)
        NumberField(t.overspeedInterval, state.overspeedInterval, t.secondUnit, actions.overspeedInterval)
    }

    SubTitle(t.sensitivityTitle)
    SwitchRow(
        title = t.sensitivityOverride,
        checked = state.sensitivityOverride,
        onChange = actions.sensitivityOverride
    )
    if (state.sensitivityOverride) {
        NumberField(t.arrivalSensitivity, state.arrivalSensitivity, t.meterUnit, actions.arrivalSensitivity)
        NumberField(t.overlapSensitivity, state.overlapSensitivity, t.meterUnit, actions.overlapSensitivity)
        NumberField(t.departureSensitivity, state.departureSensitivity, t.meterUnit, actions.departureSensitivity)
    }

    SubTitle(t.rulesTitle)
    SwitchRow(
        title = t.rulesOverride,
        checked = state.ruleOverride,
        onChange = actions.ruleOverride
    )
    if (state.ruleOverride) {
        CheckRow(t.ruleStart, state.ruleStart) { actions.ruleFlag("起点播报", it) }
        CheckRow(t.ruleEnd, state.ruleEnd) { actions.ruleFlag("终点播报", it) }
        CheckRow(t.ruleEndForecast, state.ruleEndForecast) { actions.ruleFlag("终点预报", it) }
        CheckRow(t.ruleArrival, state.ruleArrival) { actions.ruleFlag("进站播报", it) }
        CheckRow(t.ruleDeparture, state.ruleDeparture) { actions.ruleFlag("出站播报", it) }
        CheckRow(t.ruleArrivalExternal, state.ruleArrivalExternal) { actions.ruleFlag("进站外音", it) }
        CheckRow(t.ruleDepartureExternal, state.ruleDepartureExternal) { actions.ruleFlag("出站外音", it) }
        Text(
            text = t.rulesHint,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

/* ---------------- 实验性功能 ---------------- */
@Composable
private fun ExperimentalSettings(state: UiState, actions: SettingsActions) {
    val t = LocalStrings.current
    GroupTitle(t.experimental)
    CheckRow(
        title = t.separateChannels,
        checked = state.separateChannels,
        onChange = actions.separateChannels
    )
    Text(
        text = t.separateChannelsHint,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
    CheckRow(
        title = t.doubleTap,
        checked = state.doubleTapOverwrite,
        onChange = actions.doubleTapOverwrite
    )
    Text(
        text = t.doubleTapHint,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
}

/* ---------------- 数据导入导出 ---------------- */
@Composable
private fun DataSection(launchers: SettingsLaunchers, onExportAll: () -> Unit) {
    val t = LocalStrings.current
    GroupTitle(t.dataTitle)
    SettingButton(t.importAll, launchers.pickAll)
    SettingButton(t.importRoutes, launchers.pickRoutes)
    SettingButton(t.importAudio, launchers.pickAudio)
    SettingButton(t.importRules, launchers.pickRules)
    ExportButton(t.exportAll, onExportAll)
    Text(
        text = t.dataHint,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
}

/* ---------------- 选择线路（可输入搜索 + 内联候选） ---------------- */
@Composable
private fun RouteSection(
    state: UiState,
    onSelect: (String) -> Unit,
    onEditRoute: (String) -> Unit,
    onNewRoute: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val t = LocalStrings.current
    GroupTitle(t.routeTitle)

    if (state.routes.isEmpty()) {
        Text(
            t.noRoutes,
            color = cs.onSurfaceVariant,
            fontSize = 13.sp
        )
    } else {
        var expanded by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf(state.route) }

        LaunchedEffect(state.route) {
            if (!expanded) query = state.route
        }

        val keyword = query.trim()
        val filtered = if (keyword.isBlank()) {
            state.routes
        } else {
            state.routes.filter { it.contains(keyword, ignoreCase = true) }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { input ->
                query = input
                expanded = true
            },
            label = { Text(t.routeSearchLabel) },
            placeholder = { Text(t.routePlaceholder) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Text("▼", color = cs.onSurfaceVariant, fontSize = 12.sp)
                }
            },
            singleLine = true,
            textStyle = TextStyle(color = cs.onSurface, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (keyword.isNotBlank()) {
                        filtered.firstOrNull()?.let { route ->
                            query = route
                            expanded = false
                            onSelect(route)
                        }
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = cs.onSurface,
                focusedTextColor = cs.onSurface,
                cursorColor = cs.primary,
                unfocusedBorderColor = cs.outline,
                focusedBorderColor = cs.primary,
                unfocusedLabelColor = cs.onSurfaceVariant,
                focusedLabelColor = cs.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .background(cs.surfaceVariant, MaterialTheme.shapes.small)
                    .border(1.dp, cs.outlineVariant, MaterialTheme.shapes.small)
            ) {
                if (filtered.isEmpty()) {
                    Text(
                        text = t.noMatch,
                        color = cs.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        filtered.forEach { route ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = route
                                        expanded = false
                                        onSelect(route)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (route == state.route) {
                                        routeLabel(route, t) + t.currentMark
                                    } else {
                                        routeLabel(route, t)
                                    },
                                    color = cs.onSurface,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onNewRoute,
            modifier = Modifier.weight(1f)
        ) { Text(t.newRoute) }
        Button(
            onClick = { onEditRoute(state.route) },
            enabled = state.route.isNotBlank(),
            modifier = Modifier.weight(1f)
        ) { Text(t.editRoute) }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SubTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun CheckRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Spacer(Modifier.width(4.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
    }
}

/** 只允许 1-999 的数字输入。 */
@Composable
private fun NumberField(
    label: String,
    value: Int,
    unit: String,
    onChange: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = cs.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(3)
                text = digits
                digits.toIntOrNull()?.let { if (it in 1..999) onChange(it) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = cs.onSurface, fontSize = 15.sp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = cs.onSurface,
                focusedTextColor = cs.onSurface,
                cursorColor = cs.primary,
                unfocusedBorderColor = cs.outline,
                focusedBorderColor = cs.primary
            ),
            modifier = Modifier.width(96.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(unit, color = cs.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun FeedbackText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(12.dp)
    )
}

@Composable
private fun SettingButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Text(text, fontSize = 18.sp)
    }
}

@Composable
private fun ExportButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
