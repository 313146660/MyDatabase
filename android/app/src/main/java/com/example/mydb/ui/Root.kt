package com.example.mydb.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    if (vm.locked) {
        LockScreen(vm)
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

/** 指纹锁屏：验证通过后进入主界面。 */
@Suppress("DEPRECATION")
@Composable
private fun LockScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val hasPw = vm.lockPassword.isNotEmpty()
    var msg by remember { mutableStateOf<String?>(null) }
    var trying by remember { mutableStateOf(false) }
    var showPw by remember { mutableStateOf(!vm.lockEnabled && hasPw) }
    var pw by remember { mutableStateOf("") }
    var pwErr by remember { mutableStateOf(false) }

    fun retry() {
        if (trying || !vm.lockEnabled) return
        trying = true
        msg = null
        authenticateFingerprint(
            ctx,
            onSuccess = { trying = false; vm.unlock() },
            onError = { m ->
                trying = false
                msg = when {
                    m.isNotEmpty() -> m
                    hasPw -> "指纹未通过，请输入密码"
                    else -> "验证已取消"
                }
            }
        )
    }

    // 等界面真正进入前台（RESUMED）再唤起系统验证，避免冷启动瞬间弹不出对话框
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        retry()
        onPauseOrDispose { }
    }
    BackHandler(enabled = true) { }

    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(soft(Primary)),
                contentAlignment = Alignment.Center
            ) { AppIcon("lock", 30.dp, Primary) }
            Spacer(Modifier.height(18.dp))
            Text("MyDB 已锁定", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(6.dp))
            Text(
                msg ?: if (hasPw) "验证指纹或输入密码后查看客户档案" else "验证指纹后查看客户档案",
                fontSize = 12.sp, color = Muted2, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            if (vm.lockEnabled) {
                Box(Modifier.width(190.dp)) { PillButton("验证指纹", onClick = { retry() }) }
            }
            if (hasPw) {
                Spacer(Modifier.height(14.dp))
                if (!showPw) {
                    Text(
                        "使用密码解锁",
                        fontSize = 12.sp, color = Primary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { showPw = true }
                    )
                } else {
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it; pwErr = false },
                        label = { Text("解锁密码") },
                        singleLine = true,
                        isError = pwErr,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pwErr) {
                        Spacer(Modifier.height(4.dp))
                        Text("密码不正确", fontSize = 11.sp, color = Danger)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth()) {
                        PillButton("解锁", onClick = {
                            if (vm.verifyPassword(pw)) vm.unlock() else { pwErr = true; pw = "" }
                        })
                    }
                }
            } else if (msg != null && vm.lockEnabled) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "关闭指纹解锁并进入",
                    fontSize = 11.5.sp, color = Muted2,
                    modifier = Modifier.clickable { vm.setFingerprintLock(false) }
                )
            }
        }
    }
}

/** 调起系统指纹验证；Android 9 以下不支持。 */
@Suppress("DEPRECATION")
private fun authenticateFingerprint(context: android.content.Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
    if (android.os.Build.VERSION.SDK_INT < 28) {
        onError("当前系统版本不支持指纹解锁（需 Android 9+）")
        return
    }
    val fm = context.getSystemService(android.content.Context.FINGERPRINT_SERVICE)
        as? android.hardware.fingerprint.FingerprintManager
    if (fm == null || !fm.isHardwareDetected()) {
        onError("此设备不支持指纹识别")
        return
    }
    if (!fm.hasEnrolledFingerprints()) {
        onError("请先在系统设置里录入指纹")
        return
    }
    showBiometricPrompt(context, onSuccess, onError)
}

@androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.P)
private fun showBiometricPrompt(context: android.content.Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
    val prompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
        .setTitle("解锁 MyDB")
        .setSubtitle("验证指纹以查看客户档案")
        .setNegativeButton("取消", executor) { _, _ -> onError("") }
        .build()
    try {
        prompt.authenticate(
            android.os.CancellationSignal(),
            executor,
            object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 10 = 用户取消，13 = 点击了「取消」按钮
                    if (errorCode == 10 || errorCode == 13) onError("") else onError(errString.toString())
                }
            }
        )
    } catch (e: Exception) {
        onError(e.message ?: "指纹验证不可用")
    }
}
