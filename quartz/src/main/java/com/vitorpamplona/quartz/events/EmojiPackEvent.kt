package com.vitorpamplona.quartz.events

import androidx.compose.runtime.Immutable
import com.vitorpamplona.quartz.utils.TimeUtils
import com.vitorpamplona.quartz.encoders.toHexKey
import com.vitorpamplona.quartz.crypto.CryptoUtils
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.signers.NostrSigner

@Immutable
class EmojiPackEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : GeneralListEvent(id, pubKey, createdAt, kind, tags, content, sig) {

    companion object {
        const val kind = 30030
        const val alt = "Emoji pack"

        fun create(
            name: String = "",
            signer: NostrSigner,
            createdAt: Long = TimeUtils.now(),
            onReady: (EmojiPackEvent) -> Unit
        ) {
            val content = ""

            val tags = mutableListOf<Array<String>>()
            tags.add(arrayOf("d", name))
            tags.add(arrayOf("alt", alt))

            signer.sign(createdAt, kind, tags.toTypedArray(), content, onReady)
        }
    }
}

@Immutable
data class EmojiUrl(val code: String, val url: String) {
    fun encode(): String {
        return ":$code:$url"
    }

    companion object {
        fun decode(encodedEmojiSetup: String): EmojiUrl? {
            val emojiParts = encodedEmojiSetup.split(":", limit = 3)
            return if (emojiParts.size > 2) {
                EmojiUrl(emojiParts[1], emojiParts[2])
            } else {
                null
            }
        }
    }
}
