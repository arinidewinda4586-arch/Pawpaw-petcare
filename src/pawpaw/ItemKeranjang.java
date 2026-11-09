package pawpaw;

/** Satu baris di keranjang: produk + jumlahnya. */
public class ItemKeranjang {
    public Produk produk;
    public int jumlah;

    public ItemKeranjang(Produk produk, int jumlah) {
        this.produk = produk;
        this.jumlah = jumlah;
    }

    // Harga untuk baris ini (harga satuan x jumlah)
    public int totalHarga() {
        return produk.harga * jumlah;
    }
}
