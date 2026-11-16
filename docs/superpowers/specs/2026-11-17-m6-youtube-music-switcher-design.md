# Desain Milestone M6: YouTube Music (Beta) + Music Switcher Utuh

**Tanggal:** 2026-11-17  
**Target Rilis:** `v0.2.0` (`versionCode = 16`)  
**Cakupan:** Strictly **Milestone M6** (PRD Phase 2 §6.2–§6.6, §7.2 US-15–US-18, §8.3, §9, §11.3). Tidak mencakup M7 (pace-BPM / audio cue) maupun M8 (splits / PB / trends / image share).

---

## 1. Tujuan & Alasan (Why)
1. **Perbaiki realitas sumber musik di v0.1.2:** Saat ini `OnlineMusicSearchEngine` mencari dan memutar audio dari **SoundCloud**, tetapi `MusicSource` belum memiliki enum `SOUNDCLOUD` sehingga lagu SoundCloud dilabeli `YOUTUBE_MUSIC` / `SPOTIFY`.
2. **Aktifkan YouTube Music (Beta / Tidak Resmi):** Memberi opsi sumber musik kedua secara nyata di Android (InnerTube web-client API + feature flag `youtubeMusicEnabled` + label jujur `"Beta / Tidak Resmi"`).
3. **Bangun Music Switcher & Playlist Netral Utuh:** Memungkinkan pelari berpindah sumber musik di tengah workout (**SoundCloud ↔ YouTube Music**) tanpa me-reset timer/jarak/rute, mencocokkan lagu lintas platform via ISRC / fallback judul+artis+durasi (±2 detik), dan melewati lagu yang tidak tersedia di platform aktif secara jujur.
4. **Penghapusan Spotify dari UI (Keputusan Pemilik):** Pemilik menggunakan akun Spotify non-Premium dan meminta Spotify dihapus dari tampilan UI agar tidak ada tombol/kartu yang tidak berguna. Enum `MusicSource.SPOTIFY` dan helper internal PKCE tetap dipertahankan di kode domain/data hanya untuk kompatibilitas data sesi lama dan kemudahan re-aktivasi bila suatu saat dibutuhkan, sementara **Music Switcher** dan **Pengaturan → Akun Musik** hanya menampilkan **SoundCloud** dan **YouTube Music (Beta)**.

---

## 2. Non-Goals & Anti-Goals
- **Non-Goals:**
  - Tidak membuat backend perantara (BFF) untuk YouTube Music (PRD §6.3 melarang BFF tanpa persetujuan pemilik).
  - Tidak menampilkan kartu akun atau tab switcher Spotify di UI (sesuai instruksi langsung pemilik).
  - Tidak mengerjakan fitur M7 (`bpm` pace-matching, TTS audio cue) atau M8 (splits, PB, image share).
- **Anti-Goals:**
  - **Dilarang** meminta atau menyimpan password Google pengguna untuk YouTube Music.
  - **Dilarang** melakukan bypass DRM / ad-blocker.
  - **Dilarang** merusak playback SoundCloud yang sudah berjalan (full-track >45s via Media3 ExoPlayer, Media Notification, Lock Screen).
  - **Dilarang** me-reset `TrackingService` / timer / jarak / rute saat sumber musik diganti di tengah workout.

---

## 3. Pendekatan yang Dipertimbangkan & Dipilih (Approaches)

### Opsi A — Arsitektur Multi-Source Terisolasi Native Kotlin (Dipilih)
- Pisahkan sumber aktif (`OnlineMusicSearchEngine` untuk SoundCloud dan `YouTubeMusicClient` untuk YouTube Music InnerTube) di bawah `MusicRepositoryImpl`.
- YouTube Music dikontrol feature flag DataStore (`youtubeMusicEnabled`), dengan fallback aman ke pesan ramah / SoundCloud bila endpoint gagal.
- Hapus UI Spotify yang tidak terpakai di `SettingsScreen` dan `MusicPlayerBottomSheet` sesuai keputusan pemilik.
- **Kelebihan:** Zero backend, UI bersih tanpa tombol mati, bisa dimatikan instan via feature flag, SoundCloud tetap menjadi jangkar stabil.
- **Risiko:** Resolusi stream langsung YouTube Music InnerTube bersifat tidak resmi dan dapat ditolak YouTube (403 / `signatureCipher`) sewaktu-waktu — dimitigasi dengan pesan UI jujur + skip/fallback aman + tombol buka di app YouTube Music eksternal.

