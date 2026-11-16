# StrideBeat (Android Native Kotlin)

<p align="center">
  <img src="assets/icon.svg" width="96" height="96" alt="StrideBeat Logo" />
</p>

> **Strava + Musik dalam Satu Aplikasi Android Native (Kotlin from Scratch)**  
> Menggabungkan tracking olahraga outdoor akurat ala Strava dan pemutar musik terintegrasi dengan sinkronisasi recap rute, metrik performa, dan daftar lagu yang didengarkan.

---

## 📌 Status Pengembangan

- **Platform:** Android Native (100% Kotlin + Jetpack Compose Material 3)
- **Package Name:** `com.stridebeat.app` (dibangun pada v0.1.1)
- **Min SDK:** 26 (Android 8.0 Oreo) | **Target SDK:** 35 (Android 15)
- **Arsitektur:** Clean Architecture + MVVM + Unidirectional Data Flow (StateFlow) + Hilt DI + WorkManager
- **Status Milestone:**
  - ✅ **M0 — Skeleton:** Selesai (Proyek dasar, Navigation Compose, Glassmorphism Dark/Light/System Theme, instant BottomNavBar).
  - ✅ **M1 — Auth & Cloud:** Selesai (Google Sign-In Firebase Auth + Firestore Cloud Sync, Spotify OAuth 2.0 PKCE, EncryptedSharedPreferences AES-256 GCM).
  - ✅ **M2 — GPS Tracking, Foreground Service & Peta:** Selesai (Fused Location Provider, Foreground Service dengan notifikasi persisten, canvas peta polyline live, kalkulasi Haversine distance, pace, kalori estimasi, auto-pause, peringatan GPS terputus).
  - ✅ **M3 — Musik & Playback:** Selesai (SoundCloud API full-track >45 detik + Media3 ExoPlayer HLS/MP3, Media Notification & Lock Screen dengan cover album, pencatatan otomatis lagu per sesi).
  - ✅ **M4 — Recap, Riwayat & Export GPX:** Selesai (Room local SQLite database `stridebeat.db`, ekspor rute universal GPX 1.1, rekap metrik + lagu, share teks, cek update GitHub Releases).
  - ✅ **M5 — Strava Native (Phase 2):** Selesai (Strava OAuth 2.0 + PKCE + auto-refresh token AES-256 GCM, Upload ke Strava via GPX `POST /uploads` & fallback `POST /activities` dengan preview/edit nama & deskripsi lagu max 10, WorkManager offline queue + notifikasi, auto-upload opsional default MATI, impor 30 aktivitas Strava terbaru dengan deduplikasi & polyline decoder, status badge di Riwayat & Recap).
  - ⏳ **M6 — YouTube Music + Music Switcher Utuh:** Belum dikerjakan (milestone berikutnya).
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

   # Spotify Developer API
   SPOTIFY_CLIENT_ID=<isi_client_id_spotify>
   SPOTIFY_REDIRECT_URI=stridebeat://spotify-callback
   ```
   > Jika `STRAVA_CLIENT_ID` / `STRAVA_CLIENT_SECRET` belum diisi, aplikasi tetap berjalan normal dan menampilkan state **"Belum terhubung (Client ID belum dikonfigurasi)"** di Pengaturan.
3. Letakkan `google-services.json` di folder `app/` untuk mengaktifkan Firebase Auth & Cloud Firestore (jangan commit ke Git).
4. **Build & Unit Test:**
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```
   > Release build tetap memakai keystore permanen yang sudah dikonfigurasi agar APK baru dapat ditimpa langsung di atas `v0.0.12` tanpa uninstall.

---

## 🟠 Arsitektur Strava Native (M5)

Detail teknis lengkap tersedia di [`docs/STRAVA.md`](docs/STRAVA.md):

1. **OAuth 2.0 + PKCE & Token Refresh (`StravaAuthManager`):**
   * Scope minimal: `read,activity:read_all,activity:write`.
   * Redirect deep link `stridebeat://strava-callback` di `AndroidManifest.xml`.
   * Token tersimpan terenkripsi AES-256 GCM di `SecureTokenStorage` dan otomatis di-refresh sebelum expired. Logout Strava hanya menghapus token Strava tanpa memutus Google/Spotify.
2. **Upload Aktivitas & WorkManager (`StravaApiClient`, `StravaRepository`, `StravaUploadWorker`):**
   * Jalur utama upload rute GPS: `POST /api/v3/uploads` (`data_type=gpx`) agar peta rute, jarak, pace, dan daftar lagu (`🎵 Diputar di StrideBeat:` maks 10 lagu + `…dan X lagu lainnya`) ikut terunggah; fallback ke `POST /api/v3/activities` bila titik GPS `< 2`.
   * Preview & edit nama + deskripsi selalu muncul sebelum upload manual.
   * Auto-upload default **MATI** di DataStore (`StravaPreferences`), dan hanya berjalan jika durasi `≥ 1 menit` dan jarak `> 0 m`.
   * Saat offline, upload masuk antrean `WorkManager` dengan status **`Menunggu`** dan otomatis terkirim saat koneksi kembali (disertai notifikasi hasil via `StravaNotificationHelper`).
