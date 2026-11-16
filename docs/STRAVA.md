# Dokumentasi Integrasi Strava Native — StrideBeat (M5)

**Status:** Aktif (Milestone M5 — Strava API v3)  
**Referensi:** PRD Phase 2 §6.1, §7.1, §8.1  

---

## 1. Mendapatkan Client ID & Client Secret Strava

1. Buka [Strava API Settings](https://www.strava.com/settings/api) dan login dengan akun Strava Anda.
2. Buat aplikasi baru (atau gunakan aplikasi Strava Developer yang sudah ada):
   - **Application Name:** `StrideBeat`
   - **Authorization Callback Domain:** `strava-callback` (untuk deep link `stridebeat://strava-callback`).
3. Salin **Client ID** dan **Client Secret**, lalu masukkan ke file `local.properties` di root proyek (jangan di-commit ke Git):
   ```properties
   STRAVA_CLIENT_ID=<client_id_anda>
   STRAVA_CLIENT_SECRET=<client_secret_anda>
   ```
   Untuk build CI/CD GitHub Actions, tambahkan repository secrets `STRAVA_CLIENT_ID` dan `STRAVA_CLIENT_SECRET`.

> Jika `STRAVA_CLIENT_ID` / `STRAVA_CLIENT_SECRET` belum diisi, UI Pengaturan otomatis menampilkan status **"Belum terhubung (Client ID belum dikonfigurasi)"** tanpa crash, dan fitur ekspor GPX manual tetap bisa dipakai.

---

## 2. OAuth 2.0 + PKCE & Keamanan Token

- **Authorization URL:** `https://www.strava.com/oauth/mobile/authorize` dengan parameter `code_challenge` (`S256`), `redirect_uri=stridebeat://strava-callback`, dan deep link `<intent-filter>` di `AndroidManifest.xml`.
- **Scope Minimal yang Diminta:**
  - `read`: membaca profil dasar atlet (`GET /api/v3/athlete` / respons token).
  - `activity:read_all`: mengimpor hingga 30 riwayat aktivitas terbaru milik pengguna (`GET /api/v3/athlete/activities`).
  - `activity:write`: mengunggah aktivitas hasil tracking StrideBeat ke Strava (`POST /api/v3/uploads` / `POST /api/v3/activities`).
- **Penyimpanan Token:** Access token, refresh token, waktu kedaluwarsa (`expires_at`), nama atlet, dan `athlete_id` disimpan di `EncryptedSharedPreferences` terenkripsi **AES-256 GCM** (`SecureTokenStorage.kt`). Token tidak pernah ditulis ke Logcat.
- **Auto-Refresh Token:** Sebelum memanggil endpoint Strava, `StravaAuthManager.getValidAccessToken()` otomatis melakukan `POST /oauth/token` (`grant_type=refresh_token`) jika token habis atau kurang dari 60 detik sebelum expired. Jika refresh ditolak (HTTP 400/401), token dibersihkan dan status UI berubah ke **`Token expired`** dengan tombol **Login Ulang** (tanpa retry loop tak terbatas).

---

## 3. Jalur Upload Aktivitas & Antrean Offline (WorkManager)

### Pemilihan Jalur Utama (`POST /uploads` vs `POST /activities`)
Sesuai PRD §6.1, StrideBeat memilih **satu jalur utama per sesi** agar tidak terjadi aktivitas ganda:
- **Jalur Utama (sesi dengan GPS `routePoints.size >= 2`):** `POST /api/v3/uploads` (`multipart/form-data`, `data_type=gpx`, `external_id=stridebeat_<sessionId>`), diikuti polling singkat ke `GET /api/v3/uploads/{id}` untuk mendapatkan `activity_id`.
  - **Alasan:** Endpoint `POST /api/v3/activities` pada Strava API v3 hanya membuat entri manual tanpa koordinat rute peta GPS. Dengan mengunggah GPX 1.1 via `POST /uploads`, Strava menerima rute lengkap, stempel waktu tiap titik GPS, jarak, dan pace secara akurat.
- **Fallback (sesi tanpa GPS `routePoints.size < 2`):** `POST /api/v3/activities` sehingga ringkasan durasi, jarak, dan lagu tetap bisa dicatat jika GPS sempat hilang total.

### Format Nama & Deskripsi Aktivitas
- **Nama Default:** `StrideBeat Run — <tanggal> <jam>` (atau `Walk` / `Ride`), dapat diubah sebelum upload maupun prefix-nya di Pengaturan.
- **Deskripsi:**
  - Baris 1: `Jarak: X.XX km • Durasi: MM:SS • Pace: M'SS"/km`
  - Baris lagu: `🎵 Diputar di StrideBeat:` berisi maksimal **10 lagu** (`Judul — Artis`) dan ringkasan `…dan X lagu lainnya` bila lebih dari 10 lagu. Tidak ada token/email yang dikirim.

### Antrean Offline & Auto-upload
- Upload dijalankan oleh **`StravaUploadWorker` (WorkManager)** dengan `NetworkType.CONNECTED`. Jika HP sedang offline, status sesi menjadi **`Menunggu`** dan otomatis terunggah saat internet kembali, lalu memunculkan notifikasi `POST_NOTIFICATIONS`.
- **Auto-upload:** Default **MATI**. Jika diaktifkan, sesi baru otomatis dijadwalkan saat masuk Recap hanya jika `durationMs >= 60.000 ms` (≥ 1 menit) dan `distanceMeters > 0`.

---

## 4. Impor Riwayat & Memutuskan Akun

- **Impor Riwayat (`GET /api/v3/athlete/activities?per_page=30&page=1`):** Mengambil maksimal 30 aktivitas terbaru beserta `map.summary_polyline` (didekode menjadi rute `LatLng`), lalu dideduplikasi terhadap sesi lokal berdasarkan `stravaActivityId` atau kecocokan waktu mulai (±2 menit), jarak (±5% / 50m), dan durasi (±60 detik). Hasil impor ditandai badge **"Dari Strava"**.
- **Putuskan Akun:** Menekan **Putuskan** pada kartu Strava hanya menghapus token Strava (`clearStravaTokens()`) tanpa me-logout Google, Spotify, SoundCloud, maupun YouTube Music.
