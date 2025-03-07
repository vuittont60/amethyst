package com.vitorpamplona.quartz.events

import androidx.compose.runtime.Immutable
import com.vitorpamplona.quartz.utils.TimeUtils
import com.vitorpamplona.quartz.encoders.toHexKey
import com.vitorpamplona.quartz.crypto.CryptoUtils
import com.vitorpamplona.quartz.encoders.ATag
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.signers.NostrSigner

@Immutable
class PinListEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : BaseAddressableEvent(id, pubKey, createdAt, kind, tags, content, sig) {

    fun pins() = tags.filter { it.size > 1 && it[0] == "pin" }.map { it[1] }

    companion object {
        const val kind = 33888
        const val alt = "Pinned Posts"

        fun create(
            pins: List<String>,
            signer: NostrSigner,
            createdAt: Long = TimeUtils.now(),
            onReady: (PinListEvent) -> Unit
        ) {
            val tags = mutableListOf<Array<String>>()
            pins.forEach {
                tags.add(arrayOf("pin", it))
            }
            tags.add(arrayOf("alt", alt))

            signer.sign(createdAt, kind, tags.toTypedArray(), "", onReady)
        }
    }
}