### Opsi B — Tambah BFF Server untuk `ytmusicapi` + Spotify App Remote `.aar` (Ditolak)
- Membutuhkan hosting backend terpisah (ditolak pemilik & melanggar batas §6.3) serta SHA-1 & `.aar` Spotify Dashboard manual (pemilik tidak memakai Spotify Premium).

---

## 4. Desain Teknis Rinci

### 4.1 Model Domain & Warna Brand
- [`MusicSource`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/domain/model/MusicSource.kt):
  ```kotlin
  enum class MusicSource { SOUNDCLOUD, SPOTIFY, YOUTUBE_MUSIC }
  ```
  - Warna brand di UI (`Color.kt`, PRD §9):
    - `SOUNDCLOUD` → Oranye (`SoundCloudBrand = Color(0xFFFF5500)`) — dibedakan jelas dari merah YouTube Music.
    - `SPOTIFY` → Hijau (`SpotifyBrand = Color(0xFF1DB954)`) *(tetap ada untuk kompatibilitas history/recap lama)*.
    - `YOUTUBE_MUSIC` → Merah (`YouTubeBrand = Color(0xFFFF3B30)`).
  - Default `activeSource`: `MusicSource.SOUNDCLOUD` (menggantikan `YOUTUBE_MUSIC` palsu sebelumnya).
- [`Track`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/domain/model/Track.kt):
  - Tetap netral platform dengan `platformIds: Map<MusicSource, String>`, `isrc: String?`, `title`, `artist`, `album`, `durationMs`, `coverUrl`, `audioUrl`.
  - Setiap hasil search SoundCloud hanya mengisi `MusicSource.SOUNDCLOUD to id` (menghapus bug lama yang mengisi `SPOTIFY` & `YOUTUBE_MUSIC`).
- [`WorkoutPlaylist`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/domain/model/WorkoutPlaylist.kt):
  - Playlist workout netral berisi daftar `Track` lintas platform (`source: MusicSource? = null`).
- [`Converters.kt`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/data/local/room/converter/Converters.kt) & [`WorkoutRepositoryImpl.kt`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/data/repository/WorkoutRepositoryImpl.kt):
  - Pastikan deserialisasi `MusicSource.valueOf(...)` aman untuk enum `SOUNDCLOUD`, `SPOTIFY`, `YOUTUBE_MUSIC`.

### 4.2 Pencocokan Lagu Lintas Platform (`TrackMatchingEngine`)
- Perbarui [`TrackMatchingEngine`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/util/TrackMatchingEngine.kt):
  1. Kunci utama: **ISRC** cocok (case-insensitive, non-blank).
  2. Fallback: normalisasi `title` + `artist` (hapus teks bracket `(feat. ...)`, `[Official Audio]`, tanda baca) + toleransi durasi **±2 detik (`2000L` ms)** sesuai PRD §6.5.
  3. Tambah helper `findBestMatch(target: Track, candidates: List<Track>): Track?` dan `remapLegacySource(trackId: String, source: MusicSource): MusicSource`.
  4. Perbarui unit test [`TrackMatchingTest.kt`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/test/java/com/stridebeat/app/TrackMatchingTest.kt) agar menguji batas ±2 detik dan enum `SOUNDCLOUD`.

### 4.3 Integrasi Sumber Musik

#### A. SoundCloud (Stabil & Fallback Utama)
- Pertahankan [`OnlineMusicSearchEngine`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/data/remote/music/OnlineMusicSearchEngine.kt).
- Mendukung `BuildConfig.SOUNDCLOUD_CLIENT_ID` dari `local.properties` / environment dengan fallback nilai existing v0.1.2 agar tidak regresi.
- Filter durasi `> 45 detik`, resolve HLS/progressive MP3, play via [`MusicPlaybackService`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/service/MusicPlaybackService.kt).

