package com.vitorpamplona.quartz.crypto

import android.util.Log
import android.util.LruCache
import com.vitorpamplona.quartz.encoders.Hex
import fr.acinq.secp256k1.Secp256k1
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Nip04(val secp256k1: Secp256k1, val random: SecureRandom) {
    private val sharedKeyCache = SharedKeyCache()
    private val h02 = Hex.decode("02")

    fun clearCache() {
        sharedKeyCache.clearCache()
    }

    fun encrypt(msg: String, privateKey: ByteArray, pubKey: ByteArray): String {
        return encrypt(msg, getSharedSecret(privateKey, pubKey)).encodeToNIP04()
    }

    fun encrypt(msg: String, sharedSecret: ByteArray): EncryptedInfo {
        val iv = ByteArray(16)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(sharedSecret, "AES"), IvParameterSpec(iv))
        //val ivBase64 = Base64.getEncoder().encodeToString(iv)
        val encryptedMsg = cipher.doFinal(msg.toByteArray())
        //val encryptedMsgBase64 = Base64.getEncoder().encodeToString(encryptedMsg)
        return EncryptedInfo(encryptedMsg, iv)
    }

    fun decrypt(msg: String, privateKey: ByteArray, pubKey: ByteArray): String {
        val sharedSecret = getSharedSecret(privateKey, pubKey)
        return decrypt(msg, sharedSecret)
    }

    fun decrypt(encryptedInfo: EncryptedInfo, privateKey: ByteArray, pubKey: ByteArray): String {
        val sharedSecret = getSharedSecret(privateKey, pubKey)
        return decrypt(encryptedInfo.ciphertext, encryptedInfo.nonce, sharedSecret)
    }

    fun decrypt(msg: String, sharedSecret: ByteArray): String {
        val decoded = EncryptedInfo.decodeFromNIP04(msg)
        check(decoded != null) {
            "Unable to decode msg $msg as NIP04"
        }
        return decrypt(decoded.ciphertext, decoded.nonce, sharedSecret)
    }

    fun decrypt(cipher: String, nonce: String, sharedSecret: ByteArray): String {
        val iv = Base64.getDecoder().decode(nonce)
        val encryptedMsg = Base64.getDecoder().decode(cipher)
        return decrypt(encryptedMsg, iv, sharedSecret)
    }

    fun decrypt(encryptedMsg: ByteArray, iv: ByteArray, sharedSecret: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(sharedSecret, "AES"), IvParameterSpec(iv))
        return String(cipher.doFinal(encryptedMsg))
    }

    fun getSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray {
        val preComputed = sharedKeyCache.get(privateKey, pubKey)
        if (preComputed != null) return preComputed

        val computed = computeSharedSecret(privateKey, pubKey)
        sharedKeyCache.add(privateKey, pubKey, computed)
        return computed
    }

    /**
     * @return 32B shared secret
     */
    fun computeSharedSecret(privateKey: ByteArray, pubKey: ByteArray): ByteArray =
        secp256k1.pubKeyTweakMul(h02 + pubKey, privateKey).copyOfRange(1, 33)

    class EncryptedInfo(
        val ciphertext: ByteArray,
        val nonce: ByteArray
    ) {
        companion object {
            const val v: Int = 0

            fun decodePayload(payload: String): EncryptedInfo? {
                return try {
                    val byteArray = Base64.getDecoder().decode(payload)
                    check(byteArray[0].toInt() == Nip44v1.EncryptedInfo.v)
                    return EncryptedInfo(
                        nonce = byteArray.copyOfRange(1, 25),
                        ciphertext = byteArray.copyOfRange(25, byteArray.size)
                    )
                } catch (e: Exception) {
                    Log.w("NIP04", "Unable to Parse encrypted payload: ${payload}")
                    null
                }
            }

            fun decodeFromNIP04(payload: String): EncryptedInfo? {
                return try {
                    val parts = payload.split("?iv=")
                    EncryptedInfo(
                        ciphertext = Base64.getDecoder().decode(parts[0]),
                        nonce = Base64.getDecoder().decode(parts[1])
                    )
                } catch (e: Exception) {
                    Log.w("NIP04", "Unable to Parse encrypted payload: ${payload}")
                    null
                }
            }
        }

        fun encodePayload(): String {
            return Base64.getEncoder().encodeToString(
                byteArrayOf(v.toByte()) + nonce + ciphertext
            )
        }

        fun encodeToNIP04(): String {
            val nonce = Base64.getEncoder().encodeToString(nonce)
            val ciphertext = Base64.getEncoder().encodeToString(ciphertext)
            return "${ciphertext}?iv=${nonce}"
        }
    }
}