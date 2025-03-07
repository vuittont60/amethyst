package com.vitorpamplona.amethyst.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vitorpamplona.quartz.crypto.CryptoUtils
import com.vitorpamplona.quartz.encoders.toHexKey
import com.vitorpamplona.quartz.crypto.KeyPair
import com.vitorpamplona.quartz.events.ChatMessageEvent
import com.vitorpamplona.quartz.events.Event
import com.vitorpamplona.quartz.events.GiftWrapEvent
import com.vitorpamplona.quartz.events.Gossip
import com.vitorpamplona.quartz.events.SealedGossipEvent
import com.vitorpamplona.quartz.signers.NostrSignerInternal
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class GiftWrapSigningBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun createMessageEvent() {
        val sender = NostrSignerInternal(KeyPair())
        val receiver = NostrSignerInternal(KeyPair())

        benchmarkRule.measureRepeated {
            val countDownLatch = CountDownLatch(1)

            ChatMessageEvent.create(
                msg = "Hi there! This is a test message",
                to = listOf(receiver.pubKey),
                subject = "Party Tonight",
                replyTos = emptyList(),
                mentions = emptyList(),
                zapReceiver = null,
                markAsSensitive = true,
                zapRaiserAmount = 10000,
                geohash = null,
                signer = sender
            ) {
                countDownLatch.countDown()
            }

            assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))
        }
    }

    @Test
    fun sealMessage() {
        val sender = NostrSignerInternal(KeyPair())
        val receiver = NostrSignerInternal(KeyPair())

        val countDownLatch = CountDownLatch(1)

        var msg: ChatMessageEvent? = null

        ChatMessageEvent.create(
            msg = "Hi there! This is a test message",
            to = listOf(receiver.pubKey),
            subject = "Party Tonight",
            replyTos = emptyList(),
            mentions = emptyList(),
            zapReceiver = null,
            markAsSensitive = true,
            zapRaiserAmount = 10000,
            geohash = null,
            signer = sender
        ) {
            msg = it
            countDownLatch.countDown()
        }

        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))

        benchmarkRule.measureRepeated {
            val countDownLatch2 = CountDownLatch(1)
            SealedGossipEvent.create(
                event = msg!!,
                encryptTo = receiver.pubKey,
                signer = sender
            ) {
                countDownLatch2.countDown()
            }

            assertTrue(countDownLatch2.await(1, TimeUnit.SECONDS))
        }
    }

    @Test
    fun wrapSeal() {
        val sender = NostrSignerInternal(KeyPair())
        val receiver = NostrSignerInternal(KeyPair())

        val countDownLatch = CountDownLatch(1)

        var seal: SealedGossipEvent? = null

        ChatMessageEvent.create(
            msg = "Hi there! This is a test message",
            to = listOf(receiver.pubKey),
            subject = "Party Tonight",
            replyTos = emptyList(),
            mentions = emptyList(),
            zapReceiver = null,
            markAsSensitive = true,
            zapRaiserAmount = 10000,
            geohash = null,
            signer = sender
        ) {
            SealedGossipEvent.create(
                event = it,
                encryptTo = receiver.pubKey,
                signer = sender
            ) {
                seal = it
                countDownLatch.countDown()
            }
        }

        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))

        benchmarkRule.measureRepeated {
            val countDownLatch2 = CountDownLatch(1)
            GiftWrapEvent.create(
                event = seal!!,
                recipientPubKey = receiver.pubKey
            ) {
                countDownLatch2.countDown()
            }
            assertTrue(countDownLatch2.await(1, TimeUnit.SECONDS))
        }
    }

    @Test
    fun wrapToString() {
        val sender = NostrSignerInternal(KeyPair())
        val receiver = NostrSignerInternal(KeyPair())

        val countDownLatch = CountDownLatch(1)

        var wrap: GiftWrapEvent? = null

        ChatMessageEvent.create(
            msg = "Hi there! This is a test message",
            to = listOf(receiver.pubKey),
            subject = "Party Tonight",
            replyTos = emptyList(),
            mentions = emptyList(),
            zapReceiver = null,
            markAsSensitive = true,
            zapRaiserAmount = 10000,
            geohash = null,
            signer = sender
        ) {
            SealedGossipEvent.create(
                event = it,
                encryptTo = receiver.pubKey,
                signer = sender
            ) {
                GiftWrapEvent.create(
                    event = it,
                    recipientPubKey = receiver.pubKey
                ) {
                    wrap = it
                    countDownLatch.countDown()
                }
            }
        }

        assertTrue(countDownLatch.await(1, TimeUnit.SECONDS))

        benchmarkRule.measureRepeated {
            wrap!!.toJson()
        }
    }
}