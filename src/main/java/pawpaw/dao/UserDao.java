package pawpaw.dao;

import pawpaw.config.DatabaseConfig;
import pawpaw.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * JDBC Data Access Object for the {@code user} table.
 */
public class UserDao {

    private static final String BASE_SELECT =
        "SELECT user_id, username, name, email, password, role, phone, address, bio, avatar, "
            + "remember_token, created_at, updated_at FROM `user` ";

    private enum UniqueField {
        EMAIL("email"),
        USERNAME("username");

        private final String column;

        UniqueField(String column) {
            this.column = column;
        }
    }

    public Optional<User> findByEmailOrUsername(String identity) {
        if (identity == null || identity.isBlank()) {
            return Optional.empty();
        }
        String trimmed = identity.trim();
        String sql = BASE_SELECT + "WHERE LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?) LIMIT 1";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trimmed);
            ps.setString(2, trimmed);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw dbError("gagal mencari pengguna berdasarkan email atau username", ex);
        }
    }

    public Optional<User> findById(int userId) {
        if (userId <= 0) {
            return Optional.empty();
        }
        String sql = BASE_SELECT + "WHERE user_id = ? LIMIT 1";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw dbError("gagal mencari pengguna berdasarkan ID", ex);
        }
    }

    public boolean existsByEmail(String email, Integer excludeUserId) {
        return existsByField(UniqueField.EMAIL, email, excludeUserId);
    }

    public boolean existsByUsername(String username, Integer excludeUserId) {
        return existsByField(UniqueField.USERNAME, username, excludeUserId);
    }

    public User insert(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Data pengguna tidak boleh null.");
        }
        String sql = "INSERT INTO `user` (username, name, email, password, role, phone, address, bio, avatar, remember_token) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, (user.getRole() == null || user.getRole().isBlank()) ? "user" : user.getRole());
            ps.setString(6, user.getPhone());
            ps.setString(7, user.getAddress());
            ps.setString(8, user.getBio());
            ps.setString(9, user.getAvatar());
            ps.setString(10, user.getRememberToken());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal menyimpan pengguna baru ke database.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Gagal memperoleh ID pengguna baru.");
                }
                int generatedId = keys.getInt(1);
                user.setUserId(generatedId);
                return findById(generatedId).orElse(user);
            }
        } catch (SQLException ex) {
            throw dbError("gagal menyimpan pengguna baru", ex);
        }
    }

    public void updateProfile(User user) {
        if (user == null || user.getUserId() <= 0) {
            throw new IllegalArgumentException("Data pengguna untuk pembaruan profil tidak valid.");
        }
        String sql = "UPDATE `user` SET name = ?, username = ?, email = ?, phone = ?, address = ?, bio = ?, avatar = ? "
            + "WHERE user_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getAddress());
            ps.setString(6, user.getBio());
            ps.setString(7, user.getAvatar());
            ps.setInt(8, user.getUserId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Pengguna tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui profil pengguna", ex);
        }
    }

    public void updatePassword(int userId, String passwordHash) {
        if (userId <= 0 || passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Data pembaruan kata sandi tidak valid.");
        }
        String sql = "UPDATE `user` SET password = ? WHERE user_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Pengguna tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui kata sandi", ex);
        }
    }

    public int countCustomers() {
        String sql = "SELECT COUNT(*) FROM `user` WHERE role = 'user'";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw dbError("gagal menghitung jumlah pelanggan", ex);
        }
    }

    /**
     * Deletes a user row by ID (primarily for integration test cleanup).
     */
    public void deleteById(int userId) {
        if (userId <= 0) {
            return;
        }
        String sql = "DELETE FROM `user` WHERE user_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw dbError("gagal menghapus pengguna", ex);
        }
    }

    private boolean existsByField(UniqueField field, String value, Integer excludeUserId) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        String sql = excludeUserId == null
            ? "SELECT 1 FROM `user` WHERE LOWER(" + field.column + ") = ? LIMIT 1"
            : "SELECT 1 FROM `user` WHERE LOWER(" + field.column + ") = ? AND user_id <> ? LIMIT 1";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, normalized);
            if (excludeUserId != null) {
                ps.setInt(2, excludeUserId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw dbError("gagal memeriksa keunikan " + field.column, ex);
        }
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setPhone(rs.getString("phone"));
        user.setAddress(rs.getString("address"));
        user.setBio(rs.getString("bio"));
        user.setAvatar(rs.getString("avatar"));
        user.setRememberToken(rs.getString("remember_token"));
        user.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        user.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return user;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static IllegalStateException dbError(String action, SQLException ex) {
        return new IllegalStateException("Kesalahan database (" + action + "): " + ex.getMessage(), ex);
    }
}
