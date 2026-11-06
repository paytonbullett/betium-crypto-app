package com.example.data.crypto

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.digests.KeccakDigest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

data class KeyPairInfo(
    val privateKeyHex: String,
    val publicKeyHex: String,
    val uncompressedPublicKeyHex: String,
    val nativeAddress: String,
    val ethAddress: String
)

object Secp256k1Helper {
    private val ecParams = SECNamedCurves.getByName("secp256k1")
    val domainParameters = ECDomainParameters(ecParams.curve, ecParams.g, ecParams.n, ecParams.h)
    private val secureRandom = SecureRandom()

    /**
     * Generates a new random 32-byte private key and derives all Betium keys & addresses.
     */
    fun generateKeyPair(): KeyPairInfo {
        val privateKeyBytes = ByteArray(32)
        var privateKeyInt: BigInteger
        do {
            secureRandom.nextBytes(privateKeyBytes)
            privateKeyInt = BigInteger(1, privateKeyBytes)
        } while (privateKeyInt <= BigInteger.ONE || privateKeyInt >= domainParameters.n)

        val privateKeyHex = privateKeyBytes.toHex()
        return fromPrivateKeyHex(privateKeyHex)
    }

    /**
     * Derives Betium keys and addresses from an existing 64-character (32-byte) hex private key.
     */
    fun fromPrivateKeyHex(rawHex: String): KeyPairInfo {
        val cleanHex = rawHex.trim().removePrefix("0x").lowercase()
        require(cleanHex.length == 64) { "Private key must be a 64-character hex string (32 bytes)" }

        val privKeyInt = BigInteger(cleanHex, 16)
        require(privKeyInt > BigInteger.ONE && privKeyInt < domainParameters.n) {
            "Private key out of valid secp256k1 curve range"
        }

        // Q = d * G
        val publicPoint: ECPoint = domainParameters.g.multiply(privKeyInt).normalize()

        // Compressed: 33 bytes (02/03 prefix + 32-byte X)
        val compressedBytes = publicPoint.getEncoded(true)
        val compressedHex = compressedBytes.toHex()

        // Uncompressed: 65 bytes (04 prefix + 32-byte X + 32-byte Y)
        val uncompressedBytes = publicPoint.getEncoded(false)
        val uncompressedHex = uncompressedBytes.toHex()
        val keyBody = uncompressedBytes.copyOfRange(1, uncompressedBytes.size) // 64 bytes (X || Y)

        // 1. Native address: SHA-256 of 64-byte uncompressed public key body (X || Y) -> 64 hex characters (lowercase, no 0x)
        val nativeAddress = sha256Hex(keyBody)

        // 2. Ethereum address: Keccak-256 of 64 bytes of uncompressed public key (omitting the 0x04 prefix)
        // Take the last 20 bytes -> 0x + 40 lowercase hex characters
        val ethAddress = deriveEthAddress(uncompressedBytes)

        return KeyPairInfo(
            privateKeyHex = cleanHex,
            publicKeyHex = keyBody.toHex(),
            uncompressedPublicKeyHex = uncompressedHex,
            nativeAddress = nativeAddress,
            ethAddress = ethAddress
        )
    }

    /**
     * Derives Ethereum-style 0x... address from 65-byte uncompressed public key bytes.
     */
    fun deriveEthAddress(uncompressedPubBytes: ByteArray): String {
        require(uncompressedPubBytes.isNotEmpty() && uncompressedPubBytes[0] == 0x04.toByte()) {
            "Expected 65-byte uncompressed public key starting with 0x04"
        }
        val keyBody = uncompressedPubBytes.copyOfRange(1, uncompressedPubBytes.size) // 64 bytes: X || Y
        val keccak = keccak256(keyBody)
        // Last 20 bytes of 32-byte keccak hash
        val addressBytes = keccak.copyOfRange(12, 32)
        return "0x" + addressBytes.toHex()
    }

    // Replaced by DER SHA-1 signMessage below

