package com.vitorpamplona.quartz.events

import android.util.Log
import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.module.kotlin.readValue
import com.vitorpamplona.quartz.encoders.hexToByteArray
import com.vitorpamplona.quartz.crypto.CryptoUtils
import com.vitorpamplona.quartz.encoders.ATag
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.signers.NostrSigner
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

@Immutable
abstract class GeneralListEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    kind: Int,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : BaseAddressableEvent(id, pubKey, createdAt, kind, tags, content, sig) {
    @Transient
    private var privateTagsCache: Array<Array<String>>? = null

    fun category() = dTag()
    fun bookmarkedPosts() = taggedEvents()
    fun bookmarkedPeople() = taggedUsers()

    fun name() = tags.firstOrNull { it.size > 1 && it[0] == "name" }?.get(1)
    fun title() = tags.firstOrNull { it.size > 1 && it[0] == "title" }?.get(1)
    fun nameOrTitle() = name() ?: title()

    fun cachedPrivateTags(): Array<Array<String>>? {
        return privateTagsCache
    }

    fun filterTagList(key: String, privateTags: Array<Array<String>>?): ImmutableSet<String> {
        val privateUserList = privateTags?.let {
            it.filter { it.size > 1 && it[0] == key }.map { it[1] }.toSet()
        } ?: emptySet()
        val publicUserList = tags.filter { it.size > 1 && it[0] == key }.map { it[1] }.toSet()

        return (privateUserList + publicUserList).toImmutableSet()
    }

    fun isTagged(key: String, tag: String, isPrivate: Boolean, signer: NostrSigner, onReady: (Boolean) -> Unit) {
        return if (isPrivate) {
            privateTagsOrEmpty(signer = signer) {
                onReady(
                    it.any { it.size > 1 && it[0] == key && it[1] == tag }
                )
            }
        } else {
            onReady(isTagged(key, tag))
        }
    }

    fun privateTags(signer: NostrSigner, onReady: (Array<Array<String>>) -> Unit) {
        if (content.isEmpty()) {
            onReady(emptyArray())
            return
        }

        privateTagsCache?.let {
            onReady(it)
            return
        }

        try {
            signer.nip04Decrypt(content, pubKey) {
                privateTagsCache = mapper.readValue<Array<Array<String>>>(it)
                privateTagsCache?.let {
                    onReady(it)
                }
            }
        } catch (e: Throwable) {
            Log.w("GeneralList", "Error parsing the JSON ${e.message}")
        }
    }

    fun privateTagsOrEmpty(signer: NostrSigner, onReady: (Array<Array<String>>) -> Unit) {
        privateTags(signer, onReady)
    }

    fun privateTaggedUsers(signer: NostrSigner, onReady: (List<String>) -> Unit) = privateTags(signer) {
        onReady(filterUsers(it))
    }
    fun privateHashtags(signer: NostrSigner, onReady: (List<String>) -> Unit) = privateTags(signer) {
        onReady(filterHashtags(it))
    }
    fun privateGeohashes(signer: NostrSigner, onReady: (List<String>) -> Unit) = privateTags(signer) {
        onReady(filterGeohashes(it))
    }
    fun privateTaggedEvents(signer: NostrSigner, onReady: (List<String>) -> Unit) = privateTags(signer) {
        onReady(filterEvents(it))
    }
    fun privateTaggedAddresses(signer: NostrSigner, onReady: (List<ATag>) -> Unit) = privateTags(signer) {
        onReady(filterAddresses(it))
    }

    fun filterUsers(tags: Array<Array<String>>): List<String> {
        return tags.filter { it.size > 1 && it[0] == "p" }.map { it[1] }
    }

    fun filterHashtags(tags: Array<Array<String>>): List<String> {
        return tags.filter { it.size > 1 && it[0] == "t" }.map { it[1] }
    }

    fun filterGeohashes(tags: Array<Array<String>>): List<String> {
        return tags.filter { it.size > 1 && it[0] == "g" }.map { it[1] }
    }

    fun filterEvents(tags: Array<Array<String>>): List<String> {
        return tags.filter { it.size > 1 && it[0] == "e" }.map { it[1] }
    }

    fun filterAddresses(tags: Array<Array<String>>): List<ATag> {
        return tags.filter { it.firstOrNull() == "a" }.mapNotNull {
            val aTagValue = it.getOrNull(1)
            val relay = it.getOrNull(2)

            if (aTagValue != null) ATag.parse(aTagValue, relay) else null
        }
    }

    companion object {
        fun createPrivateTags(
            privEvents: List<String>? = null,
            privUsers: List<String>? = null,
            privAddresses: List<ATag>? = null,

            signer: NostrSigner,
            onReady: (String) -> Unit
        ) {
            val privTags = mutableListOf<Array<String>>()
            privEvents?.forEach {
                privTags.add(arrayOf("e", it))
            }
            privUsers?.forEach {
                privTags.add(arrayOf("p", it))
            }
            privAddresses?.forEach {
                privTags.add(arrayOf("a", it.toTag()))
            }

            return encryptTags(privTags.toTypedArray(), signer, onReady)
        }

        fun encryptTags(
            privateTags: Array<Array<String>>? = null,
            signer: NostrSigner,
            onReady: (String) -> Unit
        ) {
            val msg = mapper.writeValueAsString(privateTags)

            signer.nip04Encrypt(
                msg,
                signer.pubKey,
                onReady
            )
        }
    }
}
