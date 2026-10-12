package com.pixelchief.shizuku

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pixelchief_settings", Context.MODE_PRIVATE)

    private val _wallConfig = MutableStateFlow(loadWallConfig())
    val wallConfig: StateFlow<WallUpgradeConfig> = _wallConfig.asStateFlow()

    private val _strategy = MutableStateFlow(loadStrategy())
    val strategy: StateFlow<FarmingStrategy> = _strategy.asStateFlow()

    private val _calibration = MutableStateFlow(loadCalibration())
    val calibration: StateFlow<ScreenCalibration> = _calibration.asStateFlow()

    private val _gamePackage = MutableStateFlow(
        prefs.getString("game_package", "com.supercell.clashofclans") ?: "com.supercell.clashofclans"
    )
    val gamePackage: StateFlow<String> = _gamePackage.asStateFlow()

    private val _overlayEnabled = MutableStateFlow(
        prefs.getBoolean("overlay_enabled", false)
    )
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    fun updateWallConfig(config: WallUpgradeConfig) {
        _wallConfig.value = config
        prefs.edit().apply {
            putBoolean("wall_enabled", config.autoUpgradeEnabled)
            putLong("wall_min_gold", config.minGoldReserve)
            putLong("wall_min_elixir", config.minElixirReserve)
            putString("wall_priority", config.priority.name)
            putInt("wall_target_lvl", config.targetWallLevel)
            putBoolean("wall_keep_builder", config.keepBuilderFree)
            putString("wall_sweep", config.sweepPattern.name)
            putInt("wall_interval", config.upgradeIntervalMinutes)
            putBoolean("wall_sim", config.testTapSimulation)
            apply()
        }
    }

    private fun loadWallConfig(): WallUpgradeConfig {
        return WallUpgradeConfig(
            autoUpgradeEnabled = prefs.getBoolean("wall_enabled", true),
            minGoldReserve = prefs.getLong("wall_min_gold", 1_500_000L),
            minElixirReserve = prefs.getLong("wall_min_elixir", 1_500_000L),
            priority = runCatching {
                WallResourcePriority.valueOf(prefs.getString("wall_priority", "BALANCED") ?: "BALANCED")
            }.getOrDefault(WallResourcePriority.BALANCED),
            targetWallLevel = prefs.getInt("wall_target_lvl", 14),
            keepBuilderFree = prefs.getBoolean("wall_keep_builder", true),
            sweepPattern = runCatching {
                SweepPattern.valueOf(prefs.getString("wall_sweep", "PERIMETER_RING") ?: "PERIMETER_RING")
            }.getOrDefault(SweepPattern.PERIMETER_RING),
            upgradeIntervalMinutes = prefs.getInt("wall_interval", 15),
            testTapSimulation = prefs.getBoolean("wall_sim", false)
        )
    }

    fun updateStrategy(strat: FarmingStrategy) {
        _strategy.value = strat
        prefs.edit().apply {
            putString("strat_army", strat.armyType.name)
            putLong("strat_min_gold", strat.minGoldLoot)
            putLong("strat_min_elixir", strat.minElixirLoot)
            putLong("strat_min_de", strat.minDarkElixirLoot)
            putInt("strat_max_skips", strat.maxSearchSkips)
            putInt("strat_trophy_min", strat.trophyRangeMin)
            putInt("strat_trophy_max", strat.trophyRangeMax)
            putBoolean("strat_surrender_50", strat.autoSurrenderOn50Percent)
            putBoolean("strat_collector_focus", strat.collectorOnlyFocus)
            putLong("strat_wave_delay", strat.waveDelayMs)
            apply()
        }
    }

    private fun loadStrategy(): FarmingStrategy {
        return FarmingStrategy(
            armyType = runCatching {
                ArmyType.valueOf(prefs.getString("strat_army", "SNEAKY_GOBLINS") ?: "SNEAKY_GOBLINS")
            }.getOrDefault(ArmyType.SNEAKY_GOBLINS),
            minGoldLoot = prefs.getLong("strat_min_gold", 500_000L),
            minElixirLoot = prefs.getLong("strat_min_elixir", 500_000L),
            minDarkElixirLoot = prefs.getLong("strat_min_de", 4_500L),
            maxSearchSkips = prefs.getInt("strat_max_skips", 50),
            trophyRangeMin = prefs.getInt("strat_trophy_min", 1800),
            trophyRangeMax = prefs.getInt("strat_trophy_max", 2600),
            autoSurrenderOn50Percent = prefs.getBoolean("strat_surrender_50", true),
            collectorOnlyFocus = prefs.getBoolean("strat_collector_focus", true),
            waveDelayMs = prefs.getLong("strat_wave_delay", 350L)
        )
    }

    fun updateCalibration(cal: ScreenCalibration) {
        _calibration.value = cal
        prefs.edit().apply {
            putInt("cal_w", cal.width)
            putInt("cal_h", cal.height)
            putFloat("cal_center_x", cal.villageCenter.normalizedX)
            putFloat("cal_center_y", cal.villageCenter.normalizedY)
            putFloat("cal_attack_x", cal.attackButton.normalizedX)
            putFloat("cal_attack_y", cal.attackButton.normalizedY)
            putFloat("cal_find_x", cal.findMatchButton.normalizedX)
            putFloat("cal_find_y", cal.findMatchButton.normalizedY)
            putFloat("cal_next_x", cal.nextMatchButton.normalizedX)
            putFloat("cal_next_y", cal.nextMatchButton.normalizedY)
            putFloat("cal_end_x", cal.endBattleButton.normalizedX)
            putFloat("cal_end_y", cal.endBattleButton.normalizedY)
            putFloat("cal_home_x", cal.returnHomeButton.normalizedX)
            putFloat("cal_home_y", cal.returnHomeButton.normalizedY)
            apply()
        }
    }

    private fun loadCalibration(): ScreenCalibration {
        return ScreenCalibration(
            width = prefs.getInt("cal_w", 1080),
            height = prefs.getInt("cal_h", 2400),
            villageCenter = TargetPoint(
                "center", "Village Center",
                prefs.getFloat("cal_center_x", 0.50f),
                prefs.getFloat("cal_center_y", 0.50f)
            ),
            attackButton = TargetPoint(
                "attack", "Attack Button",
                prefs.getFloat("cal_attack_x", 0.08f),
                prefs.getFloat("cal_attack_y", 0.90f)
            ),
            findMatchButton = TargetPoint(
                "find_match", "Find Match",
                prefs.getFloat("cal_find_x", 0.85f),
                prefs.getFloat("cal_find_y", 0.72f)
            ),
            nextMatchButton = TargetPoint(
                "next_match", "Next Button",
                prefs.getFloat("cal_next_x", 0.91f),
                prefs.getFloat("cal_next_y", 0.82f)
            ),
            endBattleButton = TargetPoint(
                "end_battle", "Surrender / End",
                prefs.getFloat("cal_end_x", 0.09f),
                prefs.getFloat("cal_end_y", 0.78f)
            ),
            returnHomeButton = TargetPoint(
                "return_home", "Return Home",
                prefs.getFloat("cal_home_x", 0.50f),
                prefs.getFloat("cal_home_y", 0.88f)
            )
        )
    }

    fun setOverlayEnabled(enabled: Boolean) {
        _overlayEnabled.value = enabled
        prefs.edit().putBoolean("overlay_enabled", enabled).apply()
    }

    fun setGamePackage(pkg: String) {
        _gamePackage.value = pkg
        prefs.edit().putString("game_package", pkg).apply()
    }
}
