package org.rayista.subobus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.rayista.subobus.i18n.stringsFor
import org.rayista.subobus.vm.BusViewModel

/**
 * 前台服务：让 GPS 定位与自动报站在 App 退到后台后继续运行，
 * 同时把「报站」「停止」两个按钮放到通知栏，功能和界面上的按钮一致。
 *
 * 报站逻辑本身在 [SuboApp] 的单例 ViewModel 里，这里只负责：
 * 1. 用前台通知把进程“保活”，避免后台被系统回收；
 * 2. 接收通知按钮的点击，转给 ViewModel 执行；
 * 3. 每隔一段时间刷新通知里的“当前站 / 下一站”。
 */
class AnnouncerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var lastTitle = ""
    private var lastText = ""

    private val vm: BusViewModel?
        get() = (application as? SuboApp)?.busVm

    /** 定时刷新通知内容（站名会随 GPS 变化）。 */
    private val refresher = object : Runnable {
        override fun run() {
            updateNotification()
            if (running) handler.postDelayed(this, 1500L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()

        // 进程被系统重启时，界面可能还没起来，这里补一次定位注册
        vm?.startLocation()

        startForegroundSafely(buildNotification())
        running = true
        instanceRunning = true
        handler.postDelayed(refresher, 800L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ANNOUNCE -> vm?.announce()
            ACTION_STOP -> vm?.stop()
        }
        updateNotification(force = true)
        // 被系统杀掉后自动重建，保证后台继续报站
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        instanceRunning = false
        handler.removeCallbacks(refresher)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /* ---------------- 通知 ---------------- */

    private fun startForegroundSafely(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // 没有定位权限或系统不允许前台服务时，直接结束，避免崩溃
            running = false
            instanceRunning = false
            stopSelf()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            stringsFor(vm?.uiState?.language ?: "zh_cn").notifChannel,
            NotificationManager.IMPORTANCE_LOW
        )
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    /** 通知标题（应用名 + 线路）与正文（当前站 / 下一站）。 */
    private fun currentTexts(): Pair<String, String> {
        val state = vm?.uiState
        val t = stringsFor(state?.language ?: "zh_cn")
        val appName = getString(R.string.app_name)
        val route = state?.route.orEmpty()
        val title = if (route.isBlank()) {
            appName
        } else {
            val label = if (route.last().isDigit()) route + t.routeSuffix else route
            "$appName · $label"
        }
        val text = if (state == null || route.isBlank()) {
            t.notifNoRoute
        } else {
            val current = state.currentName.ifBlank { "—" }
            val next = state.nextName.ifBlank { "—" }
            "${t.currentStop}：$current    ${t.nextStop}$next"
        }
        return title to text
    }

    private fun updateNotification(force: Boolean = false) {
        val (title, text) = currentTexts()
        if (!force && title == lastTitle && text == lastText) return
        lastTitle = title
        lastText = text
        try {
            getSystemService(NotificationManager::class.java)
                ?.notify(NOTIFICATION_ID, buildNotification(title, text))
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(
        title: String = currentTexts().first,
        text: String = currentTexts().second
    ): Notification {
        val t = stringsFor(vm?.uiState?.language ?: "zh_cn")
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val announceIntent = PendingIntent.getService(
            this,
            REQUEST_ANNOUNCE,
            Intent(this, AnnouncerService::class.java).setAction(ACTION_ANNOUNCE),
            flags
        )
        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, AnnouncerService::class.java).setAction(ACTION_STOP),
            flags
        )
        val openIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bus)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, t.announce, announceIntent)
            .addAction(0, t.stopPlayback, stopIntent)
            .build()
    }

    companion object {
        @Volatile
        private var instanceRunning = false

        private const val CHANNEL_ID = "subo_announcer_running"
        private const val NOTIFICATION_ID = 1001

        private const val REQUEST_ANNOUNCE = 11
        private const val REQUEST_STOP = 12
        private const val REQUEST_OPEN = 13

        const val ACTION_ANNOUNCE = "org.rayista.subobus.action.ANNOUNCE"
        const val ACTION_STOP = "org.rayista.subobus.action.STOP"

        /** 启动后台报站（需要已授予定位权限）。 */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AnnouncerService::class.java)
                )
            } catch (_: Exception) {
                // 系统限制后台启动前台服务时忽略，界面内仍可正常使用
            }
        }

        /** 服务正在运行时，让它立刻刷新一次通知（例如刚改了快捷操作开关）。 */
        fun refresh(context: Context) {
            if (!instanceRunning) return
            try {
                context.startService(Intent(context, AnnouncerService::class.java))
            } catch (_: Exception) {
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, AnnouncerService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
