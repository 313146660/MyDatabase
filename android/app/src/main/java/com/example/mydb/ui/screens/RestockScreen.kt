package com.example.mydb.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.data.*
import com.example.mydb.ui.*
import com.example.mydb.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val dataKey = lib.entries.size * 31L + (lib.entries.maxOfOrNull { it.updatedAt } ?: 0L)
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val baseItems = remember(lib.id, dataKey) { restockItems(lib) }
    // 本次手动调整后的数量：entryId -> 数量（0 表示本次不补）
    val qtyAdj = remember(lib.id) { mutableStateMapOf<String, Int>() }
    val items = remember(baseItems, qtyAdj.toMap()) {
        val adj = qtyAdj.toMap()
        baseItems.map { it.copy(restock = (adj[it.entry.id] ?: it.restock).coerceAtLeast(0)) }
    }
    val sendItems = items.filter { it.restock > 0 }
    val grouped = remember(items) { items.groupBy { it.model } }
    val unit = lib.fields.firstOrNull { it.name.contains("库存") }?.unit ?: "片"
    val cfg = remember(lib.id, dataKey) { restockCfgOf(lib) }

    var textOpen by rememberSaveable(lib.id) { mutableStateOf(false) }
    val txt = remember(lib.id, dataKey, qtyAdj.toMap()) { restockText(lib, qtyAdj.toMap()) }
    val bySupplier = remember(lib.id, dataKey, qtyAdj.toMap()) { restockTextBySupplier(lib, qtyAdj.toMap()) }
    var groupBySupplier by rememberSaveable(lib.id) { mutableStateOf(false) }
    val supplierJoined = remember(bySupplier) {
        if (bySupplier.size <= 1) ""
        else bySupplier.entries.joinToString("\r\n\r\n") { (sup, t) -> "【$sup】\r\n\r\n$t" }
    }
    // 当前展示/复制/分享/保存所使用的文本：分组模式下用拼接后的供货商分组文本
    val displayTxt = if (groupBySupplier && supplierJoined.isNotEmpty()) supplierJoined else txt

    val txtName = lib.name.replace(Regex("[\\\\/:*?\"<>|]"), "_") + "-补货-" +
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date()) + ".txt"
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            // 文本编码与写盘放到 IO 线程，主线程只负责提示与记录补货历史
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        ctx.contentResolver.openOutputStream(uri)?.use {
                            it.write(("\uFEFF" + displayTxt).toByteArray(Charsets.UTF_8))
                        }
                    }
                    vm.addRestockHistory(lib.id, displayTxt, sendItems.sumOf { it.restock }, sendItems.size, sendItems)
                    vm.toast("已保存补货文本")
                } catch (e: Exception) {
                    vm.toast("保存失败：" + (e.message ?: ""))
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(
            title = "补货清单",
            subtitle = "${lib.name} · ${sendItems.size} 个规格需补货",
            onBack = { vm.back() },
            trailing = {
                IconButton(onClick = { vm.go(Scr.RestockHistory(lib.id)) }) {
                    Text("历史", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                }
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
                        "用 − / + 调整本次补货数量，点数字恢复默认，调到 0 表示本次不补；点一行看规格详情。",
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
                            val outCnt = list.count { it.isOut }
                            if (outCnt > 0) {
                                Text("缺货 $outCnt", fontSize = 11.sp, color = Danger, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                            }
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
                        val numColor = if (item.isOut) Danger else Teal
                        Row(
                            Modifier.fillMaxWidth().background(if (item.isOut) Danger.copy(alpha = 0.08f) else Surface)
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
                            if (item.isOut) {
                                Text("缺货", fontSize = 10.sp, color = Danger, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 4.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StepBtn("−", enabled = item.restock > 0) {
                                        qtyAdj[item.entry.id] = (item.restock - 1).coerceAtLeast(0)
                                    }
                                    Text(
                                        "${item.restock}",
                                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                        color = if (item.restock == 0) Muted2 else numColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { qtyAdj.remove(item.entry.id) }
                                            .padding(vertical = 2.dp)
                                    )
                                    StepBtn("+") {
                                        qtyAdj[item.entry.id] = (item.restock + 1).coerceAtMost(999)
                                    }
                                }
                                Text(
                                    "现 ${item.qty} · 目标 ${item.target}",
                                    fontSize = 9.5.sp, color = Muted2,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
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
                    "范围：$scopeText · 补货数 = 目标库存 − 当前库存（可在清单上用 − / + 调整）",
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
                    "按型号分组，数量取清单上的调整值；调到 0 的规格不会出现在文本里。",
                    fontSize = 11.5.sp, color = Muted2
                )
                if (bySupplier.size > 1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TagChip("按型号", !groupBySupplier) { groupBySupplier = false }
                        TagChip("按供货商", groupBySupplier) { groupBySupplier = true }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Box(
                    Modifier.fillMaxWidth().height(300.dp)
                        .clip(RoundedCornerShape(14.dp)).background(Bg)
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            displayTxt,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = TextMain,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        )
                    }
                }
                if (groupBySupplier && bySupplier.size > 1) {
                    Spacer(Modifier.height(10.dp))
                    Text("按供货商单独复制：", fontSize = 10.5.sp, color = Muted2)
                    bySupplier.forEach { (sup, t) ->
                        TextButton(
                            onClick = { clipboard.setText(AnnotatedString(t)); vm.toast("已复制「$sup」") },
                            modifier = Modifier.fillMaxWidth().height(32.dp)
                        ) { Text("复制 $sup", fontSize = 12.sp, color = Primary) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                PillButton(
                    "复制全部（${sendItems.size} 个规格）",
                    onClick = {
                        vm.addRestockHistory(lib.id, displayTxt, sendItems.sumOf { it.restock }, sendItems.size, sendItems)
                        clipboard.setText(AnnotatedString(displayTxt))
                        vm.toast("已复制补货文本")
                    },
                    color = Teal
                )
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            vm.addRestockHistory(lib.id, displayTxt, sendItems.sumOf { it.restock }, sendItems.size, sendItems)
                            shareRestockText(ctx, "${lib.name} 补货清单", displayTxt)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("分享给供货商", fontSize = 12.5.sp, color = Primary)
                    }
                    TextButton(
                        onClick = { saveLauncher.launch(txtName) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存为 txt", fontSize = 12.5.sp, color = Primary)
                    }
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
    val dataKey = lib.entries.size * 31L + (lib.entries.maxOfOrNull { it.updatedAt } ?: 0L)
    val cfg = remember(lib.id, dataKey) { restockCfgOf(lib) }
    val models = remember(lib.id, dataKey) { modelsOf(lib) }

    // UI 输入状态：key 只用 lib.id，entries 变化不重置用户输入
    var defPlain by remember(lib.id) { mutableStateOf(cfg.defaultPlain.toString()) }
    var defCyl by remember(lib.id) { mutableStateOf(cfg.defaultCyl.toString()) }
    val plainInputs = remember(lib.id) {
        mutableStateMapOf<String, String>().apply {
            models.forEach { put(it, cfg.targets[it]?.plain?.toString() ?: "") }
        }
    }
    val cylInputs = remember(lib.id) {
        mutableStateMapOf<String, String>().apply {
            models.forEach { put(it, cfg.targets[it]?.cyl?.toString() ?: "") }
        }
    }
    val supplierInputs = remember(lib.id) {
        mutableStateMapOf<String, String>().apply {
            models.forEach { put(it, cfg.suppliers[it] ?: "") }
        }
    }
    val rangeEdits = remember(lib.id) {
        mutableStateMapOf<String, RestockRangeRule>().apply {
            cfg.ranges.forEach { put(it.id, it) }
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
                    Column(Modifier.fillMaxWidth().background(Surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("供货商", fontSize = 10.sp, color = Muted, modifier = Modifier.width(48.dp))
                            AppTextField(supplierInputs[model].orEmpty(), { supplierInputs[model] = it }, placeholder = "未分类", keyboardType = KeyboardType.Text)
                        }
                    }
                    DividerLine()
                }
            }

            item { SectionLabel("度数范围目标（可选，命中后覆盖默认/型号目标）") }
            if (models.isNotEmpty()) {
                items(models, key = { "r_$it" }) { model ->
                    val modelRules = rangeEdits.values.filter { it.model == model }
                    Column(Modifier.fillMaxWidth().background(Surface).padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(model, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMain, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                val nr = RestockRangeRule(id = java.util.UUID.randomUUID().toString(), model = model)
                                rangeEdits[nr.id] = nr
                            }) { Text("+ 添加规则", fontSize = 11.sp, color = Primary) }
                        }
                        modelRules.forEach { rule ->
                            RangeRuleRow(
                                rule = rule,
                                onChange = { rangeEdits[it.id] = it },
                                onDelete = { rangeEdits.remove(rule.id) }
                            )
                            DividerLine()
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(16.dp)) {
                    PillButton("保存设置", onClick = {
                        vm.setRestockDefault(libId, defPlain.toIntOrNull() ?: 0, defCyl.toIntOrNull() ?: 0)
                        models.forEach { m ->
                            vm.setRestockTarget(libId, m, plainInputs[m]?.toIntOrNull(), cylInputs[m]?.toIntOrNull())
                            vm.setSupplier(libId, m, supplierInputs[m].orEmpty())
                        }
                        rangeEdits.values.forEach { r -> vm.saveRestockRangeRule(libId, r) }
                        cfg.ranges.map { it.id }.subtract(rangeEdits.keys).forEach { vm.deleteRestockRangeRule(libId, it) }
                        vm.toast("目标设置已保存")
                        vm.back()
                    }, color = Teal)
                }
            }
        }
    }
}

@Composable
private fun StepBtn(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(26.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (enabled) Teal.copy(alpha = 0.12f) else Line.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = if (enabled) Teal else Muted2
        )
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

/** 调起系统分享面板，把补货文本发给供货商（微信/QQ 等）。 */
private fun shareRestockText(ctx: android.content.Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, "分享补货文本"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockHistoryScreen(vm: AppViewModel, libId: String) {
    val rev = vm.rev
    val lib = vm.lib(libId)
    val all = vm.restockHistory
    val records = remember(all, libId) { all.filter { it.libId == libId } }
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var openId by rememberSaveable(libId) { mutableStateOf<String?>(null) }
    var confirmReceive by rememberSaveable(libId) { mutableStateOf(false) }
    val openRec = records.firstOrNull { it.id == openId }
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) }

    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(
            title = "补货历史",
            subtitle = lib?.name,
            onBack = { vm.back() },
            trailing = {
                if (records.isNotEmpty()) {
                    IconButton(onClick = {
                        vm.clearRestockHistory(libId)
                        openId = null
                        vm.toast("已清空补货历史")
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "清空", tint = Danger)
                    }
                }
            }
        )
        if (records.isEmpty()) {
            EmptyBlock("doc", "还没有补货记录", "生成补货文本后，复制 / 分享 / 保存都会自动记到这里")
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(records, key = { it.id }) { rec ->
                    Row(
                        Modifier.fillMaxWidth().background(Surface)
                            .clickable { openId = rec.id }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(fmt.format(Date(rec.at)), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                                Spacer(Modifier.width(8.dp))
                                val (tagTxt, tagCol) = if (rec.received) "已入库" to Teal else "在途" to Amber
                                Text(
                                    tagTxt, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = tagCol,
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                        .background(tagCol.copy(alpha = 0.14f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                            Text("${rec.count} 个规格 · 共 ${rec.total} 片", fontSize = 11.sp, color = Muted)
                            Text(
                                rec.text.lineSequence().firstOrNull().orEmpty(),
                                fontSize = 10.5.sp, color = Muted2,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        IconButton(onClick = {
                            vm.deleteRestockHistory(rec.id)
                            if (openId == rec.id) openId = null
                            vm.toast("已删除")
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Muted, modifier = Modifier.size(18.dp))
                        }
                    }
                    DividerLine()
                }
            }
        }
    }

    if (openRec != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { openId = null }, sheetState = sheetState, containerColor = Surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("补货文本", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Text(
                    fmt.format(Date(openRec.at)) + " · ${openRec.count} 个规格 · 共 ${openRec.total} 片",
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
                            openRec.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            color = TextMain,
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                PillButton("复制全部", onClick = {
                    clipboard.setText(AnnotatedString(openRec.text))
                    vm.toast("已复制")
                }, color = Teal)
                if (!openRec.received) {
                    Spacer(Modifier.height(4.dp))
                    PillButton("确认入库（货到后点此）", onClick = { confirmReceive = true }, color = Amber)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { shareRestockText(ctx, "${openRec.libName} 补货清单", openRec.text) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("分享给供货商", fontSize = 12.5.sp, color = Primary)
                }
            }
        }
    }
    if (confirmReceive && openRec != null) {
        AlertDialog(
            onDismissRequest = { confirmReceive = false },
            title = { Text("确认入库？", color = TextMain) },
            text = { Text("将把本次补货明细数量加到对应库存，找不到的规格会自动新建。确认后不可撤销。", color = TextMain) },
            confirmButton = {
                TextButton(onClick = {
                    vm.receiveRestock(libId, openRec.id)
                    confirmReceive = false
                    openId = null
                }) { Text("确认入库", color = Teal) }
            },
            dismissButton = { TextButton(onClick = { confirmReceive = false }) { Text("取消", color = Muted) } },
            containerColor = Surface
        )
    }
}

@Composable
private fun RangeRuleRow(
    rule: RestockRangeRule,
    onChange: (RestockRangeRule) -> Unit,
    onDelete: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(76.dp)) {
                AppTextField(rule.sphMin, { onChange(rule.copy(sphMin = it)) }, placeholder = "-4.00", keyboardType = KeyboardType.Decimal)
            }
            Text("～", color = Muted, modifier = Modifier.padding(horizontal = 2.dp))
            Box(Modifier.width(76.dp)) {
                AppTextField(rule.sphMax, { onChange(rule.copy(sphMax = it)) }, placeholder = "-2.00", keyboardType = KeyboardType.Decimal)
            }
            Spacer(Modifier.width(6.dp))
            TagChip("全部", rule.cyl == "all") { onChange(rule.copy(cyl = "all")) }
            Spacer(Modifier.width(3.dp))
            TagChip("单光", rule.cyl == "none") { onChange(rule.copy(cyl = "none")) }
            Spacer(Modifier.width(3.dp))
            TagChip("散光", rule.cyl == "has") { onChange(rule.copy(cyl = "has")) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "删除规则", tint = Muted, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("单光目标", fontSize = 10.sp, color = Muted, modifier = Modifier.width(56.dp))
            Box(Modifier.width(76.dp)) {
                AppTextField(
                    if (rule.plain < 0) "" else rule.plain.toString(),
                    { onChange(rule.copy(plain = it.toIntOrNull() ?: -1)) },
                    placeholder = "不覆盖", keyboardType = KeyboardType.Number
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("散光目标", fontSize = 10.sp, color = Muted, modifier = Modifier.width(56.dp))
            Box(Modifier.width(76.dp)) {
                AppTextField(
                    if (rule.cylTarget < 0) "" else rule.cylTarget.toString(),
                    { onChange(rule.copy(cylTarget = it.toIntOrNull() ?: -1)) },
                    placeholder = "不覆盖", keyboardType = KeyboardType.Number
                )
            }
        }
    }
}
