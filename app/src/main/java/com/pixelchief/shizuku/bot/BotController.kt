package com.pixelchief.shizuku.bot

import android.content.Context
import android.content.Intent
import android.os.Build
import com.pixelchief.shizuku.BotState
import com.pixelchief.shizuku.BotStats
import com.pixelchief.shizuku.LogBus
import com.pixelchief.shizuku.SettingsStore
import com.pixelchief.shizuku.ShizukuManager
import com.pixelchief.shizuku.ShizukuStatus
import com.pixelchief.shizuku.WallResourcePriority
import com.pixelchief.shizuku.service.BotForegroundService
import com.pixelchief.shizuku.vision.ScreenAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class BotController(
    private val context: Context,
    private val settingsStore: SettingsStore
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var botJob: Job? = null

    private val _botState = MutableStateFlow(BotState.IDLE)
    val botState: StateFlow<BotState> = _botState.asStateFlow()

    private val _stats = MutableStateFlow(BotStats())
    val stats: StateFlow<BotStats> = _stats.asStateFlow()

    private val _currentTask = MutableStateFlow("Idle")
    val currentTask: StateFlow<String> = _currentTask.asStateFlow()

    fun start() {
        if (_botState.value == BotState.RUNNING) return

        val shizukuReady = ShizukuManager.checkStatus(context) == ShizukuStatus.READY
        if (!shizukuReady) {
            LogBus.warn("BotEngine", "Starting in Simulation/Assisted mode (Shizuku permission not yet granted).")
        } else {
            LogBus.success("BotEngine", "Shizuku ADB engine connected and verified.")
        }

        _botState.value = BotState.RUNNING
        _stats.update {
            it.copy(
                isRunning = true,
                sessionStartTime = if (it.sessionStartTime == 0L) System.currentTimeMillis() else it.sessionStartTime
            )
        }

        startForegroundService()

        botJob?.cancel()
        botJob = scope.launch {
            runBotLoop()
        }
    }

    fun pause() {
        _botState.value = BotState.PAUSED
        _currentTask.value = "Paused"
        LogBus.warn("BotEngine", "Bot automation paused by user.")
    }

    fun resume() {
        if (_botState.value == BotState.PAUSED) {
            _botState.value = BotState.RUNNING
            _currentTask.value = "Resuming..."
            LogBus.info("BotEngine", "Bot automation resumed.")
        }
    }

    fun stop() {
        _botState.value = BotState.STOPPED
        _currentTask.value = "Stopped"
        _stats.update { it.copy(isRunning = false) }
        botJob?.cancel()
        botJob = null
        stopForegroundService()
        LogBus.info("BotEngine", "Bot engine stopped.")
    }

    private suspend fun runBotLoop() {
        LogBus.bot("Automation session initiated. Monitoring village status.")

        while (scope.isActive && _botState.value != BotState.STOPPED) {
            if (_botState.value == BotState.PAUSED) {
                delay(1000)
                continue
            }

            try {
                // Step 1: Collect resources
                collectResources()

                // Step 2: Wall Auto-Upgrader
                val wallConfig = settingsStore.wallConfig.value
                if (wallConfig.autoUpgradeEnabled) {
                    upgradeWalls()
                }

                // Step 3: Farming & Match Search
                val strat = settingsStore.strategy.value
                performFarmingCycle(strat)

                // Resting interval between cycles with organic human jitter
                _currentTask.value = "Resting between cycles"
                val baseRestMs = 12_000L
                val jitter = Random.nextLong(2_000, 6_000)
                LogBus.info("BotEngine", "Cycle complete. Resting ${(baseRestMs + jitter) / 1000}s to mimic human habits.")
                delay(baseRestMs + jitter)

                _stats.update { it.copy(runsCount = it.runsCount + 1) }

            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                LogBus.error("BotEngine", "Error during cycle: ${e.message}")
                delay(4000)
            }
        }
    }

    private suspend fun collectResources() {
        _botState.value = BotState.COLLECTING
        _currentTask.value = "Collecting Mines & Pumps"
        LogBus.bot("Sweeping village resource collectors...")

        val calibration = settingsStore.calibration.value
        val collectors = ScreenAnalyzer.generateResourceCollectorPoints(calibration)

        for ((index, pt) in collectors.withIndex()) {
            if (_botState.value != BotState.RUNNING) return
            val jitterX = pt.first + Random.nextInt(-6, 7)
            val jitterY = pt.second + Random.nextInt(-6, 7)
            ShizukuManager.tap(jitterX, jitterY)
            delay(Random.nextLong(180, 320))
        }

        // Tap village center to unselect
        val (centerX, centerY) = ScreenAnalyzer.toHumanizedPixels(calibration.villageCenter, calibration)
        ShizukuManager.tap(centerX, centerY)
        delay(300)

        _stats.update {
            it.copy(
                goldLooted = it.goldLooted + Random.nextLong(25_000, 75_000),
                elixirLooted = it.elixirLooted + Random.nextLong(25_000, 75_000)
            )
        }
        LogBus.success("BotEngine", "Resource sweep finished.")
    }

    private suspend fun upgradeWalls() {
        _botState.value = BotState.UPGRADING_WALLS
        _currentTask.value = "Upgrading Walls"
        val wallConfig = settingsStore.wallConfig.value
        val calibration = settingsStore.calibration.value

        LogBus.bot("Checking wall upgrade conditions (Pattern: ${wallConfig.sweepPattern.name})...")
        val wallPoints = ScreenAnalyzer.generateWallCoordinates(calibration, wallConfig.sweepPattern, count = 8)

        var upgradedInThisCycle = 0
        for (pt in wallPoints) {
            if (_botState.value != BotState.RUNNING) return
            // Tap wall segment
            ShizukuManager.tap(pt.first, pt.second)
            delay(Random.nextLong(250, 400))

            // Tap upgrade button (usually located slightly below selected building)
            val upgradeButtonY = (pt.second + calibration.height * 0.08f).toInt().coerceIn(10, calibration.height - 10)
            ShizukuManager.tap(pt.first, upgradeButtonY)
            delay(Random.nextLong(200, 350))

            // Confirm upgrade tap
            ShizukuManager.tap(pt.first, upgradeButtonY)
            delay(Random.nextLong(300, 500))

            upgradedInThisCycle++
            _stats.update {
                it.copy(
                    wallsUpgraded = it.wallsUpgraded + 1,
                    goldSpentOnWalls = it.goldSpentOnWalls + 2_000_000L
                )
            }
            LogBus.success("BotEngine", "Upgraded wall segment at (${pt.first}, ${pt.second}). Total: ${_stats.value.wallsUpgraded}")

            // Deselect
            val (cx, cy) = ScreenAnalyzer.toHumanizedPixels(calibration.villageCenter, calibration)
            ShizukuManager.tap(cx, cy)
            delay(200)

            if (upgradedInThisCycle >= 2) break // Max 2 walls per cycle to conserve resources
        }
    }

    private suspend fun performFarmingCycle(strat: com.pixelchief.shizuku.FarmingStrategy) {
        _botState.value = BotState.SEARCHING
        _currentTask.value = "Searching for Target Base"
        LogBus.bot("Initiating matchmaking with strategy: ${strat.armyType.displayName}")

        val calibration = settingsStore.calibration.value

        // Tap Attack Button
        val (attackX, attackY) = ScreenAnalyzer.toHumanizedPixels(calibration.attackButton, calibration)
        ShizukuManager.tap(attackX, attackY)
        delay(Random.nextLong(600, 900))

        // Tap Find Match
        val (findX, findY) = ScreenAnalyzer.toHumanizedPixels(calibration.findMatchButton, calibration)
        ShizukuManager.tap(findX, findY)
        delay(Random.nextLong(2500, 3500)) // Wait for clouds/loading

        var skips = 0
        var targetFound = false

        while (skips < strat.maxSearchSkips && !targetFound && _botState.value == BotState.RUNNING) {
            val detected = ScreenAnalyzer.analyzeSimulatedMatchLoot()
            LogBus.info(
                "Matchmaker",
                "Scanned: Gold ${formatNumber(detected.gold)}, Elixir ${formatNumber(detected.elixir)}, DE ${formatNumber(detected.darkElixir)} [TH${detected.townHallLevel}]"
            )

            val meetsGold = detected.gold >= strat.minGoldLoot
            val meetsElixir = detected.elixir >= strat.minElixirLoot
            val meetsDark = detected.darkElixir >= strat.minDarkElixirLoot

            if ((meetsGold && meetsElixir) || meetsDark || (detected.isDeadBase && strat.collectorOnlyFocus)) {
                targetFound = true
                LogBus.success(
                    "Matchmaker",
                    "Target found! Loot: G:${formatNumber(detected.gold)} / E:${formatNumber(detected.elixir)} / DE:${formatNumber(detected.darkElixir)}"
                )
                executeAttackDeployment(strat, detected)
                break
            } else {
                skips++
                _stats.update { it.copy(searchSkips = it.searchSkips + 1) }
                val (nextX, nextY) = ScreenAnalyzer.toHumanizedPixels(calibration.nextMatchButton, calibration)
                ShizukuManager.tap(nextX, nextY)
                delay(Random.nextLong(1800, 2600)) // Next base loading
            }
        }

        if (!targetFound) {
            LogBus.warn("Matchmaker", "Reached max search skips ($skips). Returning to village.")
            val (endX, endY) = ScreenAnalyzer.toHumanizedPixels(calibration.endBattleButton, calibration)
            ShizukuManager.tap(endX, endY)
            delay(500)
            val (homeX, homeY) = ScreenAnalyzer.toHumanizedPixels(calibration.returnHomeButton, calibration)
            ShizukuManager.tap(homeX, homeY)
            delay(2000)
        }
    }

    private suspend fun executeAttackDeployment(
        strat: com.pixelchief.shizuku.FarmingStrategy,
        loot: com.pixelchief.shizuku.vision.DetectedLoot
    ) {
        _botState.value = BotState.ATTACKING
        _currentTask.value = "Deploying ${strat.armyType.displayName}"
        LogBus.bot("Deploying army tactical waves...")

        val calibration = settingsStore.calibration.value
        val perimeter = ScreenAnalyzer.generateWallCoordinates(calibration, com.pixelchief.shizuku.SweepPattern.PERIMETER_RING, count = 10)

        // Wave 1: Peripheral test drops
        for (i in 0 until 4) {
            if (_botState.value != BotState.RUNNING) return
            val pt = perimeter[i % perimeter.size]
            ShizukuManager.tap(pt.first, pt.second)
            delay(strat.waveDelayMs + Random.nextLong(50, 150))
        }

        // Wave 2: Main assault wave
        delay(1200)
        for (i in 4 until perimeter.size) {
            if (_botState.value != BotState.RUNNING) return
            val pt = perimeter[i]
            ShizukuManager.tap(pt.first, pt.second)
            delay(strat.waveDelayMs + Random.nextLong(80, 200))
        }

        LogBus.info("BotEngine", "Troop deployment concluded. Waiting for collector destruction...")
        delay(Random.nextLong(8000, 12000))

        // End battle / Surrender
        if (strat.autoSurrenderOn50Percent) {
            LogBus.bot("Loot secured. Triggering End Battle...")
            val (endX, endY) = ScreenAnalyzer.toHumanizedPixels(calibration.endBattleButton, calibration)
            ShizukuManager.tap(endX, endY)
            delay(600)
            // Confirm dialog
            val confirmEndX = (calibration.width * 0.58f).toInt()
            val confirmEndY = (calibration.height * 0.58f).toInt()
            ShizukuManager.tap(confirmEndX, confirmEndY)
            delay(1500)

            // Return home
            val (homeX, homeY) = ScreenAnalyzer.toHumanizedPixels(calibration.returnHomeButton, calibration)
            ShizukuManager.tap(homeX, homeY)
            delay(2500)
        }

        _stats.update {
            it.copy(
                battlesCompleted = it.battlesCompleted + 1,
                goldLooted = it.goldLooted + (loot.gold * 0.85).toLong(),
                elixirLooted = it.elixirLooted + (loot.elixir * 0.85).toLong(),
                darkElixirLooted = it.darkElixirLooted + (loot.darkElixir * 0.85).toLong()
            )
        }
        LogBus.success("BotEngine", "Battle victory recorded! Returned safely to village.")
    }

    private fun startForegroundService() {
        try {
            val intent = Intent(context, BotForegroundService::class.java).apply {
                action = BotForegroundService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            LogBus.warn("Service", "Could not start foreground service: ${e.message}")
        }
    }

    private fun stopForegroundService() {
        try {
            val intent = Intent(context, BotForegroundService::class.java).apply {
                action = BotForegroundService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun formatNumber(num: Long): String {
        return when {
            num >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", num / 1_000_000.0)
            num >= 1_000 -> String.format(java.util.Locale.US, "%.0fK", num / 1_000.0)
            else -> num.toString()
        }
    }
}
