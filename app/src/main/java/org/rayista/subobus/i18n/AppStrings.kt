package org.rayista.subobus.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/** 界面语言。CSV、站名、报站语音等内容不翻译。 */
enum class AppLanguage(val id: String, val label: String) {
    ZH_CN("zh_cn", "简体中文"),
    ZH_TW("zh_tw", "繁體中文"),
    EN("en", "English");

    companion object {
        fun fromId(id: String?): AppLanguage = entries.firstOrNull { it.id == id } ?: ZH_CN
    }
}

data class AppStrings(
    val language: AppLanguage,

    /* 通用 */
    val back: String,
    val save: String,
    val cancel: String,
    val confirm: String,
    val yes: String,
    val no: String,
    val delete: String,
    val continueText: String,

    /* 主界面 */
    val currentStop: String,
    val nextStop: String,
    val up: String,
    val down: String,
    val loop: String,
    val oneWay: String,
    val announce: String,
    val stopPlayback: String,
    val repeat: String,
    val settings: String,
    val keypad: String,
    val currentSpeed: String,
    val speedLimit: String,
    val kmh: String,
    val meterUnit: String,
    val gpsStrong: String,
    val gpsMedium: String,
    val gpsWeak: String,
    val gpsNone: String,
    val routeSuffix: String,

    /* 设置 */
    val settingsTitle: String,
    val languageTitle: String,
    val languageLabel: String,
    val themeTitle: String,
    val themeSkyBlue: String,
    val themeTeal: String,
    val themeAmber: String,
    val themeViolet: String,
    val themeNightBlue: String,
    val themeForest: String,
    val themeCoffee: String,
    val themePureBlack: String,
    val general: String,
    val keepScreenOn: String,
    val haptic: String,
    val loudness: String,
    val announceSettings: String,
    val limitTitle: String,
    val limitOverride: String,
    val manualLimit: String,
    val overspeed: String,
    val overspeedRepeat: String,
    val overspeedInterval: String,
    val timesUnit: String,
    val secondUnit: String,
    val notifActions: String,
    val sensitivityTitle: String,
    val sensitivityOverride: String,
    val arrivalSensitivity: String,
    val overlapSensitivity: String,
    val departureSensitivity: String,
    val rulesTitle: String,
    val rulesOverride: String,
    val ruleStart: String,
    val ruleEnd: String,
    val ruleEndForecast: String,
    val ruleArrival: String,
    val ruleDeparture: String,
    val ruleArrivalExternal: String,
    val ruleDepartureExternal: String,
    val rulesHint: String,
    val experimental: String,
    val separateChannels: String,
    val separateChannelsHint: String,
    val doubleTap: String,
    val doubleTapHint: String,
    val routeTitle: String,
    val routeSearchLabel: String,
    val routePlaceholder: String,
    val noRoutes: String,
    val noMatch: String,
    val currentMark: String,
    val newRoute: String,
    val editRoute: String,
    val dataTitle: String,
    val importAll: String,
    val importRoutes: String,
    val importAudio: String,
    val importRules: String,
    val exportAll: String,
    val dataHint: String,

    /* 线路编辑器 */
    val newRouteTitle: String,
    val routeNameLabel: String,
    val addStopTitle: String,
    val editStopTitle: String,
    val deleteAction: String,
    val insertAbove: String,
    val untitledStop: String,
    val noVoice: String,
    val limitShort: String,
    val fVoice: String,
    val fName: String,
    val fLon: String,
    val fLat: String,
    val fAngle: String,
    val fAdIn: String,
    val fAdOut: String,
    val fTipIn: String,
    val fTipOut: String,
    val fExtIn: String,
    val fExtOut: String,
    val fLimit: String,
    val fApproach: String,
    val stopType: String,
    val typeStart: String,
    val typeMajor: String,
    val typeMinor: String,
    val typeEnd: String,
    val externalAudio: String,
    val requiredError: String,
    val unsavedTitle: String,
    val unsavedText: String,
    val discard: String,
    val stay: String,
    val overwriteTitle: String,
    val overwriteText: String,
    val overwriteContinue: String,
    val savedToast: String,
    val nameEmpty: String,
    val nameInvalid: String,
    val needOneStop: String,
    val noUpStops: String,
    val emptyUp: String,
    val emptyDown: String,
    val copyUpToDown: String,
    val nameExists: String,

    /* 运行提示 */
    val noRouteSelected: String,
    val noStops: String,
    val atStart: String,
    val atEnd: String,
    val nothingToRepeat: String,
    val noAudioPlaying: String,
    val stopped: String,
    val importingRoutes: String,
    val importingAudio: String,
    val importingRules: String,
    val importingAll: String,
    val exporting: String,
    val noRoutesAvailable: String,
    val availableRoutes: String,
    val notZip: String,
    val readZipFailed: String,
    val readFileFailed: String,
    val rulesImported: String,
    val importNothing: String,
    val exportCannotWrite: String,
    val exportNothing: String,
    val ready: String,
    val dataReady: String,
    val cannotSwitch: String,
    val switchedTo: String,
    val noLocation: String,
    val overwriteFailed: String,
    val overwriteSuccess: String,
    val routeDataEmpty: String,
    val repeatDone: String,
    val unknownError: String,

    /* 读取所选文件被拒 */
    val readDenied: String,

    /* 前台服务通知 */
    val notifChannel: String,
    val notifNoRoute: String
) {
    fun gpsLevel(raw: String): String = when (raw) {
        "强" -> gpsStrong
        "中" -> gpsMedium
        "弱" -> gpsWeak
        else -> gpsNone
    }

    fun distance(meters: Int): String = when (language) {
        AppLanguage.ZH_CN -> "距 $meters 米"
        AppLanguage.ZH_TW -> "距 $meters 公尺"
        AppLanguage.EN -> "$meters m away"
    }

    val distanceUnknown: String
        get() = when (language) {
            AppLanguage.ZH_CN -> "距离 —"
            AppLanguage.ZH_TW -> "距離 —"
            AppLanguage.EN -> "Distance —"
        }

    fun editorHint(count: Int): String = when (language) {
        AppLanguage.ZH_CN -> "共 $count 个站点 · 左滑删除 / 右滑在上方插入 / 长按拖动排序"
        AppLanguage.ZH_TW -> "共 $count 個站點 · 左滑刪除 / 右滑在上方插入 / 長按拖曳排序"
        AppLanguage.EN -> "$count stops · Swipe left to delete · Swipe right to insert above · Hold to reorder"
    }

    fun renamed(name: String): String = when (language) {
        AppLanguage.ZH_CN -> "线路名称已改为 $name"
        AppLanguage.ZH_TW -> "路線名稱已改為 $name"
        AppLanguage.EN -> "Renamed to $name"
    }

    fun routeSelected(name: String): String = when (language) {
        AppLanguage.ZH_CN -> "已选择 $name"
        AppLanguage.ZH_TW -> "已選擇 $name"
        AppLanguage.EN -> "Selected $name"
    }

    fun routeSaved(name: String): String = when (language) {
        AppLanguage.ZH_CN -> "线路已保存：$name"
        AppLanguage.ZH_TW -> "路線已儲存：$name"
        AppLanguage.EN -> "Saved $name"
    }

    fun selectedStation(name: String): String = when (language) {
        AppLanguage.ZH_CN -> "已选择 $name，按“报站”播报该站"
        AppLanguage.ZH_TW -> "已選擇 $name，按「報站」播報該站"
        AppLanguage.EN -> "Selected $name. Tap Announce to play."
    }

    fun serviceToneMissing(key: String): String = when (language) {
        AppLanguage.ZH_CN -> "服务音$key.mp3 不存在，已忽略"
        AppLanguage.ZH_TW -> "服務音$key.mp3 不存在，已忽略"
        AppLanguage.EN -> "Service tone $key not found"
    }

    fun serviceTonePlaying(key: String): String = when (language) {
        AppLanguage.ZH_CN -> "服务音$key 播放中"
        AppLanguage.ZH_TW -> "服務音$key 播放中"
        AppLanguage.EN -> "Playing service tone $key"
    }

    fun serviceToneDone(key: String): String = when (language) {
        AppLanguage.ZH_CN -> "服务音$key 播放完毕"
        AppLanguage.ZH_TW -> "服務音$key 播放完畢"
        AppLanguage.EN -> "Service tone $key finished"
    }

    fun endOfRouteSwitch(mode: String): String = when (language) {
        AppLanguage.ZH_CN -> "已到终点站，切换为$mode"
        AppLanguage.ZH_TW -> "已到終點站，切換為$mode"
        AppLanguage.EN -> "Terminus reached. Switched to $mode."
    }

    val endOfRouteRestart: String
        get() = when (language) {
            AppLanguage.ZH_CN -> "已到终点站，从起点重新开始"
            AppLanguage.ZH_TW -> "已到終點站，從起點重新開始"
            AppLanguage.EN -> "Terminus reached. Restarting from the first stop."
        }

    fun routesAvailable(list: String): String = when (language) {
        AppLanguage.ZH_CN -> "可选线路：$list"
        AppLanguage.ZH_TW -> "可選路線：$list"
        AppLanguage.EN -> "Routes: $list"
    }

    fun importDone(routes: Int, audio: Int, rules: Int, ignored: Int): String {
        return when (language) {
            AppLanguage.ZH_CN -> buildString {
                append("导入完成：")
                val parts = mutableListOf<String>()
                if (routes > 0) parts.add("线路 $routes")
                if (audio > 0) parts.add("音频 $audio")
                if (rules > 0) parts.add("规则 $rules")
                append(if (parts.isEmpty()) "没有匹配到可导入的文件" else parts.joinToString("、"))
                if (ignored > 0) append("，忽略 $ignored 个")
            }

            AppLanguage.ZH_TW -> buildString {
                append("匯入完成：")
                val parts = mutableListOf<String>()
                if (routes > 0) parts.add("路線 $routes")
                if (audio > 0) parts.add("音訊 $audio")
                if (rules > 0) parts.add("規則 $rules")
                append(if (parts.isEmpty()) "沒有符合的可匯入檔案" else parts.joinToString("、"))
                if (ignored > 0) append("，略過 $ignored 個")
            }

            AppLanguage.EN -> buildString {
                val parts = mutableListOf<String>()
                if (routes > 0) parts.add("$routes route(s)")
                if (audio > 0) parts.add("$audio audio")
                if (rules > 0) parts.add("$rules rule(s)")
                append(if (parts.isEmpty()) "No matching files" else "Imported " + parts.joinToString(", "))
                if (ignored > 0) append(", $ignored ignored")
            }
        }
    }

    fun exportDone(count: Int, sizeMb: Long): String = when (language) {
        AppLanguage.ZH_CN -> "已导出 $count 个文件（$sizeMb MB）"
        AppLanguage.ZH_TW -> "已匯出 $count 個檔案（$sizeMb MB）"
        AppLanguage.EN -> "Exported $count file(s) ($sizeMb MB)"
    }

    /** 把内部播报类型（数据键，始终是简体中文）转成当前语言显示。 */
    fun speedLine(speed: Int): String = when (language) {
        AppLanguage.ZH_CN -> "当前速度：$speed km/h"
        AppLanguage.ZH_TW -> "目前車速：$speed km/h"
        AppLanguage.EN -> "Speed: $speed km/h"
    }

    fun limitLine(limit: Int): String = when (language) {
        AppLanguage.ZH_CN -> "限速：$limit km/h"
        AppLanguage.ZH_TW -> "限速：$limit km/h"
        AppLanguage.EN -> "Limit: $limit km/h"
    }

    fun stepType(canonical: String): String = when (canonical) {
        "起点播报" -> ruleStart
        "终点播报" -> ruleEnd
        "终点预报" -> ruleEndForecast
        "进站播报" -> ruleArrival
        "出站播报" -> ruleDeparture
        "进站外音" -> ruleArrivalExternal
        "出站外音" -> ruleDepartureExternal
        else -> canonical
    }

    fun announceFinished(type: String): String = when (language) {
        AppLanguage.ZH_CN -> "${stepType(type)} 播报完毕，再按“报站”进入下一项"
        AppLanguage.ZH_TW -> "${stepType(type)} 播報完畢，再按「報站」進入下一項"
        AppLanguage.EN -> "${stepType(type)} finished. Tap Announce for the next item."
    }

    fun repeatPrefix(detail: String): String = when (language) {
        AppLanguage.ZH_CN -> "重复：$detail"
        AppLanguage.ZH_TW -> "重複：$detail"
        AppLanguage.EN -> "Replay: $detail"
    }

    fun importFailed(reason: String): String = when (language) {
        AppLanguage.ZH_CN -> "导入失败：$reason"
        AppLanguage.ZH_TW -> "匯入失敗：$reason"
        AppLanguage.EN -> "Import failed: $reason"
    }

    fun failed(reason: String): String = when (language) {
        AppLanguage.ZH_CN -> "失败：$reason"
        AppLanguage.ZH_TW -> "失敗：$reason"
        AppLanguage.EN -> "Failed: $reason"
    }
}

