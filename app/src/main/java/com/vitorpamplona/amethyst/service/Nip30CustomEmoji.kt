package com.vitorpamplona.amethyst.service

import androidx.compose.runtime.Immutable
import java.util.regex.Pattern

@Immutable
class Nip30CustomEmoji {
    val customEmojiPattern: Pattern = Pattern.compile("\\:([A-Za-z0-9_\\-]+)\\:", Pattern.CASE_INSENSITIVE)

    fun buildArray(input: String): List<String> {
        val matcher = customEmojiPattern.matcher(input)
        val list = mutableListOf<String>()
        while (matcher.find()) {
            list.add(matcher.group())
        }

        if (list.isEmpty()) {
            return listOf(input)
        }

        val regularChars = input.split(customEmojiPattern.toRegex())

        val finalList = mutableListOf<String>()
        var index = 0
        for (e in regularChars) {
            finalList.add(e)
            if (index < list.size) {
                finalList.add(list[index])
            }
            index++
        }
        return finalList
    }
}
