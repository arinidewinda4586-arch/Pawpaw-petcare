package pawpaw;

/** Data satu produk (nama, kategori, harga, dan nama file gambar di resources/images/shop). */
public class Produk {
    public String nama;
    public String kategori; // Makanan / Mainan / Perawatan
    public String label;    // tulisan kecil di bawah nama, contoh "Makanan Kucing"
    public String untuk;    // contoh "Untuk Kucing Dewasa"
    public int harga;
    public String gambar;   // nama file di resources/images/shop/

    public Produk(String nama, String kategori, String label, String untuk, int harga, String gambar) {
        this.nama = nama;
        this.kategori = kategori;
        this.label = label;
        this.untuk = untuk;
        this.harga = harga;
        this.gambar = gambar;
    }
}
