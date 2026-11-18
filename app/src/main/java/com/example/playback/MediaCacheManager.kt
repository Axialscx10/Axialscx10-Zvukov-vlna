package com.example.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages a single shared Media3 SimpleCache (256 MB LRU) + resilient OkHttp upstream client
 * for ExoPlayer audio streaming, preventing cache lock issues and surviving brief mobile dropouts.
 */
@OptIn(UnstableApi::class)
object MediaCacheManager {
    private const val MAX_CACHE_BYTES = 256L * 1024L * 1024L
    private const val CACHE_DIR_NAME = "media_stream_cache"

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    private var okHttpClient: OkHttpClient? = null

    fun getOkHttpClient(): OkHttpClient {
        okHttpClient?.let { return it }
        return synchronized(this) {
            okHttpClient ?: OkHttpClient.Builder()
                .connectionPool(ConnectionPool(4, 2, TimeUnit.MINUTES))
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
                .also { okHttpClient = it }
        }
    }

    fun getSimpleCache(context: Context): SimpleCache {
        simpleCache?.let { return it }
        return synchronized(this) {
            simpleCache ?: run {
                val appCtx = context.applicationContext
                val dir = File(appCtx.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
                val dbProvider = StandaloneDatabaseProvider(appCtx)
                SimpleCache(dir, LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES), dbProvider)
                    .also { simpleCache = it }
            }
        }
    }

    fun buildDataSourceFactory(context: Context): DataSource.Factory {
        val httpFactory = OkHttpDataSource.Factory(getOkHttpClient())
            .setUserAgent("SoundWave/32.0 (Android; ExoPlayer)")
        val upstreamFactory = DefaultDataSource.Factory(context.applicationContext, httpFactory)
        return CacheDataSource.Factory()
            .setCache(getSimpleCache(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Clears all non-locked cache directories and safely removes resources from live SimpleCache. */
    fun clearAllCaches(context: Context) {
        val appCtx = context.applicationContext
        val cache = simpleCache
        if (cache != null) {
            try {
                cache.keys.toList().forEach { key ->
                    try {
                        cache.removeResource(key)
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}
        }
        try {
            appCtx.cacheDir.listFiles()?.forEach { child ->
                if (child.name != CACHE_DIR_NAME || cache == null) {
                    child.deleteRecursively()
                }
            }
        } catch (_: Throwable) {}
    }
}
