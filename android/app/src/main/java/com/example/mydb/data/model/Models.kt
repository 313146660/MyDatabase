package com.example.mydb.data

import org.json.JSONArray
import org.json.JSONObject

/* ---------------- 字段类型 ---------------- */
object FieldType {
    const val TEXT = "text"
    const val LONGTEXT = "longtext"
    const val NUMBER = "number"
    const val PERCENT = "percent"
    const val TEL = "tel"
    const val DATE = "date"
    const val REMINDER = "reminder"
    const val SELECT = "select"
    const val MULTISELECT = "multiselect"
    const val CHECKBOX = "checkbox"
    const val PICK = "pick"
    const val AUTONUMBER = "autonumber"
    const val CREATEDAT = "createdat"
    const val UPDATEDAT = "updatedat"
    const val FORMULA = "formula"

    /** 分组展示：字段表单用网格选择，类型再多也不用一长排翻找 */
    val groups: List<Pair<String, List<Pair<String, String>>>> = listOf(
        "基础" to listOf(
            TEXT to "文本", LONGTEXT to "长文本", NUMBER to "数字", PERCENT to "进度",
            TEL to "电话", DATE to "日期", REMINDER to "提醒"
        ),
        "选项" to listOf(
            SELECT to "单选", MULTISELECT to "多选", CHECKBOX to "是 / 否"
        ),
        "高级" to listOf(
            PICK to "关联选择", AUTONUMBER to "自动编号", CREATEDAT to "创建时间",
            UPDATEDAT to "更新时间", FORMULA to "公式"
        )
    )

    val all: List<Pair<String, String>> = groups.flatMap { it.second }

    fun label(value: String): String = all.firstOrNull { it.first == value }?.second ?: "文本"

    fun isValid(value: String): Boolean = all.any { it.first == value }
}

/* ---------------- 调色板 / 图标 ---------------- */
data class Palette(val value: String, val name: String)
data class IconChoice(val value: String, val name: String)

object Palettes {
    val colors = listOf(
        Palette("#1976D2", "经典蓝"), Palette("#0F9D8A", "青碧"),
        Palette("#7C4DFF", "灵感紫"), Palette("#E05252", "珊瑚红"),
        Palette("#F29B38", "暖橙"), Palette("#546E7A", "石墨")
    )
    val icons = listOf(
        IconChoice("users", "联系人"), IconChoice("box", "物品"),
        IconChoice("doc", "文档"), IconChoice("heart", "收藏"),
        IconChoice("calendar", "日程"), IconChoice("database", "数据")
    )
    fun color(value: String?): String =
        if (value != null && colors.any { it.value == value }) value else colors[0].value
}

/* ---------------- 数据模型 ---------------- */
data class PickCfg(
    val libId: String,
    val kind: String = "value",       // value | sku
    val fieldId: String = "",
    val filterFieldId: String = "",
    val deduct: Boolean = false,
    val qty: Int = 1
)

data class PickRef(
    val libId: String,
    val entryId: String,
    val qty: Int = 1
)

data class MField(
    val id: String,
    var name: String,
    var type: String = FieldType.TEXT,
    var unit: String? = null,
    var required: Boolean = false,
    var options: MutableList<String> = mutableListOf(),
    var expr: String? = null,
    var prefix: String? = null,
    var start: Int? = null,
    var system: Boolean = false,
    var pick: PickCfg? = null
)

data class MEntry(
    val id: String,
    var favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val values: MutableMap<String, String> = linkedMapOf(),
    var picks: MutableMap<String, PickRef> = linkedMapOf()
)

data class AxisRange(val sph: List<String> = emptyList(), val cyl: List<String> = emptyList())

data class MatrixCfg(
    val group: String,
    val row: String,
    val col: String,
    val value: String,
    val ranges: MutableMap<String, AxisRange> = linkedMapOf()
)

data class AlertRule(
    val id: String,
    var model: String = "",
    var sphMin: String = "",
    var sphMax: String = "",
    var cyl: String = "",
    var threshold: Int = 0
)

data class AlertsCfg(
    var field: String,
    var defaultThreshold: Int = 0,
    val rules: MutableList<AlertRule> = mutableListOf()
)

/** 某个型号的补货目标库存：单光和带散光分开。 */
data class RestockTarget(var plain: Int = 4, var cyl: Int = 2)

/** 某个型号的度数范围补货目标：命中区间的规格用这里的目标覆盖型号默认目标。
 *  plain / cylTarget 为 -1 表示不覆盖该类（回退到型号目标 / 默认）。cyl: all / plain / has。 */
data class RestockRangeRule(
    val id: String,
    var model: String = "",
    var sphMin: String = "",
    var sphMax: String = "",
    var cyl: String = "all",
    var plain: Int = -1,
    var cylTarget: Int = -1
)

