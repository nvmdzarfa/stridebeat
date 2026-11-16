# Dokumentasi Integrasi YouTube Music — StrideBeat

**Status:** Eksperimental / Beta / Tidak Resmi (Milestone M6)  
**Feature Flag:** `MusicPreferences.youtubeMusicEnabled` & `YouTubeMusicConfig.isYouTubeMusicEnabled`  
**Sumber Musik Utama:** SoundCloud (Stabil & Out-of-the-Box)  
**Keputusan Pemilik:** Spotify dihapus dari UI aktif karena akun pemilik non-Premium; SoundCloud dan YouTube Music (Beta) menjadi dua sumber musik aktif di switcher.

---

## 1. Latar Belakang & Desain Arsitektur

YouTube Music **tidak memiliki Web API publik resmi** untuk konsumsi library pengguna atau direct audio streams tanpa video embed. Oleh karena itu, integrasi ini mengimplementasikan klien InnerTube API ringan:

1. **InnerTube Search:** `POST https://music.youtube.com/youtubei/v1/search` (`clientName: WEB_REMIX`, kategori lagu `EgWKAQIIAWoMEAMQBBAJEAoQBRAV`). Mengembalikan metadata lagu: judul, artis, album, thumbnail, dan `videoId`.
2. **InnerTube On-Demand Player:** `POST https://music.youtube.com/youtubei/v1/player` (mencoba klien `ANDROID_MUSIC` lalu fallback ke `IOS`). Resolusi direct stream audio dilakukan *just-in-time* saat track diputar, bukan saat pencarian, sehingga daftar pencarian tetap instan.
3. **Respon Terhadap Risiko `403` / `signatureCipher`:**
   - StrideBeat **tidak pernah melakukan reverse-engineering cipher DRM atau ad-blocking**.
   - Jika format audio hanya tersedia via `signatureCipher` atau HTTP 403, `YouTubeMusicClient` mengembalikan `StreamResult.Unavailable` secara aman.
   - UI menampilkan banner pemulihan yang jujur: *"Audio YouTube Music tidak tersedia langsung untuk lagu ini"* disertai tombol cepat **"Putar di SoundCloud"** dan **"Buka di YouTube Music"** (intent eksternal).
   - Pada mode playlist olahraga netral, antrean otomatis melompati track yang tidak dapat diputar (maksimal 1 putaran penuh tanpa *infinite loop*).

---

## 2. Batasan Keamanan & Privasi

1. **Tanpa Form Password Google:**
   Aplikasi tidak pernah meminta email atau password Google pengguna. Otorisasi opsional murni menggunakan Google OAuth 2.0 PKCE (`stridebeat://ytmusic-callback`).
2. **Penyimpanan Terenkripsi:**
   Token OAuth Google disimpan di `SecureTokenStorage` menggunakan AndroidX Security `EncryptedSharedPreferences` dengan skema `AES-256 GCM`.
3. **Mode Pencarian Publik (Bawaan):**
   Jika `YOUTUBE_MUSIC_OAUTH_CLIENT_ID` kosong di `local.properties`, fitur pencarian dan pemutaran tetap berfungsi penuh dalam status *"Mode Pencarian Publik (Beta)"* tanpa login.

---

## 3. Fitur Music Switcher (`SoundCloud ↔ YouTube Music`)

- Terletak di bagian atas `MusicPlayerBottomSheet` dengan dua pil:
  - **SoundCloud** (Oranye `#FF5500`)
  - **YouTube Music** (Merah `#FF3B30` dengan badge `Beta`)
- **Pencocokan Lagu Lintas Platform (`TrackMatchingEngine`):**
  - Kunci utama: ISRC matching.
  - Fallback: Normalisasi judul + artis dengan batas toleransi durasi ketat ±2 detik (`2000L` ms).
- **Independensi Tracking Olahraga:**
  - Menukar sumber musik di tengah sesi workout **tidak pernah mereset** timer, jarak, kecepatan (pace), atau polyline GPS di `TrackingService`.

---

## 4. Cara Mematikan Fitur (Kill Switch)

Jika layanan YouTube Music mengalami gangguan eksternal atau skema InnerTube berubah:
1. **Melalui UI Pengguna:**
   Buka **Pengaturan** → **AKUN MUSIK** → Matikan tombol sakelar pada kartu **YouTube Music (Beta / Tidak Resmi)**.
2. **Melalui Konfigurasi Kode:**
   Ubah nilai di `com.stridebeat.app.data.remote.youtubemusic.YouTubeMusicConfig`:
   ```kotlin
   YouTubeMusicConfig.isYouTubeMusicEnabled = false
   ```
3. **Dampak Saat Dimatikan:**
   - Tombol YouTube Music di pemutar otomatis disembunyikan.
   - Pemutar langsung beralih ke SoundCloud tanpa menghentikan pelacakan olahraga.

---

## 5. Checklist Verifikasi Manual & Regresi

- [x] Pencarian musik di SoundCloud menghasilkan full track dan audio jernih.
- [x] Pencarian musik di YouTube Music (Beta) menampilkan hasil lagu InnerTube.
- [x] Berpindah sumber di pemutar mini tidak mengganggu timer lari `WAKTU BERJALAN`.
- [x] Track legacy `sc_*` yang tersimpan di DB Room v0.1.2 ter-remap otomatis menjadi `MusicSource.SOUNDCLOUD`.
- [x] Mematikan toggle YouTube Music di Pengaturan langsung menyembunyikan opsi YouTube Music di pemutar.
- [x] Login Google OAuth (v0.0.12) tetap aman dan tidak terpengaruh oleh integrasi musik.
- [x] Integrasi Strava (v0.0.13) tetap berfungsi normal untuk auto-upload dan ekspor GPX.
