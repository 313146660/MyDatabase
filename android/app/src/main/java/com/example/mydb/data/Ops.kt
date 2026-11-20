package com.example.mydb.data

import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Calendar

fun libById(state: MState, id: String?): MLibrary? = state.libraries.firstOrNull { it.id == id }

fun fieldById(lib: MLibrary?, id: String?): MField? = lib?.fields?.firstOrNull { it.id == id }

fun uid(prefix: String): String = prefix + System.currentTimeMillis().toString(36) + (1000..9999).random().toString(36)

fun primaryField(lib: MLibrary): MField? =
    lib.fields.firstOrNull { it.type == FieldType.TEXT } ?: lib.fields.firstOrNull()

fun titleOf(lib: MLibrary, entry: MEntry): String {
    val f = primaryField(lib) ?: return "未命名条目"
    return entry.values[f.id]?.takeIf { it.isNotBlank() } ?: "未命名条目"
}

fun subtitleOf(lib: MLibrary, entry: MEntry): String {
    val first = primaryField(lib)
    return lib.fields
        .filter { it.id != first?.id && !(entry.values[it.id] ?: "").isBlank() }
        .take(2)
        .map {
            val v = oneLine(entry.values[it.id])
            when (it.type) {
                FieldType.AUTONUMBER -> "${it.name} ${(it.prefix ?: "") + v}"
                else -> "${it.name} $v${it.unit ?: ""}"
            }
        }
        .joinToString(" · ")
}

fun listFieldsOf(lib: MLibrary): List<MField> {
    val picked = lib.listFields.mapNotNull { id -> lib.fields.firstOrNull { it.id == id } }
    if (picked.isNotEmpty()) return picked
    val pf = primaryField(lib)
    val rest = lib.fields.filter {
        it.id != pf?.id && it.type !in listOf(FieldType.AUTONUMBER, FieldType.CREATEDAT, FieldType.UPDATEDAT, FieldType.FORMULA)
    }
    return listOfNotNull(pf) + rest.take(2)
}

fun cellTextOf(state: MState, lib: MLibrary, entry: MEntry, f: MField?): String {
    if (f == null) return ""
    val raw = if (f.type == FieldType.FORMULA) evalFormula(state, lib, entry, f.expr ?: "") else entry.values[f.id]
    if (f.type == FieldType.CHECKBOX) return if (raw == "1") "是" else "否"
    var s = oneLine(raw)
    if (s.isEmpty()) return ""
    if (f.type == FieldType.AUTONUMBER) s = (f.prefix ?: "") + s
    if (f.type == FieldType.PERCENT) return s + (f.unit ?: "%")
    return s + (f.unit ?: "")
}

fun subtitleCellText(state: MState, lib: MLibrary, entry: MEntry, f: MField): String {
    val v = cellTextOf(state, lib, entry, f)
    return if (v.isEmpty()) "—" else v
}

fun nameFieldOf(lib: MLibrary): MField? = lib.fields.firstOrNull { it.name.contains("姓名") } ?: primaryField(lib)
fun phoneFieldOf(lib: MLibrary): MField? = lib.fields.firstOrNull { it.name.contains("电话") || it.type == FieldType.TEL }
fun normPhone(v: String?): String = (v ?: "").replace(Regex("\\D"), "")
fun nameOf(lib: MLibrary, entry: MEntry): String {
    val f = nameFieldOf(lib) ?: return ""
    return (entry.values[f.id] ?: "").trim()
}

fun dateFieldOf(lib: MLibrary): MField? {
    val exact = Regex("^(时间|日期|验光日期|配镜日期)$")
    return lib.fields.firstOrNull { exact.matches(it.name.trim()) }
        ?: lib.fields.firstOrNull { it.name.contains("时间") || it.name.contains("日期") }
}

fun visitDate(lib: MLibrary, entry: MEntry): Long? {
    val f = dateFieldOf(lib) ?: return null
    return parseDate(entry.values[f.id])
}

fun entryDate(lib: MLibrary, entry: MEntry): Long = visitDate(lib, entry) ?: entry.updatedAt

fun latestStamp(lib: MLibrary): String {
    if (lib.entries.isEmpty()) return "暂无记录"
    if (dateFieldOf(lib) != null) {
        val ts = lib.entries.maxOfOrNull { visitDate(lib, it) ?: 0L } ?: 0L
        if (ts > 0) return fmtDay(ts)
    }
    return relTime(lib.entries.maxOf { it.updatedAt })
}

fun entryMatches(lib: MLibrary, entry: MEntry, q: String): Boolean {
    if (q.isBlank()) return true
    val query = q.trim()

    // 区间搜索：-3.00~-1.00、1.56～1.61、2025/01~2025/12
    val range = Regex("^(.+?)\\s*[~～]\\s*(.+)$").find(query)
    if (range != null) {
        val left = range.groupValues[1].trim()
        val right = range.groupValues[2].trim()
        val d1 = parseDate(left)
        val d2 = parseDate(right)
        if (d1 != null && d2 != null && Regex("\\d{4}").containsMatchIn(left)) {
            val lo = minOf(d1, d2)
            val hi = rangeEnd(right, maxOf(d1, d2))
            val ts = visitDate(lib, entry) ?: entry.updatedAt
            return ts in lo..hi
        }
        val n1 = firstNumberIn(left)
        val n2 = firstNumberIn(right)
        if (n1 != null && n2 != null) {
            val lo = minOf(n1, n2) - 0.001
            val hi = maxOf(n1, n2) + 0.001
            entry.values.values.forEach { v ->
                firstNumberIn(v)?.let { if (it in lo..hi) return true }
                sphOf(v)?.let { if (it in lo..hi) return true }
                val c = cylOf(v)
                if (c != 0.0 && c in lo..hi) return true
            }
            return false
        }
    }

    val hay = buildString {
        append(titleOf(lib, entry)).append(' ')
        entry.values.values.forEach { append(it).append(' ') }
        append(lib.name)
    }.lowercase()
    return hay.contains(query.lowercase())
}

