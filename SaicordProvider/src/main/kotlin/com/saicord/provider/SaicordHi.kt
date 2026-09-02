package com.saicord.provider

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addDuration
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Document

/**
 * Saicord Hindi Provider
 * Supports Movies, Series, Cartoons, and TV+ content from saicord.com/hi/
 *
 * This provider scrapes the Hindi dubbed content from Saicord website.
 * The site uses DataLife Engine (DLE) CMS with Cloudflare protection.
 */
class SaicordHi : SaicordBase() {
    override var name = "Saicord (Hindi)"
    override var mainUrl = "https://saicord.com"
    override var lang = "hi"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Cartoon
    )

    // Base URL for Hindi content
    private val hiBaseUrl get() = "$mainUrl/hi"

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = when (request.name) {
            "Latest Movies" -> "$hiBaseUrl/movies/"
            "Latest Series" -> "$hiBaseUrl/series/"
            "Latest Cartoons" -> "$hiBaseUrl/animation/"
            "Trending" -> "$hiBaseUrl/trending.html"
            "Top Rated" -> "$hiBaseUrl/top.html"
            "Coming Soon" -> "$hiBaseUrl/coming-soon.html"
            "TV+" -> "$hiBaseUrl/tvplus/"
            else -> "$hiBaseUrl/"
        }

        return try {
            val document = app.get(
                url,
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                    "Accept-Language" to "hi-IN,hi;q=0.9,en-US;q=0.8,en;q=0.7"
                )
            ).document
            val items = parseHomePage(document, request.name)

            if (items.isEmpty()) {
                null
            } else {
                newHomePageResponse(request, items, hasNext = items.isNotEmpty())
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseHomePage(document: Document, sectionName: String): List<SearchResponse> {
        val items = mutableListOf<SearchResponse>()

        // Try multiple selectors to find content items
        val selectors = listOf(
            ".movie-item",
            ".content-item",
            ".card",
            ".item",
            "article",
            ".poster-item",
            ".film-item",
            ".media-item",
            ".short-item",
            ".shortstory-item",
            ".grid-item"
        )

        for (selector in selectors) {
            val elements = document.select(selector)
            if (elements.isNotEmpty()) {
                elements.forEach { element ->
                    val link = element.selectFirst("a[href]") ?: return@forEach
                    val href = link.attr("href")
                    val title = element.selectFirst("h2, h3, h4, .title, .name")?.text()
                        ?: link.attr("title")
                        ?: link.text()
                    val poster = element.selectFirst("img")?.let { img ->
                        img.attr("src").ifEmpty { img.attr("data-src") }
                    }
                    val year = element.selectFirst(".year, .date, time")?.text()?.let {
                        Regex("""\d{4}""").find(it)?.value?.toIntOrNull()
                    }
                    val rating = element.selectFirst(".rating, .score")?.text()?.let {
                        Regex("""[\d.]+""").find(it)?.value?.toFloatOrNull()
                    }

                    if (href.isNotEmpty() && title.isNotEmpty()) {
                        val type = when {
                            href.contains("/movies/") -> TvType.Movie
                            href.contains("/series/") -> TvType.TvSeries
                            href.contains("/animation/") -> TvType.Cartoon
                            href.contains("/tvplus/") -> TvType.Movie
                            else -> TvType.Movie
                        }

                        items.add(
                            newMovieSearchResponse(
                                name = title,
                                url = fixUrl(href),
                                type = type
                            ) {
                                this.posterUrl = poster?.let { fixUrl(it) }
                                this.year = year
                                if (rating != null) {
                                    this.quality = when {
                                        rating >= 8.0 -> SearchQuality.HD
                                        rating >= 6.0 -> SearchQuality.SD
                                        else -> SearchQuality.LD
                                    }
                                }
                            }
                        )
                    }
                }
                if (items.isNotEmpty()) break
            }
        }

        // Fallback: try to find links with movie/series patterns
        if (items.isEmpty()) {
            document.select("a[href]").forEach { link ->
                val href = link.attr("href")
                val title = link.text().trim()

                if (title.isNotEmpty() && title.length > 2 && isContentUrl(href)) {
                    val type = when {
                        href.contains("/movies/") -> TvType.Movie
                        href.contains("/series/") -> TvType.TvSeries
                        href.contains("/animation/") -> TvType.Cartoon
                        else -> TvType.Movie
                    }

                    // Avoid duplicate entries
                    if (items.none { it.url == fixUrl(href) }) {
                        items.add(
                            newMovieSearchResponse(
                                name = title,
                                url = fixUrl(href),
                                type = type
                            )
                        )
                    }
                }
            }
        }

        return items
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        return try {
            val searchUrl = "$mainUrl/index.php?do=search&subaction=search&story=$query"
            val document = app.get(
                searchUrl,
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                    "Accept-Language" to "hi-IN,hi;q=0.9,en-US;q=0.8,en;q=0.7"
                )
            ).document

            parseSearchResults(document)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSearchResults(document: Document): List<SearchResponse> {
        val items = mutableListOf<SearchResponse>()

        // Parse search results
        val selectors = listOf(
            ".search-result",
            ".result-item",
            ".movie-item",
            ".content-item",
            ".short-item",
            ".shortstory-item",
            "article",
            ".item"
        )

        for (selector in selectors) {
            val elements = document.select(selector)
            if (elements.isNotEmpty()) {
                elements.forEach { element ->
                    val link = element.selectFirst("a[href]") ?: return@forEach
                    val href = link.attr("href")
                    val title = element.selectFirst("h2, h3, h4, .title, .name")?.text()
                        ?: link.attr("title")
                        ?: link.text()
                    val poster = element.selectFirst("img")?.let { img ->
                        img.attr("src").ifEmpty { img.attr("data-src") }
                    }
                    val year = element.selectFirst(".year, .date")?.text()?.let {
                        Regex("""\d{4}""").find(it)?.value?.toIntOrNull()
                    }

                    if (href.isNotEmpty() && title.isNotEmpty() && isContentUrl(href)) {
                        val type = when {
                            href.contains("/movies/") -> TvType.Movie
                            href.contains("/series/") -> TvType.TvSeries
                            href.contains("/animation/") -> TvType.Cartoon
                            else -> TvType.Movie
                        }

                        items.add(
                            newMovieSearchResponse(
                                name = title,
                                url = fixUrl(href),
                                type = type
                            ) {
                                this.posterUrl = poster?.let { fixUrl(it) }
                                this.year = year
                            }
                        )
                    }
                }
                if (items.isNotEmpty()) break
            }
        }

        return items
    }

    override suspend fun load(url: String): LoadResponse? {
        return try {
            val document = app.get(
                url,
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                )
            ).document

            parseDetails(document, url)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDetails(document: Document, url: String): LoadResponse? {
        // Extract title
        val title = document.selectFirst("h1, .title, .name, .movie-title")?.text()
            ?: return null

        // Extract poster image
        val poster = document.selectFirst("img.film-poster-img, .poster img, .movie-poster img, meta[property=og:image]")
            ?.let { img ->
                if (img.tagName() == "meta") img.attr("content")
                else img.attr("src").ifEmpty { img.attr("data-src") }
            }

        // Extract description/plot
        val plot = document.selectFirst(".description, .plot, .overview, .movie-description, .full-story, meta[property=og:description]")
            ?.let { el ->
                if (el.tagName() == "meta") el.attr("content")
                else el.text()
            }

        // Extract year
        val year = document.selectFirst(".year, .date, time, [itemprop=datePublished]")
            ?.text()?.let {
                Regex("""\d{4}""").find(it)?.value?.toIntOrNull()
            }

        // Extract rating
        val rating = document.selectFirst(".rating, .score, .imdb-rating")
            ?.text()?.let {
                Regex("""[\d.]+""").find(it)?.value?.toFloatOrNull()
            }

        // Extract duration
        val duration = document.selectFirst(".duration, .runtime, [itemprop=duration]")
            ?.text()

        // Extract genres/tags
        val tags = document.select(".genre a, .tag a, .genres a, [itemprop=genre]")
            .mapNotNull { it.text().trim() }
            .filter { it.isNotEmpty() }

        // Extract director
        val director = document.selectFirst(".director a, [itemprop=director]")
            ?.text()

        // Extract cast/actors
        val actors = document.select(".cast a, .actor a, .stars a, [itemprop=actor]")
            .mapNotNull { it.text().trim() }
            .filter { it.isNotEmpty() }

        // Extract trailer URL
        val trailer = document.selectFirst("iframe[src*=youtube], iframe[src*=youtu.be], a[href*=youtube]")
            ?.let { el ->
                val src = el.attr("src")
                if (src.isNotEmpty()) src
                else el.attr("href")
            }

        // Determine content type based on URL
        val isSeries = url.contains("/series/") || document.select(".episode, .season").isNotEmpty()
        val isCartoon = url.contains("/animation/")

        val contentType = when {
            isSeries -> TvType.TvSeries
            isCartoon -> TvType.Cartoon
            else -> TvType.Movie
        }

        // Check if content is coming soon
        val comingSoon = document.selectFirst(".coming-soon, .coming_soon, .soon") != null ||
                document.text().lowercase().contains("coming soon")

        return if (contentType == TvType.TvSeries) {
            // For TV Series, extract episodes
            val episodes = extractEpisodes(document, url)
            newTvSeriesLoadResponse(
                name = title,
                url = url,
                type = TvType.TvSeries,
                episodes = episodes
            ) {
                this.posterUrl = poster?.let { fixUrl(it) }
                this.plot = plot
                this.year = year
                this.showStatus = if (comingSoon) ShowStatus.ON_GOING else null
                this.duration = parseDuration(duration)
                this.tags = tags
                this.recommendations = extractRecommendations(document)
                if (rating != null) addScore(rating, maxValue = 10f)
                if (actors.isNotEmpty()) addActors(actors)
                if (trailer != null) addTrailer(trailer)
            }
        } else {
            // For Movies and Cartoons
            newMovieLoadResponse(
                name = title,
                url = url,
                type = contentType,
                dataUrl = url
            ) {
                this.posterUrl = poster?.let { fixUrl(it) }
                this.plot = plot
                this.year = year
                this.duration = parseDuration(duration)
                this.tags = tags
                this.recommendations = extractRecommendations(document)
                if (rating != null) addScore(rating, maxValue = 10f)
                if (actors.isNotEmpty()) addActors(actors)
                if (trailer != null) addTrailer(trailer)
            }
        }
    }

    private fun extractEpisodes(document: Document, seriesUrl: String): List<Episode> {
        val episodes = mutableListOf<Episode>()

        // Try to find episode links
        val episodeSelectors = listOf(
            ".episode a",
            ".episodes a",
            ".season-episodes a",
            ".episode-list a",
            ".ep-list a",
            "a[href*=episode]",
            "a[href*=ep]"
        )

        for (selector in episodeSelectors) {
            val elements = document.select(selector)
            if (elements.isNotEmpty()) {
                elements.forEachIndexed { index, element ->
                    val href = element.attr("href")
                    val title = element.text().trim()

                    if (href.isNotEmpty()) {
                        episodes.add(
                            newEpisode(fixUrl(href)) {
                                this.name = title.ifEmpty { "Episode ${index + 1}" }
                                this.episode = index + 1
                            }
                        )
                    }
                }
                if (episodes.isNotEmpty()) break
            }
        }

        return episodes
    }

    private fun extractRecommendations(document: Document): List<MovieSearchResponse> {
        return document.select(".recommended a, .similar a, .related a, .recommendations a")
            .take(10)
            .mapNotNull { element ->
                val href = element.attr("href")
                val title = element.text().trim()

                if (href.isNotEmpty() && title.isNotEmpty() && isContentUrl(href)) {
                    val type = when {
                        href.contains("/movies/") -> TvType.Movie
                        href.contains("/series/") -> TvType.TvSeries
                        href.contains("/animation/") -> TvType.Cartoon
                        else -> TvType.Movie
                    }

                    newMovieSearchResponse(
                        name = title,
                        url = fixUrl(href),
                        type = type
                    )
                } else null
            }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val document = app.get(
                data,
                headers = mapOf(
                    "User-Agent" to USER_AGENT,
                    "Referer" to hiBaseUrl
                )
            ).document

            // Try to find video sources
            val videoSources = extractVideoSources(document, data)

            // Try to load extractors for any iframe sources
            val iframes = document.select("iframe[src]").map { it.attr("src") }

            for (iframe in iframes) {
                try {
                    loadExtractor(
                        url = fixUrl(iframe),
                        referer = data,
                        subtitleCallback = subtitleCallback,
                        callback = callback
                    )
                } catch (e: Exception) {
                    // Continue with other sources
                }
            }

            // Add direct video sources if found
            videoSources.forEach { source ->
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = source.name,
                        url = source.url
                    ) {
                        this.referer = data
                        this.quality = source.quality
                    }
                )
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun extractVideoSources(document: Document, pageUrl: String): List<VideoSource> {
        val sources = mutableListOf<VideoSource>()

        // Look for direct video URLs
        val videoPatterns = listOf(
            """src\s*[=:]\s*["']([^"']+\.m3u8[^"']*)["']""",
            """src\s*[=:]\s*["']([^"']+\.mp4[^"']*)["']""",
            """file\s*[=:]\s*["']([^"']+\.m3u8[^"']*)["']""",
            """file\s*[=:]\s*["']([^"']+\.mp4[^"']*)["']""",
            """source\s*[=:]\s*["']([^"']+\.m3u8[^"']*)["']""",
            """source\s*[=:]\s*["']([^"']+\.mp4[^"']*)["']"""
        )

        val html = document.html()
        for (pattern in videoPatterns) {
            val matches = Regex(pattern, RegexOption.IGNORE_CASE).findAll(html)
            for (match in matches) {
                val url = match.groupValues[1]
                if (url.isNotEmpty() && !url.contains("youtube") && !url.contains("dailymotion")) {
                    sources.add(
                        VideoSource(
                            name = "Direct",
                            url = fixUrl(url),
                            quality = Qualities.P720.value
                        )
                    )
                }
            }
        }

        return sources
    }

    private fun parseDuration(duration: String?): Int? {
        if (duration == null) return null
        val hours = Regex("""(\d+)\s*h""", RegexOption.IGNORE_CASE).find(duration)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = Regex("""(\d+)\s*m""", RegexOption.IGNORE_CASE).find(duration)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val totalMinutes = hours * 60 + minutes
        return if (totalMinutes > 0) totalMinutes else null
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    data class VideoSource(
        val name: String,
        val url: String,
        val quality: Int
    )
}