val ZhCnStrings = AppStrings(
    language = AppLanguage.ZH_CN,
    back = "返回", save = "保存", cancel = "取消", confirm = "确定",
    yes = "是", no = "否", delete = "删除", continueText = "继续",
    currentStop = "当前站", nextStop = "下一站：",
    up = "上行", down = "下行", loop = "环线", oneWay = "单向",
    announce = "报站", stopPlayback = "终止", repeat = "重复", settings = "设置",
    keypad = "数字键盘", currentSpeed = "当前速度", speedLimit = "限速",
    kmh = "km/h", meterUnit = "米",
    gpsStrong = "强", gpsMedium = "中", gpsWeak = "弱", gpsNone = "无",
    routeSuffix = " 路",
    settingsTitle = "报站器设置", languageTitle = "语言", languageLabel = "界面语言",
    themeTitle = "主题配色",
    themeSkyBlue = "天空蓝", themeTeal = "青碧", themeAmber = "暖阳", themeViolet = "紫罗兰",
    themeNightBlue = "夜空蓝", themeForest = "深林绿", themeCoffee = "暖棕", themePureBlack = "纯黑",
    general = "通用", keepScreenOn = "前台保持屏幕常亮", haptic = "按键震动反馈",
    loudness = "响度均衡（自动提升小声录音）",
    announceSettings = "报站功能设置", limitTitle = "限速设置",
    limitOverride = "是否覆写线路设定（限速）", manualLimit = "手动限速", overspeed = "是否播报超速提醒",
    overspeedRepeat = "超速提醒播报次数", overspeedInterval = "超速提醒间隔时间",
    timesUnit = "次", secondUnit = "秒", notifActions = "通知栏快捷操作",
    sensitivityTitle = "进出站灵敏度", sensitivityOverride = "是否覆写线路设定（灵敏度）",
    arrivalSensitivity = "进站灵敏度", overlapSensitivity = "重合灵敏度", departureSensitivity = "出站灵敏度",
    rulesTitle = "覆写 audiolist.csv", rulesOverride = "是否覆写 audiolist.csv",
    ruleStart = "起点播报", ruleEnd = "终点播报", ruleEndForecast = "终点预报",
    ruleArrival = "进站播报", ruleDeparture = "出站播报",
    ruleArrivalExternal = "进站外音", ruleDepartureExternal = "出站外音",
    rulesHint = "勾选=播报，不勾选=不播。关闭上面的开关时，完全按 audiolist.csv 的“是否播报”。",
    experimental = "实验性功能",
    separateChannels = "是否分离声道",
    separateChannelsHint = "勾选后：进站外音、出站外音仅左声道；起点播报、终点播报、终点预报、进站播报、出站播报仅右声道。",
    doubleTap = "双击覆写站点位置",
    doubleTapHint = "勾选后，在主页面双击“当前站”，会用手机当前位置覆写线路 CSV 里该站的经纬度。",
    routeTitle = "选择线路", routeSearchLabel = "线路（可输入搜索）", routePlaceholder = "输入线路号",
    noRoutes = "暂无线路数据，请先导入或新增", noMatch = "无匹配线路", currentMark = "（当前）",
    newRoute = "新增线路", editRoute = "编辑线路",
    dataTitle = "数据导入和导出", importAll = "一键导入（整合 ZIP）", importRoutes = "导入线路 ZIP",
    importAudio = "导入音频 ZIP", importRules = "导入 audiolist.csv", exportAll = "一键导出所有数据（ZIP）",
    dataHint = "导入：同名覆盖，其他文件保留；CSV 自动转 UTF-8。\n导出：把 bus/、mp3/、system/ 一起打包，保存到你选择的位置。",
    newRouteTitle = "新建线路", routeNameLabel = "线路名称", addStopTitle = "新增站点", editStopTitle = "编辑站点",
    deleteAction = "删除", insertAbove = "在上方插入", untitledStop = "未命名站点", noVoice = "无报站语音",
    limitShort = "限速",
    fVoice = "报站语音 *", fName = "站名 *", fLon = "经度（度分，如 11930.6582）",
    fLat = "纬度（如 3209.3318）", fAngle = "角度", fAdIn = "进站广告", fAdOut = "出站广告",
    fTipIn = "进站提示", fTipOut = "出站提示", fExtIn = "进站扩展", fExtOut = "出站扩展",
    fLimit = "限速", fApproach = "站前里程",
    stopType = "是否大站", typeStart = "起点站", typeMajor = "大站", typeMinor = "小站", typeEnd = "终点站",
    externalAudio = "外音开否", requiredError = "报站语音和站名不能为空",
    unsavedTitle = "尚未保存", unsavedText = "当前线路有未保存的修改，确定要放弃修改并返回吗？",
    discard = "确定", stay = "取消",
    overwriteTitle = "覆盖下行？", overwriteText = "下行已有数据，复制上行站点会覆盖下行所有已有数据，是否继续？",
    overwriteContinue = "继续",
    savedToast = "已保存", nameEmpty = "线路名称不能为空", nameInvalid = "线路名称不能包含 \\ / : * ? \" < > |",
    needOneStop = "请至少添加一个站点", noUpStops = "上行还没有站点",
    emptyUp = "上行还没有站点，点右上角 + 添加", emptyDown = "下行还没有站点，点右上角 + 添加",
    copyUpToDown = "复制上行站点至下行", nameExists = "名称已存在或重命名失败",
    noRouteSelected = "请先选择线路并导入数据", noStops = "暂无站点可切换",
    atStart = "已经是起点站", atEnd = "已经是终点站",
    nothingToRepeat = "暂无可重复的音频", noAudioPlaying = "当前没有正在播报的内容", stopped = "已终止本次播报",
    importingRoutes = "正在导入线路包…", importingAudio = "正在导入音频包…",
    importingRules = "正在导入规则…", importingAll = "正在一键导入…", exporting = "正在导出全部数据…",
    noRoutesAvailable = "当前没有可选线路", availableRoutes = "可选线路",
    notZip = "这不是 ZIP 压缩档，请选择 .zip 文件", readZipFailed = "读取压缩档失败",
    readFileFailed = "读取文件失败", rulesImported = "audiolist.csv 已导入（已检测编码并统一为 UTF-8）",
    importNothing = "没有匹配到可导入的文件", exportCannotWrite = "无法写入所选位置", exportNothing = "没有可导出的数据",
    ready = "设备就绪", dataReady = "初始数据已就绪",
    cannotSwitch = "，无法切换上下行", switchedTo = "已切换为",
    noLocation = "尚未获取到定位，无法覆写", overwriteFailed = "站点位置覆写失败", overwriteSuccess = "站点位置覆写成功",
    routeDataEmpty = "线路数据为空", repeatDone = "重复完毕，尚未推进", unknownError = "未知错误",
    readDenied = "无法读取所选文件，请重新选择（部分车机请从“文件”应用而不是“最近”中打开）",
    notifChannel = "报站运行状态", notifNoRoute = "未选择线路，请先导入或新增"
)

