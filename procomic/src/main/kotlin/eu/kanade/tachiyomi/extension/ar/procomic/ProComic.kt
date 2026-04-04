package eu.kanade.tachiyomi.extension.ar.procomic

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
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
import org.jsoup.parser.Parser

class ProComic : HttpSource() {

    override val name = "ProComic"
    override val baseUrl = "https://procomic.pro"
    override val lang = "ar"
    override val supportsLatest = true

    // In-memory sitemap cache to avoid re-downloading on every scroll page
    private var cachedSitemapEntries: List<String>? = null
    private var sitemapCacheTime: Long = 0L
    private val SITEMAP_CACHE_DURATION = 30 * 60 * 1000L // 30 minutes

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
        .add("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
        .add("Sec-Fetch-Dest", "document")
        .add("Sec-Fetch-Mode", "navigate")
        .add("Sec-Fetch-Site", "same-origin")
        .add("Sec-Fetch-User", "?1")
        .add("Upgrade-Insecure-Requests", "1")

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

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val segments = manga.url.trim('/').split('/')
        val type = segments.getOrNull(1) ?: "novel"
        val id = segments.getOrNull(2) ?: ""
        val mangaSlug = segments.getOrNull(3) ?: ""

        val apiType = if (type == "novel") "novels" else type

        val apiHeaders = headers.newBuilder()
            .set("Accept", "application/json, text/plain, */*")
            .set("Sec-Fetch-Dest", "empty")
            .set("Sec-Fetch-Mode", "cors")
            .removeAll("Sec-Fetch-User")
            .removeAll("Upgrade-Insecure-Requests")
            .build()

        // Try single large request first (most reliable — gets all chapters at once)
        val allUrl = "$baseUrl/api/public/$apiType/$id/chapters?limit=10000&order=desc"
        val allResponse = client.newCall(GET(allUrl, apiHeaders)).await()

        if (allResponse.isSuccessful) {
            val body = allResponse.body.string()
            val chapters = parseChaptersFromJson(body, type, id, mangaSlug)
            if (chapters.isNotEmpty()) return chapters
            // Fix: If we got empty list but response was successful, check if API returned valid JSON
            // This catches cases where API returns {"data": []} but chapters actually exist
            allResponse.close()
        } else {
            allResponse.close()
        }

        // Fallback: paginated fetching with offset
        val allChapters = mutableListOf<SChapter>()
        var page = 1
        val limit = 100

        while (true) {
            val url = "$baseUrl/api/public/$apiType/$id/chapters?limit=$limit&offset=${(page - 1) * limit}&order=desc"
            val response = client.newCall(GET(url, apiHeaders)).await()

            if (!response.isSuccessful) {
                response.close()
                break
            }

            val jsonString = response.body.string()
            val json = try {
                org.json.JSONObject(jsonString)
            } catch (e: Exception) { break }

            val data = json.optJSONArray("data") ?: break
            if (data.length() == 0) break

            parseChapterArray(data, type, id, mangaSlug, allChapters)

            // Fix: Stop if we got fewer results than requested (no more pages)
            if (data.length() < limit) break
            page++

            // Fix: Safety limit to prevent infinite loops (max 10000 chapters)
            if (page > 100) break
        }

        return allChapters.sortedByDescending { it.chapter_number }
    }

    override fun chapterListRequest(manga: SManga): Request = throw UnsupportedOperationException()
    override fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()

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
        // Fix: Use Jsoup for more robust XML parsing instead of regex
        val document = Jsoup.parse(xml, "", Parser.xmlParser())
        val entries = document.select("loc").eachText()
            .filter { loc ->
                loc.contains("/series/") && loc.substringAfter("/series/").split("/").size == 3
            }

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

    private fun parseChaptersFromJson(jsonString: String, type: String, id: String, mangaSlug: String): List<SChapter> {
        val json = try {
            org.json.JSONObject(jsonString)
        } catch (e: Exception) { return emptyList() }

        val data = json.optJSONArray("data") ?: return emptyList()
        val chapters = mutableListOf<SChapter>()
        parseChapterArray(data, type, id, mangaSlug, chapters)
        return chapters.sortedByDescending { it.chapter_number }
    }

    private fun parseChapterArray(
        data: org.json.JSONArray,
        type: String,
        id: String,
        mangaSlug: String,
        chapters: MutableList<SChapter>,
    ) {
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            // Fix: Case-insensitive language check to catch "ar", "AR", "Arabic", etc.
            val language = item.optString("language", "AR")
            if (language.isNotBlank() && !language.equals("AR", ignoreCase = true) && !language.equals("Arabic", ignoreCase = true)) continue

            val chapterId = item.getInt("id")
            val numStr = item.getString("chapter_number")
            val title = item.optString("title", "")

            val chapter = SChapter.create()
            chapter.url = "/series/$type/$id/$mangaSlug/$chapterId/$numStr"
            chapter.name = if (title.isNotBlank()) title else "الفصل $numStr"
            chapter.chapter_number = numStr.toFloatOrNull() ?: -1f
            chapter.date_upload = parseDate(item.optString("published_at"))
            chapters.add(chapter)
        }
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .parse(dateStr)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    private fun Response.asJsoup(): Document {
        return Jsoup.parse(body.string(), baseUrl)
    }
}
