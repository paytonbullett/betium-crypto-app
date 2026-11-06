package com.example.data.miner

import com.example.data.model.BlockTemplate

data class MinerState(
    val isRunning: Boolean = false,
    val hashrate: Long = 0L, // H/s
    val blocksFound: Int = 0,
    val totalHashes: Long = 0L,
    val currentHeight: Long = 0L,
    val statusMessage: String = "Idle",
    val isThrottled: Boolean = false,
    val minerAddress: String = "",
    val currentTemplate: BlockTemplate? = null,
    val recentLogs: List<String> = emptyList()
)
