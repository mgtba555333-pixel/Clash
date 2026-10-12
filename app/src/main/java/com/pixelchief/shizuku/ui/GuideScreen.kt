package com.pixelchief.shizuku.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelchief.shizuku.LogBus
import com.pixelchief.shizuku.ShizukuManager
import com.pixelchief.shizuku.ShizukuStatus

@Composable
fun GuideScreen() {
    val context = LocalContext.current
    val shizukuStatus by ShizukuManager.status.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Shizuku Setup Guide",
                subtitle = "Complete ADB wireless pairing without computer or root"
            )
        }

        // Live Status Banner
        item {
            GlowCard(borderColor = if (shizukuStatus == ShizukuStatus.READY) EmeraldGreen else GoldYellow) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (shizukuStatus == ShizukuStatus.READY) EmeraldGreen else GoldYellow,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Connection Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            ShizukuStatusBadge(shizukuStatus)
                        }
                    }

                    Button(
                        onClick = {
                            val newStatus = ShizukuManager.checkStatus(context)
                            LogBus.info("Guide", "Checked status: $newStatus")
                            Toast.makeText(context, "Status: $newStatus", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check")
                    }
                }
            }
        }

        // Step 1: Install Shizuku
        item {
            GuideStepCard(
                step = 1,
                title = "Install Shizuku App",
                description = "Shizuku provides elevated ADB capabilities to simulate user taps and swipe gestures without rooting your device.",
                actionText = "Get on Google Play",
                onAction = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                    context.startActivity(intent)
                }
            )
        }

        // Step 2: Wireless Debugging
        item {
            GuideStepCard(
                step = 2,
                title = "Enable Wireless Debugging",
                description = "Go to Android Settings -> Developer Options -> Turn on 'Wireless Debugging' while connected to Wi-Fi."
            )
        }

        // Step 3: Pairing Code
        item {
            GuideStepCard(
                step = 3,
                title = "Pair Shizuku with Code",
                description = "Open Shizuku -> Tap 'Pairing' -> Tap 'Developer options' -> 'Pair device with pairing code' and enter the 6-digit code in the notification."
            )
        }

        // Step 4: Start Shizuku Service
        item {
            GuideStepCard(
                step = 4,
                title = "Start Shizuku Service",
                description = "Return to Shizuku and tap 'Start'. Shizuku will start its system background bridge.",
                actionText = "Open Shizuku App",
                onAction = {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    } else {
                        Toast.makeText(context, "Shizuku is not installed yet", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Step 5: Authorize PixelChief
        item {
            GuideStepCard(
                step = 5,
                title = "Authorize PixelChief",
                description = "Allow PixelChief when prompted in the Shizuku app, or tap 'Request Permission' below.",
                actionText = "Request Permission",
                onAction = {
                    ShizukuManager.requestPermission()
                }
            )
        }

        // Alternative ADB PC Command
        item {
            GlowCard {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Alternative: Start via PC ADB",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = {
                            val cmd = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd))
                            Toast.makeText(context, "Command copied", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanNeon)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "If wireless debugging is unavailable, run this in your PC terminal with USB Debugging enabled:",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF030712),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                            color = CyanNeon,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuideStepCard(
    step: Int,
    title: String,
    description: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(
                color = CyanNeon.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = step.toString(), color = CyanNeon, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, color = Color.LightGray, fontSize = 12.sp)

                if (actionText != null && onAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = actionText, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
