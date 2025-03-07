package com.vitorpamplona.amethyst.service.previews

import com.vitorpamplona.amethyst.service.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private const val ELEMENT_TAG_META = "meta"
private const val ATTRIBUTE_VALUE_PROPERTY = "property"
private const val ATTRIBUTE_VALUE_NAME = "name"
private const val ATTRIBUTE_VALUE_ITEMPROP = "itemprop"

/*for <meta itemprop=... to get title */
private val META_X_TITLE = arrayOf(
    "og:title", "\"og:title\"", "'og:title'", "name", "\"name\"", "'name'",
    "twitter:title",
    "\"twitter:title\"",
    "'twitter:title'",
    "title",
    "\"title\"",
    "'title'"
)

/*for <meta itemprop=... to get description */
private val META_X_DESCRIPTION = arrayOf(
    "og:description", "\"og:description\"", "'og:description'",
    "description", "\"description\"", "'description'",
    "twitter:description",
    "\"twitter:description\"",
    "'twitter:description'",
    "description",
    "\"description\"",
    "'description'"
)

/*for <meta itemprop=... to get image */
private val META_X_IMAGE = arrayOf(
    "og:image", "\"og:image\"", "'og:image'",
    "image", "\"image\"", "'image'",
    "twitter:image",
    "\"twitter:image\"",
    "'twitter:image'"
)

private const val CONTENT = "content"

suspend fun getDocument(url: String, timeOut: Int = 30000): UrlInfoItem =
    withContext(Dispatchers.IO) {
        val request: Request = Request.Builder().url(url).get().build()
        HttpClient.getHttpClient().newCall(request).execute().use {
            if (it.isSuccessful) {
                val mimeType = it.headers.get("Content-Type")?.toMediaType()
                    ?: throw IllegalArgumentException("Website returned unknown mimetype: ${it.headers.get("Content-Type")}")

                if (mimeType.type == "text" && mimeType.subtype == "html") {
                    val document = Jsoup.parse(it.body.string())
                    parseHtml(url, document, mimeType)
                } else if (mimeType.type == "image") {
                    UrlInfoItem(url, image = url, mimeType = mimeType)
                } else if (mimeType.type == "video") {
                    UrlInfoItem(url, image = url, mimeType = mimeType)
                } else {
                    throw IllegalArgumentException("Website returned unknown encoding for previews: $mimeType")
                }
            } else {
                throw IllegalArgumentException("Website returned: " + it.code)
            }
        }
    }

suspend fun parseHtml(url: String, document: Document, type: MediaType): UrlInfoItem =
    withContext(Dispatchers.IO) {
        val metaTags = document.getElementsByTag(ELEMENT_TAG_META)

        var title: String = ""
        var description: String = ""
        var image: String = ""

        metaTags.forEach {
            when (it.attr(ATTRIBUTE_VALUE_PROPERTY)) {
                in META_X_TITLE -> if (title.isEmpty()) {
                    title = it.attr(CONTENT)
                }
                in META_X_DESCRIPTION -> if (description.isEmpty()) {
                    description = it.attr(CONTENT)
                }
                in META_X_IMAGE -> if (image.isEmpty()) {
                    image = it.attr(CONTENT)
                }
            }

            when (it.attr(ATTRIBUTE_VALUE_NAME)) {
                in META_X_TITLE -> if (title.isEmpty()) {
                    title = it.attr(CONTENT)
                }
                in META_X_DESCRIPTION -> if (description.isEmpty()) {
                    description = it.attr(CONTENT)
                }
                in META_X_IMAGE -> if (image.isEmpty()) {
                    image = it.attr(CONTENT)
                }
            }

            when (it.attr(ATTRIBUTE_VALUE_ITEMPROP)) {
                in META_X_TITLE -> if (title.isEmpty()) {
                    title = it.attr(CONTENT)
                }
                in META_X_DESCRIPTION -> if (description.isEmpty()) {
                    description = it.attr(CONTENT)
                }
                in META_X_IMAGE -> if (image.isEmpty()) {
                    image = it.attr(CONTENT)
                }
            }

            if (title.isNotEmpty() && description.isNotEmpty() && image.isNotEmpty()) {
                return@withContext UrlInfoItem(url, title, description, image, type)
            }
        }
        return@withContext UrlInfoItem(url, title, description, image, type)
    }
