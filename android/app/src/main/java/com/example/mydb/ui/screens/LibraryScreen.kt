package com.example.mydb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.data.*
import com.example.mydb.ui.*
import com.example.mydb.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(vm: AppViewModel, libId: String) {
    val rev = vm.rev
    val lib = vm.lib(libId)
    if (lib == null) {
        EmptyBlock("database", "数据库不存在", "它可能已经被删除")
        return
    }
    var menu by remember { mutableStateOf(false) }
    var query by rememberSaveable(libId) { mutableStateOf("") }
    var filter by rememberSaveable(libId) { mutableStateOf("all") }
    var limit by rememberSaveable(libId) { mutableStateOf(80) }
    val matrixMode = lib.matrix != null && (lib.view ?: "list") == "matrix"

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
            title = lib.name,
            subtitle = "${lib.entries.size} 条记录 · ${lib.fields.size} 个字段",
            onBack = { vm.back() },
            trailing = {
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "设置", tint = TextMain)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("编辑数据库") }, onClick = {
                            menu = false; vm.go(Scr.LibForm(lib.id))
                        })
                        DropdownMenuItem(text = { Text("字段管理") }, onClick = {
                            menu = false; vm.go(Scr.Fields(lib.id))
                        })
                        DropdownMenuItem(text = { Text("列表显示字段") }, onClick = {
                            menu = false; vm.go(Scr.ListFields(lib.id))
                        })
                        if (lib.alerts != null) {
                            DropdownMenuItem(text = { Text("库存提醒规则") }, onClick = {
                                menu = false; vm.go(Scr.Alerts(lib.id))
                            })
                            DropdownMenuItem(text = { Text("补货清单") }, onClick = {
                                menu = false; vm.go(Scr.Restock(lib.id))
                            })
                        }
                        DropdownMenuItem(text = { Text("CSV 导入 / 导出") }, onClick = {
                            menu = false; vm.go(Scr.Csv(lib.id))
                        })
                    }
                }
            }
        )

        if (lib.matrix != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                SegButton("列表", !matrixMode, Modifier.weight(1f)) { vm.setView(lib.id, "list") }
                Spacer(Modifier.width(6.dp))
                SegButton("表格", matrixMode, Modifier.weight(1f)) { vm.setView(lib.id, "matrix") }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (matrixMode) {
            MatrixContent(vm, lib)
        } else {
            ListContent(vm, lib, query, { query = it }, filter, { filter = it }, limit, { limit += 120 }, rev)
        }
        } // Column

        FloatingActionButton(
            onClick = { vm.go(Scr.Editor(lib.id, null)) },
            containerColor = hexColor(lib.color),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 24.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新增记录")
        }
    }
}

@Composable
private fun SegButton(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(if (active) Surface else Color.Transparent)
            .border(1.dp, if (active) Line else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (active) TextMain else Muted2)
    }
}

/* ---------------- 列表 ---------------- */

