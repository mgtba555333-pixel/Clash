package com.pixelchief.shizuku.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelchief.shizuku.LogBus
import com.pixelchief.shizuku.LogLevel
import com.pixelchief.shizuku.ShizukuManager
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val logs by LogBus.logs.collectAsState()
    var commandInput by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
    val scope = rememberCoroutineScope()

    val quickCommands = listOf(
        "wm size",
        "getprop ro.product.model",
        "pm list packages -e supercell",
        "input tap 500 500",
        "screencap -p /sdcard/screen.png",
        "dumpsys power | grep mHolding"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp)
    ) {
        SectionHeader(
            title = "ADB Live Terminal",
            subtitle = "Execute real-time shell commands & inspect system events",
            action = {
                Row {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val text = logs.joinToString("\n") { "[${LogBus.formatTime(it.timestamp)}] [${it.tag}] ${it.message}" }
                        clipboard.setPrimaryClip(ClipData.newPlainText("PixelChief Logs", text))
                        Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs", tint = CyanNeon)
                    }
                    IconButton(onClick = { LogBus.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.LightGray)
                    }
                }
            }
        )

        // Quick Command Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            items(quickCommands) { cmd ->
                FilterChip(
                    selected = false,
                    onClick = { commandInput = cmd },
                    label = { Text(cmd, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Slate800,
                        labelColor = CyanNeon
                    )
                )
            }
        }

        // Command Entry Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("input tap 500 1200", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyanNeon
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        val cmd = commandInput.trim()
                        commandInput = ""
                        scope.launch {
                            LogBus.info("Terminal", "$ adb shell $cmd")
                            val res = ShizukuManager.executeCommand(cmd)
                            if (res.exitCode == 0) {
                                if (res.stdout.isNotEmpty()) {
                                    LogBus.success("Terminal", res.stdout)
                                } else {
                                    LogBus.success("Terminal", "Done (exit code 0)")
                                }
                            } else {
                                LogBus.error("Terminal", "Exit ${res.exitCode}: ${res.stderr.ifEmpty { "Command failed" }}")
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Run", tint = CyanNeon)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Terminal Log Window
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val filteredLogs = if (selectedFilter == null) logs else logs.filter { it.level == selectedFilter }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredLogs, key = { it.id }) { entry ->
                    val color = when (entry.level) {
                        LogLevel.SUCCESS -> EmeraldGreen
                        LogLevel.WARN -> GoldYellow
                        LogLevel.ERROR -> RoseRed
                        LogLevel.BOT -> CyanNeon
                        LogLevel.DEBUG -> PurpleNeon
                        LogLevel.INFO -> Color.LightGray
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${LogBus.formatTime(entry.timestamp)} ",
                            color = Color.DarkGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "[${entry.tag}] ",
                            color = color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = entry.message,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