/** 补货清单配置：默认目标、补货范围、各型号单独目标、度数范围规则、供货商。 */
data class RestockCfg(
    var defaultPlain: Int = 4,
    var defaultCyl: Int = 2,
    var scope: String = "below_target",
    val targets: MutableMap<String, RestockTarget> = linkedMapOf(),
    val ranges: MutableList<RestockRangeRule> = mutableListOf(),
    val suppliers: MutableMap<String, String> = linkedMapOf()
)

data class MLibrary(
    val id: String,
    var name: String,
    var icon: String = "database",
    var color: String = "#1976D2",
    var view: String? = null,
    var matrix: MatrixCfg? = null,
    var alerts: AlertsCfg? = null,
    var restock: RestockCfg? = null,
    var listFields: MutableList<String> = mutableListOf(),
    val fields: MutableList<MField> = mutableListOf(),
    val entries: MutableList<MEntry> = mutableListOf(),
    var listShowNames: Boolean = true,
    var listFont: String? = null,
    var listFontSize: Int? = null,
    var sort: String? = null
)

data class MReminder(
    val id: String,
    var read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    var message: String = ""
)

data class MState(
    val libraries: MutableList<MLibrary> = mutableListOf(),
    val reminders: MutableList<MReminder> = mutableListOf()
) {
    fun lib(id: String?): MLibrary? = libraries.firstOrNull { it.id == id }
    fun field(lib: MLibrary?, id: String?): MField? = lib?.fields?.firstOrNull { it.id == id }
}

/* ---------------- JSON ---------------- */
private fun JSONObject.text(key: String): String {
    val v = opt(key)
    return when (v) {
        null, JSONObject.NULL -> ""
        is String -> v
        else -> v.toString()
    }
}

private fun JSONObject.bool(key: String, def: Boolean = false): Boolean {
    val v = opt(key)
    return when (v) {
        is Boolean -> v
        is Number -> v.toInt() != 0
        is String -> v == "1" || v == "true"
        else -> def
    }
}

private fun JSONObject.int(key: String, def: Int): Int {
    val v = opt(key)
    return when (v) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: def
        else -> def
    }
}

private fun JSONArray.strList(): MutableList<String> {
    val out = mutableListOf<String>()
    for (i in 0 until length()) {
        val v = opt(i)
        if (v != null && v != JSONObject.NULL) out.add(v.toString())
    }
    return out
}

fun parseField(o: JSONObject): MField {
    val f = MField(
        id = o.text("id"),
        name = o.text("name"),
        type = o.optString("type", FieldType.TEXT).ifBlank { FieldType.TEXT }
            .let { if (FieldType.isValid(it)) it else FieldType.TEXT },
        unit = o.text("unit").ifBlank { null },
        required = o.bool("required"),
        options = o.optJSONArray("options")?.strList() ?: mutableListOf(),
        expr = o.text("expr").ifBlank { null },
        prefix = o.text("prefix").ifBlank { null },
        start = if (o.has("start")) o.int("start", 1) else null,
        system = o.bool("system")
    )
    o.optJSONObject("pick")?.let { p ->
        f.pick = PickCfg(
            libId = p.text("libId"),
            kind = p.optString("kind", "value"),
            fieldId = p.text("fieldId"),
            filterFieldId = p.text("filterFieldId"),
            deduct = p.bool("deduct"),
            qty = p.int("qty", 1).coerceAtLeast(1)
        )
    }
    return f
}

fun parseEntry(o: JSONObject): MEntry {
    val values = linkedMapOf<String, String>()
    o.optJSONObject("values")?.let { v ->
        for (k in v.keys()) values[k] = v.text(k)
    }
    val picks = linkedMapOf<String, PickRef>()
    o.optJSONObject("picks")?.let { p ->
        for (k in p.keys()) {
            val r = p.optJSONObject(k) ?: continue
            picks[k] = PickRef(r.text("libId"), r.text("entryId"), r.int("qty", 1))
        }
    }
    return MEntry(
        id = o.text("id"),
        favorite = o.bool("favorite"),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
        values = values,
        picks = picks
    )
}