@Composable
private fun ListContent(
    vm: AppViewModel,
    lib: MLibrary,
    query: String,
    onQuery: (String) -> Unit,
    filter: String,
    onFilter: (String) -> Unit,
    limit: Int,
    onMore: () -> Unit,
    rev: Int
) {
    val color = hexColor(lib.color)
    val phoneField = phoneFieldOf(lib)
    val nameField = nameFieldOf(lib)

    val phoneNames = remember(lib.id, rev) {
        val map = HashMap<String, MutableSet<String>>()
        if (phoneField != null && nameField != null) {
            lib.entries.forEach { e ->
                val p = normPhone(e.values[phoneField.id])
                if (p.isEmpty()) return@forEach
                val n = nameOf(lib, e)
                if (n.isNotEmpty()) map.getOrPut(p) { mutableSetOf() }.add(n)
            }
        }
        map
    }
    fun isFamily(e: MEntry): Boolean {
        if (phoneField == null || nameField == null) return false
        val set = phoneNames[normPhone(e.values[phoneField.id])]
        return set != null && set.size > 1
    }
    val familyCount = remember(lib.id, rev) { lib.entries.count { isFamily(it) } }
    val favCount = lib.entries.count { it.favorite }

    val sort = lib.sort ?: "visit_desc"
    val sortOptions = listOf(
        "visit_desc" to "最近验光",
        "updated_desc" to "最近修改",
        "visit_asc" to "最早验光",
        "name_asc" to "名称 A→Z",
        "name_desc" to "名称 Z→A"
    )
    val sortLabel = sortOptions.firstOrNull { it.first == sort }?.second ?: "最近验光"
    val collator = remember { java.text.Collator.getInstance(java.util.Locale.CHINA) }
    val filtered = lib.entries.filter { entryMatches(lib, it, query) }
        .let { list -> if (filter == "fav") list.filter { it.favorite } else if (filter == "family") list.filter { isFamily(it) } else list }
    val all = when (sort) {
        "updated_desc" -> filtered.sortedByDescending { it.updatedAt }
        "visit_asc" -> filtered.sortedWith(compareBy<MEntry> { visitDate(lib, it) ?: Long.MAX_VALUE }.thenByDescending { it.updatedAt })
        "name_asc" -> filtered.sortedWith { a, b -> collator.compare(titleOf(lib, a), titleOf(lib, b)) }
        "name_desc" -> filtered.sortedWith { a, b -> collator.compare(titleOf(lib, b), titleOf(lib, a)) }
        else -> filtered.sortedWith(compareByDescending<MEntry> { visitDate(lib, it) ?: 0L }.thenByDescending { it.updatedAt })
    }
    val history = vm.searchHistory[lib.id].orEmpty()
    var sortMenu by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color)))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { AppIcon(lib.icon, 21.dp, Color.White) }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text(lib.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${lib.entries.size} 条记录，最近一次 ${latestStamp(lib)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.82f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    Pill("${lib.fields.size} 个字段")
                    Spacer(Modifier.width(8.dp))
                    Pill("${favCount} 个收藏")
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                SearchBox(
                    query, "在「${lib.name}」中搜索", onQuery,
                    onSubmit = { vm.addSearchQuery(lib.id, query) }
                )
                if (query.isBlank() && history.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索历史", fontSize = 10.5.sp, color = Muted2, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "清空", fontSize = 10.5.sp, color = Primary,
                            modifier = Modifier.clickable { vm.clearSearchHistory(lib.id) }.padding(4.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        history.forEach { h ->
                            TagChip(h, false, "search") {
                                onQuery(h)
                                vm.addSearchQuery(lib.id, h)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box {
                    TagChip("排序 · $sortLabel", true, "sliders") { sortMenu = true }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        sortOptions.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                sortMenu = false
                                vm.setSort(lib.id, key)
                            })
                        }
                    }
                }
                TagChip("全部 ${lib.entries.size}", filter == "all") { onFilter("all") }
                TagChip("收藏 $favCount", filter == "fav") { onFilter("fav") }
                if (familyCount > 0) TagChip("家庭 $familyCount", filter == "family", icon = "users") { onFilter("family") }
            }
        }
        item { Spacer(Modifier.height(6.dp)) }

        if (all.isEmpty()) {
            item { EmptyBlock("database", "还没有记录", "点右下角的 + 添加第一条记录") }
        } else {
            val visible = all.take(limit)
            items(visible, key = { it.id }) { entry ->
                EntryRow(vm, lib, entry, isFamily(entry)) {
                    if (query.isNotBlank()) vm.addSearchQuery(lib.id, query)
                    vm.go(Scr.Detail(lib.id, entry.id))
                }
            }
            if (all.size > visible.size) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp)
                            .clip(RoundedCornerShape(14.dp)).background(Surface)
                            .clickable(onClick = onMore).padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("显示更多（还有 ${all.size - visible.size} 条）", fontSize = 12.5.sp, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Text("共 ${all.size} 条记录", Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center, fontSize = 10.5.sp, color = Muted2)
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun Pill(text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 10.5.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EntryRow(vm: AppViewModel, lib: MLibrary, entry: MEntry, family: Boolean, onClick: () -> Unit) {
    val rev = vm.rev
    val lf = listFieldsOf(lib)
    val cfg = matrixCfg(lib)
    val head = lf.firstOrNull()
    var title = head?.let { cellTextOf(vm.state, lib, entry, it) }?.takeIf { it.isNotBlank() } ?: titleOf(lib, entry)
    if (cfg != null && head?.id == cfg.group.id) {
        val sph = toPower(entry.values[cfg.row.id])
        val cyl = cylValueOf(entry, cfg)
        title += " ${fmtPower(sph)}" + if (cyl != 0.0) " / ${fmtPower(cyl)}" else ""
    }
    val (titleSize, metaSize) = listFontSizes(lib)
    Row(
        Modifier.fillMaxWidth().background(Surface).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = titleSize, fontWeight = FontWeight.SemiBold, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (family) {
                    Spacer(Modifier.width(5.dp))
                    Text("家庭", fontSize = 9.5.sp, color = Teal, modifier = Modifier
                        .clip(RoundedCornerShape(50)).background(TealSoft).padding(horizontal = 5.dp, vertical = 1.dp))
                }
                if (entry.favorite) {
                    Spacer(Modifier.width(5.dp))
                    Text("★", fontSize = 10.sp, color = Color(0xFFE0A100))
                }
            }
            if (lf.size > 1) {
                Spacer(Modifier.height(3.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    lf.drop(1).forEach { f ->
                        val v = cellTextOf(vm.state, lib, entry, f)
                        Text(
                            buildAnnotatedString {
                                if (lib.listShowNames) {
                                    withStyle(SpanStyle(color = Muted2)) { append(f.name) }
                                    append(" ")
                                }
                                withStyle(SpanStyle(color = if (v.isBlank()) Muted else TextMain)) {
                                    append(v.ifBlank { "—" })
                                }
                            },
                            fontSize = metaSize,
                            lineHeight = metaSize * 1.3f,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(stampOf(lib, entry), fontSize = 10.5.sp, color = Muted2)
        AppIcon("chevron", 16.dp, Muted2)
    }
    Box(Modifier.padding(horizontal = 16.dp).height(1.dp).background(Line))
}

fun listFontTitleSp(lib: MLibrary): Float {
    lib.listFontSize?.let { return it.toFloat().coerceIn(8f, 30f) }
    return when (lib.listFont) {
        "s" -> 12.5f
        "l" -> 15.5f
        "xl" -> 17f
        "m" -> 14f
        else -> AppPrefs.listFontSize.toFloat()
    }
}

fun listFontSizes(lib: MLibrary): Pair<androidx.compose.ui.unit.TextUnit, androidx.compose.ui.unit.TextUnit> {
    val t = listFontTitleSp(lib)
    return t.sp to (t * 0.86f).sp
}

fun stampOf(lib: MLibrary, entry: MEntry): String {
    val ts = visitDate(lib, entry)
    return if (ts == null) relTime(entry.updatedAt) else {
        val d = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        if (d.get(java.util.Calendar.YEAR) == java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
            "${d.get(java.util.Calendar.MONTH) + 1}/${d.get(java.util.Calendar.DAY_OF_MONTH)}"
        else fmtDay(ts)
    }
}

/* ---------------- 表格 ---------------- */

private fun powerAxis(values: Collection<Double>, isCyl: Boolean = false): List<Double> {
    val arr = values.sortedWith(compareBy({ abs(it) }, { it }))
    if (arr.isEmpty()) return emptyList()
    val step = 0.25
    if (isCyl && arr.contains(0.0)) {
        val out = mutableListOf(0.0)
        val rest = arr.filter { it != 0.0 }
        if (rest.isEmpty()) return out
        val maxMag = rest.maxOf { abs(it) }
        var m = 0.5
        while (m <= maxMag + 1e-9) {
            out.add(Math.round(-m * 100) / 100.0)
            m += step
        }
        return out
    }
    val min = arr.min()
    val max = arr.max()
    val start = arr.first()
    val dir = if (start <= 0) -1 else 1
    val out = mutableListOf<Double>()
    var v = start
    while (if (dir < 0) v >= min - 1e-9 else v <= max + 1e-9) {
        out.add(Math.round(v * 100) / 100.0)
        v += dir * step
    }
    return out
}

@Composable
private fun MatrixContent(vm: AppViewModel, lib: MLibrary) {
    val rev = vm.rev
    val data = matrixData(lib) ?: return
    val names = data.models.keys.toList()
    if (names.isEmpty()) {
        EmptyBlock("box", "还没有库存数据", "用 CSV 导入，或新增一条记录")
        return
    }
    var selected by remember(lib.id) { mutableStateOf(names.first()) }
    val model = if (selected in names) selected else names.first()
    val g = data.models[model]!!
    val range = lib.matrix?.ranges?.get(model)
    val sphs = if (range != null && range.sph.isNotEmpty()) powerAxis(range.sph.mapNotNull { toPower(it) }) else powerAxis(g.sphs)
    val cyls = if (range != null && range.cyl.isNotEmpty()) powerAxis(range.cyl.mapNotNull { toPower(it) }, true) else powerAxis(g.cyls, true)
    val al = alertsOf(lib)
    val lows = lowStockOf(lib)
    val totals = remember(lib.id, rev) { data.models.mapValues { it.value.cells.values.sum() } }
    val modelLows = remember(lib.id, rev) {
        val m = HashMap<String, Int>()
        data.models.forEach { (name, grp) ->
            var c = 0
            if (al != null) {
                grp.filled.forEach { key ->
                    val parts = key.split("|")
                    val s = (parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0) / 100.0
                    val cy = (parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0) / 100.0
                    if ((grp.cells[key] ?: 0.0) <= thresholdFor(al, name, s, cy)) c++
                }
            }
            m[name] = c
        }
        m
    }

    var sheet by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }
    var jumpTo by remember(lib.id) { mutableStateOf<Pair<Double, Double>?>(null) }
    var renameTarget by remember(lib.id) { mutableStateOf<String?>(null) }
    var batchOpen by remember(lib.id) { mutableStateOf(false) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    fun firstLowOf(name: String): Pair<Double, Double>? {
        val al2 = alertsOf(lib) ?: return null
        val grp = data.models[name] ?: return null
        var best: Pair<Double, Double>? = null
        var bestQty = Double.MAX_VALUE
        grp.filled.forEach { key ->
            val parts = key.split("|")
            val s = (parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0) / 100.0
            val cy = (parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0) / 100.0
            val q = grp.cells[key] ?: 0.0
            if (q <= thresholdFor(al2, name, s, cy) && q < bestQty) {
                bestQty = q
                best = s to cy
            }
        }
        return best
    }

    LaunchedEffect(jumpTo, model) {
        val target = jumpTo ?: return@LaunchedEffect
        val si = sphs.indexOfFirst { abs(it - target.first) < 0.001 }
        val ci = cyls.indexOfFirst { abs(it - target.second) < 0.001 }
        if (si >= 0 && ci >= 0) {
            val y = with(density) { (38.dp * (si + 1)).toPx() }
            val x = with(density) { (84.dp + 54.dp * ci - 40.dp).toPx() }
            vScroll.animateScrollTo(y.toInt())
            hScroll.animateScrollTo(x.toInt().coerceAtLeast(0))
        }
        jumpTo = null
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(names, key = { it }) { n ->
                val active = n == model
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) Teal else Surface)
                        .border(1.dp, if (active) Teal else Line, RoundedCornerShape(14.dp))
                        .combinedClickable(
                            onClick = {
                                selected = n
                                jumpTo = firstLowOf(n)
                            },
                            onLongClick = { renameTarget = n }
                        )
                        .padding(horizontal = 13.dp, vertical = 8.dp)
                ) {
                    Text(n, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else TextMain, maxLines = 1)
                    Text(
                        "${totals[n]?.toInt() ?: 0} ${data.cfg.value.unit ?: "片"}",
                        fontSize = 10.sp, color = if (active) Color.White.copy(alpha = 0.8f) else Muted2
                    )
                    val nLow = modelLows[n] ?: 0
                    if (nLow > 0) {
                        Text(
                            "$nLow 提醒",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else Danger
                        )
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("点格子改库存 · 左右滑动看更多柱镜", fontSize = 10.5.sp, color = Muted2, modifier = Modifier.weight(1f))
            if (vm.canUndoBatch) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(AmberSoft).border(1.dp, Amber.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .clickable {
                            val n = vm.undoBatch()
                            vm.toast("已撤销 $n 个规格的库存修改")
                        }.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon("refresh", 12.dp, Amber)
                    Spacer(Modifier.width(3.dp))
                    Text("撤销", fontSize = 10.5.sp, color = Amber, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(6.dp))
            }
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Surface).border(1.dp, Line, RoundedCornerShape(50))
                    .clickable { batchOpen = true }.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon("sliders", 12.dp, Muted)
                Spacer(Modifier.width(3.dp))
                Text("批量", fontSize = 10.5.sp, color = Muted, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(6.dp))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Surface).border(1.dp, Line, RoundedCornerShape(50))
                    .clickable { vm.go(Scr.Restock(lib.id)) }.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon("bell", 12.dp, if (lows.isEmpty()) Muted else Danger)
                if (lows.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    Text("提醒 ${lows.size}", fontSize = 10.5.sp, color = Danger, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(vScroll).horizontalScroll(hScroll)
        ) {
            // header
            Row {
                HeaderCell("球镜 \\ 柱镜", 84.dp)
                cyls.forEach { c -> HeaderCell(if (c == 0.0) "0" else fmtPower(c), 54.dp) }
                HeaderCell("合计", 54.dp, TealSoft, Teal)
            }
            sphs.forEach { s ->
                Row {
                    HeaderCell(fmtPower(s), 84.dp, Bg, TextMain)
                    var rowTotal = 0.0
                    cyls.forEach { c ->
                        val key = cellKey(s, c)
                        val qty = g.cells[key] ?: 0.0
                        rowTotal += qty
                        val filled = g.filled.contains(key)
                        val low = al != null && filled && qty <= thresholdFor(al, model, s, c)
                        MatrixCell(qty, low) { sheet = Triple(model, s, c) }
                    }
                    HeaderCell(if (rowTotal == 0.0) "·" else rowTotal.toInt().toString(), 54.dp, TealSoft.copy(alpha = 0.5f), Teal)
                }
            }
            Row {
                HeaderCell("合计", 84.dp, Bg, TextMain)
                cyls.forEach { c ->
                    val colTotal = sphs.sumOf { s -> g.cells[cellKey(s, c)] ?: 0.0 }
                    HeaderCell(if (colTotal == 0.0) "·" else colTotal.toInt().toString(), 54.dp, TealSoft.copy(alpha = 0.5f), Teal)
                }
                HeaderCell(g.cells.values.sum().toInt().toString(), 54.dp, Teal, Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("库存", fontSize = 10.sp, color = Muted2)
                Spacer(Modifier.width(6.dp))
                listOf(1, 2, 3, 4).forEach { n ->
                    Box(
                        Modifier.padding(end = 3.dp).size(13.dp).clip(RoundedCornerShape(4.dp))
                            .background(tealLevel(n.toDouble()))
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("颜色越深库存越多（4 ${data.cfg.value.unit ?: "片"}以上同档）", fontSize = 10.sp, color = Muted2)
            }
            if (al != null) {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(13.dp).clip(RoundedCornerShape(4.dp)).background(Surface).border(1.5.dp, Danger, RoundedCornerShape(4.dp)))
                    Spacer(Modifier.width(6.dp))
                    Text("红框 = 低于该型号的提醒阈值", fontSize = 10.sp, color = Muted2)
                }
            }
            Spacer(Modifier.height(84.dp))
        }
    }

    val target = sheet
    if (target != null) {
        StockSheet(
            vm = vm, lib = lib, model = target.first, sph = target.second, cyl = target.third,
            onClose = { sheet = null }
        )
    }

    val rt = renameTarget
    if (rt != null) {
        RenameModelDialog(vm, lib, rt, onClose = { renameTarget = null })
    }

    if (batchOpen) {
        BatchSheet(vm, lib, model, onClose = { batchOpen = false })
    }
}

@Composable
private fun RenameModelDialog(vm: AppViewModel, lib: MLibrary, old: String, onClose: () -> Unit) {
    var text by remember(old) { mutableStateOf(old) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("修改型号名称") },
        text = {
            Column {
                Text(
                    "「$old」下所有镜片会一起改名，提醒规则和表格范围也会跟着更新。",
                    fontSize = 12.sp, color = Muted
                )
                Spacer(Modifier.height(10.dp))
                AppTextField(text, { text = it }, placeholder = "例如：1.56 非球面绿膜")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (text.isBlank()) {
                    vm.toast("名称不能为空")
                } else {
                    val n = vm.renameModelTo(lib.id, old, text)
                    vm.toast("已改名，影响 $n 条镜片")
                    onClose()
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchSheet(vm: AppViewModel, lib: MLibrary, currentModel: String, onClose: () -> Unit) {
    var scopeAll by remember { mutableStateOf(false) }
    var sphMin by remember { mutableStateOf("") }
    var sphMax by remember { mutableStateOf("") }
    var cyl by remember { mutableStateOf("all") }
    var qty by remember { mutableStateOf("") }
    var presetDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    fun applyPreset(p: BatchPreset) {
        scopeAll = p.allModels
        sphMin = p.sphMin
        sphMax = p.sphMax
        cyl = p.cyl
        qty = p.qty
    }

    val builtins = remember(currentModel) {
        listOf(
            BatchPreset("本型号 -4.00~-1.00 → 4", false, "-4.00", "-1.00", "all", "4"),
            BatchPreset("本型号 有散光 → 2", false, "", "", "has", "2")
        )
    }
    val rawMin = toPower(sphMin)
    val rawMax = toPower(sphMax)
    val filter = BatchFilter(
        model = if (scopeAll) null else currentModel,
        sphMin = if (rawMin != null && rawMax != null) minOf(rawMin, rawMax) else rawMin,
        sphMax = if (rawMin != null && rawMax != null) maxOf(rawMin, rawMax) else rawMax,
        cyl = cyl
    )
    val count = vm.batchPreviewCount(lib.id, filter)
    val unit = lib.fields.firstOrNull { it.name.contains("库存") }?.unit ?: "片"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("批量改库存", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("按型号、球镜范围、散光筛选，把库存一次改成同一个值。", fontSize = 11.5.sp, color = Muted2)
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("预设", fontSize = 11.5.sp, color = Muted)
                Spacer(Modifier.weight(1f))
                Text(
                    "保存当前条件",
                    fontSize = 11.sp, color = Primary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        presetName = if (qty.isBlank()) "预设" else "库存 $qty"
                        presetDialog = true
                    }.padding(4.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                builtins.forEach { p ->
                    PresetChip(p.name, onClick = { applyPreset(p) })
                }
                vm.batchPresets.forEach { p ->
                    PresetChip(p.name, onClick = { applyPreset(p) }, onLongClick = { deleteTarget = p.name })
                }
            }
            Text("点预设套用；长按自定义预设可删除。", fontSize = 9.5.sp, color = Muted2)
            Spacer(Modifier.height(14.dp))

            Text("范围", fontSize = 11.5.sp, color = Muted)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TagChip("当前型号", !scopeAll) { scopeAll = false }
                TagChip("全部型号", scopeAll) { scopeAll = true }
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    Column {
                        Text("球镜从", fontSize = 11.5.sp, color = Muted)
                        Spacer(Modifier.height(4.dp))
                        AppTextField(sphMin, { sphMin = it }, placeholder = "-4.00", keyboardType = KeyboardType.Text)
                    }
                }
                Box(Modifier.weight(1f)) {
                    Column {
                        Text("球镜到", fontSize = 11.5.sp, color = Muted)
                        Spacer(Modifier.height(4.dp))
                        AppTextField(sphMax, { sphMax = it }, placeholder = "-1.00", keyboardType = KeyboardType.Text)
                    }
                }
            }
            Text("留空表示不限；可以带正负号，写反了会自动按大小处理。", fontSize = 9.5.sp, color = Muted2)
            Spacer(Modifier.height(12.dp))

            Text("散光", fontSize = 11.5.sp, color = Muted)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TagChip("不限", cyl == "all") { cyl = "all" }
                TagChip("无散光", cyl == "none") { cyl = "none" }
                TagChip("有散光", cyl == "has") { cyl = "has" }
            }
            Spacer(Modifier.height(14.dp))

            Text("库存改为（$unit）", fontSize = 11.5.sp, color = Muted)
            Spacer(Modifier.height(6.dp))
            AppTextField(qty, { t -> if (t.all { it.isDigit() }) qty = t }, placeholder = "例如 4；留空 = 清空", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(12.dp))

            Text(
                "将影响 $count 个规格",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) Danger else Muted2
            )
            Spacer(Modifier.height(12.dp))
            PillButton(
                if (count == 0) "没有匹配的规格" else "应用（$count 个）",
                onClick = {
                    if (count == 0) return@PillButton
                    val n = vm.batchStock(lib.id, filter, qty)
                    vm.toast(if (qty.isBlank()) "已清空 $n 个规格，可点顶部「撤销」" else "已把 $n 个规格改成 $qty $unit，可点顶部「撤销」")
                    onClose()
                },
                color = Teal
            )
        }
    }

    if (presetDialog) {
        AlertDialog(
            onDismissRequest = { presetDialog = false },
            title = { Text("保存为预设") },
            text = {
                Column {
                    Text("当前条件会保存成快捷按钮，下次一点就套用。", fontSize = 12.sp, color = Muted)
                    Spacer(Modifier.height(10.dp))
                    AppTextField(presetName, { presetName = it }, placeholder = "预设名称")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val nm = presetName.trim()
                    if (nm.isEmpty()) {
                        vm.toast("请填个名字")
                    } else {
                        vm.saveBatchPreset(
                            BatchPreset(
                                name = nm,
                                allModels = scopeAll,
                                sphMin = sphMin,
                                sphMax = sphMax,
                                cyl = cyl,
                                qty = qty
                            )
                        )
                        presetDialog = false
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { presetDialog = false }) { Text("取消") } }
        )
    }

    val del = deleteTarget
    if (del != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除预设「$del」？") },
            text = { Text("只删除这个快捷按钮，不影响库存。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    vm.deleteBatchPreset(del)
                }) { Text("删除", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun PresetChip(text: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Bg)
            .border(1.dp, Line, RoundedCornerShape(50))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 10.5.sp, color = TextMain)
    }
}

private fun tealLevel(qty: Double): Color {
    val n = qty.toInt()
    return when {
        n <= 0 -> Color.Transparent
        n >= 4 -> Teal.copy(alpha = 0.52f)
        else -> Teal.copy(alpha = 0.10f + 0.12f * n)
    }
}

@Composable
private fun HeaderCell(text: String, width: androidx.compose.ui.unit.Dp, bg: Color = Bg, fg: Color = Muted2) {
    Box(
        Modifier.width(width).height(38.dp).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 10.5.sp, color = fg, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MatrixCell(qty: Double, low: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.width(54.dp).height(38.dp)
            .border(1.dp, Line)
            .background(tealLevel(qty))
            .then(if (low) Modifier.border(1.5.dp, Danger) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (qty <= 0.0) "·" else qty.toInt().toString(),
            fontSize = 12.sp,
            fontWeight = if (qty > 0) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                qty.toInt() >= 4 -> Color.White
                qty > 0 -> if (Palette.dark) Color(0xFFD6FFF8) else Color(0xFF0B6E61)
                else -> Muted2
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockSheet(vm: AppViewModel, lib: MLibrary, model: String, sph: Double, cyl: Double, onClose: () -> Unit) {
    val rev = vm.rev
    val cfg = matrixCfg(lib) ?: return
    val existing = findSku(lib, model, sph, cyl)
    val initial = remember(model, sph, cyl, rev) { existing?.values?.get(cfg.value.id)?.trim() ?: "" }
    var text by remember(model, sph, cyl) { mutableStateOf(initial) }
    var showBlank by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 26.dp)) {
            Text(model, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text(
                "球镜 ${fmtPower(sph)}" + if (cyl != 0.0) " / 柱镜 ${fmtPower(cyl)}" else " · 单光",
                fontSize = 12.sp, color = Muted2
            )
            Spacer(Modifier.height(16.dp))
            FieldLabel("库存（${cfg.value.unit ?: "片"}）", "留空表示没有这个镜片，不计库存、不提醒；填 0 表示缺货，会提醒")
            Spacer(Modifier.height(6.dp))
            AppTextField(text, { t -> if (t.all { it.isDigit() }) text = t }, placeholder = "留空 / 0 / 1…", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(12.dp))
            Text("快速设置", fontSize = 11.sp, color = Muted2)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("0", "1", "2", "3", "4").forEach { n ->
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(Bg).border(1.dp, Line, RoundedCornerShape(11.dp))
                            .clickable { text = n }.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { Text(n, fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.SemiBold) }
                }
                Row(
                    Modifier.weight(1.4f).clip(RoundedCornerShape(11.dp)).background(Bg).border(1.dp, Line, RoundedCornerShape(11.dp))
                        .clickable { text = "" }.padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center
                ) { Text("留空", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(16.dp))
            PillButton("保存", onClick = {
                val msg = vm.mxSet(lib.id, model, sph, cyl, text)
                vm.toast(msg)
                onClose()
            }, color = Teal, filled = true)
        }
    }
}
