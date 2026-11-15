package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.net.ssl.SSLException

/** Error returned by the server or while talking to it. [message] is already user-readable (Slovak). */
class SubsonicException(message: String, val code: Int = 0) : Exception(message)

data class PingInfo(
    val apiVersion: String,
    val serverType: String,
    val serverVersion: String,
    val openSubsonic: Boolean
)

data class SearchResult(
    val artists: List<ArtistItem>,
    val albums: List<AlbumItem>,
    val tracks: List<TrackItem>
)

data class ScanStatus(val scanning: Boolean, val count: Long)

data class StreamProbe(val firstByteMs: Long, val totalMs: Long, val bytes: Int, val contentType: String)

/**
 * Minimal Subsonic / OpenSubsonic (Navidrome) REST client.
 *
 * Authentication uses the salted token scheme: t = md5(password + salt), s = salt.
 * The password itself is never needed after the first login.
 *
 * Ids in the returned list models carry a prefix so the UI can route clicks:
 * "pl:" playlist, "al:" album, "ar:" artist. Track ids are raw server ids.
 */
class SubsonicApi(
    baseUrl: String,
    val username: String,
    private val token: String,
    private val salt: String
) {
    val baseUrl: String = normalizeUrl(baseUrl)

    // ---------------------------------------------------------------------------------------------
    // URL building
    // ---------------------------------------------------------------------------------------------

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    fun buildUrl(method: String, params: List<Pair<String, String>> = emptyList(), json: Boolean = true): String {
        val sb = StringBuilder()
        sb.append(baseUrl).append("/rest/").append(method).append(".view?")
        sb.append("u=").append(enc(username))
        sb.append("&t=").append(enc(token))
        sb.append("&s=").append(enc(salt))
        sb.append("&v=").append(API_VERSION)
        sb.append("&c=").append(CLIENT_NAME)
        if (json) sb.append("&f=json")
        for ((k, v) in params) {
            sb.append('&').append(enc(k)).append('=').append(enc(v))
        }
        return sb.toString()
    }

    /** Cover art URL. The URL is stable for a given login (salt does not change), so image caches work. */
    fun coverUrl(coverArtId: String, size: Int = 600): String {
        if (coverArtId.isBlank()) return ""
        return buildUrl("getCoverArt", listOf("id" to coverArtId, "size" to size.toString()), json = false)
    }

    /**
     * Stream URL. [format] null = server default (original file unless the user's server profile
     * transcodes), "raw" = never transcode, "mp3" / "opus" = transcode with [maxBitRate] kbps.
     */
    fun streamUrl(trackId: String, format: String? = null, maxBitRate: Int? = null): String {
        val params = mutableListOf("id" to trackId)
        if (!format.isNullOrBlank()) params += "format" to format
        if (maxBitRate != null && maxBitRate > 0) params += "maxBitRate" to maxBitRate.toString()
        params += "estimateContentLength" to "true"
        return buildUrl("stream", params, json = false)
    }

    /** Download URL for downloading raw original audio files from the server for offline caching. */
    fun downloadUrl(trackId: String): String =
        buildUrl("download", listOf("id" to trackId), json = false)

    // ---------------------------------------------------------------------------------------------
    // HTTP
    // ---------------------------------------------------------------------------------------------

    private fun httpGet(url: String): String {
        var conn: HttpURLConnection? = null
        try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 25_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code !in 200..299) throw SubsonicException(httpMessage(code), code)
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: SubsonicException) {
            throw e
        } catch (e: UnknownHostException) {
            throw SubsonicException("Server sa nenašiel. Skontroluj adresu servera.")
        } catch (e: SocketTimeoutException) {
            throw SubsonicException("Server neodpovedá (vypršal čas). Skontroluj adresu, port a pripojenie.")
        } catch (e: ConnectException) {
            throw SubsonicException("Server odmieta spojenie. Skontroluj adresu a port.")
        } catch (e: SSLException) {
            throw SubsonicException("Problém so zabezpečeným spojením (HTTPS / certifikát). Skús http:// ak je server len v lokálnej sieti.")
        } catch (e: MalformedURLException) {
            throw SubsonicException("Neplatná adresa servera.")
        } catch (e: IOException) {
            throw SubsonicException("Nepodarilo sa pripojiť k serveru: ${e.message ?: "neznáma chyba"}")
        } catch (e: IllegalArgumentException) {
            throw SubsonicException("Neplatná adresa servera.")
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpMessage(code: Int): String = when (code) {
        401, 403 -> "Prístup zamietnutý (HTTP $code)."
        404 -> "Na tejto adrese sa nenašlo Navidrome / Subsonic API (HTTP 404). Skontroluj adresu."
        502, 503, 504 -> "Server je momentálne nedostupný (HTTP $code)."
        else -> "Server vrátil chybu HTTP $code."
    }

    private fun errorMessage(code: Int, serverMessage: String?): String = when (code) {
        40 -> "Nesprávne používateľské meno alebo heslo."
        41 -> "Server nepodporuje prihlásenie tokenom."
        50 -> "Na túto akciu nemáš oprávnenie."
        70 -> "Požadovaná položka sa na serveri nenašla."
        else -> serverMessage?.takeIf { it.isNotBlank() } ?: "Chyba servera (kód $code)."
    }

    private suspend fun call(method: String, params: List<Pair<String, String>> = emptyList()): JSONObject =
        withContext(Dispatchers.IO) {
            val body = httpGet(buildUrl(method, params))
            val root = try {
                JSONObject(body).getJSONObject("subsonic-response")
            } catch (e: JSONException) {
                throw SubsonicException("Server neodpovedá ako Navidrome / Subsonic API. Skontroluj adresu a port.")
            }
            if (root.optString("status") != "ok") {
                val err = root.optJSONObject("error")
                val code = err?.optInt("code") ?: 0
                throw SubsonicException(errorMessage(code, err?.optString("message")), code)
            }
            root
        }

    // ---------------------------------------------------------------------------------------------
    // API calls
    // ---------------------------------------------------------------------------------------------

    suspend fun ping(): PingInfo {
        val r = call("ping")
        return PingInfo(
            apiVersion = r.optString("version"),
            serverType = r.optString("type").ifBlank { "Subsonic" },
            serverVersion = r.optString("serverVersion"),
            openSubsonic = r.optBoolean("openSubsonic", false)
        )
    }

    @Volatile
    private var cachedTrackPool: List<TrackItem>? = null

    @Volatile
    private var cachedAlbumPool: List<AlbumItem>? = null

    @Volatile
    private var cachedRawGenres: List<GenreItem>? = null

    private suspend fun rawGenres(): List<GenreItem> {
        cachedRawGenres?.let { if (it.isNotEmpty()) return it }
        val r = call("getGenres")
        val list = r.optJSONObject("genres")?.arr("genre").orEmpty()
            .map { GenreItem(it.optString("value").trim(), it.optInt("songCount")) }
            .filter { it.name.isNotBlank() }
            .sortedByDescending { it.songCount }
        if (list.isNotEmpty()) cachedRawGenres = list
        return list
    }

    /**
     * Loads and caches up to 3,500 tracks from the server in parallel so smart genres can rescue
     * songs that were tagged simply as "Rock" or left untagged in their ID3 metadata.
     */
    suspend fun getOrLoadCatalogPool(): List<TrackItem> = coroutineScope {
        cachedTrackPool?.let { if (it.isNotEmpty()) return@coroutineScope it }
        val raw = try { rawGenres() } catch (_: Exception) { emptyList() }

        // 1. Try OpenSubsonic / Navidrome search3 with empty query across 6 parallel pages (3,000 songs)
        val searchPages = (0 until 6).map { pageIdx ->
            async {
                try {
                    val r = call(
                        "search3",
                        listOf(
                            "query" to "",
                            "artistCount" to "0",
                            "albumCount" to "0",
                            "songCount" to "500",
                            "songOffset" to (pageIdx * 500).toString()
                        )
                    )
                    r.optJSONObject("searchResult3")?.arr("song").orEmpty().map(::songFrom)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten()

        // 2. Also pull from the top largest genre(s) (like "Rock") if search3 returned fewer tracks
        val topGenre = raw.firstOrNull()
        val rockPages = if (topGenre != null && searchPages.size < topGenre.songCount.coerceAtMost(2500)) {
            val pagesNeeded = ((topGenre.songCount.coerceAtMost(3000) + 499) / 500).coerceIn(1, 6)
            (0 until pagesNeeded).map { pageIdx ->
                async {
                    try {
                        val r = call(
                            "getSongsByGenre",
                            listOf(
                                "genre" to topGenre.name,
                                "count" to "500",
                                "offset" to (pageIdx * 500).toString()
                            )
                        )
                        r.optJSONObject("songsByGenre")?.arr("song").orEmpty().map(::songFrom)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        } else {
            emptyList()
        }

        val combined = (searchPages + rockPages).distinctBy { it.id }
        if (combined.isNotEmpty()) {
            cachedTrackPool = combined
        }
        combined
    }

    suspend fun getOrLoadAlbumsPool(): List<AlbumItem> = coroutineScope {
        cachedAlbumPool?.let { if (it.isNotEmpty()) return@coroutineScope it }
        val pages = (0 until 4).map { pageIdx ->
            async {
                try {
                    val r = call(
                        "getAlbumList2",
                        listOf(
                            "type" to "alphabeticalByName",
                            "size" to "500",
                            "offset" to (pageIdx * 500).toString()
                        )
                    )
                    r.optJSONObject("albumList2")?.arr("album").orEmpty().map(::albumFrom)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.id }
        if (pages.isNotEmpty()) {
            cachedAlbumPool = pages
        }
        pages
    }

    /** type: newest, recent, frequent, random, alphabeticalByName, byGenre... */
    suspend fun albumList(
        type: String,
        size: Int = 30,
        offset: Int = 0,
        genre: String? = null,
        allowSmartFallback: Boolean = true
    ): List<AlbumItem> {
        if (type == "byGenre" && !genre.isNullOrBlank()) {
            val spec = if (allowSmartFallback) SmartGenreCatalog.findSpec(genre) else null
            val direct = try {
                val params = listOf("type" to type, "size" to size.toString(), "offset" to offset.toString(), "genre" to genre)
                val r = call("getAlbumList2", params)
                r.optJSONObject("albumList2")?.arr("album").orEmpty().map(::albumFrom)
            } catch (_: Exception) {
                emptyList()
            }
            if (spec != null && direct.size < size && offset == 0) {
                val poolAlbums = try { getOrLoadAlbumsPool() } catch (_: Exception) { emptyList() }
                val matched = poolAlbums.filter { SmartGenreCatalog.matchesAlbum(it, spec) }
                return (direct + matched).distinctBy { it.id }.take(size)
            }
            return direct
        }
        val params = mutableListOf("type" to type, "size" to size.toString(), "offset" to offset.toString())
        if (!genre.isNullOrBlank()) params += "genre" to genre
        val r = call("getAlbumList2", params)
        return r.optJSONObject("albumList2")?.arr("album").orEmpty().map(::albumFrom)
    }

    suspend fun randomSongs(size: Int = 40, genre: String? = null): List<TrackItem> {
        if (!genre.isNullOrBlank()) {
            val spec = SmartGenreCatalog.findSpec(genre)
            val direct = try {
                val r = call("getRandomSongs", listOf("size" to size.toString(), "genre" to genre))
                r.optJSONObject("randomSongs")?.arr("song").orEmpty().map(::songFrom)
            } catch (_: Exception) {
                emptyList()
            }
            if (spec != null && direct.size < size) {
                val pool = try { getOrLoadCatalogPool() } catch (_: Exception) { emptyList() }
                val matched = pool.filter { SmartGenreCatalog.matchesTrack(it, spec) }.shuffled()
                return (direct + matched).distinctBy { it.id }.take(size)
            }
            return direct
        }
        val params = mutableListOf("size" to size.toString())
        val r = call("getRandomSongs", params)
        return r.optJSONObject("randomSongs")?.arr("song").orEmpty().map(::songFrom)
    }

    suspend fun artists(): List<ArtistItem> {
        val r = call("getArtists")
        val indexes = r.optJSONObject("artists")?.arr("index").orEmpty()
        return indexes.flatMap { it.arr("artist") }.map(::artistFrom)
    }

    suspend fun playlists(): List<PlaylistItem> {
        val r = call("getPlaylists")
        return r.optJSONObject("playlists")?.arr("playlist").orEmpty().map(::playlistFrom)
    }

    /**
     * Returns an enriched list of genres combining Smart Genres (populated from both server tags
     * and artist/album/title/year classification of the catalog pool) with raw server genres.
     */
    suspend fun genres(): List<GenreItem> = coroutineScope {
        val rawDef = async { rawGenres() }
        val poolDef = async { try { getOrLoadCatalogPool() } catch (_: Exception) { emptyList() } }
        val raw = rawDef.await()
        val pool = poolDef.await()

        val smartItems = SmartGenreCatalog.specs.mapNotNull { spec ->
            val matchingServerTagNames = SmartGenreCatalog.matchingServerGenres(spec.canonicalName, raw, spec)
                .map { it.lowercase(Locale.ROOT) }
                .toSet()
            val rawTagSum = raw
                .filter { it.name.trim().lowercase(Locale.ROOT) in matchingServerTagNames }
                .sumOf { it.songCount }
            val poolMatches = pool.count { SmartGenreCatalog.matchesTrack(it, spec) }
            // Estimate total distinct tracks for this smart genre
            val totalCount = maxOf(rawTagSum, poolMatches, minOf(rawTagSum + poolMatches, pool.size.coerceAtLeast(rawTagSum)))
            if (totalCount > 0) {
                GenreItem(spec.canonicalName, totalCount)
            } else {
                null
            }
        }

        val coveredNames = SmartGenreCatalog.specs.flatMap {
            it.aliases + it.canonicalName.lowercase(Locale.ROOT)
        }.toSet()

        val remainingServerGenres = raw.filter { g ->
            g.name.trim().lowercase(Locale.ROOT) !in coveredNames && g.songCount > 0
        }

        // Put Smart Genres (sorted by songCount descending) alongside top server genres like Rock
        (smartItems + remainingServerGenres)
            .distinctBy { it.name.lowercase(Locale.ROOT) }
            .sortedByDescending { it.songCount }
    }

    suspend fun search(query: String, artistCount: Int = 10, albumCount: Int = 16, songCount: Int = 40): SearchResult {
        val r = call(
            "search3",
            listOf(
                "query" to query,
                "artistCount" to artistCount.toString(),
                "albumCount" to albumCount.toString(),
                "songCount" to songCount.toString()
            )
        )
        val res = r.optJSONObject("searchResult3")
        return SearchResult(
            artists = res?.arr("artist").orEmpty().map(::artistFrom),
            albums = res?.arr("album").orEmpty().map(::albumFrom),
            tracks = res?.arr("song").orEmpty().map(::songFrom)
        )
    }

    suspend fun starredTracks(): List<TrackItem> {
        val r = call("getStarred2")
        return r.optJSONObject("starred2")?.arr("song").orEmpty().map(::songFrom)
    }

    suspend fun star(id: String) {
        call("star", listOf("id" to id))
    }

    suspend fun unstar(id: String) {
        call("unstar", listOf("id" to id))
    }

    /** submission = false only reports "now playing"; true registers a finished play. */
    suspend fun scrobble(id: String, submission: Boolean = true) {
        call(
            "scrobble",
            listOf("id" to id, "submission" to submission.toString(), "time" to System.currentTimeMillis().toString())
        )
    }

    /**
     * Downloads the first [maxBytes] of a stream URL and reports timing. Used by the diagnostics
     * button to check that (transcoded) streaming really works and how fast the server is.
     */
    suspend fun probeStream(url: String, maxBytes: Int = 262_144): StreamProbe = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val start = System.nanoTime()
            conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 25_000
            val code = conn.responseCode
            if (code !in 200..299) throw SubsonicException(httpMessage(code), code)
            val type = conn.contentType.orEmpty()
            val input = conn.inputStream
            val buf = ByteArray(16_384)
            var total = 0
            var firstByteMs = -1L
            while (total < maxBytes) {
                val n = input.read(buf)
                if (n < 0) break
                if (firstByteMs < 0) firstByteMs = (System.nanoTime() - start) / 1_000_000
                total += n
            }
            val totalMs = (System.nanoTime() - start) / 1_000_000
            StreamProbe(firstByteMs.coerceAtLeast(0), totalMs, total, type)
        } catch (e: SubsonicException) {
            throw e
        } catch (e: IOException) {
            throw SubsonicException("Test streamu zlyhal: ${e.message ?: "chyba siete"}")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun similarSongs(id: String, count: Int = 20): List<TrackItem> {
        val r = call("getSimilarSongs2", listOf("id" to id, "count" to count.toString()))
        return r.optJSONObject("similarSongs2")?.arr("song").orEmpty().map(::songFrom)
    }

    /**
     * Lyrics. Prefers time-synced lyrics (OpenSubsonic getLyricsBySongId); falls back to plain
     * getLyrics. Unsynced lines have startMs = -1.
     */
    suspend fun lyrics(track: TrackItem): List<LyricLine> {
        try {
            val r = call("getLyricsBySongId", listOf("id" to track.id))
            val list = r.optJSONObject("lyricsList")?.arr("structuredLyrics").orEmpty()
            val chosen = list.firstOrNull { it.optBoolean("synced") } ?: list.firstOrNull()
            if (chosen != null) {
                val synced = chosen.optBoolean("synced")
                val lines = chosen.arr("line").mapNotNull { l ->
                    val text = l.optString("value")
                    if (text.isBlank()) null
                    else LyricLine(if (synced) l.optLong("start", 0L) else -1L, text.trim())
                }
                if (lines.isNotEmpty()) return lines
            }
        } catch (e: SubsonicException) {
            // Old servers without OpenSubsonic: fall through to the legacy call.
        }
        return try {
            val r = call("getLyrics", listOf("artist" to track.artist, "title" to track.title))
            val text = r.optJSONObject("lyrics")?.optString("value").orEmpty()
            text.lines().map { it.trim() }.filter { it.isNotBlank() }.map { LyricLine(-1L, it) }
        } catch (e: SubsonicException) {
            emptyList()
        }
    }

    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList()) {
        val params = mutableListOf("name" to name)
        songIds.forEach { params += "songId" to it }
        call("createPlaylist", params)
    }

    suspend fun startScan(): ScanStatus {
        val r = call("startScan")
        return scanStatusFrom(r)
    }

    suspend fun scanStatus(): ScanStatus = scanStatusFrom(call("getScanStatus"))

    private fun scanStatusFrom(root: JSONObject): ScanStatus {
        val s = root.optJSONObject("scanStatus")
        return ScanStatus(s?.optBoolean("scanning") ?: false, s?.optLong("count") ?: 0L)
    }

    // ---------------------------------------------------------------------------------------------
    // Detail pages (always returned as CollectionDetail)
    // ---------------------------------------------------------------------------------------------

    suspend fun albumDetail(rawId: String): CollectionDetail {
        val a = call("getAlbum", listOf("id" to rawId)).optJSONObject("album")
            ?: throw SubsonicException("Album sa nenašiel.")
        val tracks = a.arr("song").map(::songFrom)
        val cover = a.optString("coverArt").ifBlank { "al-$rawId" }
        val year = a.optInt("year")
        val rawArtist = cleanArtistName(a.optString("artist"))
        val subtitle = listOf(rawArtist, if (year > 0) year.toString() else "")
            .filter { it.isNotBlank() }.joinToString(" • ")
        return CollectionDetail(
            id = "al:$rawId",
            kind = "Album",
            title = cleanDisplayTitle(a.optString("name")),
            subtitle = subtitle,
            coverUrl = coverUrl(cover, 800),
            metaLine = metaLine(tracks),
            tracks = tracks
        )
    }

    suspend fun playlistDetail(rawId: String): CollectionDetail {
        val p = call("getPlaylist", listOf("id" to rawId)).optJSONObject("playlist")
            ?: throw SubsonicException("Playlist sa nenašiel.")
        val tracks = p.arr("entry").map(::songFrom)
        val cover = p.optString("coverArt").ifBlank { "pl-$rawId" }
        val owner = p.optString("owner")
        return CollectionDetail(
            id = "pl:$rawId",
            kind = "Playlist",
            title = p.optString("name"),
            subtitle = if (owner.isNotBlank()) "Vytvoril $owner" else p.optString("comment"),
            coverUrl = coverUrl(cover, 800),
            metaLine = metaLine(tracks),
            tracks = tracks
        )
    }

    /** Artist page: lists the artist's albums and songs (up to 20 albums, loaded in parallel). */
    suspend fun artistDetail(rawId: String): CollectionDetail {
        val a = call("getArtist", listOf("id" to rawId)).optJSONObject("artist")
            ?: throw SubsonicException("Interpret sa nenašiel.")
        val albums = a.arr("album")
        val albumItems = albums.map(::albumFrom)
        val tracks = coroutineScope {
            albums.take(60).map { al ->
                async {
                    try {
                        val songs = call("getAlbum", listOf("id" to al.optString("id")))
                            .optJSONObject("album")?.arr("song").orEmpty()
                        songs.map(::songFrom)
                    } catch (e: SubsonicException) {
                        emptyList<TrackItem>()
                    }
                }
            }.awaitAll().flatten()
        }
        val cover = a.optString("coverArt").ifBlank { "ar-$rawId" }
        return CollectionDetail(
            id = "ar:$rawId",
            kind = "Interpret",
            title = a.optString("name"),
            subtitle = "${albums.size} ${plural(albums.size, "album", "albumy", "albumov")}",
            coverUrl = coverUrl(cover, 800),
            metaLine = metaLine(tracks),
            tracks = tracks,
            albums = albumItems
        )
    }

    /**
     * Resolves an artist by name when a track in a mix/queue has an empty or compilation artistId.
     * Searches Navidrome for the artist, loads their full discography (albums + tracks) if found,
     * or builds a collection from all matching albums and songs on the server.
     */
    suspend fun artistDetailByName(rawName: String): CollectionDetail {
        val cleaned = cleanArtistName(rawName).trim()
        val primary = cleaned
            .split(Regex("(?i)\\s+(feat\\.?|ft\\.?|with|vs\\.?)\\s+|[,;/&]"))
            .firstOrNull()?.trim()
            .orEmpty()
            .ifBlank { cleaned }
        val query = primary.ifBlank { rawName.trim() }
        if (query.isBlank() || query.equals("Neznámy interpret", ignoreCase = true)) {
            throw SubsonicException("Interpret nie je pri tejto skladbe uvedený.")
        }

        val sr = search(query, artistCount = 25, albumCount = 50, songCount = 150)
        val matchedArtist = sr.artists.firstOrNull { it.name.equals(cleaned, ignoreCase = true) }
            ?: sr.artists.firstOrNull { it.name.equals(primary, ignoreCase = true) }
            ?: sr.artists.firstOrNull {
                it.name.contains(primary, ignoreCase = true) || primary.contains(it.name, ignoreCase = true)
            }
            ?: sr.artists.firstOrNull()

        if (matchedArtist != null) {
            val rawId = matchedArtist.id.removePrefix("ar:")
            if (rawId.isNotBlank()) {
                try {
                    val detail = artistDetail(rawId)
                    if (detail.tracks.isNotEmpty() || detail.albums.isNotEmpty()) {
                        return detail
                    }
                } catch (_: SubsonicException) {
                    // Fall back to search results below if getArtist fails
                }
            }
        }

        val matchingAlbums = sr.albums.filter {
            it.subtitle.contains(primary, ignoreCase = true) ||
                it.title.contains(primary, ignoreCase = true)
        }.ifEmpty { sr.albums }

        val matchingTracks = sr.tracks.filter {
            it.artist.contains(primary, ignoreCase = true) ||
                it.title.contains(primary, ignoreCase = true)
        }.ifEmpty { sr.tracks }

        if (matchingAlbums.isEmpty() && matchingTracks.isEmpty()) {
            throw SubsonicException("Pre interpreta „$query“ sa na serveri nenašli žiadne skladby ani albumy.")
        }

        val displayTitle = matchedArtist?.name ?: cleaned.ifBlank { query }
        val subtitleText = if (matchingAlbums.isNotEmpty()) {
            "${matchingAlbums.size} ${plural(matchingAlbums.size, "album", "albumy", "albumov")}"
        } else {
            "Skladby interpreta"
        }
        val cover = matchedArtist?.imageUrl?.takeIf { it.isNotBlank() }
            ?: matchingAlbums.firstOrNull()?.imageUrl?.takeIf { it.isNotBlank() }
            ?: matchingTracks.firstOrNull()?.imageUrl.orEmpty()

        return CollectionDetail(
            id = "ar_name:$query",
            kind = "Interpret",
            title = displayTitle,
            subtitle = subtitleText,
            coverUrl = cover,
            metaLine = metaLine(matchingTracks),
            tracks = matchingTracks,
            albums = matchingAlbums
        )
    }

    /**
     * Resolves an album by name when a track in a mix/queue has an empty albumId.
     */
    suspend fun albumDetailByName(rawAlbumName: String): CollectionDetail {
        val cleaned = cleanAlbumName(rawAlbumName).trim().ifBlank { rawAlbumName.trim() }
        if (cleaned.isBlank()) throw SubsonicException("Názov albumu nie je uvedený.")
        val sr = search(cleaned, artistCount = 5, albumCount = 25, songCount = 100)
        val matchedAlbum = sr.albums.firstOrNull { it.title.equals(cleaned, ignoreCase = true) }
            ?: sr.albums.firstOrNull {
                it.title.contains(cleaned, ignoreCase = true) || cleaned.contains(it.title, ignoreCase = true)
            }
            ?: sr.albums.firstOrNull()

        if (matchedAlbum != null) {
            val rawId = matchedAlbum.id.removePrefix("al:")
            if (rawId.isNotBlank()) {
                return albumDetail(rawId)
            }
        }

        val matchingTracks = sr.tracks.filter {
            it.album.equals(cleaned, ignoreCase = true) || it.album.contains(cleaned, ignoreCase = true)
        }.ifEmpty { sr.tracks }

        if (matchingTracks.isEmpty()) {
            throw SubsonicException("Album „$cleaned“ sa na serveri nenašiel.")
        }

        return CollectionDetail(
            id = "al_name:$cleaned",
            kind = "Album",
            title = cleaned,
            subtitle = matchingTracks.firstOrNull()?.artist.orEmpty(),
            coverUrl = matchingTracks.firstOrNull()?.imageUrl.orEmpty(),
            metaLine = metaLine(matchingTracks),
            tracks = matchingTracks
        )
    }

    suspend fun favoritesDetail(): CollectionDetail {
        val tracks = starredTracks()
        return CollectionDetail(
            id = "fav",
            kind = "Zbierka",
            title = "Obľúbené skladby",
            subtitle = "Skladby označené srdiečkom",
            coverUrl = tracks.firstOrNull()?.imageUrl.orEmpty(),
            metaLine = metaLine(tracks),
            tracks = tracks
        )
    }

    suspend fun genreDetail(genre: String, maxTracks: Int = 5000): CollectionDetail = coroutineScope {
        val spec = SmartGenreCatalog.findSpec(genre)
        val serverGenres = try { rawGenres() } catch (_: Exception) { emptyList() }
        val matchingTags = SmartGenreCatalog.matchingServerGenres(genre, serverGenres, spec)
            .distinctBy { it.lowercase(Locale.ROOT) }

        val tracksByTagDeferred = async {
            val allSongs = mutableListOf<TrackItem>()
            for (tag in matchingTags.take(6)) {
                var offset = 0
                val pageSize = 500
                while (offset < maxTracks && allSongs.size < maxTracks) {
                    val page = try {
                        val r = call(
                            "getSongsByGenre",
                            listOf(
                                "genre" to tag,
                                "count" to pageSize.toString(),
                                "offset" to offset.toString()
                            )
                        )
                        r.optJSONObject("songsByGenre")?.arr("song").orEmpty().map(::songFrom)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    allSongs += page
                    if (page.size < pageSize) break
                    offset += pageSize
                }
            }
            allSongs.distinctBy { it.id }
        }

        val albumsByTagDeferred = async {
            val allAlbums = mutableListOf<AlbumItem>()
            for (tag in matchingTags.take(5)) {
                var offset = 0
                val pageSize = 500
                while (offset < 1000 && allAlbums.size < 1500) {
                    val page = try {
                        albumList("byGenre", size = pageSize, offset = offset, genre = tag, allowSmartFallback = false)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    allAlbums += page
                    if (page.size < pageSize) break
                    offset += pageSize
                }
            }
            allAlbums.distinctBy { it.id }
        }

        val smartPoolTracksDeferred = async {
            if (spec != null) {
                try {
                    getOrLoadCatalogPool().filter { SmartGenreCatalog.matchesTrack(it, spec) }
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

        val smartPoolAlbumsDeferred = async {
            if (spec != null) {
                try {
                    getOrLoadAlbumsPool().filter { SmartGenreCatalog.matchesAlbum(it, spec) }
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

        var tracks = (tracksByTagDeferred.await() + smartPoolTracksDeferred.await())
            .distinctBy { it.id }
            .take(maxTracks)
        var apiAlbums = (albumsByTagDeferred.await() + smartPoolAlbumsDeferred.await())
            .distinctBy { it.id }

        // If a custom non-smart genre returned very few tracks from tags, enrich from catalog pool or search
        if (spec == null && tracks.size < 25) {
            try {
                val normGenre = SmartGenreCatalog.normalize(genre)
                val rescued = getOrLoadCatalogPool().filter { t ->
                    SmartGenreCatalog.normalize("${t.artist} ${t.album} ${t.title}").contains(normGenre)
                }
                tracks = (tracks + rescued).distinctBy { it.id }.take(maxTracks)
            } catch (_: Exception) { }
        }

        if (tracks.isEmpty() && apiAlbums.isEmpty()) {
            try {
                val sr = search(spec?.title ?: genre, artistCount = 30, albumCount = 100, songCount = 500)
                tracks = sr.tracks
                apiAlbums = sr.albums
            } catch (_: Exception) {
                // Keep empty
            }
        }

        // Build complete list of albums / movies / games / sources from both getAlbumList2 and the tracks themselves
        val derivedAlbums = tracks
            .groupBy { t ->
                val cleanAlb = cleanAlbumName(t.album).trim()
                val hasValidAlbum = cleanAlb.isNotBlank() &&
                    !cleanAlb.equals("Unknown Album", ignoreCase = true) &&
                    !cleanAlb.equals("Neznámy album", ignoreCase = true) &&
                    !cleanAlb.equals("[Unknown Album]", ignoreCase = true)
                when {
                    t.albumId.isNotBlank() && hasValidAlbum -> "al:${t.albumId}"
                    hasValidAlbum -> "al_group:$cleanAlb"
                    else -> {
                        val cleanArt = cleanArtistName(t.artist).trim()
                            .ifBlank {
                                if (t.title.contains(" - ")) t.title.substringBefore(" - ").trim() else "Rôzne"
                            }
                        "al_group:$cleanArt"
                    }
                }
            }
            .map { (groupKey, groupTracks) ->
                val first = groupTracks.first()
                val cleanAlb = cleanAlbumName(first.album).trim()
                val hasValidAlbum = cleanAlb.isNotBlank() &&
                    !cleanAlb.equals("Unknown Album", ignoreCase = true) &&
                    !cleanAlb.equals("Neznámy album", ignoreCase = true) &&
                    !cleanAlb.equals("[Unknown Album]", ignoreCase = true)
                val displayTitle = if (hasValidAlbum) {
                    cleanAlb
                } else {
                    cleanArtistName(first.artist).trim()
                        .ifBlank {
                            if (first.title.contains(" - ")) first.title.substringBefore(" - ").trim() else "Nezaradené"
                        }
                }
                val artistSubtitle = cleanArtistName(first.artist).trim()
                val sub = listOf(
                    artistSubtitle.takeIf { !it.equals(displayTitle, ignoreCase = true) }.orEmpty(),
                    if (first.year > 0) first.year.toString() else ""
                ).filter { it.isNotBlank() }.joinToString(" • ")

                AlbumItem(
                    id = groupKey,
                    title = displayTitle,
                    subtitle = sub,
                    techSpec = "${groupTracks.size} ${plural(groupTracks.size, "skladba", "skladby", "skladieb")}",
                    badge = first.badge,
                    imageUrl = first.imageUrl
                )
            }

        val existingIds = apiAlbums.map { it.id }.toSet()
        val existingTitles = apiAlbums.map { it.title.lowercase(Locale.ROOT) }.toSet()
        val mergedAlbums = (apiAlbums + derivedAlbums.filter {
            it.id !in existingIds && it.title.lowercase(Locale.ROOT) !in existingTitles
        }).sortedBy { it.title.lowercase(Locale.ROOT) }

        val countPart = if (mergedAlbums.isNotEmpty()) {
            "${mergedAlbums.size} ${plural(mergedAlbums.size, "album / dielo", "albumy / diela", "albumov / diel")} • ${tracks.size} ${plural(tracks.size, "skladba", "skladby", "skladieb")}"
        } else {
            "${tracks.size} ${plural(tracks.size, "skladba", "skladby", "skladieb")}"
        }
        val subtitleText = if (spec != null && spec.subtitle.isNotBlank()) {
            "${spec.subtitle} • $countPart"
        } else {
            countPart
        }

        CollectionDetail(
            id = "ge:$genre",
            kind = "Žáner",
            title = spec?.title ?: genre,
            subtitle = subtitleText,
            coverUrl = mergedAlbums.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl
                ?: tracks.firstOrNull()?.imageUrl.orEmpty(),
            metaLine = metaLine(tracks),
            tracks = tracks,
            albums = mergedAlbums
        )
    }

    // ---------------------------------------------------------------------------------------------
    // JSON -> models
    // ---------------------------------------------------------------------------------------------

    private fun songFrom(o: JSONObject): TrackItem {
        val id = o.optString("id")
        val suffix = o.optString("suffix").lowercase(Locale.ROOT)
        val bitRate = o.optInt("bitRate")
        val bitDepth = o.optInt("bitDepth")
        val samplingRate = o.optInt("samplingRate")
        val durationSec = o.optInt("duration")
        val albumId = o.optString("albumId")
        val lossless = suffix in LOSSLESS_SUFFIXES
        val cover = o.optString("coverArt")
            .ifBlank { if (albumId.isNotBlank()) "al-$albumId" else id }

        val badge = when {
            lossless && bitDepth > 0 -> "${suffix.uppercase(Locale.ROOT)} $bitDepth-BIT"
            lossless -> suffix.uppercase(Locale.ROOT)
            bitRate > 0 -> "$bitRate KBPS"
            else -> suffix.uppercase(Locale.ROOT)
        }
        val techParts = mutableListOf<String>()
        if (suffix.isNotBlank()) techParts += suffix.uppercase(Locale.ROOT)
        if (bitDepth > 0) techParts += "$bitDepth-bit"
        if (samplingRate > 0) techParts += "${formatKhz(samplingRate)} kHz"
        if (bitRate > 0) techParts += "$bitRate kbps"

        val plays = o.optInt("playCount")
        val cleanTitle = cleanDisplayTitle(o.optString("title").ifBlank { "Bez názvu" })
        var cleanArtist = cleanArtistName(o.optString("artist"))
        val cleanAlbum = cleanAlbumName(o.optString("album"))
        if (cleanArtist.isBlank() && cleanTitle.contains(" - ")) {
            val parts = cleanTitle.split(" - ", limit = 2)
            if (parts[0].trim().isNotBlank()) {
                cleanArtist = parts[0].trim()
            }
        }
        val finalArtist = cleanArtist.ifBlank { "Neznámy interpret" }

        return TrackItem(
            id = id,
            title = cleanTitle,
            artist = finalArtist,
            album = cleanAlbum,
            duration = formatDuration(durationSec),
            badge = badge,
            imageUrl = coverUrl(cover, 600),
            playCount = if (plays > 0) "$plays prehratí" else null,
            isLiked = o.has("starred"),
            isHiResBadgeCyan = lossless,
            albumId = albumId,
            artistId = o.optString("artistId"),
            durationSec = durationSec,
            suffix = suffix,
            bitRate = bitRate,
            bitDepth = bitDepth,
            samplingRate = samplingRate,
            year = o.optInt("year"),
            coverArtId = cover,
            techLine = techParts.joinToString(" • ")
        )
    }

    private fun albumFrom(o: JSONObject): AlbumItem {
        val id = o.optString("id")
        val cover = o.optString("coverArt").ifBlank { "al-$id" }
        val year = o.optInt("year")
        val songs = o.optInt("songCount")
        val dur = o.optInt("duration")
        val rawArtist = cleanArtistName(o.optString("artist"))
        val subtitle = listOf(rawArtist, if (year > 0) year.toString() else "")
            .filter { it.isNotBlank() }.joinToString(" • ")
        val spec = buildString {
            if (songs > 0) append("$songs ${plural(songs, "skladba", "skladby", "skladieb")}")
            if (dur > 0) {
                if (isNotEmpty()) append(" • ")
                append(formatTotal(dur))
            }
        }
        return AlbumItem(
            id = "al:$id",
            title = cleanDisplayTitle(o.optString("name").ifBlank { o.optString("title") }.ifBlank { "Bez názvu" }),
            subtitle = subtitle,
            techSpec = spec,
            badge = "",
            imageUrl = coverUrl(cover, 500)
        )
    }

    private fun artistFrom(o: JSONObject): ArtistItem {
        val id = o.optString("id")
        val albums = o.optInt("albumCount")
        val cover = o.optString("coverArt").ifBlank { "ar-$id" }
        return ArtistItem(
            id = "ar:$id",
            name = o.optString("name").ifBlank { "Neznámy interpret" },
            trackCount = if (albums > 0) "$albums ${plural(albums, "album", "albumy", "albumov")}" else "",
            imageUrl = coverUrl(cover, 500)
        )
    }

    private fun playlistFrom(o: JSONObject): PlaylistItem {
        val id = o.optString("id")
        val songs = o.optInt("songCount")
        val dur = o.optInt("duration")
        val cover = o.optString("coverArt").ifBlank { "pl-$id" }
        val owner = o.optString("owner")
        return PlaylistItem(
            id = "pl:$id",
            title = o.optString("name").ifBlank { "Bez názvu" },
            subtitle = if (owner.isNotBlank()) owner else o.optString("comment"),
            trackCount = "$songs ${plural(songs, "skladba", "skladby", "skladieb")}",
            durationOrExtra = if (dur > 0) formatTotal(dur) else "",
            imageUrl = coverUrl(cover, 400)
        )
    }

    private fun metaLine(tracks: List<TrackItem>): String {
        if (tracks.isEmpty()) return "Žiadne skladby"
        val total = tracks.sumOf { it.durationSec }
        val count = "${tracks.size} ${plural(tracks.size, "skladba", "skladby", "skladieb")}"
        return if (total > 0) "$count • ${formatTotal(total)}" else count
    }

    companion object {
        const val API_VERSION = "1.16.1"
        const val CLIENT_NAME = "SoundWave"

        private val LOSSLESS_SUFFIXES = setOf("flac", "alac", "wav", "aiff", "aif", "ape", "wv", "dsf", "dff")

        /** "192.168.1.5:4533/" -> "http://192.168.1.5:4533". Keeps https:// if the user typed it. */
        fun normalizeUrl(input: String): String {
            var s = input.trim()
            if (s.isEmpty()) return ""
            if (!s.startsWith("http://", ignoreCase = true) && !s.startsWith("https://", ignoreCase = true)) {
                s = "http://$s"
            }
            return s.trimEnd('/')
        }

        fun md5(input: String): String {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun newSalt(): String {
            val bytes = ByteArray(8)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        /** Computes the token for the salted-token auth scheme. */
        fun tokenFor(password: String, salt: String): String = md5(password + salt)
    }
}

// -------------------------------------------------------------------------------------------------
// Helpers shared with the UI / ViewModel
// -------------------------------------------------------------------------------------------------

/** The Subsonic JSON uses either an array or (rarely) a single object, or omits the key. */
private fun JSONObject.arr(key: String): List<JSONObject> {
    return when (val v = opt(key)) {
        is JSONArray -> (0 until v.length()).mapNotNull { v.optJSONObject(it) }
        is JSONObject -> listOf(v)
        else -> emptyList()
    }
}

fun formatDuration(totalSec: Int): String {
    if (totalSec <= 0) return "0:00"
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatMs(ms: Long): String = formatDuration((ms / 1000L).toInt())

/** "3 h 12 min" / "47 min" */
fun formatTotal(totalSec: Int): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return when {
        h > 0 -> "$h h $m min"
        m > 0 -> "$m min"
        else -> "$totalSec s"
    }
}

private fun formatKhz(samplingRate: Int): String {
    return if (samplingRate % 1000 == 0) (samplingRate / 1000).toString()
    else String.format(Locale.US, "%.1f", samplingRate / 1000.0)
}

/** Slovak plural: 1 skladba, 2-4 skladby, 5+ skladieb. */
fun plural(n: Int, one: String, few: String, many: String): String = when {
    n == 1 -> one
    n in 2..4 -> few
    else -> many
}

/** Albums and artists can be shown in the same list rows as playlists. */
fun AlbumItem.toRow(): PlaylistItem = PlaylistItem(
    id = id,
    title = title,
    subtitle = subtitle,
    trackCount = techSpec,
    imageUrl = imageUrl
)

fun ArtistItem.toRow(): PlaylistItem = PlaylistItem(
    id = id,
    title = name,
    subtitle = "Interpret",
    trackCount = trackCount,
    imageUrl = imageUrl
)
