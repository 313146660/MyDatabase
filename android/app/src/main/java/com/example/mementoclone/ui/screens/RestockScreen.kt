package com.example.mementoclone.ui.screens

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
import com.example.mementoclone.data.*
import com.example.mementoclone.ui.*
import com.example.mementoclone.ui.theme.*
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
    val items = remember(lib.id, rev) { lowStockOf(lib) }
    val grouped = remember(items) { items.groupBy { it.model } }
    val unit = lib.fields.firstOrNull { it.name.contains("库存") }?.unit ?: "片"

    var textOpen by rememberSaveable(lib.id) { mutableStateOf(false) }
    var plainQty by rememberSaveable(lib.id) { mutableStateOf("4") }
    var cylQty by rememberSaveable(lib.id) { mutableStateOf("2") }
    val plain = plainQty.toIntOrNull() ?: 4
    val cylN = cylQty.toIntOrNull() ?: 2
    val txt = remember(lib.id, rev, plain, cylN) { restockText(lib, plain, cylN) }

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
            subtitle = "${lib.name} · ${items.size} 个规格低于阈值",
            onBack = { vm.back() }
        )
        if (items.isEmpty()) {
            EmptyBlock("box", "库存都够用", "没有低于提醒阈值的规格")
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
                            val zero = list.count { it.qty <= 0 }
                            Text(
                                "${list.size} 个" + if (zero > 0) " · $zero 缺货" else "",
                                fontSize = 10.5.sp,
                                color = if (zero > 0) Danger else Muted2,
                                fontWeight = if (zero > 0) FontWeight.SemiBold else FontWeight.Normal
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
                                "${item.qty.toInt()} $unit",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = if (item.qty <= 0) Danger else Amber
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("阈值 ${item.threshold}", fontSize = 10.sp, color = Muted2)
                            AppIcon("chevron", 15.dp, Muted2)
                        }
                        DividerLine()
                    }
                }
                item { Spacer(Modifier.height(6.dp)) }
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    "补货数量：单光 $plain $unit · 带散光 $cylN $unit",
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
                    "按型号分组，单光按 $plain $unit、带散光按 $cylN $unit 生成，复制后直接发给供货商。",
                    fontSize = 11.5.sp, color = Muted2
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        Column {
                            Text("单光数量", fontSize = 11.5.sp, color = Muted)
                            Spacer(Modifier.height(4.dp))
                            AppTextField(
                                plainQty,
                                { t -> plainQty = t.filter { it.isDigit() }.take(3) },
                                placeholder = "4", keyboardType = KeyboardType.Number
                            )
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        Column {
                            Text("带散光数量", fontSize = 11.5.sp, color = Muted)
                            Spacer(Modifier.height(4.dp))
                            AppTextField(
                                cylQty,
                                { t -> cylQty = t.filter { it.isDigit() }.take(3) },
                                placeholder = "2", keyboardType = KeyboardType.Number
                            )
                        }
                    }
                }
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