3. **Impor Riwayat & Deduplikasi:**
   * `GET /api/v3/athlete/activities?per_page=30` mengimpor maksimal 30 aktivitas terbaru beserta `map.summary_polyline` yang didekode ke `List<LatLng>`, diberi badge **"Dari Strava"**, dan dideduplikasi via `stravaActivityId` atau kecocokan waktu/jarak/durasi.
   * Room Database naik dari versi `1` ke `2` dengan `MIGRATION_1_2` sehingga seluruh riwayat `v0.0.12` tetap aman saat update APK.

---

## 🧪 Cara Pengujian Manual & Otomatis (Milestone M5)

1. **Uji Pengaturan & State Akun Strava:**
   * Buka tab **Pengaturan** → bagian **AKUN TERHUBUNG (STRAVA)**.
   * Pastikan jika `STRAVA_CLIENT_ID` belum diisi, tampil **"Belum terhubung (Client ID belum dikonfigurasi)"** (tidak crash).
   * Jika sudah dikonfigurasi: tap **Hubungkan**, login Strava, pastikan kembali ke StrideBeat dengan status **"Terhubung: <Nama Atlet>"**.
   * Coba tap **Putuskan** pada kartu Strava → pastikan akun Google & Spotify tetap terhubung.
2. **Uji Auto-upload & Preview Upload di Recap:**
   * Pastikan toggle **Auto-upload ke Strava** default **MATI**.
   * Lakukan sesi workout singkat (< 1 menit) dengan Auto-upload **ON** → di Recap muncul alasan dilewati dan tidak mengunggah sesi kosong ke Strava.
   * Pada sesi valid → tap **Upload ke Strava**, periksa dialog preview nama (`StrideBeat Run — ...`) dan deskripsi (jarak/durasi/pace + daftar lagu max 10).
   * Matikan internet lalu tap **Kirim ke Strava** → status di Recap & Riwayat menjadi **`Menunggu`**. Nyalakan internet kembali → WorkManager otomatis mengunggah dan memunculkan notifikasi selesai + badge **`Strava ✓`** (ketuk untuk membuka link Strava).
3. **Uji Impor Riwayat Strava:**
   * Di **Riwayat** (atau **Pengaturan**), tap **Impor Strava**.
   * Pastikan maksimal 30 aktivitas muncul berlabel **"Dari Strava"** dengan rute tergambar; ketuk **Impor Strava** kedua kalinya → tidak terjadi duplikasi.
4. **Uji Otomatis (Unit Test & Maestro):**
   ```bash
   ./gradlew testDebugUnitTest
   maestro test maestro/m5_strava_flow.yaml
   ```

---

## ✅ Checklist Regression Test v0.0.12 (Wajib Lolos M5)

- [x] **Unit Test Build & M5 Logic (`DomainModelTest`, `GpxExporterTest`, `StravaIntegrationTest`):** Lolos pada CI `testDebugUnitTest`.
- [ ] Login Google (Firebase Auth) + sync Firestore + mode offline Room tetap jalan *(verifikasi manual di perangkat fisik pasca rilis APK)*.
- [ ] Tracking 10 menit: GPS polyline live, jarak/durasi/pace/kalori benar, auto-pause jalan, peringatan GPS terputus muncul saat GPS hilang.
- [ ] Musik SoundCloud: search full-track >45 detik, playback HLS/MP3 via ExoPlayer, skip/seekbar, Media Notification & Lock Screen dengan cover.
- [ ] Spotify OAuth PKCE + token terenkripsi tetap jalan.
- [ ] Recap & Riwayat: rute + statistik + lagu tersimpan, share teks jalan, ekspor GPX jalan.
- [ ] Beranda 7 hari + shortcut sesi terakhir tetap benar.
- [ ] Cek update via GitHub Releases (popup + notifikasi) tetap jalan.
- [ ] Tema Dark / Light / Ikuti Sistem + bottom bar instan tetap jalan.

---

## 🔑 Yang Masih Dibutuhkan dari Pemilik (M5 Live Strava)

- **`STRAVA_CLIENT_ID` & `STRAVA_CLIENT_SECRET`:** Buat / ambil dari [Strava API Settings](https://www.strava.com/settings/api) dengan Authorization Callback Domain `strava-callback`, lalu simpan ke `local.properties` dan GitHub Secrets agar fitur OAuth, Upload, dan Impor Strava dapat login secara live (tanpa key ini, UI otomatis menampilkan placeholder `Belum terhubung`).
