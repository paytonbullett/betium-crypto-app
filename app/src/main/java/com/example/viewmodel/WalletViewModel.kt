package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClientProvider
import com.example.data.crypto.Bip39Helper
import com.example.data.crypto.KeyPairInfo
import com.example.data.crypto.Secp256k1Helper
import com.example.data.model.BalanceResponse
import com.example.data.model.ChainStatus
import com.example.data.model.FeesResponse
import com.example.data.model.NewTransactionRequest
import com.example.data.model.SupplyResponse
import com.example.data.model.TokenInfo
import com.example.data.model.TransactionResponse
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.storage.SecureKeyStorage
import com.example.data.storage.StoredWallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

data class WalletUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val wallet: StoredWallet? = null,
    val nativeBalance: String = "0.00000000",
    val nativeBalanceUnits: Long = 0L,
    val nonce: Long = 0L,
    val chainStatus: ChainStatus? = null,
    val supply: SupplyResponse? = null,
    val fees: FeesResponse? = null,
    val tokens: List<TokenInfo> = emptyList(),
    val isNodeConnected: Boolean = false,
    val nodeLatencyMs: Long = -1L,
    val errorMessage: String? = null
)

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data class TransactionSuccess(val txId: String) : UiEvent
    data class TransactionFailed(val error: String) : UiEvent
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureKeyStorage(application)
    private val preferencesRepo = UserPreferencesRepository(application)

    val userPreferences: StateFlow<UserPreferences> = preferencesRepo.userPreferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UserPreferences()
        )

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        loadWalletFromStorage()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferences.collect { prefs ->
                if (_uiState.value.wallet != null) {
                    refreshData(prefs.nodeUrl)
                }
            }
        }
    }

    fun loadWalletFromStorage() {
        val stored = secureStorage.getWallet()
        _uiState.update { it.copy(wallet = stored) }
        if (stored != null) {
            refreshData(userPreferences.value.nodeUrl)
        }
    }

    /**
     * Creates a fresh Betium wallet using cryptographically secure secp256k1
     * and generates a 12-word BIP-39 mnemonic phrase.
     */
    fun createNewWallet(walletName: String = "Betium Main Wallet"): Pair<StoredWallet, List<String>> {
        val mnemonic = Bip39Helper.generateMnemonic()
        val privateKeyHex = Bip39Helper.mnemonicToPrivateKeyHex(mnemonic)
        val keyPair = Secp256k1Helper.fromPrivateKeyHex(privateKeyHex)
        val stored = secureStorage.saveWallet(
            keyPair = keyPair,
            name = walletName,
            mnemonic = mnemonic.joinToString(" ")
        )
        _uiState.update { it.copy(wallet = stored) }
        refreshData(userPreferences.value.nodeUrl)
        return Pair(stored, mnemonic)
    }

    /**
     * Restores a wallet from a 12-word BIP-39 mnemonic phrase.
     */
    fun restoreFromMnemonic(words: List<String>, walletName: String = "Restored Wallet"): Result<StoredWallet> {
        return try {
            val cleanWords = words.map { it.trim().lowercase() }.filter { it.isNotBlank() }
            if (cleanWords.size != 12) {
                return Result.failure(IllegalArgumentException("Mnemonic must contain exactly 12 words"))
            }
            if (!Bip39Helper.validateMnemonic(cleanWords)) {
                return Result.failure(IllegalArgumentException("One or more words are not valid BIP-39 words"))
            }
            val privateKeyHex = Bip39Helper.mnemonicToPrivateKeyHex(cleanWords)
            val keyPair = Secp256k1Helper.fromPrivateKeyHex(privateKeyHex)
            val stored = secureStorage.saveWallet(
                keyPair = keyPair,
                name = walletName,
                mnemonic = cleanWords.joinToString(" ")
            )
            _uiState.update { it.copy(wallet = stored) }
            refreshData(userPreferences.value.nodeUrl)
            Result.success(stored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores a wallet directly from a raw 64-character hex private key.
     */
    fun restoreFromPrivateKey(rawHex: String, walletName: String = "Imported Key Wallet"): Result<StoredWallet> {
        return try {
            val keyPair = Secp256k1Helper.fromPrivateKeyHex(rawHex)
            val stored = secureStorage.saveWallet(
                keyPair = keyPair,
                name = walletName,
                mnemonic = null
            )
            _uiState.update { it.copy(wallet = stored) }
            refreshData(userPreferences.value.nodeUrl)
            Result.success(stored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun wipeWallet() {
        secureStorage.wipeWallet()
        _uiState.update {
            WalletUiState(
                wallet = null,
                nativeBalance = "0.00000000",
                tokens = emptyList()
            )
        }
    }

    fun refreshData(customNodeUrl: String? = null) {
        val activeWallet = _uiState.value.wallet ?: return
        val nodeUrl = customNodeUrl ?: userPreferences.value.nodeUrl

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    val api = ApiClientProvider.getService(nodeUrl)
                    val startTime = System.currentTimeMillis()

                    // 1. Fetch chain status & test connection
                    val chainStatus = try {
                        api.getChainStatus()
                    } catch (e: Exception) {
                        null
                    }
                    val latency = System.currentTimeMillis() - startTime
                    val isConnected = chainStatus != null

                    // 2. Fetch Native BETM Balance (Dual check: Native 64-hex and Ethereum 0x...)
                    val nativeBal = try {
                        api.getBalance(activeWallet.nativeAddress)
                    } catch (e: Exception) {
                        null
                    }

                    val ethBal = try {
                        api.getBalance(activeWallet.ethAddress)
                    } catch (e: Exception) {
                        null
                    }

                    // Use the non-zero / higher balance
                    val balanceRes = when {
                        nativeBal != null && ethBal != null -> {
                            if (ethBal.balanceUnits > nativeBal.balanceUnits) ethBal else nativeBal
                        }
                        nativeBal != null -> nativeBal
                        ethBal != null -> ethBal
                        else -> BalanceResponse(
                            address = activeWallet.nativeAddress,
                            balance = userPreferences.value.lastKnownBalance
                        )
                    }

                    // 3. Fetch Supply and Fees
                    val supply = try { api.getSupply() } catch (e: Exception) { null }
                    val fees = try { api.getFees() } catch (e: Exception) { null }

                    // 4. Token Auto-Detect: Dual check on both nativeAddress and ethAddress
                    val detectedTokens = try {
                        val allTokens = api.getTokens().tokens
                        allTokens.map { token ->
                            val nativeTokenBal = try {
                                api.getTokenBalance(activeWallet.nativeAddress, token.tokenId)
                            } catch (e: Exception) {
                                null
                            }
                            val ethTokenBal = try {
                                api.getTokenBalance(activeWallet.ethAddress, token.tokenId)
                            } catch (e: Exception) {
                                null
                            }

                            val bestTokenBal = when {
                                nativeTokenBal != null && ethTokenBal != null -> {
                                    if (ethTokenBal.balanceUnits > nativeTokenBal.balanceUnits) ethTokenBal else nativeTokenBal
                                }
                                nativeTokenBal != null -> nativeTokenBal
                                ethTokenBal != null -> ethTokenBal
                                else -> null
                            }

                            token.copy(
                                userBalance = bestTokenBal?.balance ?: "0.00000000",
                                userBalanceUnits = bestTokenBal?.balanceUnits ?: 0L
                            )
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }

                    // Save last known state to DataStore
                    preferencesRepo.updateLastKnownState(
                        balance = balanceRes.balance,
                        height = chainStatus?.height ?: 0L
                    )

                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            isNodeConnected = isConnected,
                            nodeLatencyMs = if (isConnected) latency else -1L,
                            chainStatus = chainStatus,
                            supply = supply,
                            fees = fees,
                            nativeBalance = balanceRes.balance,
                            nativeBalanceUnits = balanceRes.balanceUnits,
                            nonce = balanceRes.nonce,
                            tokens = detectedTokens
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        isNodeConnected = false,
                        errorMessage = e.localizedMessage ?: "Failed to connect to Betium node"
                    )
                }
            }
        }
    }

    /**
     * Builds, canonically signs, and broadcasts a Betium transaction.
     */
    fun sendTransaction(
        recipientAddress: String,
        amountText: String,
        feeText: String = "0.00010000",
        tokenId: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        val activeWallet = _uiState.value.wallet ?: run {
            onComplete(false, "No active wallet found")
            return
        }

        val cleanRecipient = recipientAddress.trim()
        if (!Secp256k1Helper.isValidAddress(cleanRecipient)) {
            onComplete(false, "Invalid recipient address. Must be a 64-char native hex or 0x... Ethereum address.")
            return
        }

        val amountBd = amountText.toBigDecimalOrNull()
        if (amountBd == null || amountBd <= BigDecimal.ZERO) {
            onComplete(false, "Please enter a valid amount greater than 0")
            return
        }

        // Format amount and fee strictly to 8 decimal places
        val formattedAmount = amountBd.setScale(8, RoundingMode.HALF_UP).toPlainString()
        val formattedFee = (feeText.toBigDecimalOrNull() ?: BigDecimal("0.00010000"))
            .setScale(8, RoundingMode.HALF_UP).toPlainString()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val api = ApiClientProvider.getService(userPreferences.value.nodeUrl)

                    // Fetch latest balance & nonce across both addresses
                    val nativeBal = try { api.getBalance(activeWallet.nativeAddress) } catch (e: Exception) { null }
                    val ethBal = try { api.getBalance(activeWallet.ethAddress) } catch (e: Exception) { null }
                    val latestBalance = when {
                        nativeBal != null && ethBal != null -> if (ethBal.balanceUnits > nativeBal.balanceUnits) ethBal else nativeBal
                        nativeBal != null -> nativeBal
                        ethBal != null -> ethBal
                        else -> api.getBalance(activeWallet.nativeAddress)
                    }
                    val nonce = latestBalance.nonce + 1
                    val senderAddress = if (latestBalance.address.isNotBlank()) latestBalance.address else activeWallet.nativeAddress

                    // Check balance for BETM
                    if (tokenId.isNullOrBlank()) {
                        val currentBalBd = BigDecimal(latestBalance.balance)
                        val totalRequired = amountBd + BigDecimal(formattedFee)
                        if (currentBalBd < totalRequired) {
                            return@withContext Pair(false, "Insufficient balance: $currentBalBd BETM (Required: $totalRequired BETM)")
                        }
                    }

                    // Build canonical transaction payload for signing
                    val canonicalJson = Secp256k1Helper.createCanonicalTxPayload(
                        sender = senderAddress,
                        recipient = cleanRecipient,
                        amount = formattedAmount,
                        fee = formattedFee,
                        nonce = nonce,
                        tokenId = tokenId
                    )

                    // Deterministic RFC 6979 ECDSA signature using secp256k1 private key
                    val signatureHex = Secp256k1Helper.signMessage(
                        privateKeyHex = activeWallet.privateKeyHex,
                        messageBytes = canonicalJson.toByteArray(Charsets.UTF_8)
                    )

                    val request = NewTransactionRequest(
                        sender = senderAddress,
                        recipient = cleanRecipient,
                        amount = formattedAmount,
                        fee = formattedFee,
                        nonce = nonce,
                        publicKey = activeWallet.publicKeyHex,
                        signature = signatureHex,
                        tokenId = tokenId
                    )

                    val response: TransactionResponse = api.submitTransaction(request)
                    val txId = response.txId ?: "tx_${System.currentTimeMillis()}"
                    val success = response.status.equals("ok", ignoreCase = true) ||
                            response.status.equals("success", ignoreCase = true) ||
                            response.error == null

                    val msg = if (success) {
                        "Transaction submitted successfully! ID: ${txId.take(16)}..."
                    } else {
                        response.error ?: response.message ?: "Transaction rejected by node"
                    }

                    Pair(success, msg)
                }

                _uiState.update { it.copy(isLoading = false) }
                if (result.first) {
                    _events.emit(UiEvent.TransactionSuccess(result.second))
                    refreshData()
                } else {
                    _events.emit(UiEvent.TransactionFailed(result.second))
                }
                onComplete(result.first, result.second)

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                val errorMsg = e.localizedMessage ?: "Network or broadcast failure"
                _events.emit(UiEvent.TransactionFailed(errorMsg))
                onComplete(false, errorMsg)
            }
        }
    }

    fun updateNodeUrl(newUrl: String) {
        viewModelScope.launch {
            preferencesRepo.updateNodeUrl(newUrl)
            refreshData(newUrl)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepo.updateThemeMode(mode)
        }
    }

    fun updateAccentColor(hex: String) {
        viewModelScope.launch {
            preferencesRepo.updateAccentColor(hex)
        }
    }

    fun updateOledDark(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateOledDark(enabled)
        }
    }

    fun updateShowAllTokens(showAll: Boolean) {
        viewModelScope.launch {
            preferencesRepo.updateShowAllTokens(showAll)
        }
    }

    suspend fun testNodeConnection(url: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            ApiClientProvider.testNode(url)
        }
    }
}
