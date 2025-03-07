package com.vitorpamplona.quartz.events

import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.module.kotlin.readValue
import com.vitorpamplona.quartz.utils.TimeUtils
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.signers.NostrSigner

@Immutable
class AudioHeaderEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {

    fun download() = tags.firstOrNull { it.size > 1 && it[0] == DOWNLOAD_URL }?.get(1)
    fun stream() = tags.firstOrNull { it.size > 1 && it[0] == STREAM_URL }?.get(1)
    fun wavefrom() = tags.firstOrNull { it.size > 1 && it[0] == WAVEFORM }?.get(1)?.let {
        mapper.readValue<List<Int>>(it)
    }

    companion object {
        const val kind = 1808
        const val alt = "Audio header"

        private const val DOWNLOAD_URL = "download_url"
        private const val STREAM_URL = "stream_url"
        private const val WAVEFORM = "waveform"

        fun create(
            description: String,
            downloadUrl: String,
            streamUrl: String? = null,
            wavefront: String? = null,
            sensitiveContent: Boolean? = null,
            signer: NostrSigner,
            createdAt: Long = TimeUtils.now(),
            onReady: (AudioHeaderEvent) -> Unit
        ) {
            val tags = listOfNotNull(
                downloadUrl.let { arrayOf(DOWNLOAD_URL, it) },
                streamUrl?.let { arrayOf(STREAM_URL, it) },
                wavefront?.let { arrayOf(WAVEFORM, it) },
                sensitiveContent?.let {
                    if (it) {
                        arrayOf("content-warning", "")
                    } else {
                        null
                    }
                },
                arrayOf("alt", alt)
            ).toTypedArray()

            signer.sign(createdAt, kind, tags, description, onReady)
        }
    }
}
