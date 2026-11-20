package com.example.mydb.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.data.*
import com.example.mydb.ui.*
import com.example.mydb.ui.theme.*
import kotlin.math.abs

@Composable
fun DetailScreen(vm: AppViewModel, libId: String, entryId: String) {
    val rev = vm.rev
    val lib = vm.lib(libId)
    val dataKey = lib?.let { it.entries.size * 31L + (it.entries.maxOfOrNull { e -> e.updatedAt } ?: 0L) } ?: 0L
    val entry = lib?.entries?.firstOrNull { it.id == entryId }
    if (lib == null || entry == null) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            TopBar("记录不存在", onBack = { vm.back() })
            EmptyBlock("database", "记录不存在", "它可能已经被删除")
        }
        return
    }
    val color = hexColor(lib.color)
    var confirmDelete by remember { mutableStateOf(false) }
    var mergeOpen by remember { mutableStateOf(false) }

    val cfg = matrixCfg(lib)
    val subtitle = if (cfg != null) {
        val sph = toPower(entry.values[cfg.row.id])
        val cyl = cylValueOf(entry, cfg)
        "球镜 ${fmtPower(sph)}" + if (cyl != 0.0) " · 柱镜 ${fmtPower(cyl)}" else " · 单光"
    } else subtitleOf(lib, entry)
    val history = remember(lib.id, entry.id, dataKey) { historyOf(lib, entry) }
    val family = remember(lib.id, entry.id, dataKey) { familyGroup(lib, entry) }
    val related = remember(rev, entry.id) { relatedFor(vm.state, lib, entry) }
    val picks = remember(rev, entry.id) { pickStockViews(vm.state, lib, entry) }
    val clipboard = LocalClipboardManager.current
    val phoneVal = phoneFieldOf(lib)?.let { oneLine(entry.values[it.id]) }.orEmpty()

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        TopBar(
            title = titleOf(lib, entry),
            subtitle = lib.name,
            onBack = { vm.back() },
            trailing = {
                IconButton(onClick = { vm.toggleFavorite(lib.id, entry.id) }) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "收藏",
                        tint = if (entry.favorite) Color(0xFFE0A100) else Muted2
                    )
                }
                IconButton(onClick = { vm.go(Scr.Editor(lib.id, entry.id)) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = TextMain)
                }
            }
        )

        Column(Modifier.padding(horizontal = 16.dp)) {
            var fieldsOpen by rememberSaveable(entry.id) { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface)
                    .clickable { fieldsOpen = !fieldsOpen }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(titleOf(lib, entry), color, 48.dp)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(titleOf(lib, entry), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = Muted2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (phoneVal.isNotBlank()) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(PrimarySoft)
                            .clickable {
                                clipboard.setText(AnnotatedString(phoneVal))
                                vm.toast("已复制电话 $phoneVal")
                            }.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("复制电话", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(if (fieldsOpen) "upload" else "download", 17.dp, Primary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (fieldsOpen) "收起" else "全部字段",
                        fontSize = 10.sp, color = Primary, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // 字段：默认折叠，点上面的摘要卡展开
            if (fieldsOpen) {
                MCard(Modifier.fillMaxWidth()) {
                    val fields = lib.fields
                    fields.forEachIndexed { i, f ->
                        val raw = if (f.type == FieldType.FORMULA) evalFormula(vm.state, lib, entry, f.expr ?: "") else entry.values[f.id]
                        val shown = when (f.type) {
                            FieldType.AUTONUMBER -> (f.prefix ?: "") + (raw ?: "")
                            FieldType.CHECKBOX -> if (raw == "1") "是" else "否"
                            FieldType.PERCENT -> if (raw.isNullOrBlank()) "" else raw + (f.unit ?: "%")
                            else -> (raw ?: "") + (f.unit ?: "")
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.go(Scr.Editor(lib.id, entry.id)) }
                                .padding(horizontal = 15.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(f.name, fontSize = 11.5.sp, color = Muted2, modifier = Modifier.width(82.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                shown.ifBlank { "—" },
                                fontSize = 13.5.sp,
                                color = if (shown.isBlank()) Muted2 else TextMain,
                                fontWeight = if (f.id == lib.fields.firstOrNull()?.id) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (i != fields.lastIndex) DividerLine()
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (family != null) {
                Spacer(Modifier.height(14.dp))
                Text("家庭组", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(Modifier.height(6.dp))
                MCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(family.display, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
                            Text("${family.people} 人 · ${family.visits} 次到店 · 合计 ¥${String.format("%.2f", family.total)}", fontSize = 11.sp, color = Muted2)
                        }
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(TealSoft)
                                .clickable { mergeOpen = true }.padding(horizontal = 11.dp, vertical = 6.dp)
                        ) { Text("合并", fontSize = 11.5.sp, color = Teal, fontWeight = FontWeight.SemiBold) }
                    }
                    DividerLine()
                    family.members.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.openDetail(lib.id, m.entry.id) }
                                .padding(horizontal = 15.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(m.name, fontSize = 13.sp, color = TextMain, modifier = Modifier.weight(1f))
                            Text(stampOf(lib, m.entry), fontSize = 10.5.sp, color = Muted2)
                            AppIcon("chevron", 15.dp, Muted2)
                        }
                    }
                }
                Text("同电话不同名会自动归为家庭组；点「合并」可以把他们并成同一个人。", fontSize = 10.5.sp, color = Muted2, modifier = Modifier.padding(top = 5.dp, start = 2.dp))
            }

            if (history.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("验光历史", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(Modifier.width(6.dp))
                    Text("${history.size} 次", fontSize = 10.5.sp, color = Muted2)
                }
                Spacer(Modifier.height(6.dp))
                TrendChart(lib, history)
                history.forEachIndexed { i, item ->
                    val prev = history.getOrNull(i + 1)
                    HistoryCard(vm, lib, item, prev?.entry, onOpen = { vm.openDetail(lib.id, it) })
                }
            }

            if (picks.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("配镜出库", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                    Spacer(Modifier.width(6.dp))
                    Text("本条记录扣减的镜片", fontSize = 10.5.sp, color = Muted2)
                }
                Spacer(Modifier.height(6.dp))
                MCard(Modifier.fillMaxWidth()) {
                    picks.forEachIndexed { i, p ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable(enabled = p.found) { vm.openDetail(p.srcLibId, p.srcEntryId) }
                                .padding(horizontal = 15.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                val title = (p.model + " " + p.spec).trim()
                                Text(title.ifBlank { p.fieldName }, fontSize = 13.sp, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (p.found) "${p.fieldName} · 库存 ${p.stock}" else "${p.fieldName} · 库存条目已删除",
                                    fontSize = 10.5.sp, color = if (p.found) Muted2 else Danger
                                )
                            }
                            Text("×${p.qty}", fontSize = 13.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                            if (p.found) {
                                Spacer(Modifier.width(6.dp))
                                AppIcon("chevron", 15.dp, Muted2)
                            }
                        }
                        if (i != picks.lastIndex) DividerLine()
                    }
                }
            }

            if (related.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("在其他库里的记录", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(Modifier.height(6.dp))
                MCard(Modifier.fillMaxWidth()) {
                    related.forEach { (l, e) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.openDetail(l.id, e.id) }
                                .padding(horizontal = 15.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Tile(l.icon, hexColor(l.color), 30.dp, 15.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(titleOf(l, e), fontSize = 13.sp, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(l.name, fontSize = 10.5.sp, color = Muted2)
                            }
                            AppIcon("chevron", 15.dp, Muted2)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "删除这条记录",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Danger,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)).background(DangerSoft)
                    .clickable { confirmDelete = true }.padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "删除后不会自动把镜片库存加回去。",
                fontSize = 10.5.sp, color = Muted2, modifier = Modifier.padding(start = 2.dp, top = 5.dp)
            )
            Spacer(Modifier.height(30.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条记录？") },
            text = { Text("删除后无法恢复，也不会回补镜片库存。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteEntry(lib.id, entry.id)
                    vm.back()
                }) { Text("删除", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }

    if (mergeOpen && family != null) {
        MergeSheet(vm, lib, entry, family, onClose = { mergeOpen = false })
    }
}

private data class TrendSum(val from: Double, val to: Double, val diff: Double)

private fun trendSum(se: List<Double?>): TrendSum? {
    val from = se.firstOrNull { it != null } ?: return null
    val to = se.lastOrNull { it != null } ?: return null
    // 与度数高低一致：近视更负、远视更正，都算「加深」
    return TrendSum(from, to, round2(abs(to) - abs(from)))
}

@Composable
private fun TrendChart(lib: MLibrary, history: List<HistItem>) {
    val odF = lib.fields.firstOrNull { it.name.trim().equals("OD", true) }
    val osF = lib.fields.firstOrNull { it.name.trim().equals("OS", true) }
    if (odF == null && osF == null) return
    val items = history.reversed()
    val odSe = items.map { e -> odF?.let { seOf(e.entry.values[it.id]) } }
    val osSe = items.map { e -> osF?.let { seOf(e.entry.values[it.id]) } }
    // 画图用「度数高低」：SE 绝对值越大 = 度数越高，向上就是变高
    val od = odSe.map { it?.let { v -> abs(v) } }
    val os = osSe.map { it?.let { v -> abs(v) } }
    val values = (od + os).filterNotNull()
    if (values.size < 2) return
    var lo = values.min()
    var hi = values.max()
    if (hi - lo < 1.0) {
        lo -= 0.5
        hi += 0.5
    }
    val pad = (hi - lo) * 0.18
    lo -= pad
    hi += pad
    val span = (hi - lo).toFloat()
    val odColor = Color(0xFFF0763C)
    val osColor = Color(0xFF2F7FE0)
    val odSum = trendSum(odSe)
    val osSum = trendSum(osSe)

    MCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("度数趋势", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(Modifier.width(6.dp))
                Text("等效球镜 · 向上 = 度数变高", fontSize = 9.5.sp, color = Muted2)
                Spacer(Modifier.weight(1f))
                if (odF != null) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(odColor))
                    Spacer(Modifier.width(4.dp))
                    Text("OD 右眼", fontSize = 10.sp, color = odColor, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                }
                if (osF != null) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(osColor))
                    Spacer(Modifier.width(4.dp))
                    Text("OS 左眼", fontSize = 10.sp, color = osColor, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Canvas(Modifier.fillMaxWidth().height(128.dp)) {
                val left = 8.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val top = 12.dp.toPx()
                val bottom = size.height - 10.dp.toPx()
                fun yOf(v: Double) = bottom - ((v - lo).toFloat() / span) * (bottom - top)
                fun xOf(i: Int) =
                    if (items.size <= 1) (left + right) / 2f
                    else left + (right - left) * i / (items.size - 1).toFloat()

                for (k in 0..2) {
                    val gy = top + (bottom - top) * k / 2f
                    drawLine(Line, Offset(left, gy), Offset(right, gy), 1f)
                }
                fun series(data: List<Double?>, color: Color) {
                    var prev: Offset? = null
                    data.forEachIndexed { i, v ->
                        if (v == null) {
                            prev = null
                            return@forEachIndexed
                        }
                        val p = Offset(xOf(i), yOf(v))
                        prev?.let {
                            drawLine(color, it, p, 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                        drawCircle(Color.White, 4.2.dp.toPx(), p)
                        drawCircle(color, 3.2.dp.toPx(), p)
                        prev = p
                    }
                }
                if (odF != null) series(od, odColor)
                if (osF != null) series(os, osColor)

                // 首尾两个点标上具体度数，OD 在线上方、OS 在线下方
                val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 9.5.sp.toPx()
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                fun labels(raw: List<Double?>, strength: List<Double?>, color: Color, above: Boolean) {
                    val first = strength.indexOfFirst { it != null }
                    val last = strength.indexOfLast { it != null }
                    if (first < 0 || last < 0) return
                    listOf(first, last).distinct().forEach { i ->
                        val v = strength[i] ?: return@forEach
                        val se = raw[i] ?: return@forEach
                        val x = xOf(i)
                        val y = yOf(v)
                        val align = when {
                            first == last -> if (x < (left + right) / 2f) android.graphics.Paint.Align.LEFT else android.graphics.Paint.Align.RIGHT
                            i == first -> android.graphics.Paint.Align.LEFT
                            else -> android.graphics.Paint.Align.RIGHT
                        }
                        labelPaint.textAlign = align
                        labelPaint.color = color.toArgb()
                        val tx = when (align) {
                            android.graphics.Paint.Align.LEFT -> x + 4.dp.toPx()
                            android.graphics.Paint.Align.RIGHT -> x - 4.dp.toPx()
                            else -> x
                        }
                        val ty = if (above) (y - 7.dp.toPx()).coerceAtLeast(labelPaint.textSize)
                        else (y + 14.dp.toPx()).coerceAtMost(size.height - 2.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(fmtPower(se), tx, ty, labelPaint)
                    }
                }
                if (odF != null) labels(odSe, od, odColor, true)
                if (osF != null) labels(osSe, os, osColor, false)
            }
            Spacer(Modifier.height(6.dp))
            if (odSum != null) TrendLine("OD 右眼", odSum, odColor)
            if (osSum != null) TrendLine("OS 左眼", osSum, osColor)
            Text("上面是第一次和最近一次验光的对比", fontSize = 9.sp, color = Muted2, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(4.dp))
            Row {
                Text(fmtDay(entryDate(lib, items.first().entry)), fontSize = 9.5.sp, color = Muted2)
                Spacer(Modifier.weight(1f))
                Text(fmtDay(entryDate(lib, items.last().entry)), fontSize = 9.5.sp, color = Muted2)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun TrendLine(label: String, t: TrendSum, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(
            "$label  ${fmtPower(t.from)} → ${fmtPower(t.to)}",
            fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextMain
        )
        Spacer(Modifier.width(8.dp))
        val desc = when {
            t.diff > 0.001 -> "加深 ${String.format("%.2f", t.diff)}D"
            t.diff < -0.001 -> "降低 ${String.format("%.2f", -t.diff)}D"
            else -> "持平"
        }
        Text(desc, fontSize = 9.5.sp, color = if (t.diff > 0.001) color else Muted2, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HistoryCard(vm: AppViewModel, lib: MLibrary, item: HistItem, prev: MEntry?, onOpen: (String) -> Unit) {
    val e = item.entry
    val dateField = dateFieldOf(lib)
    val use = lib.fields.firstOrNull { it.name.contains("用途") }
    val od = lib.fields.firstOrNull { Regex("^OD$", RegexOption.IGNORE_CASE).matches(it.name) }
    val os = lib.fields.firstOrNull { Regex("^OS$", RegexOption.IGNORE_CASE).matches(it.name) }
    val total = lib.fields.firstOrNull { Regex("合计|金额|价格").containsMatchIn(it.name) }
    val no = lib.fields.firstOrNull { Regex("^NO\\.?$").matches(it.name) }
    val lens = lib.fields.firstOrNull { it.name.contains("镜片") }
    val frame = lib.fields.firstOrNull { it.name.contains("镜框") || it.name.contains("镜架") }
    fun v(f: MField?): String = if (f == null) "" else oneLine(e.values[f.id])
    val badges = if (prev != null) deltaBadges(lib, e, prev) else emptyList()

    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (item.current) Surface else Surface.copy(alpha = 0.7f))
            .border(1.dp, if (item.current) Primary.copy(alpha = 0.4f) else Line, RoundedCornerShape(16.dp))
            .clickable(enabled = !item.current) { onOpen(e.id) }
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                v(dateField).ifBlank { fmtDay(entryDate(lib, e)) },
                fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextMain
            )
            if (item.current) {
                Spacer(Modifier.width(6.dp))
                Text("当前", fontSize = 9.5.sp, color = Primary, modifier = Modifier
                    .clip(RoundedCornerShape(50)).background(PrimarySoft).padding(horizontal = 6.dp, vertical = 1.dp))
            }
            if (item.dup > 1) {
                Spacer(Modifier.width(6.dp))
                Text("同记录 ×${item.dup}", fontSize = 9.5.sp, color = Muted2)
            }
            Spacer(Modifier.weight(1f))
            if (v(use).isNotBlank()) Text(v(use), fontSize = 9.5.sp, color = Muted)
            if (!item.current) AppIcon("chevron", 14.dp, Muted2)
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (v(od).isNotBlank()) {
                Text("OD ", fontSize = 10.5.sp, color = Muted2)
                Text(v(od), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(Modifier.width(14.dp))
            }
            if (v(os).isNotBlank()) {
                Text("OS ", fontSize = 10.5.sp, color = Muted2)
                Text(v(os), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = TextMain)
            }
            if (v(od).isBlank() && v(os).isBlank()) {
                Text(subtitleOf(lib, e).ifBlank { "无度数记录" }, fontSize = 12.sp, color = Muted2)
            }
        }
        val meta = listOfNotNull(
            v(total).takeIf { it.isNotBlank() }?.let { "合计 ¥$it" },
            v(lens).takeIf { it.isNotBlank() },
            v(frame).takeIf { it.isNotBlank() },
            v(no).takeIf { it.isNotBlank() }?.let { "NO. $it" }
        )
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            Text(meta.joinToString(" · "), fontSize = 10.5.sp, color = Muted2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (badges.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                badges.forEach { d ->
                    val c = if (d.deeper) Danger else Teal
                    Text(
                        "${d.label} ${if (d.deeper) "加深" else "回退"} ${String.format("%.2f", d.amount)}D",
                        fontSize = 9.5.sp, color = c, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text("相比上一次", fontSize = 9.5.sp, color = Muted2, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MergeSheet(vm: AppViewModel, lib: MLibrary, entry: MEntry, family: FamilyInfo, onClose: () -> Unit) {
    val pf = phoneFieldOf(lib) ?: return
    val nf = nameFieldOf(lib) ?: return
    val phone = normPhone(entry.values[pf.id])
    val myName = nameOf(lib, entry)
    val counts = remember(entry.id, vm.rev) {
        val m = LinkedHashMap<String, Int>()
        lib.entries.forEach { e ->
            if (normPhone(e.values[pf.id]) != phone) return@forEach
            val n = nameOf(lib, e)
            if (n.isEmpty() || n == myName) return@forEach
            m[n] = (m[n] ?: 0) + 1
        }
        m.entries.sortedByDescending { it.value }
    }
    var target by remember { mutableStateOf(counts.firstOrNull()?.key ?: "") }
    var keepCurrent by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text("合并为同一个人", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("把所选姓名的记录改成「$myName」，之后会归到同一个人的验光历史里。只处理共用这个电话的记录。", fontSize = 11.5.sp, color = Muted2)
            Spacer(Modifier.height(14.dp))
            Text("要合并的姓名", fontSize = 11.5.sp, color = Muted)
            Spacer(Modifier.height(6.dp))
            counts.forEach { (n, c) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (target == n) PrimarySoft else Bg)
                        .border(1.dp, if (target == n) Primary else Line, RoundedCornerShape(12.dp))
                        .clickable { target = n }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(n, fontSize = 13.5.sp, color = TextMain, modifier = Modifier.weight(1f))
                    Text("$c 条记录", fontSize = 10.5.sp, color = Muted2)
                    RadioButton(selected = target == n, onClick = { target = n })
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("合并后保留哪个名字", fontSize = 11.5.sp, color = Muted)
            Row(Modifier.fillMaxWidth().clickable { keepCurrent = true }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = keepCurrent, onClick = { keepCurrent = true })
                Text("保留「$myName」", fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth().clickable { keepCurrent = false }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = !keepCurrent, onClick = { keepCurrent = false })
                Text("保留所选姓名", fontSize = 13.sp)
            }
            Spacer(Modifier.height(14.dp))
            PillButton("合并", onClick = {
                if (target.isNotBlank()) {
                    vm.mergeNames(lib.id, entry.id, target, keepCurrent)
                    onClose()
                }
            }, color = Teal)
        }
    }
}
