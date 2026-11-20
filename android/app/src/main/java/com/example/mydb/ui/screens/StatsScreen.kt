package com.example.mydb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.data.CustomerStats
import com.example.mydb.data.customerStats
import com.example.mydb.ui.AppViewModel
import com.example.mydb.ui.SectionHead
import com.example.mydb.ui.TopBar
import com.example.mydb.ui.hexColor
import com.example.mydb.ui.theme.Bg
import com.example.mydb.ui.theme.Danger
import com.example.mydb.ui.theme.Muted2
import com.example.mydb.ui.theme.Primary
import com.example.mydb.ui.theme.Surface
import com.example.mydb.ui.theme.TextMain
import com.example.mydb.ui.theme.soft

private fun moneyStr(v: Double): String =
    if (v == v.toInt().toDouble()) v.toInt().toString() else v.toString()

@Composable
fun StatsScreen(vm: AppViewModel, libId: String) {
    val lib = vm.state.libraries.firstOrNull { it.id == libId }
    val dataKey = lib?.let { it.entries.size * 31L + (it.entries.maxOfOrNull { e -> e.updatedAt } ?: 0L) } ?: 0L
    val stats = remember(libId, dataKey) { lib?.let { customerStats(it) } }
    val color = if (lib != null) hexColor(lib.color) else Primary

    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar("客户统计", subtitle = lib?.name, onBack = { vm.back() })
        if (lib == null || stats == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无数据", color = Muted2, fontSize = 13.sp)
            }
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item { OverviewGrid(stats!!) }
            item { SectionHead("近 12 个月营收") }
            item { RevenueChart(stats.monthlyRevenue, color) }
            item { SectionHead("热销镜片 Top 10") }
            item { BarList(stats.topLens, color) }
            item { SectionHead("用途分布") }
            item { BarList(stats.useDist, color) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun OverviewGrid(s: CustomerStats) {
    val cards = listOf(
        "总记录" to s.records.toString(),
        "总客户" to s.customers.toString(),
        "总营收" to "¥${moneyStr(s.totalRevenue)}",
        "客单价" to "¥${moneyStr(s.avgOrder)}",
        "本月新增" to s.thisMonthNew.toString(),
        "复购率" to "${s.repeatRate}%",
        "超期客户" to s.overdueCount.toString()
    )
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        cards.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    val c = if (label == "超期客户") Danger else if (label.startsWith("总")) Primary else Muted2
                    StatCard(label, value, c, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Surface).padding(14.dp).fillMaxWidth()
    ) {
        Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.5.sp, color = Muted2)
    }
}

@Composable
private fun RevenueChart(data: List<Pair<String, Double>>, color: Color) {
    val max = data.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    Row(
        Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        data.forEach { (label, value) ->
            val ratio = (value / max).toFloat().coerceIn(0f, 1f)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    if (value > 0) moneyStr(value) else "",
                    fontSize = 8.sp, color = Muted2, maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .height((ratio * 100).dp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(color.copy(alpha = if (value > 0) 0.8f else 0.12f))
                )
                Spacer(Modifier.height(3.dp))
                Text(label.substringAfter("/"), fontSize = 8.sp, color = Muted2)
            }
        }
    }
}

@Composable
private fun BarList(items: List<Pair<String, Int>>, color: Color) {
    if (items.isEmpty()) {
        Text("暂无数据", Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Muted2, fontSize = 12.sp)
        return
    }
    val max = items.maxOf { it.second }.coerceAtLeast(1)
    Column(Modifier.padding(horizontal = 16.dp)) {
        items.forEach { (name, cnt) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name, fontSize = 11.5.sp, color = TextMain,
                    modifier = Modifier.width(130.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.weight(1f).height(13.dp)
                        .clip(RoundedCornerShape(6.dp)).background(soft(TextMain))
                ) {
                    Box(
                        Modifier.fillMaxHeight()
                            .fillMaxWidth((cnt.toFloat() / max).coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.75f))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("$cnt", fontSize = 11.sp, color = Muted2, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
