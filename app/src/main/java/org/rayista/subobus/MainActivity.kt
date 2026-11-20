package org.rayista.subobus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import org.rayista.subobus.i18n.LocalStrings
import org.rayista.subobus.i18n.stringsFor
import org.rayista.subobus.ui.MainScreen
import org.rayista.subobus.ui.RouteEditorScreen
import org.rayista.subobus.ui.SettingsScreen
import org.rayista.subobus.ui.SuboTheme
import org.rayista.subobus.vm.BusViewModel
import org.rayista.subobus.vm.RouteEditorViewModel

class MainActivity : ComponentActivity() {

    private val vm: BusViewModel by viewModels()
    private val editorVm: RouteEditorViewModel by viewModels()

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.startLocation() else vm.locationDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state = vm.uiState

            // 前台保持屏幕常亮（可在设置里关闭）
            DisposableEffect(state.keepScreenOn) {
                if (state.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            SuboTheme(themeId = state.themeId) {
              CompositionLocalProvider(LocalStrings provides stringsFor(state.language)) {
                var showSettings by remember { mutableStateOf(false) }
                var editorOpen by remember { mutableStateOf(false) }

                BackHandler(enabled = showSettings && !editorOpen) { showSettings = false }

                if (editorOpen) {
                    RouteEditorScreen(
                        vm = editorVm,
                        onBack = { editorOpen = false },
                        onSaved = { route -> vm.refreshRoutes(route) }
                    )
                } else if (showSettings) {
                    SettingsScreen(
                        state = state,
                        onBack = { showSettings = false },
                        onImportRoutesZip = { vm.importRoutesZip(it) },
                        onImportAudioZip = { vm.importAudioZip(it) },
                        onImportRules = { vm.importRules(it) },
                        onImportAllZip = { vm.importAllZip(it) },
                        onExportAll = { vm.exportAllData(it) },
                        onSelectRoute = {
                            vm.selectRoute(it)
                            showSettings = false
                        },
                        onEditRoute = { route ->
                            editorVm.open(route, false)
                            editorOpen = true
                        },
                        onNewRoute = {
                            editorVm.open("", true)
                            editorOpen = true
                        },
                        onLimitOverride = { vm.setLimitOverride(it) },
                        onManualLimit = { vm.setManualLimit(it) },
                        onOverspeedEnabled = { vm.setOverspeedEnabled(it) },
                        onSensitivityOverride = { vm.setSensitivityOverride(it) },
                        onArrivalSensitivity = { vm.setArrivalSensitivity(it) },
                        onOverlapSensitivity = { vm.setOverlapSensitivity(it) },
                        onDepartureSensitivity = { vm.setDepartureSensitivity(it) },
                        onDoubleTapOverwrite = { vm.setDoubleTapOverwrite(it) },
                        onRuleOverride = { vm.setRuleOverride(it) },
                        onRuleFlag = { type, enabled -> vm.setRuleEnabled(type, enabled) },
                        onSeparateChannels = { vm.setSeparateChannels(it) },
                        onThemeId = { vm.setThemeId(it) },
                        onKeepScreenOn = { vm.setKeepScreenOn(it) },
                        onHapticEnabled = { vm.setHapticEnabled(it) },
                        onLoudnessNormalization = { vm.setLoudnessNormalization(it) },
                        onLanguage = { vm.setLanguage(it) }
                    )
                } else {
                    MainScreen(
                        state = state,
                        onAnnounce = { vm.announce() },
                        onStop = { vm.stop() },
                        onRepeat = { vm.repeat() },
                        onSettings = { showSettings = true },
                        onDirection = { vm.toggleDirection() },
                        onKey = { vm.pressKey(it) },
                        onPrevStation = { vm.moveStation(-1) },
                        onNextStation = { vm.moveStation(1) },
                        onOverwriteLocation = { vm.overwriteStationLocation() }
                    )
                }
              }
            }
        }

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) vm.startLocation() else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