/** 区间右端补到当天 / 当月 / 当年结束，避免 2025/01~2025/12 漏掉 12 月。 */
private fun rangeEnd(raw: String, ts: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = ts
    when {
        Regex("^\\d{4}\\s*[-/年.]\\s*\\d{1,2}\\s*$").containsMatchIn(raw) -> cal.add(java.util.Calendar.MONTH, 1)
        Regex("^\\d{4}\\s*$").containsMatchIn(raw) -> cal.add(java.util.Calendar.YEAR, 1)
        else -> cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    return cal.timeInMillis - 1
}

fun samePersonEntries(lib: MLibrary, entry: MEntry): List<MEntry> {
    val nf = nameFieldOf(lib) ?: return emptyList()
    val name = nameOf(lib, entry)
    if (name.isEmpty()) return emptyList()
    return lib.entries.filter { it.id != entry.id && nameOf(lib, it) == name }
}

data class FamilyMember(val name: String, val entry: MEntry, val ts: Long)
data class FamilyInfo(
    val phone: String,
    val display: String,
    val members: List<FamilyMember>,
    val total: Double,
    val visits: Int,
    val people: Int
)

fun familyGroup(lib: MLibrary, entry: MEntry): FamilyInfo? {
    val pf = phoneFieldOf(lib) ?: return null
    val nf = nameFieldOf(lib) ?: return null
    val phone = normPhone(entry.values[pf.id])
    if (phone.isEmpty()) return null
    val myName = nameOf(lib, entry)
    val byName = LinkedHashMap<String, FamilyMember>()
    var total = 0.0
    var visits = 0
    val moneyField = lib.fields.firstOrNull { Regex("合计|金额|价格").containsMatchIn(it.name) }
    lib.entries.forEach { e ->
        if (normPhone(e.values[pf.id]) != phone) return@forEach
        visits++
        if (moneyField != null) {
            val money = firstNumberIn(e.values[moneyField.id])
            if (money != null) total += money
        }
        val n = nameOf(lib, e)
        if (n.isEmpty() || n == myName) return@forEach
        val ts = entryDate(lib, e)
        val prev = byName[n]
        if (prev == null || ts > prev.ts) byName[n] = FamilyMember(n, e, ts)
    }
    val members = byName.values.sortedByDescending { it.ts }
    if (members.isEmpty()) return null
    return FamilyInfo(phone, oneLine(entry.values[pf.id]), members, round2(total), visits, members.size + 1)
}

fun relatedFor(state: MState, lib: MLibrary, entry: MEntry): List<Pair<MLibrary, MEntry>> {
    val nameField = nameFieldOf(lib)
    val phoneField = phoneFieldOf(lib)
    val name = nameField?.let { entry.values[it.id] }?.trim().orEmpty()
    val phone = phoneField?.let { normPhone(entry.values[it.id]) }.orEmpty()
    if (name.isEmpty() && phone.isEmpty()) return emptyList()
    val out = mutableListOf<Pair<MLibrary, MEntry>>()
    state.libraries.forEach { l ->
        l.entries.forEach { e ->
            if (l.id == lib.id && e.id == entry.id) return@forEach
            val nf = nameFieldOf(l)
            val pf = phoneFieldOf(l)
            val n = nf?.let { e.values[it.id] }?.trim().orEmpty()
            val p = pf?.let { normPhone(e.values[it.id]) }.orEmpty()
            if (name.isNotEmpty() && n.isNotEmpty() && name == n) {
                out.add(l to e)
            } else if (phone.isNotEmpty() && p.isNotEmpty() && phone == p && (n.isEmpty() || name.isEmpty() || n == name)) {
                out.add(l to e)
            }
        }
    }
    return out
}

/* ---------------- 验光历史 ---------------- */

data class HistItem(val entry: MEntry, val current: Boolean, val dup: Int)

private fun historySignature(lib: MLibrary, entry: MEntry): String {
    val parts = listOf(Regex("^OD$", RegexOption.IGNORE_CASE), Regex("^OS$", RegexOption.IGNORE_CASE), Regex("合计|金额|价格"), Regex("^NO\\.?$"))
        .map { re -> lib.fields.firstOrNull { re.containsMatchIn(it.name) }?.let { oneLine(entry.values[it.id]).lowercase() } ?: "" }
    return (listOf(entryDate(lib, entry).toString()) + parts).joinToString("\u0001")
}

fun historyOf(lib: MLibrary, entry: MEntry): List<HistItem> {
    if (dateFieldOf(lib) == null) return emptyList()
    val raw = (listOf(HistItem(entry, true, 1)) + samePersonEntries(lib, entry).map { HistItem(it, false, 1) })
        .sortedByDescending { entryDate(lib, it.entry) }
    val seen = LinkedHashMap<String, HistItem>()
    val out = mutableListOf<HistItem>()
    raw.forEach { item ->
        val sig = historySignature(lib, item.entry)
        val hit = seen[sig]
        if (hit != null) {
            seen[sig] = hit.copy(dup = hit.dup + 1, current = hit.current || item.current)
            val idx = out.indexOfFirst { it.entry.id == hit.entry.id }
            if (idx >= 0) out[idx] = seen[sig]!!
        } else {
            seen[sig] = item
            out.add(item)
        }
    }
    return out
}

data class Delta(val label: String, val deeper: Boolean, val amount: Double)

fun deltaBadges(lib: MLibrary, cur: MEntry, prev: MEntry): List<Delta> {
    val odF = lib.fields.firstOrNull { Regex("^OD$", RegexOption.IGNORE_CASE).matches(it.name) }
    val osF = lib.fields.firstOrNull { Regex("^OS$", RegexOption.IGNORE_CASE).matches(it.name) }
    val out = mutableListOf<Delta>()
    listOf("OD" to odF, "OS" to osF).forEach { (label, f) ->
        if (f == null) return@forEach
        val a = firstNumberIn(prev.values[f.id]) ?: return@forEach
        val b = firstNumberIn(cur.values[f.id]) ?: return@forEach
        // 用绝对值比较：近视 -2.00 → -3.00 是加深，远视 +1.00 → +2.00 同样是加深
        val diff = round2(abs(b) - abs(a))
        if (diff == 0.0) return@forEach
        out.add(Delta(label, diff > 0, abs(diff)))
    }
    return out
}

/* ---------------- 矩阵 ---------------- */

data class MatrixCfgView(val group: MField, val row: MField, val col: MField, val value: MField)

fun matrixCfg(lib: MLibrary): MatrixCfgView? {
    val m = lib.matrix ?: return null
    val g = fieldById(lib, m.group) ?: return null
    val r = fieldById(lib, m.row) ?: return null
    val c = fieldById(lib, m.col) ?: return null
    val v = fieldById(lib, m.value) ?: return null
    return MatrixCfgView(g, r, c, v)
}

fun cylValueOf(entry: MEntry, cfg: MatrixCfgView): Double {
    val raw = entry.values[cfg.col.id] ?: return 0.0
    if (raw.isBlank()) return 0.0
    return toPower(raw) ?: 0.0
}

data class ModelGroup(
    val model: String,
    val sphs: MutableSet<Double> = linkedSetOf(),
    val cyls: MutableSet<Double> = linkedSetOf(),
    val cells: MutableMap<String, Double> = mutableMapOf(),
    val filled: MutableSet<String> = mutableSetOf()
)

data class MatrixData(val cfg: MatrixCfgView, val models: LinkedHashMap<String, ModelGroup>)

fun cellKey(sph: Double, cyl: Double): String = "${(sph * 100).roundToInt()}|${(cyl * 100).roundToInt()}"

fun matrixData(lib: MLibrary): MatrixData? {
    val cfg = matrixCfg(lib) ?: return null
    val models = LinkedHashMap<String, ModelGroup>()
    lib.entries.forEach { e ->
        val model = (e.values[cfg.group.id] ?: "").trim()
        val sph = toPower(e.values[cfg.row.id]) ?: return@forEach
        val cyl = cylValueOf(e, cfg)
        if (model.isEmpty()) return@forEach
        val g = models.getOrPut(model) { ModelGroup(model) }
        g.sphs.add(sph)
        g.cyls.add(cyl)
        val k = cellKey(sph, cyl)
        val raw = e.values[cfg.value.id]
        val has = raw != null && raw.isNotBlank()
        g.cells[k] = (g.cells[k] ?: 0.0) + (raw?.toDoubleOrNull() ?: 0.0)
        if (has) g.filled.add(k)
    }
    return MatrixData(cfg, models)
}

fun findSku(lib: MLibrary, model: String, sph: Double, cyl: Double): MEntry? {
    val cfg = matrixCfg(lib) ?: return null
    return lib.entries.firstOrNull {
        (it.values[cfg.group.id] ?: "").trim() == model &&
            toPower(it.values[cfg.row.id]) == sph &&
            cylValueOf(it, cfg) == cyl
    }
}

/** 返回提示文字；变更需要调用方保存 */
fun mxSave(lib: MLibrary, model: String, sph: Double, cyl: Double, qtyRaw: String): String {
    val cfg = matrixCfg(lib) ?: return ""
    val blank = qtyRaw.trim().isEmpty()
    val found = findSku(lib, model, sph, cyl)
    if (found == null && blank) return "留空 = 没有这个镜片，不写入、不提醒"
    val hit = found ?: MEntry(id = uid("e_")).also { e ->
        e.values[cfg.group.id] = model
        e.values[cfg.row.id] = fmtPower(sph)
        e.values[cfg.col.id] = fmtPower(cyl)
        lib.entries.add(e)
    }
    hit.values[cfg.value.id] = if (blank) "" else (qtyRaw.trim().toDoubleOrNull() ?: 0.0).toInt().coerceAtLeast(0).toString()
    hit.updatedAt = System.currentTimeMillis()
    val pos = "$model · ${fmtPower(sph)}" + if (cyl != 0.0) " / ${fmtPower(cyl)}" else ""
    return if (blank) "$pos → 未入库（不提醒）" else "$pos → ${hit.values[cfg.value.id]} ${cfg.value.unit ?: "副"}"
}

/* ---------------- 型号改名 / 批量改库存 ---------------- */

fun renameModel(lib: MLibrary, old: String, newRaw: String): Int {
    val cfg = matrixCfg(lib) ?: return 0
    val new = newRaw.trim()
    if (new.isEmpty() || new == old) return 0
    var n = 0
    lib.entries.forEach { e ->
        if ((e.values[cfg.group.id] ?: "").trim() == old) {
            e.values[cfg.group.id] = new
            e.updatedAt = System.currentTimeMillis()
            n++
        }
    }
    lib.alerts?.rules?.forEach { if (it.model == old) it.model = new }
    lib.matrix?.ranges?.let { r ->
        r.remove(old)?.let { r[new] = it }
    }
    return n
}

data class BatchFilter(
    val model: String? = null,
    val sphMin: Double? = null,
    val sphMax: Double? = null,
    val cyl: String = "all"
)

data class BatchPreset(
    val name: String,
    val allModels: Boolean = false,
    val sphMin: String = "",
    val sphMax: String = "",
    val cyl: String = "all",
    val qty: String = ""
)

data class StockBackup(val entryId: String, val value: String?)

fun batchCandidates(lib: MLibrary, f: BatchFilter): List<MEntry> {
    val cfg = matrixCfg(lib) ?: return emptyList()
    return lib.entries.filter { e ->
        val model = (e.values[cfg.group.id] ?: "").trim()
        if (f.model != null && model != f.model) return@filter false
        val sph = toPower(e.values[cfg.row.id]) ?: return@filter false
        val cyl = cylValueOf(e, cfg)
        if (f.sphMin != null && sph < f.sphMin - 1e-6) return@filter false
        if (f.sphMax != null && sph > f.sphMax + 1e-6) return@filter false
        when (f.cyl) {
            "none" -> if (cyl != 0.0) return@filter false
            "has" -> if (cyl == 0.0) return@filter false
        }
        true
    }
}

/** 批量把匹配到的 SKU 库存改成 qtyRaw；留空表示清空（未入库、不提醒）。返回影响条数。 */
fun batchSetStock(lib: MLibrary, f: BatchFilter, qtyRaw: String): Int {
    val cfg = matrixCfg(lib) ?: return 0
    val blank = qtyRaw.isBlank()
    val qty = (qtyRaw.toDoubleOrNull() ?: 0.0).toInt().coerceAtLeast(0)
    val list = batchCandidates(lib, f)
    list.forEach { e ->
        if (blank) e.values.remove(cfg.value.id) else e.values[cfg.value.id] = qty.toString()
        e.updatedAt = System.currentTimeMillis()
    }
    return list.size
}

/* ---------------- 库存提醒 ---------------- */

data class AlertsView(val cfg: MatrixCfgView, val field: MField, val defaultThreshold: Int, val rules: List<AlertRule>)

fun alertsOf(lib: MLibrary): AlertsView? {
    val cfg = matrixCfg(lib) ?: return null
    val a = lib.alerts ?: return null
    val field = fieldById(lib, a.field) ?: cfg.value
    return AlertsView(cfg, field, a.defaultThreshold.coerceAtLeast(0), a.rules)
}

fun ruleMatches(rule: AlertRule, model: String, sph: Double, cyl: Double): Boolean {
    if (rule.model.isNotEmpty() && rule.model != model) return false
    val lo = toPower(rule.sphMin)
    val hi = toPower(rule.sphMax)
    if (lo != null && hi != null) {
        val a = minOf(lo, hi)
        val b = maxOf(lo, hi)
        if (sph < a - 1e-9 || sph > b + 1e-9) return false
    } else if (lo != null && sph < lo - 1e-9) {
        return false
    } else if (hi != null && sph > hi + 1e-9) {
        return false
    }
    if (rule.cyl == "none" && cyl != 0.0) return false
    if (rule.cyl == "has" && cyl == 0.0) return false
    return true
}

fun thresholdFor(al: AlertsView?, model: String, sph: Double, cyl: Double): Int {
    if (al == null) return 0
    val rule = al.rules.firstOrNull { ruleMatches(it, model, sph, cyl) }
    return (rule?.threshold ?: al.defaultThreshold).coerceAtLeast(0)
}

data class LowItem(val entry: MEntry, val model: String, val sph: Double, val cyl: Double, val qty: Double, val threshold: Int)

fun lowStockOf(lib: MLibrary): List<LowItem> {
    val al = alertsOf(lib) ?: return emptyList()
    val out = mutableListOf<LowItem>()
    lib.entries.forEach { e ->
        val model = (e.values[al.cfg.group.id] ?: "").trim()
        val sph = toPower(e.values[al.cfg.row.id]) ?: return@forEach
        val cyl = cylValueOf(e, al.cfg)
        if (model.isEmpty()) return@forEach
        val raw = e.values[al.field.id]
        if (raw == null || raw.isBlank()) return@forEach
        val qty = raw.toDoubleOrNull() ?: 0.0
        val th = thresholdFor(al, model, sph, cyl)
        if (qty <= th) out.add(LowItem(e, model, sph, cyl, qty, th))
    }
    return out.sortedWith(compareBy({ it.qty }, { it.model }, { abs(it.sph) }))
}

/* ---------------- 补货清单 ---------------- */

const val RESTOCK_SCOPE_BELOW_TARGET = "below_target"
const val RESTOCK_SCOPE_LOW_STOCK = "low_stock"

data class RestockItem(
    val entry: MEntry,
    val model: String,
    val sph: Double,
    val cyl: Double,
    val qty: Int,
    val target: Int,
    val restock: Int
) {
    /** 缺货：当前库存为 0（含留空）。补货清单里缺货排最前、标红。 */
    val isOut: Boolean get() = qty <= 0
}

/** 补货单里的一条明细：货到入库时按型号/球镜/柱镜匹配库存规格。 */
data class RestockOrderItem(
    val model: String,
    val sph: Double,
    val cyl: Double,
    val qty: Int
)

/** 一条补货历史记录：某次生成并复制/分享/保存过的补货文本快照。
 *  items 为结构化明细（用于入库），received 标记是否已确认入库。 */
data class RestockRecord(
    val id: String,
    val libId: String,
    val libName: String,
    val at: Long,
    val total: Int,
    val count: Int,
    val text: String,
    val items: List<RestockOrderItem> = emptyList(),
    val received: Boolean = false
)

fun restockCfgOf(lib: MLibrary): RestockCfg = lib.restock ?: RestockCfg()

fun restockTargetFor(cfg: RestockCfg, model: String, isPlain: Boolean, sph: Double = 0.0, cyl: Double = 0.0): Int {
    // 命中的范围规则里取最具体的（球镜区间跨度最小）；plain/cylTarget 为 -1 表示不覆盖
    val hit = cfg.ranges.filter { rangeMatches(it, model, sph, cyl) }.minByOrNull { rangeSpan(it) }
    val rangeV = if (isPlain) (hit?.plain ?: -1) else (hit?.cylTarget ?: -1)
    val t = cfg.targets[model]
    val v = if (rangeV >= 0) rangeV else if (isPlain) (t?.plain ?: cfg.defaultPlain) else (t?.cyl ?: cfg.defaultCyl)
    return v.coerceAtLeast(0)
}

private fun rangeMatches(rule: RestockRangeRule, model: String, sph: Double, cyl: Double): Boolean {
    if (rule.model.isNotEmpty() && rule.model != model) return false
    val lo = toPower(rule.sphMin)
    val hi = toPower(rule.sphMax)
    if (lo != null && hi != null) {
        val a = minOf(lo, hi); val b = maxOf(lo, hi)
        if (sph < a - 1e-9 || sph > b + 1e-9) return false
    } else if (lo != null && sph < lo - 1e-9) return false
    else if (hi != null && sph > hi + 1e-9) return false
    if (rule.cyl == "none" && cyl != 0.0) return false
    if (rule.cyl == "has" && cyl == 0.0) return false
    return true
}

/** 规则的球镜区间跨度，越小越具体；只设一边或都不设视为最不具体。 */
private fun rangeSpan(rule: RestockRangeRule): Double {
    val lo = toPower(rule.sphMin) ?: return Double.POSITIVE_INFINITY
    val hi = toPower(rule.sphMax) ?: return Double.POSITIVE_INFINITY
    return abs(hi - lo)
}

/** 库里出现过的所有型号（去重，按出现顺序）。 */
fun modelsOf(lib: MLibrary): List<String> {
    val mx = matrixCfg(lib) ?: return emptyList()
    val out = linkedSetOf<String>()
    lib.entries.forEach { e ->
        val m = (e.values[mx.group.id] ?: "").trim()
        if (m.isNotEmpty()) out.add(m)
    }
    return out.toList()
}

/** 计算需要补货的规格：补货数 = 目标库存 − 当前库存，>0 才列出。 */
fun restockItems(lib: MLibrary): List<RestockItem> {
    val mx = matrixCfg(lib) ?: return emptyList()
    val cfg = restockCfgOf(lib)
    val al = alertsOf(lib)
    val onlyLow = cfg.scope == RESTOCK_SCOPE_LOW_STOCK
    val out = mutableListOf<RestockItem>()
    lib.entries.forEach { e ->
        val model = (e.values[mx.group.id] ?: "").trim()
        val sph = toPower(e.values[mx.row.id]) ?: return@forEach
        val cyl = cylValueOf(e, mx)
        if (model.isEmpty()) return@forEach
        val raw = e.values[mx.value.id]
        val qty = if (raw.isNullOrBlank()) 0 else (raw.toDoubleOrNull() ?: 0.0).toInt()
        if (onlyLow && qty > thresholdFor(al, model, sph, cyl)) return@forEach
        val target = restockTargetFor(cfg, model, cyl == 0.0, sph, cyl)
        val need = (target - qty).coerceAtLeast(0)
        if (need <= 0) return@forEach
        out.add(RestockItem(e, model, sph, cyl, qty, target, need))
    }
    // 同型号内按度数排序：球镜浅→深（绝对值升序），同球镜下单光在前、散光柱镜浅→深
    return out.sortedWith(compareBy({ it.model }, { abs(it.sph) }, { it.sph }, { abs(it.cyl) }, { it.cyl }))
}

fun restockUnit(lib: MLibrary): String =
    lib.fields.firstOrNull { it.name.contains("库存") }?.unit?.takeIf { it.isNotBlank() } ?: "片"

/** 生成发给供货商的纯文本：按型号分组，数量 = 目标库存 − 当前库存；含型号小计与合计。
 *  同型号内按度数浅→深排序，不在文本里加段标签。 */
fun restockText(lib: MLibrary): String = buildRestockText(restockItems(lib), restockUnit(lib))

private fun buildRestockText(list: List<RestockItem>, unit: String): String {
    val sb = StringBuilder()
    var total = 0
    list.groupBy { it.model }.forEach { (model, ml) ->
        if (sb.isNotEmpty()) sb.append("\r\n")
        val sum = ml.sumOf { it.restock }
        total += sum
        sb.append(model).append("  共 ").append(sum).append(" ").append(unit).append("\r\n")
        ml.forEach { appendRestockLine(sb, it) }
    }
    if (total > 0) sb.append("\r\n合计：").append(total).append(" ").append(unit).append("\r\n")
    return sb.toString().trimEnd()
}

/** 按供货商分组生成补货文本：未填供货商的型号归「未分类」。返回 供货商名 -> 该组文本。 */
fun restockTextBySupplier(lib: MLibrary): Map<String, String> {
    val cfg = restockCfgOf(lib)
    val unit = restockUnit(lib)
    val groups = linkedMapOf<String, MutableList<RestockItem>>()
    restockItems(lib).forEach { item ->
        val sup = cfg.suppliers[item.model]?.takeIf { it.isNotBlank() } ?: "未分类"
        groups.getOrPut(sup) { mutableListOf() }.add(item)
    }
    val out = linkedMapOf<String, String>()
    groups.forEach { (sup, list) -> out[sup] = buildRestockText(list, unit) }
    return out
}

private fun appendRestockLine(sb: StringBuilder, i: RestockItem) {
    sb.append(fmtPower(i.sph)).append("ds")
    if (i.cyl != 0.0) sb.append(" / ").append(fmtPower(i.cyl)).append("dc")
    sb.append("，").append(i.restock).append("\r\n")
}

fun restockHistoryFromJson(arr: org.json.JSONArray?): List<RestockRecord> {
    if (arr == null) return emptyList()
    val out = mutableListOf<RestockRecord>()
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val items = mutableListOf<RestockOrderItem>()
        o.optJSONArray("items")?.let { ia ->
            for (j in 0 until ia.length()) {
                val io = ia.optJSONObject(j) ?: continue
                items.add(RestockOrderItem(io.optString("model"), io.optDouble("sph"), io.optDouble("cyl"), io.optInt("qty")))
            }
        }
        out.add(
            RestockRecord(
                id = o.optString("id"),
                libId = o.optString("libId"),
                libName = o.optString("libName"),
                at = o.optLong("at"),
                total = o.optInt("total"),
                count = o.optInt("count"),
                text = o.optString("text"),
                items = items,
                received = o.optBoolean("received", false)
            )
        )
    }
    return out
}

