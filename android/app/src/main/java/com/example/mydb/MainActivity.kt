package com.example.mydb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.example.mydb.ui.AppRoot
import com.example.mydb.ui.AppViewModel
import com.example.mydb.ui.theme.Bg
import com.example.mydb.ui.theme.MyDBTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyDBTheme {
                Surface(color = Bg) {
                    AppRoot(vm)
                }
            }
        }
    }

    override fun onStop() {
        vm.saveNow()
        stoppedAt = System.currentTimeMillis()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        // 后台超过 30 秒返回时重新上锁，避免选文件、分享等短时切换反复验证指纹
        if (stoppedAt > 0 && System.currentTimeMillis() - stoppedAt > 30_000) vm.lockNow()
    }
}
