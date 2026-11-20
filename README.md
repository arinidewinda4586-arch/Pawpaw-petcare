# PawPaw Pet Care — Aplikasi Desktop (Java Swing + MySQL)

Aplikasi desktop **PawPaw Pet Care** berbasis **Pure JDK Swing (JDK 25)** dan **MySQL (`pet_care`)** tanpa dependensi library UI eksternal. Aplikasi ini menyatukan ruang kerja **Pelanggan** (Beranda, Adopsi, Toko & Keranjang, Layanan Kesehatan, Profil & Peliharaan) dan ruang kerja **Administrator** (Dashboard Operasional, CRUD Master Produk/Hewan/Layanan, serta manajemen transaksi Pesanan, Verifikasi Adopsi, dan Booking Kesehatan) dalam satu shell desktop modern dengan tampilan **Web-Matched UI (Fase 2)** yang selaras dengan versi web PawPaw Pet Care.

---

## Desain & Pengalaman Antarmuka (Fase 2 — Web-Matched UI)

- **Tipografi Bundel `Poppins` (SIL OFL 1.1)**: Seluruh aplikasi menggunakan 4 bobot font `Poppins` (`Regular`, `Medium`, `SemiBold`, `Bold`) yang dibundel di `src/main/resources/fonts/` (beserta lisensi `OFL.txt`), disalin otomatis ke `bin/fonts/` saat build, dan dimuat oleh `Theme` dengan fallback aman ke `Segoe UI` / `SansSerif`.
- **Sistem Dual-Palette (`app.css` vs `admin.css`)**:
  - **Mode Pelanggan & Autentikasi (`Theme` / `app.css`)**: Kanvas biru muda `#EEF6FD`, kartu putih membulat (`20px`), navy identitas `#0F2854`, biru `#1C4D8D` / `#4988C4`, aksen emas `#FFC93C`, serta aksen kartu pastel (`#FFD6D6`, `#D6E8FF`, `#D8F5D0`, `#E8D9FF`).
  - **Mode Administrator (`Theme.Admin` / `admin.css`)**: Kanvas `#F2F7FF`, sidebar navy `#0F2747`, tombol primer solid `#2F6FED`, tombol aksi pastel (Detail `#EDF4FF`, Edit `#E6F6EF`, Hapus `#FFF0EF`), header tabel `#F1F6FF`, dan pil `#ID` biru muda (`IdPillRenderer`) pada kolom identitas tabel.
- **Layar Autentikasi Maskot Kucing Interaktif (`AuthPanel`)**: Latar belakang gradien radial biru langit, ilustrasi maskot kucing vektor yang mengikuti arah kursor dan menutup mata saat mengetik kata sandi, kartu tunggal membulat (`28px`) dengan animasi balik (*flip*) antara **Masuk** dan **Daftar**, kotak centang **Ingat saya** (menyimpan identitas login saja tanpa kata sandi melalui `Preferences`), petunjuk akun demo, serta indikator koneksi MySQL waktu-nyata.
- **Shell Desktop dengan Sidebar Lipat (`DesktopShell`)**: Klik tombol brand `logo.png` di bagian atas sidebar untuk melipat/membuka panel navigasi (`220px` Pelanggan / `245px` Admin ↔ `84px` mode ikon ringkas dengan tooltip), dilengkapi top bar konteks halaman, pengalih mode Admin ↔ Pelanggan tanpa logout, dan status bar koneksi database di bagian bawah.

---

## Fitur Utama

