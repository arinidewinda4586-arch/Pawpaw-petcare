# PawPaw Pet Care — GUI gabungan (javax.swing)

Satu aplikasi desktop yang menggabungkan seluruh halaman: Login, Beranda, Adopsi (daftar, detail, form),
Toko, Keranjang, Checkout, dan Kesehatan. Memakai Swing/AWT dan JDBC untuk koneksi ke MySQL/MariaDB.

## Koneksi database MySQL

Project ini sudah memiliki kelas `Database.java`. Login memeriksa akun pada tabel `user`,
sedangkan katalog Adopsi dan Toko dibaca dari tabel `animal` dan `product`.

1. Jalankan MySQL dari XAMPP.
2. Buka phpMyAdmin, lalu import file `pet_care.sql` yang kamu lampirkan. Pastikan nama database `pet_care`.
3. Unduh **MySQL Connector/J** dari https://dev.mysql.com/downloads/connector/j/ dan letakkan file JAR-nya di folder `lib`.
4. Unduh **jBCrypt 0.4** dari https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar dan letakkan di folder `lib`.
5. Buka `src/pawpaw/Database.java`. Sesuaikan `USER` dan `PASSWORD` jika konfigurasi MySQL-mu bukan `root` tanpa password.
6. Jalankan aplikasi. Login menerima email atau username yang terdaftar di tabel `user`; password diverifikasi terhadap hash bcrypt di database.

**Catatan akun:** dump SQL hanya berisi hash bcrypt, bukan password aslinya. Jadi password akun seed tidak bisa diketahui dari file SQL. Untuk menetapkan password baru, jalankan perintah berikut dari terminal XAMPP (ganti `passwordbaru` sesuai keinginan):

```bash
php -r "echo password_hash('passwordbaru', PASSWORD_BCRYPT), PHP_EOL;"
```

Salin hash yang dihasilkan, lalu jalankan di phpMyAdmin:

```sql
UPDATE `user`
SET `password` = 'TEMPEL_HASH_DI_SINI'
WHERE `username` = 'arini';
```

Untuk akun admin, ganti kondisi menjadi `WHERE username = 'admin'`.

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
| `src/pawpaw/DataStore.java`, `Produk.java`, `ItemKeranjang.java` | Keranjang dan beberapa riwayat sementara di memori |
| `src/pawpaw/Database.java` | Koneksi JDBC, autentikasi bcrypt, pemuatan katalog hewan dan produk |
| `lib/` | Letakkan MySQL Connector/J dan jBCrypt di sini |
| `resources/images/` | Semua gambar, satu salinan saja |
| `resources/images/shop/` | Foto produk toko |
| `arsip/` | File lama yang tidak ikut dikompilasi (lihat `arsip/CATATAN.txt`) |
| `.vscode`, `Build-Windows.bat`, `Jalankan-Windows.bat` | Pengaturan Run dan build |

## Gambar yang belum ada

Aplikasi tetap jalan; bagian ini menampilkan kotak abu-biru bergambar telapak kaki sampai file ditambahkan:

- Foto hewan adopsi di `resources/images/`: `anggora.jpg`, `goat.jpg`, `bengal.jpg`, `rabbit.jpg`
- Foto produk di `resources/images/shop/`: `shampoo-kucing.png`

## Catatan

Login dan pembacaan katalog sudah memakai database. Keranjang, pengajuan adopsi, booking kesehatan, dan checkout masih memakai penyimpanan sementara di memori pada tahap integrasi ini. Menu Profil, Riwayat Pesanan,
Daftar, Lupa sandi, dan media sosial menampilkan pesan "belum tersedia".
