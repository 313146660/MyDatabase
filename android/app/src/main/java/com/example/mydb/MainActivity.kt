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