val ZhTwStrings = AppStrings(
    language = AppLanguage.ZH_TW,
    back = "返回", save = "儲存", cancel = "取消", confirm = "確定",
    yes = "是", no = "否", delete = "刪除", continueText = "繼續",
    currentStop = "目前站", nextStop = "下一站：",
    up = "上行", down = "下行", loop = "環線", oneWay = "單向",
    announce = "報站", stopPlayback = "終止", repeat = "重複", settings = "設定",
    keypad = "數字鍵盤", currentSpeed = "目前車速", speedLimit = "限速",
    kmh = "km/h", meterUnit = "公尺",
    gpsStrong = "強", gpsMedium = "中", gpsWeak = "弱", gpsNone = "無",
    routeSuffix = " 路",
    settingsTitle = "報站器設定", languageTitle = "語言", languageLabel = "介面語言",
    themeTitle = "主題配色",
    themeSkyBlue = "天空藍", themeTeal = "青碧", themeAmber = "暖陽", themeViolet = "紫羅蘭",
    themeNightBlue = "夜空藍", themeForest = "深林綠", themeCoffee = "暖棕", themePureBlack = "純黑",
    general = "一般", keepScreenOn = "前景保持螢幕恆亮", haptic = "按鍵震動回饋",
    loudness = "響度均衡（自動提升小聲錄音）",
    announceSettings = "報站功能設定", limitTitle = "限速設定",
    limitOverride = "是否覆寫路線設定（限速）", manualLimit = "手動限速", overspeed = "是否播放超速提醒",
    overspeedRepeat = "超速提醒播放次數", overspeedInterval = "超速提醒間隔時間",
    timesUnit = "次", secondUnit = "秒", notifActions = "通知列快捷操作",
    sensitivityTitle = "進出站靈敏度", sensitivityOverride = "是否覆寫路線設定（靈敏度）",
    arrivalSensitivity = "進站靈敏度", overlapSensitivity = "重合靈敏度", departureSensitivity = "出站靈敏度",
    rulesTitle = "覆寫 audiolist.csv", rulesOverride = "是否覆寫 audiolist.csv",
    ruleStart = "起點播報", ruleEnd = "終點播報", ruleEndForecast = "終點預報",
    ruleArrival = "進站播報", ruleDeparture = "出站播報",
    ruleArrivalExternal = "進站外音", ruleDepartureExternal = "出站外音",
    rulesHint = "勾選＝播放，不勾選＝不播。關閉上方開關時，完全依 audiolist.csv 的「是否播報」。",
    experimental = "實驗性功能",
    separateChannels = "是否分離聲道",
    separateChannelsHint = "勾選後：進站外音、出站外音僅左聲道；起點播報、終點播報、終點預報、進站播報、出站播報僅右聲道。",
    doubleTap = "雙擊覆寫站點位置",
    doubleTapHint = "勾選後，在主畫面雙擊「目前站」，會以手機目前位置覆寫路線 CSV 中該站的經緯度。",
    routeTitle = "選擇路線", routeSearchLabel = "路線（可輸入搜尋）", routePlaceholder = "輸入路線名稱",
    noRoutes = "尚無路線資料，請先匯入或新增", noMatch = "沒有符合的路線", currentMark = "（目前）",
    newRoute = "新增路線", editRoute = "編輯路線",
    dataTitle = "資料匯入與匯出", importAll = "一鍵匯入（整合 ZIP）", importRoutes = "匯入路線 ZIP",
    importAudio = "匯入音訊 ZIP", importRules = "匯入 audiolist.csv", exportAll = "一鍵匯出所有資料（ZIP）",
    dataHint = "匯入：同名覆蓋，其他檔案保留；CSV 自動轉為 UTF-8。\n匯出：將 bus/、mp3/、system/ 一起打包，儲存到你選擇的位置。",
    newRouteTitle = "新增路線", routeNameLabel = "路線名稱", addStopTitle = "新增站點", editStopTitle = "編輯站點",
    deleteAction = "刪除", insertAbove = "在上方插入", untitledStop = "未命名站點", noVoice = "無報站語音",
    limitShort = "限速",
    fVoice = "報站語音 *", fName = "站名 *", fLon = "經度（度分，如 11930.6582）",
    fLat = "緯度（如 3209.3318）", fAngle = "角度", fAdIn = "進站廣告", fAdOut = "出站廣告",
    fTipIn = "進站提示", fTipOut = "出站提示", fExtIn = "進站擴充", fExtOut = "出站擴充",
    fLimit = "限速", fApproach = "站前里程",
    stopType = "是否大站", typeStart = "起點站", typeMajor = "大站", typeMinor = "小站", typeEnd = "終點站",
    externalAudio = "外音開否", requiredError = "報站語音與站名不能為空",
    unsavedTitle = "尚未儲存", unsavedText = "目前路線有未儲存的修改，確定要放棄修改並返回嗎？",
    discard = "確定", stay = "取消",
    overwriteTitle = "覆蓋下行？", overwriteText = "下行已有資料，複製上行站點會覆蓋下行所有資料，是否繼續？",
    overwriteContinue = "繼續",
    savedToast = "已儲存", nameEmpty = "路線名稱不能為空", nameInvalid = "路線名稱不能包含 \\ / : * ? \" < > |",
    needOneStop = "請至少新增一個站點", noUpStops = "上行尚無站點",
    emptyUp = "上行尚無站點，點右上角 + 新增", emptyDown = "下行尚無站點，點右上角 + 新增",
    copyUpToDown = "複製上行站點至下行", nameExists = "名稱已存在或重新命名失敗",
    noRouteSelected = "請先選擇路線並匯入資料", noStops = "尚無站點可切換",
    atStart = "已經是起點站", atEnd = "已經是終點站",
    nothingToRepeat = "尚無可重複的音訊", noAudioPlaying = "目前沒有正在播放的內容", stopped = "已終止本次播報",
    importingRoutes = "正在匯入路線包…", importingAudio = "正在匯入音訊包…",
    importingRules = "正在匯入規則…", importingAll = "正在一鍵匯入…", exporting = "正在匯出所有資料…",
    noRoutesAvailable = "目前沒有可選路線", availableRoutes = "可選路線",
    notZip = "這不是 ZIP 壓縮檔，請選擇 .zip 檔案", readZipFailed = "讀取壓縮檔失敗",
    readFileFailed = "讀取檔案失敗", rulesImported = "audiolist.csv 已匯入（已偵測編碼並統一為 UTF-8）",
    importNothing = "沒有符合的可匯入檔案", exportCannotWrite = "無法寫入所選位置", exportNothing = "沒有可匯出的資料",
    ready = "裝置就緒", dataReady = "初始資料已就緒",
    cannotSwitch = "，無法切換上下行", switchedTo = "已切換為",
    noLocation = "尚未取得定位，無法覆寫", overwriteFailed = "站點位置覆寫失敗", overwriteSuccess = "站點位置覆寫成功",
    routeDataEmpty = "路線資料為空", repeatDone = "重複完畢，尚未推進", unknownError = "未知錯誤",
    readDenied = "無法讀取所選檔案，請重新選擇（部分車機請從「檔案」應用程式而非「最近」中開啟）",
    notifChannel = "報站執行狀態", notifNoRoute = "尚未選擇路線，請先匯入或新增"
)

