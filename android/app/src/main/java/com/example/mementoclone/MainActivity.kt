package com.example.mementoclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import com.example.mementoclone.ui.AppRoot
import com.example.mementoclone.ui.AppViewModel
import com.example.mementoclone.ui.theme.Bg
import com.example.mementoclone.ui.theme.MyDBTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

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
        super.onStop()
    }
}
