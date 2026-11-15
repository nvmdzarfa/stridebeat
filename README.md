# StrideBeat (Android Native Kotlin)

<p align="center">
  <img src="assets/icon.svg" width="96" height="96" alt="StrideBeat Logo" />
</p>

> **Strava + Musik dalam Satu Aplikasi Android Native (Kotlin from Scratch)**  
> Menggabungkan tracking olahraga outdoor akurat ala Strava dan pemutar musik multi-platform (Spotify & YouTube Music) dengan sinkronisasi recap rute, metrik performa, dan daftar lagu yang didengarkan.

---

## 📌 Status Pengembangan

- **Platform:** Android Native (100% Kotlin + Jetpack Compose Material 3)
- **Min SDK:** 26 (Android 8.0 Oreo) | **Target SDK:** 35 (Android 15)
- **Arsitektur:** Clean Architecture + MVVM + Unidirectional Data Flow (StateFlow) + Hilt DI
- **Status Milestone:**
  - ✅ **M0 — Skeleton:** Selesai (Proyek dasar, Navigation Compose, Theme, 5 layar placeholder).
  - ✅ **M1 — Auth (Strava, Spotify, YouTube Music):** Selesai (OAuth 2.0 PKCE, Spotify Client ID terpasang, EncryptedSharedPreferences AES-256 GCM).
  - ✅ **M2 — GPS Tracking, Foreground Service & Peta:** Selesai (Fused Location Provider, Foreground Service dengan notifikasi persisten, canvas peta polyline live, kalkulasi Haversine distance, pace, kalori estimasi, dan auto-pause).
  - ✅ **M3 — Musik & Switcher (Spotify / YouTube Music):** Selesai (Music Hub & Switcher, pencocokan lagu lintas platform dengan ISRC + toleransi durasi, background playback MediaSessionCompat, pencatatan otomatis lagu per sesi olahraga tanpa memutus tracking).
  - ✅ **M4 — Recap, Riwayat & Export GPX:** Selesai (Room local SQLite database `stridebeat.db`, ekspor rute universal GPX 1.1 untuk upload langsung ke Strava tanpa subscription berbayar, rekap metrik lengkap + lagu, dialog konfirmasi hapus riwayat).

---

## 🏃 Arsitektur Recap, Riwayat & Export GPX (M4)

1. **Room Local Database (`StrideBeatDatabase`):**
   * Menyimpan riwayat sesi lengkap offline di file database lokal SQLite `stridebeat.db` (PRD §7.6).
   * Dilengkapi TypeConverters untuk serialisasi `List<LatLng>`, `List<PlayedTrack>`, dan enum `WorkoutType`.
   * Mendukung full reactive Flow data stream (`getAllSessions()`) dan operasi CRUD (Simpan, Baca, Hapus).
2. **Universal GPX 1.1 Exporter (`GpxExporter`):**
   * Menghasilkan file XML GPX 1.1 standar dengan metadata titik rute (`trkpt`) berstempel waktu UTC.
   * Rute dapat langsung dibagikan atau diunggah secara bebas ke **Strava (strava.com/upload)**, Garmin Connect, maupun Komoot tanpa perlu berlangganan Strava Developer API berbayar.
3. **Penyatuan Metrik & Musik (Recap):**
   * Menggabungkan visualisasi polyline rute lari, ringkasan waktu, jarak, pace rata-rata, kalori (estimasi), serta linimasa lagu yang diputar selama sesi.

---

## 🎵 Arsitektur Musik & Switcher M3

1. **Prinsip Arsitektur Hub:**
   * Aplikasi berfungsi sebagai **hub / switcher**, memutar lagu lewat platform masing-masing tanpa membobol DRM atau mem-bypass akun langganan (PRD §1.1).
   * **Pencocokan Lagu Lintas Platform (`TrackMatchingEngine`):** Menggunakan ISRC (*International Standard Recording Code*) sebagai kunci pemersatu, dengan fallback normalisasi judul + artis + durasi (±3 detik).
2. **Background Playback (`MusicPlaybackService`):**
   * Menggunakan `MediaSessionCompat` dan `NotificationCompat.MediaStyle` (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`).
   * Tombol kontrol pemutar musik (*Play/Pause/Skip*) aktif di layar kunci (*lockscreen*) dan notification drawer.
3. **Pencatatan Sinkron ke Sesi:**
   * Setiap lagu yang berputar selama workout otomatis dicatat beserta stempel waktu (`playedAt`), dan langsung dilampirkan ke data ringkasan sesi di layar Recap.
   * Mengganti platform musik di tengah latihan **TIDAK AKAN MERESET** timer, rute, atau akumulasi jarak GPS pelari.

---

## 🧪 Cara Pengujian Manual (Milestone M4)

1. **Uji Alur Sesi Lengkap (Workout -> Recap -> Riwayat) di Preview:**
   * Buka preview interaktif di atas -> Klik tombol **MULAI OLAHRAGA**.
   * Coba kontrol tombol *Jeda / Lanjut* dan pemutar musik.
   * Ketuk tombol **Selesai** -> konfirmasi **Ya, Selesai**.
   * Di layar **Recap**, perhatikan visualisasi rute peta canvas, 4 kartu metrik performa, dan linimasa lagu yang menemani lari kamu.
   * Coba klik tombol **Export GPX**: sistem akan menyiapkan file XML GPX standar untuk Strava.
   * Ketuk tombol **Selesai & Simpan Sesi**: sesi akan tersimpan otomatis ke database Room dan diarahkan ke Beranda.
   * Klik tab **Riwayat** di bilah bawah: lihat sesi yang baru saja kamu selesaikan masuk ke dalam daftar tersimpan offline.
   * Coba klik ikon tong sampah pada kartu sesi: dialog konfirmasi penghapusan sesi akan muncul sesuai standar PRD §7.7.
2. **Uji Otomatis Maestro M4:**
   ```bash
   maestro test maestro/m4_recap_history_flow.yaml
   ```
3. **Uji Unit Test GPX & Model:**
   ```bash
   ./gradlew testDebugUnitTest
   ```