fun restockHistoryToJson(list: List<RestockRecord>): org.json.JSONArray {
    val arr = org.json.JSONArray()
    list.forEach { r ->
        val obj = org.json.JSONObject()
            .put("id", r.id).put("libId", r.libId).put("libName", r.libName)
            .put("at", r.at).put("total", r.total).put("count", r.count).put("text", r.text)
            .put("received", r.received)
        val ia = org.json.JSONArray()
        r.items.forEach { it2 ->
            ia.put(org.json.JSONObject().put("model", it2.model).put("sph", it2.sph).put("cyl", it2.cyl).put("qty", it2.qty))
        }
        obj.put("items", ia)
        arr.put(obj)
    }
    return arr
}

/** 查找库里型号/球镜/柱镜完全匹配的库存条目，用于入库定位。 */
fun findStockEntry(lib: MLibrary, model: String, sph: Double, cyl: Double): MEntry? {
    val mx = matrixCfg(lib) ?: return null
    return lib.entries.firstOrNull { e ->
        (e.values[mx.group.id] ?: "").trim() == model &&
            powerEq(toPower(e.values[mx.row.id]), sph) &&
            abs(cylValueOf(e, mx) - cyl) < 1e-6
    }
}

private fun powerEq(a: Double?, b: Double): Boolean = a != null && abs(a - b) < 1e-6

