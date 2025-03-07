package com.vitorpamplona.quartz.events

import androidx.compose.runtime.Immutable
import com.linkedin.urls.detection.UrlDetector
import com.linkedin.urls.detection.UrlDetectorOptions
import com.vitorpamplona.quartz.utils.TimeUtils
import com.vitorpamplona.quartz.encoders.ATag
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.signers.NostrSigner

@Immutable
class TextNoteEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : BaseTextNoteEvent(id, pubKey, createdAt, kind, tags, content, sig) {

    fun root() = tags.firstOrNull() { it.size > 3 && it[3] == "root" }?.get(1)

    companion object {
        const val kind = 1

        fun create(
            msg: String,
            replyTos: List<String>?,
            mentions: List<String>?,
            addresses: List<ATag>?,
            extraTags: List<String>?,
            zapReceiver: List<ZapSplitSetup>? = null,
            markAsSensitive: Boolean,
            zapRaiserAmount: Long?,
            replyingTo: String?,
            root: String?,
            directMentions: Set<HexKey>,
            geohash: String? = null,
            nip94attachments: List<Event>? = null,
            signer: NostrSigner,
            createdAt: Long = TimeUtils.now(),
            onReady: (TextNoteEvent) -> Unit
        ) {
            val tags = mutableListOf<Array<String>>()
            replyTos?.let {
                tags.addAll(
                    it.positionalMarkedTags(
                        tagName = "e",
                        root = root,
                        replyingTo = replyingTo,
                        directMentions = directMentions
                    )
                )
            }
            mentions?.forEach {
                if (it in directMentions) {
                    tags.add(arrayOf("p", it, "", "mention"))
                } else {
                    tags.add(arrayOf("p", it))
                }
            }
            addresses?.map { it.toTag() }?.let {
                tags.addAll(
                    it.positionalMarkedTags(
                        tagName = "a",
                        root = root,
                        replyingTo = replyingTo,
                        directMentions = directMentions
                    )
                )
            }
            findHashtags(msg).forEach {
                tags.add(arrayOf("t", it))
                tags.add(arrayOf("t", it.lowercase()))
            }
            extraTags?.forEach {
                tags.add(arrayOf("t", it))
            }
            zapReceiver?.forEach {
                tags.add(arrayOf("zap", it.lnAddressOrPubKeyHex, it.relay ?: "", it.weight.toString()))
            }
            findURLs(msg).forEach {
                tags.add(arrayOf("r", it))
            }
            if (markAsSensitive) {
                tags.add(arrayOf("content-warning", ""))
            }
            zapRaiserAmount?.let {
                tags.add(arrayOf("zapraiser", "$it"))
            }
            geohash?.let {
                tags.addAll(geohashMipMap(it))
            }
            nip94attachments?.let {
                it.forEach {
                    //tags.add(arrayOf("nip94", it.toJson()))
                }
            }

            signer.sign(createdAt, kind, tags.toTypedArray(), msg, onReady)
        }

        /**
         * Returns a list of NIP-10 marked tags that are also ordered at best effort
         * to support the deprecated method of positional tags to maximize backwards compatibility
         * with clients that support replies but have not been updated to understand tag markers.
         *
         * https://github.com/nostr-protocol/nips/blob/master/10.md
         *
         * The tag to the root of the reply chain goes first.
         * The tag to the reply event being responded to goes last.
         * The order for any other tag does not matter, so keep the relative order.
         */
        private fun List<String>.positionalMarkedTags(
            tagName: String,
            root: String?,
            replyingTo: String?,
            directMentions: Set<HexKey>
        ) =
            sortedWith { o1, o2 ->
                when {
                    o1 == o2 -> 0
                    o1 == root -> -1 // root goes first
                    o2 == root -> 1 // root goes first
                    o1 == replyingTo -> 1 // reply event being responded to goes last
                    o2 == replyingTo -> -1 // reply event being responded to goes last
                    else -> 0 // keep the relative order for any other tag
                }
            }.map {
                when (it) {
                    root -> arrayOf(tagName, it, "", "root")
                    replyingTo -> arrayOf(tagName, it, "", "reply")
                    in directMentions -> arrayOf(tagName, it, "", "mention")
                    else -> arrayOf(tagName, it)
                }
            }
    }
}

fun findURLs(text: String): List<String> {
    return UrlDetector(text, UrlDetectorOptions.Default).detect().map { it.originalUrl }
}
