package com.vitorpamplona.quartz.events

import androidx.compose.runtime.Immutable
import com.vitorpamplona.quartz.utils.TimeUtils
import com.vitorpamplona.quartz.encoders.toHexKey
import com.vitorpamplona.quartz.crypto.CryptoUtils
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.signers.NostrSigner

@Immutable
class FileStorageHeaderEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {

    fun dataEventId() = tags.firstOrNull { it.size > 1 && it[0] == "e" }?.get(1)

    fun encryptionKey() = tags.firstOrNull { it.size > 2 && it[0] == ENCRYPTION_KEY }?.let { AESGCM(it[1], it[2]) }
    fun mimeType() = tags.firstOrNull { it.size > 1 && it[0] == MIME_TYPE }?.get(1)
    fun hash() = tags.firstOrNull { it.size > 1 && it[0] == HASH }?.get(1)
    fun size() = tags.firstOrNull { it.size > 1 && it[0] == FILE_SIZE }?.get(1)
    fun alt() = tags.firstOrNull { it.size > 1 && it[0] == ALT }?.get(1)
    fun dimensions() = tags.firstOrNull { it.size > 1 && it[0] == DIMENSION }?.get(1)
    fun magnetURI() = tags.firstOrNull { it.size > 1 && it[0] == MAGNET_URI }?.get(1)
    fun torrentInfoHash() = tags.firstOrNull { it.size > 1 && it[0] == TORRENT_INFOHASH }?.get(1)
    fun blurhash() = tags.firstOrNull { it.size > 1 && it[0] == BLUR_HASH }?.get(1)

    companion object {
        const val kind = 1065
        const val altDescription = "Descriptors for a binary file"

        private const val ENCRYPTION_KEY = "aes-256-gcm"
        private const val MIME_TYPE = "m"
        private const val FILE_SIZE = "size"
        private const val DIMENSION = "dim"
        private const val HASH = "x"
        private const val MAGNET_URI = "magnet"
        private const val TORRENT_INFOHASH = "i"
        private const val BLUR_HASH = "blurhash"
        private const val ALT = "alt"

        fun create(
            storageEvent: FileStorageEvent,
            mimeType: String? = null,
            alt: String? = null,
            hash: String? = null,
            size: String? = null,
            dimensions: String? = null,
            blurhash: String? = null,
            magnetURI: String? = null,
            torrentInfoHash: String? = null,
            encryptionKey: AESGCM? = null,
            sensitiveContent: Boolean? = null,
            signer: NostrSigner,
            createdAt: Long = TimeUtils.now(),
            onReady: (FileStorageHeaderEvent) -> Unit
        ) {
            val tags = listOfNotNull(
                arrayOf("e", storageEvent.id),
                mimeType?.let { arrayOf(MIME_TYPE, mimeType) },
                hash?.let { arrayOf(HASH, it) },
                alt?.let { arrayOf(ALT, it) } ?: arrayOf("alt", altDescription),
                size?.let { arrayOf(FILE_SIZE, it) },
                dimensions?.let { arrayOf(DIMENSION, it) },
                blurhash?.let { arrayOf(BLUR_HASH, it) },
                magnetURI?.let { arrayOf(MAGNET_URI, it) },
                torrentInfoHash?.let { arrayOf(TORRENT_INFOHASH, it) },
                encryptionKey?.let { arrayOf(ENCRYPTION_KEY, it.key, it.nonce) },
                sensitiveContent?.let {
                    if (it) {
                        arrayOf("content-warning", "")
                    } else {
                        null
                    }
                }
            )

            val content = alt ?: ""
            signer.sign(createdAt, kind, tags.toTypedArray(), content, onReady)
        }
    }
}