/** 所有库的低库存，用于首页 */fun lowStockAll(state: MState): List<Pair<MLibrary, List<LowItem>>> =
    state.libraries.map { it to lowStockOf(it) }.filter { it.second.isNotEmpty() }

/* ---------------- 关联选择 ---------------- */

data class PickOption(
    val value: String,
    val plain: String,
    val label: String,
    val sub: String,
    val stock: Int?,
    val unit: String,
    val libId: String,
    val entryId: String
)

fun pickOptions(state: MState, lib: MLibrary, field: MField, values: Map<String, String>): List<PickOption> {
    val p = field.pick ?: return emptyList()
    val src = libById(state, p.libId) ?: return emptyList()
    if (p.kind == "sku") {
        val mx = matrixCfg(src) ?: return emptyList()
        var filter = if (p.filterFieldId.isNotEmpty()) (values[p.filterFieldId] ?: "").trim() else ""
        fun build(f: String): List<PickOption> {
            val rows = mutableListOf<PickOption>()
            src.entries.forEach { e ->
                val model = (e.values[mx.group.id] ?: "").trim()
                if (model.isEmpty()) return@forEach
                if (f.isNotEmpty() && model != f) return@forEach
                val sph = toPower(e.values[mx.row.id]) ?: return@forEach
                val cyl = cylValueOf(e, mx)
                val plain = fmtPower(sph) + if (cyl != 0.0) "/${fmtPower(cyl)}" else ""
                rows.add(
                    PickOption(
                        value = powerText(sph, cyl),
                        plain = plain,
                        label = powerText(sph, cyl),
                        sub = model,
                        stock = e.values[mx.value.id]?.toDoubleOrNull()?.toInt(),
                        unit = mx.value.unit ?: "",
                        libId = src.id,
                        entryId = e.id
                    )
                )
            }
            return rows
        }
        var out = build(filter)
        if (out.isEmpty() && filter.isNotEmpty()) out = build("")
        return out.sortedWith(compareBy({ abs(toPower(it.plain.substringBefore('/')) ?: 0.0) }, { toPower(it.plain.substringBefore('/')) ?: 0.0 }, { abs(toPower(it.plain.substringAfter('/', "0")) ?: 0.0) }, { toPower(it.plain.substringAfter('/', "0")) ?: 0.0 }))
    }
    val sf = fieldById(src, p.fieldId) ?: return emptyList()
    val mx = matrixCfg(src)
    val seen = LinkedHashMap<String, PickOption>()
    src.entries.forEach { e ->
        val v = (e.values[sf.id] ?: "").trim()
        if (v.isEmpty() || seen.containsKey(v)) return@forEach
        var sub = ""
        if (mx != null && mx.group.id == sf.id) {
            var n = 0.0
            src.entries.forEach { x -> if ((x.values[sf.id] ?: "").trim() == v) n += x.values[mx.value.id]?.toDoubleOrNull() ?: 0.0 }
            sub = "库存 ${n.toInt()}${mx.value.unit ?: ""}"
        }
        seen[v] = PickOption(v, v, v, sub, null, "", "", "")
    }
    return seen.values.toList()
}

