package com.example.mydb.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.ui.screens.*
import com.example.mydb.ui.theme.*

@Composable
fun AppRoot(vm: AppViewModel) {
    if (vm.booting) {
        BootScreen()
        return
    }
    val stack = vm.stack
    val current = stack.lastOrNull() ?: Scr.Home
    val showTabs = current is Scr.Home || current is Scr.Settings
    val snackbar = remember { SnackbarHostState() }

    BackHandler(enabled = stack.size > 1) { vm.back() }

    LaunchedEffect(vm.toastText) {
        val msg = vm.toastText ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        vm.clearToast()
    }

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { if (showTabs) BottomBar(current, vm) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (current) {
                is Scr.Home -> HomeScreen(vm)
                is Scr.Settings -> SettingsScreen(vm)
                is Scr.Library -> LibraryScreen(vm, current.libId)
                is Scr.Detail -> DetailScreen(vm, current.libId, current.entryId)
                is Scr.Editor -> EditorScreen(vm, current.libId, current.entryId)
                is Scr.Backup -> BackupScreen(vm)
                is Scr.About -> AboutScreen(vm)
                is Scr.Fields -> FieldsScreen(vm, current.libId)
                is Scr.FieldForm -> FieldFormScreen(vm, current.libId, current.fieldId)
                is Scr.ListFields -> ListFieldsScreen(vm, current.libId)
                is Scr.LibForm -> LibFormScreen(vm, current.libId)
                is Scr.Alerts -> AlertsScreen(vm, current.libId)
                is Scr.AlertForm -> AlertFormScreen(vm, current.libId, current.ruleId)
                is Scr.Csv -> CsvScreen(vm, current.libId)
                is Scr.Restock -> RestockScreen(vm, current.libId)
                is Scr.RestockCfg -> RestockConfigScreen(vm, current.libId)
                is Scr.RestockHistory -> RestockHistoryScreen(vm, current.libId)
                is Scr.Stats -> StatsScreen(vm, current.libId)
            }
        }
    }
}

@Composable
private fun BottomBar(current: Scr, vm: AppViewModel) {
    Column(Modifier.fillMaxWidth().background(Surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem("home", "首页", current is Scr.Home) { vm.tab(Scr.Home) }
            TabItem("settings", "我的", current is Scr.Settings) { vm.tab(Scr.Settings) }
        }
    }
}

@Composable
private fun TabItem(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 26.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(icon, 21.dp, if (active) Primary else Muted2)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = if (active) Primary else Muted2)
    }
}

/** 冷启动加载页：整库 JSON 在后台解析期间展示。 */
@Composable
private fun BootScreen() {
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.size(26.dp), color = Primary, strokeWidth = 2.5.dp)
            Spacer(Modifier.height(14.dp))
            Text("正在读取数据…", fontSize = 12.5.sp, color = Muted2)
        }
    }
}
