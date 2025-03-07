package com.vitorpamplona.quartz.events

import androidx.compose.runtime.Immutable
import com.vitorpamplona.quartz.utils.TimeUtils
import com.vitorpamplona.quartz.encoders.toHexKey
import com.vitorpamplona.quartz.crypto.CryptoUtils
import com.vitorpamplona.quartz.encoders.ATag
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.signers.NostrSigner

@Immutable
class RelaySetEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : BaseAddressableEvent(id, pubKey, createdAt, kind, tags, content, sig) {
    fun relays() = tags.filter { it.size > 1 && it[0] == "r" }.map { it[1] }
    fun description() = tags.firstOrNull() { it.size > 1 && it[0] == "description" }?.get(1)

    companion object {
        const val kind = 30022
        const val alt = "Relay list"

        fun create(
            relays: List<String>,
            signer: NostrSigner,
            createdAt: Long = TimeUtils.now(),
            onReady: (RelaySetEvent) -> Unit
        ) {
            val tags = mutableListOf<Array<String>>()
            relays.forEach {
                tags.add(arrayOf("r", it))
            }
            tags.add(arrayOf("alt", alt))

            signer.sign(createdAt, kind, tags.toTypedArray(), "", onReady)
        }
    }
}
