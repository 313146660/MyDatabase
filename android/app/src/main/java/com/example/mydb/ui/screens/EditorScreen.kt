package com.example.mydb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.data.*
import com.example.mydb.ui.*
import com.example.mydb.ui.theme.*

@Composable
fun EditorScreen(vm: AppViewModel, libId: String, entryId: String?) {
    val rev = vm.rev
    val lib = vm.lib(libId)
    if (lib == null) {
        EmptyBlock("database", "数据库不存在", "")
        return
    }
    val existing = entryId?.let { id -> lib.entries.firstOrNull { it.id == id } }
    val isNew = existing == null
    val values = remember(libId, entryId) {
        mutableStateMapOf<String, String>().apply {
            if (existing != null) putAll(existing.values)
        }
    }
    val picked = remember(libId, entryId) { mutableSetOf<String>() }
    var pickField by remember { mutableStateOf<MField?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val color = hexColor(lib.color)

    // 新记录默认填今天
    LaunchedEffect(libId, entryId) {
        if (isNew) {
            lib.fields.filter { it.type == FieldType.DATE }.forEach { f ->
                if ((values[f.id] ?: "").isBlank()) values[f.id] = fmtDay(System.currentTimeMillis())
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(
            title = if (isNew) "新增记录" else "编辑记录",
            subtitle = lib.name,
            onBack = { vm.back() },
            trailing = {
                TextButton(onClick = {
                    val missing = lib.fields.firstOrNull { it.required && it.type != FieldType.FORMULA && (values[it.id] ?: "").isBlank() }
                    if (missing != null) {
                        error = "请填写「${missing.name}」"
                        return@TextButton
                    }
                    val msg = if (isNew) {
                        vm.addEntry(lib.id, values, picked)
                    } else {
                        vm.updateEntry(lib.id, existing!!.id, values, picked)
                    }
                    if (msg != null) vm.toast(msg)
                    vm.back()
                }) {
                    Text("保存", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }
        )

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (error != null) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp))
                        .background(DangerSoft).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(error!!, fontSize = 12.sp, color = Danger, modifier = Modifier.weight(1f))
                    IconButton(onClick = { error = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = Danger, modifier = Modifier.size(14.dp))
                    }
                }
            }
            lib.fields.forEach { f ->
                if (f.type == FieldType.FORMULA) return@forEach
                MCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                            if (f.required) Text(" *", fontSize = 12.sp, color = Danger)
                            Spacer(Modifier.weight(1f))
                            Text(
                                FieldType.label(f.type) + if (f.unit != null) " · ${f.unit}" else "",
                                fontSize = 9.5.sp, color = Muted2
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        when (f.type) {
                            FieldType.SELECT -> SelectInput(f, values[f.id] ?: "", onChange = { values[f.id] = it })
                            FieldType.MULTISELECT -> SelectInput(
                                f, values[f.id] ?: "", multi = true,
                                onChange = { values[f.id] = it }
                            )
                            FieldType.PICK -> PickInput(
                                lib = lib, field = f, value = values[f.id] ?: "",
                                onPick = { pickField = f },
                                onClear = { values[f.id] = ""; picked.remove(f.id) }
                            )
                            FieldType.LONGTEXT -> AppTextField(
                                values[f.id] ?: "", { values[f.id] = it }, placeholder = "填写备注…",
                                singleLine = false, minLines = 3
                            )
                            FieldType.NUMBER -> AppTextField(
                                values[f.id] ?: "", { values[f.id] = it }, placeholder = "0",
                                keyboardType = KeyboardType.Decimal
                            )
                            FieldType.DATE -> Column {
                                AppTextField(values[f.id] ?: "", { values[f.id] = it }, placeholder = "2026/11/19")
                                Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("今天", "昨天").forEach { label ->
                                        val ts = System.currentTimeMillis() - if (label == "昨天") 86_400_000L else 0L
                                        Text(
                                            label, fontSize = 10.5.sp, color = Primary,
                                            modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimarySoft)
                                                .clickable { values[f.id] = fmtDay(ts) }.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            FieldType.AUTONUMBER -> Text(
                                if (isNew) "保存后自动生成" else (f.prefix ?: "") + (values[f.id] ?: ""),
                                fontSize = 13.sp, color = Muted2
                            )
                            FieldType.CREATEDAT, FieldType.UPDATEDAT -> Text(
                                values[f.id]?.ifBlank { "保存后自动生成" } ?: "保存后自动生成",
                                fontSize = 13.sp, color = Muted2
                            )
                            else -> AppTextField(
                                values[f.id] ?: "", { values[f.id] = it },
                                placeholder = "填写${f.name}",
                                keyboardType = if (f.type == FieldType.TEL) KeyboardType.Phone else KeyboardType.Text
                            )
                        }
                        if (f.type == FieldType.PICK) {
                            val p = f.pick
                            if (p != null && p.deduct) {
                                Text("保存后从镜片库扣库存；换规格会先回补上一次。", fontSize = 9.5.sp, color = Muted2, modifier = Modifier.padding(top = 5.dp))
                            }
                        }
                        if (values[f.id].isNullOrBlank() && (f.type == FieldType.PICK)) {
                            // 提示可选项
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            PillButton(if (isNew) "保存记录" else "保存修改", onClick = {
                val missing = lib.fields.firstOrNull { it.required && it.type != FieldType.FORMULA && (values[it.id] ?: "").isBlank() }
                if (missing != null) {
                    error = "请填写「${missing.name}」"
                    return@PillButton
                }
                val msg = if (isNew) vm.addEntry(lib.id, values, picked) else vm.updateEntry(lib.id, existing!!.id, values, picked)
                if (msg != null) vm.toast(msg)
                vm.back()
            }, color = color)
            Spacer(Modifier.height(30.dp))
        }
    }

    val target = pickField
    if (target != null) {
        val maxPick = lib.fields.firstOrNull { it.id == target.id }
        if (maxPick != null) {
            PickSheet(
                vm = vm, lib = lib, field = maxPick, values = values,
                onClose = { pickField = null },
                onSelect = { opt ->
                    values[target.id] = opt.value
                    picked.add(target.id)
                    pickField = null
                }
            )
        }
    }
}

@Composable
private fun PickInput(lib: MLibrary, field: MField, value: String, onPick: () -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Bg)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onPick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            value.ifBlank { "点击选择" },
            fontSize = 14.sp,
            color = if (value.isBlank()) Muted2 else TextMain,
            fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotBlank()) {
            IconButton(onClick = onClear, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "清除", tint = Muted2, modifier = Modifier.size(15.dp))
            }
        }
        AppIcon("chevron", 17.dp, Muted2)
    }
}

@Composable
private fun SelectInput(field: MField, value: String, multi: Boolean = false, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val options = if (multi && field.options.isEmpty()) listOf("是", "否") else field.options
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Bg)
                .border(1.dp, Line, RoundedCornerShape(12.dp))
                .clickable { open = true }.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value.ifBlank { "未选择" }, fontSize = 14.sp, color = if (value.isBlank()) Muted2 else TextMain, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Muted2)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(o, fontSize = 13.5.sp) }, onClick = { open = false; onChange(o) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickSheet(
    vm: AppViewModel,
    lib: MLibrary,
    field: MField,
    values: Map<String, String>,
    onClose: () -> Unit,
    onSelect: (PickOption) -> Unit
) {
    val rev = vm.rev
    var query by remember { mutableStateOf("") }
    val options = remember(field.id, rev, values[field.pick?.filterFieldId ?: ""]) {
        pickOptions(vm.state, lib, field, values)
    }
    val shown = options.filter { o ->
        query.isBlank() || (o.label + " " + o.plain + " " + o.sub).lowercase().contains(query.lowercase())
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.86f).padding(horizontal = 18.dp)) {
            Text("选择${field.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text(
                if (field.pick?.kind == "sku") "从镜片库挑选规格，保存后会扣库存" else "从关联库挑选一个值",
                fontSize = 11.sp, color = Muted2
            )
            Spacer(Modifier.height(10.dp))
            if (options.size > 6) {
                SearchBox(query, "搜索型号或度数", { query = it })
                Spacer(Modifier.height(8.dp))
            }
            LazyColumn(Modifier.weight(1f)) {
                if (shown.isEmpty()) {
                    item { EmptyBlock("search", "没有可选的选项", "先到镜片库添加库存，或清除筛选") }
                }
                items(shown, key = { it.libId + ":" + it.entryId + ":" + it.value }) { o ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(o) }
                            .padding(vertical = 10.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(o.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                            if (o.sub.isNotBlank()) Text(o.sub, fontSize = 11.sp, color = Muted2)
                        }
                        if (o.stock != null) {
                            val stockColor = if (o.stock > 0) Teal else Danger
                            Text(
                                "${o.stock}${o.unit}",
                                fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = stockColor,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(stockColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        AppIcon("chevron", 16.dp, Muted2)
                    }
                    DividerLine()
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
