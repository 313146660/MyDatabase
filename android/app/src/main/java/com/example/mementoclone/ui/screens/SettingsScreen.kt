package com.example.mementoclone.ui.screens

import android.content.Intent
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
import com.example.mementoclone.data.*
import com.example.mementoclone.ui.*
import com.example.mementoclone.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun stampOrNever(ts: Long): String =
    if (ts <= 0) "还没有" else SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINA).format(Date(ts))

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val rev = vm.rev
    val libs = vm.state.libraries
    val total = libs.sumOf { it.entries.size }
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
                Text("v2.0", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
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
                InfoRow("info", "关于 MyDB", "本地优先的个人数据库 · 版本 2.0", Muted)
                DividerLine()
                InfoRow("database", "数据说明", "数据保存在本机；换机前请先导出备份", Muted)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "MyDB 2.0 · 本地存储 · 不收集任何数据",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 10.5.sp, color = Muted2
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

/* ---------------- 备份 ---------------- */

@Composable
fun BackupScreen(vm: AppViewModel) {
    val rev = vm.rev
    val ctx = LocalContext.current
    var busyLocal by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportText().toByteArray(Charsets.UTF_8)) }
                vm.markLocalExport()
                vm.toast("已导出备份")
            } catch (e: Exception) {
                vm.toast("导出失败：" + (e.message ?: "无法写入"))
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
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
                    Text("${vm.backupSizeText()} · 上次导出：${stampOrNever(vm.bk.lastLocalAt)}", fontSize = 10.5.sp, color = Muted2)
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
                                    if (vm.undoImport()) vm.toast("已恢复到导入前的数据")
                                    else vm.toast("快照不可用")
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
    var mode by remember { mutableStateOf("append") }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val text = vm.csvExportText(lib.id) ?: return@rememberLauncherForActivityResult
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                vm.toast("已导出 ${lib.entries.size} 条记录")
            } catch (e: Exception) {
                vm.toast("导出失败：" + (e.message ?: ""))
            }
        }
    }
    var pendingText by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<Csv.ImportPreview?>(null) }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (text == null) {
                    vm.toast("读取失败")
                } else {
                    pendingText = text
                    preview = Csv.preview(lib, text)
                }
            } catch (e: Exception) {
                vm.toast("读取失败：" + (e.message ?: ""))
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
