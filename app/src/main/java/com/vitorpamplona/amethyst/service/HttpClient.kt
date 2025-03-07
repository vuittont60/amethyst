package com.vitorpamplona.amethyst.service

import android.util.Log
import com.vitorpamplona.amethyst.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Duration
import kotlin.properties.Delegates

object HttpClient {
    val DEFAULT_TIMEOUT_ON_WIFI = Duration.ofSeconds(10L)
    val DEFAULT_TIMEOUT_ON_MOBILE = Duration.ofSeconds(30L)

    var proxyChangeListeners = ArrayList<() -> Unit>()
    var defaultTimeout = DEFAULT_TIMEOUT_ON_WIFI

    var defaultHttpClient: OkHttpClient? = null

    // fires off every time value of the property changes
    private var internalProxy: Proxy? by Delegates.observable(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            proxyChangeListeners.forEach {
                it()
            }
        }
    }

    fun start(proxy: Proxy?) {
        if (internalProxy != proxy) {
            this.internalProxy = proxy
            this.defaultHttpClient = getHttpClient()
        }
    }

    fun changeTimeouts(timeout: Duration) {
        Log.d("HttpClient", "Changing timeout to: $timeout")
        if (this.defaultTimeout.seconds != timeout.seconds) {
            this.defaultTimeout = timeout
            this.defaultHttpClient = getHttpClient()
        }
    }

    fun getHttpClient(timeout: Duration): OkHttpClient {
        val seconds = if (internalProxy != null) timeout.seconds * 2 else timeout.seconds
        val duration = Duration.ofSeconds(seconds)
        return OkHttpClient.Builder()
            .proxy(internalProxy)
            .readTimeout(duration)
            .connectTimeout(duration)
            .writeTimeout(duration)
            .addInterceptor(DefaultContentTypeInterceptor())
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    class DefaultContentTypeInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest: Request = chain.request()
            val requestWithUserAgent: Request = originalRequest
                .newBuilder()
                .header("User-Agent", "Amethyst/${BuildConfig.VERSION_NAME}")
                .build()
            return chain.proceed(requestWithUserAgent)
        }
    }

    fun getHttpClientForRelays(): OkHttpClient {
        if (this.defaultHttpClient == null) {
            this.defaultHttpClient = getHttpClient(defaultTimeout)
        }
        return defaultHttpClient!!
    }

    fun getHttpClient(): OkHttpClient {
        if (this.defaultHttpClient == null) {
            this.defaultHttpClient = getHttpClient(defaultTimeout)
        }
        return defaultHttpClient!!
    }

    fun getProxy(): Proxy? {
        return internalProxy
    }

    fun initProxy(useProxy: Boolean, hostname: String, port: Int): Proxy? {
        return if (useProxy) Proxy(Proxy.Type.SOCKS, InetSocketAddress(hostname, port)) else null
    }
}
