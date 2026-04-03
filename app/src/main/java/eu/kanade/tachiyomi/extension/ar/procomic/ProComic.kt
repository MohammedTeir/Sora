package eu.kanade.tachiyomi.extension.ar.procomic

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ProComic : HttpSource() {

    override val name = "ProComic"
    override val baseUrl = "https://procomic.net"
    override val lang = "ar"
    override val supportsLatest = true

    override val mirrorCandidates = listOf("https://procomic.pro")

    // In-memory sitemap cache to avoid re-downloading on every scroll page
    private var cachedSitemapEntries: List<String>? = null
    private var sitemapCacheTime: Long = 0L
    private val SITEMAP_CACHE_DURATION = 30 * 60 * 1000L // 30 minutes

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .add("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")

    // ========================= Popular =============================

    override fun popularMangaRequest(page: Int): Request {
        return if (page == 1) {
            // Page 1: fetch HTML for items WITH thumbnails
            GET("$baseUrl/series", headers)
        } else {
            // Page 2+: fetch sitemap for client-side pagination
            // page param is for tracking only — server ignores it
            GET("$baseUrl/sitemap.xml?page=$page", headers)
        }
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val url = response.request.url.toString()

        if (!url.contains("sitemap")) {
            // Page 1: parse HTML — gets items with thumbnails
            val document = response.asJsoup()
            val mangas = mutableListOf<SManga>()

            document.select("a[href*=/series/]").forEach { element ->
                val href = element.attr("href")
                val segments = href.trimStart('/').split("/")
                if (segments.size == 4 && segments[0] == "series") {
                    val manga = SManga.create()
                    manga.setUrlWithoutDomain(href)

                    val titleEl = element.selectFirst("h3") ?: element.selectFirst("h2")
                    manga.title = titleEl?.text()?.trim() ?: element.text().trim()
                    if (manga.title.isBlank()) return@forEach

                    element.selectFirst("img")?.let { img ->
                        manga.thumbnail_url = img.absUrl("src").ifBlank {
                            img.attr("data-src").ifBlank {
                                img.attr("srcset")?.split(" ")?.firstOrNull() ?: ""
                            }
                        }
                    }

                    mangas.add(manga)
                }
            }

            // hasNextPage = true to trigger sitemap fetch on scroll
            return MangasPage(mangas.distinctBy { it.url }, mangas.isNotEmpty())
        } else {
            // Page 2+: use cached sitemap entries with client-side pagination
            val pageStr = url.substringAfterLast("page=").substringBefore("&")
            val page = pageStr.toIntOrNull() ?: 2

            val allSeriesLocs = getSitemapEntries(response)

            val pageSize = 18
            val startIndex = (page - 1) * pageSize
            val pagedLocs = allSeriesLocs.drop(startIndex).take(pageSize)

            val mangas = pagedLocs.map { loc ->
                sitemapLocToManga(loc)
            }

            val hasNextPage = (startIndex + pageSize) < allSeriesLocs.size
            return MangasPage(mangas, hasNextPage)
        }
    }

    // ========================= Latest ==============================

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/updates", headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangaMap = linkedMapOf<String, SManga>()

        // /updates page has links to individual chapters:
        // /series/{type}/{id}/{slug}/{chapterId}/{chapterNum}
        // We extract the series URL from the first 4 segments
        document.select("a[href*=/series/]").forEach { element ->
            val href = element.attr("href")
            val segments = href.trimStart('/').split("/")

            // Chapter links have 6 segments, series links have 4
            val seriesPath = when {
                segments.size >= 6 && segments[0] == "series" ->
                    "/${segments.take(4).joinToString("/")}"
                segments.size == 4 && segments[0] == "series" ->
                    "/$href".trimEnd('/')
                else -> return@forEach
            }

            // Skip if we already have this series
            if (seriesPath in mangaMap) return@forEach

            val manga = SManga.create()
            manga.setUrlWithoutDomain(seriesPath)

            // Title from h3/h2 or the text of the link
            val titleEl = element.selectFirst("h3") ?: element.selectFirst("h2")
            manga.title = titleEl?.text()?.trim()
                ?: element.ownText().trim()
                ?: element.text().trim()
            if (manga.title.isBlank()) return@forEach

            // Thumbnail from img inside the link
            element.selectFirst("img")?.let { img ->
                manga.thumbnail_url = img.absUrl("src").ifBlank {
                    img.attr("data-src").ifBlank {
                        img.attr("srcset")?.split(" ")?.firstOrNull() ?: ""
                    }
                }
            }

            mangaMap[seriesPath] = manga
        }

        return MangasPage(mangaMap.values.toList(), false)
    }

    // ========================= Search ==============================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        // Embed the page and query in the URL for the parsing step
        return GET("$baseUrl/sitemap.xml?page=$page&query=${query.trim().replace(" ", "-")}", headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val url = response.request.url.toString()
        val query = url.substringAfterLast("query=").substringBefore("&").replace("-", " ")
        val pageStr = url.substringAfterLast("page=").substringBefore("&")
        val page = pageStr.toIntOrNull() ?: 1

        var allSeriesLocs = getSitemapEntries(response)

        // Filter locally by query if provided
        if (query.isNotBlank()) {
            allSeriesLocs = allSeriesLocs.filter { loc ->
                val slug = loc.substringAfterLast("/")
                val title = slug.replace("-", " ")
                title.contains(query, ignoreCase = true)
            }
        }

        val pageSize = 18
        val startIndex = (page - 1) * pageSize
        val pagedLocs = allSeriesLocs.drop(startIndex).take(pageSize)

        val mangas = pagedLocs.map { loc ->
            sitemapLocToManga(loc)
        }

        val hasNextPage = (startIndex + pageSize) < allSeriesLocs.size
        return MangasPage(mangas, hasNextPage)
    }

    // ========================= Details =============================

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val manga = SManga.create()

        // Title
        manga.title = document.selectFirst("h1")?.text()?.trim() ?: ""

        // Thumbnail - cover image
        document.selectFirst("img.object-cover")?.let { img ->
            manga.thumbnail_url = img.absUrl("src")
        }

        // Description from meta tag or page content
        document.selectFirst("meta[name=description]")?.let { meta ->
            manga.description = meta.attr("content")
        }

        // Status
        val statusText = document.select("span").text()
        manga.status = when {
            statusText.contains("مستمر", ignoreCase = true) || statusText.contains("ongoing", ignoreCase = true) -> SManga.ONGOING
            statusText.contains("مكتمل", ignoreCase = true) || statusText.contains("completed", ignoreCase = true) -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }

        return manga
    }

    // ========================= Chapters ============================

    override fun chapterListRequest(manga: SManga): Request {
        return GET(baseUrl + manga.url, headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = mutableListOf<SChapter>()

        // Chapter links: /series/{type}/{id}/{slug}/{chapterId}/{chapterNum}
        document.select("a[href*=/series/]").forEach { element ->
            val href = element.attr("href")
            val segments = href.trimStart('/').split("/")
            // Chapter URLs have 6 path segments
            if (segments.size == 6 && segments[0] == "series") {
                val chapter = SChapter.create()
                chapter.setUrlWithoutDomain(href)

                // Chapter name
                val chapterName = element.selectFirst("span.break-words, span.font-semibold, h3")?.text()?.trim()
                chapter.name = chapterName ?: "الفصل ${segments.last()}"

                // Chapter number
                val chapterNum = segments.last().toFloatOrNull()
                if (chapterNum != null) {
                    chapter.chapter_number = chapterNum
                }

                chapters.add(chapter)
            }
        }

        return chapters.distinctBy { it.url }
    }

    // ========================= Pages ===============================

    override fun pageListRequest(chapter: SChapter): Request {
        return GET(baseUrl + chapter.url, headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val html = response.body.string()
        val pages = mutableListOf<Page>()

        // Chapter images are embedded in Next.js RSC payloads as CDN URLs
        val cdnPattern = Regex("""(https?://cdn\d*\.procomic\.(?:pro|net)/\d+/\d+/[^\s"'\\]+\.(?:avif|webp|jpg|jpeg|png))""", RegexOption.IGNORE_CASE)
        val appPattern = Regex("""(https?://app\.procomic\.net/chapters/\d+/\d+/[^\s"'\\]+\.(?:avif|webp|jpg|jpeg|png))""", RegexOption.IGNORE_CASE)

        val imageUrls = mutableListOf<String>()

        // Collect CDN image URLs (original quality)
        cdnPattern.findAll(html).forEach { match ->
            val url = match.groupValues[1]
            if (url !in imageUrls) {
                imageUrls.add(url)
            }
        }

        // If no CDN images found, try app.procomic.net desktop variants
        if (imageUrls.isEmpty()) {
            appPattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if ("desktop" in url && url !in imageUrls) {
                    imageUrls.add(url)
                }
            }
        }

        // If still no images, try all app.procomic.net images
        if (imageUrls.isEmpty()) {
            appPattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url !in imageUrls) {
                    imageUrls.add(url)
                }
            }
        }

        // Fallback: parse using Jsoup for any visible <img> tags
        if (imageUrls.isEmpty()) {
            val document = Jsoup.parse(html)
            document.select("img[src*=procomic], img[src*=prochan]").forEach { img ->
                val src = img.absUrl("src")
                if (src.isNotBlank() && src !in imageUrls) {
                    imageUrls.add(src)
                }
            }
        }

        imageUrls.forEachIndexed { index, url ->
            pages.add(Page(index, "", url))
        }

        return pages
    }

    override fun chapterPageParse(response: Response): SChapter = throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used")

    // ========================= Helpers =============================

    /**
     * Parse sitemap.xml and return all series URLs, using an in-memory cache
     * to avoid re-downloading the full sitemap on every pagination scroll.
     */
    private fun getSitemapEntries(response: Response): List<String> {
        val now = System.currentTimeMillis()
        cachedSitemapEntries?.let { cached ->
            if ((now - sitemapCacheTime) < SITEMAP_CACHE_DURATION) {
                response.close()
                return cached
            }
        }

        val xml = response.body.string()
        val regex = Regex("""<loc>(https?://[^/]+/series/[^<]+)</loc>""")
        val entries = regex.findAll(xml).map { it.groupValues[1] }
            // Domain-agnostic filter: series URLs have 3 segments after /series/
            .filter { loc -> loc.substringAfter("/series/").split("/").size == 3 }
            .toList()

        cachedSitemapEntries = entries
        sitemapCacheTime = now
        return entries
    }

    /**
     * Convert a sitemap <loc> URL into an SManga with a title derived from the slug.
     */
    private fun sitemapLocToManga(loc: String): SManga {
        // Extract path after the domain, keeping it domain-agnostic
        val path = "/" + loc.substringAfter("//").substringAfter("/")
        val slug = path.split("/").last()
        val title = slug.split("-").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
        return SManga.create().apply {
            setUrlWithoutDomain(path)
            this.title = title
        }
    }

    private fun Response.asJsoup(): Document {
        return Jsoup.parse(body.string(), baseUrl)
    }
}
