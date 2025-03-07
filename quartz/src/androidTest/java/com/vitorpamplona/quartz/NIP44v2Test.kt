package com.vitorpamplona.quartz;

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.vitorpamplona.quartz.crypto.Nip44v2
import com.vitorpamplona.quartz.encoders.hexToByteArray
import com.vitorpamplona.quartz.encoders.toHexKey
import fr.acinq.secp256k1.Secp256k1
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.security.SecureRandom


@RunWith(AndroidJUnit4::class)
public class NIP44v2Test {
    val vectors: VectorFile = jacksonObjectMapper().readValue(
        getInstrumentation().context.assets.open("nip44.vectors.json"),
        VectorFile::class.java
    )

    val random = SecureRandom()
    val nip44v2 = Nip44v2(Secp256k1.get(), random)

    @Test
    fun conversationKeyTest() {
        for (v in vectors.v2?.valid?.getConversationKey!!) {
            val conversationKey = nip44v2.getConversationKey(v.sec1!!.hexToByteArray(), v.pub2!!.hexToByteArray())

            assertEquals(v.conversationKey, conversationKey.toHexKey())
        }
    }

    @Test
    fun paddingTest() {
        for (v in vectors.v2?.valid?.calcPaddedLen!!) {
            val actual = nip44v2.calcPaddedLen(v[0])
            assertEquals(v[1], actual)
        }
    }

    @Test
    fun encryptDecryptTest() {
        for (v in vectors.v2?.valid?.encryptDecrypt!!) {
            val pub2 = com.vitorpamplona.quartz.crypto.KeyPair(v.sec2!!.hexToByteArray())
            val conversationKey = nip44v2.getConversationKey(v.sec1!!.hexToByteArray(), pub2.pubKey)
            assertEquals(v.conversationKey, conversationKey.toHexKey())

            val ciphertext = nip44v2.encryptWithNonce(
                v.plaintext!!,
                conversationKey,
                v.nonce!!.hexToByteArray()
            ).encodePayload()

            assertEquals(v.payload, ciphertext)

            val decrypted = nip44v2.decrypt(v.payload!!, conversationKey)
            assertEquals(v.plaintext, decrypted)
        }
    }

    @Test
    fun encryptDecryptLongTest() {
        for (v in vectors.v2?.valid?.encryptDecryptLongMsg!!) {
            val conversationKey = v.conversationKey!!.hexToByteArray()
            val plaintext = v.pattern!!.repeat(v.repeat!!)

            assertEquals(v.plaintextSha256, sha256Hex(plaintext.toByteArray(Charsets.UTF_8)))

            val ciphertext = nip44v2.encryptWithNonce(
                plaintext,
                conversationKey,
                v.nonce!!.hexToByteArray()
            ).encodePayload()

            assertEquals(v.payloadSha256, sha256Hex(ciphertext.toByteArray(Charsets.UTF_8)))

            val decrypted = nip44v2.decrypt(ciphertext, conversationKey)

            assertEquals(plaintext, decrypted)
        }
    }

    @Test
    fun invalidMessageLenghts() {
        for (v in vectors.v2?.invalid?.encryptMsgLengths!!) {
            val key = ByteArray(32)
            random.nextBytes(key)
            try {
                nip44v2.encrypt("a".repeat(v), key)
                fail("Should Throw for ${v}")
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    fun invalidDecrypt() {
        for (v in vectors.v2?.invalid?.decrypt!!) {
            try {
                val result = nip44v2.decrypt(v.payload!!, v.conversationKey!!.hexToByteArray())
                assertNull(result)
                //fail("Should Throw for ${v.note}")
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    fun invalidConversationKey() {
        for (v in vectors.v2?.invalid?.getConversationKey!!) {
            try {
                nip44v2.getConversationKey(v.sec1!!.hexToByteArray(), v.pub2!!.hexToByteArray())
                fail("Should Throw for ${v.note}")
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    fun sha256Hex(data: ByteArray): String {
        // Creates a new buffer every time
        return MessageDigest.getInstance("SHA-256").digest(data).toHexKey()
    }
}

data class VectorFile (
    val v2 : V2? = V2()
)

data class V2 (
    val valid   : Valid?   = Valid(),
    val invalid : Invalid? = Invalid()
)

data class Valid (
    @JsonProperty("get_conversation_key"     ) val getConversationKey    : ArrayList<GetConversationKey>    = arrayListOf(),
    @JsonProperty("get_message_keys"         ) val getMessageKeys        : GetMessageKeys?                  = GetMessageKeys(),
    @JsonProperty("calc_padded_len"          ) val calcPaddedLen         : ArrayList<ArrayList<Int>>        = arrayListOf(),
    @JsonProperty("encrypt_decrypt"          ) val encryptDecrypt        : ArrayList<EncryptDecrypt>        = arrayListOf(),
    @JsonProperty("encrypt_decrypt_long_msg" ) val encryptDecryptLongMsg : ArrayList<EncryptDecryptLongMsg> = arrayListOf()
)

data class Invalid (
    @JsonProperty("encrypt_msg_lengths"  ) val encryptMsgLengths  : ArrayList<Int>                = arrayListOf(),
    @JsonProperty("get_conversation_key" ) val getConversationKey : ArrayList<GetConversationKey> = arrayListOf(),
    @JsonProperty("decrypt"              ) val decrypt            : ArrayList<Decrypt>            = arrayListOf()
)

data class GetConversationKey (
    val sec1 : String? = null,
    val pub2 : String? = null,
    val note : String? = null,
    @JsonProperty("conversation_key" ) val conversationKey : String? = null
)

data class GetMessageKeys (
    @JsonProperty("conversation_key" ) val conversationKey : String?         = null,
    val keys : ArrayList<Keys> = arrayListOf()
)

data class Keys (
    @JsonProperty("nonce"        ) val nonce       : String? = null,
    @JsonProperty("chacha_key"   ) val chachaKey   : String? = null,
    @JsonProperty("chacha_nonce" ) val chachaNonce : String? = null,
    @JsonProperty("hmac_key"     ) val hmacKey     : String? = null
)

data class EncryptDecrypt (
    val sec1            : String? = null,
    val sec2            : String? = null,
    @JsonProperty("conversation_key" ) val conversationKey : String? = null,
    val nonce           : String? = null,
    val plaintext       : String? = null,
    val payload         : String? = null
)

data class EncryptDecryptLongMsg (
    @JsonProperty("conversation_key" ) val conversationKey : String? = null,
    val nonce           : String? = null,
    val pattern         : String? = null,
    val repeat          : Int?    = null,
    @JsonProperty("plaintext_sha256" ) val plaintextSha256 : String? = null,
    @JsonProperty("payload_sha256"   ) val payloadSha256   : String? = null
)

data class Decrypt (
    @JsonProperty("conversation_key" ) val conversationKey : String? = null,
    val nonce           : String? = null,
    val plaintext       : String? = null,
    val payload         : String? = null,
    val note            : String? = null
)

