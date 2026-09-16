package com.example.util

import java.net.URI
import java.util.regex.Pattern

data class ParsedReelData(
    val cleanUrl: String,
    val shortCode: String,
    val suggestedTitle: String,
    val extractedCaption: String,
    val isInstagramUrl: Boolean,
    val suggestedCategory: String
)

object InstagramUrlParser {

    private val URL_PATTERN = Pattern.compile(
        "(https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_+.~#?&/=]*))",
        Pattern.CASE_INSENSITIVE
    )

    private val REEL_PATTERN = Pattern.compile(
        "https?://(?:www\\.)?instagr(?:am\\.com|\\.am)/(?:reel|reels|p|share/reel)/([a-zA-Z0-9_-]+)",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(rawText: String): ParsedReelData {
        val trimmed = rawText.trim()
        val matcher = URL_PATTERN.matcher(trimmed)
        
        var foundUrl = ""
        if (matcher.find()) {
            foundUrl = matcher.group(1) ?: ""
        }

        val reelMatcher = REEL_PATTERN.matcher(foundUrl.ifEmpty { trimmed })
        val isInstagram = reelMatcher.find()
        val shortCode = if (isInstagram) reelMatcher.group(1) ?: "" else ""

        val cleanUrl = if (isInstagram && shortCode.isNotEmpty()) {
            "https://www.instagram.com/reel/$shortCode/"
        } else if (foundUrl.isNotEmpty()) {
            try {
                val uri = URI(foundUrl)
                // Strip noisy tracking parameters like igsh, utm_*
                val cleanQuery = uri.query?.split("&")
                    ?.filterNot { it.startsWith("igsh=") || it.startsWith("utm_") || it.startsWith("fbclid=") }
                    ?.joinToString("&")
                URI(uri.scheme, uri.authority, uri.path, cleanQuery.takeIf { !it.isNullOrEmpty() }, null).toString()
            } catch (e: Exception) {
                foundUrl
            }
        } else {
            trimmed
        }

        // Remaining text minus url can be caption or user note
        val textWithoutUrl = if (foundUrl.isNotEmpty()) {
            trimmed.replace(foundUrl, "").trim()
        } else {
            ""
        }

        val suggestedTitle = when {
            textWithoutUrl.isNotEmpty() -> {
                // First line or first 50 chars of caption
                val firstLine = textWithoutUrl.lines().firstOrNull()?.trim().orEmpty()
                if (firstLine.length > 50) firstLine.take(50) + "..." else firstLine
            }
            shortCode.isNotEmpty() -> "Instagram Reel ($shortCode)"
            else -> "Instagram Reel Note"
        }

        val suggestedCategory = guessCategory(trimmed + " " + textWithoutUrl)

        return ParsedReelData(
            cleanUrl = cleanUrl,
            shortCode = shortCode,
            suggestedTitle = suggestedTitle,
            extractedCaption = textWithoutUrl,
            isInstagramUrl = isInstagram,
            suggestedCategory = suggestedCategory
        )
    }

    private fun guessCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("recipe") || lower.contains("cook") || lower.contains("food") ||
                    lower.contains("bake") || lower.contains("eat") || lower.contains("dinner") ||
                    lower.contains("lunch") || lower.contains("breakfast") || lower.contains("delicious") -> "Recipe"

            lower.contains("workout") || lower.contains("gym") || lower.contains("fitness") ||
                    lower.contains("exercise") || lower.contains("muscle") || lower.contains("stretch") ||
                    lower.contains("yoga") || lower.contains("mobility") || lower.contains("cardio") -> "Fitness"

            lower.contains("travel") || lower.contains("trip") || lower.contains("visit") ||
                    lower.contains("hotel") || lower.contains("flight") || lower.contains("vacation") ||
                    lower.contains("japan") || lower.contains("city") || lower.contains("itinerary") -> "Travel"

            lower.contains("edit") || lower.contains("video") || lower.contains("art") ||
                    lower.contains("design") || lower.contains("creative") || lower.contains("photo") ||
                    lower.contains("camera") || lower.contains("transition") -> "Creative"

            lower.contains("buy") || lower.contains("product") || lower.contains("amazon") ||
                    lower.contains("deal") || lower.contains("gadget") || lower.contains("gift") ||
                    lower.contains("shop") -> "Shopping"

            lower.contains("hack") || lower.contains("tip") || lower.contains("tricks") ||
                    lower.contains("productivity") || lower.contains("lifehack") -> "Life Hack"

            lower.contains("tutorial") || lower.contains("how to") || lower.contains("learn") ||
                    lower.contains("course") || lower.contains("guide") || lower.contains("code") -> "Tutorial"

            else -> "Ideas"
        }
    }
}
