# Dokumentasi Integrasi YouTube Music — StrideBeat

**Versi:** `0.2.0` (`versionCode 16`)  
**Status:** Eksperimental / Beta / Tidak Resmi (Milestone M6)  
**Feature Flag:** `MusicPreferences.youtubeMusicEnabled` & `YouTubeMusicConfig.isYouTubeMusicEnabled`  
**Sumber Musik Utama:** SoundCloud (Stabil & Out-of-the-Box)  
**Keputusan Pemilik:**
- **Spotify:** Dihapus dari UI aktif karena akun pemilik non-Premium; SoundCloud dan YouTube Music (Beta) menjadi dua sumber musik aktif di switcher.
- **Login YouTube Music (Celah 1 — Opsi A):** Tombol login Google OAuth dihapus dari UI aktif dan AuthViewModel. Aplikasi murni berjalan dalam **Mode Pencarian Publik (Beta)** tanpa tombol login semu/mati suri.
- **Library/Liked YouTube Music (Celah 2):** Dilewati (cukup pencarian publik, tanpa injeksi cookie/WebView capture demi menjaga privasi dan keamanan akun Google pengguna).

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

## 2. Batasan Keamanan, Privasi & Keputusan Arsitektur

1. **Celah 1 — Nasib Login YouTube Music (Diputuskan: Opsi A):**
   - **Keputusan:** Alur login Google OAuth PKCE YouTube Music dihapus dari antarmuka Pengaturan dan `AuthViewModel`.
   - **Alasan:** Token Google OAuth 2.0 PKCE yang diterbitkan Google Cloud berlingkup `youtube.readonly` ditujukan untuk Google Data API v3 (`youtube.googleapis.com`), sedangkan antarmuka pemutaran InnerTube (`music.youtube.com/youtubei/v1`) memerlukan sesi cookie web browser Google. Menghubungkan akun Google tanpa mengonsumsi token tersebut di klien InnerTube adalah login "mati suri". Dengan Opsi A, UI bersih, transparan, dan beroperasi murni dalam **Mode Pencarian Publik (Beta)** tanpa perlu login atau kredensial apa pun.
   - Helper internal `YouTubeMusicAuthManager` tetap ada di repository data jika di masa depan Google merilis API pemutar resmi.
2. **Celah 2 — Akses Library / Liked Playlist Pribadi (Diputuskan: Cukup Search Publik):**
   - **Keputusan:** Akses library/liked via WebView cookie capture dan `SAPISIDHASH` (pola BitChord) **sengaja tidak dikerjakan**.
   - **Alasan:** 
     1. Menghindari risiko keamanan dan pembatasan akun Google pengguna akibat scraping session cookie.
     2. Mencegah pelanggaran lisensi copy-paste (kode BitChord berlisensi GPLv3).
     3. Mode pencarian publik dan playlist workout netral sudah mencukupi kebutuhan olahraga outdoor di Milestone M6.
3. **Tanpa Form Password Google & Tanpa DRM Bypass:**
   Aplikasi tidak pernah meminta email atau password Google pengguna, tidak membobol enkripsi DRM, tidak mem-bypass YouTube Premium, tidak memblokir iklan dalam app, dan tidak mencetak token ke Logcat.

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

## 5. Fitur Impor Playlist YouTube (Hasil Respon Testing Fisik)

Berdasarkan masukan pengujian live di perangkat fisik (pemilik tidak memerlukan template kaku 3-4 lagu dan menginginkan fitur playlist langsung dari YouTube agar tidak perlu mencari lagu satu per satu):
1. **InnerTube Browse (`/browse`):**
   - Mendukung link atau ID playlist publik dari YouTube Music maupun YouTube Web (`browseId: "VL" + playlistId`).
   - Parsing adaptif mendukung struktur modern `lockupViewModel` (YouTube Web 2024), `playlistVideoRenderer` (klasik), dan `musicResponsiveListItemRenderer` (YouTube Music).
2. **Penyimpanan Playlist Lokal (`PlaylistStorage`):**
   - Menggunakan DataStore Preferences (`stridebeat_user_playlists`) sebagai penyimpanan JSON terisolasi tanpa merubah atau mengambil risiko migrasi skema SQLite Room.
   - Pengguna dapat mengimpor playlist publik puluhan hingga 100+ lagu, memutarnya secara berurutan selama olahraga, dan menghapusnya kapan saja.
3. **Empty State & UI Bersih:**
   - Template kaku 3-lagu digantikan oleh daftar playlist pengguna yang fleksibel dengan tombol `+ Impor YouTube`.
   - Dialog impor dilengkapi chip contoh cepat dan feedback visual status impor.

---

## 6. Checklist Verifikasi Live di HP (Dilakukan Pemilik)

Checklist ini dilakukan secara langsung di perangkat fisik Android oleh pemilik sebelum mengklaim M6 sepenuhnya selesai:
- [ ] **Build debug/release:** Unduh / instal APK `v0.2.0` di HP.
- [ ] **Nyalakan YouTube Music:** Pengaturan → AKUN MUSIK → Toggle ON, label `Beta / Tidak Resmi` terlihat.
- [ ] **Cari lagu YTM → Putar:** Cek judul, artis, durasi, cover muncul; audio berbunyi; seekbar/skip jalan; notifikasi media & lock screen tampil dengan kontrol playback.
- [ ] **Impor Playlist YouTube:** Buka pemutar musik → tap "+ Impor YouTube" → tempel link playlist YouTube atau coba contoh (Pop Hits 100 lagu) → pastikan puluhan lagu terimpor dan siap diputar berurutan.
- [ ] **Workout + Switcher:** Mulai workout (lari/jalan 10 menit) sambil lagu YTM berputar → ganti sumber ke SoundCloud di tengah sesi → pastikan timer, jarak, dan rute tidak ter-reset, dan lagu tercatat dengan sumber yang benar di Recap.
- [ ] **Cek Lagu Cipher/Unavailable:** Cari lagu yang tidak dapat diputar langsung (misal lagu Sony/label ber-DRM) → pastikan muncul pesan ramah + tombol "Putar di SoundCloud" / "Buka di YouTube Music", bukan crash.
- [ ] **Matikan Fitur (Kill-switch):** Toggle OFF di Pengaturan → app tetap berjalan mulus dengan SoundCloud saja.
- [ ] **Regresi Fitur v0.0.12 & M5:** Login Google Firebase, GPS tracking + auto-pause, riwayat & ekspor GPX, koneksi & upload Strava.
