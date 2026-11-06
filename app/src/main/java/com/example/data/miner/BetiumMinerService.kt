package com.example.data.miner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.api.ApiClientProvider
import com.example.data.crypto.Secp256k1Helper
import com.example.data.model.BlockSubmitRequest
import com.example.data.model.BlockTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigInteger

class BetiumMinerService : Service() {

    companion object {
        const val ACTION_START = "com.example.betium.ACTION_START_MINER"
        const val ACTION_STOP = "com.example.betium.ACTION_STOP_MINER"
        const val EXTRA_MINER_ADDRESS = "extra_miner_address"
        const val EXTRA_NODE_URL = "extra_node_url"
        private const val CHANNEL_ID = "betium_miner_channel"
        private const val NOTIFICATION_ID = 9472
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var minerAddress: String = ""
    private var nodeUrl: String = "https://crypto.bullettsocial.space"
    private var isMining = false
    private var blocksFoundCount = 0
    private var totalHashesCount = 0L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkBatteryState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                minerAddress = intent.getStringExtra(EXTRA_MINER_ADDRESS) ?: ""
                nodeUrl = intent.getStringExtra(EXTRA_NODE_URL) ?: "https://crypto.bullettsocial.space"
                if (minerAddress.isNotBlank() && !isMining) {
                    startMiningProcess()
                }
            }
            ACTION_STOP -> {
                stopMiningProcess()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startMiningProcess() {
        isMining = true
        startForeground(NOTIFICATION_ID, buildNotification("Starting Betium miner...", 0L, blocksFoundCount))

        MinerManager.updateState {
            it.copy(
                isRunning = true,
                minerAddress = minerAddress,
                statusMessage = "Miner initialized. Requesting template..."
            )
        }
        MinerManager.log("Miner started for address: ${minerAddress.take(10)}...${minerAddress.takeLast(6)}")
        MinerManager.log("Connecting to node: $nodeUrl")

        serviceScope.launch {
            val apiService = ApiClientProvider.getService(nodeUrl)

            var lastHashrateCalcTime = System.currentTimeMillis()
            var hashesSinceLastCalc = 0L
            var currentHashrate = 0L
            var lastNotificationUpdate = 0L

            while (isActive && isMining) {
                try {
                    // Check battery throttle
                    val isThrottled = checkBatteryState()
                    if (isThrottled) {
                        MinerManager.updateState { it.copy(isThrottled = true, statusMessage = "Paused (Low battery / Thermal)") }
                        delay(5000)
                        continue
                    }

                    MinerManager.updateState { it.copy(isThrottled = false, statusMessage = "Fetching block template...") }
                    val template: BlockTemplate = try {
                        apiService.getMiningTemplate(minerAddress)
                    } catch (e: Exception) {
                        MinerManager.log("Template fetch failed: ${e.message}. Retrying in 4s...")
                        delay(4000)
                        continue
                    }

                    val blockIndex = template.index ?: template.height ?: 0L
                    val prevHash = template.previousHash ?: "0".repeat(64)
                    val difficulty = if (template.difficulty > 0) template.difficulty else 8L
                    val protocolVersion = template.protocolVersion
                    val merkleRoot = template.merkleRoot ?: ""
                    val transactions = template.transactions ?: emptyList()
                    val txJson = canonicalizeJson(transactions)
                    val timestamp = template.timestamp ?: (System.currentTimeMillis() / 1000)

                    // Betium canonical block JSON prefix/suffix for ultra-fast native PoW hashing
                    // Keys in strict alphabetical order: difficulty, index, merkle_root, nonce, previous_hash, protocol_version, timestamp, transactions
                    val prefix = """{"difficulty":$difficulty,"index":$blockIndex,"merkle_root":"$merkleRoot","nonce":"""
                    val suffix = ""","previous_hash":"$prevHash","protocol_version":$protocolVersion,"timestamp":$timestamp,"transactions":$txJson}"""

                    MinerManager.updateState {
                        it.copy(
                            currentHeight = blockIndex,
                            currentTemplate = template,
                            statusMessage = "Mining block #$blockIndex (Difficulty $difficulty)"
                        )
                    }
                    MinerManager.log("Template loaded: Block #$blockIndex • Diff $difficulty")

                    var nonce = template.initialNonce ?: (System.currentTimeMillis() and 0xFFFFL)
                    var blockSolved = false

                    // Mine in chunks of 600 hashes to yield coroutine & update stats
                    val batchSize = 600
                    while (isActive && isMining && !blockSolved) {
                        for (i in 0 until batchSize) {
                            nonce++
                            totalHashesCount++
                            hashesSinceLastCalc++

                            // Construct canonical Betium block JSON for candidate nonce
                            val candidateJson = prefix + nonce + suffix
                            val hashHex = Secp256k1Helper.sha256Hex(candidateJson.toByteArray(Charsets.UTF_8))

                            // Check difficulty target using exact bit-shift test: (hashBigInt >> (256 - difficulty)) == 0
                            val hashBigInt = BigInteger(hashHex, 16)
                            val shiftAmount = 256 - difficulty.toInt()
                            val meetsTarget = if (shiftAmount >= 0) {
                                hashBigInt.shiftRight(shiftAmount) == BigInteger.ZERO
                            } else {
                                countLeadingZeroBits(hashHex) >= difficulty
                            }

                            if (meetsTarget) {
                                blockSolved = true
                                MinerManager.log("🎉 BLOCK SOLVED! Nonce: $nonce")
                                MinerManager.log("Hash: $hashHex")
                                MinerManager.log("Hashed JSON payload: $candidateJson")

                                val submitReq = BlockSubmitRequest(
                                    difficulty = difficulty,
                                    index = blockIndex,
                                    merkleRoot = merkleRoot,
                                    nonce = nonce,
                                    previousHash = prevHash,
                                    protocolVersion = protocolVersion,
                                    timestamp = timestamp,
                                    transactions = transactions,
                                    hash = hashHex,
                                    miner = minerAddress
                                )

                                try {
                                    val submitRes = apiService.submitBlock(submitReq)
                                    val accepted = submitRes.accepted ||
                                            submitRes.status.equals("ok", ignoreCase = true) ||
                                            submitRes.status.equals("success", ignoreCase = true) ||
                                            submitRes.error == null

                                    if (accepted) {
                                        blocksFoundCount++
                                        MinerManager.log("Block submitted successfully: ${submitRes.message ?: submitRes.status ?: "Accepted"}")
                                        MinerManager.updateState {
                                            it.copy(
                                                blocksFound = blocksFoundCount,
                                                statusMessage = "Block #$blockIndex accepted! Reward credited."
                                            )
                                        }
                                    } else {
                                        val errDetails = submitRes.error ?: submitRes.message ?: "Unknown node rejection"
                                        MinerManager.log("❌ Node rejected block #$blockIndex: $errDetails")
                                    }
                                } catch (e: retrofit2.HttpException) {
                                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                                    val nodeMsg = try {
                                        val adapter = com.squareup.moshi.Moshi.Builder().build().adapter(Map::class.java)
                                        val map = adapter.fromJson(errorBody)
                                        (map?.get("error") ?: map?.get("message") ?: "HTTP ${e.code()}").toString()
                                    } catch (parseErr: Exception) {
                                        "HTTP ${e.code()}: $errorBody"
                                    }
                                    MinerManager.log("❌ Node Error (${e.code()}): $nodeMsg")
                                    MinerManager.log("Payload hashed: $candidateJson")
                                } catch (subErr: Exception) {
                                    MinerManager.log("❌ Block submission exception: ${subErr.localizedMessage ?: subErr.message}")
                                }
                                break
                            }
                        }

                        // Periodic Hashrate Calculation
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastHashrateCalcTime
                        if (elapsed >= 1000) {
                            currentHashrate = (hashesSinceLastCalc * 1000L) / elapsed
                            hashesSinceLastCalc = 0
                            lastHashrateCalcTime = now

                            MinerManager.updateState {
                                it.copy(
                                    hashrate = currentHashrate,
                                    totalHashes = totalHashesCount,
                                    blocksFound = blocksFoundCount
                                )
                            }

                            // Update notification every 3 seconds
                            if (now - lastNotificationUpdate >= 3000) {
                                lastNotificationUpdate = now
                                updateNotification(
                                    "Mining block #$blockIndex • Diff $difficulty",
                                    currentHashrate,
                                    blocksFoundCount
                                )
                            }
                        }

                        // Small cooperative pause to avoid blocking UI thread
                        delay(2)
                    }

                } catch (e: Exception) {
                    MinerManager.log("Miner loop error: ${e.message}")
                    delay(3000)
                }
            }
        }
    }

    private fun countLeadingZeroBits(hex: String): Int {
        var count = 0
        for (c in hex) {
            val digit = Character.digit(c, 16)
            if (digit < 0) break
            val zeros = when (digit) {
                0 -> 4
                1 -> 3
                2, 3 -> 2
                4, 5, 6, 7 -> 1
                else -> 0
            }
            count += zeros
            if (digit != 0) break
        }
        return count
    }

    private fun checkBatteryState(): Boolean {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        // Throttle if below 15% and not connected to a charger
        return batteryPct < 15 && !isCharging
    }

    private fun stopMiningProcess() {
        isMining = false
        MinerManager.updateState {
            it.copy(
                isRunning = false,
                hashrate = 0L,
                statusMessage = "Miner stopped"
            )
        }
        MinerManager.log("Miner stopped by user.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Betium Light Miner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status and hashrate of the Betium light miner"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String, hashrate: Long, blocks: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, BetiumMinerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val hrText = if (hashrate >= 1000) "${hashrate / 1000.0} kH/s" else "$hashrate H/s"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Betium Light Miner")
            .setContentText("$statusText • $hrText • Blocks: $blocks")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop Mining", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String, hashrate: Long, blocks: Int) {
        val notification = buildNotification(statusText, hashrate, blocks)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        stopMiningProcess()
        serviceJob.cancel()
    }

    private fun canonicalizeJson(obj: Any?): String {
        return when (obj) {
            null -> "null"
            is String -> "\"$obj\""
            is Number -> {
                val d = obj.toDouble()
                if (d == d.toLong().toDouble()) {
                    d.toLong().toString()
                } else {
                    // Avoid scientific notation and ensure compact formatting
                    java.math.BigDecimal(obj.toString()).stripTrailingZeros().toPlainString()
                }
            }
            is Boolean -> obj.toString()
            is Map<*, *> -> {
                val entries = obj.entries
                    .filter { it.key != null && it.value != null }
                    .sortedBy { it.key.toString() }
                    .joinToString(",") { (k, v) ->
                        "\"$k\":${canonicalizeJson(v)}"
                    }
                "{$entries}"
            }
            is List<*> -> {
                val items = obj.joinToString(",") { canonicalizeJson(it) }
                "[$items]"
            }
            else -> "\"$obj\""
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