data class StockChange(val libName: String, val label: String, val from: Int, val to: Int, val unit: String, val dir: Int)

private fun adjustStock(state: MState, libId: String, entryId: String, delta: Int): StockChange? {
    val src = libById(state, libId) ?: return null
    val e = src.entries.firstOrNull { it.id == entryId } ?: return null
    val mx = matrixCfg(src) ?: return null
    val cur = e.values[mx.value.id]?.toDoubleOrNull()?.toInt() ?: 0
    val next = (cur + delta).coerceAtLeast(0)
    e.values[mx.value.id] = next.toString()
    e.updatedAt = System.currentTimeMillis()
    val sph = toPower(e.values[mx.row.id])
    val cyl = cylValueOf(e, mx)
    val label = "${(e.values[mx.group.id] ?: "")} ${fmtPower(sph)}" + if (cyl != 0.0) "/${fmtPower(cyl)}" else ""
    return StockChange(src.name, label.trim(), cur, next, mx.value.unit ?: "", if (delta < 0) -1 else 1)
}

fun skuTextOf(src: MLibrary, e: MEntry): String {
    val mx = matrixCfg(src) ?: return ""
    return powerText(toPower(e.values[mx.row.id]), cylValueOf(e, mx))
}

fun applyPickStock(state: MState, lib: MLibrary, entry: MEntry, values: Map<String, String>, picked: Set<String>): List<StockChange> {
    val changes = mutableListOf<StockChange>()
    lib.fields.forEach { f ->
        if (f.type != FieldType.PICK) return@forEach
        val p = f.pick ?: return@forEach
        if (!p.deduct) return@forEach
        val opts = pickOptions(state, lib, f, values)
        val valRaw = (values[f.id] ?: "").trim()
        val valKey = if (p.kind == "sku") normPower(valRaw) else valRaw
        val prev = entry.picks[f.id]
        val prevSrc = prev?.let { libById(state, it.libId) }
        val prevSku = if (prev != null && prevSrc != null) prevSrc.entries.firstOrNull { it.id == prev.entryId } else null
        val sameAsPrev = prev != null && prevSrc != null && prevSku != null && normPower(skuTextOf(prevSrc, prevSku)) == valKey
        val wasPicked = picked.isEmpty() || picked.contains(f.id)
        if (!wasPicked) {
            if (prev != null && !sameAsPrev) {
                adjustStock(state, prev.libId, prev.entryId, prev.qty)?.let { changes.add(it) }
                entry.picks.remove(f.id)
            }
            return@forEach
        }
        val modelHint = if (p.filterFieldId.isNotEmpty()) (values[p.filterFieldId] ?: "").trim() else ""
        val hit = (if (prev != null) opts.firstOrNull { it.entryId == prev.entryId && normPower(it.value) == valKey } else null)
            ?: opts.firstOrNull { normPower(it.value) == valKey && modelHint.isNotEmpty() && it.sub == modelHint }
            ?: opts.firstOrNull { normPower(it.value) == valKey }
        val same = prev != null && hit != null && prev.entryId == hit.entryId
        if (prev != null && !same) {
            adjustStock(state, prev.libId, prev.entryId, prev.qty)?.let { changes.add(it) }
            entry.picks.remove(f.id)
        }
        if (hit != null && !same) {
            val qty = p.qty.coerceAtLeast(1)
            adjustStock(state, hit.libId, hit.entryId, -qty)?.let { changes.add(it) }
            entry.picks[f.id] = PickRef(hit.libId, hit.entryId, qty)
        }
    }
    return changes
}