#### B. YouTube Music (Beta / Tidak Resmi — Feature-Flagged)
- **Preferensi & Feature Flag (`MusicPreferences` DataStore):**
  - Simpan `youtubeMusicEnabled: Boolean` (default `true` sesuai pilihan pemilik), tersinkronisasi dengan `YouTubeMusicConfig.isYouTubeMusicEnabled`.
  - Dapat dimatikan/dinyalakan dari **Pengaturan → Akun Musik → YouTube Music (Beta / Tidak Resmi)** tanpa me-restart app.
- **Autentikasi Opsional (`YouTubeMusicAuthManager` + `SecureTokenStorage`):**
  - **Tanpa form password Google.**
  - Jika `YOUTUBE_MUSIC_OAUTH_CLIENT_ID` terkonfigurasi: luncurkan Google OAuth 2.0 PKCE (`https://accounts.google.com/o/oauth2/v2/auth`, scope `https://www.googleapis.com/auth/youtube.readonly`, redirect `stridebeat://ytmusic-callback`) untuk menampilkan nama akun terhubung (`YouTubeMusicAuthState.Connected`) & simpan token terenkripsi di `SecureTokenStorage` AES-256 GCM.
  - Jika `YOUTUBE_MUSIC_OAUTH_CLIENT_ID` kosong: jangan crash dan jangan blokir pencarian publik — tampilkan status jujur *"Aktif — Mode Pencarian Publik (Beta)"*.
  - Error auth selalu ditampilkan dalam bahasa biasa: *"YouTube Music sedang tidak bisa dihubungkan. Coba lagi nanti."*
- **Search & Playback Client (`YouTubeMusicClient`):**
  - Search katalog YouTube Music via `POST https://music.youtube.com/youtubei/v1/search` (`clientName = "WEB_REMIX"`, params filter lagu `EgWKAQIIAWoMEAMQBBAJEAoQBRAV`), parse `videoId`, `title`, `artist`, `album`, `durationMs`, `thumbnailUrl` → model `Track` dengan `platformIds = mapOf(MusicSource.YOUTUBE_MUSIC to videoId)`.
  - Saat track YT Music diputar: resolve direct audio stream via `POST https://music.youtube.com/youtubei/v1/player` (`ANDROID_MUSIC` / `IOS` fallback client) untuk mengambil `adaptiveFormats` audio-only dengan `url` langsung (tanpa `signatureCipher`, tanpa bypass DRM).
  - Jika `url` audio langsung didapat → putar native via `MusicPlaybackService` (Media3 ExoPlayer) + catat ke `playedTracksInSession` dengan `source = MusicSource.YOUTUBE_MUSIC`.
  - Jika YouTube menolak / hanya memberi `signatureCipher` / offline → **tidak crash & tidak pura-pura memutar**: tampilkan status jelas *"Audio YouTube Music tidak tersedia langsung untuk lagu ini"* di bottom sheet, sediakan aksi cepat **"Putar di SoundCloud"** atau **"Buka di YouTube Music"**, atau lanjut otomatis saat mode playlist jika tidak tersedia.

#### C. Spotify (Dihapus dari UI sesuai Keputusan Pemilik)
- Kartu Spotify di `SettingsScreen` dan pill Spotify di `MusicPlayerBottomSheet` **dihapus dari UI** atas permintaan pemilik (akun non-Premium, tidak dipakai).
- `SpotifyAuthManager` & enum `MusicSource.SPOTIFY` tetap aman secara internal untuk kompatibilitas riwayat lama dan jika di masa depan pemilik mengaktifkan Spotify kembali; di `README.md` dicatat bahwa Spotify sengaja disembunyikan dari UI.

