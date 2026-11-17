# StrideBeat (Android Native Kotlin)

<p align="center">
  <img src="assets/icon.svg" width="96" height="96" alt="StrideBeat Logo" />
</p>

> **Strava + Musik dalam Satu Aplikasi Android Native (Kotlin from Scratch)**  
> Menggabungkan tracking olahraga outdoor akurat ala Strava dan pemutar musik terintegrasi dengan sinkronisasi recap rute, metrik performa, dan daftar lagu yang didengarkan.

---

## 📌 Status Pengembangan

- **Versi Terkini:** `v0.2.1` (`versionCode = 17`)
- **Platform:** Android Native (100% Kotlin + Jetpack Compose Material 3)
- **Package Name:** `com.stridebeat.app`
- **Min SDK:** 26 (Android 8.0 Oreo) | **Target SDK:** 35 (Android 15)
- **Arsitektur:** Clean Architecture + MVVM + Unidirectional Data Flow (StateFlow) + Hilt DI + WorkManager
- **Status Milestone:**
  - ✅ **M0 — Skeleton:** Selesai (Proyek dasar, Navigation Compose, Glassmorphism Dark/Light/System Theme, instant BottomNavBar).
  - ✅ **M1 — Auth & Cloud:** Selesai (Google Sign-In Firebase Auth + Firestore Cloud Sync, Spotify OAuth 2.0 PKCE, EncryptedSharedPreferences AES-256 GCM).
  - ✅ **M2 — GPS Tracking, Foreground Service & Peta:** Selesai (Fused Location Provider, Foreground Service dengan notifikasi persisten, canvas peta polyline live, kalkulasi Haversine distance, pace, kalori estimasi, auto-pause, peringatan GPS terputus).
  - ✅ **M3 — Musik & Playback:** Selesai (SoundCloud API full-track >45 detik + Media3 ExoPlayer HLS/MP3, Media Notification & Lock Screen dengan cover album, pencatatan otomatis lagu per sesi).
  - ✅ **M4 — Recap, Riwayat & Export GPX:** Selesai (Room local SQLite database `stridebeat.db`, ekspor rute universal GPX 1.1, rekap metrik + lagu, share teks, cek update GitHub Releases).
  - ✅ **M5 — Strava Native (Phase 2):** Selesai (Strava OAuth 2.0 + PKCE + auto-refresh token AES-256 GCM, Upload ke Strava via GPX `POST /uploads` & fallback `POST /activities`, WorkManager offline queue + notifikasi, auto-upload opsional default MATI, impor 30 aktivitas Strava terbaru dengan deduplikasi).
  - ✅ **M6 — YouTube Music (Beta) + Music Switcher & Impor Playlist:** Selesai Implementasi & CI (Multi-source switcher `SoundCloud ↔ YouTube Music`, InnerTube API `/search` & `/player`, fitur Impor Playlist YouTube publik puluhan hingga 100+ lagu dengan penyimpanan lokal DataStore, feature flag kill-switch di Pengaturan, Spotify disembunyikan dari UI aktif per keputusan pemilik, YouTube Music murni Mode Pencarian Publik Beta).
  - ⏳ **M7 — Musik Mengikuti Pace + Audio Cue:** Belum dikerjakan.
  - ⏳ **M8 — Statistik Lanjutan + Share Gambar:** Belum dikerjakan.

---

## 🔧 Cara Setup (`local.properties`) & Build

1. Salin `local.properties.example` menjadi `local.properties` di root proyek.
2. Isi key developer sesuai kebutuhan:
   ```properties
   # Strava API (https://www.strava.com/settings/api — Callback Domain: strava-callback)
   STRAVA_CLIENT_ID=<isi_client_id_strava>
   STRAVA_CLIENT_SECRET=<isi_client_secret_strava>

   # Spotify Developer API (Opsional - UI dinonaktifkan per keputusan pemilik)
   SPOTIFY_CLIENT_ID=11f00fd3daf04d1a9c6b38594a26005a
   SPOTIFY_REDIRECT_URI=stridebeat://spotify-callback

   # SoundCloud Client ID (Opsional - ada fallback default)
   SOUNDCLOUD_CLIENT_ID=
   ```
   > Jika `STRAVA_CLIENT_ID` / `STRAVA_CLIENT_SECRET` belum diisi, aplikasi tetap berjalan normal dan menampilkan status **"Belum terhubung"** di Pengaturan.
3. Letakkan `google-services.json` di folder `app/` untuk mengaktifkan Firebase Auth & Cloud Firestore (jangan commit ke Git).
4. **Build & Unit Test:**
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```
   > Release build memakai keystore permanen yang sudah terkonfigurasi pada CI agar APK baru (`v0.2.0`) dapat diinstal menimpa versi sebelumnya tanpa uninstall.

---

## 🔴 Arsitektur YouTube Music Beta & Music Switcher (M6)

Detail teknis lengkap tersedia di [`docs/YOUTUBE_MUSIC.md`](docs/YOUTUBE_MUSIC.md):

1. **Multi-Source Switcher (`SoundCloud ↔ YouTube Music`):**
   * Berada di bagian atas `MusicPlayerBottomSheet`.
   * Berpindah sumber di tengah latihan **tidak pernah mereset** timer, jarak, kecepatan (pace), atau polyline rute GPS di `TrackingService`.
   * Pencocokan lagu lintas platform via `TrackMatchingEngine`: ISRC match atau normalisasi judul + artis dengan batas toleransi durasi ketat ±2 detik (`2000L` ms).
2. **InnerTube API Klien Ringan (`YouTubeMusicClient`):**
   * Katalog lagu dicari via `POST https://music.youtube.com/youtubei/v1/search` (`clientName: WEB_REMIX`).
   * Resolusi streaming dilakukan on-demand (*just-in-time*) saat lagu hendak diputar via `POST https://music.youtube.com/youtubei/v1/player` (`ANDROID_MUSIC` & fallback `IOS`).
   * Format audio langsung yang didapat diputar secara native melalui Media3 ExoPlayer.
