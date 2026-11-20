package com.example.mementoclone.data

data class CsvTable(val headers: List<String>, val rows: List<List<String>>)

object Csv {
    fun parse(text: String): CsvTable {
        var s = text
        if (s.startsWith("\uFEFF")) s = s.substring(1)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        fun endCell() {
            row.add(cell.toString())
            cell.setLength(0)
        }
        fun endRow() {
            endCell()
            if (row.any { it.isNotBlank() }) rows.add(row)
            row = mutableListOf()
        }
        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') {
                        cell.append('"'); i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> endCell()
                    '\r' -> {
                        if (i + 1 < s.length && s[i + 1] == '\n') i++
                        endRow()
                    }
                    '\n' -> endRow()
                    else -> cell.append(c)
                }
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
        if (rows.isEmpty()) return CsvTable(emptyList(), emptyList())
        return CsvTable(rows.first(), rows.drop(1))
    }

    fun escape(v: String?): String {
        val s = v ?: ""
        return if (s.contains(',') || s.contains('"') || s.contains('\n') || s.contains('\r')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
    }

    fun exportFields(lib: MLibrary): List<MField> = lib.fields.filter {
        it.type !in listOf(FieldType.FORMULA, FieldType.AUTONUMBER, FieldType.CREATEDAT, FieldType.UPDATEDAT)
    }

    fun export(state: MState, lib: MLibrary): String {
        val fields = exportFields(lib)
        val sb = StringBuilder("\uFEFF")
        sb.append(fields.joinToString(",") { escape(it.name) }).append("\r\n")
        lib.entries.forEach { e ->
            sb.append(fields.joinToString(",") { escape(e.values[it.id] ?: "") }).append("\r\n")
        }
        return sb.toString()
    }

    private fun inferType(header: String, rows: List<List<String>>, idx: Int): String {
        if (Regex("电话|手机").containsMatchIn(header)) return FieldType.TEL
        if (Regex("备注|说明|地址").containsMatchIn(header)) return FieldType.LONGTEXT
        val vals = rows.mapNotNull { it.getOrNull(idx)?.trim()?.takeIf(String::isNotEmpty) }
        if (vals.isNotEmpty() && vals.all { Regex("^-?\\d+(\\.\\d+)?$").matches(it.replace(",", "")) }) return FieldType.NUMBER
        return FieldType.TEXT
    }

    private fun identityKey(lib: MLibrary, values: Map<String, String>): String {
        val name = nameFieldOf(lib)?.let { values[it.id] }?.trim().orEmpty()
        val phone = phoneFieldOf(lib)?.let { normPhone(values[it.id]) }.orEmpty()
        val date = dateFieldOf(lib)?.let { values[it.id] }?.trim().orEmpty()
        if (name.isNotEmpty() || phone.isNotEmpty() || date.isNotEmpty()) return "$name\u0001$phone\u0001$date"
        return values.values.joinToString("\u0001")
    }

    data class ImportResult(val added: Int, val skipped: Int, val newFields: Int)

    data class ImportPreview(
        val headers: List<String>,
        val matched: List<String>,
        val newFields: List<String>,
        val rows: Int
    )

    fun preview(lib: MLibrary, text: String): ImportPreview {
        val table = parse(text)
        val matched = mutableListOf<String>()
        val added = mutableListOf<String>()
        table.headers.forEach { h ->
            val name = h.trim()
            if (name.isEmpty()) return@forEach
            if (lib.fields.any { it.name.trim() == name }) matched.add(name) else added.add(name)
        }
        return ImportPreview(table.headers, matched, added, table.rows.size)
    }

    fun import(lib: MLibrary, text: String, mode: String): ImportResult {
        val table = parse(text)
        if (table.headers.isEmpty()) return ImportResult(0, 0, 0)
        val mapping = arrayOfNulls<MField>(table.headers.size)
        var addedFields = 0
        table.headers.forEachIndexed { i, h ->
            val name = h.trim()
            if (name.isEmpty()) return@forEachIndexed
            val hit = lib.fields.firstOrNull { it.name.trim() == name }
            if (hit != null) {
                mapping[i] = hit
            } else {
                val f = MField(uid("f_"), name, inferType(name, table.rows, i))
                lib.fields.add(f)
                mapping[i] = f
                addedFields++
            }
        }
        if (mode == "replace") lib.entries.clear()
        val seen = HashSet<String>()
        if (mode == "skip") lib.entries.forEach { seen.add(identityKey(lib, it.values)) }
        val created = mutableListOf<MEntry>()
        var skipped = 0
        table.rows.forEach { cols ->
            if (cols.all { it.isBlank() }) return@forEach
            val values = linkedMapOf<String, String>()
            mapping.forEachIndexed { i, f ->
                if (f == null || f.type == FieldType.FORMULA) return@forEachIndexed
                val v = cols.getOrNull(i)?.trim().orEmpty()
                if (v.isNotEmpty()) values[f.id] = v
            }
            if (values.isEmpty()) return@forEach
            if (mode == "skip") {
                val key = identityKey(lib, values)
                if (seen.contains(key)) {
                    skipped++
                    return@forEach
                }
                seen.add(key)
            }
            created.add(MEntry(id = uid("e_"), createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), values = values))
        }
        if (created.isNotEmpty()) {
            lib.fields.filter { it.type == FieldType.AUTONUMBER }.forEach { f ->
                var n = nextAutoNumber(lib, f)
                created.forEach { it.values[f.id] = (n++).toString() }
            }
            lib.fields.filter { it.type == FieldType.CREATEDAT }.forEach { f ->
                created.forEach { e -> e.values[f.id] = fmtDay(e.createdAt) }
            }
            lib.fields.filter { it.type == FieldType.UPDATEDAT }.forEach { f ->
                created.forEach { e -> e.values[f.id] = fmtStamp(e.updatedAt) }
            }
        }
        lib.entries.addAll(0, created)
        return ImportResult(created.size, skipped, addedFields)
    }
}
