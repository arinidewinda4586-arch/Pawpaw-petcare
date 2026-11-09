# PawPaw Pet Care — GUI gabungan (javax.swing)

Satu aplikasi desktop yang menggabungkan seluruh halaman: Login, Beranda, Adopsi (daftar, detail, form),
Toko, Keranjang, Checkout, dan Kesehatan. Memakai Swing/AWT saja; tidak perlu Maven atau library eksternal.

## Menjalankan di Windows / VS Code

1. Pasang JDK 17 atau lebih baru (`java -version` dan `javac -version` harus berjalan).
2. Buka folder ini (yang langsung berisi `.vscode`) di VS Code, pasang Extension Pack for Java.
3. Buka `src/pawpaw/MainApp.java`, klik **Run** (atau F5, konfigurasi **PawPaw Swing**).

Alternatif: klik dua kali `Jalankan-Windows.bat` (kompilasi, salin gambar ke `bin`, lalu jalankan).
Tutup GUI sebelum menjalankan ulang.

## Alur aplikasi

Login (email dan sandi apa pun yang tidak kosong) → Beranda → menu sidebar / kartu layanan:

| Menu | Halaman | Keterangan |
|---|---|---|
| Beranda | HomeView | Kartu layanan terhubung ke Adopsi, Toko, dan Kesehatan |
| Adopsi | AdoptionPage → AdoptionDetailPage → AdoptionForm | Filter kategori, detail hewan, form pengajuan |
| Toko | TokoPage → KeranjangPage → CheckoutPage | Cari/filter produk, tambah ke keranjang, ubah jumlah, buat pesanan |
| Kesehatan | KesehatanPage | Pilih layanan, form booking, daftar appointment |
| Profil | — | Belum tersedia |
| Logout | LoginView | Mengakhiri sesi; halaman lain tidak bisa dibuka tanpa login |

Klik logo untuk memperkecil/memperluas sidebar. Jendela sempit memakai menu ☰. Konten panjang bisa digulir.

## Struktur

| Lokasi | Fungsi |
|---|---|
| `src/pawpaw/MainApp.java` | JFrame, sesi login, navigasi semua halaman, helper tampilan, pemuat gambar |
| `src/pawpaw/HalamanDasar.java` | Kerangka bersama (promo, sidebar, banner, scroll, adaptif); diwarisi semua halaman setelah login |
| `src/pawpaw/LoginView.java`, `UserSession.java` | Login dan sesi dummy di memori |
| `src/pawpaw/HomeView.java` | Beranda |
| `src/pawpaw/Adoption*.java` | Adopsi, detail, form |
| `src/pawpaw/TokoPage.java`, `KeranjangPage.java`, `CheckoutPage.java` | Belanja |
| `src/pawpaw/KesehatanPage.java` | Booking layanan kesehatan |
| `src/pawpaw/DataStore.java`, `Produk.java`, `ItemKeranjang.java` | Data sementara di memori (hilang saat ditutup) |
| `resources/images/` | Semua gambar, satu salinan saja |
| `resources/images/shop/` | Foto produk toko |
| `arsip/` | File lama yang tidak ikut dikompilasi (lihat `arsip/CATATAN.txt`) |
| `.vscode`, `Build-Windows.bat`, `Jalankan-Windows.bat` | Pengaturan Run dan build |

## Gambar yang belum ada

Aplikasi tetap jalan; bagian ini menampilkan kotak abu-biru bergambar telapak kaki sampai file ditambahkan:

- Foto hewan adopsi di `resources/images/`: `anggora.jpg`, `goat.jpg`, `bengal.jpg`, `rabbit.jpg`
- Foto produk di `resources/images/shop/`: `shampoo-kucing.png`

## Catatan

Tidak ada database atau autentikasi sungguhan; semua data tersimpan di memori. Menu Profil, Riwayat Pesanan,
Daftar, Lupa sandi, dan media sosial menampilkan pesan "belum tersedia".
