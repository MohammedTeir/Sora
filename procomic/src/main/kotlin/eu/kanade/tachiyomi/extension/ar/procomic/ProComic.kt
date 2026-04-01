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
import org.jsoup.nodes.Element

class ProComic : HttpSource() {

    override val name = "ProComic"
    override val baseUrl = "https://procomic.pro"
    override val lang = "ar"
    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .add("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")

    // ========================= Popular =============================

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/series", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = mutableListOf<SManga>()

        // Find all links that point to a series page (4 path segments: series/type/id/slug)
        document.select("a[href*=/series/]").forEach { element ->
            val href = element.attr("href")
            val segments = href.trimStart('/').split("/")
            // A series link has exactly 4 segments: series/{type}/{id}/{slug}
            if (segments.size == 4 && segments[0] == "series") {
                val manga = SManga.create()
                manga.setUrlWithoutDomain(href)

                // Title from h3 inside the link
                val titleEl = element.selectFirst("h3")
                manga.title = titleEl?.text()?.trim() ?: element.text().trim()
                if (manga.title.isBlank()) return@forEach

                // Thumbnail from img inside the link
                element.selectFirst("img")?.let { img ->
                    manga.thumbnail_url = img.absUrl("src").ifBlank {
                        img.attr("data-src")
                    }
                }

                mangas.add(manga)
            }
        }

        return MangasPage(mangas.distinctBy { it.url }, false)
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

            // Title from h3 or the text of the link
            val titleEl = element.selectFirst("h3")
            manga.title = titleEl?.text()?.trim()
                ?: element.ownText().trim()
                ?: element.text().trim()
            if (manga.title.isBlank()) return@forEach

            // Thumbnail from img inside the link
            element.selectFirst("img")?.let { img ->
                manga.thumbnail_url = img.absUrl("src").ifBlank {
                    img.attr("data-src")
                }
            }

            mangaMap[seriesPath] = manga
        }

        return MangasPage(mangaMap.values.toList(), false)
    }

    // ========================= Search ==============================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/series?search=$query", headers)
    }

    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

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

    private fun Response.asJsoup(): Document {
        return Jsoup.parse(body.string(), baseUrl)
    }
}
