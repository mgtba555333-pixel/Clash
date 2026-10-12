package com.pixelchief.shizuku.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelchief.shizuku.BotState
import com.pixelchief.shizuku.LogEntry
import com.pixelchief.shizuku.LogLevel
import com.pixelchief.shizuku.ShizukuStatus

// Color Palette for Cybernetic Chief aesthetic
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val GoldYellow = Color(0xFFFBBF24)
val AmberDark = Color(0xFFD97706)
val CyanNeon = Color(0xFF22D3EE)
val CyanDark = Color(0xFF0891B2)
val EmeraldGreen = Color(0xFF10B981)
val RoseRed = Color(0xFFF43F5E)
val PurpleNeon = Color(0xFFA855F7)

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ShizukuStatusBadge(status: ShizukuStatus) {
    when (status) {
        ShizukuStatus.READY -> StatusBadge("Shizuku Active", EmeraldGreen)
        ShizukuStatus.CHECKING -> StatusBadge("Checking Shizuku...", CyanNeon)
        ShizukuStatus.PERMISSION_DENIED -> StatusBadge("Permission Required", GoldYellow)
        ShizukuStatus.NOT_RUNNING -> StatusBadge("Service Stopped", RoseRed)
        ShizukuStatus.NOT_INSTALLED -> StatusBadge("Not Installed", Slate700)
    }
}

@Composable
fun BotStateBadge(state: BotState) {
    when (state) {
        BotState.RUNNING -> StatusBadge("Running", EmeraldGreen)
        BotState.SEARCHING -> StatusBadge("Searching Base", CyanNeon)
        BotState.ATTACKING -> StatusBadge("Attacking", RoseRed)
        BotState.UPGRADING_WALLS -> StatusBadge("Upgrading Walls", GoldYellow)
        BotState.COLLECTING -> StatusBadge("Collecting", PurpleNeon)
        BotState.PAUSED -> StatusBadge("Paused", GoldYellow)
        BotState.STOPPED, BotState.IDLE -> StatusBadge("Idle", Color.Gray)
    }
}

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Slate700,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = GoldYellow,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        if (action != null) {
            action()
        }
    }
}

@Composable
fun ResourceSlider(
    label: String,
    value: Long,
    min: Long,
    max: Long,
    step: Long,
    unit: String = "",
    color: Color = GoldYellow,
    onValueChange: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = formatAmount(value) + (if (unit.isNotEmpty()) " $unit" else ""),
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange((it / step).toLong() * step) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Slate700
            )
        )
    }
}

fun formatAmount(amount: Long): String {
    return when {
        amount >= 1_000_000 -> String.format(java.util.Locale.US, "%.2fM", amount / 1_000_000.0)
        amount >= 1_000 -> String.format(java.util.Locale.US, "%.0fK", amount / 1_000.0)
        else -> amount.toString()
    }
}