fun stockToast(changes: List<StockChange>): String? {
    val c = changes.lastOrNull() ?: return null
    return "${c.libName} · ${c.label} 库存 ${c.from} → ${c.to}${c.unit}"
}

/* ---------------- 自动字段 ---------------- */

fun nextAutoNumber(lib: MLibrary, field: MField): Int {
    var max = 0
    lib.entries.forEach { e ->
        val n = Regex("(\\d+)\\s*$").find(e.values[field.id] ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0
        if (n > max) max = n
    }
    return maxOf(max + 1, field.start ?: 1)
}

fun stampAutoFields(lib: MLibrary, entry: MEntry, isNew: Boolean) {
    val now = System.currentTimeMillis()
    lib.fields.forEach { f ->
        val cur = (entry.values[f.id] ?: "").trim()
        when (f.type) {
            FieldType.AUTONUMBER -> if (isNew || cur.isEmpty()) entry.values[f.id] = nextAutoNumber(lib, f).toString()
            FieldType.CREATEDAT -> if (isNew || cur.isEmpty()) entry.values[f.id] = fmtDay(now)
            FieldType.UPDATEDAT -> entry.values[f.id] = fmtStamp(now)
        }
    }
}

/* ---------------- 客户聚合视图 ---------------- */

/** 复查周期（月），按用途区分：远用 18、近用 12、渐进 24。 */
fun reviewMonths(use: String?): Int = when (use?.trim()) {
    "近用" -> 12
    "渐进" -> 24
    else -> 18
}

fun moneyFieldOf(lib: MLibrary): MField? =
    lib.fields.firstOrNull { Regex("合计|金额|价格").containsMatchIn(it.name) }

fun useFieldOf(lib: MLibrary): MField? =
    lib.fields.firstOrNull { it.name.contains("用途") }

fun lensFieldOf(lib: MLibrary): MField? =
    lib.fields.firstOrNull { it.name.contains("镜片") }

data class CustomerGroup(
    val key: String,
    val display: String,
    val phone: String,
    val names: Set<String>,
    val entries: List<MEntry>,
    val count: Int,
    val total: Double,
    val lastDate: Long?,
    val monthsSinceLast: Int,
    val overdue: Boolean
)

/** 按电话或姓名聚合客户记录。query 非空时只保留组内任一记录命中搜索的组。overdueFirst=true 时超期客户排最前。 */
fun customerGroupsCalc(lib: MLibrary, by: String, query: String = "", overdueFirst: Boolean = true, sortKey: String = "last_desc"): List<CustomerGroup> {
    val pf = phoneFieldOf(lib)
    val nf = nameFieldOf(lib)
    val mf = moneyFieldOf(lib)
    val uf = useFieldOf(lib)
    val groups = LinkedHashMap<String, MutableList<MEntry>>()
    lib.entries.forEach { e ->
        val k = when (by) {
            "phone" -> pf?.let { normPhone(e.values[it.id]) }?.takeIf { it.isNotEmpty() }
            "name" -> nf?.let { nameOf(lib, e) }?.takeIf { it.isNotEmpty() }
            else -> null
        } ?: return@forEach
        groups.getOrPut(k) { mutableListOf() }.add(e)
    }
    val now = System.currentTimeMillis()
    val dayMs = 1000L * 60 * 60 * 24
    val out = mutableListOf<CustomerGroup>()
    groups.forEach { (key, list) ->
        if (query.isNotBlank() && list.none { entryMatches(lib, it, query) }) return@forEach
        val sorted = list.sortedByDescending { entryDate(lib, it) }
        val names = linkedSetOf<String>()
        var total = 0.0
        sorted.forEach { e ->
            nf?.let { val n = nameOf(lib, e); if (n.isNotEmpty()) names.add(n) }
            mf?.let { firstNumberIn(e.values[it.id])?.let { m -> total += m } }
        }
        val last = sorted.firstOrNull()?.let { entryDate(lib, it) }
        val lastUse = sorted.firstOrNull()?.let { uf?.id?.let { id -> it.values[id] } }
        val months = if (last != null) ((now - last) / (dayMs * 30)).toInt() else 0
        val display = if (by == "phone") {
            (names.firstOrNull() ?: "未命名") + if (names.size > 1) " 等${names.size}人" else ""
        } else {
            key
        }
        val phone = pf?.let { f ->
            sorted.firstNotNullOfOrNull { normPhone(it.values[f.id]).takeIf { v -> v.isNotEmpty() } }
        } ?: ""
        out.add(
            CustomerGroup(key, display, phone, names, sorted, sorted.size, round2(total),
                last, months, last != null && months > reviewMonths(lastUse))
        )
    }
    val base = when (sortKey) {
        "total_desc" -> out.sortedWith(
            compareByDescending<CustomerGroup> { it.total }.thenByDescending { it.lastDate ?: 0L }
        )
        "total_asc" -> out.sortedWith(
            compareBy<CustomerGroup> { it.total }.thenByDescending { it.lastDate ?: 0L }
        )
        "count_desc" -> out.sortedWith(
            compareByDescending<CustomerGroup> { it.count }.thenByDescending { it.lastDate ?: 0L }
        )
        "count_asc" -> out.sortedWith(
            compareBy<CustomerGroup> { it.count }.thenByDescending { it.lastDate ?: 0L }
        )
        "avg_desc" -> out.sortedWith(
            compareByDescending<CustomerGroup> { it.total / it.count.coerceAtLeast(1) }
                .thenByDescending { it.lastDate ?: 0L }
        )
        "avg_asc" -> out.sortedWith(
            compareBy<CustomerGroup> { it.total / it.count.coerceAtLeast(1) }
                .thenByDescending { it.lastDate ?: 0L }
        )
        "last_asc" -> out.sortedWith(compareBy<CustomerGroup> { it.lastDate ?: Long.MAX_VALUE })
        else -> out.sortedWith(compareByDescending<CustomerGroup> { it.lastDate ?: 0L })
    }
    // 稳定排序：超期置顶时保留上面选定的排序作为组内次序
    return if (overdueFirst) {
        base.sortedByDescending { if (it.overdue) 1 else 0 }
    } else {
        base
    }
}

/* ---------------- 派生结果缓存：跨页面存活，避免每次进入列表重算 ---------------- */

private object DeriveCache {
    private val map = java.util.concurrent.ConcurrentHashMap<String, Any>()
    private val order = java.util.concurrent.ConcurrentLinkedQueue<String>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String, block: () -> T): T {
        (map[key] as? T)?.let { return it }
        val v = block()
        if (map.putIfAbsent(key, v) == null) {
            order.add(key)
            while (order.size > 24) {
                val old = order.poll() ?: break
                map.remove(old)
            }
        }
        return v
    }
}

