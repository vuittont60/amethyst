package com.vitorpamplona.quartz.events

import androidx.compose.runtime.Immutable
import com.vitorpamplona.quartz.utils.*
import com.vitorpamplona.quartz.encoders.Bech32
import com.vitorpamplona.quartz.crypto.CryptoUtils
import com.vitorpamplona.quartz.crypto.KeyPair
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.encoders.hexToByteArray
import com.vitorpamplona.quartz.encoders.toHexKey
import com.vitorpamplona.quartz.signers.NostrSigner
import com.vitorpamplona.quartz.signers.NostrSignerInternal
import java.nio.charset.Charset
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Immutable
class LnZapRequestEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {

    @Transient
    private var privateZapEvent: LnZapPrivateEvent? = null

    fun zappedPost() = tags.filter { it.size > 1 && it[0] == "e" }.map { it[1] }

    fun zappedAuthor() = tags.filter { it.size > 1 && it[0] == "p" }.map { it[1] }

    fun isPrivateZap() = tags.any { t -> t.size >= 2 && t[0] == "anon" && t[1].isNotBlank() }

    fun getPrivateZapEvent(loggedInUserPrivKey: ByteArray, pubKey: HexKey): LnZapPrivateEvent? {
        val anonTag = tags.firstOrNull { t -> t.size >= 2 && t[0] == "anon" }
        if (anonTag != null) {
            val encnote = anonTag[1]
            if (encnote.isNotBlank()) {
                try {
                    val note = decryptPrivateZapMessage(encnote, loggedInUserPrivKey, pubKey.hexToByteArray())
                    val decryptedEvent = fromJson(note)
                    if (decryptedEvent.kind == 9733) {
                        return decryptedEvent as LnZapPrivateEvent
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    fun cachedPrivateZap(): LnZapPrivateEvent? {
        return privateZapEvent
    }

    fun decryptPrivateZap(signer: NostrSigner, onReady: (Event) -> Unit) {
        privateZapEvent?.let {
            onReady(it)
            return
        }

        signer.decryptZapEvent(this) {
            // caches it
            privateZapEvent = it
            onReady(it)
        }
    }

    companion object {
        const val kind = 9734
        const val alt = "Zap request"

        fun create(
            originalNote: EventInterface,
            relays: Set<String>,
            signer: NostrSigner,
            pollOption: Int?,
            message: String,
            zapType: LnZapEvent.ZapType,
            toUserPubHex: String?, // Overrides in case of Zap Splits
            createdAt: Long = TimeUtils.now(),
            onReady: (LnZapRequestEvent) -> Unit
        ) {
            if (zapType == LnZapEvent.ZapType.NONZAP) return

            var tags = listOf(
                arrayOf("e", originalNote.id()),
                arrayOf("p", toUserPubHex ?: originalNote.pubKey()),
                arrayOf("relays") + relays,
                arrayOf("alt", alt)
            )
            if (originalNote is AddressableEvent) {
                tags = tags + listOf(arrayOf("a", originalNote.address().toTag()))
            }
            if (pollOption != null && pollOption >= 0) {
                tags = tags + listOf(arrayOf(POLL_OPTION, pollOption.toString()))
            }

            if (zapType == LnZapEvent.ZapType.ANONYMOUS) {
                tags = tags + listOf(arrayOf("anon"))
                NostrSignerInternal(KeyPair()).sign(createdAt, kind, tags.toTypedArray(), message, onReady)
            } else if (zapType == LnZapEvent.ZapType.PRIVATE) {
                tags = tags + listOf(arrayOf("anon", ""))
                signer.sign(createdAt, kind, tags.toTypedArray(), message, onReady)
            } else {
                signer.sign(createdAt, kind, tags.toTypedArray(), message, onReady)
            }
        }

        fun create(
            userHex: String,
            relays: Set<String>,
            signer: NostrSigner,
            message: String,
            zapType: LnZapEvent.ZapType,
            createdAt: Long = TimeUtils.now(),
            onReady: (LnZapRequestEvent) -> Unit
        ) {
            if (zapType == LnZapEvent.ZapType.NONZAP) return

            var tags = arrayOf(
                arrayOf("p", userHex),
                arrayOf("relays") + relays
            )

            if (zapType == LnZapEvent.ZapType.ANONYMOUS) {
                tags += arrayOf(arrayOf("anon", ""))
                NostrSignerInternal(KeyPair()).sign(createdAt, kind, tags, message, onReady)
            } else if (zapType == LnZapEvent.ZapType.PRIVATE) {
                tags += arrayOf(arrayOf("anon", ""))
                signer.sign(createdAt, kind, tags, message, onReady)
            } else {
                signer.sign(createdAt, kind, tags, message, onReady)
            }
        }


        fun createEncryptionPrivateKey(privkey: String, id: String, createdAt: Long): ByteArray {
            val str = privkey + id + createdAt.toString()
            val strbyte = str.toByteArray(Charset.forName("utf-8"))
            return CryptoUtils.sha256(strbyte)
        }

        fun encryptPrivateZapMessage(msg: String, privkey: ByteArray, pubkey: ByteArray): String {
            val sharedSecret = CryptoUtils.getSharedSecretNIP04(privkey, pubkey)
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)

            val keySpec = SecretKeySpec(sharedSecret, "AES")
            val ivSpec = IvParameterSpec(iv)

            val utf8message = msg.toByteArray(Charset.forName("utf-8"))
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedMsg = cipher.doFinal(utf8message)

            val encryptedMsgBech32 = Bech32.encode("pzap", Bech32.eight2five(encryptedMsg), Bech32.Encoding.Bech32)
            val ivBech32 = Bech32.encode("iv", Bech32.eight2five(iv), Bech32.Encoding.Bech32)

            return encryptedMsgBech32 + "_" + ivBech32
        }

        private fun decryptPrivateZapMessage(msg: String, privkey: ByteArray, pubkey: ByteArray): String {
            val sharedSecret = CryptoUtils.getSharedSecretNIP04(privkey, pubkey)
            if (sharedSecret.size != 16 && sharedSecret.size != 32) {
                throw IllegalArgumentException("Invalid shared secret size")
            }
            val parts = msg.split("_")
            if (parts.size != 2) {
                throw IllegalArgumentException("Invalid message format")
            }
            val iv = parts[1].run { Bech32.decode(this).second }
            val encryptedMsg = parts.first().run { Bech32.decode(this).second }
            val encryptedBytes = Bech32.five2eight(encryptedMsg, 0)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(
                Cipher.DECRYPT_MODE, SecretKeySpec(sharedSecret, "AES"), IvParameterSpec(
                    Bech32.five2eight(iv, 0))
            )

            try {
                val decryptedMsgBytes = cipher.doFinal(encryptedBytes)
                return String(decryptedMsgBytes)
            } catch (ex: BadPaddingException) {
                throw IllegalArgumentException("Bad padding: ${ex.message}")
            }
        }
    }
}
