package com.example.mydb.ui

sealed interface Scr {
    data object Home : Scr
    data object Settings : Scr
    data class Library(val libId: String) : Scr
    data class Detail(val libId: String, val entryId: String) : Scr
    data class Editor(val libId: String, val entryId: String?) : Scr
    data object Backup : Scr
    data object About : Scr
    data class Fields(val libId: String) : Scr
    data class FieldForm(val libId: String, val fieldId: String?) : Scr
    data class ListFields(val libId: String) : Scr
    data class LibForm(val libId: String?) : Scr
    data class Alerts(val libId: String) : Scr
    data class AlertForm(val libId: String, val ruleId: String?) : Scr
    data class Csv(val libId: String) : Scr
    data class Restock(val libId: String) : Scr
    data class RestockCfg(val libId: String) : Scr
}
