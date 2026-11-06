package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.crypto.KeyPairInfo
import com.example.data.crypto.Secp256k1Helper

data class StoredWallet(
    val name: String,
    val privateKeyHex: String,
    val publicKeyHex: String,
    val nativeAddress: String,
    val ethAddress: String,
    val mnemonic: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

class SecureKeyStorage(context: Context) {
    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "betium_secure_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_HAS_WALLET = "has_wallet"
        private const val KEY_WALLET_NAME = "wallet_name"
        private const val KEY_PRIV_KEY = "private_key_hex"
        private const val KEY_PUB_KEY = "public_key_hex"
        private const val KEY_NATIVE_ADDR = "native_address"
        private const val KEY_ETH_ADDR = "eth_address"
        private const val KEY_MNEMONIC = "mnemonic_phrase"
        private const val KEY_CREATED_AT = "created_at"
    }

    fun hasWallet(): Boolean {
        return prefs.getBoolean(KEY_HAS_WALLET, false) && !getPrivateKey().isNullOrBlank()
    }

    fun saveWallet(
        keyPair: KeyPairInfo,
        name: String = "Betium Main Wallet",
        mnemonic: String? = null
    ): StoredWallet {
        prefs.edit()
            .putBoolean(KEY_HAS_WALLET, true)
            .putString(KEY_WALLET_NAME, name)
            .putString(KEY_PRIV_KEY, keyPair.privateKeyHex)
            .putString(KEY_PUB_KEY, keyPair.publicKeyHex)
            .putString(KEY_NATIVE_ADDR, keyPair.nativeAddress)
            .putString(KEY_ETH_ADDR, keyPair.ethAddress)
            .putString(KEY_MNEMONIC, mnemonic ?: "")
            .putLong(KEY_CREATED_AT, System.currentTimeMillis())
            .apply()

        return StoredWallet(
            name = name,
            privateKeyHex = keyPair.privateKeyHex,
            publicKeyHex = keyPair.publicKeyHex,
            nativeAddress = keyPair.nativeAddress,
            ethAddress = keyPair.ethAddress,
            mnemonic = mnemonic
        )
    }

    fun getWallet(): StoredWallet? {
        if (!hasWallet()) return null
        val privKey = prefs.getString(KEY_PRIV_KEY, null) ?: return null
        val pubKey = prefs.getString(KEY_PUB_KEY, null) ?: ""
        val nativeAddr = prefs.getString(KEY_NATIVE_ADDR, null) ?: ""
        val ethAddr = prefs.getString(KEY_ETH_ADDR, null) ?: ""
        val name = prefs.getString(KEY_WALLET_NAME, "Betium Wallet") ?: "Betium Wallet"
        val mnemonic = prefs.getString(KEY_MNEMONIC, null)?.takeIf { it.isNotBlank() }
        val createdAt = prefs.getLong(KEY_CREATED_AT, 0L)

        return StoredWallet(
            name = name,
            privateKeyHex = privKey,
            publicKeyHex = pubKey,
            nativeAddress = nativeAddr,
            ethAddress = ethAddr,
            mnemonic = mnemonic,
            createdAt = createdAt
        )
    }

    fun getPrivateKey(): String? {
        return prefs.getString(KEY_PRIV_KEY, null)
    }

    fun getNativeAddress(): String? {
        return prefs.getString(KEY_NATIVE_ADDR, null)
    }

    fun getEthAddress(): String? {
        return prefs.getString(KEY_ETH_ADDR, null)
    }

    fun wipeWallet() {
        prefs.edit().clear().apply()
    }
}
