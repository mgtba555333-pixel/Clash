package com.pixelchief.shizuku.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.pixelchief.shizuku.BotState
import com.pixelchief.shizuku.LogBus
import com.pixelchief.shizuku.PixelChiefApp
import com.pixelchief.shizuku.ShizukuManager
import com.pixelchief.shizuku.ShizukuStatus
import com.pixelchief.shizuku.service.OverlayService
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onNavigateToTerminal: () -> Unit,
    onNavigateToGuide: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PixelChiefApp
    val botController = app.botController
    val settingsStore = app.settingsStore

    val botState by botController.botState.collectAsState()
    val stats by botController.stats.collectAsState()
    val currentTask by botController.currentTask.collectAsState()
    val shizukuStatus by ShizukuManager.status.collectAsState()
    val overlayEnabled by settingsStore.overlayEnabled.collectAsState()
    val logs by LogBus.logs.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hero_banner),
                    contentDescription = "Village Command Center",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Slate900.copy(alpha = 0.92f)),
                                startY = 120f
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PixelChief",
                            color = GoldYellow,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ShizukuStatusBadge(shizukuStatus)
                    }
                    Text(
                        text = "Shizuku ADB Tactical Automation Engine",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Shizuku Status & Pairing Alert (if not ready)
        if (shizukuStatus != ShizukuStatus.READY) {
            item {
                GlowCard(borderColor = GoldYellow) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = GoldYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Shizuku Authorization Needed",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (shizukuStatus == ShizukuStatus.PERMISSION_DENIED)
                                        "Grant Shizuku permission to enable touch & input simulation."
                                    else
                                        "Start Shizuku via Wireless Debugging or root.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToGuide,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                            ) {
                                Text("Setup Guide")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (shizukuStatus == ShizukuStatus.PERMISSION_DENIED) {
                                        ShizukuManager.requestPermission()
                                    } else {
                                        ShizukuManager.checkStatus(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldYellow, contentColor = Slate900)
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }
        }

        // Engine Control Card
        item {
            GlowCard(borderColor = if (botState == BotState.RUNNING) EmeraldGreen else CyanNeon) {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Automation Engine",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current Task: $currentTask",
                                color = if (botState == BotState.RUNNING) CyanNeon else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        BotStateBadge(botState)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (botState == BotState.RUNNING) {
                            Button(
                                onClick = { botController.pause() },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberDark),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause")
                            }
                        } else if (botState == BotState.PAUSED) {
                            Button(
                                onClick = { botController.resume() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resume")
                            }
                        } else {
                            Button(
                                onClick = { botController.start() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Bot")
                            }
                        }

                        Button(
                            onClick = { botController.stop() },
                            enabled = botState != BotState.IDLE && botState != BotState.STOPPED,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoseRed,
                                disabledContainerColor = Slate700
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop")
                        }
                    }
                }
            }
        }

        // Quick Launch & Floating Overlay Controls
        item {
            GlowCard {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = CyanNeon)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Floating HUD Overlay", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Control bot over Clash of Clans", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = overlayEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } else {
                                    settingsStore.setOverlayEnabled(enable)
                                    val serviceIntent = Intent(context, OverlayService::class.java)
                                    if (enable) context.startService(serviceIntent) else context.stopService(serviceIntent)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanNeon,
                                checkedTrackColor = CyanDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                ShizukuManager.launchApp(settingsStore.gamePackage.value)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = GoldYellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Launch Clash of Clans", color = Color.White)
                    }
                }
            }
        }

        // Live Session Stats Section
        item {
            SectionHeader(
                title = "Session Telemetry",
                subtitle = "Real-time automated resource & upgrade counters"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Gold Looted",
                    value = formatAmount(stats.goldLooted),
                    icon = Icons.Default.ElectricBolt,
                    accentColor = GoldYellow,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Elixir Looted",
                    value = formatAmount(stats.elixirLooted),
                    icon = Icons.Default.ElectricBolt,
                    accentColor = PurpleNeon,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Dark Elixir",
                    value = formatAmount(stats.darkElixirLooted),
                    icon = Icons.Default.Shield,
                    accentColor = CyanNeon,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Walls Upgraded",
                    value = stats.wallsUpgraded.toString(),
                    subtitle = "${formatAmount(stats.goldSpentOnWalls)} G spent",
                    icon = Icons.Default.Security,
                    accentColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Battles Won",
                    value = stats.battlesCompleted.toString(),
                    icon = Icons.Default.MilitaryTech,
                    accentColor = GoldYellow,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Search Skips",
                    value = stats.searchSkips.toString(),
                    icon = Icons.Default.Layers,
                    accentColor = Color.LightGray,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Mini Terminal Stream
        item {
            GlowCard {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = CyanNeon)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Live Console Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = onNavigateToTerminal) {
                            Icon(Icons.Default.Build, contentDescription = "Terminal", tint = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val recentLogs = logs.take(3)
                    if (recentLogs.isEmpty()) {
                        Text("No logs recorded yet.", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        recentLogs.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "[${LogBus.formatTime(entry.timestamp)}] ",
                                    color = Color.DarkGray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${entry.tag}: ",
                                    color = CyanNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = entry.message,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
