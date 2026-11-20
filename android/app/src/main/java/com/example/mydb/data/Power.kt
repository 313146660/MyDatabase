package com.example.mydb.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val NUM = Regex("-?\\d+(?:\\.\\d+)?")

fun firstNumberIn(v: String?): Double? = NUM.find(v ?: "")?.value?.toDoubleOrNull()

/** -2.75ds → -2.75；+1.25 → 1.25；空 → null */
fun toPower(v: String?): Double? {
    val s = (v ?: "").replace('\u2212', '-').replace('\u2013', '-').replace('\u2014', '-')
    if (s.isBlank()) return null
    val m = NUM.find(s) ?: return null
    return m.value.toDoubleOrNull()?.let { Math.round(it * 100) / 100.0 }
}

fun fmtPower(n: Double?): String {
    if (n == null || n.isNaN() || n.isInfinite()) return "—"
    if (n == 0.0) return "0"
    val sign = if (n > 0) "+" else ""
    return sign + String.format(Locale.US, "%.2f", n)
}

fun powerText(sph: Double?, cyl: Double?): String {
    if (sph == null) return ""
    val base = fmtPower(sph) + "ds"
    return if (cyl != null && cyl != 0.0) base + "/" + fmtPower(cyl) + "dc" else base
}

/** 比较用：忽略 ds/dc、空格和全角符号，只留数字与符号 */
fun normPower(v: String?): String {
    var s = (v ?: "").replace('\u2212', '-').replace('\u2013', '-').replace('\u2014', '-')
    s = s.replace(Regex("\\s+"), "")
    s = s.replace(Regex("(?i)ds|dc"), "")
    s = s.replace(Regex("[^0-9+\\-./]"), "")
    return s
}

fun sphOf(v: String?): Double? {
    val s = v ?: ""
    Regex("(-?\\d+(?:\\.\\d+)?)\\s*ds", RegexOption.IGNORE_CASE).find(s)?.let { return it.groupValues[1].toDoubleOrNull() }
    if (Regex("(-?\\d+(?:\\.\\d+)?)\\s*dc", RegexOption.IGNORE_CASE).containsMatchIn(s)) return 0.0
    return firstNumberIn(s)
}

fun cylOf(v: String?): Double {
    val m = Regex("(-?\\d+(?:\\.\\d+)?)\\s*dc", RegexOption.IGNORE_CASE).find(v ?: "")
    return m?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
}

fun seOf(v: String?): Double? {
    val s = sphOf(v) ?: return null
    return Math.round((s + cylOf(v) / 2) * 100) / 100.0
}

fun fmtSigned(x: Double?, digits: Int = 2): String {
    if (x == null || x.isNaN() || x.isInfinite()) return "—"
    val sign = if (x > 0) "+" else ""
    return sign + String.format(Locale.US, "%.${digits}f", x)
}

fun parseDate(v: String?): Long? {
    val s = (v ?: "").trim()
    if (s.isEmpty()) return null
    val m = Regex("^(\\d{4})\\s*[-/年.]\\s*(\\d{1,2})\\s*[-/月.]\\s*(\\d{1,2})").find(s)
    if (m != null) {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(m.groupValues[1].toInt(), m.groupValues[2].toInt() - 1, m.groupValues[3].toInt())
        return cal.timeInMillis
    }
    val formats = listOf("yyyy/M/d", "yyyy-MM-dd", "yyyy.MM.dd", "yyyy/M/d HH:mm", "yyyy-MM-dd HH:mm")
    for (f in formats) {
        try {
            val d = SimpleDateFormat(f, Locale.CHINA).parse(s) ?: continue
            return d.time
        } catch (_: Exception) {
        }
    }
    return null
}

fun fmtDay(ts: Long): String = SimpleDateFormat("yyyy/M/d", Locale.CHINA).format(Date(ts))

fun fmtStamp(ts: Long): String = SimpleDateFormat("yyyy/M/d HH:mm", Locale.CHINA).format(Date(ts))

fun fmtClock(ts: Long): String = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(ts))

fun oneLine(v: String?): String = (v ?: "").replace(Regex("\\s*\\r?\\n\\s*"), " / ")

fun relTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000} 小时前"
        diff < 2 * 86_400_000L -> "昨天"
        diff < 30 * 86_400_000L -> "${diff / 86_400_000L} 天前"
        else -> SimpleDateFormat("M/d", Locale.CHINA).format(Date(ts))
    }
}

fun round2(x: Double): Double = Math.round(x * 100) / 100.0

fun todayCn(): String {
    val d = Calendar.getInstance()
    val week = listOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")[d.get(Calendar.DAY_OF_WEEK) - 1]
    return "${d.get(Calendar.MONTH) + 1}月${d.get(Calendar.DAY_OF_MONTH)}日 $week"
}

