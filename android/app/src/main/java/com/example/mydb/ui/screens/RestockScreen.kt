package com.example.mydb.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.data.*
import com.example.mydb.ui.*
import com.example.mydb.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(vm: AppViewModel, libId: String) {
    val rev = vm.rev
    val lib = vm.lib(libId)
    if (lib == null) {
        EmptyBlock("database", "数据库不存在", "它可能已经被删除")
        return
    }
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val items = remember(lib.id, rev) { restockItems(lib) }
    val grouped = remember(items) { items.groupBy { it.model } }
    val unit = lib.fields.firstOrNull { it.name.contains("库存") }?.unit ?: "片"
    val cfg = remember(lib.id, rev) { restockCfgOf(lib) }

    var textOpen by rememberSaveable(lib.id) { mutableStateOf(false) }
    val txt = remember(lib.id, rev) { restockText(lib) }

    val txtName = lib.name.replace(Regex("[\\\\/:*?\"<>|]"), "_") + "-补货-" +
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date()) + ".txt"
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use {
                    it.write(("\uFEFF" + txt).toByteArray(Charsets.UTF_8))
                }
                vm.toast("已保存补货文本")
            } catch (e: Exception) {
                vm.toast("保存失败：" + (e.message ?: ""))
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(
            title = "补货清单",
            subtitle = "${lib.name} · ${items.size} 个规格需补货",
            onBack = { vm.back() },
            trailing = {
                IconButton(onClick = { vm.go(Scr.RestockCfg(lib.id)) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "目标设置", tint = TextMain)
                }
            }
        )
        if (items.isEmpty()) {
            EmptyBlock("box", "库存都到目标了", "没有需要补货的规格")
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Text(
                        "点一行看这条规格的详情；下方可以生成发给供货商的文本。",
                        fontSize = 11.sp, color = Muted2,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                grouped.forEach { (model, list) ->
                    item(key = "h_$model") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(model, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.weight(1f))
                            val modelSum = list.sumOf { it.restock }
                            Text(
                                "共 $modelSum $unit",
                                fontSize = 12.sp,
                                color = Teal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(list.size, key = { i -> list[i].entry.id }) { i ->
                        val item = list[i]
                        Row(
                            Modifier.fillMaxWidth().background(Surface)
                                .clickable { vm.openDetail(lib.id, item.entry.id) }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                fmtPower(item.sph),
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain,
                                modifier = Modifier.width(64.dp)
                            )
                            Text(
                                if (item.cyl == 0.0) "单光" else fmtPower(item.cyl),
                                fontSize = 12.sp, color = Muted, modifier = Modifier.width(64.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "补 ${item.restock} $unit",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teal
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("现 ${item.qty} · 目标 ${item.target}", fontSize = 10.sp, color = Muted2)
                            AppIcon("chevron", 15.dp, Muted2)
                        }
                        DividerLine()
                    }
                }
                item { Spacer(Modifier.height(6.dp)) }
            }
            Column(Modifier.padding(16.dp)) {
                val total = items.sumOf { it.restock }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("本次合计", fontSize = 12.sp, color = Muted)
                    Spacer(Modifier.width(6.dp))
                    Text("$total $unit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Teal)
                }
                Spacer(Modifier.height(6.dp))
                val scopeText = if (cfg.scope == RESTOCK_SCOPE_LOW_STOCK) "仅低于提醒阈值" else "全部低于目标"
                Text(
                    "范围：$scopeText · 补货数 = 目标库存 − 当前库存",
                    fontSize = 10.5.sp, color = Muted2,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                PillButton("生成补货文本（可复制）", onClick = { textOpen = true }, color = Teal)
            }
        }
    }

    if (textOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { textOpen = false }, sheetState = sheetState, containerColor = Surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("补货文本", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Text(
                    "按型号分组，数量 = 目标库存 − 当前库存。点右上角「目标设置」调整各型号目标。",
                    fontSize = 11.5.sp, color = Muted2
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().height(300.dp)
                        .clip(RoundedCornerShape(14.dp)).background(Bg)
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            txt,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = TextMain,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                PillButton(
                    "复制全部（${items.size} 个规格）",
                    onClick = {
                        clipboard.setText(AnnotatedString(txt))
                        vm.toast("已复制补货文本")
                    },
                    color = Teal
                )
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { saveLauncher.launch(txtName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存为 txt 文件", fontSize = 12.5.sp, color = Primary)
                }
            }
        }
    }
}

@Composable
fun RestockConfigScreen(vm: AppViewModel, libId: String) {
    val rev = vm.rev
    val lib = vm.lib(libId)
    if (lib == null) {
        EmptyBlock("database", "数据库不存在", "它可能已经被删除")
        return
    }
    val cfg = remember(lib.id, rev) { restockCfgOf(lib) }
    val models = remember(lib.id, rev) { modelsOf(lib) }

    var defPlain by remember(lib.id, rev) { mutableStateOf(cfg.defaultPlain.toString()) }
    var defCyl by remember(lib.id, rev) { mutableStateOf(cfg.defaultCyl.toString()) }
    val plainInputs = remember(lib.id, rev) {
        mutableStateMapOf<String, String>().apply {
            models.forEach { put(it, cfg.targets[it]?.plain?.toString() ?: "") }
        }
    }
    val cylInputs = remember(lib.id, rev) {
        mutableStateMapOf<String, String>().apply {
            models.forEach { put(it, cfg.targets[it]?.cyl?.toString() ?: "") }
        }
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(title = "目标库存设置", subtitle = lib.name, onBack = { vm.back() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionLabel("默认目标（型号未单独设置时使用）") }
            item {
                Row(
                    Modifier.fillMaxWidth().background(Surface).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        Column {
                            Text("单光目标", fontSize = 11.sp, color = Muted)
                            AppTextField(defPlain, { defPlain = it.filter { c -> c.isDigit() }.take(3) }, placeholder = "4", keyboardType = KeyboardType.Number)
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        Column {
                            Text("散光目标", fontSize = 11.sp, color = Muted)
                            AppTextField(defCyl, { defCyl = it.filter { c -> c.isDigit() }.take(3) }, placeholder = "2", keyboardType = KeyboardType.Number)
                        }
                    }
                }
            }

            item { SectionLabel("补货范围") }
            item {
                ScopeRow("全部低于目标", "库存没到目标值的规格都补货，库存留空按 0 算", cfg.scope != RESTOCK_SCOPE_LOW_STOCK) {
                    vm.setRestockScope(libId, RESTOCK_SCOPE_BELOW_TARGET)
                }
            }
            item { DividerLine() }
            item {
                ScopeRow("仅低于提醒阈值", "只列库存提醒标红的规格，数量仍按补到目标算", cfg.scope == RESTOCK_SCOPE_LOW_STOCK) {
                    vm.setRestockScope(libId, RESTOCK_SCOPE_LOW_STOCK)
                }
            }

            item { SectionLabel("各型号目标（留空 = 用默认）") }
            if (models.isEmpty()) {
                item { EmptyBlock("box", "暂无型号", "先在库存里录入型号，再来设置目标") }
            } else {
                items(models, key = { it }) { model ->
                    Row(
                        Modifier.fillMaxWidth().background(Surface).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(model, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextMain, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.width(82.dp)) {
                            Column {
                                Text("单光", fontSize = 10.sp, color = Muted)
                                AppTextField(plainInputs[model].orEmpty(), { plainInputs[model] = it.filter { c -> c.isDigit() }.take(3) }, placeholder = cfg.defaultPlain.toString(), keyboardType = KeyboardType.Number)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.width(82.dp)) {
                            Column {
                                Text("散光", fontSize = 10.sp, color = Muted)
                                AppTextField(cylInputs[model].orEmpty(), { cylInputs[model] = it.filter { c -> c.isDigit() }.take(3) }, placeholder = cfg.defaultCyl.toString(), keyboardType = KeyboardType.Number)
                            }
                        }
                    }
                    DividerLine()
                }
            }

            item {
                Column(Modifier.padding(16.dp)) {
                    PillButton("保存设置", onClick = {
                        vm.setRestockDefault(libId, defPlain.toIntOrNull() ?: 0, defCyl.toIntOrNull() ?: 0)
                        models.forEach { m ->
                            vm.setRestockTarget(libId, m, plainInputs[m]?.toIntOrNull(), cylInputs[m]?.toIntOrNull())
                        }
                        vm.toast("目标设置已保存")
                        vm.back()
                    }, color = Teal)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp, color = Muted2, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
private fun ScopeRow(label: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text(desc, fontSize = 10.5.sp, color = Muted)
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Teal, modifier = Modifier.size(18.dp))
        }
    }
}
