package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChainStatus(
    @Json(name = "height") val height: Long = 0,
    @Json(name = "difficulty") val difficulty: Long = 0,
    @Json(name = "protocol_version") val protocolVersion: Int = 1,
    @Json(name = "peers") val peers: Int = 0,
    @Json(name = "pending_transactions") val pendingTransactions: Int = 0
)

@JsonClass(generateAdapter = true)
data class BalanceResponse(
    @Json(name = "address") val address: String = "",
    @Json(name = "balance") val balance: String = "0.00000000",
    @Json(name = "balance_units") val balanceUnits: Long = 0,
    @Json(name = "nonce") val nonce: Long = 0
)

@JsonClass(generateAdapter = true)
data class TokenBalanceResponse(
    @Json(name = "address") val address: String = "",
    @Json(name = "token_id") val tokenId: String = "",
    @Json(name = "balance") val balance: String = "0.00000000",
    @Json(name = "balance_units") val balanceUnits: Long = 0
)

@JsonClass(generateAdapter = true)
data class TokenInfo(
    @Json(name = "token_id") val tokenId: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "symbol") val symbol: String = "",
    @Json(name = "contract_address") val contractAddress: String? = null,
    @Json(name = "total_supply") val totalSupply: String? = null,
    @Json(name = "total_supply_units") val totalSupplyUnits: Long? = null,
    @Json(name = "creator") val creator: String? = null,
    @Json(name = "created_at") val createdAt: Long? = null,
    // Transient UI property:
    val userBalance: String = "0.00000000",
    val userBalanceUnits: Long = 0
)

@JsonClass(generateAdapter = true)
data class TokensListResponse(
    @Json(name = "tokens") val tokens: List<TokenInfo> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SupplyResponse(
    @Json(name = "max_supply") val maxSupply: String? = null,
    @Json(name = "max_supply_units") val maxSupplyUnits: Long? = null,
    @Json(name = "total_circulating_supply") val totalCirculatingSupply: String? = null,
    @Json(name = "total_circulating_supply_units") val totalCirculatingSupplyUnits: Long? = null
)

@JsonClass(generateAdapter = true)
data class FeesResponse(
    @Json(name = "fee") val fee: String? = "0.00010000",
    @Json(name = "standard_fee") val standardFee: String? = null,
    @Json(name = "min_fee") val minFee: String? = null
)

@JsonClass(generateAdapter = true)
data class NewTransactionRequest(
    @Json(name = "sender") val sender: String,
    @Json(name = "recipient") val recipient: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "fee") val fee: String,
    @Json(name = "nonce") val nonce: Long,
    @Json(name = "public_key") val publicKey: String,
    @Json(name = "signature") val signature: String,
    @Json(name = "token_id") val tokenId: String? = null
)

@JsonClass(generateAdapter = true)
data class TransactionResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "tx_id") val txId: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockTemplate(
    @Json(name = "index") val index: Long? = null,
    @Json(name = "height") val height: Long? = null,
    @Json(name = "previous_hash") val previousHash: String? = null,
    @Json(name = "difficulty") val difficulty: Long = 1,
    @Json(name = "target") val target: String? = null,
    @Json(name = "miner") val miner: String? = null,
    @Json(name = "timestamp") val timestamp: Long? = null,
    @Json(name = "transactions") val transactions: List<Map<String, Any>>? = null,
    @Json(name = "nonce") val initialNonce: Long? = null
)

@JsonClass(generateAdapter = true)
data class BlockSubmitRequest(
    @Json(name = "miner") val miner: String,
    @Json(name = "nonce") val nonce: Long,
    @Json(name = "block_hash") val blockHash: String? = null,
    @Json(name = "index") val index: Long? = null,
    @Json(name = "previous_hash") val previousHash: String? = null
)

@JsonClass(generateAdapter = true)
data class BlockSubmitResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "accepted") val accepted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class JsonRpcRequest(
    @Json(name = "jsonrpc") val jsonrpc: String = "2.0",
    @Json(name = "method") val method: String,
    @Json(name = "params") val params: List<Any> = emptyList(),
    @Json(name = "id") val id: Int = 1
)

@JsonClass(generateAdapter = true)
data class JsonRpcResponse(
    @Json(name = "jsonrpc") val jsonrpc: String = "2.0",
    @Json(name = "id") val id: Int = 1,
    @Json(name = "result") val result: Any? = null,
    @Json(name = "error") val error: Map<String, Any>? = null
)