fun parseLibrary(o: JSONObject): MLibrary {
    val fields = mutableListOf<MField>()
    o.optJSONArray("fields")?.let { a ->
        for (i in 0 until a.length()) a.optJSONObject(i)?.let { fields.add(parseField(it)) }
    }
    val entries = mutableListOf<MEntry>()
    o.optJSONArray("entries")?.let { a ->
        for (i in 0 until a.length()) a.optJSONObject(i)?.let { entries.add(parseEntry(it)) }
    }
    val lib = MLibrary(
        id = o.text("id"),
        name = o.text("name"),
        icon = o.text("icon").ifBlank { "database" },
        color = Palettes.color(o.text("color")),
        view = o.text("view").ifBlank { null },
        listFields = o.optJSONArray("listFields")?.strList() ?: mutableListOf(),
        fields = fields,
        entries = entries
    )
    if (o.has("listShowNames")) lib.listShowNames = o.bool("listShowNames")
    lib.listFont = o.text("listFont").ifBlank { null }
    lib.sort = o.text("sort").ifBlank { null }
    if (o.has("listFontSize")) {
        val n = o.optInt("listFontSize", 0)
        if (n > 0) lib.listFontSize = n
    }
    o.optJSONObject("matrix")?.let { m ->
        val ranges = linkedMapOf<String, AxisRange>()
        m.optJSONObject("ranges")?.let { rs ->
            for (k in rs.keys()) {
                val r = rs.optJSONObject(k) ?: continue
                ranges[k] = AxisRange(
                    sph = r.optJSONArray("sph")?.strList() ?: mutableListOf(),
                    cyl = r.optJSONArray("cyl")?.strList() ?: mutableListOf()
                )
            }
        }
        lib.matrix = MatrixCfg(m.text("group"), m.text("row"), m.text("col"), m.text("value"), ranges)
    }
    o.optJSONObject("alerts")?.let { a ->
        val rules = mutableListOf<AlertRule>()
        a.optJSONArray("rules")?.let { ra ->
            for (i in 0 until ra.length()) {
                val r = ra.optJSONObject(i) ?: continue
                rules.add(
                    AlertRule(
                        id = r.text("id"),
                        model = r.text("model"),
                        sphMin = r.text("sphMin"),
                        sphMax = r.text("sphMax"),
                        cyl = r.text("cyl"),
                        threshold = r.int("threshold", 0)
                    )
                )
            }
        }
        lib.alerts = AlertsCfg(a.text("field"), a.int("defaultThreshold", 0), rules)
    }
    o.optJSONObject("restock")?.let { r ->
        val targets = linkedMapOf<String, RestockTarget>()
        r.optJSONObject("targets")?.let { ts ->
            for (k in ts.keys()) {
                val t = ts.optJSONObject(k) ?: continue
                targets[k] = RestockTarget(t.int("plain", 4), t.int("cyl", 2))
            }
        }
        val ranges = mutableListOf<RestockRangeRule>()
        r.optJSONArray("ranges")?.let { rs ->
            for (i in 0 until rs.length()) {
                val o2 = rs.optJSONObject(i) ?: continue
                ranges.add(
                    RestockRangeRule(
                        id = o2.optString("id"),
                        model = o2.optString("model"),
                        sphMin = o2.optString("sphMin"),
                        sphMax = o2.optString("sphMax"),
                        cyl = o2.optString("cyl").ifBlank { "all" },
                        plain = o2.optInt("plain", -1),
                        cylTarget = o2.optInt("cylTarget", -1)
                    )
                )
            }
        }
        val suppliers = linkedMapOf<String, String>()
        r.optJSONObject("suppliers")?.let { ss ->
            for (k in ss.keys()) suppliers[k] = ss.optString(k)
        }
        lib.restock = RestockCfg(
            defaultPlain = r.int("defaultPlain", 4),
            defaultCyl = r.int("defaultCyl", 2),
            scope = r.text("scope").ifBlank { "below_target" },
            targets = targets,
            ranges = ranges,
            suppliers = suppliers
        )
    }
    return lib
}

fun parseState(o: JSONObject): MState {
    val libs = mutableListOf<MLibrary>()
    o.optJSONArray("libraries")?.let { a ->
        for (i in 0 until a.length()) a.optJSONObject(i)?.let { libs.add(parseLibrary(it)) }
    }
    val reminders = mutableListOf<MReminder>()
    o.optJSONArray("reminders")?.let { a ->
        for (i in 0 until a.length()) {
            val r = a.optJSONObject(i) ?: continue
            reminders.add(
                MReminder(
                    id = r.text("id"),
                    read = r.bool("read"),
                    createdAt = r.optLong("createdAt", System.currentTimeMillis()),
                    message = r.text("message")
                )
            )
        }
    }
    return MState(libs, reminders)
}