### 4.4 Music Switcher, Resolusi Stream & Playlist Netral (`MusicRepositoryImpl`)
- **Playlist Netral Bawaan (`WorkoutPlaylists`):**
  - Sediakan 2–3 playlist workout netral (`Lari Pagi 140+ BPM`, `Tempo Boost`, `Warmup & Cooldown`) berisi `Track` netral yang memiliki `isrc` serta metadata judul/artis/durasi untuk pencocokan antar platform (`SOUNDCLOUD` & `YOUTUBE_MUSIC`).
  - Saat user memilih playlist netral dan menekan putar di platform aktif (`SOUNDCLOUD` atau `YOUTUBE_MUSIC`):
    - Jika `Track` belum punya ID/stream untuk platform aktif, cari kecocokan di platform aktif via `searchTracks("${track.title} ${track.artist}")` + `TrackMatchingEngine.findBestMatch(...)` (ISRC atau judul+artis+durasi ±2d).
    - Resolusi `/player` YouTube Music dilakukan **just-in-time saat track hendak diputar** (bukan saat listing hasil search, agar pencarian tetap cepat).
    - Jika tidak ditemukan kecocokan atau `/player` ditolak (403/`signatureCipher`) saat mode playlist → catat pesan status `"Tidak tersedia di [Nama Platform]"` ke `playbackNotice` UI & log non-sensitif, lalu otomatis lompat ke lagu berikutnya yang tersedia di playlist (maksimal 1 putaran agar tidak infinite loop).
- **Switch Sumber di Tengah Workout (`switchSource(newSource)`):**
  1. Jika `newSource == YOUTUBE_MUSIC` dan `!youtubeMusicEnabled` → tolak dengan pesan *"YouTube Music sedang dinonaktifkan di Pengaturan"*.
  2. Jika `newSource == SPOTIFY` (hanya bila dipanggil secara internal/legacy) → abaikan / kembalikan `false` dengan fallback aman ke `SOUNDCLOUD` tanpa merusak tracking.
  3. **Jangan** sentuh `TrackingService` / `TrackingStateManager` (timer, jarak, pace, dan polyline GPS tidak pernah ter-reset).
  4. Jika ada `currentTrack` saat berpindah antara `SOUNDCLOUD` ↔ `YOUTUBE_MUSIC`:
     - Coba temukan lagu setara di `newSource` (cek `platformIds[newSource]` atau search + `TrackMatchingEngine`).
     - Jika ketemu & stream dapat diputar → lanjutkan putar lagu yang setara di platform baru & catat `PlayedTrack` baru dengan `source = newSource` dan `playedAt` saat itu.
     - Jika search ketemu tetapi `/player` YT Music menolak (`signatureCipher`/403) pada switch single-track → tampilkan banner pemulihan jujur (*"Audio YouTube Music tidak tersedia langsung untuk lagu ini"*) dengan tombol cepat **"Putar di SoundCloud"** / **"Buka di YouTube Music"**.
     - Jika sama sekali tidak ada di platform baru → tampilkan pesan jelas `"\"${track.title}\" tidak tersedia di ${newSource.displayName}"`, lalu lanjut ke lagu berikutnya yang tersedia di antrean (jika ada).
- **Kompatibilitas Data Lama (`v0.0.12`–`v0.1.2`):**
  - Di `Converters.kt` & `WorkoutRepositoryImpl.kt`, saat membaca sesi lama yang `PlayedTrack.trackId` diawali `"sc_"` namun tersimpan sebagai `MusicSource.YOUTUBE_MUSIC`, remap otomatis saat deserialisasi menjadi `MusicSource.SOUNDCLOUD` (beserta `session.musicSource` jika seluruh lagu sesi tersebut adalah `sc_*`), tanpa perlu merubah versi skema Room.
- **State UI Tambahan di `MusicRepository` & `MusicViewModel`:**
  - `youtubeMusicEnabled: StateFlow<Boolean>`
  - `playbackNotice: StateFlow<String?>` + `unavailableExternalUrl: StateFlow<String?>` + `clearPlaybackNotice()`

