package com.vitorpamplona.amethyst.ui.dal

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.ui.actions.updated
import com.vitorpamplona.quartz.events.ChatroomKey
import com.vitorpamplona.quartz.events.ChatroomKeyable
import com.vitorpamplona.quartz.events.PrivateDmEvent

class ChatroomListNewFeedFilter(val account: Account) : AdditiveFeedFilter<Note>() {

    override fun feedKey(): String {
        return account.userProfile().pubkeyHex
    }

    // returns the last Note of each user.
    override fun feed(): List<Note> {
        val me = account.userProfile()
        val followingKeySet = account.followingKeySet()

        val newChatrooms = me.privateChatrooms.filter {
            !it.value.senderIntersects(followingKeySet) && !me.hasSentMessagesTo(it.key) && !account.isAllHidden(it.key.users)
        }

        val privateMessages = newChatrooms.mapNotNull { it ->
            it.value
                .roomMessages
                .sortedWith(compareBy({ it.createdAt() }, { it.idHex }))
                .lastOrNull { it.event != null }
        }

        return privateMessages
            .sortedWith(compareBy({ it.createdAt() }, { it.idHex }))
            .reversed()
    }

    override fun updateListWith(oldList: List<Note>, newItems: Set<Note>): List<Note> {
        val me = account.userProfile()

        // Gets the latest message by room from the new items.
        val newRelevantPrivateMessages = filterRelevantPrivateMessages(newItems, account)

        if (newRelevantPrivateMessages.isEmpty()) {
            return oldList
        }

        var myNewList = oldList

        newRelevantPrivateMessages.forEach { newNotePair ->
            var hasUpdated = false
            oldList.forEach { oldNote ->
                val oldRoom = (oldNote.event as? ChatroomKeyable)?.chatroomKey(me.pubkeyHex)

                if (newNotePair.key == oldRoom) {
                    hasUpdated = true
                    if ((newNotePair.value.createdAt() ?: 0) > (oldNote.createdAt() ?: 0)) {
                        myNewList = myNewList.updated(oldNote, newNotePair.value)
                    }
                }
            }
            if (!hasUpdated) {
                myNewList = myNewList.plus(newNotePair.value)
            }
        }

        return sort(myNewList.toSet()).take(1000)
    }

    override fun applyFilter(newItems: Set<Note>): Set<Note> {
        // Gets the latest message by room from the new items.
        val newRelevantPrivateMessages = filterRelevantPrivateMessages(newItems, account)

        return if (newRelevantPrivateMessages.isEmpty()) {
            emptySet()
        } else {
            newRelevantPrivateMessages.values.toSet()
        }
    }

    private fun filterRelevantPrivateMessages(newItems: Set<Note>, account: Account): MutableMap<ChatroomKey, Note> {
        val me = account.userProfile()
        val followingKeySet = account.followingKeySet()

        val newRelevantPrivateMessages = mutableMapOf<ChatroomKey, Note>()
        newItems.filter { it.event is PrivateDmEvent }.forEach { newNote ->
            val roomKey = (newNote.event as? ChatroomKeyable)?.chatroomKey(me.pubkeyHex)
            val room = account.userProfile().privateChatrooms[roomKey]

            if (roomKey != null && room != null &&
                (newNote.author?.pubkeyHex != me.pubkeyHex && room.senderIntersects(followingKeySet) && !me.hasSentMessagesTo(roomKey)) &&
                !account.isAllHidden(roomKey.users)
            ) {
                val lastNote = newRelevantPrivateMessages.get(roomKey)
                if (lastNote != null) {
                    if ((newNote.createdAt() ?: 0) > (lastNote.createdAt() ?: 0)) {
                        newRelevantPrivateMessages.put(roomKey, newNote)
                    }
                } else {
                    newRelevantPrivateMessages.put(roomKey, newNote)
                }
            }
        }
        return newRelevantPrivateMessages
    }

    override fun sort(collection: Set<Note>): List<Note> {
        return collection
            .sortedWith(compareBy({ it.createdAt() }, { it.idHex }))
            .reversed()
    }
}
