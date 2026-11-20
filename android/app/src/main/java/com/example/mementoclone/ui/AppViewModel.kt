package com.example.mementoclone.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mementoclone.data.*
import com.example.mementoclone.ui.theme.Palette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    var state: MState = store.load().first
        private set

    var rev by mutableIntStateOf(0)
        private set

    val stack = mutableStateListOf<Scr>(Scr.Home)

    var toastText by mutableStateOf<String?>(null)
        private set

    var bk by mutableStateOf(Cloud.configFromJson(store.getBk()))
        private set

    var cloudStatus by mutableStateOf("")
        private set

    var busy by mutableStateOf(false)
        private set

    var dark by mutableStateOf(false)
        private set

    var globalFont by mutableStateOf(14)
        private set

    var snapshotAt by mutableStateOf<Long?>(null)
        private set

    var batchPresets by mutableStateOf<List<BatchPreset>>(emptyList())
        private set

    /** 每个库最近用过的搜索词，最新的在前。 */
    var searchHistory by mutableStateOf<Map<String, List<String>>>(emptyMap())
        private set

    var canUndoBatch by mutableStateOf(false)
        private set

    private var batchUndo: Pair<String, List<StockBackup>>? = null

    private var saveJob: Job? = null
    private var cloudJob: Job? = null

    init {
        val s = store.settings()
        dark = s.optBoolean("darkMode", false)
        globalFont = (s.optInt("listFontSize", 14)).coerceIn(10, 26)
        Palette.apply(dark)
        AppPrefs.listFontSize = globalFont
        snapshotAt = store.snapshot()?.first
        s.optJSONArray("batchPresets")?.let { arr ->
            val list = mutableListOf<BatchPreset>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name", "")
                if (name.isBlank()) continue
                list.add(
                    BatchPreset(
                        name = name,
                        allModels = o.optBoolean("allModels", false),
                        sphMin = o.optString("sphMin", ""),
                        sphMax = o.optString("sphMax", ""),
                        cyl = o.optString("cyl", "all"),
                        qty = o.optString("qty", "")
                    )
                )
            }
            batchPresets = list
        }
        s.optJSONObject("searchHistory")?.let { obj ->
            val map = mutableMapOf<String, List<String>>()
            obj.keys().forEach { libId ->
                val arr = obj.optJSONArray(libId) ?: return@forEach
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val q = arr.optString(i, "").trim()
                    if (q.isNotEmpty()) list.add(q)
                }
                if (list.isNotEmpty()) map[libId] = list
            }
            searchHistory = map
        }
        ensureAllMeta()
        if (stack.isEmpty()) stack.add(Scr.Home)
    }

    private fun persistSettings() {
        val arr = org.json.JSONArray()
        batchPresets.forEach { p ->
            arr.put(
                JSONObject().put("name", p.name).put("allModels", p.allModels)
                    .put("sphMin", p.sphMin).put("sphMax", p.sphMax)
                    .put("cyl", p.cyl).put("qty", p.qty)
            )
        }
        val hist = JSONObject()
        searchHistory.forEach { (libId, list) -> hist.put(libId, org.json.JSONArray(list)) }
        store.saveSettings(
            JSONObject().put("darkMode", dark).put("listFontSize", globalFont)
                .put("batchPresets", arr).put("searchHistory", hist)
        )
    }

    fun addSearchQuery(libId: String, raw: String) {
        val q = raw.trim()
        if (q.length < 1) return
        val old = searchHistory[libId].orEmpty().filter { it != q }
        val list = (listOf(q) + old).take(8)
        if (searchHistory[libId] == list) return
        searchHistory = searchHistory + (libId to list)
        persistSettings()
    }

    fun clearSearchHistory(libId: String) {
        if (!searchHistory.containsKey(libId)) return
        searchHistory = searchHistory - libId
        persistSettings()
    }

    fun setSort(libId: String, sort: String) {
        val lib = lib(libId) ?: return
        lib.sort = sort
        touch()
    }

    fun saveBatchPreset(p: BatchPreset) {
        val list = batchPresets.filter { it.name != p.name }.toMutableList()
        list.add(0, p)
        batchPresets = list.take(8)
        persistSettings()
        toast("已保存预设「${p.name}」")
    }

    fun deleteBatchPreset(name: String) {
        batchPresets = batchPresets.filter { it.name != name }
        persistSettings()
        toast("已删除预设")
    }

    fun setNight(v: Boolean) {
        dark = v
        Palette.apply(v)
        persistSettings()
        rev++
    }

    fun setDefaultFont(v: Int) {
        globalFont = v.coerceIn(10, 26)
        AppPrefs.listFontSize = globalFont
        persistSettings()
        rev++
    }

    /* ---------------- 基础 ---------------- */

    fun lib(id: String?): MLibrary? = libById(state, id)

    fun toast(msg: String) {
        toastText = msg
    }

    fun clearToast() {
        toastText = null
    }

    fun touch() {
        rev++
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350)
            store.commit(state)
        }
        if (bk.auto && bk.url.isNotBlank()) {
            cloudJob?.cancel()
            cloudJob = viewModelScope.launch {
                delay(6000)
                val r = Cloud.upload(bk, exportText())
                if (r.ok) {
                    bk = bk.copy(lastCloudAt = System.currentTimeMillis())
                    store.saveBk(Cloud.configToJson(bk))
                    cloudStatus = "自动备份 · " + SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
                }
            }
        }
    }

    fun saveNow() {
        saveJob?.cancel()
        store.commit(state)
    }

    override fun onCleared() {
        super.onCleared()
        saveJob?.cancel()
        store.commit(state)
    }

    private fun ensureAllMeta() {
        state.libraries.forEach { l ->
            if (l.icon.isBlank()) l.icon = "database"
            l.color = Palettes.color(l.color)
            if (l.listFields.isEmpty()) l.listFields.addAll(listFieldsOf(l).map { it.id })
            l.fields.forEach { f ->
                if (f.type == FieldType.AUTONUMBER && (f.start ?: 0) <= 0) f.start = 1
            }
        }
    }

    /* ---------------- 导航 ---------------- */

    fun go(scr: Scr) {
        stack.add(scr)
    }

    /** 详情页之间跳转（验光历史、家庭成员、关联记录）只替换栈顶，避免返回要按很多次。 */
    fun openDetail(libId: String, entryId: String) {
        val scr = Scr.Detail(libId, entryId)
        val last = stack.lastOrNull()
        if (last is Scr.Detail) stack[stack.size - 1] = scr else stack.add(scr)
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.size - 1)
        return true
    }

    fun tab(scr: Scr) {
        stack.clear()
        stack.add(scr)
    }

    /* ---------------- 提醒 ---------------- */

    fun dismissReminder(id: String) {
        state.reminders.removeAll { it.id == id }
        touch()
    }

    /* ---------------- 条目 ---------------- */

    fun addEntry(libId: String, values: Map<String, String>, picked: Set<String>): String? {
        val lib = lib(libId) ?: return null
        val entry = MEntry(id = uid("e_"))
        entry.values.putAll(values)
        stampAutoFields(lib, entry, true)
        val changes = applyPickStock(state, lib, entry, values, picked)
        lib.entries.add(entry)
        touch()
        return stockToast(changes) ?: "已保存"
    }

    fun updateEntry(libId: String, entryId: String, values: Map<String, String>, picked: Set<String>): String? {
        val lib = lib(libId) ?: return null
        val entry = lib.entries.firstOrNull { it.id == entryId } ?: return null
        lib.fields.forEach { f ->
            if (f.type == FieldType.FORMULA) return@forEach
            val v = values[f.id] ?: ""
            if (v.isBlank()) entry.values.remove(f.id) else entry.values[f.id] = v
        }
        stampAutoFields(lib, entry, false)
        entry.updatedAt = System.currentTimeMillis()
        val changes = applyPickStock(state, lib, entry, values, picked)
        touch()
        return stockToast(changes) ?: "已保存"
    }

    fun deleteEntry(libId: String, entryId: String) {
        val lib = lib(libId) ?: return
        lib.entries.removeAll { it.id == entryId }
        touch()
        toast("已删除")
    }

    fun toggleFavorite(libId: String, entryId: String) {
        val e = lib(libId)?.entries?.firstOrNull { it.id == entryId } ?: return
        e.favorite = !e.favorite
        touch()
    }

    fun mergeNames(libId: String, entryId: String, targetName: String, keepCurrent: Boolean) {
        val lib = lib(libId) ?: return
        val entry = lib.entries.firstOrNull { it.id == entryId } ?: return
        val pf = phoneFieldOf(lib) ?: return
        val nf = nameFieldOf(lib) ?: return
        val phone = normPhone(entry.values[pf.id])
        val myName = nameOf(lib, entry)
        val fromName = if (keepCurrent) targetName else myName
        val toName = if (keepCurrent) myName else targetName
        var n = 0
        lib.entries.forEach { e ->
            if (normPhone(e.values[pf.id]) != phone) return@forEach
            if (nameOf(lib, e) != fromName) return@forEach
            e.values[nf.id] = toName
            e.updatedAt = System.currentTimeMillis()
            n++
        }
        touch()
        toast("已把 $n 条记录合并到「$toName」")
    }

    /* ---------------- 矩阵 ---------------- */

    fun mxSet(libId: String, model: String, sph: Double, cyl: Double, qty: String): String {
        val lib = lib(libId) ?: return ""
        val msg = mxSave(lib, model, sph, cyl, qty)
        touch()
        return msg
    }

    /* ---------------- 库与字段 ---------------- */

    fun createLibrary(name: String, fieldsCsv: String, icon: String, color: String): MLibrary? {
        val n = name.trim()
        if (n.isEmpty()) return null
        val names = fieldsCsv.split(Regex("[,，]")).map { it.trim() }.filter { it.isNotEmpty() }
        val lib = MLibrary(id = uid("lib_"), name = n, icon = icon.ifBlank { "database" }, color = Palettes.color(color))
        (if (names.isEmpty()) listOf("名称") else names).forEach { fn ->
            lib.fields.add(MField(uid("f_"), fn, FieldType.TEXT))
        }
        state.libraries.add(lib)
        touch()
        toast("已创建「$n」")
        return lib
    }

    fun updateLibrary(libId: String, name: String, icon: String, color: String, fontSize: Int? = null) {
        val lib = lib(libId) ?: return
        if (name.isNotBlank()) lib.name = name.trim()
        lib.icon = icon.ifBlank { "database" }
        lib.color = Palettes.color(color)
        fontSize?.let { lib.listFontSize = it.coerceIn(8, 30) }
        touch()
        toast("已保存")
    }

    fun setListShowNames(libId: String, show: Boolean) {
        val lib = lib(libId) ?: return
        lib.listShowNames = show
        touch()
    }

    fun deleteLibrary(libId: String) {
        state.libraries.removeAll { it.id == libId }
        touch()
        toast("已删除数据库")
    }

    fun addField(libId: String, field: MField): Boolean {
        val lib = lib(libId) ?: return false
        if (field.name.isBlank()) return false
        lib.fields.add(field)
        touch()
        toast("已添加字段")
        return true
    }

    fun updateField(libId: String, fieldId: String, field: MField): Boolean {
        val lib = lib(libId) ?: return false
        val idx = lib.fields.indexOfFirst { it.id == fieldId }
        if (idx < 0) return false
        val old = lib.fields[idx]
        if (old.type == FieldType.FORMULA && field.type != FieldType.FORMULA) {
            lib.entries.forEach { it.values.remove(old.id) }
        }
        lib.fields[idx] = field
        touch()
        toast("字段已保存")
        return true
    }

    fun deleteField(libId: String, fieldId: String) {
        val lib = lib(libId) ?: return
        lib.fields.removeAll { it.id == fieldId }
        lib.entries.forEach { it.values.remove(fieldId) }
        lib.listFields.remove(fieldId)
        touch()
        toast("字段已删除")
    }

    fun moveField(libId: String, fieldId: String, dir: Int) {
        val lib = lib(libId) ?: return
        val i = lib.fields.indexOfFirst { it.id == fieldId }
        val j = i + dir
        if (i < 0 || j < 0 || j >= lib.fields.size) return
        val tmp = lib.fields[i]
        lib.fields[i] = lib.fields[j]
        lib.fields[j] = tmp
        touch()
    }

    fun renumberField(libId: String, fieldId: String) {
        val lib = lib(libId) ?: return
        val f = lib.fields.firstOrNull { it.id == fieldId } ?: return
        if (f.type != FieldType.AUTONUMBER) return
        val list = lib.entries.sortedBy { visitDate(lib, it) ?: it.createdAt }
        var n = (f.start ?: 1).coerceAtLeast(1)
        list.forEach { it.values[f.id] = (n++).toString() }
        touch()
        toast("已重新编号 ${list.size} 条")
    }

    fun setListFields(libId: String, ids: List<String>) {
        val lib = lib(libId) ?: return
        lib.listFields.clear()
        lib.listFields.addAll(ids)
        touch()
    }

    fun setView(libId: String, view: String) {
        val lib = lib(libId) ?: return
        lib.view = view
        touch()
    }

    /* ---------------- 型号改名 / 批量改库存 ---------------- */

    fun renameModelTo(libId: String, old: String, newName: String): Int {
        val lib = lib(libId) ?: return 0
        val n = renameModel(lib, old, newName)
        if (n > 0) touch()
        return n
    }

    fun batchPreviewCount(libId: String, f: BatchFilter): Int {
        val lib = lib(libId) ?: return 0
        return batchCandidates(lib, f).size
    }

    fun batchStock(libId: String, f: BatchFilter, qty: String): Int {
        val lib = lib(libId) ?: return 0
        val cfg = matrixCfg(lib) ?: return 0
        val candidates = batchCandidates(lib, f)
        if (candidates.isEmpty()) return 0
        batchUndo = libId to candidates.map { StockBackup(it.id, it.values[cfg.value.id]) }
        canUndoBatch = true
        val n = batchSetStock(lib, f, qty)
        if (n > 0) touch()
        return n
    }

    /** 撤销上一次批量改库存。 */
    fun undoBatch(): Int {
        val u = batchUndo ?: return 0
        val lib = lib(u.first) ?: return 0
        val cfg = matrixCfg(lib) ?: return 0
        var n = 0
        u.second.forEach { b ->
            val e = lib.entries.firstOrNull { it.id == b.entryId } ?: return@forEach
            if (b.value == null) e.values.remove(cfg.value.id) else e.values[cfg.value.id] = b.value
            e.updatedAt = System.currentTimeMillis()
            n++
        }
        batchUndo = null
        canUndoBatch = false
        touch()
        return n
    }

    /* ---------------- 提醒规则 ---------------- */

    fun setDefaultThreshold(libId: String, value: Int) {
        val lib = lib(libId) ?: return
        lib.alerts?.defaultThreshold = value.coerceAtLeast(0)
        touch()
        toast("提醒设置已保存")
    }

    fun saveAlertRule(libId: String, ruleId: String?, rule: AlertRule) {
        val lib = lib(libId) ?: return
        val alerts = lib.alerts ?: return
        if (ruleId == null) {
            alerts.rules.add(rule)
        } else {
            val i = alerts.rules.indexOfFirst { it.id == ruleId }
            if (i >= 0) alerts.rules[i] = rule else alerts.rules.add(rule)
        }
        touch()
        toast("规则已保存")
    }

    fun deleteAlertRule(libId: String, ruleId: String) {
        val lib = lib(libId) ?: return
        lib.alerts?.rules?.removeAll { it.id == ruleId }
        touch()
        toast("规则已删除")
    }

    /* ---------------- CSV ---------------- */

    fun csvExportText(libId: String): String? {
        val lib = lib(libId) ?: return null
        return Csv.export(state, lib)
    }

    fun csvExportName(libId: String): String {
        val lib = lib(libId)
        val base = (lib?.name ?: "数据").replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
        return "$base-$d.csv"
    }

    fun csvImport(libId: String, text: String, mode: String): Csv.ImportResult? {
        val lib = lib(libId) ?: return null
        val result = Csv.import(lib, text, mode)
        touch()
        return result
    }

    /* ---------------- 备份 ---------------- */

    fun exportText(): String = backupPayload(state).toString()

    fun exportName(): String {
        val d = SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(Date())
        return "mydb-backup-$d.json"
    }

    fun backupSizeText(): String {
        val bytes = exportText().toByteArray(Charsets.UTF_8).size
        return if (bytes < 1024) "$bytes B" else String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    }

    fun importText(text: String): Boolean {
        return try {
            val s = parseBackup(text)
            if (s.libraries.isEmpty()) return false
            store.saveSnapshot(state.toJson().toString())
            snapshotAt = System.currentTimeMillis()
            state = s
            ensureAllMeta()
            stack.clear()
            stack.add(Scr.Home)
            touch()
            saveNow()
            true
        } catch (_: Exception) {
            false
        }
    }

    /** 撤销上一次「从文件恢复 / 从云端恢复」。 */
    fun undoImport(): Boolean {
        val snap = store.snapshot() ?: return false
        return try {
            val s = parseState(JSONObject(snap.second))
            if (s.libraries.isEmpty()) return false
            state = s
            ensureAllMeta()
            stack.clear()
            stack.add(Scr.Home)
            touch()
            saveNow()
            store.clearSnapshot()
            snapshotAt = null
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearSnapshot() {
        store.clearSnapshot()
        snapshotAt = null
    }

    fun markLocalExport() {
        bk = bk.copy(lastLocalAt = System.currentTimeMillis())
        store.saveBk(Cloud.configToJson(bk))
        rev++
    }

    fun saveBkConfig(url: String, port: String, user: String, pass: String, path: String, auto: Boolean) {
        bk = bk.copy(
            url = url.trim(), port = port.trim(), user = user.trim(), pass = pass,
            path = path.trim().ifBlank { "MyDB" }, auto = auto
        )
        store.saveBk(Cloud.configToJson(bk))
        cloudStatus = "设置已保存"
    }

    fun uploadNow() {
        if (busy) return
        busy = true
        cloudStatus = "正在上传…"
        viewModelScope.launch {
            val r = Cloud.upload(bk, exportText())
            busy = false
            cloudStatus = r.message + " · " + SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
            if (r.ok) {
                bk = bk.copy(lastCloudAt = System.currentTimeMillis())
                store.saveBk(Cloud.configToJson(bk))
            }
            toast(r.message)
        }
    }

    fun restoreNow() {
        if (busy) return
        busy = true
        cloudStatus = "正在读取云端…"
        viewModelScope.launch {
            val (r, text) = Cloud.download(bk)
            busy = false
            if (!r.ok || text == null) {
                cloudStatus = r.message
                toast(r.message)
                return@launch
            }
            if (importText(text)) {
                cloudStatus = "已从云端恢复 · " + SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
                toast("已从云端恢复")
            } else {
                cloudStatus = "云端文件格式不正确"
                toast("云端文件格式不正确")
            }
        }
    }

    fun noteCloud(s: String) {
        cloudStatus = s
    }

    fun bkJson(): JSONObject = Cloud.configToJson(bk)
}