val EnStrings = AppStrings(
    language = AppLanguage.EN,
    back = "Back", save = "Save", cancel = "Cancel", confirm = "OK",
    yes = "Yes", no = "No", delete = "Delete", continueText = "Continue",
    currentStop = "Current", nextStop = "Next",
    up = "Up", down = "Down", loop = "Loop", oneWay = "One-way",
    announce = "Announce", stopPlayback = "Stop", repeat = "Repeat", settings = "Settings",
    keypad = "Keypad", currentSpeed = "Speed", speedLimit = "Limit",
    kmh = "km/h", meterUnit = "m",
    gpsStrong = "Strong", gpsMedium = "Fair", gpsWeak = "Weak", gpsNone = "None",
    routeSuffix = "",
    settingsTitle = "Settings", languageTitle = "Language", languageLabel = "Language",
    themeTitle = "Theme",
    themeSkyBlue = "Sky Blue", themeTeal = "Teal", themeAmber = "Amber", themeViolet = "Violet",
    themeNightBlue = "Night Blue", themeForest = "Forest", themeCoffee = "Coffee", themePureBlack = "Pure Black",
    general = "General", keepScreenOn = "Keep screen on", haptic = "Haptic feedback",
    loudness = "Loudness boost",
    announceSettings = "Announcement", limitTitle = "Speed limit",
    limitOverride = "Override route limit", manualLimit = "Manual limit", overspeed = "Overspeed alert",
    overspeedRepeat = "Overspeed repeat count", overspeedInterval = "Overspeed interval",
    timesUnit = "times", secondUnit = "s", notifActions = "Notification quick actions",
    sensitivityTitle = "Sensitivity", sensitivityOverride = "Override route distance",
    arrivalSensitivity = "Arrival", overlapSensitivity = "Overlap", departureSensitivity = "Departure",
    rulesTitle = "Override audiolist.csv", rulesOverride = "Override audiolist.csv",
    ruleStart = "Start", ruleEnd = "End", ruleEndForecast = "End forecast",
    ruleArrival = "Arrival", ruleDeparture = "Departure",
    ruleArrivalExternal = "Arrival ext.", ruleDepartureExternal = "Departure ext.",
    rulesHint = "Checked = play, unchecked = mute. When disabled, audiolist.csv is used as-is.",
    experimental = "Experimental",
    separateChannels = "Split channels",
    separateChannelsHint = "External audio plays on the left channel only; main announcements play on the right channel only.",
    doubleTap = "Double-tap to set stop location",
    doubleTapHint = "When enabled, double-tap the current stop to write the phone's location into the route CSV.",
    routeTitle = "Route", routeSearchLabel = "Search route", routePlaceholder = "Route name",
    noRoutes = "No route data. Import or create one first.", noMatch = "No match", currentMark = " (current)",
    newRoute = "New route", editRoute = "Edit route",
    dataTitle = "Import & export", importAll = "Import all (ZIP)", importRoutes = "Import routes ZIP",
    importAudio = "Import audio ZIP", importRules = "Import audiolist.csv", exportAll = "Export all (ZIP)",
    dataHint = "Import: same-name files are replaced. CSV is converted to UTF-8.\nExport: packs bus/, mp3/ and system/ to a location you choose.",
    newRouteTitle = "New route", routeNameLabel = "Route name", addStopTitle = "Add stop", editStopTitle = "Edit stop",
    deleteAction = "Delete", insertAbove = "Insert above", untitledStop = "Untitled", noVoice = "No voice",
    limitShort = "Limit",
    fVoice = "Voice *", fName = "Stop name *", fLon = "Longitude (DDMM.MMMM, e.g. 11930.6582)",
    fLat = "Latitude (e.g. 3209.3318)", fAngle = "Angle", fAdIn = "Arrival ad", fAdOut = "Departure ad",
    fTipIn = "Arrival tip", fTipOut = "Departure tip", fExtIn = "Arrival ext.", fExtOut = "Departure ext.",
    fLimit = "Speed limit", fApproach = "Approach distance",
    stopType = "Stop type", typeStart = "Start", typeMajor = "Major", typeMinor = "Minor", typeEnd = "End",
    externalAudio = "External audio", requiredError = "Voice and stop name are required",
    unsavedTitle = "Unsaved changes", unsavedText = "Discard changes and go back?",
    discard = "Discard", stay = "Cancel",
    overwriteTitle = "Overwrite down direction?", overwriteText = "Down direction already has stops. Copying will overwrite all of them. Continue?",
    overwriteContinue = "Continue",
    savedToast = "Saved", nameEmpty = "Route name cannot be empty", nameInvalid = "Route name contains invalid characters",
    needOneStop = "Add at least one stop", noUpStops = "Up direction has no stops",
    emptyUp = "No stops in the up direction. Tap + to add.", emptyDown = "No stops in the down direction. Tap + to add.",
    copyUpToDown = "Copy up to down", nameExists = "Name exists or rename failed",
    noRouteSelected = "Select a route and import data first", noStops = "No stops to switch",
    atStart = "Already at the first stop", atEnd = "Already at the last stop",
    nothingToRepeat = "Nothing to repeat", noAudioPlaying = "Nothing is playing", stopped = "Playback stopped",
    importingRoutes = "Importing routes…", importingAudio = "Importing audio…",
    importingRules = "Importing rules…", importingAll = "Importing…", exporting = "Exporting…",
    noRoutesAvailable = "No routes available", availableRoutes = "Routes",
    notZip = "Not a ZIP file", readZipFailed = "Cannot read ZIP",
    readFileFailed = "Cannot read file", rulesImported = "audiolist.csv imported (UTF-8)",
    importNothing = "No matching files", exportCannotWrite = "Cannot write to the selected location", exportNothing = "Nothing to export",
    ready = "Ready", dataReady = "Initial data ready",
    cannotSwitch = ": cannot switch direction", switchedTo = "Switched to ",
    noLocation = "No location fix yet", overwriteFailed = "Failed to update stop location", overwriteSuccess = "Stop location updated",
    routeDataEmpty = "Route data is empty", repeatDone = "Replay finished", unknownError = "Unknown error",
    readDenied = "Cannot read the selected file. Please pick it again (on some head units, open it from Files rather than Recent).",
    notifChannel = "Announcement service", notifNoRoute = "No route selected"
)

fun stringsFor(languageId: String?): AppStrings = when (AppLanguage.fromId(languageId)) {
    AppLanguage.ZH_CN -> ZhCnStrings
    AppLanguage.ZH_TW -> ZhTwStrings
    AppLanguage.EN -> EnStrings
}

val LocalStrings = staticCompositionLocalOf { ZhCnStrings }
