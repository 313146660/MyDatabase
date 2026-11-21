package com.example.mydb.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.R
import com.example.mydb.ui.*
import com.example.mydb.ui.theme.*

@Composable
fun AboutScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val md = remember {
        ctx.resources.openRawResource(R.raw.about).bufferedReader().use { it.readText() }
    }
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        TopBar("关于 MyDB", onBack = { vm.back() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(4.dp))
            Markdown(md)
            Spacer(Modifier.height(18.dp))
            Text(
                "版本 ${appVersionName(ctx)}（${appVersionCode(ctx)}）",
                fontSize = 11.sp, color = Muted2
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Markdown(text: String) {
    text.split("\n").forEach { line ->
        when {
            line.startsWith("# ") -> Text(
                inline(line.removePrefix("# ").trim()),
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            line.startsWith("## ") -> Text(
                inline(line.removePrefix("## ").trim()),
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
            line.startsWith("- ") -> Text(
                inline("•  " + line.removePrefix("- ")),
                fontSize = 13.5.sp, color = TextMain, lineHeight = 20.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
            )
            line.startsWith("> ") -> Text(
                inline(line.removePrefix("> ")),
                fontSize = 12.5.sp, color = Muted,
                modifier = Modifier.padding(start = 8.dp, top = 3.dp, bottom = 3.dp)
            )
            line.isBlank() -> Spacer(Modifier.height(6.dp))
            else -> Text(
                inline(line),
                fontSize = 13.5.sp, color = TextMain, lineHeight = 20.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

private fun inline(s: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < s.length) {
        when {
            s.startsWith("**", i) -> {
                val end = s.indexOf("**", i + 2)
                if (end > 0) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(s.substring(i + 2, end))
                    pop()
                    i = end + 2
                } else { append(s[i]); i++ }
            }
            s[i] == '`' -> {
                val end = s.indexOf('`', i + 1)
                if (end > 0) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                    append(s.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else { append(s[i]); i++ }
            }
            else -> { append(s[i]); i++ }
        }
    }
}
