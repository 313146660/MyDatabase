package com.example.mydb.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.mydb.BuildConfig
import com.example.mydb.data.*
import com.example.mydb.ui.*
import com.example.mydb.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun stampOrNever(ts: Long): String =
    if (ts <= 0) "还没有" else SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINA).format(Date(ts))

private fun bgUnrestricted(ctx: android.content.Context): Boolean {
    val pm = ctx.getSystemService(PowerManager::class.java) ?: return true
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

private fun soundLabel(ctx: android.content.Context, c: ReminderCfg): String = when {
    c.sound == ReminderCfg.SILENT -> "静音"
    c.sound.isBlank() -> "系统默认铃声"
    else -> runCatching {
        RingtoneManager.getRingtone(ctx, Uri.parse(c.sound))?.getTitle(ctx)
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "自定义铃声"
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val rev = vm.rev
    val libs = vm.state.libraries
    val total = libs.sumOf { it.entries.size }
    var showPwDlg by remember { mutableStateOf(false) }
    if (showPwDlg) {
        PasswordDialog(
            onDismiss = { showPwDlg = false },
            onConfirm = { vm.applyLockPassword(it); showPwDlg = false; vm.toast("解锁密码已保存") }
        )
    }
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        TopBar("我的")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Primary).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Text("M", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("MyDB", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${libs.size} 个数据库 · $total 条记录", color = Color.White.copy(alpha = 0.8f), fontSize = 11.5.sp)
                }
                Text("v${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Spacer(Modifier.height(16.dp))

            Text("数据", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            MCard(Modifier.fillMaxWidth()) {
                InfoRow("upload", "备份与恢复", "本地 JSON、WebDAV、自动备份", Primary) { vm.go(Scr.Backup) }
                DividerLine()
                InfoRow("plus", "新建数据库", "自定义图标、颜色和字段", Teal) { vm.go(Scr.LibForm(null)) }
            }

            Spacer(Modifier.height(16.dp))
            Text("数据库", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            MCard(Modifier.fillMaxWidth()) {
                libs.forEachIndexed { i, l ->
                    InfoRow(l.icon, l.name, "${l.entries.size} 条记录 · ${l.fields.size} 个字段", hexColor(l.color)) {
                        vm.go(Scr.Library(l.id))
                    }
                    if (i != libs.lastIndex) DividerLine()
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("安全", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            MCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("指纹解锁", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                        Text(
                            if (android.os.Build.VERSION.SDK_INT >= 28) "打开应用或离开超过 30 秒后需验证指纹"
                            else "需要 Android 9 及以上系统",
                            fontSize = 10.5.sp, color = Muted2
                        )
                    }
                    Switch(
                        checked = vm.lockEnabled,
                        enabled = android.os.Build.VERSION.SDK_INT >= 28,
                        onCheckedChange = { vm.setFingerprintLock(it) }
                    )
                }
                DividerLine()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("解锁密码", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                        Text(
                            if (vm.lockPassword.isEmpty()) "未设置。指纹失效时可用密码进入"
                            else "已设置，可替代指纹解锁",
                            fontSize = 10.5.sp, color = Muted2
                        )
                    }
                    TextButton(onClick = { showPwDlg = true }) {
                        Text(if (vm.lockPassword.isEmpty()) "设置" else "修改", fontSize = 12.sp)
                    }
                    if (vm.lockPassword.isNotEmpty()) {
                        TextButton(onClick = { vm.applyLockPassword(null); vm.toast("已清除解锁密码") }) {
                            Text("清除", fontSize = 12.sp, color = Danger)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("提醒", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            ReminderSettingsCard()

            Spacer(Modifier.height(16.dp))
            Text("外观", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            MCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("深色模式", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                        Text("夜里看更舒服，立即生效", fontSize = 10.5.sp, color = Muted2)
                    }
                    Switch(checked = vm.dark, onCheckedChange = { vm.setNight(it) })
                }
                DividerLine()
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text("列表默认字号", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Text("没有单独设置的库都用这个字号（10–26）", fontSize = 10.5.sp, color = Muted2)
                    Spacer(Modifier.height(8.dp))
                    var gText by remember(vm.globalFont) { mutableStateOf(vm.globalFont.toString()) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            AppTextField(
                                gText,
                                { t -> if (t.all { it.isDigit() } && t.length <= 2) gText = t },
                                placeholder = "14",
                                keyboardType = KeyboardType.Number
                            )
                            Row(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)) {
                                Text("sp", fontSize = 11.sp, color = Muted2)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.width(90.dp)) {
                            PillButton("应用", onClick = {
                                val v = gText.toIntOrNull() ?: return@PillButton
                                if (v in 10..26) vm.setDefaultFont(v) else vm.toast("请输入 10–26")
                            })
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("关于", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            MCard(Modifier.fillMaxWidth()) {
                InfoRow("info", "关于 MyDB", "本地优先的个人数据库 · 版本 ${BuildConfig.VERSION_NAME}", Muted) { vm.go(Scr.About) }
                DividerLine()
                InfoRow("database", "数据说明", "数据保存在本机；换机前请先导出备份", Muted)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "MyDB ${BuildConfig.VERSION_NAME} · 本地存储 · 不收集任何数据",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.5.sp, color = Muted2
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

/* ---------------- 提醒设置 ---------------- */

@Composable
private fun ReminderSettingsCard() {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(Reminders.cfg(ctx)) }
    var canNotify by remember { mutableStateOf(Reminders.canNotify(ctx)) }
    var canExact by remember { mutableStateOf(Reminders.canExact(ctx)) }
    var bgOk by remember { mutableStateOf(bgUnrestricted(ctx)) }
    var tested by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        canNotify = Reminders.canNotify(ctx)
        canExact = Reminders.canExact(ctx)
        bgOk = bgUnrestricted(ctx)
        onPauseOrDispose { }
    }
    val soundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri = res.data?.let {
                IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            }
            cfg = cfg.copy(sound = uri?.toString() ?: ReminderCfg.SILENT)
            Reminders.setCfg(ctx, cfg)
        }
    }

    MCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("提醒铃声", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Text(soundLabel(ctx, cfg), fontSize = 10.5.sp, color = Muted2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = {
                val cur = when {
                    cfg.sound == ReminderCfg.SILENT -> null
                    cfg.sound.isBlank() -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    else -> Uri.parse(cfg.sound)
                }
                soundLauncher.launch(
                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, cur)
                    }
                )
            }) { Text("更换", fontSize = 12.sp) }
        }
        DividerLine()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("震动", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Text(if (cfg.vibrate) "提醒时同时震动" else "只响铃，不震动", fontSize = 10.5.sp, color = Muted2)
            }
            Switch(
                checked = cfg.vibrate,
                onCheckedChange = { cfg = cfg.copy(vibrate = it); Reminders.setCfg(ctx, cfg) }
            )
        }
        DividerLine()
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text("声音 / 震动时长", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text("到点后持续响铃和震动，超时自动停止", fontSize = 10.5.sp, color = Muted2)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(5, 15, 30, 60).forEach { sec ->
                    val on = cfg.seconds == sec
                    Text(
                        "${sec} 秒", fontSize = 11.sp,
                        color = if (on) Color.White else Primary,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(if (on) Primary else PrimarySoft)
                            .clickable {
                                cfg = cfg.copy(seconds = sec)
                                Reminders.setCfg(ctx, cfg)
                            }
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                    )
                }
            }
        }
        DividerLine()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("通知权限", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Text(
                    if (canNotify) "已允许，提醒会弹出通知" else "未允许，提醒不会弹出",
                    fontSize = 10.5.sp, color = if (canNotify) Muted2 else Danger
                )
            }
            if (!canNotify) {
                TextButton(onClick = {
                    runCatching {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }) { Text("去开启", fontSize = 12.sp) }
            }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            DividerLine()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("精确闹钟", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                    Text(
                        if (canExact) "已允许，提醒按设定时间触发" else "未允许，提醒可能延迟几分钟",
                        fontSize = 10.5.sp, color = if (canExact) Muted2 else Danger
                    )
                }
                if (!canExact) {
                    TextButton(onClick = {
                        runCatching {
                            ctx.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(Uri.parse("package:${ctx.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }) { Text("去允许", fontSize = 12.sp) }
                }
            }
        }
        DividerLine()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("后台提醒", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Text(
                    if (bgOk) "已允许后台运行，锁屏也能提醒" else "未允许，锁屏或切到后台后可能不提醒",
                    fontSize = 10.5.sp, color = if (bgOk) Muted2 else Danger
                )
                if (!bgOk) {
                    Text("小米 / 华为等系统还需在「自启动」里允许", fontSize = 9.5.sp, color = Muted2)
                }
            }
            if (!bgOk) {
                TextButton(onClick = {
                    val ok = runCatching {
                        ctx.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(Uri.parse("package:${ctx.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.isSuccess
                    if (!ok) {
                        runCatching {
                            ctx.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }) { Text("去允许", fontSize = 12.sp) }
            }
        }
        DividerLine()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("测试提醒", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                Text(
                    if (tested) "已安排，5 秒后应弹出通知" else "5 秒后发一条通知，用来确认权限、铃声和震动",
                    fontSize = 10.5.sp, color = if (tested) Primary else Muted2
                )
            }
            TextButton(onClick = {
                Reminders.test(ctx)
                tested = true
            }) { Text("开始测试", fontSize = 12.sp) }
        }
    }
}

/* ---------------- 备份 ---------------- */

@Composable
fun BackupScreen(vm: AppViewModel) {
    val rev = vm.rev
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busyLocal by remember { mutableStateOf(false) }
    // 备份体积 = 整库序列化，放到 IO 线程计算，避免组合阶段阻塞主线程
    var sizeText by remember { mutableStateOf("—") }
    LaunchedEffect(rev) {
        sizeText = withContext(Dispatchers.IO) { vm.backupSizeText() }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) { vm.exportText() }
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    }
                    vm.markLocalExport()
                    vm.toast("已导出备份")
                } catch (e: Exception) {
                    vm.toast("导出失败：" + (e.message ?: "无法写入"))
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    }
                    if (text != null && vm.importText(text)) {
                        vm.toast("恢复成功")
                    } else {
                        vm.toast("文件格式不正确，无法恢复")
                    }
                } catch (e: Exception) {
                    vm.toast("读取失败：" + (e.message ?: "无法读取"))
                }
            }
        }
    }

    var url by remember(vm.bk.url) { mutableStateOf(vm.bk.url) }
    var port by remember(vm.bk.port) { mutableStateOf(vm.bk.port) }
    var user by remember(vm.bk.user) { mutableStateOf(vm.bk.user) }
    var pass by remember(vm.bk.pass) { mutableStateOf(vm.bk.pass) }
    var path by remember(vm.bk.path) { mutableStateOf(vm.bk.path) }
    var auto by remember(vm.bk.auto) { mutableStateOf(vm.bk.auto) }

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        TopBar("备份与恢复", "包含所有库、字段、记录和提醒规则", onBack = { vm.back() })
        Column(Modifier.padding(horizontal = 16.dp)) {
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("本地文件", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Text("$sizeText · 上次导出：${stampOrNever(vm.bk.lastLocalAt)}", fontSize = 10.5.sp, color = Muted2)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            PillButton("导出备份…", onClick = { exportLauncher.launch(vm.exportName()) })
                        }
                        Box(Modifier.weight(1f)) {
                            PillButton("从文件恢复", onClick = { importLauncher.launch(arrayOf("*/*")) }, color = Muted, filled = false)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("导出会打开系统保存窗口，可以自选目录和文件名；恢复会替换当前全部数据。", fontSize = 10.5.sp, color = Muted2)
                }
            }

            if (vm.snapshotAt != null) {
                Spacer(Modifier.height(10.dp))
                MCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("恢复前快照", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text(
                            "上次恢复前的数据已暂存：${stampOrNever(vm.snapshotAt ?: 0L)}",
                            fontSize = 10.5.sp, color = Muted2
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) {
                                PillButton("撤销上次恢复", onClick = {
                                    scope.launch {
                                        if (vm.undoImport()) vm.toast("已恢复到导入前的数据")
                                        else vm.toast("快照不可用")
                                    }
                                }, color = Amber)
                            }
                            Box(Modifier.weight(1f)) {
                                PillButton("丢弃快照", onClick = {
                                    vm.clearSnapshot()
                                    vm.toast("已丢弃快照")
                                }, color = Muted, filled = false)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("WebDAV", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Text("备份文件保存为 目录/mydb-backup.json（旧版 memento-backup.json 仍可恢复）", fontSize = 10.5.sp, color = Muted2)
                    Spacer(Modifier.height(6.dp))
                    FormRow("WebDAV 地址", "填到账号根目录；不写协议时默认 https") {
                        AppTextField(url, { url = it }, placeholder = "https://dav.example.com/remote.php/dav/files/me")
                    }
                    FormRow("端口", "自建 NAS 才需要，填了会覆盖地址里的端口") {
                        AppTextField(port, { port = it }, placeholder = "默认端口（443 / 80）", keyboardType = KeyboardType.Number)
                    }
                    FormRow("用户名") {
                        AppTextField(user, { user = it }, placeholder = "留空表示不需要登录")
                    }
                    FormRow("密码 / 应用密码") {
                        AppTextField(pass, { pass = it }, placeholder = "建议用应用专用密码")
                    }
                    FormRow("远端目录") {
                        AppTextField(path, { path = it }, placeholder = "MyDB")
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("自动备份", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                            Text("每次修改后约 6 秒上传一次", fontSize = 10.5.sp, color = Muted2)
                        }
                        Switch(checked = auto, onCheckedChange = { auto = it })
                    }
                    Spacer(Modifier.height(10.dp))
                    PillButton("保存 WebDAV 设置", onClick = {
                        vm.saveBkConfig(url, port, user, pass, path, auto)
                        vm.toast("WebDAV 设置已保存")
                    }, color = Primary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            PillButton(if (vm.busy) "上传中…" else "立即上传", onClick = {
                                vm.saveBkConfig(url, port, user, pass, path, auto)
                                vm.uploadNow()
                            }, color = Teal)
                        }
                        Box(Modifier.weight(1f)) {
                            PillButton("从云端恢复", onClick = {
                                vm.saveBkConfig(url, port, user, pass, path, auto)
                                vm.restoreNow()
                            }, color = Muted, filled = false)
                        }
                    }
                    if (vm.cloudStatus.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(vm.cloudStatus, fontSize = 11.sp, color = if (vm.cloudStatus.contains("失败") || vm.cloudStatus.contains("不正确")) Danger else Teal)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("上次成功上传：${stampOrNever(vm.bk.lastCloudAt)}", fontSize = 10.sp, color = Muted2)
                }
            }

            Spacer(Modifier.height(14.dp))
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("换手机怎么迁移", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(Modifier.height(4.dp))
                    Text("1. 在旧手机点「导出备份…」，把 JSON 存到文件管理器或网盘。\n2. 在新手机安装 MyDB，进入「备份与恢复 → 从文件恢复」。\n3. 选刚才的 JSON，数据会完整恢复。", fontSize = 11.5.sp, color = Muted)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

/* ---------------- CSV ---------------- */

@Composable
fun CsvScreen(vm: AppViewModel, libId: String) {
    val lib = vm.lib(libId) ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("append") }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) { vm.csvExportText(lib.id) } ?: return@launch
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    }
                    vm.toast("已导出 ${lib.entries.size} 条记录")
                } catch (e: Exception) {
                    vm.toast("导出失败：" + (e.message ?: ""))
                }
            }
        }
    }
    var pendingText by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<Csv.ImportPreview?>(null) }
    var importing by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    }
                    if (text == null) {
                        vm.toast("读取失败")
                    } else {
                        val p = withContext(Dispatchers.IO) { Csv.preview(lib, text) }
                        pendingText = text
                        preview = p
                    }
                } catch (e: Exception) {
                    vm.toast("读取失败：" + (e.message ?: ""))
                }
            }
        }
    }
    val fields = Csv.exportFields(lib)

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        TopBar("CSV 导入 / 导出", lib.name + " · ${lib.entries.size} 条记录", onBack = { vm.back() })
        Column(Modifier.padding(horizontal = 16.dp)) {
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("导出 CSV", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Text("按当前字段导出，Excel 可直接打开。自动编号和公式列不会导出。", fontSize = 10.5.sp, color = Muted2)
                    Spacer(Modifier.height(10.dp))
                    Text(fields.joinToString("、") { it.name }, fontSize = 11.sp, color = Muted)
                    Spacer(Modifier.height(12.dp))
                    PillButton("导出 CSV", onClick = { exportLauncher.launch(vm.csvExportName(lib.id)) })
                }
            }
            Spacer(Modifier.height(14.dp))
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("导入 CSV", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Text("按表头自动匹配字段；表头里没有的列会自动新增为字段。", fontSize = 10.5.sp, color = Muted2)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "append" to "追加到现有记录",
                        "skip" to "追加，跳过重复行（姓名 + 电话 + 时间）",
                        "replace" to "清空后替换"
                    ).forEach { (v, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { mode = v }.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = mode == v, onClick = { mode = v })
                            Text(label, fontSize = 12.5.sp, color = TextMain)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    PillButton(if (importing) "导入中…" else "选择 CSV 文件", onClick = {
                        if (!importing) importLauncher.launch(arrayOf("*/*"))
                    }, color = Teal)
                    Spacer(Modifier.height(8.dp))
                    Text("建议使用 UTF-8 编码的 CSV；导入不会扣减镜片库存。", fontSize = 10.5.sp, color = Muted2)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    val p = preview
    val raw = pendingText
    if (p != null && raw != null) {
        AlertDialog(
            onDismissRequest = { if (!importing) { preview = null; pendingText = null } },
            title = { Text("确认导入") },
            text = {
                Column {
                    Text("文件共 ${p.rows} 行数据", fontSize = 13.sp, color = TextMain)
                    Spacer(Modifier.height(6.dp))
                    Text("匹配到 ${p.matched.size} 个已有字段", fontSize = 12.5.sp, color = Muted)
                    if (p.newFields.isNotEmpty()) {
                        Text("将新增 ${p.newFields.size} 个字段：${p.newFields.joinToString("、")}", fontSize = 12.sp, color = Amber)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (mode) {
                            "replace" -> "模式：清空后替换（会先存快照）"
                            "skip" -> "模式：追加，跳过重复行"
                            else -> "模式：追加到现有记录"
                        },
                        fontSize = 11.5.sp, color = Muted2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (importing) return@TextButton
                    importing = true
                    scope.launch {
                        // 数据量不大（一千多条约 70ms），直接在主线程完成，避免与列表渲染并发改数据
                        val r = vm.csvImport(lib.id, raw, mode)
                        importing = false
                        preview = null
                        pendingText = null
                        if (r == null) vm.toast("导入失败")
                        else vm.toast(
                            if (r.skipped > 0) "新增 ${r.added} 条，跳过 ${r.skipped} 条重复"
                            else "已导入 ${r.added} 条记录"
                        )
                    }
                }) { Text(if (importing) "导入中…" else "开始导入") }
            },
            dismissButton = {
                TextButton(onClick = { preview = null; pendingText = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置解锁密码") },
        text = {
            Column {
                OutlinedTextField(
                    value = p1,
                    onValueChange = { p1 = it; err = null },
                    label = { Text("新密码（至少 4 位）") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = p2,
                    onValueChange = { p2 = it; err = null },
                    label = { Text("再次输入") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (err != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(err!!, color = Danger, fontSize = 11.sp)
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text("忘记密码将无法进入应用，请务必牢记", fontSize = 10.sp, color = Muted2)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (p1.length < 4) err = "密码至少 4 位"
                else if (p1 != p2) err = "两次输入不一致"
                else onConfirm(p1)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
