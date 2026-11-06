package com.example.data.miner

import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MinerManager {
    private val _minerState = MutableStateFlow(MinerState())
    val minerState: StateFlow<MinerState> = _minerState.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun startMining(context: Context, minerAddress: String, nodeUrl: String) {
        val intent = Intent(context, BetiumMinerService::class.java).apply {
            action = BetiumMinerService.ACTION_START
            putExtra(BetiumMinerService.EXTRA_MINER_ADDRESS, minerAddress)
            putExtra(BetiumMinerService.EXTRA_NODE_URL, nodeUrl)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopMining(context: Context) {
        val intent = Intent(context, BetiumMinerService::class.java).apply {
            action = BetiumMinerService.ACTION_STOP
        }
        context.startService(intent)
    }

    internal fun updateState(transform: (MinerState) -> MinerState) {
        _minerState.update(transform)
    }

    internal fun log(message: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $message"
        _minerState.update { current ->
            val updatedLogs = (current.recentLogs + entry).takeLast(60)
            current.copy(recentLogs = updatedLogs)
        }
    }

    internal fun clearLogs() {
        _minerState.update { it.copy(recentLogs = emptyList()) }
    }
}
