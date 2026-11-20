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
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mydb.MainActivity
import com.example.mydb.R

/** 「提醒」字段的系统通知：到点弹通知，开机后自动重排 */
object Reminders {
    const val CHANNEL_ID = "todo_reminder"
    private const val ACTION = "com.example.mydb.ACTION_REMINDER"
    private const val PREFS = "mydb_reminders"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "待办提醒", NotificationManager.IMPORTANCE_HIGH)
        )
    }

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

        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        next.forEach { (uri, v) ->
            val pi = pendingOf(ctx, uri, v.second, v.third)
            try {
                if (exact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, v.first, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, v.first, pi)
                }
            } catch (_: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, v.first, pi)
            }
        }
        prefs.edit().putStringSet("scheduled", next.keys).apply()
    }

    private fun pendingOf(
        ctx: Context, uri: String, title: String = "", body: String = ""
    ): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION)
            .setData(Uri.parse(uri))
            .putExtra("title", title)
            .putExtra("body", body)
        return PendingIntent.getBroadcast(
            ctx, uri.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        Reminders.ensureChannel(ctx)
        val title = intent.getStringExtra("title") ?: "待办提醒"
        val body = intent.getStringExtra("body").orEmpty()
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body.ifBlank { "提醒时间到了" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        ctx.getSystemService(NotificationManager::class.java)
            ?.notify(intent.dataString?.hashCode() ?: 0, n)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Reminders.rescheduleAll(ctx)
    }
}
