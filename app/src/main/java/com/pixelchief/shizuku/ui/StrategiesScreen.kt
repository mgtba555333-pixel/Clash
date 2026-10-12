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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelchief.shizuku.ArmyType
import com.pixelchief.shizuku.LogBus
import com.pixelchief.shizuku.PixelChiefApp

@Composable
fun StrategiesScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PixelChiefApp
    val settingsStore = app.settingsStore
    val strategy by settingsStore.strategy.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Farming Strategies",
                subtitle = "Army composition, loot filters, and deployment tactical settings"
            )
        }

        // Army Selector Cards
        item {
            Text(
                text = "Select Army Profile",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(ArmyType.values()) { army ->
            val isSelected = strategy.armyType == army
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Slate800 else Slate900
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) CyanNeon else Slate700.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                onClick = {
                    settingsStore.updateStrategy(strategy.copy(armyType = army))
                    LogBus.info("Strategy", "Selected army: ${army.displayName}")
                }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = if (isSelected) CyanNeon else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = army.displayName,
                            color = if (isSelected) CyanNeon else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = army.description,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Loot Filter Thresholds
        item {
            GlowCard {
                Column {
                    Text(
                        text = "Matchmaking Minimum Loot Targets",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "The bot will skip bases until these loot criteria are met",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ResourceSlider(
                        label = "Minimum Gold Target",
                        value = strategy.minGoldLoot,
                        min = 100_000L,
                        max = 1_500_000L,
                        step = 50_000L,
                        color = GoldYellow,
                        onValueChange = {
                            settingsStore.updateStrategy(strategy.copy(minGoldLoot = it))
                        }
                    )

                    ResourceSlider(
                        label = "Minimum Elixir Target",
                        value = strategy.minElixirLoot,
                        min = 100_000L,
                        max = 1_500_000L,
                        step = 50_000L,
                        color = PurpleNeon,
                        onValueChange = {
                            settingsStore.updateStrategy(strategy.copy(minElixirLoot = it))
                        }
                    )

                    ResourceSlider(
                        label = "Minimum Dark Elixir Target",
                        value = strategy.minDarkElixirLoot,
                        min = 1_000L,
                        max = 15_000L,
                        step = 500L,
                        color = CyanNeon,
                        onValueChange = {
                            settingsStore.updateStrategy(strategy.copy(minDarkElixirLoot = it))
                        }
                    )
                }
            }
        }

        // Tactical Deploy & Surrender Controls
        item {
            GlowCard {
                Column {
                    Text(
                        text = "Tactical Battle Directives",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Surrender after Collectors (50% / 1 Star)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Saves remainder troops and accelerates next cycle", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = strategy.autoSurrenderOn50Percent,
                            onCheckedChange = {
                                settingsStore.updateStrategy(strategy.copy(autoSurrenderOn50Percent = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyanNeon,
                                checkedTrackColor = Slate700
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dead Base Focus Only", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Prioritizes full exterior mines & sleeping x-bows", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = strategy.collectorOnlyFocus,
                            onCheckedChange = {
                                settingsStore.updateStrategy(strategy.copy(collectorOnlyFocus = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GoldYellow,
                                checkedTrackColor = Slate700
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Max Search Skips
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Max Search Skips Before Return", color = Color.White, fontSize = 13.sp)
                        Text("${strategy.maxSearchSkips} skips", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = strategy.maxSearchSkips.toFloat(),
                        onValueChange = {
                            settingsStore.updateStrategy(strategy.copy(maxSearchSkips = it.toInt()))
                        },
                        valueRange = 10f..100f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanNeon,
                            inactiveTrackColor = Slate700
                        )
                    )
                }
            }
        }
    }
}