fun MState.toJson(): JSONObject {
    val root = JSONObject()
    val libs = JSONArray()
    libraries.forEach { lib ->
        val lo = JSONObject()
            .put("id", lib.id)
            .put("name", lib.name)
            .put("icon", lib.icon)
            .put("color", lib.color)
            .put("listFields", JSONArray(lib.listFields))
        if (!lib.listShowNames) lo.put("listShowNames", false)
        lib.listFont?.let { lo.put("listFont", it) }
        lib.listFontSize?.let { lo.put("listFontSize", it) }
        lib.sort?.let { lo.put("sort", it) }
        if (lib.view != null) lo.put("view", lib.view)
        lib.matrix?.let { m ->
            val mo = JSONObject()
                .put("group", m.group).put("row", m.row)
                .put("col", m.col).put("value", m.value)
            val ranges = JSONObject()
            m.ranges.forEach { (k, r) ->
                ranges.put(k, JSONObject().put("sph", JSONArray(r.sph)).put("cyl", JSONArray(r.cyl)))
            }
            mo.put("ranges", ranges)
            lo.put("matrix", mo)
        }
        lib.alerts?.let { a ->
            val ao = JSONObject()
                .put("field", a.field)
                .put("defaultThreshold", a.defaultThreshold)
            val rules = JSONArray()
            a.rules.forEach { r ->
                rules.put(
                    JSONObject()
                        .put("id", r.id).put("model", r.model)
                        .put("sphMin", r.sphMin).put("sphMax", r.sphMax)
                        .put("cyl", r.cyl).put("threshold", r.threshold)
                )
            }
            ao.put("rules", rules)
            lo.put("alerts", ao)
        }
        lib.restock?.let { r ->
            val ro = JSONObject()
                .put("defaultPlain", r.defaultPlain)
                .put("defaultCyl", r.defaultCyl)
                .put("scope", r.scope)
            val ts = JSONObject()
            r.targets.forEach { (k, t) ->
                ts.put(k, JSONObject().put("plain", t.plain).put("cyl", t.cyl))
            }
            ro.put("targets", ts)
            val rs = JSONArray()
            r.ranges.forEach { rr ->
                rs.put(
                    JSONObject()
                        .put("id", rr.id).put("model", rr.model)
                        .put("sphMin", rr.sphMin).put("sphMax", rr.sphMax)
                        .put("cyl", rr.cyl).put("plain", rr.plain).put("cylTarget", rr.cylTarget)
                )
            }
            ro.put("ranges", rs)
            val ss = JSONObject()
            r.suppliers.forEach { (k, v) -> ss.put(k, v) }
            ro.put("suppliers", ss)
            lo.put("restock", ro)
        }
        val fs = JSONArray()
        lib.fields.forEach { f ->
            val fo = JSONObject()
                .put("id", f.id).put("name", f.name).put("type", f.type)
            if (f.unit != null) fo.put("unit", f.unit)
            if (f.required) fo.put("required", true)
            if (f.options.isNotEmpty()) fo.put("options", JSONArray(f.options))
            if (f.expr != null) fo.put("expr", f.expr)
            if (f.prefix != null) fo.put("prefix", f.prefix)
            if (f.start != null) fo.put("start", f.start)
            if (f.system) fo.put("system", true)
            f.pick?.let { p ->
                fo.put(
                    "pick",
                    JSONObject()
                        .put("libId", p.libId).put("kind", p.kind)
                        .put("fieldId", p.fieldId).put("filterFieldId", p.filterFieldId)
                        .put("deduct", p.deduct).put("qty", p.qty)
                )
            }
            fs.put(fo)
        }
        lo.put("fields", fs)
        val es = JSONArray()
        lib.entries.forEach { e ->
            val eo = JSONObject()
                .put("id", e.id)
                .put("favorite", e.favorite)
                .put("createdAt", e.createdAt)
                .put("updatedAt", e.updatedAt)
            val vs = JSONObject()
            e.values.forEach { (k, v) -> vs.put(k, v) }
            eo.put("values", vs)
            if (e.picks.isNotEmpty()) {
                val ps = JSONObject()
                e.picks.forEach { (k, r) ->
                    ps.put(k, JSONObject().put("libId", r.libId).put("entryId", r.entryId).put("qty", r.qty))
                }
                eo.put("picks", ps)
            }
            es.put(eo)
        }
        lo.put("entries", es)
        libs.put(lo)
    }
    root.put("libraries", libs)
    root.put("links", JSONArray())
    val rs = JSONArray()
    reminders.forEach { r ->
        rs.put(JSONObject().put("id", r.id).put("read", r.read).put("createdAt", r.createdAt).put("message", r.message))
    }
    root.put("reminders", rs)
    return root
}

fun backupPayload(state: MState): JSONObject = JSONObject()
    .put("app", "MyDB")
    .put("kind", "backup")
    .put("version", 2)
    .put("schemaVersion", 2)
    .put("exportedAt", System.currentTimeMillis())
    .put("data", state.toJson())

fun parseBackup(text: String): MState {
    val root = JSONObject(text)
    val data = if (root.has("data") && root.optJSONObject("data") != null) root.getJSONObject("data") else root
    return parseState(data)
}
