package com.vitorpamplona.quartz.events

import androidx.compose.runtime.Immutable
import com.vitorpamplona.quartz.utils.TimeUtils
import com.vitorpamplona.quartz.encoders.toHexKey
import com.vitorpamplona.quartz.crypto.CryptoUtils
import com.vitorpamplona.quartz.crypto.KeyPair
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.signers.NostrSigner

@Immutable
class DeletionEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {
    fun deleteEvents() = tags.map { it[1] }

    companion object {
        const val kind = 5
        const val alt = "Deletion event"

        fun create(deleteEvents: List<String>, signer: NostrSigner, createdAt: Long = TimeUtils.now(), onReady: (DeletionEvent) -> Unit) {
            val content = ""
            val tags = deleteEvents.map { arrayOf("e", it) }.plusElement(arrayOf("alt", alt)).toTypedArray()
            signer.sign(createdAt, kind, tags, content, onReady)
        }
    }
}
