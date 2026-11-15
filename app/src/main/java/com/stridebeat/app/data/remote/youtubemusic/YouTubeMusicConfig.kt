package com.stridebeat.app.data.remote.youtubemusic

/**
 * Konfigurasi & Feature Flag integrasi YouTube Music (tidak resmi).
 * Sesuai PRD §6.3:
 * Integrasi ini bersifat eksperimental / beta dan HARUS bisa dinonaktifkan
 * tanpa memengaruhi kestabilan pemutar musik utama (Spotify).
 */
object YouTubeMusicConfig {
    /**
     * Feature flag kontrol aktivasi YouTube Music.
     * Jika false, UI hanya menampilkan Spotify sebagai sumber aktif.
     */
    var isYouTubeMusicEnabled: Boolean = true

    const val TAG = "YouTubeMusicSource"
    const val DOCS_REFERENCE = "docs/YOUTUBE_MUSIC.md"
}
