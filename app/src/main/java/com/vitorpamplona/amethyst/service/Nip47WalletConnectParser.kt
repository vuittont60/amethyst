package com.vitorpamplona.amethyst.ui.note

import android.net.Uri
import com.vitorpamplona.amethyst.model.Nip47URI
import com.vitorpamplona.quartz.encoders.decodePublicKey
import com.vitorpamplona.quartz.encoders.toHexKey

// Rename to the corect nip number when ready.
object Nip47WalletConnectParser {
    fun parse(uri: String): Nip47URI {
        // nostrwalletconnect://b889ff5b1513b641e2a139f661a661364979c5beee91842f8f0ef42ab558e9d4?relay=wss%3A%2F%2Frelay.damus.io&metadata=%7B%22name%22%3A%22Example%22%7D

        val url = Uri.parse(uri)

        if (url.scheme != "nostrwalletconnect" && url.scheme != "nostr+walletconnect") {
            throw IllegalArgumentException("Not a Wallet Connect QR Code")
        }

        val pubkey = url.host ?: throw IllegalArgumentException("Hostname cannot be null")

        val pubkeyHex = try {
            decodePublicKey(pubkey).toHexKey()
        } catch (e: Exception) {
            throw IllegalArgumentException("Hostname is not a valid Nostr Pubkey")
        }

        val relay = url.getQueryParameter("relay")
        val secret = url.getQueryParameter("secret")

        return Nip47URI(pubkeyHex, relay, secret)
    }
}
