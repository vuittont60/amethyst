package com.vitorpamplona.amethyst.service.relays

import android.util.Log
import com.vitorpamplona.amethyst.service.checkNotInMainThread
import com.vitorpamplona.quartz.events.Event
import com.vitorpamplona.quartz.events.EventInterface
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The Nostr Client manages multiple personae the user may switch between. Events are received and
 * published through multiple relays.
 * Events are stored with their respective persona.
 */
object Client : RelayPool.Listener {
    private var listeners = setOf<Listener>()
    private var relays = emptyArray<Relay>()
    private var subscriptions = mapOf<String, List<TypedFilter>>()

    @Synchronized
    fun reconnect(relays: Array<Relay>?, onlyIfChanged: Boolean = false) {
        Log.d("Relay", "Relay Pool Reconnecting to ${relays?.size} relays")
        checkNotInMainThread()

        if (onlyIfChanged) {
            if (!isSameRelaySetConfig(relays)) {
                if (this.relays.isNotEmpty()) {
                    RelayPool.disconnect()
                    RelayPool.unregister(this)
                    RelayPool.unloadRelays()
                }

                if (relays != null) {
                    RelayPool.register(this)
                    RelayPool.loadRelays(relays.toList())
                    RelayPool.requestAndWatch()
                    this.relays = relays
                }
            }
        } else {
            if (this.relays.isNotEmpty()) {
                RelayPool.disconnect()
                RelayPool.unregister(this)
                RelayPool.unloadRelays()
            }

            if (relays != null) {
                RelayPool.register(this)
                RelayPool.loadRelays(relays.toList())
                RelayPool.requestAndWatch()
                this.relays = relays
            }
        }
    }

    fun isSameRelaySetConfig(newRelayConfig: Array<Relay>?): Boolean {
        if (relays.size != newRelayConfig?.size) return false

        relays.forEach { oldRelayInfo ->
            val newRelayInfo = newRelayConfig.find { it.url == oldRelayInfo.url } ?: return false

            if (!oldRelayInfo.isSameRelayConfig(newRelayInfo)) return false
        }

        return true
    }

    fun sendFilter(
        subscriptionId: String = UUID.randomUUID().toString().substring(0..10),
        filters: List<TypedFilter> = listOf()
    ) {
        checkNotInMainThread()

        subscriptions = subscriptions + Pair(subscriptionId, filters)
        RelayPool.sendFilter(subscriptionId)
    }

    fun sendFilterOnlyIfDisconnected(
        subscriptionId: String = UUID.randomUUID().toString().substring(0..10),
        filters: List<TypedFilter> = listOf()
    ) {
        checkNotInMainThread()

        subscriptions = subscriptions + Pair(subscriptionId, filters)
        RelayPool.sendFilterOnlyIfDisconnected(subscriptionId)
    }

    fun send(
        signedEvent: EventInterface,
        relay: String? = null,
        feedTypes: Set<FeedType>? = null,
        relayList: List<Relay>? = null,
        onDone: (() -> Unit)? = null
    ) {
        checkNotInMainThread()

        if (relayList != null) {
            RelayPool.sendToSelectedRelays(relayList, signedEvent)
        } else if (relay == null) {
            RelayPool.send(signedEvent)
        } else {
            val useConnectedRelayIfPresent = RelayPool.getRelays(relay)

            if (useConnectedRelayIfPresent.isNotEmpty()) {
                useConnectedRelayIfPresent.forEach {
                    it.send(signedEvent)
                }
            } else {
                /** temporary connection */
                newSporadicRelay(
                    relay,
                    feedTypes,
                    onConnected = { relay ->
                        relay.send(signedEvent)
                    },
                    onDone = onDone
                )
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun newSporadicRelay(url: String, feedTypes: Set<FeedType>?, onConnected: (Relay) -> Unit, onDone: (() -> Unit)?) {
        val relay = Relay(url, true, true, feedTypes ?: emptySet())
        RelayPool.addRelay(relay)

        relay.connectAndRun {
            allSubscriptions().forEach {
                relay.sendFilter(requestId = it)
            }

            onConnected(relay)

            GlobalScope.launch(Dispatchers.IO) {
                delay(60000) // waits for a reply
                relay.disconnect()
                RelayPool.removeRelay(relay)

                if (onDone != null) {
                    onDone()
                }
            }
        }
    }

    fun close(subscriptionId: String) {
        RelayPool.close(subscriptionId)
        subscriptions = subscriptions.minus(subscriptionId)
    }

    fun isActive(subscriptionId: String): Boolean {
        return subscriptions.contains(subscriptionId)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onEvent(event: Event, subscriptionId: String, relay: Relay, afterEOSE: Boolean) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onEvent(event, subscriptionId, relay, afterEOSE) }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onError(error: Error, subscriptionId: String, relay: Relay) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onError(error, subscriptionId, relay) }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onRelayStateChange(type: Relay.StateType, relay: Relay, channel: String?) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onRelayStateChange(type, relay, channel) }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onSendResponse(eventId: String, success: Boolean, message: String, relay: Relay) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onSendResponse(eventId, success, message, relay) }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onAuth(relay: Relay, challenge: String) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onAuth(relay, challenge) }
        }
    }

    override fun onNotify(relay: Relay, description: String) {
        // Releases the Web thread for the new payload.
        // May need to add a processing queue if processing new events become too costly.
        GlobalScope.launch(Dispatchers.Default) {
            listeners.forEach { it.onNotify(relay, description) }
        }
    }

    fun subscribe(listener: Listener) {
        listeners = listeners.plus(listener)
    }

    fun isSubscribed(listener: Listener): Boolean {
        return listeners.contains(listener)
    }

    fun unsubscribe(listener: Listener) {
        listeners = listeners.minus(listener)
    }

    fun allSubscriptions(): Set<String> {
        return subscriptions.keys
    }

    fun getSubscriptionFilters(subId: String): List<TypedFilter> {
        return subscriptions[subId] ?: emptyList()
    }

    abstract class Listener {
        /**
         * A new message was received
         */
        open fun onEvent(event: Event, subscriptionId: String, relay: Relay, afterEOSE: Boolean) = Unit

        /**
         * A new or repeat message was received
         */
        open fun onError(error: Error, subscriptionId: String, relay: Relay) = Unit

        /**
         * Connected to or disconnected from a relay
         */
        open fun onRelayStateChange(type: Relay.StateType, relay: Relay, channel: String?) = Unit

        /**
         * When an relay saves or rejects a new event.
         */
        open fun onSendResponse(eventId: String, success: Boolean, message: String, relay: Relay) = Unit

        open fun onAuth(relay: Relay, challenge: String) = Unit

        open fun onNotify(relay: Relay, description: String) = Unit
    }
}
