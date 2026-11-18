package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CachedHomeSnapshot(
    val quickPicks: List<TrackItem>,
    val listenAgain: List<AlbumItem>,
    val newest: List<AlbumItem>,
    val genres: List<GenreItem>,
    val playlists: List<PlaylistItem>
)

data class CachedLibrarySnapshot(
    val playlists: List<PlaylistItem>,
    val artists: List<ArtistItem>,
    val albums: List<AlbumItem>,
    val favorites: List<TrackItem>,
    val serverSongCount: Long
)

data class CachedExploreSnapshot(
    val artists: List<ArtistItem>,
    val albums: List<AlbumItem>,
    val tracks: List<TrackItem>,
    val genres: List<GenreItem>
)

/**
 * Lightweight disk JSON cache for Home, Library, and Explore catalogs.
 * Makes Android TV & mobile startup instantaneous (especially over Cloudflare tunnels)
 * without ever persisting Subsonic authentication tokens to disk.
 */
class CatalogCacheStore(context: Context) {

    private val cacheDir = File(context.applicationContext.filesDir, "catalog_cache").apply { mkdirs() }

    private fun accountTag(serverUrl: String, username: String): String =
        SubsonicApi.md5("${serverUrl.trim()}|${username.trim()}").take(16)

    private fun fileFor(serverUrl: String, username: String, section: String): File =
        File(cacheDir, "${accountTag(serverUrl, username)}_$section.json")

    fun saveHome(serverUrl: String, username: String, snap: CachedHomeSnapshot) {
        if (serverUrl.isBlank() || username.isBlank()) return
        try {
            val root = JSONObject().apply {
                put("quickPicks", JSONArray().apply { snap.quickPicks.forEach { put(trackToJson(it)) } })
                put("listenAgain", JSONArray().apply { snap.listenAgain.forEach { put(albumToJson(it)) } })
                put("newest", JSONArray().apply { snap.newest.forEach { put(albumToJson(it)) } })
                put("genres", JSONArray().apply { snap.genres.forEach { put(genreToJson(it)) } })
                put("playlists", JSONArray().apply { snap.playlists.forEach { put(playlistToJson(it)) } })
            }
            writeAtomic(fileFor(serverUrl, username, "home"), root.toString())
        } catch (_: Exception) {}
    }

    fun loadHome(api: SubsonicApi): CachedHomeSnapshot? {
        val f = fileFor(api.baseUrl, api.username, "home")
        if (!f.exists()) return null
        return try {
            val root = JSONObject(f.readText(Charsets.UTF_8))
            val quickPicks = parseArray(root.optJSONArray("quickPicks")) { trackFromJson(it, api) }
            val listenAgain = parseArray(root.optJSONArray("listenAgain")) { albumFromJson(it, api) }
            val newest = parseArray(root.optJSONArray("newest")) { albumFromJson(it, api) }
            val genres = parseArray(root.optJSONArray("genres")) { genreFromJson(it) }
            val playlists = parseArray(root.optJSONArray("playlists")) { playlistFromJson(it, api) }
            if (quickPicks.isEmpty() && listenAgain.isEmpty() && newest.isEmpty()) null
            else CachedHomeSnapshot(quickPicks, listenAgain, newest, genres, playlists)
        } catch (_: Exception) {
            null
        }
    }

    fun saveLibrary(serverUrl: String, username: String, snap: CachedLibrarySnapshot) {
        if (serverUrl.isBlank() || username.isBlank()) return
        try {
            val root = JSONObject().apply {
                put("playlists", JSONArray().apply { snap.playlists.forEach { put(playlistToJson(it)) } })
                put("artists", JSONArray().apply { snap.artists.forEach { put(artistToJson(it)) } })
                put("albums", JSONArray().apply { snap.albums.forEach { put(albumToJson(it)) } })
                put("favorites", JSONArray().apply { snap.favorites.forEach { put(trackToJson(it)) } })
                put("serverSongCount", snap.serverSongCount)
            }
            writeAtomic(fileFor(serverUrl, username, "library"), root.toString())
        } catch (_: Exception) {}
    }

