package com.pixelchief.shizuku

enum class BotState {
    IDLE,
    RUNNING,
    PAUSED,
    SEARCHING,
    ATTACKING,
    UPGRADING_WALLS,
    COLLECTING,
    STOPPED
}

enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_DENIED,
    READY,
    CHECKING
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR,
    DEBUG,
    BOT
}

data class LogEntry(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String,
    val message: String
)

data class BotStats(
    val isRunning: Boolean = false,
    val sessionStartTime: Long = 0L,
    val runsCount: Int = 0,
    val wallsUpgraded: Int = 0,
    val goldSpentOnWalls: Long = 0L,
    val elixirSpentOnWalls: Long = 0L,
    val goldLooted: Long = 0L,
    val elixirLooted: Long = 0L,
    val darkElixirLooted: Long = 0L,
    val searchSkips: Int = 0,
    val battlesCompleted: Int = 0,
    val obstaclesCleared: Int = 0
)

enum class WallResourcePriority {
    GOLD_ONLY,
    ELIXIR_ONLY,
    BALANCED,
    CHEAPEST_FIRST
}

enum class SweepPattern {
    PERIMETER_RING,
    SPIRAL_INWARD,
    CORE_DEFENSE_FIRST,
    ROW_BY_ROW
}

data class WallUpgradeConfig(
    val autoUpgradeEnabled: Boolean = true,
    val minGoldReserve: Long = 1_500_000L,
    val minElixirReserve: Long = 1_500_000L,
    val priority: WallResourcePriority = WallResourcePriority.BALANCED,
    val targetWallLevel: Int = 14,
    val keepBuilderFree: Boolean = true,
    val sweepPattern: SweepPattern = SweepPattern.PERIMETER_RING,
    val upgradeIntervalMinutes: Int = 15,
    val testTapSimulation: Boolean = false
)

enum class ArmyType(val displayName: String, val description: String) {
    SNEAKY_GOBLINS("Sneaky Goblins", "Surgical looting of external and internal resource collectors & Town Hall"),
    BARCH("BARCH (Barb + Archer)", "Cost-effective classic peripheral dead-base farming sweep"),
    BABY_DRAGONS("Baby Dragons", "High DPS rage aura targeting outer pumps, drills, and storages"),
    ZAP_DRAGONS("Zap Dragons", "Lightning strikes on Air Defenses followed by dragon sweep"),
    CUSTOM_ARMY("Custom Composition", "Configurable deployment waves and custom troop placement")
}

data class FarmingStrategy(
    val id: String = "sneaky_gobs",
    val armyType: ArmyType = ArmyType.SNEAKY_GOBLINS,
    val minGoldLoot: Long = 450_000L,
    val minElixirLoot: Long = 450_000L,
    val minDarkElixirLoot: Long = 4_000L,
    val maxSearchSkips: Int = 45,
    val trophyRangeMin: Int = 1800,
    val trophyRangeMax: Int = 2600,
    val autoSurrenderOn50Percent: Boolean = true,
    val collectorOnlyFocus: Boolean = true,
    val waveDelayMs: Long = 350L,
    val randomDelayJitterMs: Long = 180L,
    val returnHomeAfterAttack: Boolean = true
)

data class TargetPoint(
    val id: String,
    val label: String,
    val normalizedX: Float, // 0.0f to 1.0f of screen width
    val normalizedY: Float  // 0.0f to 1.0f of screen height
)

data class ScreenCalibration(
    val width: Int = 1080,
    val height: Int = 2400,
    val densityDpi: Int = 420,
    val villageCenter: TargetPoint = TargetPoint("center", "Village Center", 0.50f, 0.50f),
    val attackButton: TargetPoint = TargetPoint("attack", "Attack Button", 0.08f, 0.90f),
    val findMatchButton: TargetPoint = TargetPoint("find_match", "Find Match", 0.85f, 0.72f),
    val nextMatchButton: TargetPoint = TargetPoint("next_match", "Next Button", 0.91f, 0.82f),
    val endBattleButton: TargetPoint = TargetPoint("end_battle", "Surrender / End", 0.09f, 0.78f),
    val returnHomeButton: TargetPoint = TargetPoint("return_home", "Return Home", 0.50f, 0.88f),
    val goldCollectorSample: TargetPoint = TargetPoint("gold_mine", "Gold Mine Sample", 0.38f, 0.42f),
    val elixirPumpSample: TargetPoint = TargetPoint("elixir_pump", "Elixir Collector", 0.62f, 0.45f)
)

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
)
