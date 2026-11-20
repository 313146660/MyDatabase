package com.example.mydb.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 全局偏好：不属于任何库，界面直接读这里。 */
object AppPrefs {
    /** 新库 / 未单独设置字号的库使用的列表标题字号（sp）。 */
    var listFontSize by mutableStateOf(14)
}
