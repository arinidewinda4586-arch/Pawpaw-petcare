package pawpaw;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Akses database PawPaw. Konfigurasi default untuk XAMPP:
 * host localhost, port 3306, database pet_care, user root, password kosong.
 */
public final class Database {
    private static final String URL =
            "jdbc:mysql://localhost:3306/pet_care?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static final class Account {
        public final int id;
        public final String name, email, role;
        Account(int id, String name, String email, String role) {
            this.id = id; this.name = name; this.email = email; this.role = role;
        }
    }

    /** Mendukung hash bcrypt Laravel ($2y$) dari dump SQL. */
    public static Account authenticate(String identity, String password) throws Exception {
        String sql = "SELECT user_id, name, email, password, role FROM `user` " +
                     "WHERE email = ? OR username = ? LIMIT 1";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, identity.trim());
            ps.setString(2, identity.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String hash = rs.getString("password");
                if (hash == null) return null;
                // jBCrypt memakai prefix 2a; hash Laravel umumnya memakai 2y.
                if (hash.startsWith("$2y$")) hash = "$2a$" + hash.substring(4);
                // jBCrypt diperlukan untuk memeriksa hash bcrypt dari database Laravel.
                Class<?> bcrypt = Class.forName("org.mindrot.jbcrypt.BCrypt");
                boolean valid = (Boolean) bcrypt.getMethod("checkpw", String.class, String.class)
                        .invoke(null, password, hash);
                if (!valid) return null;
                return new Account(rs.getInt("user_id"), rs.getString("name"),
                        rs.getString("email"), rs.getString("role"));
            }
        }
    }

    /** Memuat katalog hewan dari tabel animal ke format yang dipakai AdoptionPage. */
    public static List<String[]> loadAnimals() throws SQLException {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT nama, species, breed, gender, age, status, image " +
                     "FROM animal ORDER BY animal_id";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String species = titleCase(rs.getString("species"));
                String status = rs.getString("status").equalsIgnoreCase("available")
                        ? "Tersedia" : rs.getString("status").equalsIgnoreCase("adopted")
                        ? "Sudah Diadopsi" : "Tidak Tersedia";
                String image = rs.getString("image");
                if (image != null) {
                    image = image.replaceFirst("^assets/img/", "");
                    image = image.substring(image.lastIndexOf('/') + 1);
                }
                result.add(new String[] { rs.getString("nama"), species, rs.getString("breed"),
                        rs.getString("gender"), rs.getString("age"), status, image });
            }
        }
        return result;
    }

    /** Memuat katalog produk dari tabel product ke format yang dipakai TokoPage. */
    public static List<Produk> loadProducts() throws SQLException {
        List<Produk> result = new ArrayList<>();
        String sql = "SELECT product_name, category, price, image FROM product ORDER BY product_id";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String category = titleCase(rs.getString("category"));
                String name = rs.getString("product_name");
                String image = rs.getString("image");
                if (image != null) {
                    image = image.substring(image.lastIndexOf('/') + 1);
                }
                String label = category;
                String untuk = category.equalsIgnoreCase("makanan") ? "Makanan hewan peliharaan"
                        : category.equalsIgnoreCase("perawatan") ? "Perlengkapan perawatan"
                        : "Mainan hewan peliharaan";
                result.add(new Produk(name, category, label, untuk,
                        rs.getBigDecimal("price").intValue(), image));
            }
        }
        return result;
    }

    public static void refreshCatalog() throws SQLException {
        List<String[]> animals = loadAnimals();
        List<Produk> products = loadProducts();
        DataStore.daftarHewan.clear();
        DataStore.daftarHewan.addAll(animals);
        DataStore.daftarProduk.clear();
        DataStore.daftarProduk.addAll(products);
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }
}
