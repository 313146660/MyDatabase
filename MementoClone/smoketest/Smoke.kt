package smoke

import com.example.mementoclone.data.*
import org.json.JSONObject
import java.io.File

fun main() {
    // 数据层自测需要一个真实备份文件；不把任何个人数据提交进仓库。
    val path = System.getenv("MEMENTO_BACKUP") ?: "smoketest/backup.json"
    val file = File(path)
    if (!file.exists()) {
        println("SKIP: put a backup at smoketest/backup.json or set MEMENTO_BACKUP=/path/to/backup.json")
        return
    }
    val text = file.readText(Charsets.UTF_8)
    val t0 = System.currentTimeMillis()
    val s = parseBackup(text)
    println("parse_ms=" + (System.currentTimeMillis() - t0))
    println("libs=" + s.libraries.size)
    s.libraries.forEach { l ->
        println("  lib=" + l.name + " entries=" + l.entries.size + " fields=" + l.fields.size + " view=" + l.view)
    }

    val cust = s.libraries.first { it.name == "客户" }
    val lens = s.libraries.first { it.name.contains("镜片") }

    val md = matrixData(lens)
    if (md == null) {
        println("matrix=null")
    } else {
        println("matrix_models=" + md.models.size)
        println("lens_total=" + md.models.values.sumOf { g -> g.cells.values.sum() }.toInt())
    }
    println("lows=" + lowStockOf(lens).size)

    // 绿膜 -1.00/-1.00 应为 2
    val green = md?.models?.keys?.firstOrNull { it.contains("绿膜") }
    println("green_model=" + green)
    if (green != null) {
        val e = findSku(lens, green, -1.0, -1.0)
        println("green_-1_-1=" + (e?.values?.get("lf_stock") ?: "missing"))
    }

    val best = cust.entries.map { it to historyOf(cust, it) }.maxByOrNull { it.second.size }
    if (best != null) {
        println("max_history=" + best.second.size + " name=" + nameOf(cust, best.first))
        val badges = best.second.zipWithNext().flatMap { (a, b) -> deltaBadges(cust, a.entry, b.entry) }
        println("delta_badges=" + badges.size)
    }

    // 家庭组
    val fam = cust.entries.firstNotNullOfOrNull { familyGroup(cust, it) }
    println("family=" + (fam?.display ?: "none") + " people=" + (fam?.people ?: 0))

    // 关联
    val rel = cust.entries.firstNotNullOfOrNull { relatedFor(s, cust, it).takeIf { list -> list.isNotEmpty() } }
    println("related=" + (rel?.size ?: 0))

    // 备份 round-trip
    val out = backupPayload(s).toString()
    val s2 = parseBackup(out)
    println("roundtrip_same_counts=" + (s2.libraries.map { it.entries.size } == s.libraries.map { it.entries.size }))
    val origRoot = JSONObject(text)
    val origData = if (origRoot.has("data")) origRoot.getJSONObject("data") else origRoot
    println("orig_root_keys=" + origRoot.keys().asSequence().toList())
    println("orig_data_keys=" + origData.keys().asSequence().toList())
    println("orig_links=" + (origData.optJSONArray("links")?.length() ?: -1))
    s2.libraries.forEach { l -> println("  rt lib=" + l.name + " entries=" + l.entries.size + " fields=" + l.fields.size) }
    println("roundtrip_same_total=" + (s2.libraries.sumOf { it.entries.size } == s.libraries.sumOf { it.entries.size }))
    println("payload_bytes=" + out.toByteArray(Charsets.UTF_8).size)

    // CSV 导出
    val csv = Csv.export(s, cust)
    val lines = csv.split("\r\n").filter { it.isNotEmpty() }
    println("csv_rows=" + (lines.size - 1))
    println("csv_header=" + lines.first().removePrefix("\uFEFF"))
    println("csv_fields=" + Csv.exportFields(cust).size)

    // 瞳距无多行
    val pd = cust.fields.first { it.name == "瞳距" }
    println("pd_multiline=" + cust.entries.count { (it.values[pd.id] ?: "").contains("\n") })
    println("pd_empty=" + cust.entries.count { (it.values[pd.id] ?: "").isBlank() })

    // 新建 SKU 到矩阵（内存）
    val before = md?.models?.values?.sumOf { it.cells.values.sum() } ?: 0.0
    mxSave(lens, "测试型号", -1.0, 0.0, "3")
    val after = matrixData(lens)?.models?.values?.sumOf { it.cells.values.sum() } ?: 0.0
    println("mxSave_add=" + (after - before).toInt())

    // CSV 导入测试
    val result = Csv.import(cust, "姓名,电话,用途,OD,OD轴向,OS,OS轴向,瞳距,镜框,镜片,合计,时间,NO.,ADD,备注,客户\r\n测试甲,13800000000,远用,-1.00,,-1.00,,60,,绿膜,-1.00/-1.00,100,2026/11/19,1,,,", "append")
    println("csv_import_added=" + result.added + " skipped=" + result.skipped + " newFields=" + result.newFields)

    println("OK")

    val m1 = cust.entries.count { entryMatches(cust, it, "-3.00~-1.00") }
    println("range_power=" + m1)
    val m2 = cust.entries.count { entryMatches(cust, it, "2025/01~2025/12") }
    println("range_date=" + m2)
    val m3 = lens.entries.count { entryMatches(lens, it, "1.56~1.61") }
    println("range_model=" + m3)
    val payload = backupPayload(s)
    println("payload_version=" + payload.optInt("version") + " schema=" + payload.optInt("schemaVersion"))

    val b1 = batchCandidates(lens, BatchFilter("1.56 非球绿膜", -4.0, -1.0, "all")).size
    val b2 = batchCandidates(lens, BatchFilter("1.56 非球绿膜", null, null, "has")).size
    val b3 = batchCandidates(lens, BatchFilter(null, null, null, "has")).size
    println("batch_green_-4_-1=$b1 batch_green_has=$b2 batch_all_has=$b3")
    val changed = batchSetStock(lens, BatchFilter("1.56 非球绿膜", -4.0, -1.0, "all"), "4")
    val sumAfter = matrixData(lens)!!.models.values.sumOf { it.cells.values.sum() }.toInt()
    println("batch_changed=$changed total_after=$sumAfter")
    val renamed = renameModel(lens, "1.56 非球绿膜", "1.56 非球面绿膜")
    val hasNew = matrixData(lens)!!.models.containsKey("1.56 非球面绿膜")
    val rulesOld = alertsOf(lens)?.rules?.count { it.model == "1.56 非球绿膜" } ?: -1
    val rulesNew = alertsOf(lens)?.rules?.count { it.model == "1.56 非球面绿膜" } ?: -1
    println("renamed=$renamed hasNew=$hasNew rulesOld=$rulesOld rulesNew=$rulesNew")

    val odF = cust.fields.first { it.name.trim().equals("OD", true) }
    fun ent(v: String) = MEntry(id = uid("t_")).also { it.values[odF.id] = v }
    val dPlus = deltaBadges(cust, ent("+2.00"), ent("+1.00")).map { it.deeper }
    val dMyo = deltaBadges(cust, ent("-3.00"), ent("-2.00")).map { it.deeper }
    val dBack = deltaBadges(cust, ent("-1.50"), ent("-2.00")).map { it.deeper }
    println("delta_plus_deeper=$dPlus delta_myo_deeper=$dMyo delta_back_deeper=$dBack")

    val rt = restockText(lens, 4, 2)
    println("restock_lines=" + rt.lines().size)
    println("restock_head=" + rt.lines().take(5).joinToString("|"))
}
