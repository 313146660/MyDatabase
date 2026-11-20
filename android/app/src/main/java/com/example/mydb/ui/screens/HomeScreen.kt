package com.example.mydb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.data.*
import com.example.mydb.ui.*
import com.example.mydb.ui.theme.*

@Composable
fun HomeScreen(vm: AppViewModel) {
    val rev = vm.rev
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val libs = vm.state.libraries
    val totalEntries = remember(rev) { libs.sumOf { it.entries.size } }
    val totalFields = remember(rev) { libs.sumOf { it.fields.size } }
    val lows = remember(rev) { lowStockAll(vm.state) }
    val lowCount = remember(lows) { lows.sumOf { it.second.size } }
    val zeroCount = remember(lows) { lows.sumOf { it.second.count { item -> item.qty <= 0 } } }

    val recent = remember(rev) {
        libs.flatMap { l -> l.entries.map { l to it } }
            .sortedByDescending { it.second.updatedAt }
            .take(8)
    }

    val filteredLibs = if (q.isEmpty()) libs else libs.filter { it.name.lowercase().contains(q) }
    val filteredRecent = remember(q, rev) {
        if (q.isEmpty()) recent else libs.flatMap { l ->
            l.entries.filter { entryMatches(l, it, q) }.map { l to it }
        }.sortedByDescending { it.second.updatedAt }.take(30)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF1B7FD6), Color(0xFF11589F))))
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("MYDATABASE", fontSize = 10.5.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.65f), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("我的数据库", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(todayCn(), fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) { Text("M", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                }
                Spacer(Modifier.height(14.dp))
                SearchBox(value = query, placeholder = "搜索库、条目或字段", onChange = { query = it })
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        if (lows.isNotEmpty()) {
            item {
                val first = lows.first()
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(DangerSoft)
                        .clickable {
                            vm.setView(first.first.id, "matrix")
                            vm.go(Scr.Library(first.first.id))
                        }
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon("bell", 18.dp, Danger)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "$lowCount 个规格库存偏低" + if (zeroCount > 0) " · $zeroCount 个已缺货" else "",
                            fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Danger
                        )
                        Text(
                            first.second.take(3).joinToString("、") { "${it.model} ${fmtPower(it.sph)}" + if (it.cyl != 0.0) "/${fmtPower(it.cyl)}" else "" } + if (first.second.size > 3) " 等" else "",
                            fontSize = 11.5.sp, color = Danger.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    AppIcon("chevron", 17.dp, Danger)
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
        }

        if (q.isEmpty()) {
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(Surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Stat(libs.size.toString(), "数据库", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(34.dp).background(Line))
                    Stat(totalEntries.toString(), "条记录", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(34.dp).background(Line))
                    Stat(totalFields.toString(), "字段", Modifier.weight(1f))
                }
            }
        }

        item {
            SectionHead(if (q.isEmpty()) "我的数据库" else "匹配的数据库") {
                Text(
                    "＋ 新建",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary,
                    modifier = Modifier.clickable { vm.go(Scr.LibForm(null)) }.padding(4.dp)
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val cards: List<MLibrary?> = filteredLibs + if (q.isEmpty()) listOf(null) else emptyList()
                cards.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { lib ->
                            if (lib == null) {
                                AddLibraryCard(Modifier.weight(1f)) { vm.go(Scr.LibForm(null)) }
                            } else {
                                LibraryCard(lib, Modifier.weight(1f)) {
                                    if (lib.matrix != null) vm.setView(lib.id, lib.view ?: "matrix")
                                    vm.go(Scr.Library(lib.id))
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            SectionHead(if (q.isEmpty()) "最近更新" else "匹配的条目") {
                Text("${filteredRecent.size} 条", fontSize = 11.sp, color = Muted2)
            }
        }

        if (filteredRecent.isEmpty()) {
            item { EmptyBlock("search", "没有找到结果", "换个关键词试试，或者新建一个数据库") }
        } else {
            items(filteredRecent, key = { it.first.id + ":" + it.second.id }) { (lib, entry) ->
                RecentRow(vm, lib, entry)
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Text(label, fontSize = 10.5.sp, color = Muted2)
    }
}

@Composable
private fun LibraryCard(lib: MLibrary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = hexColor(lib.color)
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Surface).clickable(onClick = onClick)
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tile(lib.icon, c, 38.dp, 19.dp)
            Spacer(Modifier.weight(1f))
            Text(lib.entries.size.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
        }
        Spacer(Modifier.height(11.dp))
        Text(lib.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${lib.fields.size} 个字段" + if (lib.entries.isNotEmpty()) " · " + relTime(lib.entries.maxOf { it.updatedAt }) else "",
            fontSize = 10.5.sp, color = Muted2, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddLibraryCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(Surface.copy(alpha = 0.6f))
            .clickable(onClick = onClick).padding(13.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Bg),
            contentAlignment = Alignment.Center
        ) { AppIcon("plus", 19.dp, Muted) }
        Spacer(Modifier.height(11.dp))
        Text("新建数据库", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Muted)
        Text("自定义字段和视图", fontSize = 10.5.sp, color = Muted2)
    }
}

@Composable
private fun RecentRow(vm: AppViewModel, lib: MLibrary, entry: MEntry) {
    val rev = vm.rev
    val name = titleOf(lib, entry)
    val sub = subtitleOf(lib, entry)
    Row(
        Modifier.fillMaxWidth().clickable { vm.go(Scr.Detail(lib.id, entry.id)) }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(name, hexColor(lib.color), 38.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                lib.name + if (sub.isNotBlank()) " · $sub" else "",
                fontSize = 11.sp, color = Muted2, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(relTime(entry.updatedAt), fontSize = 10.5.sp, color = Muted2)
    }
}
