package com.example.mydb.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mydb.data.*
import com.example.mydb.ui.theme.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    /** 落盘 + 闹钟排程用：不随 viewModelScope / 页面销毁而取消 */
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 启动时先占位，真实数据由 init 中的 IO 协程解析完成后替换。 */
    var state: MState = MState()
        private set

    /** 启动加载中：界面显示加载页，期间禁止落盘以免覆盖真实数据。 */
    var booting by mutableStateOf(true)
        private set

    var rev by mutableIntStateOf(0)
        private set

    val stack = mutableStateListOf<Scr>(Scr.Home)

    var toastText by mutableStateOf<String?>(null)
        private set

    var bk by mutableStateOf(Cloud.configFromJson(JSONObject()))
        private set

    var cloudStatus by mutableStateOf("")
        private set

    var busy by mutableStateOf(false)
        private set

    var dark by mutableStateOf(false)
        private set

    /** 指纹解锁开关：开启后冷启动、或从后台超过 30 秒返回时需要验证指纹。 */
    var lockEnabled by mutableStateOf(false)
        private set

    var locked by mutableStateOf(false)
        private set

    /** 解锁密码（salt:hash），空 = 未设置；作为指纹失效时的兜底 */
    var lockPassword by mutableStateOf("")
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

    /** 补货历史记录（所有库混合，按时间倒序），最多保留 50 条。 */
    var restockHistory by mutableStateOf<List<RestockRecord>>(emptyList())
        private set

    var canUndoBatch by mutableStateOf(false)
        private set

    private var batchUndo: Pair<String, List<StockBackup>>? = null

    private var saveJob: Job? = null
    private var cloudJob: Job? = null

    init {
        if (stack.isEmpty()) stack.add(Scr.Home)
        // 冷启动：prefs 首次读写（含整库 JSON 的 XML 加载）与整库解析全部放到 IO 线程，
        // 完成后一次性应用到 UI 状态，避免主线程等待磁盘 I/O 与 JSON 解析。
        viewModelScope.launch {
            val s = withContext(Dispatchers.IO) { store.settings() }
            val loaded = withContext(Dispatchers.IO) { store.load().first }
            val snapAt = withContext(Dispatchers.IO) { store.snapshot()?.first }
            val bkJson = withContext(Dispatchers.IO) { store.getBk() }

            dark = s.optBoolean("darkMode", false)
            lockEnabled = android.os.Build.VERSION.SDK_INT >= 28 && s.optBoolean("lockEnabled", false)
            // 只保存 salt:SHA-256 摘要，不保存明文
            lockPassword = s.optString("lockPassword", "")
            locked = lockEnabled || lockPassword.isNotEmpty()
            globalFont = (s.optInt("listFontSize", 14)).coerceIn(10, 26)
            Palette.apply(dark)
            AppPrefs.listFontSize = globalFont
            snapshotAt = snapAt
            bk = Cloud.configFromJson(bkJson)
            if (s.has("batchPresets")) {
                val arr = s.optJSONArray("batchPresets")
                val list = mutableListOf<BatchPreset>()
                if (arr != null) {
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
                }
                batchPresets = list
            } else {
                // 首次启动：播种两个内置预设，之后可长按删除，或改完条件用同名保存来覆盖修改
                batchPresets = listOf(
                    BatchPreset("本型号 -4.00~-1.00 → 4", false, "-4.00", "-1.00", "all", "4"),
                    BatchPreset("本型号 有散光 → 2", false, "", "", "has", "2")
                )
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
            restockHistory = restockHistoryFromJson(s.optJSONArray("restockHistory"))

            s.optJSONObject("custViews")?.let { obj ->
                val map = mutableMapOf<String, CustView>()
                obj.keys().forEach { libId ->
                    val o = obj.optJSONObject(libId) ?: return@forEach
                    map[libId] = CustView(
                        mode = o.optBoolean("mode", false),
                        by = o.optString("by", "phone"),
                        pinned = o.optBoolean("pinned", true),
                        sort = o.optString("sort", "last_desc")
                    )
                }
                if (map.isNotEmpty()) custViews = map
            }

            state = loaded
            ensureAllMeta()
            rev++
            booting = false
            scheduleWarm()
            // 冷启动重排「提醒」闹钟（数据可能在别处被改过）
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { Reminders.reschedule(getApplication(), state) }
            }
        }
    }

    /** 后台预热派生缓存（列表 / 聚合），进入列表时直接命中，不占主线程 */
    private var warmJob: Job? = null

    private fun scheduleWarm() {
        warmJob?.cancel()
        val libs = state.libraries
        warmJob = viewModelScope.launch(Dispatchers.Default) {
            delay(200)
            runCatching {
                libs.forEach { lib ->
                    if (lib.entries.isEmpty()) return@forEach
                    phoneNamesOf(lib)
                    familyCountOf(lib)
                    listView(lib, lib.sort ?: "last_desc", "", "all")
                    if (phoneFieldOf(lib) != null) {
                        overduePhoneCount(lib)
                        customerGroups(lib, "phone", "", true, "last_desc")
                    }
                }
            }
        }
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
        val cvs = JSONObject()
        custViews.forEach { (libId, cv) ->
            cvs.put(
                libId,
                JSONObject().put("mode", cv.mode).put("by", cv.by)
                    .put("pinned", cv.pinned).put("sort", cv.sort)
            )
        }
        store.saveSettings(
            JSONObject().put("darkMode", dark).put("listFontSize", globalFont)
                .put("lockEnabled", lockEnabled)
                .put("lockPassword", lockPassword)
                .put("batchPresets", arr).put("searchHistory", hist)
                .put("restockHistory", restockHistoryToJson(restockHistory))
                .put("custViews", cvs)
        )
    }

    fun setFingerprintLock(v: Boolean) {
        if (lockEnabled == v) return
        lockEnabled = v
        locked = false
        persistSettings()
    }

    fun unlock() { locked = false }

    fun lockNow() { if (lockEnabled || lockPassword.isNotEmpty()) locked = true }

    /** 设置解锁密码；传 null 或空串表示清除。 */
    fun applyLockPassword(pw: String?) {
        lockPassword = if (pw.isNullOrEmpty()) "" else {
            val salt = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
            "$salt:${sha256(salt + pw)}"
        }
        locked = false
        persistSettings()
    }

    fun verifyPassword(pw: String): Boolean {
        val parts = lockPassword.split(":")
        if (parts.size != 2) return false
        return sha256(parts[0] + pw) == parts[1]
    }

    private fun sha256(s: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

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

    /** 记录一次补货文本快照（复制/分享/保存时调用）。与最近一条同库文本相同则去重。
     *  items 为结构化明细，用于货到后一键入库。 */
    fun addRestockHistory(libId: String, text: String, total: Int, count: Int, items: List<RestockItem> = emptyList()) {
        if (text.isBlank()) return
        val last = restockHistory.firstOrNull()
        if (last != null && last.libId == libId && last.text == text) return
        val orderItems = items.map { RestockOrderItem(it.model, it.sph, it.cyl, it.restock) }
        val rec = RestockRecord(
            id = java.util.UUID.randomUUID().toString(),
            libId = libId, libName = lib(libId)?.name ?: "",
            at = System.currentTimeMillis(),
            total = total, count = count, text = text,
            items = orderItems, received = false
        )
        restockHistory = (listOf(rec) + restockHistory).take(50)
        persistSettings()
    }

    fun deleteRestockHistory(id: String) {
        restockHistory = restockHistory.filter { it.id != id }
        persistSettings()
    }

    /** 清空补货历史；libId 为空时清空全部。 */
    fun clearRestockHistory(libId: String? = null) {
        restockHistory = if (libId == null) emptyList() else restockHistory.filter { it.libId != libId }
        persistSettings()
    }

    /** 客户视图偏好：按库记忆，离开客户库再进来不会重置 */
    data class CustView(
        val mode: Boolean = false,
        val by: String = "phone",
        val pinned: Boolean = true,
        val sort: String = "last_desc"
    )

    var custViews by mutableStateOf<Map<String, CustView>>(emptyMap())
        private set

    fun custViewOf(libId: String): CustView = custViews[libId] ?: CustView()

    fun updateCustView(libId: String, f: (CustView) -> CustView) {
        val next = f(custViewOf(libId))
        if (next == custViewOf(libId)) return
        custViews = custViews + (libId to next)
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
        if (booting) return
        rev++
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350)
            withContext(Dispatchers.IO) {
                store.commitJson(state.toJson().toString())
                runCatching { Reminders.reschedule(getApplication(), state) }
            }
            scheduleWarm()
        }
        if (bk.auto && bk.url.isNotBlank()) {
            cloudJob?.cancel()
            cloudJob = viewModelScope.launch {
                delay(6000)
                val json = withContext(Dispatchers.IO) { exportText() }
                val r = Cloud.upload(bk, json)
                if (r.ok) {
                    bk = bk.copy(lastCloudAt = System.currentTimeMillis())
                    store.saveBk(Cloud.configToJson(bk))
                    cloudStatus = "自动备份 · " + SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())
                }
            }
        }
    }

    fun saveNow() {
        if (booting) return
        saveJob?.cancel()
        // 退到后台时也要把提醒闹钟排上，否则保存后立刻退出会漏掉提醒
        val snapshot = state
        bgScope.launch {
            runCatching { store.commitJson(snapshot.toJson().toString()) }
            runCatching { Reminders.reschedule(getApplication(), snapshot) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (booting) return
        saveJob?.cancel()
        val snapshot = state
        bgScope.launch {
            runCatching { store.commitJson(snapshot.toJson().toString()) }
            runCatching { Reminders.reschedule(getApplication(), snapshot) }
        }
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

    /* ---------------- 补货目标 ---------------- */

    private fun ensureRestock(lib: MLibrary): RestockCfg {
        if (lib.restock == null) lib.restock = RestockCfg()
        return lib.restock!!
    }

    fun setRestockScope(libId: String, scope: String) {
        val lib = lib(libId) ?: return
        ensureRestock(lib).scope = scope
        touch()
    }

    fun setRestockDefault(libId: String, plain: Int, cyl: Int) {
        val lib = lib(libId) ?: return
        val r = ensureRestock(lib)
        r.defaultPlain = plain.coerceAtLeast(0)
        r.defaultCyl = cyl.coerceAtLeast(0)
        touch()
    }

    /** 设置某型号的补货目标；plain/cyl 为 null 或 ≤0 表示该项留空（用默认）。两项都空则清除该型号配置。 */
    fun setRestockTarget(libId: String, model: String, plain: Int?, cyl: Int?) {
        val lib = lib(libId) ?: return
        val r = ensureRestock(lib)
        val pEmpty = plain == null || plain <= 0
        val cEmpty = cyl == null || cyl <= 0
        if (pEmpty && cEmpty) {
            r.targets.remove(model)
        } else {
            val cur = r.targets[model] ?: RestockTarget()
            if (!pEmpty) cur.plain = plain!!
            if (!cEmpty) cur.cyl = cyl!!
            r.targets[model] = cur
        }
        touch()
    }

    /** 新增/更新某型号的度数范围补货目标规则（按 id 去重）。 */
    fun saveRestockRangeRule(libId: String, rule: RestockRangeRule) {
        val lib = lib(libId) ?: return
        val r = ensureRestock(lib)
        r.ranges.removeAll { it.id == rule.id }
        r.ranges.add(rule)
        touch()
    }

    fun deleteRestockRangeRule(libId: String, id: String) {
        val lib = lib(libId) ?: return
        ensureRestock(lib).ranges.removeAll { it.id == id }
        touch()
    }

    /** 设置某型号的供货商；name 为空则清除。 */
    fun setSupplier(libId: String, model: String, name: String) {
        val lib = lib(libId) ?: return
        val r = ensureRestock(lib)
        val n = name.trim()
        if (n.isEmpty()) r.suppliers.remove(model) else r.suppliers[model] = n
        touch()
    }

    /** 货到入库：把补货单明细数量加到库存，找不到匹配则新建条目。 */
    fun receiveRestock(libId: String, recordId: String) {
        val lib = lib(libId) ?: return
        val rec = restockHistory.firstOrNull { it.id == recordId } ?: return
        if (rec.received || rec.items.isEmpty()) {
            toast("没有可入库的明细"); return
        }
        val mx = matrixCfg(lib) ?: return
        var updated = 0
        var created = 0
        rec.items.forEach { item ->
            val existing = findStockEntry(lib, item.model, item.sph, item.cyl)
            if (existing != null) {
                val raw = existing.values[mx.value.id]
                val cur = if (raw.isNullOrBlank()) 0 else (raw.toDoubleOrNull() ?: 0.0).toInt()
                existing.values[mx.value.id] = (cur + item.qty).toString()
                existing.updatedAt = System.currentTimeMillis()
                updated++
            } else {
                val entry = MEntry(id = java.util.UUID.randomUUID().toString())
                entry.values[mx.group.id] = item.model
                entry.values[mx.row.id] = fmtPower(item.sph)
                entry.values[mx.col.id] = if (item.cyl == 0.0) "0" else fmtPower(item.cyl)
                entry.values[mx.value.id] = item.qty.toString()
                stampAutoFields(lib, entry, true)
                lib.entries.add(entry)
                created++
            }
        }
        restockHistory = restockHistory.map { if (it.id == recordId) it.copy(received = true) else it }
        touch()
        persistSettings()
        toast("已入库：更新 $updated 条，新建 $created 条")
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

    suspend fun importText(text: String): Boolean {
        // 解析整库 JSON 与生成快照都在 IO 线程完成，只把状态应用留在主线程
        val parsed = withContext(Dispatchers.IO) {
            try {
                val s = parseBackup(text)
                if (s.libraries.isEmpty()) null else s to state.toJson().toString()
            } catch (_: Exception) {
                null
            }
        } ?: return false
        withContext(Dispatchers.IO) { store.saveSnapshot(parsed.second) }
        snapshotAt = System.currentTimeMillis()
        state = parsed.first
        ensureAllMeta()
        stack.clear()
        stack.add(Scr.Home)
        touch()
        saveNow()
        return true
    }

    /** 撤销上一次「从文件恢复 / 从云端恢复」。 */
    suspend fun undoImport(): Boolean {
        val snap = withContext(Dispatchers.IO) { store.snapshot() } ?: return false
        val s = withContext(Dispatchers.IO) {
            try {
                parseState(JSONObject(snap.second))
            } catch (_: Exception) {
                null
            }
        } ?: return false
        if (s.libraries.isEmpty()) return false
        state = s
        ensureAllMeta()
        stack.clear()
        stack.add(Scr.Home)
        touch()
        saveNow()
        withContext(Dispatchers.IO) { store.clearSnapshot() }
        snapshotAt = null
        return true
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
            val json = withContext(Dispatchers.IO) { exportText() }
            val r = Cloud.upload(bk, json)
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
