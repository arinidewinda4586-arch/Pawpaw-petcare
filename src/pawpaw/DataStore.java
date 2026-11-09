package pawpaw;

import java.util.ArrayList;

/**
 * Tempat menyimpan data sementara (di memori) yang dipakai semua halaman.
 * Data hilang saat program ditutup.
 */
public class DataStore {

    // ====== Adopsi ======
    // Format tiap hewan: {nama, jenis, ras, gender, umur, status, namaFileGambar}
    public static ArrayList<String[]> daftarHewan = new ArrayList<>();

    // Format tiap pengajuan: {nama, ras, gender, umur, tanggal, status, namaFileGambar}
    public static ArrayList<String[]> riwayatAdopsi = new ArrayList<>();

    // ====== Checkout ======
    public static ArrayList<Produk> daftarProduk = new ArrayList<>();
    public static ArrayList<ItemKeranjang> keranjang = new ArrayList<>();

    // Format tiap pesanan: {idPesanan, username, nama, telepon, pengiriman, total, tanggal, daftarItem}
    public static ArrayList<String[]> daftarPesanan = new ArrayList<>();

    // ====== Kesehatan ======
    // Format tiap appointment: {pemilik, namaHewan, jenisHewan, layanan, tanggalWaktu, status}
    public static ArrayList<String[]> daftarAppointment = new ArrayList<>();

    static {
        daftarHewan.add(new String[]{"Siti", "Kucing", "Anggora", "Betina", "1 Tahun", "Tersedia", "anggora.jpg"});
        daftarHewan.add(new String[]{"Jeno", "Lainnya", "African Pygmy", "Jantan", "8 Bulan", "Tersedia", "goat.jpg"});
        daftarHewan.add(new String[]{"Jubariyah", "Kucing", "Bengal", "Betina", "13 Bulan", "Sudah Diadopsi", "bengal.jpg"});
        daftarHewan.add(new String[]{"Mochi", "Kelinci", "Lionhead", "Betina", "6 Bulan", "Tersedia", "rabbit.jpg"});

        // Produk: nama, kategori, label, untuk siapa, harga, nama file gambar (resources/images/shop/)
        daftarProduk.add(new Produk("Whiskas Adult", "Makanan", "Makanan Kucing",
                "Untuk Kucing Dewasa", 75000, "whiskas.png"));
        daftarProduk.add(new Produk("Pedigree Adult", "Makanan", "Makanan Anjing",
                "Untuk Anjing Dewasa", 100000, "pedigree.png"));
        daftarProduk.add(new Produk("Shampoo Anjing", "Perawatan", "Perawatan",
                "Untuk Anjing", 90000, "shampoo-anjing.png"));
        daftarProduk.add(new Produk("Shampoo Kucing", "Perawatan", "Perawatan",
                "Untuk Kucing", 85000, "shampoo-kucing.png"));
        daftarProduk.add(new Produk("Sisir Grooming", "Perawatan", "Perawatan",
                "Untuk Kucing & Anjing", 60000, "sisir-grooming.png"));
        daftarProduk.add(new Produk("Mainan Bintang", "Mainan", "Mainan",
                "Untuk Kucing & Anjing", 35000, "mainan-bintang.png"));

        daftarAppointment.add(new String[] { "agus", "agus", "Kucing", "Booking Jadwal Periksa",
                "10 Oktober 2026, 10.01", "Selesai" });
        daftarAppointment.add(new String[] { "Milo", "Milo", "Kucing", "Jadwalkan Vaksin",
                "5 Oktober 2026, 09.00", "Disetujui" });
        daftarAppointment.add(new String[] { "pawik", "pawik", "Anjing", "Panggil Dokter Ke Rumah",
                "20 September 2026, 14.00", "Selesai" });
    }

    // Jumlah semua barang di keranjang (untuk tombol Keranjang di Toko)
    public static int jumlahItemKeranjang() {
        int n = 0;
        for (ItemKeranjang item : keranjang)
            n += item.jumlah;
        return n;
    }

    // Menambah produk ke keranjang. Kalau sudah ada, jumlahnya +1.
    public static void tambahKeKeranjang(Produk produk) {
        for (ItemKeranjang item : keranjang) {
            if (item.produk == produk) {
                item.jumlah++;
                return;
            }
        }
        keranjang.add(new ItemKeranjang(produk, 1));
    }

    // Subtotal harga semua barang di keranjang
    public static int subtotalKeranjang() {
        int total = 0;
        for (ItemKeranjang item : keranjang) {
            total += item.totalHarga();
        }
        return total;
    }

    // Mengubah angka 100000 menjadi teks "Rp 100.000"
    public static String formatRupiah(int angka) {
        return "Rp " + String.format("%,d", angka).replace(',', '.');
    }

    // Warna latar kartu hewan sesuai jenisnya (sama dengan warna kartu layanan di beranda)
    public static java.awt.Color warnaJenis(String jenis) {
        switch (jenis) {
            case "Kucing":
                return new java.awt.Color(255, 214, 214);
            case "Anjing":
                return new java.awt.Color(214, 232, 255);
            case "Kelinci":
                return new java.awt.Color(216, 245, 208);
            default:
                return new java.awt.Color(232, 217, 255);
        }
    }
}