### Ruang Kerja Pelanggan (5 Halaman)
- **Beranda (`user.dashboard`)**: Header sapaan personal, 4 kartu menu cepat pastel bergaya website (*Adopsi*, *Toko*, *Cek Kesehatan*, *Jadwalkan Vaksin*), ringkasan 4 kartu KPI (hewan peliharaan, pesanan, pengajuan adopsi, booking aktif), serta tabel aktivitas terbaru dalam viewport gulir vertikal.
- **Adopsi Hewan (`user.adoption`)**: Katalog visual berupa grid kartu hewan responsif (`ResponsiveCardGrid`) dengan pencarian dan 5 filter pil jenis berikon (*Semua*, *Kucing*, *Anjing*, *Kelinci*, *Lainnya*), inspektor detail + foto hewan, formulir pengajuan adopsi modal (`AdoptionFormDialog`), dan riwayat pengajuan beserta pembatalan mandiri untuk status `pending`.
- **Toko PawPaw (`user.shop`)**: Katalog visual grid kartu produk responsif dengan tombol tambah cepat ke keranjang, pencarian dan 5 filter pil kategori (*Semua*, *Makanan*, *Perawatan*, *Mainan*, *Perlengkapan*), inspektor pratinjau + keranjang persisten per sesi yang dapat digulir dengan validasi stok waktu-nyata, checkout transaksional (`CheckoutDialog`) dengan opsi *Ambil di Toko* atau *Pengantaran* (`Rp 15.000`), riwayat pesanan, pembatalan `pending`, dan struk modal (`OrderInvoiceDialog`).
- **Layanan Kesehatan (`user.health`)**: Grid kartu layanan klinik pastel dengan ikon vektor, biaya, dan chip jadwal jam praktik (WIB), pilihan hewan peliharaan atau entri hewan baru otomatis, pemilihan tanggal dan slot jam dinamis berdasarkan sisa kuota sesi, riwayat booking, pembatalan `pending`, serta kartu janji temu (`BookingCardDialog`).
- **Profil & Hewan Saya (`user.profile`)**: Header hero `#D6E8FF` dengan avatar melingkar berbingkai `#FFD6D6` dan 3 chip statistik langsung, pembaruan biodata akun, unggah foto avatar (disalin ke `uploads/avatars/`), CRUD hewan peliharaan milik sendiri, dan ubah kata sandi.

### Ruang Kerja Administrator (7 Halaman + Pengalih Mode)
Seluruh halaman admin menggunakan palet `admin.css` (`Theme.Admin`), tombol aksi pastel (`Detail`, `Edit`, `Hapus`), dan badge pil `#ID` (`IdPillRenderer`):
- **Dashboard Admin (`admin.dashboard`)**: Kartu sorotan **Total Pendapatan**, 6 kartu KPI operasional (Produk, Pesanan, Hewan Adopsi, Pengajuan Adopsi, Booking Kesehatan, Pelanggan), tombol **Aksi Cepat** (`+ Produk`, `+ Hewan`, `+ Layanan`), dan 3 tab aktivitas terbaru dengan pil `#ID`.
- **Manajemen Produk (`admin.products`)**: Pencarian, filter kategori, tabel sortable dengan pil `#ID`, pratinjau foto, modal tambah/edit bergulir (`ProductFormDialog`), unggah foto ke `uploads/products/`, serta penghapusan yang menjaga integritas FK terhadap pesanan.
- **Manajemen Pesanan (`admin.orders`)**: Filter status & pencarian kode/pelanggan/penerima, inspektor detail pengiriman & pembayaran, tabel item pesanan, modal **Lihat Struk**, pembaruan status (`pending` → `paid` → `processing` → `shipped` → `completed` / `cancelled`), penguncian status final (`completed`/`cancelled`), dan konfirmasi pemulihan stok + penandaan pembayaran `GAGAL` saat membatalkan pesanan aktif.
- **Manajemen Hewan Adopsi (`admin.animals`)**: Pencarian, filter jenis & status ketersediaan, tabel dengan pil `#ID`, pratinjau foto, modal tambah/edit bergulir (`AnimalFormDialog`), unggah foto ke `uploads/animals/`, dan penghapusan.
- **Verifikasi Adopsi (`admin.adoptions`)**: Inspektor detail pemohon & status ketersediaan hewan secara langsung, input **Catatan Admin**, aksi `Setujui`, `Tolak`, dan `Pending`, penguncian status final (`approved`/`cancelled`), serta persetujuan transaksional yang otomatis mengubah status hewan menjadi `adopted` dan menolak pengajuan `pending` lainnya untuk hewan yang sama.
- **Layanan Kesehatan (`admin.services`)**: Manajemen layanan klinik, dokter praktik, hari praktik, daftar slot jam (`HH:mm`), kuota harian per sesi, biaya, modal tambah/edit (`ServiceFormDialog`), dan penghapusan terjaga FK terhadap booking.
- **Booking Kesehatan (`admin.bookings`)**: Filter status & pencarian, inspektor detail pemilik/hewan/pembayaran, kolom **Alasan Penolakan**, pembaruan status (`pending`, `approved`, `completed`, `rejected`) dengan tampilan status final `Dibatalkan (cancelled)` read-only yang tersinkronisasi dengan kuota & status `payment`, **Kartu Booking**, dan **Hapus Booking**.
- **Pengalih Mode (`Mode Pelanggan` ↔ `Kembali ke Panel Admin`)**: Admin dapat meninjau tampilan pelanggan kapan saja lalu kembali ke panel admin tanpa logout (otomatis beralih antara palet `app.css` dan `admin.css`).

---