3. **Penanganan Format Cipher / 403 & Tombol Pemulihan:**
   * Aplikasi tidak membobol DRM dan tidak mem-bypass YouTube Premium.
   * Lagu yang format audionya hanya tersedia melalui `signatureCipher` atau HTTP 403 dikembalikan sebagai `StreamResult.Unavailable` secara aman. UI menampilkan banner jujur: *"Audio YouTube Music tidak tersedia langsung untuk lagu ini"* disertai tombol cepat **"Putar di SoundCloud"** dan **"Buka di YouTube Music"**.
4. **Keputusan Celah 1 (Nasib Login YTM — Opsi A):**
   * Alur login Google OAuth YouTube Music dihapus dari UI Pengaturan dan `AuthViewModel` agar tidak ada tombol semu/mati suri, karena InnerTube API pemutaran audio tidak menerima token Bearer Google Data API v3 tanpa cookie sesi browser. YouTube Music murni berjalan dalam **Mode Pencarian Publik (Beta)**.
5. **Keputusan Celah 2 (Library / Liked via Cookie):**
   * Akses library/liked lewat WebView capture cookie dan `SAPISIDHASH` (pola BitChord) **sengaja dilewati** untuk menjaga privasi pengguna, mencegah risiko pembatasan akun Google, dan menghindari copy-paste kode GPLv3.

---

## 🧪 Laporan Hasil Unit Test & Verifikasi Otomatis

Seluruh 34 unit test dijalankan dan lolos 100% pada CI (`testDebugUnitTest`):

| Test Suite | File Uji | Jumlah Kasus | Status | Cakupan |
|---|---|---|---|---|
| **Domain Model** | `DomainModelTest.kt` | 4 test | ✅ **Lolos** | Model data workout, format durasi/pace, enum `MusicSource` |
| **GPX Exporter** | `GpxExporterTest.kt` | 3 test | ✅ **Lolos** | Struktur XML GPX 1.1, titik rute, track point timestamps |
| **Strava Integration** | `StravaIntegrationTest.kt` | 8 test | ✅ **Lolos** | OAuth PKCE, GPX payload builder, deduplikasi 30 aktivitas |
| **Track Matching** | `TrackMatchingTest.kt` | 7 test | ✅ **Lolos** | ISRC matching, normalisasi judul/artis, toleransi durasi ketat ±2 detik |
| **Music Switcher** | `MusicSwitcherTest.kt` | 7 test | ✅ **Lolos** | State switcher, isolasi tracking, playlist skip loop guard, remapping `sc_*` |
| **YouTube Music Parser** | `YouTubeMusicParsingTest.kt` | 5 test | ✅ **Lolos** | Parsing response JSON search & player InnerTube, direct audio url, cipher handling |
| **Total** | | **34 test** | ✅ **34 Lolos (100%)** | |

---

## 📱 Checklist Verifikasi Live di HP (Dilakukan Pemilik / Geiza)

*Catatan: Milestone M6 tidak diklaim selesai sebelum verifikasi langsung di HP fisik berhasil dilakukan.*

- [ ] **1. Instalasi APK v0.2.0:** Unduh file APK rilis dari GitHub Releases dan pasang langsung di HP.
- [ ] **2. Aktivasi YouTube Music:** Buka **Pengaturan** → **AKUN MUSIK** → Pastikan kartu **YouTube Music** memiliki badge **Beta / Tidak Resmi** dan status **Mode Pencarian Publik (Beta)**. Nyalakan toggle ON.
- [ ] **3. Uji Pemutaran YouTube Music:** Cari lagu di pencarian YouTube Music → putar lagu → pastikan judul, artis, cover album, dan audio ExoPlayer berbunyi. Cek seekbar, tombol skip, media notification di status bar, dan lock screen controls.
- [ ] **4. Uji Sesi Workout + Music Switcher:** Mulai latihan (lari/jalan 10 menit) sambil memutar lagu YouTube Music → buka bottom sheet musik dan ganti sumber ke **SoundCloud** di tengah sesi → pastikan timer, jarak, pace, dan rute polyline GPS **tidak reset**. Selesaikan sesi dan pastikan riwayat/Recap mencatat sumber lagu dengan benar.
- [ ] **5. Uji Lagu SignatureCipher / Unavailable:** Cari lagu yang kemungkinan cipher-only → pastikan muncul pesan ramah *"Audio YouTube Music tidak tersedia langsung"* + tombol *"Putar di SoundCloud"* / *"Buka di YouTube Music"*, tanpa force close/crash.
- [ ] **6. Uji Kill-Switch:** Matikan toggle YouTube Music di Pengaturan → pastikan tombol YouTube Music di player hilang dan aplikasi beralih mulus ke SoundCloud.
- [ ] **7. Uji Regresi v0.0.12 & M5:** Login Firebase Auth, GPS tracking auto-pause, ekspor rute GPX, dan koneksi Strava tetap berfungsi normal.
