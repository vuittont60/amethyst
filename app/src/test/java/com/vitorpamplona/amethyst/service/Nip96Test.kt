package com.vitorpamplona.amethyst.service

import junit.framework.TestCase.assertEquals
import org.junit.Test

class Nip96Test {

    val json = """
        {
          "api_url": "https://nostr.build/api/v2/nip96/upload",
          "download_url": "https://media.nostr.build",
          "supported_nips": [
            94,
            96,
            98
          ],
          "tos_url": "https://nostr.build/tos/",
          "content_types": [
            "image/*",
            "video/*",
            "audio/*"
          ],
          "plans": {
            "free": {
              "name": "Free",
              "is_nip98_required": true,
              "url": "https://nostr.build",
              "max_byte_size": 26214400,
              "file_expiration": [
                0,
                0
              ],
              "media_transformations": {
                "image": [
                  "resizing",
                  "format_conversion",
                  "compression",
                  "metadata_stripping"
                ],
                "video": [
                  "resizing",
                  "format_conversion",
                  "compression"
                ]
              }
            },
            "professional": {
              "name": "Professional",
              "is_nip98_required": true,
              "url": "https://nostr.build/signup/new/",
              "max_byte_size": 10737418240,
              "file_expiration": [
                0,
                0
              ],
              "media_transformations": {
                "image": [
                  "resizing",
                  "format_conversion",
                  "compression",
                  "metadata_stripping"
                ],
                "video": [
                  "resizing",
                  "format_conversion",
                  "compression"
                ]
              }
            },
            "creator": {
              "name": "Creator",
              "is_nip98_required": true,
              "url": "https://nostr.build/signup/new/",
              "max_byte_size": 26843545600,
              "file_expiration": [
                0,
                0
              ],
              "media_transformations": {
                "image": [
                  "resizing",
                  "format_conversion",
                  "compression",
                  "metadata_stripping"
                ],
                "video": [
                  "resizing",
                  "format_conversion",
                  "compression"
                ]
              }
            }
          }
        }
    """.trimIndent()

    @Test()
    fun parseNostrBuild() {
        val info = Nip96Retriever().parse(json)

        assertEquals("https://nostr.build/api/v2/nip96/upload", info.apiUrl)
        assertEquals("https://media.nostr.build", info.downloadUrl)
        assertEquals(listOf(94, 96, 98), info.supportedNips)
        assertEquals("https://nostr.build/tos/", info.tosUrl)
        assertEquals(listOf("image/*", "video/*", "audio/*"), info.contentTypes)

        assertEquals(listOf("creator", "free", "professional"), info.plans.keys.sorted())

        assertEquals("Free", info.plans["free"]?.name)
        assertEquals(true, info.plans["free"]?.isNip98Required)
        assertEquals("https://nostr.build", info.plans["free"]?.url)
        assertEquals(26214400L, info.plans["free"]?.maxByteSize)
        assertEquals(listOf(0, 0), info.plans["free"]?.fileExpiration)
        assertEquals(listOf("image", "video"), info.plans["free"]?.mediaTransformations?.keys?.sorted())

        assertEquals(26843545600L, info.plans["creator"]?.maxByteSize)
        assertEquals(10737418240L, info.plans["professional"]?.maxByteSize)
    }
}
