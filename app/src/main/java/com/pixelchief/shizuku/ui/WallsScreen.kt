package com.pixelchief.shizuku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelchief.shizuku.LogBus
import com.pixelchief.shizuku.PixelChiefApp
import com.pixelchief.shizuku.ShizukuManager
import com.pixelchief.shizuku.SweepPattern
import com.pixelchief.shizuku.WallResourcePriority
import com.pixelchief.shizuku.vision.ScreenAnalyzer
import kotlinx.coroutines.launch

@Composable
fun WallsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PixelChiefApp
    val settingsStore = app.settingsStore
    val botController = app.botController

    val wallConfig by settingsStore.wallConfig.collectAsState()
    val stats by botController.stats.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            SectionHeader(
                title = "Auto Wall Upgrader",
                subtitle = "Automated wall piece detection, reserve protection & upgrading"
            )
        }

        // Master Toggle Card
        item {
            GlowCard(borderColor = if (wallConfig.autoUpgradeEnabled) EmeraldGreen else Slate700) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (wallConfig.autoUpgradeEnabled) EmeraldGreen else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Auto Upgrade Enabled",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (wallConfig.autoUpgradeEnabled) "Upgrades walls during routine cycles" else "Paused",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Switch(
                        checked = wallConfig.autoUpgradeEnabled,
                        onCheckedChange = {
                            settingsStore.updateWallConfig(wallConfig.copy(autoUpgradeEnabled = it))
                            LogBus.info("Walls", "Auto Wall Upgrade toggled: $it")
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldGreen,
                            checkedTrackColor = Slate700
                        )
                    )
                }
            }
        }

        // Summary Stats Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Walls Upgraded",
                    value = stats.wallsUpgraded.toString(),
                    icon = Icons.Default.Security,
                    accentColor = GoldYellow,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Gold Invested",
                    value = formatAmount(stats.goldSpentOnWalls),
                    icon = Icons.Default.ElectricBolt,
                    accentColor = AmberDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Safety Reserve Thresholds
        item {
            GlowCard {
                Column {
                    Text(
                        text = "Resource Protection Thresholds",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Never upgrade walls if resources fall below these reserves",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ResourceSlider(
                        label = "Minimum Gold Reserve",
                        value = wallConfig.minGoldReserve,
                        min = 500_000L,
                        max = 12_000_000L,
                        step = 250_000L,
                        color = GoldYellow,
                        onValueChange = {
                            settingsStore.updateWallConfig(wallConfig.copy(minGoldReserve = it))
                        }
                    )

                    ResourceSlider(
                        label = "Minimum Elixir Reserve",
                        value = wallConfig.minElixirReserve,
                        min = 500_000L,
                        max = 12_000_000L,
                        step = 250_000L,
                        color = PurpleNeon,
                        onValueChange = {
                            settingsStore.updateWallConfig(wallConfig.copy(minElixirReserve = it))
                        }
                    )
                }
            }
        }

        // Priority & Sweep Strategy
        item {
            GlowCard {
                Column {
                    Text(
                        text = "Resource Payment Priority",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WallResourcePriority.values().forEach { priority ->
                            FilterChip(
                                selected = wallConfig.priority == priority,
                                onClick = {
                                    settingsStore.updateWallConfig(wallConfig.copy(priority = priority))
                                },
                                label = { Text(priority.name.replace("_", " "), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldYellow,
                                    selectedLabelColor = Slate900,
                                    containerColor = Slate700,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Wall Sweep Pattern",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SweepPattern.values().forEach { pattern ->
                            FilterChip(
                                selected = wallConfig.sweepPattern == pattern,
                                onClick = {
                                    settingsStore.updateWallConfig(wallConfig.copy(sweepPattern = pattern))
                                },
                                label = { Text(pattern.name.replace("_", " "), fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = Slate900,
                                    containerColor = Slate700,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Builder Guard & Target Level
        item {
            GlowCard {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Handyman, contentDescription = null, tint = GoldYellow)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Reserve 1 Builder for Walls", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Prevents all builders from getting locked on long upgrades", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = wallConfig.keepBuilderFree,
                            onCheckedChange = {
                                settingsStore.updateWallConfig(wallConfig.copy(keepBuilderFree = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GoldYellow,
                                checkedTrackColor = Slate700
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test Tap Upgrade
                    Button(
                        onClick = {
                            scope.launch {
                                LogBus.bot("Executing single wall upgrade test tap sequence...")
                                val cal = settingsStore.calibration.value
                                val pts = ScreenAnalyzer.generateWallCoordinates(cal, wallConfig.sweepPattern, 1)
                                if (pts.isNotEmpty()) {
                                    val pt = pts.first()
                                    ShizukuManager.tap(pt.first, pt.second)
                                    LogBus.success("Walls", "Tapped test wall at (${pt.first}, ${pt.second})")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyanNeon)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Wall Tap via ADB", color = Color.White)
                    }
                }
            }
        }
    }
}
