package com.stridebeat.app.data.remote.youtubemusic

/**
 * Konfigurasi & Feature Flag integrasi YouTube Music (tidak resmi).
 * Sesuai PRD §6.3:
 * Integrasi ini bersifat eksperimental / beta dan HARUS bisa dinonaktifkan
 * tanpa memengaruhi kestabilan pemutar musik SoundCloud / fallback latihan.
 */
object YouTubeMusicConfig {
    /**
     * Feature flag kontrol aktivasi YouTube Music dalam memori runtime.
     * Default true, disinkronkan dengan MusicPreferences.
     */
    var isYouTubeMusicEnabled: Boolean = true

    const val TAG = "YouTubeMusicSource"
    const val DOCS_REFERENCE = "docs/YOUTUBE_MUSIC.md"
}