/* ---------------- 公式 ---------------- */

private fun fieldValueOf(state: MState, lib: MLibrary, entry: MEntry, name: String): String {
    val f = lib.fields.firstOrNull { it.name.trim() == name.trim() } ?: return ""
    if (f.type == FieldType.FORMULA) return evalFormula(state, lib, entry, f.expr ?: "")
    return entry.values[f.id] ?: ""
}

fun evalFormula(state: MState, lib: MLibrary, entry: MEntry, expr: String): String {
    if (expr.isBlank()) return ""
    return try {
        val v = FormulaParser(expr) { name -> fieldValueOf(state, lib, entry, name) }.parse()
        when {
            v == null -> ""
            v is String -> v
            v is Boolean -> if (v) "是" else "否"
            v is Double -> fmtSigned(v)
            else -> v.toString()
        }
    } catch (_: Exception) {
        ""
    }
}

/** 支持原型里用到的公式子集：字符串 + 拼接、字段名、fmt/se/sph/cyl/num/text/round/cond/abs */
private class FormulaParser(val src: String, val lookup: (String) -> String) {
    var i = 0

    fun parse(): Any? {
        val v = expr()
        return v
    }

    private fun ws() {
        while (i < src.length && src[i].isWhitespace()) i++
    }

    private fun peek(): Char? {
        ws()
        return if (i < src.length) src[i] else null
    }

    private fun expr(): Any? {
        var left = term()
        while (true) {
            ws()
            if (i < src.length && src[i] == '+') {
                i++
                val right = term()
                left = when {
                    left is String || right is String -> str(left) + str(right)
                    left is Double && right is Double -> left + right
                    left == null -> right
                    right == null -> left
                    else -> str(left) + str(right)
                }
            } else break
        }
        return left
    }

    private fun term(): Any? {
        ws()
        if (i >= src.length) return null
        val c = src[i]
        if (c == '\'' || c == '"') return stringLit(c)
        if (c == '(') {
            i++
            val v = expr()
            ws()
            if (i < src.length && src[i] == ')') i++
            return v
        }
        if (c.isDigit() || c == '-' || c == '+') return number()
        if (c.isLetter() || c == '_') return ident()
        i++
        return null
    }

    private fun stringLit(quote: Char): String {
        i++
        val sb = StringBuilder()
        while (i < src.length && src[i] != quote) {
            if (src[i] == '\\' && i + 1 < src.length) i++
            sb.append(src[i])
            i++
        }
        if (i < src.length) i++
        return sb.toString()
    }

    private fun number(): Double? {
        val start = i
        if (src[i] == '-' || src[i] == '+') i++
        while (i < src.length && (src[i].isDigit() || src[i] == '.')) i++
        return src.substring(start, i).toDoubleOrNull()
    }

    private fun ident(): Any? {
        val start = i
        while (i < src.length && (src[i].isLetterOrDigit() || src[i] == '_')) i++
        val name = src.substring(start, i)
        ws()
        if (i < src.length && src[i] == '(') {
            i++
            val args = mutableListOf<Any?>()
            ws()
            if (i < src.length && src[i] != ')') {
                while (true) {
                    args.add(expr())
                    ws()
                    if (i < src.length && src[i] == ',') {
                        i++
                        continue
                    }
                    break
                }
            }
            ws()
            if (i < src.length && src[i] == ')') i++
            return call(name, args)
        }
        return when (name) {
            "true" -> true
            "false" -> false
            else -> lookup(name)
        }
    }

    private fun call(name: String, args: List<Any?>): Any? {
        fun argStr(n: Int): String = str(args.getOrNull(n))
        fun argNum(n: Int): Double? = when (val v = args.getOrNull(n)) {
            is Double -> v
            is String -> toPower(v)
            else -> null
        }
        return when (name) {
            "num" -> firstNumberIn(lookup(argStr(0)))
            "sph" -> sphOf(lookup(argStr(0)))
            "cyl" -> cylOf(lookup(argStr(0)))
            "se" -> seOf(lookup(argStr(0)))
            "text" -> lookup(argStr(0))
            "fmt" -> fmtSigned(argNum(0), (argNum(1) ?: 2.0).toInt())
            "round" -> argNum(0)?.let { r -> val p = Math.pow(10.0, argNum(1) ?: 2.0); Math.round(r * p) / p }
            "cond" -> if (args.getOrNull(0) == true) args.getOrNull(1) else args.getOrNull(2)
            "abs" -> argNum(0)?.let { Math.abs(it) }
            else -> null
        }
    }

    private fun str(v: Any?): String = when (v) {
        null -> ""
        is String -> v
        is Double -> if (v == Math.floor(v)) v.toLong().toString() else v.toString()
        else -> v.toString()
    }
}
