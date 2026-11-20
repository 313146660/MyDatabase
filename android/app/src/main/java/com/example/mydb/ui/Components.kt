package com.example.mydb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mydb.data.MLibrary
import com.example.mydb.ui.theme.*

fun hexColor(hex: String?): Color {
    val h = (hex ?: "#1976D2").removePrefix("#")
    return try {
        val v = h.toLong(16)
        Color(0xFF000000 or v)
    } catch (_: Exception) {
        Primary
    }
}

fun appIcon(name: String?): ImageVector = when (name) {
    "users" -> Icons.Filled.Person
    "box" -> Icons.Filled.ShoppingCart
    "doc" -> Icons.Filled.List
    "heart" -> Icons.Filled.Favorite
    "calendar" -> Icons.Filled.DateRange
    "database" -> Icons.Filled.List
    "home" -> Icons.Filled.Home
    "settings" -> Icons.Filled.Settings
    "search" -> Icons.Filled.Search
    "plus" -> Icons.Filled.Add
    "trash" -> Icons.Filled.Delete
    "edit" -> Icons.Filled.Edit
    "check" -> Icons.Filled.Check
    "close" -> Icons.Filled.Close
    "chevron" -> Icons.Filled.KeyboardArrowRight
    "bell" -> Icons.Filled.Notifications
    "refresh" -> Icons.Filled.Refresh
    "star" -> Icons.Filled.Star
    "more" -> Icons.Filled.MoreVert
    "layers" -> Icons.Filled.List
    "sliders" -> Icons.Filled.Menu
    "download" -> Icons.Filled.KeyboardArrowDown
    "upload" -> Icons.Filled.KeyboardArrowUp
    "link" -> Icons.Filled.Share
    "phone" -> Icons.Filled.Phone
    "lock" -> Icons.Filled.Lock
    "info" -> Icons.Filled.Info
    "warning" -> Icons.Filled.Warning
    "person" -> Icons.Filled.Person
    else -> Icons.Filled.List
}

@Composable
fun AppIcon(name: String?, size: Dp = 20.dp, tint: Color = Muted) {
    Icon(appIcon(name), contentDescription = null, modifier = Modifier.size(size), tint = tint)
}

fun paletteSoft(hex: String): Color = hexColor(hex).copy(alpha = 0.12f)

@Composable
fun Tile(icon: String?, color: Color, size: Dp = 42.dp, iconSize: Dp = 20.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(icon, iconSize, color)
    }
}

@Composable
fun Avatar(text: String, color: Color, size: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape)
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(color.copy(alpha = 0.75f), color)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            (text.trim().take(1).ifBlank { "?" }).uppercase(),
            color = Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = TextMain)
            }
            Spacer(Modifier.width(2.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 11.5.sp, color = Muted2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
fun SectionHead(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun MCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(18.dp)),
        content = content
    )
}

@Composable
fun EmptyBlock(icon: String = "database", title: String, desc: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 44.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(PrimarySoft),
            contentAlignment = Alignment.Center
        ) { AppIcon(icon, 26.dp, Primary) }
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextMain)
        Spacer(Modifier.height(4.dp))
        Text(desc, fontSize = 12.sp, color = Muted2)
    }
}

@Composable
fun TagChip(text: String, active: Boolean = false, icon: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Primary else Surface)
            .border(1.dp, if (active) Primary else Line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            AppIcon(icon, 13.dp, if (active) Color.White else Muted)
            Spacer(Modifier.width(4.dp))
        }
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.White else Muted)
    }
}

@Composable
fun SearchBox(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    onSubmit: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon("search", 17.dp, Muted2)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f)
                .onFocusChanged { onFocusChange?.invoke(it.isFocused) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.5.sp, color = TextMain),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit?.invoke() }),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, fontSize = 13.5.sp, color = Muted2)
                    inner()
                }
            }
        )
        if (value.isNotEmpty()) {
            IconButton(onClick = { onChange("") }, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "清空", tint = Muted2, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, color: Color = Primary, filled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) color else Surface)
            .border(1.dp, if (filled) color else Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (filled) Color.White else color)
    }
}

@Composable
fun FieldLabel(text: String, hint: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Muted)
        if (!hint.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(hint, fontSize = 10.5.sp, color = Muted2)
        }
    }
}

@Composable
fun LabeledBox(label: String, hint: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        FieldLabel(label, hint)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
fun AppTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Bg)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextMain),
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, fontSize = 13.5.sp, color = Muted2)
                inner()
            }
        }
    )
}

@Composable
fun FormRow(label: String, hint: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        FieldLabel(label, hint)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
fun SelectRow(label: String, selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FieldLabel(label)
    Spacer(Modifier.height(6.dp))
    Box {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Bg)
                .border(1.dp, Line, RoundedCornerShape(12.dp))
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                options.firstOrNull { it.first == selected }?.second ?: options.firstOrNull()?.second ?: selected,
                fontSize = 14.sp, color = TextMain, modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Muted2)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (v, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 13.5.sp) },
                    onClick = { open = false; onSelect(v) }
                )
            }
        }
    }
}

@Composable
fun BackScaffold(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg), content = content)
}

@Composable
fun InfoRow(icon: String, title: String, desc: String, color: Color = Primary, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(color.copy(alpha = 0.11f)), contentAlignment = Alignment.Center) {
            AppIcon(icon, 17.dp, color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            if (desc.isNotBlank()) Text(desc, fontSize = 11.5.sp, color = Muted2)
        }
        if (onClick != null) Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Muted2, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

