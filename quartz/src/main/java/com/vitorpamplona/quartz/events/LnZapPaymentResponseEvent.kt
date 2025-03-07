package com.vitorpamplona.quartz.events

import android.util.Log
import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.vitorpamplona.quartz.crypto.CryptoUtils
import com.vitorpamplona.quartz.encoders.HexKey
import com.vitorpamplona.quartz.signers.NostrSigner

@Immutable
class LnZapPaymentResponseEvent(
    id: HexKey,
    pubKey: HexKey,
    createdAt: Long,
    tags: Array<Array<String>>,
    content: String,
    sig: HexKey
) : Event(id, pubKey, createdAt, kind, tags, content, sig) {
    // Once one of an app user decrypts the payment, all users else can see it.
    @Transient
    private var response: Response? = null

    fun requestAuthor() = tags.firstOrNull() { it.size > 1 && it[0] == "p" }?.get(1)
    fun requestId() = tags.firstOrNull() { it.size > 1 && it[0] == "e" }?.get(1)

    fun talkingWith(oneSideHex: String): HexKey {
        return if (pubKey == oneSideHex) requestAuthor() ?: pubKey else pubKey
    }

    private fun plainContent(signer: NostrSigner, onReady: (String) -> Unit) {
        try {
            signer.nip04Decrypt(content, talkingWith(signer.pubKey)) { content ->
                onReady(content)
            }
        } catch (e: Exception) {
            Log.w("PrivateDM", "Error decrypting the message ${e.message}")
        }
    }

    fun response(signer: NostrSigner, onReady: (Response) -> Unit) {
        response?.let {
            onReady(it)
            return
        }

        try {
            if (content.isNotEmpty()) {
                plainContent(signer) {
                    mapper.readValue(it, Response::class.java)?.let {
                        response = it
                        onReady(it)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("LnZapPaymentResponseEvent", "Can't parse content as a payment response: $content", e)
        }
    }

    companion object {
        const val kind = 23195
        const val alt = "Zap payment response"
    }
}

// RESPONSE OBJECTS
abstract class Response(
    @JsonProperty("result_type")
    val resultType: String
)

// PayInvoice Call

class PayInvoiceSuccessResponse(val result: PayInvoiceResultParams? = null) :
    Response("pay_invoice") {
    class PayInvoiceResultParams(val preimage: String)
}

class PayInvoiceErrorResponse(val error: PayInvoiceErrorParams? = null) :
    Response("pay_invoice") {
    class PayInvoiceErrorParams(val code: ErrorType?, val message: String?)

    enum class ErrorType {
        @JsonProperty(value = "RATE_LIMITED")
        RATE_LIMITED, // The client is sending commands too fast. It should retry in a few seconds.
        @JsonProperty(value = "NOT_IMPLEMENTED")
        NOT_IMPLEMENTED, // The command is not known or is intentionally not implemented.
        @JsonProperty(value = "INSUFFICIENT_BALANCE")
        INSUFFICIENT_BALANCE, // The wallet does not have enough funds to cover a fee reserve or the payment amount.
        @JsonProperty(value = "QUOTA_EXCEEDED")
        QUOTA_EXCEEDED, // The wallet has exceeded its spending quota.
        @JsonProperty(value = "RESTRICTED")
        RESTRICTED, // This public key is not allowed to do this operation.
        @JsonProperty(value = "UNAUTHORIZED")
        UNAUTHORIZED, // This public key has no wallet connected.
        @JsonProperty(value = "INTERNAL")
        INTERNAL, // An internal error.
        @JsonProperty(value = "OTHER")
        OTHER // Other error.
    }
}


class ResponseDeserializer : StdDeserializer<Response>(Response::class.java) {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Response? {
        val jsonObject: JsonNode = jp.codec.readTree(jp)
        val resultType = jsonObject.get("result_type")?.asText()

        if (resultType == "pay_invoice") {
            val result = jsonObject.get("result")
            val error = jsonObject.get("error")
            if (result != null) {
                return jp.codec.treeToValue(jsonObject, PayInvoiceSuccessResponse::class.java)
            }
            if (error != null) {
                return jp.codec.treeToValue(jsonObject, PayInvoiceErrorResponse::class.java)
            }
        } else {
            // tries to guess
            if (jsonObject.get("result")?.get("preimage") != null) {
                return jp.codec.treeToValue(jsonObject, PayInvoiceSuccessResponse::class.java)
            }
            if (jsonObject.get("error")?.get("code") != null) {
                return jp.codec.treeToValue(jsonObject, PayInvoiceErrorResponse::class.java)
            }
        }
        return null
    }
}