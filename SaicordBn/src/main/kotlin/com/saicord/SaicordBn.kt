package com.saicord

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addDuration
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class SaicordBn : MainAPI() {
    override var name = "Saicord (Bengali)"
    override var mainUrl = "https://saicord.com"
    override var lang = "bn"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Cartoon)

    private val bnUrl get() = "$mainUrl/bn"

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    private fun fixUrl(url: String): String {
        if (url.startsWith("http")) return url
        if (url.startsWith("//")) return "https:$url"
        if (url.startsWith("/")) return mainUrl + url
        return "$mainUrl/$url"
    }

    private fun headers() = mapOf(
        "User-Agent" to USER_AGENT,
        "Accept-Language" to "bn-BD,bn;q=0.9,en-US;q=0.8,en;q=0.7"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val url = when (request.name) {
            "Movies" -> "$bnUrl/movies/"
            "Series" -> "$bnUrl/series/"
            "Cartoons" -> "$bnUrl/animation/"
            "Trending" -> "$bnUrl/trending.html"
            "Top Rated" -> "$bnUrl/top.html"
            "Coming Soon" -> "$bnUrl/coming-soon.html"
            else -> bnUrl
        }

        val document = app.get(url, headers = headers()).document
        val items = document.select("article, .short-item, .shortstory, .movie-item, .content-item, .item, .card, .poster-item, .grid-item").mapNotNull { el ->
            val link = el.selectFirst("a[href]") ?: return@mapNotNull null
            val href = link.attr("href")
            val title = el.selectFirst("h2, h3, h4, .title, .name")?.text()
                ?: link.attr("title").ifEmpty { null }
                ?: link.text().trim()
            val poster = el.selectFirst("img")?.let { img ->
                img.attr("src").ifEmpty { img.attr("data-src") }.ifEmpty { img.attr("data-original") }
            }
            val year = Regex("""\d{4}""").find(el.text())?.value?.toIntOrNull()

            if (title.isBlank()) return@mapNotNull null

            val type = when {
                href.contains("/movies/") -> TvType.Movie
                href.contains("/series/") -> TvType.TvSeries
                href.contains("/animation/") -> TvType.Cartoon
                else -> TvType.Movie
            }

            newMovieSearchResponse(title, fixUrl(href), type) {
                this.posterUrl = poster?.let { fixUrl(it) }
                this.year = year
            }
        }

        return newHomePageResponse(request, items)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        val document = app.get(
            "$mainUrl/index.php?do=search&subaction=search&story=$query",
            headers = headers()
        ).document

        return document.select("article, .short-item, .shortstory, .movie-item, .content-item, .item").mapNotNull { el ->
            val link = el.selectFirst("a[href]") ?: return@mapNotNull null
            val href = link.attr("href")
            val title = el.selectFirst("h2, h3, h4, .title, .name")?.text()
                ?: link.attr("title").ifEmpty { null }
                ?: link.text().trim()
            val poster = el.selectFirst("img")?.let { img ->
                img.attr("src").ifEmpty { img.attr("data-src") }.ifEmpty { img.attr("data-original") }
            }

            if (title.isBlank()) return@mapNotNull null

            val type = when {
                href.contains("/movies/") -> TvType.Movie
                href.contains("/series/") -> TvType.TvSeries
                href.contains("/animation/") -> TvType.Cartoon
                else -> TvType.Movie
            }

            newMovieSearchResponse(title, fixUrl(href), type) {
                this.posterUrl = poster?.let { fixUrl(it) }
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url, headers = headers()).document

        val title = doc.selectFirst("h1, .title, .name, .movie-title")?.text() ?: return null
        val poster = doc.selectFirst("img.film-poster-img, .poster img, meta[property=og:image]")
            ?.let { if (it.tagName() == "meta") it.attr("content") else it.attr("src").ifEmpty { it.attr("data-src") } }
        val plot = doc.selectFirst(".description, .plot, .overview, .full-story, meta[property=og:description]")
            ?.let { if (it.tagName() == "meta") it.attr("content") else it.text() }
        val year = Regex("""\d{4}""").find(
            doc.selectFirst(".year, .date, time, [itemprop=datePublished]")?.text() ?: ""
        )?.value?.toIntOrNull()
        val rating = Regex("""[\d.]+""").find(
            doc.selectFirst(".rating, .score, .imdb-rating")?.text() ?: ""
        )?.value?.toFloatOrNull()
        val duration = doc.selectFirst(".duration, .runtime")?.text()
            ?.let { Regex("""(\d+)""").find(it)?.value?.toIntOrNull() }
        val tags = doc.select(".genre a, .tag a, .genres a").map { it.text().trim() }.filter { it.isNotEmpty() }
        val actors = doc.select(".cast a, .actor a, .stars a").map { it.text().trim() }.filter { it.isNotEmpty() }

        val isSeries = url.contains("/series/") || doc.select(".episode, .season").isNotEmpty()
        val isCartoon = url.contains("/animation/")

        return if (isSeries) {
            val episodes = doc.select(".episode a, .episodes a, a[href*=episode], a[href*=ep], a[href*=watch]").mapNotNull { el ->
                val epUrl = el.attr("href")
                val epTitle = el.text().trim()
                if (epUrl.isEmpty()) return@mapNotNull null
                newEpisode(fixUrl(epUrl)) { name = epTitle }
            }
            newTvSeriesLoadResponse(title, fixUrl(url), TvType.TvSeries, episodes) {
                this.posterUrl = poster?.let { fixUrl(it) }
                this.plot = plot
                this.year = year
                this.duration = duration
                this.tags = tags
                if (actors.isNotEmpty()) addActors(actors)
            }
        } else {
            val type = if (isCartoon) TvType.Cartoon else TvType.Movie
            newMovieLoadResponse(title, fixUrl(url), type, fixUrl(url)) {
                this.posterUrl = poster?.let { fixUrl(it) }
                this.plot = plot
                this.year = year
                this.duration = duration
                this.tags = tags
                if (actors.isNotEmpty()) addActors(actors)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = headers()).document

        // Extract direct video sources - try multiple patterns
        val patterns = listOf(
            // m3u8 streams
            """(?:src|file|source|video_url|stream_url|videoUrl|streamUrl)\s*[:=]\s*["']([^"']+\.m3u8[^"']*)["']""",
            // mp4 files
            """(?:src|file|source|video_url|stream_url|videoUrl|streamUrl)\s*[:=]\s*["']([^"']+\.mp4[^"']*)["']""",
            // Any video URL (broader match)
            """(?:src|file|source|video_url|stream_url|videoUrl|streamUrl)\s*[:=]\s*["'](https?://[^"']+(?:\.m3u8|\.mp4|\.webm|\.mkv)[^"']*)["']""",
            // HLS streams
            """["'](https?://[^"']+\.m3u8[^"']*)["']""",
            // Generic video files
            """["'](https?://[^"']+\.(?:mp4|mkv|webm)[^"']*)["']""",
        )
        val html = doc.html()
        val foundUrls = mutableSetOf<String>()

        for (pattern in patterns) {
            Regex(pattern, RegexOption.IGNORE_CASE).findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotEmpty() && url !in foundUrls) {
                    foundUrls.add(url)
                    callback(
                        newExtractorLink(name, name, fixUrl(url), ExtractorLinkType.VIDEO) {
                            this.referer = data
                            this.quality = Qualities.P720.value
                        }
                    )
                }
            }
        }

        // Try loadExtractor for iframes and video tags
        doc.select("iframe[src], video source, video[src]").forEach { element ->
            val src = when {
                element.tagName() == "source" -> element.attr("src")
                element.tagName() == "video" -> element.attr("src")
                else -> element.attr("src")
            }
            if (src.isNotEmpty()) {
                val fullUrl = fixUrl(src)
                if (fullUrl !in foundUrls) {
                    foundUrls.add(fullUrl)
                    try {
                        // If direct video file, create extractor link
                        if (Regex("""\.(m3u8|mp4|mkv|webm)($|\?)""", RegexOption.IGNORE_CASE).containsMatchIn(fullUrl)) {
                            callback(
                                newExtractorLink(name, name, fullUrl, ExtractorLinkType.VIDEO) {
                                    this.referer = data
                                    this.quality = Qualities.P720.value
                                }
                            )
                        } else {
                            // Try as iframe through loadExtractor
                            loadExtractor(fullUrl, data, subtitleCallback, callback)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        return true
    }
}
