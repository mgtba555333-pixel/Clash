package com.pixelchief.shizuku.vision

import com.pixelchief.shizuku.ScreenCalibration
import com.pixelchief.shizuku.SweepPattern
import com.pixelchief.shizuku.TargetPoint
import kotlin.random.Random

data class DetectedLoot(
    val gold: Long,
    val elixir: Long,
    val darkElixir: Long,
    val isDeadBase: Boolean,
    val townHallLevel: Int
)

enum class ScreenState {
    VILLAGE,
    SEARCHING_MATCH,
    IN_BATTLE,
    BATTLE_END_SUMMARY,
    RELOAD_DIALOG,
    UNKNOWN
}

object ScreenAnalyzer {

    fun toScreenPixels(point: TargetPoint, calibration: ScreenCalibration): Pair<Int, Int> {
        val basePxX = (point.normalizedX * calibration.width).toInt()
        val basePxY = (point.normalizedY * calibration.height).toInt()
        return Pair(basePxX, basePxY)
    }

    fun toHumanizedPixels(point: TargetPoint, calibration: ScreenCalibration, maxJitterPx: Int = 8): Pair<Int, Int> {
        val (x, y) = toScreenPixels(point, calibration)
        val jitterX = Random.nextInt(-maxJitterPx, maxJitterPx + 1)
        val jitterY = Random.nextInt(-maxJitterPx, maxJitterPx + 1)
        val finalX = (x + jitterX).coerceIn(0, calibration.width - 1)
        val finalY = (y + jitterY).coerceIn(0, calibration.height - 1)
        return Pair(finalX, finalY)
    }

    fun generateWallCoordinates(
        calibration: ScreenCalibration,
        pattern: SweepPattern,
        count: Int = 12
    ): List<Pair<Int, Int>> {
        val center = toScreenPixels(calibration.villageCenter, calibration)
        val list = mutableListOf<Pair<Int, Int>>()
        val radiusStep = (calibration.width * 0.04f).toInt()

        when (pattern) {
            SweepPattern.PERIMETER_RING -> {
                val radius = (calibration.width * 0.28f).toInt()
                for (i in 0 until count) {
                    val angle = (2 * Math.PI * i / count)
                    val x = (center.first + radius * Math.cos(angle)).toInt().coerceIn(10, calibration.width - 10)
                    val y = (center.second + (radius * 0.65f) * Math.sin(angle)).toInt().coerceIn(10, calibration.height - 10)
                    list.add(Pair(x, y))
                }
            }
            SweepPattern.SPIRAL_INWARD -> {
                for (i in 0 until count) {
                    val radius = (calibration.width * (0.35f - (i * 0.015f))).toInt().coerceAtLeast(40)
                    val angle = (2 * Math.PI * i * 1.5 / count)
                    val x = (center.first + radius * Math.cos(angle)).toInt().coerceIn(10, calibration.width - 10)
                    val y = (center.second + (radius * 0.65f) * Math.sin(angle)).toInt().coerceIn(10, calibration.height - 10)
                    list.add(Pair(x, y))
                }
            }
            SweepPattern.CORE_DEFENSE_FIRST -> {
                for (i in 0 until count) {
                    val radius = (calibration.width * 0.12f).toInt() + (i % 3) * radiusStep
                    val angle = (2 * Math.PI * i / count)
                    val x = (center.first + radius * Math.cos(angle)).toInt().coerceIn(10, calibration.width - 10)
                    val y = (center.second + (radius * 0.65f) * Math.sin(angle)).toInt().coerceIn(10, calibration.height - 10)
                    list.add(Pair(x, y))
                }
            }
            SweepPattern.ROW_BY_ROW -> {
                val startX = center.first - (calibration.width * 0.25f).toInt()
                val startY = center.second - (calibration.height * 0.15f).toInt()
                for (i in 0 until count) {
                    val row = i / 4
                    val col = i % 4
                    val x = (startX + col * (calibration.width * 0.14f).toInt()).coerceIn(10, calibration.width - 10)
                    val y = (startY + row * (calibration.height * 0.10f).toInt()).coerceIn(10, calibration.height - 10)
                    list.add(Pair(x, y))
                }
            }
        }
        return list
    }

    fun generateResourceCollectorPoints(calibration: ScreenCalibration): List<Pair<Int, Int>> {
        val center = toScreenPixels(calibration.villageCenter, calibration)
        val ringRadius = (calibration.width * 0.24f).toInt()
        val collectors = mutableListOf<Pair<Int, Int>>()
        val angles = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)
        for (deg in angles) {
            val rad = Math.toRadians(deg)
            val x = (center.first + ringRadius * Math.cos(rad)).toInt().coerceIn(20, calibration.width - 20)
            val y = (center.second + (ringRadius * 0.68f) * Math.sin(rad)).toInt().coerceIn(20, calibration.height - 20)
            collectors.add(Pair(x, y))
        }
        return collectors
    }

    fun analyzeSimulatedMatchLoot(): DetectedLoot {
        val isDead = Random.nextDouble() < 0.35 // 35% chance of high loot dead base
        val gold = if (isDead) Random.nextLong(550_000, 1_450_000) else Random.nextLong(120_000, 480_000)
        val elixir = if (isDead) Random.nextLong(550_000, 1_450_000) else Random.nextLong(120_000, 480_000)
        val darkElixir = if (isDead) Random.nextLong(4_000, 12_500) else Random.nextLong(500, 3_500)
        val th = Random.nextInt(12, 17)
        return DetectedLoot(
            gold = gold,
            elixir = elixir,
            darkElixir = darkElixir,
            isDeadBase = isDead,
            townHallLevel = th
        )
    }
}