    fun loadLibrary(api: SubsonicApi): CachedLibrarySnapshot? {
        val f = fileFor(api.baseUrl, api.username, "library")
        if (!f.exists()) return null
        return try {
            val root = JSONObject(f.readText(Charsets.UTF_8))
            val playlists = parseArray(root.optJSONArray("playlists")) { playlistFromJson(it, api) }
            val artists = parseArray(root.optJSONArray("artists")) { artistFromJson(it, api) }
            val albums = parseArray(root.optJSONArray("albums")) { albumFromJson(it, api) }
            val favorites = parseArray(root.optJSONArray("favorites")) { trackFromJson(it, api) }
            val count = root.optLong("serverSongCount", 0L)
            if (playlists.isEmpty() && artists.isEmpty() && albums.isEmpty() && favorites.isEmpty()) null
            else CachedLibrarySnapshot(playlists, artists, albums, favorites, count)
        } catch (_: Exception) {
            null
        }
    }

    fun saveExplore(serverUrl: String, username: String, snap: CachedExploreSnapshot) {
        if (serverUrl.isBlank() || username.isBlank()) return
        try {
            val root = JSONObject().apply {
                put("artists", JSONArray().apply { snap.artists.forEach { put(artistToJson(it)) } })
                put("albums", JSONArray().apply { snap.albums.forEach { put(albumToJson(it)) } })
                put("tracks", JSONArray().apply { snap.tracks.forEach { put(trackToJson(it)) } })
                put("genres", JSONArray().apply { snap.genres.forEach { put(genreToJson(it)) } })
            }
            writeAtomic(fileFor(serverUrl, username, "explore"), root.toString())
        } catch (_: Exception) {}
    }

    fun loadExplore(api: SubsonicApi): CachedExploreSnapshot? {
        val f = fileFor(api.baseUrl, api.username, "explore")
        if (!f.exists()) return null
        return try {
            val root = JSONObject(f.readText(Charsets.UTF_8))
            val artists = parseArray(root.optJSONArray("artists")) { artistFromJson(it, api) }
            val albums = parseArray(root.optJSONArray("albums")) { albumFromJson(it, api) }
            val tracks = parseArray(root.optJSONArray("tracks")) { trackFromJson(it, api) }
            val genres = parseArray(root.optJSONArray("genres")) { genreFromJson(it) }
            if (artists.isEmpty() && albums.isEmpty() && tracks.isEmpty()) null
            else CachedExploreSnapshot(artists, albums, tracks, genres)
        } catch (_: Exception) {
            null
        }
    }

    fun clearAll() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    private fun writeAtomic(target: File, content: String) {
        val tmp = File(cacheDir, "${target.name}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }

    private inline fun <T> parseArray(arr: JSONArray?, mapper: (JSONObject) -> T): List<T> {
        if (arr == null) return emptyList()
        val list = ArrayList<T>(arr.length())
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { list.add(mapper(it)) }
        }
        return list
    }

    private fun trackToJson(t: TrackItem): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("artist", t.artist)
        put("album", t.album)
        put("duration", t.duration)
        put("badge", t.badge)
        put("playCount", t.playCount ?: "")
        put("isLiked", t.isLiked)
        put("isHiResBadgeCyan", t.isHiResBadgeCyan)
        put("albumId", t.albumId)
        put("artistId", t.artistId)
        put("durationSec", t.durationSec)
        put("suffix", t.suffix)
        put("bitRate", t.bitRate)
        put("bitDepth", t.bitDepth)
        put("samplingRate", t.samplingRate)
        put("year", t.year)
        put("coverArtId", t.coverArtId)
        put("techLine", t.techLine)
        if (t.replayGainTrackDb != null) put("rgTrack", t.replayGainTrackDb.toDouble())
        if (t.replayGainAlbumDb != null) put("rgAlbum", t.replayGainAlbumDb.toDouble())
    }

