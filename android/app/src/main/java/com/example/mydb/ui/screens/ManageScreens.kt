package com.example.mydb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

/* ---------------- 新建 / 编辑数据库 ---------------- */

@Composable
fun LibFormScreen(vm: AppViewModel, libId: String?) {
    val existing = libId?.let { vm.lib(it) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: "database") }
    var color by remember { mutableStateOf(existing?.color ?: "#1976D2") }
    var fontSize by remember {
        mutableStateOf(
            (
                existing?.listFontSize ?: when (existing?.listFont) {
                    "s" -> 12; "l" -> 16; "xl" -> 17; "m" -> 14; else -> vm.globalFont
                }
                ).toString()
        )
    }
    var fieldsCsv by remember { mutableStateOf("名称,备注") }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        TopBar(if (existing == null) "新建数据库" else "数据库设置", existing?.name ?: "", onBack = { vm.back() })
        Column(Modifier.padding(horizontal = 16.dp)) {
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    FormRow("名称", "第一个字段会作为条目标题") {
                        AppTextField(name, { name = it }, placeholder = "例如：客户、镜片库、药品")
                    }
                    if (existing == null) {
                        FormRow("初始字段", "用逗号分隔，例如：姓名,电话,备注") {
                            AppTextField(fieldsCsv, { fieldsCsv = it })
                        }
                    }
                    FormRow("图标") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Palettes.icons.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { ic ->
                                        val active = ic.value == icon
                                        Row(
                                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                                .background(if (active) hexColor(color).copy(alpha = 0.12f) else Bg)
                                                .border(1.dp, if (active) hexColor(color) else Line, RoundedCornerShape(12.dp))
                                                .clickable { icon = ic.value }.padding(vertical = 9.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AppIcon(ic.value, 16.dp, if (active) hexColor(color) else Muted)
                                            Spacer(Modifier.width(4.dp))
                                            Text(ic.name, fontSize = 11.sp, color = if (active) hexColor(color) else Muted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    FormRow("主题色") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Palettes.colors.forEach { p ->
                                val active = p.value == color
                                Box(
                                    Modifier.size(34.dp).clip(RoundedCornerShape(12.dp))
                                        .background(hexColor(p.value))
                                        .then(if (active) Modifier.border(3.dp, TextMain, RoundedCornerShape(12.dp)) else Modifier)
                                        .clickable { color = p.value },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (active) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                    FormRow("列表字号", "标题字号（sp），字段值约为它的 86%；建议 12–18，只影响列表条目") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                AppTextField(
                                    fontSize,
                                    { t -> if (t.all { it.isDigit() } && t.length <= 2) fontSize = t },
                                    placeholder = "14",
                                    keyboardType = KeyboardType.Number
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("sp", fontSize = 12.sp, color = Muted2)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    PillButton(if (existing == null) "创建数据库" else "保存设置", onClick = {
                        if (name.isBlank()) {
                            vm.toast("请填写名称")
                            return@PillButton
                        }
                        if (existing == null) {
                            val lib = vm.createLibrary(name, fieldsCsv, icon, color)
                            if (lib != null) lib.listFontSize = (fontSize.toIntOrNull() ?: 14).coerceIn(8, 30)
                            vm.back()
                            if (lib != null) vm.go(Scr.Library(lib.id))
                        } else {
                            vm.updateLibrary(existing.id, name, icon, color, fontSize.toIntOrNull())
                            vm.back()
                        }
                    }, color = hexColor(color))
                }
            }

            if (existing != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DangerSoft)
                        .clickable { confirmDelete = true }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppIcon("trash", 16.dp, Danger)
                    Spacer(Modifier.width(6.dp))
                    Text("删除这个数据库", fontSize = 13.sp, color = Danger, fontWeight = FontWeight.SemiBold)
                }
                Text("会删除库里所有记录，无法恢复。", fontSize = 10.5.sp, color = Muted2, modifier = Modifier.padding(start = 4.dp, top = 5.dp))
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除「${existing.name}」？") },
            text = { Text("${existing.entries.size} 条记录会一起删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteLibrary(existing.id)
                    vm.tab(Scr.Home)
                }) { Text("删除", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

/* ---------------- 字段管理 ---------------- */

@Composable
fun FieldsScreen(vm: AppViewModel, libId: String) {
    val rev = vm.rev
    val lib = vm.lib(libId) ?: return
    var deleting by remember { mutableStateOf<MField?>(null) }
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar("字段管理", "${lib.name} · ${lib.fields.size} 个字段", onBack = { vm.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("字段顺序就是详情页和编辑页的显示顺序；第一个字段会作为条目标题。", fontSize = 11.sp, color = Muted2, modifier = Modifier.padding(vertical = 6.dp))
            MCard(Modifier.fillMaxWidth()) {
                lib.fields.forEachIndexed { i, f ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Bg), contentAlignment = Alignment.Center) {
                            AppIcon(fieldIcon(f), 15.dp, Muted)
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(f.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (i == 0) {
                                    Spacer(Modifier.width(5.dp))
                                    Text("标题", fontSize = 9.sp, color = Primary, modifier = Modifier
                                        .clip(RoundedCornerShape(50)).background(PrimarySoft).padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            }
                            Text(
                                FieldType.label(f.type) + (f.unit?.let { " · $it" } ?: "") +
                                    if (f.type == FieldType.FORMULA) " · 实时计算" else " · ${lib.entries.count { (it.values[f.id] ?: "").isNotBlank() }} 条有值",
                                fontSize = 10.sp, color = Muted2, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { vm.moveField(lib.id, f.id, -1) }, enabled = i > 0, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移", tint = if (i > 0) Muted else Line, modifier = Modifier.size(17.dp))
                        }
                        IconButton(onClick = { vm.moveField(lib.id, f.id, 1) }, enabled = i < lib.fields.lastIndex, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移", tint = if (i < lib.fields.lastIndex) Muted else Line, modifier = Modifier.size(17.dp))
                        }
                        if (f.type == FieldType.AUTONUMBER) {
                            IconButton(onClick = { vm.renumberField(lib.id, f.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Filled.Refresh, contentDescription = "重新编号", tint = Muted, modifier = Modifier.size(15.dp))
                            }
                        }
                        IconButton(onClick = { vm.go(Scr.FieldForm(lib.id, f.id)) }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = Muted, modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = { deleting = f }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除", tint = Danger, modifier = Modifier.size(15.dp))
                        }
                    }
                    if (i != lib.fields.lastIndex) DividerLine()
                }
            }
            Spacer(Modifier.height(10.dp))
            PillButton("＋ 添加字段", onClick = { vm.go(Scr.FieldForm(lib.id, null)) }, color = Primary, filled = false)
            Spacer(Modifier.height(30.dp))
        }
    }

    val target = deleting
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除字段「${target.name}」？") },
            text = { Text("会同时清除所有记录里这一列的值，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    vm.deleteField(lib.id, target.id)
                }) { Text("删除", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }
        )
    }
}

private fun fieldIcon(f: MField): String = when (f.type) {
    FieldType.TEXT, FieldType.LONGTEXT -> "doc"
    FieldType.NUMBER -> "database"
    FieldType.TEL -> "phone"
    FieldType.DATE -> "calendar"
    FieldType.PICK -> "link"
    FieldType.SELECT, FieldType.MULTISELECT -> "sliders"
    FieldType.AUTONUMBER -> "refresh"
    FieldType.FORMULA -> "star"
    else -> "doc"
}

@Composable
fun FieldFormScreen(vm: AppViewModel, libId: String, fieldId: String?) {
    val lib = vm.lib(libId) ?: return
    val existing = fieldId?.let { id -> lib.fields.firstOrNull { it.id == id } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: FieldType.TEXT) }
    var unit by remember { mutableStateOf(existing?.unit ?: "") }
    var required by remember { mutableStateOf(existing?.required ?: false) }
    var options by remember { mutableStateOf(existing?.options?.joinToString(",") ?: "") }
    var expr by remember { mutableStateOf(existing?.expr ?: "") }
    var prefix by remember { mutableStateOf(existing?.prefix ?: "") }
    var start by remember { mutableStateOf((existing?.start ?: 1).toString()) }
    var pickLibId by remember { mutableStateOf(existing?.pick?.libId ?: "") }
    var pickKind by remember { mutableStateOf(existing?.pick?.kind ?: "value") }
    var pickFieldId by remember { mutableStateOf(existing?.pick?.fieldId ?: "") }
    var pickFilterId by remember { mutableStateOf(existing?.pick?.filterFieldId ?: "") }
    var pickDeduct by remember { mutableStateOf(existing?.pick?.deduct ?: false) }
    var pickQty by remember { mutableStateOf((existing?.pick?.qty ?: 1).toString()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val otherLibs = vm.state.libraries.filter { it.id != lib.id }
    val srcLib = otherLibs.firstOrNull { it.id == pickLibId }

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        TopBar(if (existing == null) "添加字段" else "编辑字段", lib.name, onBack = { vm.back() })
        Column(Modifier.padding(horizontal = 16.dp)) {
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    FormRow("字段名称") { AppTextField(name, { name = it }, placeholder = "例如：姓名、度数、备注") }
                    SelectRow("类型", type, FieldType.all) { type = it }
                    if (type in listOf(FieldType.NUMBER, FieldType.RATING)) {
                        FormRow("单位", "例如：元、片、度") { AppTextField(unit, { unit = it }, placeholder = "可不填") }
                    }
                    if (type == FieldType.SELECT || type == FieldType.MULTISELECT) {
                        FormRow("选项", "用逗号分隔，例如：远用,近用,渐进") { AppTextField(options, { options = it }) }
                    }
                    if (type == FieldType.FORMULA) {
                        FormRow("公式", "可用：字段名、se('OD')、sph()、cyl()、fmt()、+") {
                            AppTextField(expr, { expr = it }, placeholder = "'OD ' + fmt(se('OD'))")
                        }
                        val sample = lib.entries.firstOrNull()
                        val preview = remember(expr, libId, vm.rev) {
                            if (sample == null || expr.isBlank()) "" else try {
                                evalFormula(vm.state, lib, sample, expr)
                            } catch (_: Exception) {
                                "公式有误"
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(10.dp)).background(Bg)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("预览", fontSize = 10.5.sp, color = Muted2)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    sample == null -> "库里还没有记录，先存一条再试"
                                    expr.isBlank() -> "填一个公式试试"
                                    preview.isBlank() -> "（空值）"
                                    else -> preview
                                },
                                fontSize = 12.5.sp, color = if (preview == "公式有误") Danger else TextMain,
                                maxLines = 3, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (type == FieldType.AUTONUMBER) {
                        Row {
                            Box(Modifier.weight(1f)) { FormRow("前缀") { AppTextField(prefix, { prefix = it }, placeholder = "NO-") } }
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.weight(1f)) { FormRow("起始值") { AppTextField(start, { start = it }, keyboardType = KeyboardType.Number) } }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("必填", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                            Text("保存记录时必须填写", fontSize = 10.5.sp, color = Muted2)
                        }
                        Switch(checked = required, onCheckedChange = { required = it })
                    }

                    if (type == FieldType.PICK) {
                        DividerLine()
                        Spacer(Modifier.height(8.dp))
                        Text("关联选择", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text("从另一个库挑选值；只保存文字快照，删除来源库不会影响已填内容。", fontSize = 10.5.sp, color = Muted2)
                        if (otherLibs.isEmpty()) {
                            Text("还没有其它库可以做来源。", fontSize = 11.sp, color = Danger, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            SelectRow(
                                "来源库", pickLibId,
                                listOf("" to "请选择") + otherLibs.map { it.id to it.name }
                            ) {
                                pickLibId = it
                                pickFieldId = ""
                                pickFilterId = ""
                            }
                            SelectRow(
                                "选择方式", pickKind,
                                listOf("value" to "选一个值（去重，不扣库存）", "sku" to "选镜片规格（可扣库存）")
                            ) { pickKind = it }
                            if (pickKind == "value") {
                                SelectRow(
                                    "取值字段", pickFieldId,
                                    listOf("" to "请选择") + (srcLib?.fields?.map { it.id to it.name } ?: emptyList())
                                ) { pickFieldId = it }
                            } else {
                                SelectRow(
                                    "按哪个字段筛选", pickFilterId,
                                    listOf("" to "不筛选（显示全部规格）") + lib.fields.map { it.id to it.name }
                                ) { pickFilterId = it }
                            }
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("扣库存", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                                    Text("选中后从来源库对应规格减 1", fontSize = 10.5.sp, color = Muted2)
                                }
                                Switch(checked = pickDeduct, onCheckedChange = { pickDeduct = it })
                            }
                            if (pickDeduct) {
                                FormRow("每次扣减数量") { AppTextField(pickQty, { pickQty = it }, keyboardType = KeyboardType.Number) }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    PillButton(if (existing == null) "添加字段" else "保存字段", onClick = {
                        if (name.isBlank()) {
                            vm.toast("请填写字段名称")
                            return@PillButton
                        }
                        val f = MField(
                            id = existing?.id ?: uid("f_"),
                            name = name.trim(),
                            type = type,
                            unit = unit.trim().ifBlank { null },
                            required = required,
                            options = options.split(Regex("[,，]")).map { it.trim() }.filter { it.isNotEmpty() }.toMutableList(),
                            expr = expr.ifBlank { null },
                            prefix = prefix.ifBlank { null },
                            start = start.toIntOrNull() ?: 1,
                            system = existing?.system ?: false
                        )
                        if (type == FieldType.PICK && pickLibId.isNotBlank()) {
                            f.pick = PickCfg(
                                libId = pickLibId,
                                kind = pickKind,
                                fieldId = if (pickKind == "value") pickFieldId else "",
                                filterFieldId = if (pickKind == "sku") pickFilterId else "",
                                deduct = pickDeduct,
                                qty = pickQty.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            )
                        }
                        if (existing == null) {
                            vm.addField(lib.id, f)
                        } else {
                            vm.updateField(lib.id, existing.id, f)
                        }
                        vm.back()
                    }, color = Primary)
                }
            }

            if (existing != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DangerSoft)
                        .clickable { confirmDelete = true }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("删除这个字段", fontSize = 13.sp, color = Danger, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除字段「${existing.name}」？") },
            text = { Text("所有记录里这一列的值都会被清除。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteField(lib.id, existing.id)
                    vm.back()
                }) { Text("删除", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

/* ---------------- 列表显示字段 ---------------- */

@Composable
fun ListFieldsScreen(vm: AppViewModel, libId: String) {
    val rev = vm.rev
    val lib = vm.lib(libId) ?: return
    var selected by remember(libId, rev) { mutableStateOf(listFieldsOf(lib).map { it.id }) }
    val on = selected.mapNotNull { id -> lib.fields.firstOrNull { it.id == id } }
    val off = lib.fields.filter { it.id !in selected }

    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar("列表显示字段", lib.name, onBack = { vm.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("已显示 ${on.size} 个。第一个字段作为标题，其余会自动换行。", fontSize = 11.sp, color = Muted2, modifier = Modifier.padding(vertical = 6.dp))
            MCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("显示字段名称", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                        Text("关掉后只显示值，一行能放更多内容", fontSize = 10.5.sp, color = Muted2)
                    }
                    Switch(checked = lib.listShowNames, onCheckedChange = { vm.setListShowNames(lib.id, it) })
                }
            }
            Spacer(Modifier.height(10.dp))
            MCard(Modifier.fillMaxWidth()) {
                on.forEachIndexed { i, f ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", fontSize = 11.sp, color = Muted2, modifier = Modifier.width(20.dp))
                        Text(f.name, fontSize = 13.sp, color = TextMain, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            if (i > 0) {
                                val list = selected.toMutableList()
                                val t = list[i]; list[i] = list[i - 1]; list[i - 1] = t
                                selected = list
                            }
                        }, enabled = i > 0, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移", tint = if (i > 0) Muted else Line, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = {
                            if (i < selected.size - 1) {
                                val list = selected.toMutableList()
                                val t = list[i]; list[i] = list[i + 1]; list[i + 1] = t
                                selected = list
                            }
                        }, enabled = i < selected.size - 1, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移", tint = if (i < selected.size - 1) Muted else Line, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { selected = selected - f.id }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "移除", tint = Danger, modifier = Modifier.size(15.dp))
                        }
                    }
                    if (i != on.lastIndex) DividerLine()
                }
                if (on.isEmpty()) {
                    Text("还没有选择字段", fontSize = 12.sp, color = Muted2, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("未显示 ${off.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            MCard(Modifier.fillMaxWidth()) {
                off.forEachIndexed { i, f ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = selected + f.id }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon("plus", 15.dp, Muted2)
                        Spacer(Modifier.width(8.dp))
                        Text(f.name, fontSize = 13.sp, color = TextMain, modifier = Modifier.weight(1f))
                        Text(FieldType.label(f.type), fontSize = 10.sp, color = Muted2)
                    }
                    if (i != off.lastIndex) DividerLine()
                }
                if (off.isEmpty()) Text("所有字段都已经显示", fontSize = 12.sp, color = Muted2, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.height(14.dp))
            PillButton("保存显示设置", onClick = {
                vm.setListFields(lib.id, selected)
                vm.back()
            })
            Spacer(Modifier.height(30.dp))
        }
    }
}

/* ---------------- 库存提醒 ---------------- */

@Composable
fun AlertsScreen(vm: AppViewModel, libId: String) {
    val rev = vm.rev
    val lib = vm.lib(libId) ?: return
    val al = alertsOf(lib) ?: return
    var def by remember(libId, rev) { mutableStateOf(al.defaultThreshold.toString()) }
    val items = lowStockOf(lib)
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        TopBar("库存提醒", lib.name, onBack = { vm.back() })
        Column(Modifier.padding(horizontal = 16.dp)) {
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (items.isEmpty()) "当前没有低于阈值的规格"
                        else "当前有 ${items.size} 个规格低于阈值" + if (items.any { it.qty <= 0 }) "，其中 ${items.count { it.qty <= 0 }} 个已缺货" else "",
                        fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (items.isEmpty()) Teal else Danger
                    )
                    Spacer(Modifier.height(10.dp))
                    FormRow("统一阈值", "没有单独规则的规格使用这个值；0 表示只在缺货时提醒") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { AppTextField(def, { def = it }, keyboardType = KeyboardType.Number) }
                            Spacer(Modifier.width(8.dp))
                            PillButton("保存", onClick = {
                                vm.setDefaultThreshold(lib.id, def.toIntOrNull() ?: 0)
                            }, color = Primary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("单独规则", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.weight(1f))
                Text("＋ 添加", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { vm.go(Scr.AlertForm(lib.id, null)) }.padding(4.dp))
            }
            Spacer(Modifier.height(6.dp))
            if (al.rules.isEmpty()) {
                MCard(Modifier.fillMaxWidth()) {
                    Text("还没有单独规则，所有规格都使用统一阈值。", fontSize = 12.sp, color = Muted2, modifier = Modifier.padding(14.dp))
                }
            } else {
                al.rules.forEachIndexed { i, r ->
                    MCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.go(Scr.AlertForm(lib.id, r.id)) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(AmberSoft), contentAlignment = Alignment.Center) {
                                AppIcon("bell", 15.dp, Amber)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ruleTitle(r), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                                Text("库存 ≤ ${r.threshold} 时提醒", fontSize = 10.5.sp, color = Muted2)
                            }
                            AppIcon("chevron", 16.dp, Muted2)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("规则从上到下匹配，先匹配到的生效。「球镜从 / 到」按数值比较，留空表示不限。", fontSize = 10.5.sp, color = Muted2)
            Spacer(Modifier.height(30.dp))
        }
    }
}

private fun ruleTitle(r: AlertRule): String {
    val model = r.model.ifBlank { "全部型号" }
    val lo = toPower(r.sphMin)
    val hi = toPower(r.sphMax)
    val range = when {
        lo != null && hi != null -> "球镜 ${fmtPower(minOf(lo, hi))} ~ ${fmtPower(maxOf(lo, hi))}"
        lo != null -> "球镜 ≥ ${fmtPower(lo)}"
        hi != null -> "球镜 ≤ ${fmtPower(hi)}"
        else -> "全部球镜"
    }
    val cyl = when (r.cyl) {
        "none" -> "无散光"
        "has" -> "有散光"
        else -> "散光不限"
    }
    return "$model · $range · $cyl"
}

@Composable
fun AlertFormScreen(vm: AppViewModel, libId: String, ruleId: String?) {
    val lib = vm.lib(libId) ?: return
    val al = alertsOf(lib) ?: return
    val existing = ruleId?.let { id -> al.rules.firstOrNull { it.id == id } }
    val models = remember(lib.id, vm.rev) {
        lib.entries.map { (it.values[al.cfg.group.id] ?: "").trim() }.filter { it.isNotEmpty() }.distinct()
    }
    var model by remember { mutableStateOf(existing?.model ?: "") }
    var sphMin by remember { mutableStateOf(existing?.sphMin ?: "") }
    var sphMax by remember { mutableStateOf(existing?.sphMax ?: "") }
    var cyl by remember { mutableStateOf(existing?.cyl ?: "all") }
    var threshold by remember { mutableStateOf((existing?.threshold ?: 1).toString()) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        TopBar(if (existing == null) "添加提醒规则" else "编辑提醒规则", "可以按型号、球镜范围和散光分别设置", onBack = { vm.back() })
        Column(Modifier.padding(horizontal = 16.dp)) {
            MCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SelectRow("型号", model, listOf("" to "全部型号") + models.map { it to it }) { model = it }
                    Row {
                        Box(Modifier.weight(1f)) { FormRow("球镜从", "留空不限") { AppTextField(sphMin, { sphMin = it }, placeholder = "-4.00") } }
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) { FormRow("球镜到", "留空不限") { AppTextField(sphMax, { sphMax = it }, placeholder = "-1.00") } }
                    }
                    SelectRow(
                        "柱镜", cyl,
                        listOf("all" to "不限", "none" to "无散光（柱镜 0）", "has" to "有散光")
                    ) { cyl = it }
                    FormRow("库存 ≤", "低于或等于这个数量时提醒") {
                        AppTextField(threshold, { threshold = it }, keyboardType = KeyboardType.Number)
                    }
                    Spacer(Modifier.height(10.dp))
                    PillButton("保存规则", onClick = {
                        vm.saveAlertRule(
                            lib.id, existing?.id,
                            AlertRule(
                                id = existing?.id ?: uid("ar_"),
                                model = model,
                                sphMin = sphMin.trim(),
                                sphMax = sphMax.trim(),
                                cyl = cyl,
                                threshold = (threshold.toIntOrNull() ?: 0).coerceAtLeast(0)
                            )
                        )
                        vm.back()
                    }, color = Amber)
                }
            }
            if (existing != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DangerSoft)
                        .clickable { confirmDelete = true }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) { Text("删除这条规则", fontSize = 13.sp, color = Danger, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条规则？") },
            text = { Text("删除后会使用统一阈值。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteAlertRule(lib.id, existing.id)
                    vm.back()
                }) { Text("删除", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}
