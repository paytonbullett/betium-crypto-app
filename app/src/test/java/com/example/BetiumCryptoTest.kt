package com.example

import com.example.data.crypto.Bip39Helper
import com.example.data.crypto.Secp256k1Helper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BetiumCryptoTest {

    @Test
    fun testGenerateKeyPair_createsValidSecp256k1KeysAndAddresses() {
        val keyPair = Secp256k1Helper.generateKeyPair()

        // 32-byte private key = 64 hex characters
        assertEquals(64, keyPair.privateKeyHex.length)
        // Compressed public key = 66 hex characters (33 bytes)
        assertEquals(66, keyPair.publicKeyHex.length)
        // Native address = SHA-256 of public key = 64 hex characters
        assertEquals(64, keyPair.nativeAddress.length)
        assertTrue(keyPair.nativeAddress.all { it in "0123456789abcdef" })

        // Ethereum address = 0x + 40 hex characters = 42 chars
        assertTrue(keyPair.ethAddress.startsWith("0x"))
        assertEquals(42, keyPair.ethAddress.length)
        assertTrue(keyPair.ethAddress.substring(2).all { it in "0123456789abcdef" })

        // Validate addresses with validator
        assertTrue(Secp256k1Helper.isValidAddress(keyPair.nativeAddress))
        assertTrue(Secp256k1Helper.isValidAddress(keyPair.ethAddress))
    }

    @Test
    fun testFromPrivateKeyHex_isDeterministic() {
        val samplePrivKey = "88b02c68889cd9bdff3ae6f4e47ff123886781d1a1dd76f0bb66302221948764"
        val keyPair1 = Secp256k1Helper.fromPrivateKeyHex(samplePrivKey)
        val keyPair2 = Secp256k1Helper.fromPrivateKeyHex(samplePrivKey)

        assertEquals(keyPair1.nativeAddress, keyPair2.nativeAddress)
        assertEquals(keyPair1.ethAddress, keyPair2.ethAddress)
        assertEquals(keyPair1.publicKeyHex, keyPair2.publicKeyHex)
    }

    @Test
    fun testBip39_generationAndValidation() {
        val mnemonic = Bip39Helper.generateMnemonic()
        assertEquals(12, mnemonic.size)
        assertTrue(Bip39Helper.validateMnemonic(mnemonic))

        val privKeyHex = Bip39Helper.mnemonicToPrivateKeyHex(mnemonic)
        assertEquals(64, privKeyHex.length)
        val keyPair = Secp256k1Helper.fromPrivateKeyHex(privKeyHex)
        assertNotNull(keyPair)
    }

    @Test
    fun testCanonicalTxPayload_buildsStrictSortedJson() {
        val payload = Secp256k1Helper.createCanonicalTxPayload(
            sender = "sender_address_hex",
            recipient = "recipient_address_hex",
            amount = "1.50000000",
            fee = "0.00010000",
            nonce = 8
        )

        // Strict alphabetical keys: amount, fee, nonce, recipient, sender
        val expected = """{"amount":"1.50000000","fee":"0.00010000","nonce":8,"recipient":"recipient_address_hex","sender":"sender_address_hex"}"""
        assertEquals(expected, payload)

        // Sign transaction
        val samplePrivKey = "88b02c68889cd9bdff3ae6f4e47ff123886781d1a1dd76f0bb66302221948764"
        val signature = Secp256k1Helper.signMessage(samplePrivKey, payload.toByteArray(Charsets.UTF_8))
        // 64-byte hex signature (r || s) = 128 hex chars
        assertEquals(128, signature.length)
    }
}