/** 库数据指纹：记录数、最近修改时间、id 与收藏位哈希，任一变化都会让派生缓存失效 */
fun libDataKey(lib: MLibrary): Long {
    var h = lib.entries.size.toLong()
    var maxUpd = 0L
    lib.entries.forEach {
        if (it.updatedAt > maxUpd) maxUpd = it.updatedAt
        h = h * 31 + it.id.hashCode() + if (it.favorite) 1 else 0
    }
    return h * 1_000_003L + maxUpd
}

/** 电话 → 该电话下出现过的姓名集合（用于识别家庭组） */
fun phoneNamesOf(lib: MLibrary): Map<String, Set<String>> =
    DeriveCache.get<Map<String, Set<String>>>("pn|${lib.id}|${libDataKey(lib)}") {
        val pf = phoneFieldOf(lib)
        val nf = nameFieldOf(lib)
        val map = HashMap<String, MutableSet<String>>()
        if (pf != null && nf != null) {
            lib.entries.forEach { e ->
                val p = normPhone(e.values[pf.id])
                if (p.isEmpty()) return@forEach
                val n = nameOf(lib, e)
                if (n.isNotEmpty()) map.getOrPut(p) { mutableSetOf() }.add(n)
            }
        }
        map
    }

fun familyCountOf(lib: MLibrary): Int =
    DeriveCache.get("fc|${lib.id}|${libDataKey(lib)}") {
        val pf = phoneFieldOf(lib)
        if (pf == null || nameFieldOf(lib) == null) 0
        else {
            val names = phoneNamesOf(lib)
            lib.entries.count { e -> (names[normPhone(e.values[pf.id])]?.size ?: 0) > 1 }
        }
    }

/** 记录模式列表：筛选 + 排序，结果按库指纹缓存。
 *  sort 与客户视图共用一套键：last / total / avg / count + _asc / _desc，
 *  total / avg / count 取该记录所属客户的聚合值，未聚合到客户的记录排最后。 */
fun listView(lib: MLibrary, sort: String, query: String, filter: String, by: String = "phone"): List<MEntry> =
    DeriveCache.get<List<MEntry>>("lv|${lib.id}|${libDataKey(lib)}|$sort|$query|$filter|$by") {
        val pf = phoneFieldOf(lib)
        val names = phoneNamesOf(lib)
        val filtered = lib.entries.filter { entryMatches(lib, it, query) }
            .let { list ->
                when (filter) {
                    "fav" -> list.filter { it.favorite }
                    "family" -> list.filter { e -> pf != null && (names[normPhone(e.values[pf.id])]?.size ?: 0) > 1 }
                    else -> list
                }
            }
        val asc = sort.endsWith("_asc")
        val field = sort.substringBeforeLast('_')
        if (field == "total" || field == "avg" || field == "count") {
            val stats = custStatIndex(lib, by)
            fun valueOf(e: MEntry): Double {
                val g = stats[custGroupKeyOf(lib, e, by)] ?: return if (asc) Double.MAX_VALUE else -1.0
                return when (field) {
                    "total" -> g.total
                    "avg" -> g.total / g.count.coerceAtLeast(1)
                    else -> g.count.toDouble()
                }
            }
            return@get if (asc) {
                filtered.sortedWith(compareBy<MEntry> { valueOf(it) }.thenByDescending { it.updatedAt })
            } else {
                filtered.sortedWith(compareByDescending<MEntry> { valueOf(it) }.thenByDescending { it.updatedAt })
            }
        }
        if (asc) {
            filtered.sortedWith(compareBy<MEntry> { visitDate(lib, it) ?: Long.MAX_VALUE }.thenByDescending { it.updatedAt })
        } else {
            filtered.sortedWith(compareByDescending<MEntry> { visitDate(lib, it) ?: 0L }.thenByDescending { it.updatedAt })
        }
    }

/** 记录所属客户的聚合键（电话规范化 / 姓名），无聚合字段时返回空串 */
fun custGroupKeyOf(lib: MLibrary, entry: MEntry, by: String): String = when (by) {
    "phone" -> phoneFieldOf(lib)?.let { normPhone(entry.values[it.id]) }.orEmpty()
    "name" -> nameOf(lib, entry)
    else -> ""
}