### 4.5 UI & Pengaturan
- [`SettingsScreen.kt`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/presentation/settings/SettingsScreen.kt):
  - Di section **AKUN MUSIK**:
    - Hapus `SpotifyAccountCard` dan dialog konfigurasi Spotify dari UI agar tidak ada fitur mati.
    - Tampilkan **YouTube Music** dengan badge merah **`Beta / Tidak Resmi`**, toggle **Aktifkan YouTube Music (Feature Flag)** (`youtubeMusicEnabled` default `true`), status jujur *"Aktif — Mode Pencarian Publik (Beta)"* (atau status OAuth terhubung jika client ID diisi), plus catatan ringkas bahwa **SoundCloud** aktif otomatis tanpa login.
- [`MusicPlayerBottomSheet.kt`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/presentation/workout/components/MusicPlayerBottomSheet.kt):
  - Bagian atas: **SUMBER MUSIK AKTIF** berupa switcher **`SoundCloud`** (oranye) dan **`YouTube Music` `[Beta]`** (merah, otomatis tersembunyi jika `youtubeMusicEnabled == false`) — tanpa pill Spotify.
  - Banner notice jika ada pesan (`"tidak tersedia di [platform]"`, tombol **"Putar di SoundCloud"**, atau **"Buka di YouTube Music"**).
  - Bagian **PLAYLIST WORKOUT NETRAL** (pilih playlist netral & putar di sumber aktif).
  - Bagian **Pencarian Online** (mencari ke `SOUNDCLOUD` atau `YOUTUBE_MUSIC` sesuai sumber aktif).
- [`WorkoutScreen.kt`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/presentation/workout/WorkoutScreen.kt) & [`RecapScreen.kt`](file:///C:/Users/zaruf/.t3/projects/stridebeat/app/src/main/java/com/stridebeat/app/presentation/recap/RecapScreen.kt):
  - Tampilkan warna & label yang benar untuk sumber musik: SoundCloud (oranye), YouTube Music (merah), dan Spotify (hijau jika ada di riwayat lama).

---

## 5. Rencana Pengujian & Dokumentasi
1. **Unit Tests (`app/src/test/...`):**
   - `TrackMatchingTest.kt`: uji ISRC match, fallback judul+artis dengan batas tepat ±2 detik (`2000L` ms lulus, `2500L` ms gagal), enum `SOUNDCLOUD`.
   - `MusicSwitcherTest.kt`: uji `switchSource` saat feature flag OFF ditolak aman, pencatatan `PlayedTrack` per sumber (`SOUNDCLOUD` vs `YOUTUBE_MUSIC`), remapping sesi `sc_*` lama, dan logika skip saat track tidak tersedia di platform target tanpa crash/loop.
2. **Maestro Flow (`maestro/m6_music_switcher_flow.yaml`):**
   - Mulai workout → buka bottom sheet → verifikasi switcher `SoundCloud` / `YouTube Music` → ganti sumber di tengah workout → verifikasi tracking timer tetap berjalan (`WAKTU BERJALAN`) → selesaikan sesi & cek rekap.
   - Perbarui juga `maestro/m3_music_flow.yaml` agar selaras dengan label switcher `SoundCloud` / `YouTube Music`.
3. **Dokumentasi:**
   - Perbarui [`docs/YOUTUBE_MUSIC.md`](file:///C:/Users/zaruf/.t3/projects/stridebeat/docs/YOUTUBE_MUSIC.md) (endpoint InnerTube `/search` & `/player` yang dipakai, risiko 403/`signatureCipher`, cara mematikan via toggle UI & `YouTubeMusicConfig`).
   - Perbarui [`README.md`](file:///C:/Users/zaruf/.t3/projects/stridebeat/README.md) (penghapusan Spotify dari UI per keputusan pemilik non-Premium, arsitektur M6 Music Switcher, cara test manual M6, checklist regresi v0.0.12).
4. **Release CI:**
   - Bump `versionCode = 16`, `versionName = "0.2.0"` di `app/build.gradle.kts` & `.github/workflows/build-apk.yml`.
   - Pastikan unit test + `zipalign` + `apksigner` v1/v2/v3 + `aapt2` lolos dan keystore permanen tetap digunakan.
