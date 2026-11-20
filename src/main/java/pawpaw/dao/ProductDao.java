package pawpaw.dao;

import pawpaw.config.AppConfig;
import pawpaw.config.DatabaseConfig;
import pawpaw.model.Product;
import pawpaw.util.FormatUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JDBC Data Access Object for the {@code product} table.
 */
public class ProductDao {

    private static final String BASE_SELECT =
        "SELECT product_id, product_name, price, stock, category, slug, image, description, created_at, updated_at "
            + "FROM product ";

    private static final int MAX_BASE_SLUG_LENGTH = 135;

    public List<Product> findAll() {
        String sql = BASE_SELECT + "ORDER BY product_id DESC";
        List<Product> products = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
            return products;
        } catch (SQLException ex) {
            throw dbError("gagal memuat daftar produk", ex);
        }
    }

    public Optional<Product> findById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            return findById(c, id, false);
        } catch (SQLException ex) {
            throw dbError("gagal mencari produk", ex);
        }
    }

    public Optional<Product> findByIdForUpdate(Connection c, int id) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (id <= 0) {
            return Optional.empty();
        }
        try {
            return findById(c, id, true);
        } catch (SQLException ex) {
            throw dbError("gagal mengunci data produk", ex);
        }
    }

    public boolean existsBySlug(String slug, Integer excludeId) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            return existsBySlug(c, slug, excludeId);
        } catch (SQLException ex) {
            throw dbError("gagal memeriksa keunikan slug produk", ex);
        }
    }

    public boolean hasOrderDetails(int productId) {
        if (productId <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM order_detail WHERE product_id = ? LIMIT 1";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw dbError("gagal memeriksa riwayat pesanan produk", ex);
        }
    }

    public Product insert(Product p) {
        if (p == null) {
            throw new IllegalArgumentException("Data produk tidak boleh null.");
        }
        String sql = "INSERT INTO product (product_name, price, stock, category, slug, image, description) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConfig.getConnection()) {
            String rawSlug = (p.getSlug() != null && !p.getSlug().isBlank()) ? p.getSlug() : p.getProductName();
            String uniqueSlug = generateUniqueSlug(c, rawSlug, null);
            String image = (p.getImage() == null || p.getImage().isBlank())
                ? AppConfig.DEFAULT_PRODUCT_IMAGE
                : p.getImage();
            BigDecimal price = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;

            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getProductName());
                ps.setBigDecimal(2, price);
                ps.setInt(3, p.getStock());
                ps.setString(4, p.getCategory());
                ps.setString(5, uniqueSlug);
                ps.setString(6, image);
                ps.setString(7, p.getDescription());

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new IllegalStateException("Gagal menyimpan data produk baru.");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new IllegalStateException("Gagal memperoleh ID produk baru.");
                    }
                    int generatedId = keys.getInt(1);
                    p.setProductId(generatedId);
                    p.setSlug(uniqueSlug);
                    return findById(c, generatedId, false).orElse(p);
                }
            }
        } catch (SQLException ex) {
            throw dbError("gagal menambahkan produk", ex);
        }
    }

    public void update(Product p) {
        if (p == null || p.getProductId() <= 0) {
            throw new IllegalArgumentException("Data pembaruan produk tidak valid.");
        }
        String sql = "UPDATE product SET product_name = ?, price = ?, stock = ?, category = ?, "
            + "slug = ?, image = ?, description = ? WHERE product_id = ?";
        try (Connection c = DatabaseConfig.getConnection()) {
            String rawSlug = (p.getSlug() != null && !p.getSlug().isBlank()) ? p.getSlug() : p.getProductName();
            String uniqueSlug = generateUniqueSlug(c, rawSlug, p.getProductId());
            String image = (p.getImage() == null || p.getImage().isBlank())
                ? AppConfig.DEFAULT_PRODUCT_IMAGE
                : p.getImage();
            BigDecimal price = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, p.getProductName());
                ps.setBigDecimal(2, price);
                ps.setInt(3, p.getStock());
                ps.setString(4, p.getCategory());
                ps.setString(5, uniqueSlug);
                ps.setString(6, image);
                ps.setString(7, p.getDescription());
                ps.setInt(8, p.getProductId());

                int affected = ps.executeUpdate();
                if (affected == 0) {
                    throw new IllegalArgumentException("Data produk tidak ditemukan.");
                }
                p.setSlug(uniqueSlug);
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui produk", ex);
        }
    }

    public void adjustStock(Connection c, int productId, int delta) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (productId <= 0) {
            throw new IllegalArgumentException("ID produk tidak valid.");
        }
        String sql = delta < 0
            ? "UPDATE product SET stock = stock + ? WHERE product_id = ? AND stock + ? >= 0"
            : "UPDATE product SET stock = stock + ? WHERE product_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, productId);
            if (delta < 0) {
                ps.setInt(3, delta);
            }
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal memperbarui stok produk #" + productId + ".");
            }
        } catch (SQLException ex) {
            throw dbError("gagal menyesuaikan stok produk", ex);
        }
    }

    public void delete(int productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("ID produk tidak valid.");
        }
        String sql = "DELETE FROM product WHERE product_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data produk tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal menghapus produk", ex);
        }
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM product";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw dbError("gagal menghitung jumlah produk", ex);
        }
    }

    private Optional<Product> findById(Connection c, int id, boolean forUpdate) throws SQLException {
        String sql = BASE_SELECT + "WHERE product_id = ? LIMIT 1" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private boolean existsBySlug(Connection c, String slug, Integer excludeId) throws SQLException {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        String normalized = slug.trim().toLowerCase(Locale.ROOT);
        String sql = excludeId == null
            ? "SELECT 1 FROM product WHERE LOWER(slug) = ? LIMIT 1"
            : "SELECT 1 FROM product WHERE LOWER(slug) = ? AND product_id <> ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalized);
            if (excludeId != null) {
                ps.setInt(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String generateUniqueSlug(Connection c, String rawText, Integer excludeId) throws SQLException {
        String base = FormatUtil.slugify(rawText);
        if (base.isEmpty()) {
            base = "produk";
        }
        if (base.length() > MAX_BASE_SLUG_LENGTH) {
            base = base.substring(0, MAX_BASE_SLUG_LENGTH).replaceAll("-+$", "");
            if (base.isEmpty()) {
                base = "produk";
            }
        }
        String candidate = base;
        int counter = 1;
        while (existsBySlug(c, candidate, excludeId)) {
            candidate = base + "-" + counter;
            counter++;
        }
        return candidate;
    }

    private static Product mapRow(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("product_id"));
        product.setProductName(rs.getString("product_name"));
        BigDecimal price = rs.getBigDecimal("price");
        product.setPrice(price != null ? price : BigDecimal.ZERO);
        product.setStock(rs.getInt("stock"));
        product.setCategory(rs.getString("category"));
        product.setSlug(rs.getString("slug"));
        product.setImage(rs.getString("image"));
        product.setDescription(rs.getString("description"));
        product.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        product.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return product;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static IllegalStateException dbError(String action, SQLException ex) {
        return new IllegalStateException("Kesalahan database (" + action + "): " + ex.getMessage(), ex);
    }
}
