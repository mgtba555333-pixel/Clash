package com.pixelchief.shizuku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelchief.shizuku.LogBus
import com.pixelchief.shizuku.PixelChiefApp
import com.pixelchief.shizuku.ScreenCalibration
import com.pixelchief.shizuku.ShizukuManager
import com.pixelchief.shizuku.TargetPoint
import com.pixelchief.shizuku.vision.ScreenAnalyzer
import kotlinx.coroutines.launch

@Composable
fun CalibrationScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as PixelChiefApp
    val settingsStore = app.settingsStore
    val calibration by settingsStore.calibration.collectAsState()
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    val targetPoints = listOf(
        calibration.villageCenter,
        calibration.attackButton,
        calibration.findMatchButton,
        calibration.nextMatchButton,
        calibration.endBattleButton,
        calibration.returnHomeButton
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Screen Calibration",
                subtitle = "Align coordinate mapping and verify ADB tap locations"
            )
        }

        // Screen Dimensions Card
        item {
            GlowCard {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AspectRatio, contentDescription = null, tint = CyanNeon)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Display Geometry", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Active resolution: ${calibration.width} x ${calibration.height} px", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val displayMetrics = context.resources.displayMetrics
                                settingsStore.updateCalibration(
                                    calibration.copy(
                                        width = displayMetrics.widthPixels,
                                        height = displayMetrics.heightPixels,
                                        densityDpi = displayMetrics.densityDpi
                                    )
                                )
                                LogBus.success("Calibration", "Auto-detected screen: ${displayMetrics.widthPixels}x${displayMetrics.heightPixels}")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto-Detect")
                        }
                    }
                }
            }
        }

        // Target Points List with Test Tap Button
        item {
            Text(
                text = "Target Coordinate Calibration Points",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(targetPoints) { point ->
            val px = ScreenAnalyzer.toScreenPixels(point, calibration)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = point.label,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Pixel: (${px.first}, ${px.second}) | Norm: (${String.format(java.util.Locale.US, "%.2f", point.normalizedX)}, ${String.format(java.util.Locale.US, "%.2f", point.normalizedY)})",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val res = ShizukuManager.tap(px.first, px.second)
                                    LogBus.info("ADB Tap", "Tapped ${point.label} at (${px.first}, ${px.second}) [Code: ${res.exitCode}]")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Tap", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sliders for X and Y adjustment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("X Position (${(point.normalizedX * 100).toInt()}%)", color = Color.Gray, fontSize = 10.sp)
                            Slider(
                                value = point.normalizedX,
                                onValueChange = { newX ->
                                    val updated = updatePointInCalibration(calibration, point.id, newX, point.normalizedY)
                                    settingsStore.updateCalibration(updated)
                                },
                                valueRange = 0.02f..0.98f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanNeon,
                                    activeTrackColor = CyanNeon,
                                    inactiveTrackColor = Slate700
                                )
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Y Position (${(point.normalizedY * 100).toInt()}%)", color = Color.Gray, fontSize = 10.sp)
                            Slider(
                                value = point.normalizedY,
                                onValueChange = { newY ->
                                    val updated = updatePointInCalibration(calibration, point.id, point.normalizedX, newY)
                                    settingsStore.updateCalibration(updated)
                                },
                                valueRange = 0.02f..0.98f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldYellow,
                                    activeTrackColor = GoldYellow,
                                    inactiveTrackColor = Slate700
                                )
                            )
                        }
                    }
                }
            }
        }

        // Reset Defaults
        item {
            OutlinedButton(
                onClick = {
                    settingsStore.updateCalibration(ScreenCalibration())
                    LogBus.info("Calibration", "Reset calibration points to default 1080x2400 layout.")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
            ) {
                Text("Reset Calibration to Defaults")
            }
        }
    }
}

private fun updatePointInCalibration(
    cal: ScreenCalibration,
    id: String,
    x: Float,
    y: Float
): ScreenCalibration {
    return when (id) {
        "center" -> cal.copy(villageCenter = cal.villageCenter.copy(normalizedX = x, normalizedY = y))
        "attack" -> cal.copy(attackButton = cal.attackButton.copy(normalizedX = x, normalizedY = y))
        "find_match" -> cal.copy(findMatchButton = cal.findMatchButton.copy(normalizedX = x, normalizedY = y))
        "next_match" -> cal.copy(nextMatchButton = cal.nextMatchButton.copy(normalizedX = x, normalizedY = y))
        "end_battle" -> cal.copy(endBattleButton = cal.endBattleButton.copy(normalizedX = x, normalizedY = y))
        "return_home" -> cal.copy(returnHomeButton = cal.returnHomeButton.copy(normalizedX = x, normalizedY = y))
        else -> cal
    }
}
