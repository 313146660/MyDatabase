package com.example.mydb.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class Store(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mydb_store", Context.MODE_PRIVATE)

    fun load(): Pair<MState, Boolean> {
        val raw = prefs.getString(KEY_STATE, null)
        if (raw.isNullOrBlank()) return Seed.state() to true
        return try {
            parseState(JSONObject(raw)) to false
        } catch (_: Exception) {
            Seed.state() to true
        }
    }

    fun commit(state: MState) {
        prefs.edit().putString(KEY_STATE, state.toJson().toString()).apply()
    }

    /** 已序列化好的 JSON 字符串（供调用方在后台线程生成后写入）。 */
    fun commitJson(json: String) {
        prefs.edit().putString(KEY_STATE, json).apply()
    }

    fun rawJson(): String? = prefs.getString(KEY_STATE, null)

    fun getBk(): JSONObject {
        val raw = prefs.getString(KEY_BACKUP, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun saveBk(o: JSONObject) {
        prefs.edit().putString(KEY_BACKUP, o.toString()).apply()
    }

    fun settings(): JSONObject {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun saveSettings(o: JSONObject) {
        prefs.edit().putString(KEY_SETTINGS, o.toString()).apply()
    }

    /** 上次「从文件恢复」之前的数据，用于撤销。 */
    fun snapshot(): Pair<Long, String>? {
        val json = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        if (json.isBlank()) return null
        return prefs.getLong(KEY_SNAPSHOT_AT, 0L) to json
    }

    fun saveSnapshot(json: String) {
        prefs.edit()
            .putString(KEY_SNAPSHOT, json)
            .putLong(KEY_SNAPSHOT_AT, System.currentTimeMillis())
            .apply()
    }

    fun clearSnapshot() {
        prefs.edit().remove(KEY_SNAPSHOT).remove(KEY_SNAPSHOT_AT).apply()
    }

    companion object {
        const val KEY_STATE = "state_json"
        const val KEY_BACKUP = "backup_json"
        const val KEY_SETTINGS = "settings_json"
        const val KEY_SNAPSHOT = "snapshot_json"
        const val KEY_SNAPSHOT_AT = "snapshot_at"
    }
}

/** 空存储时的初始库：字段 id 与真实备份一致，之后导入不会冲突 */
object Seed {
    fun state(): MState {
        val customer = MLibrary(
            id = "lib_customer", name = "客户", icon = "users", color = "#1976D2",
            listFields = mutableListOf("c_name", "c_phone", "c_od", "c_os", "c_pd", "c_total")
        )
        customer.fields.addAll(
            listOf(
                MField("c_name", "姓名"),
                MField("c_phone", "电话", FieldType.TEL),
                MField("c_use", "用途", FieldType.SELECT, options = mutableListOf("远用", "近用", "渐进")),
                MField("c_od", "OD", FieldType.PICK, pick = PickCfg("lib_lens", "sku", "", "c_lens", true, 1)),
                MField("c_od_axis", "OD轴向"),
                MField("c_os", "OS", FieldType.PICK, pick = PickCfg("lib_lens", "sku", "", "c_lens", true, 1)),
                MField("c_os_axis", "OS轴向"),
                MField("c_pd", "瞳距"),
                MField("c_frame", "镜框"),
                MField("c_lens", "镜片", FieldType.PICK, pick = PickCfg("lib_lens", "value", "lf_model", "", false, 1)),
                MField("c_total", "合计", FieldType.NUMBER, unit = "元"),
                MField("c_date", "时间", FieldType.DATE),
                MField("c_no", "NO."),
                MField("c_add", "ADD"),
                MField("c_note", "备注", FieldType.LONGTEXT),
                MField("c_customer", "客户"),
                MField("f_no_kq6ix9", "编号", FieldType.AUTONUMBER, prefix = "NO-", start = 1001),
                MField("f_se_9pxoq0", "等效球镜", FieldType.FORMULA, expr = "'OD ' + fmt(se('OD')) + ' / OS ' + fmt(se('OS'))")
            )
        )

        val lens = MLibrary(
            id = "lib_lens", name = "镜片库", icon = "box", color = "#0F9D8A",
            view = "matrix",
            listFields = mutableListOf("lf_model", "lf_stock")
        )
        lens.fields.addAll(
            listOf(
                MField("lf_model", "型号"),
                MField("lf_sph", "球镜"),
                MField("lf_cyl", "柱镜"),
                MField("lf_stock", "库存", FieldType.NUMBER, unit = "片")
            )
        )
        lens.matrix = MatrixCfg("lf_model", "lf_sph", "lf_cyl", "lf_stock")
        lens.alerts = AlertsCfg("lf_stock", 0)

        return MState(mutableListOf(customer, lens), mutableListOf())
    }
}