## Struktur Direktori & Paket

```text
Pawpaw-petcare/
├── Build-Windows.bat                 # Kompilasi src/main/java + salin resources ke bin/
├── Import-Database-Windows.bat       # Impor database/pet_care.sql ke MySQL lokal
├── Jalankan-Windows.bat              # Build lalu jalankan pawpaw.Main
├── Run-Tests-Windows.bat             # Kompilasi & jalankan seluruh 11 test suite (fail-fast)
├── database/
│   └── pet_care.sql                  # Skema lengkap & data seed (10-20 baris per tabel)
├── lib/
│   ├── jbcrypt-0.4.jar               # Hash & verifikasi BCrypt ($2a$/$2y$/$2b$)
│   └── mysql-connector-j-26.10.0.jar # Driver JDBC MySQL
├── .vscode/
│   ├── launch.json                   # Konfigurasi Run/Debug "PawPaw Swing"
│   ├── settings.json
│   └── tasks.json                    # Task Build & Jalankan Semua Test
├── src/
│   ├── main/
│   │   ├── java/pawpaw/
│   │   │   ├── Main.java             # Entry point Swing, UIManager defaults, AppWindow
│   │   │   ├── config/               # AppConfig, DatabaseConfig
│   │   │   ├── model/                # POJO domain & SessionState
│   │   │   ├── dao/                  # Akses JDBC (User, Pet, Animal, Adoption, Product, Order, Payment, Health, Dashboard)
│   │   │   ├── service/              # Logika bisnis & transaksi atomik
│   │   │   ├── util/                 # Theme (dual-palette & Poppins), VectorIcons, FormatUtil, ImageStore, PasswordUtil
│   │   │   └── gui/
│   │   │       ├── auth/             # AuthPanel (Masuk & Daftar, maskot kucing interaktif, flip card, Ingat saya)
│   │   │       ├── common/           # AppWindow, DesktopShell, IdPillRenderer, PagePanel, PhotoPanel, ResponsiveCardGrid, StatusBadgeRenderer, UiKit
│   │   │       ├── user/             # Workspace visual & dialog pelanggan (WorkspaceWidgets + 5 halaman + 4 dialog)
│   │   │       └── admin/            # Dashboard, master CRUD, dan transaksi admin (AdminWidgets + 7 halaman + 3 dialog CRUD)
│   │   └── resources/
│   │       ├── db.properties         # Konfigurasi URL, user, password JDBC MySQL
│   │       ├── fonts/                # Font bundel Poppins (Regular, Medium, SemiBold, Bold) + lisensi OFL.txt
│   │       └── images/               # Aset gambar hewan, layanan, dan produk (images/shop/)
│   └── test/
│       └── java/pawpaw/              # TestAssert & 11 suite unit, integrasi DB, dan UI smoke test
```

> Catatan arsitektur: Seluruh kode UI di `src/main/java/pawpaw/gui/` hanya berinteraksi melalui lapisan `service/` dan `model/` — nol impor `java.sql.*` dan pengemasan aset font `Poppins` ke `bin/fonts/` diverifikasi otomatis oleh `BuildLayoutTest`, serta nol karakter emoji mentah pada komponen UI diverifikasi oleh UI smoke test.

---

## Setup Database (`pet_care`)

1. Pastikan **MySQL / MariaDB** (misalnya melalui XAMPP) berjalan pada `127.0.0.1:3306`.
2. Jalankan skrip impor otomatis dari root repositori:
   ```bat
   Import-Database-Windows.bat
   ```
   Skrip ini mendeteksi `mysql.exe` di `D:\XAMPP\mysql\bin`, `C:\xampp\mysql\bin`, atau `PATH` lalu mengimpor `database\pet_care.sql` dengan charset `utf8mb4`.
   *(Untuk eksekusi non-interaktif tanpa `pause`, jalankan `Import-Database-Windows.bat --no-pause`.)*
3. Sesuaikan koneksi database pada `src/main/resources/db.properties` bila berbeda dari default:
   ```properties
   db.url=jdbc:mysql://127.0.0.1:3306/pet_care?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta&characterEncoding=UTF-8
   db.user=root
   db.password=
   ```

### Akun Bawaan (Seed `database/pet_care.sql`)

`PasswordUtil.verify` mendukung verifikasi hash BCrypt format Laravel (`$2y$` / `$2b$`) maupun standar jBCrypt (`$2a$`). Akun seed yang tersedia pada `database/pet_care.sql`:

