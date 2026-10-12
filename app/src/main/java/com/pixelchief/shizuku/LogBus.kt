package com.pixelchief.shizuku

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogBus {
    private const val MAX_LOGS = 250
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(
                tag = "System",
                level = LogLevel.INFO,
                message = "PixelChief Core initialized. Ready for Shizuku ADB binding."
            )
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(tag: String, level: LogLevel, message: String) {
        val entry = LogEntry(
            tag = tag,
            level = level,
            message = message
        )
        _logs.update { current ->
            (listOf(entry) + current).take(MAX_LOGS)
        }
    }

    fun info(tag: String, message: String) = log(tag, LogLevel.INFO, message)
    fun success(tag: String, message: String) = log(tag, LogLevel.SUCCESS, message)
    fun warn(tag: String, message: String) = log(tag, LogLevel.WARN, message)
    fun error(tag: String, message: String) = log(tag, LogLevel.ERROR, message)
    fun debug(tag: String, message: String) = log(tag, LogLevel.DEBUG, message)
    fun bot(message: String) = log("BotEngine", LogLevel.BOT, message)

    fun clear() {
        _logs.value = listOf(
            LogEntry(
                tag = "Terminal",
                level = LogLevel.INFO,
                message = "Terminal buffer cleared."
            )
        )
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
}
