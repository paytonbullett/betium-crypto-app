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
        // Public key body = 128 hex characters (64 bytes: X || Y)
        assertEquals(128, keyPair.publicKeyHex.length)
        // Native address = SHA-256 of 64-byte uncompressed public key body = 64 hex characters
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

        // Strict alphabetical keys with integer units: amount, fee, nonce, recipient, sender
        val expected = """{"amount":150000000,"fee":10000,"nonce":8,"recipient":"recipient_address_hex","sender":"sender_address_hex"}"""
        assertEquals(expected, payload)

        // Sign transaction
        val samplePrivKey = "88b02c68889cd9bdff3ae6f4e47ff123886781d1a1dd76f0bb66302221948764"
        val signature = Secp256k1Helper.signMessage(samplePrivKey, payload.toByteArray(Charsets.UTF_8))
        // DER signature is typically 140-142 hex chars (70-71 bytes)
        assertTrue(signature.length >= 136)
    }

    @Test
    fun testLiveSignatureFormats() {
        val keyPair = Secp256k1Helper.generateKeyPair()
        val payload = """{"amount":100000000,"fee":10000,"nonce":1,"recipient":"${keyPair.nativeAddress}","sender":"${keyPair.nativeAddress}"}"""
        val payloadBytes = payload.toByteArray(Charsets.UTF_8)
        
        // A. Sign SHA-256
        val sigSha256 = signRaw(keyPair.privateKeyHex, Secp256k1Helper.sha256(payloadBytes))
        println("Probe A (Sign SHA256):")
        testSend(keyPair, payload, sigSha256, 100000000, 10000, 1)

        // B. Sign SHA-1
        val mdSha1 = java.security.MessageDigest.getInstance("SHA-1")
        val sigSha1 = signRaw(keyPair.privateKeyHex, mdSha1.digest(payloadBytes))
        println("Probe B (Sign SHA1):")
        testSend(keyPair, payload, sigSha1, 100000000, 10000, 1)

        // C. Sign Raw Payload bytes (truncated/padded to curve order if needed)
        val sigRawBytes = signRaw(keyPair.privateKeyHex, payloadBytes)
        println("Probe C (Sign raw bytes):")
        testSend(keyPair, payload, sigRawBytes, 100000000, 10000, 1)

        // D. Sign Double SHA-256
        val sigDblSha256 = signRaw(keyPair.privateKeyHex, Secp256k1Helper.sha256(Secp256k1Helper.sha256(payloadBytes)))
        println("Probe D (Sign Double SHA256):")
        testSend(keyPair, payload, sigDblSha256, 100000000, 10000, 1)
    }

    private fun signRaw(privKeyHex: String, hash: ByteArray): String {
        val privKeyInt = java.math.BigInteger(privKeyHex, 16)
        val privParams = org.bouncycastle.crypto.params.ECPrivateKeyParameters(privKeyInt, Secp256k1Helper.domainParameters)
        val signer = org.bouncycastle.crypto.signers.ECDSASigner(org.bouncycastle.crypto.signers.HMacDSAKCalculator(org.bouncycastle.crypto.digests.SHA256Digest()))
        signer.init(true, privParams)
        val components = signer.generateSignature(hash)
        var r = components[0]
        var s = components[1]
        val halfCurveOrder = Secp256k1Helper.domainParameters.n.shiftRight(1)
        if (s > halfCurveOrder) {
            s = Secp256k1Helper.domainParameters.n.subtract(s)
        }
        return (Secp256k1Helper.to32ByteArray(r) + Secp256k1Helper.to32ByteArray(s))
            .joinToString("") { "%02x".format(it) }
    }

    private fun testSend(keyPair: com.example.data.crypto.KeyPairInfo, payload: String, sig: String, amount: Long, fee: Long, nonce: Long) {
        try {
            val jsonBody = """{"sender":"${keyPair.nativeAddress}","recipient":"${keyPair.nativeAddress}","amount":$amount,"fee":$fee,"nonce":$nonce,"public_key":"${keyPair.publicKeyHex}","signature":"$sig"}"""
            val url = java.net.URL("https://crypto.bullettsocial.space/transaction/new")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.doOutput = true
            conn.outputStream.use { it.write(jsonBody.toByteArray()) }
            val code = conn.responseCode
            val resp = try { conn.inputStream.bufferedReader().readText() } catch (e: Exception) { conn.errorStream?.bufferedReader()?.readText() }
            println("Result for sig len ${sig.length / 2} bytes: HTTP $code -> $resp")
        } catch (e: Exception) {
            println("Exception: ${e.message}")
        }
    }
}
