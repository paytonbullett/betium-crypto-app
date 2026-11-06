package com.example.data.api

import com.example.data.model.BalanceResponse
import com.example.data.model.BlockSubmitRequest
import com.example.data.model.BlockSubmitResponse
import com.example.data.model.BlockTemplate
import com.example.data.model.ChainStatus
import com.example.data.model.FeesResponse
import com.example.data.model.JsonRpcRequest
import com.example.data.model.JsonRpcResponse
import com.example.data.model.NewTransactionRequest
import com.example.data.model.SupplyResponse
import com.example.data.model.TokenBalanceResponse
import com.example.data.model.TokenInfo
import com.example.data.model.TokensListResponse
import com.example.data.model.TransactionResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface BetiumApiService {
    @GET("chain/status")
    suspend fun getChainStatus(): ChainStatus

    @GET("balance/{address}")
    suspend fun getBalance(
        @Path("address") address: String
    ): BalanceResponse

    @GET("balance/{address}/{token_id}")
    suspend fun getTokenBalance(
        @Path("address") address: String,
        @Path("token_id") tokenId: String
    ): TokenBalanceResponse

    @GET("tokens")
    suspend fun getTokens(): TokensListResponse

    @GET("token/{token_id}")
    suspend fun getTokenInfo(
        @Path("token_id") tokenId: String
    ): TokenInfo

    @GET("supply")
    suspend fun getSupply(): SupplyResponse

    @GET("fees")
    suspend fun getFees(): FeesResponse

    @POST("transaction/new")
    suspend fun submitTransaction(
        @Body tx: NewTransactionRequest
    ): TransactionResponse

    @GET("mine/template")
    suspend fun getMiningTemplate(
        @Query("miner") miner: String
    ): BlockTemplate

    @POST("block/submit")
    suspend fun submitBlock(
        @Body block: BlockSubmitRequest
    ): BlockSubmitResponse

    @POST("rpc")
    suspend fun jsonRpc(
        @Body rpc: JsonRpcRequest
    ): JsonRpcResponse

    @GET("network")
    suspend fun getNetworkInfo(): Map<String, Any>
}

object ApiClientProvider {
    private var currentBaseUrl: String = "https://crypto.bullettsocial.space"
    private var cachedService: BetiumApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Synchronized
    fun getService(baseUrl: String = currentBaseUrl): BetiumApiService {
        val normalized = normalizeBaseUrl(baseUrl)
        if (cachedService != null && currentBaseUrl == normalized) {
            return cachedService!!
        }

        currentBaseUrl = normalized
        val retrofit = Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        val service = retrofit.create(BetiumApiService::class.java)
        cachedService = service
        return service
    }

    fun normalizeBaseUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        if (!clean.endsWith("/")) {
            clean = "$clean/"
        }
        return clean
    }

    suspend fun testNode(url: String): Pair<Boolean, String> {
        val startTime = System.currentTimeMillis()
        return try {
            val service = getService(url)
            val status = service.getChainStatus()
            val latency = System.currentTimeMillis() - startTime
            Pair(true, "Height #${status.height} • Diff ${status.difficulty} • ${latency}ms latency")
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Connection failed")
        }
    }
}