    /**
     * Deterministic RFC 6979 ECDSA signature encoded in ASN.1 DER format over SHA-1 message hash.
     * Matches Python ecdsa default behavior for Betium transaction signing.
     */
    fun signMessage(privateKeyHex: String, messageBytes: ByteArray): String {
        val cleanHex = privateKeyHex.trim().removePrefix("0x").lowercase()
        val privKeyInt = BigInteger(cleanHex, 16)
        val privParams = ECPrivateKeyParameters(privKeyInt, domainParameters)

        // SHA-1 message hash (matching python ecdsa default for secp256k1 signing)
        val mdSha1 = MessageDigest.getInstance("SHA-1")
        val hash = mdSha1.digest(messageBytes)

        // RFC 6979 deterministic k calculator with SHA-1
        val signer = ECDSASigner(HMacDSAKCalculator(org.bouncycastle.crypto.digests.SHA1Digest()))
        signer.init(true, privParams)
        val components = signer.generateSignature(hash)
        var r = components[0]
        var s = components[1]

        // Low-S malleability check (BIP 62)
        val halfCurveOrder = domainParameters.n.shiftRight(1)
        if (s > halfCurveOrder) {
            s = domainParameters.n.subtract(s)
        }

        // Strict ASN.1 DER SEQUENCE of two INTEGERs
        val derSeq = org.bouncycastle.asn1.DERSequence(
            arrayOf(
                org.bouncycastle.asn1.ASN1Integer(r),
                org.bouncycastle.asn1.ASN1Integer(s)
            )
        )
        return derSeq.encoded.toHex()
    }

    /**
     * Creates the canonical Betium transaction payload string for signing.
     * Amount and fee are converted to integer units (multiplied by 10^8).
     * Keys are sorted alphabetically with numeric amount/fee/nonce (no quotes).
     * Format: {"amount":150000000,"fee":10000,"nonce":8,"recipient":"...","sender":"..."}
     */
    fun createCanonicalTxPayload(
        sender: String,
        recipient: String,
        amount: String,
        fee: String,
        nonce: Long,
        tokenId: String? = null
    ): String {
        val amountBd = amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
        val feeBd = fee.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
        val amountUnits = amountBd.multiply(java.math.BigDecimal("100000000")).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
        val feeUnits = feeBd.multiply(java.math.BigDecimal("100000000")).setScale(0, java.math.RoundingMode.HALF_UP).toLong()

        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"amount\":").append(amountUnits).append(",")
        sb.append("\"fee\":").append(feeUnits).append(",")
        sb.append("\"nonce\":").append(nonce).append(",")
        sb.append("\"recipient\":\"").append(recipient).append("\",")
        sb.append("\"sender\":\"").append(sender).append("\"")
        if (!tokenId.isNullOrBlank()) {
            sb.append(",\"token_id\":\"").append(tokenId).append("\"")
        }
        sb.append("}")
        return sb.toString()
    }

    /**
     * Validates whether a given string is a valid Betium native address (64 hex characters)
     * or a valid Ethereum-style address (0x followed by 40 hex characters).
     */
    fun isValidAddress(address: String): Boolean {
        val clean = address.trim()
        if (clean.length == 64 && clean.all { it in "0123456789abcdefABCDEF" }) {
            return true
        }
        if (clean.startsWith("0x", ignoreCase = true) && clean.length == 42 &&
            clean.substring(2).all { it in "0123456789abcdefABCDEF" }
        ) {
            return true
        }
        return false
    }

    fun sha256(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data)
    }

    fun sha256Hex(data: ByteArray): String {
        return sha256(data).toHex()
    }

    fun keccak256(data: ByteArray): ByteArray {
        val digest = KeccakDigest(256)
        digest.update(data, 0, data.size)
        val output = ByteArray(32)
        digest.doFinal(output, 0)
        return output
    }

    fun to32ByteArray(b: BigInteger): ByteArray {
        val raw = b.toByteArray()
        val result = ByteArray(32)
        if (raw.size > 32) {
            System.arraycopy(raw, raw.size - 32, result, 0, 32)
        } else {
            System.arraycopy(raw, 0, result, 32 - raw.size, raw.size)
        }
        return result
    }

    fun ByteArray.toHex(): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(size * 2)
        for (b in this) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i ushr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }
}
