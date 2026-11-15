# Dokumentasi Integrasi YouTube Music — StrideBeat

**Status:** Eksperimental / Beta / Tidak Resmi  
**Feature Flag:** `YouTubeMusicConfig.isYouTubeMusicEnabled`  
**Referensi:** PRD §6.3  

---

## 1. Latar Belakang & Masalah Teknis

YouTube Music **tidak memiliki API publik resmi** untuk konsumsi library pengguna, pencarian audio-only track, liked songs, dan remote playback setara Spotify Web API & Spotify Android SDK.

API resmi yang disediakan Google, yaitu **YouTube Data API v3**, dirancang khusus untuk video (kuota terbatas, tidak mendukung streaming audio latar belakang native tanpa video embed, dan tidak memiliki endpoint library YouTube Music).

Oleh karena itu, integrasi YouTube Music pada StrideBeat menggunakan pendekatan **pihak ketiga / tidak resmi** yang berisiko mengalami perubahan sepihak sewaktu-waktu dari Google/YouTube.

---

## 2. Batasan & Prinsip Keamanan Wajib

1. **Jangan Meminta Password Google Pengguna:**
   Aplikasi **TIDAK PERNAH** meminta username dan password Google di form buatan sendiri. Otorisasi dilakukan via OAuth 2.0 resmi Google atau via sesi web resmi.
2. **Tanpa Bypass DRM & Tanpa Adblocker di Aplikasi:**
   Sesuai prinsip PRD §1.1, StrideBeat adalah *hub / switcher* dan recap manager, bukan pembobol DRM atau ad-blocker.
3. **Penyajian Jujur di UI:**
   Di UI Pengaturan dan Pemutar Musik, YouTube Music selalu diberi label **"Beta / Tidak Resmi"**.
4. **Isolasi Arsitektur (MusicSource Layer):**
   Semua pemanggilan YouTube Music dibungkus dalam antarmuka `MusicRepository` dan `MusicSource`. Jika terjadi galat (error HTTP 403, 401, perubahan skema response), aplikasi tidak boleh crash dan otomatis fallback ke Spotify atau mode latihan tanpa musik.

---

## 3. Pendekatan Integrasi & Library Komunitas

Integrasi ini mengacu pada pola yang dipakai komunitas open-source:
- **`ytmusicapi` (Python / Kotlin port):** Meniru request HTTP headers dari YouTube Music Web Client (InnerTube API client endpoints seperti `/youtubei/v1/browse`, `/youtubei/v1/search`, `/youtubei/v1/next`).
- **BitChord / NewPipe Extractor / Innertube:** Mengurai payload metadata lagu (judul, artis, album, thumbnail URL, ISRC jika ada) untuk normalisasi ke model `Track` StrideBeat.

---

## 4. Cara Menonaktifkan Integrasi (Kill Switch)

Jika Google mengubah endpoint InnerTube atau integrasi ini bermasalah:

1. **Secara Kode (Runtime/Config):**
   Ubah nilai di `com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicConfig`:
   ```kotlin
   YouTubeMusicConfig.isYouTubeMusicEnabled = false
   ```
2. **Dampak Penonaktifan:**
   - Opsi YouTube Music di layar Workout dan Pengaturan akan otomatis disembunyikan / dinonaktifkan.
   - Sesi olahraga yang sedang berlangsung atau sebelumnya tetap utuh.
   - Spotify tetap berfungsi normal sebagai sumber musik utama yang stabil.