/** 客户级指标索引：累计消费 / 客单价 / 次数，按库指纹缓存 */
fun custStatIndex(lib: MLibrary, by: String): Map<String, CustomerGroup> =
    DeriveCache.get("csi|${lib.id}|${libDataKey(lib)}|$by") {
        customerGroupsCalc(lib, by, "", false, "last_desc").associateBy { it.key }
    }

/** 客户聚合（带缓存） */
fun customerGroups(lib: MLibrary, by: String, query: String = "", overdueFirst: Boolean = true, sortKey: String = "last_desc"): List<CustomerGroup> =
    DeriveCache.get("cg|${lib.id}|${libDataKey(lib)}|$by|$query|$overdueFirst|$sortKey") {
        customerGroupsCalc(lib, by, query, overdueFirst, sortKey)
    }

/* ---------------- 客户经营看板 ---------------- */

data class CustomerStats(
    val records: Int,
    val customers: Int,
    val totalRevenue: Double,
    val avgOrder: Double,
    val thisMonthNew: Int,
    val repeatRate: Double,
    val overdueCount: Int,
    val topLens: List<Pair<String, Int>>,
    val useDist: List<Pair<String, Int>>,
    val monthlyRevenue: List<Pair<String, Double>>
)

private fun monthLabel(ts: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    return "${c.get(Calendar.YEAR)}/${c.get(Calendar.MONTH) + 1}"
}

fun customerStats(lib: MLibrary): CustomerStats {
    val pf = phoneFieldOf(lib)
    val mf = moneyFieldOf(lib)
    val uf = useFieldOf(lib)
    val lf = lensFieldOf(lib)

    val phoneFirst = LinkedHashMap<String, Long>()
    val phoneCount = LinkedHashMap<String, Int>()
    var revenue = 0.0
    var paidCount = 0
    lib.entries.forEach { e ->
        mf?.let { firstNumberIn(e.values[it.id])?.let { m -> revenue += m; paidCount++ } }
        pf?.let { f ->
            val p = normPhone(e.values[f.id])
            if (p.isNotEmpty()) {
                val d = entryDate(lib, e)
                if (!phoneFirst.containsKey(p) || d < phoneFirst[p]!!) phoneFirst[p] = d
                phoneCount[p] = (phoneCount[p] ?: 0) + 1
            }
        }
    }
    val customers = phoneFirst.size
    val thisMonthKey = monthLabel(System.currentTimeMillis())
    val thisMonthNew = phoneFirst.count { monthLabel(it.value) == thisMonthKey }
    val repeat = phoneCount.count { it.value >= 2 }
    val repeatRate = if (customers > 0) round2(repeat.toDouble() / customers * 100) else 0.0

    val overdue = customerGroups(lib, "phone").count { it.overdue }

    val topLens = lf?.let { f ->
        lib.entries.mapNotNull { (it.values[f.id] ?: "").trim().takeIf { v -> v.isNotEmpty() } }
            .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(10)
    } ?: emptyList()

    val useDist = uf?.let { f ->
        lib.entries.mapNotNull { (it.values[f.id] ?: "").trim().takeIf { v -> v.isNotEmpty() } }
            .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
    } ?: emptyList()

    // 近 12 个月营收（含本月）
    val monthMap = LinkedHashMap<String, Double>()
    for (i in 11 downTo 0) {
        val c = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
        monthMap["${c.get(Calendar.YEAR)}/${c.get(Calendar.MONTH) + 1}"] = 0.0
    }
    lib.entries.forEach { e ->
        mf?.let { f ->
            val m = firstNumberIn(e.values[f.id]) ?: return@forEach
            val key = monthLabel(entryDate(lib, e))
            if (monthMap.containsKey(key)) monthMap[key] = (monthMap[key] ?: 0.0) + m
        }
    }
    val monthlyRevenue = monthMap.map { it.key to round2(it.value) }

    return CustomerStats(
        records = lib.entries.size,
        customers = customers,
        totalRevenue = round2(revenue),
        avgOrder = if (paidCount > 0) round2(revenue / paidCount) else 0.0,
        thisMonthNew = thisMonthNew,
        repeatRate = repeatRate,
        overdueCount = overdue,
        topLens = topLens,
        useDist = useDist,
        monthlyRevenue = monthlyRevenue
    )
}

/* ---------------- 配镜出库留痕 ---------------- */

data class PickStockView(
    val fieldName: String,
    val model: String,
    val spec: String,
    val qty: Int,
    val stock: Int,
    val srcLibId: String,
    val srcEntryId: String,
    val found: Boolean
)

/** 取出这条记录所有「扣库存」pick（如 OD / OS）关联的镜片库存条目，用于详情页展示。 */
fun pickStockViews(state: MState, lib: MLibrary, entry: MEntry): List<PickStockView> {
    val out = mutableListOf<PickStockView>()
    lib.fields.forEach { f ->
        if (f.type != FieldType.PICK) return@forEach
        val p = f.pick ?: return@forEach
        if (!p.deduct) return@forEach
        val ref = entry.picks[f.id] ?: return@forEach
        val src = libById(state, ref.libId)
        val sku = src?.entries?.firstOrNull { it.id == ref.entryId }
        val mx = if (src != null) matrixCfg(src) else null
        val model = if (sku != null && mx != null) (sku.values[mx.group.id] ?: "") else ""
        val spec = sku?.let { skuTextOf(src!!, it) } ?: ""
        val stock = if (sku != null && mx != null) (sku.values[mx.value.id]?.toIntOrNull() ?: 0) else 0
        out.add(PickStockView(f.name, model, spec, ref.qty, stock, ref.libId, ref.entryId, sku != null))
    }
    return out
}

/** 轻量统计：按电话分组，最近配镜日期已超用途周期的客户数。比 customerGroups 再 count 省一次完整分组+排序。 */
fun overduePhoneCount(lib: MLibrary): Int =
    DeriveCache.get("ov|${lib.id}|${libDataKey(lib)}") { overduePhoneCountCalc(lib) }

private fun overduePhoneCountCalc(lib: MLibrary): Int {
    val pf = phoneFieldOf(lib) ?: return 0
    val uf = useFieldOf(lib)
    val now = System.currentTimeMillis()
    val dayMs = 1000L * 60 * 60 * 24
    val latest = LinkedHashMap<String, Pair<Long, String>>()
    lib.entries.forEach { e ->
        val phone = pf.let { e.values[it.id]?.let { v -> normPhone(v) } }.orEmpty()
        if (phone.isEmpty()) return@forEach
        val last = entryDate(lib, e)
        val prev = latest[phone]
        if (prev == null || last > prev.first) latest[phone] = last to (uf?.let { e.values[it.id] } ?: "")
    }
    return latest.values.count { (last, use) ->
        val months = ((now - last) / (dayMs * 30)).toInt()
        months > reviewMonths(use)
    }
}
