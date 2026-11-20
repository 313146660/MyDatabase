package com.example.mydb.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mydb.MainActivity
import com.example.mydb.R

/** 提醒通知的个性化设置：铃声、震动、响铃时长 */
data class ReminderCfg(
    /** "" = 系统默认，SILENT = 静音，其它为铃声 Uri */
    val sound: String = "",
    val vibrate: Boolean = true,
    val seconds: Int = 15
) {
    companion object {
        const val SILENT = "silent"
    }
}

/** 「提醒」字段的系统通知：到点弹通知，开机后自动重排 */
object Reminders {
    private const val ACTION = "com.example.mydb.ACTION_REMINDER"
    private const val PREFS = "mydb_reminders"
    private const val CFG = "mydb_reminder_cfg"
    private const val CHANNEL_PREFIX = "todo_reminder_"

    /* ---------------- 设置 ---------------- */

    fun cfg(ctx: Context): ReminderCfg {
        val p = ctx.getSharedPreferences(CFG, Context.MODE_PRIVATE)
        return ReminderCfg(
            sound = p.getString("sound", "") ?: "",
            vibrate = p.getBoolean("vibrate", true),
            seconds = p.getInt("seconds", 15)
        )
    }

    fun setCfg(ctx: Context, c: ReminderCfg) {
        ctx.getSharedPreferences(CFG, Context.MODE_PRIVATE).edit()
            .putString("sound", c.sound)
            .putBoolean("vibrate", c.vibrate)
            .putInt("seconds", c.seconds)
            .apply()
        ensureChannel(ctx)
    }

    private fun soundUri(c: ReminderCfg): Uri? = when {
        c.sound == ReminderCfg.SILENT -> null
        c.sound.isBlank() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        else -> Uri.parse(c.sound)
    }

    /** 通道参数变了就得换通道 id（系统创建后不允许改声音/震动） */
    private fun channelKey(sound: Uri?, vibrate: Boolean, sec: Int): String {
        val sig = "${sound ?: "none"}|$vibrate|$sec"
        return CHANNEL_PREFIX + Integer.toHexString(sig.hashCode())
    }

    fun channelId(ctx: Context): String {
        val c = cfg(ctx)
        return channelKey(soundUri(c), c.vibrate, c.seconds.coerceIn(3, 120))
    }

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val c = cfg(ctx)
        val sound = soundUri(c)
        val sec = c.seconds.coerceIn(3, 120)
        val id = channelKey(sound, c.vibrate, sec)
        if (nm.getNotificationChannel(id) != null) return
        // 清掉旧参数的通道，避免系统设置里越积越多
        nm.notificationChannels.filter { it.id.startsWith(CHANNEL_PREFIX) }.forEach { nm.deleteNotificationChannel(it.id) }

        val ch = NotificationChannel(id, "待办提醒", NotificationManager.IMPORTANCE_HIGH)
        ch.enableVibration(c.vibrate)
        if (c.vibrate) {
            val pattern = ArrayList<Long>().apply {
                add(0L)
                var t = 0L
                while (t < sec * 1000L) { add(600L); add(400L); t += 1000L }
            }
            ch.vibrationPattern = pattern.toLongArray()
        }
        if (sound != null) {
            ch.setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        nm.createNotificationChannel(ch)
    }

    /* ---------------- 权限 ---------------- */

    fun canNotify(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return false
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return true
        val ch = nm.getNotificationChannel(channelId(ctx))
        return !(ch != null && ch.importance == NotificationManager.IMPORTANCE_NONE)
    }

    fun canExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    /** 5 秒后发一条测试通知，用来验证权限、通道、铃声与震动 */
    fun test(ctx: Context) {
        ensureChannel(ctx)
        val at = System.currentTimeMillis() + 5_000L
        arm(ctx, at, "mydb://reminder/test/$at", "测试提醒", "提醒功能正常，到点后就会这样通知你")
    }

    /**
     * 登记闹钟。有精确闹钟权限时用 setAlarmClock：这是系统优先级最高的闹钟，
     * 锁屏 / 待机 / 后台限制下也尽量准时唤醒；没有权限时退化为允许待机触发的闹钟。
     */
    private fun arm(ctx: Context, at: Long, uri: String, title: String, body: String) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingOf(ctx, uri, title, body, at)
        if (canExact(ctx)) {
            try {
                val show = PendingIntent.getActivity(
                    ctx, 1, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
                return
            } catch (_: SecurityException) {
            }
        }
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
        }
    }

    /* ---------------- 排程 ---------------- */

    /** 从存储读取全部数据后重排（开机、冷启动用） */
    fun rescheduleAll(ctx: Context) {
        reschedule(ctx, Store(ctx).load().first)
    }

    /** 按当前数据全量重排：取消已删除/已过期/已修改的旧闹钟，登记未来的提醒 */
    fun reschedule(ctx: Context, st: MState) {
        ensureChannel(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()

        // uri -> (触发时间, 标题, 内容)
        val next = LinkedHashMap<String, Triple<Long, String, String>>()
        st.libraries.forEach { lib ->
            lib.entries.forEach { e ->
                lib.fields.filter { it.type == FieldType.REMINDER }.forEach { f ->
                    val at = parseStamp(e.values[f.id]) ?: return@forEach
                    if (at <= now) return@forEach
                    val uri = "mydb://reminder/${lib.id}/${e.id}/${f.id}"
                    next[uri] = Triple(at, "${lib.name} · ${titleOf(lib, e)}", f.name)
                }
            }
        }

        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet("scheduled", emptySet()).orEmpty().forEach { uri ->
            if (!next.containsKey(uri)) am.cancel(pendingOf(ctx, uri))
        }

        next.forEach { (uri, v) ->
            arm(ctx, v.first, uri, v.second, v.third)
        }
        prefs.edit().putStringSet("scheduled", next.keys).apply()
    }

    private fun pendingOf(
        ctx: Context, uri: String, title: String = "", body: String = "", at: Long = 0L
    ): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION)
            .setData(Uri.parse(uri))
            .putExtra("title", title)
            .putExtra("body", body)
            .putExtra("at", at)
        return PendingIntent.getBroadcast(
            ctx, uri.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (!Reminders.canNotify(ctx)) return
        Reminders.ensureChannel(ctx)
        val c = Reminders.cfg(ctx)
        val title = intent.getStringExtra("title") ?: "待办提醒"
        val body = intent.getStringExtra("body").orEmpty()
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, Reminders.channelId(ctx))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body.ifBlank { "提醒时间到了" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setTimeoutAfter(c.seconds.coerceIn(3, 120) * 1000L)
            .build()
        // 通知 id 带触发时间：同一条目再次提醒时会重新弹出，而不是静默替换旧通知
        val id = (intent.dataString.orEmpty() + "@" + intent.getLongExtra("at", 0L)).hashCode()
        ctx.getSystemService(NotificationManager::class.java)?.notify(id, n)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Reminders.rescheduleAll(ctx)
    }
}