    private fun trackFromJson(o: JSONObject, api: SubsonicApi): TrackItem {
        val coverId = o.optString("coverArtId", "")
        return TrackItem(
            id = o.optString("id", ""),
            title = o.optString("title", ""),
            artist = o.optString("artist", ""),
            album = o.optString("album", ""),
            duration = o.optString("duration", "0:00"),
            badge = o.optString("badge", ""),
            imageUrl = api.resolveCoverUrl(coverId, "", 360),
            playCount = o.optString("playCount", "").takeIf { it.isNotBlank() },
            isLiked = o.optBoolean("isLiked", false),
            isHiResBadgeCyan = o.optBoolean("isHiResBadgeCyan", true),
            albumId = o.optString("albumId", ""),
            artistId = o.optString("artistId", ""),
            durationSec = o.optInt("durationSec", 0),
            suffix = o.optString("suffix", ""),
            bitRate = o.optInt("bitRate", 0),
            bitDepth = o.optInt("bitDepth", 0),
            samplingRate = o.optInt("samplingRate", 0),
            year = o.optInt("year", 0),
            coverArtId = coverId,
            techLine = o.optString("techLine", ""),
            replayGainTrackDb = if (o.has("rgTrack")) o.optDouble("rgTrack").takeIf { !it.isNaN() }?.toFloat() else null,
            replayGainAlbumDb = if (o.has("rgAlbum")) o.optDouble("rgAlbum").takeIf { !it.isNaN() }?.toFloat() else null
        )
    }

    private fun albumToJson(a: AlbumItem): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("title", a.title)
        put("subtitle", a.subtitle)
        put("techSpec", a.techSpec)
        put("badge", a.badge)
        put("coverArtId", a.coverArtId.ifBlank { a.id.removePrefix("al:").let { if (it.isNotBlank()) "al-$it" else "" } })
    }

    private fun albumFromJson(o: JSONObject, api: SubsonicApi): AlbumItem {
        val coverId = o.optString("coverArtId", "")
        return AlbumItem(
            id = o.optString("id", ""),
            title = o.optString("title", ""),
            subtitle = o.optString("subtitle", ""),
            techSpec = o.optString("techSpec", ""),
            badge = o.optString("badge", ""),
            imageUrl = api.resolveCoverUrl(coverId, "", 240),
            coverArtId = coverId
        )
    }

    private fun artistToJson(a: ArtistItem): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("name", a.name)
        put("trackCount", a.trackCount)
        put("coverArtId", a.coverArtId.ifBlank { a.id.removePrefix("ar:").let { if (it.isNotBlank()) "ar-$it" else "" } })
    }

    private fun artistFromJson(o: JSONObject, api: SubsonicApi): ArtistItem {
        val coverId = o.optString("coverArtId", "")
        return ArtistItem(
            id = o.optString("id", ""),
            name = o.optString("name", ""),
            trackCount = o.optString("trackCount", ""),
            imageUrl = api.resolveCoverUrl(coverId, "", 240),
            coverArtId = coverId
        )
    }

    private fun playlistToJson(p: PlaylistItem): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("title", p.title)
        put("subtitle", p.subtitle)
        put("trackCount", p.trackCount)
        put("durationOrExtra", p.durationOrExtra)
        put("badge", p.badge)
        put("coverArtId", p.coverArtId.ifBlank { p.id.removePrefix("pl:").let { if (it.isNotBlank()) "pl-$it" else "" } })
    }

    private fun playlistFromJson(o: JSONObject, api: SubsonicApi): PlaylistItem {
        val coverId = o.optString("coverArtId", "")
        return PlaylistItem(
            id = o.optString("id", ""),
            title = o.optString("title", ""),
            subtitle = o.optString("subtitle", ""),
            trackCount = o.optString("trackCount", ""),
            durationOrExtra = o.optString("durationOrExtra", ""),
            badge = o.optString("badge", ""),
            imageUrl = api.resolveCoverUrl(coverId, "", 240),
            coverArtId = coverId
        )
    }

    private fun genreToJson(g: GenreItem): JSONObject = JSONObject().apply {
        put("name", g.name)
        put("songCount", g.songCount)
    }

    private fun genreFromJson(o: JSONObject): GenreItem = GenreItem(
        name = o.optString("name", ""),
        songCount = o.optInt("songCount", 0)
    )
}
