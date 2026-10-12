package com.pixelchief.shizuku

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import com.pixelchief.shizuku.ui.CalibrationScreen
import com.pixelchief.shizuku.ui.CyanDark
import com.pixelchief.shizuku.ui.CyanNeon
import com.pixelchief.shizuku.ui.DashboardScreen
import com.pixelchief.shizuku.ui.GoldYellow
import com.pixelchief.shizuku.ui.GuideScreen
import com.pixelchief.shizuku.ui.Slate800
import com.pixelchief.shizuku.ui.Slate900
import com.pixelchief.shizuku.ui.StrategiesScreen
import com.pixelchief.shizuku.ui.TerminalScreen
import com.pixelchief.shizuku.ui.WallsScreen

enum class AppTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Chief", Icons.Default.Dashboard),
    WALLS("Walls", Icons.Default.Security),
    STRATEGIES("Attack", Icons.Default.MilitaryTech),
    CALIBRATE("Calibrate", Icons.Default.AspectRatio),
    TERMINAL("Terminal", Icons.Default.Terminal),
    GUIDE("Guide", Icons.Default.HelpOutline)
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                LogBus.info("Permission", "Notification permission granted.")
            } else {
                LogBus.warn("Permission", "Notification permission denied; service notices might be hidden.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        ShizukuManager.checkStatus(this)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                MainAppNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuManager.checkStatus(this)
    }
}

@Composable
fun MainAppNavHost() {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        currentTab = AppTab.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Slate900,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) CyanNeon else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                color = if (isSelected) CyanNeon else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Slate800,
                            selectedIconColor = CyanNeon,
                            unselectedIconColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate900)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    onNavigateToTerminal = { currentTab = AppTab.TERMINAL },
                    onNavigateToGuide = { currentTab = AppTab.GUIDE }
                )
                AppTab.WALLS -> WallsScreen()
                AppTab.STRATEGIES -> StrategiesScreen()
                AppTab.CALIBRATE -> CalibrationScreen()
                AppTab.TERMINAL -> TerminalScreen()
                AppTab.GUIDE -> GuideScreen()
            }
        }
    }
}
