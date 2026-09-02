package com.saicord.provider

import com.lagradost.cloudstream3.MainAPI

/**
 * Base class for Saicord providers
 * Contains common functionality for both Bengali and Hindi providers
 */
abstract class SaicordBase : MainAPI() {
    /**
     * Check if a URL is a valid content URL (movie, series, cartoon)
     */
    protected fun isContentUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("/movies/") ||
                lowerUrl.contains("/series/") ||
                lowerUrl.contains("/animation/") ||
                lowerUrl.contains("/tvplus/") ||
                (lowerUrl.endsWith(".html") && lowerUrl.contains(Regex("""/\d+-""")))
    }

    /**
     * Fix relative URLs to absolute URLs
     */
    protected fun fixUrl(url: String): String {
        if (url.startsWith("http")) return url
        if (url.startsWith("//")) return "https:$url"
        if (url.startsWith("/")) return "$mainUrl$url"
        return "$mainUrl/$url"
    }

    /**
     * Extract content ID from URL
     * URLs typically follow pattern: /category/id-name.html
     */
    protected fun extractContentId(url: String): String? {
        val regex = Regex("""/(\d+)-""")
        return regex.find(url)?.groupValues?.get(1)
    }
}