| Peran | Email | Username | Kata Sandi Seed |
|---|---|---|---|
| **Admin** | `admin@pawpaw.com` | `admin_pawpaw` | `admin123` |
| **Pelanggan** | `jesika@pawpaw.com` *(dan akun pelanggan seed lain seperti `arin@pawpaw.com`, `arini@pawpaw.com`)* | `jesika_putri` | `password123` |

Jika ingin mengganti kata sandi akun seed langsung di MySQL, buat hash BCrypt baru (mis. melalui `PasswordUtil.hash(...)` atau PHP CLI) lalu perbarui kolom `user.password` di database `pet_care`. Anda juga dapat mendaftarkan akun pelanggan baru langsung dari tampilan **Daftar** pada kartu autentikasi.

---

## Build, Menjalankan & Pengujian

### Prasyarat
- **JDK 25** (tersedia `java` dan `javac` di `PATH`).
- **MySQL** aktif dengan database `pet_care` terimpor (khusus pengujian integrasi DB dan UI live).

### Perintah Windows (Command Prompt / PowerShell)

1. **Build proyek:**
   ```bat
   cmd /c Build-Windows.bat
   ```
   Mengompilasi seluruh file di `src\main\java\` dengan encoding UTF-8 ke `bin\` dan menyalin seluruh `src\main\resources\` (termasuk `fonts\` dan `images\`) ke `bin\`.

2. **Jalankan aplikasi desktop:**
   ```bat
   cmd /c Jalankan-Windows.bat
   ```
   *(Atau klik dua kali `Jalankan-Windows.bat` dari File Explorer.)*

3. **Jalankan seluruh 11 test suite (fail-fast):**
   ```bat
   cmd /c Run-Tests-Windows.bat
   ```
   Suite yang dijalankan berurutan:
   - `pawpaw.BuildLayoutTest`
   - `pawpaw.CoreUtilAndConfigTest`
   - `pawpaw.AuthAndProfileServiceTest`
   - `pawpaw.AdoptionServiceTest`
   - `pawpaw.ShopServiceTest`
   - `pawpaw.HealthAndDashboardServiceTest`
   - `pawpaw.ShellAndAuthUiSmokeTest`
   - `pawpaw.UserCorePanelsSmokeTest`
   - `pawpaw.UserShopHealthPanelsSmokeTest`
   - `pawpaw.AdminMasterPanelsSmokeTest`
   - `pawpaw.AdminTransactionPanelsSmokeTest`

   Untuk menjalankan satu suite tertentu:
   ```bat
   cmd /c Run-Tests-Windows.bat pawpaw.AdminTransactionPanelsSmokeTest
   ```

4. **Pemeriksaan kompilasi ketat (`-Xlint:all -Werror`, opsional untuk CI/verifikasi):**
   ```bat
   cmd /c "dir /s /b src\main\java\*.java > sources.txt && javac -encoding UTF-8 -Xlint:all -Werror -cp lib/* -d bin @sources.txt && del /q sources.txt"
   cmd /c "dir /s /b src\test\java\*.java > test-sources.txt && javac -encoding UTF-8 -Xlint:all -Werror -cp bin;lib/* -d bin-test @test-sources.txt && del /q test-sources.txt"
   ```

### Menjalankan di VS Code
1. Buka folder root repositori di VS Code (yang memuat folder `.vscode`).
2. Pasang rekomendasi ekstensi Java (`vscjava.vscode-java-pack`).
3. Tekan **F5** (konfigurasi **PawPaw Swing** di `.vscode/launch.json`) untuk menjalankan `pawpaw.Main`, atau jalankan Task **PawPaw: Build tanpa Maven** / **PawPaw: Jalankan Semua Test** dari `.vscode/tasks.json`.

---

## Catatan Runtime Upload & Aset

- Font bundel `Poppins` (`Poppins-Regular.ttf`, `Poppins-Medium.ttf`, `Poppins-SemiBold.ttf`, `Poppins-Bold.ttf`, dan lisensi `OFL.txt`) disimpan di `src/main/resources/fonts/`, disalin ke `bin/fonts/` oleh `Build-Windows.bat`, dan dimuat otomatis oleh `Theme`.
- Gambar bawaan disimpan pada `src/main/resources/images/` dan dimuat melalui `ImageStore`.
- Foto baru yang diunggah melalui panel profil atau form admin (produk/hewan) disalin secara aman ke folder runtime `uploads/` (`uploads/avatars/`, `uploads/products/`, `uploads/animals/`).
- Jika file gambar tertentu belum tersedia atau gagal dimuat, `PhotoPanel` menampilkan placeholder ikon vektor secara bersih tanpa error.
