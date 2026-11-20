package pawpaw.dao;

import pawpaw.config.DatabaseConfig;
import pawpaw.model.HealthService;

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
import java.util.Optional;

/**
 * JDBC Data Access Object for the {@code health_service} table.
 */
public class HealthServiceDao {

    private static final String BASE_SELECT =
        "SELECT service_id, service_name, price, doctor_name, practice_days, time_slots, "
            + "daily_quota, description, icon, card_color, created_at, updated_at "
            + "FROM health_service ";

    public List<HealthService> findAll() {
        String sql = BASE_SELECT + "ORDER BY service_id ASC";
        List<HealthService> services = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                services.add(mapRow(rs));
            }
            return services;
        } catch (SQLException ex) {
            throw dbError("gagal memuat daftar layanan kesehatan", ex);
        }
    }

    public Optional<HealthService> findById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            return findById(c, id, false);
        } catch (SQLException ex) {
            throw dbError("gagal mencari layanan kesehatan", ex);
        }
    }

    public Optional<HealthService> findByIdForUpdate(Connection c, int id) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (id <= 0) {
            return Optional.empty();
        }
        try {
            return findById(c, id, true);
        } catch (SQLException ex) {
            throw dbError("gagal mengunci data layanan kesehatan", ex);
        }
    }

    public boolean hasBookings(int serviceId) {
        if (serviceId <= 0) {
            return false;
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            return hasBookings(c, serviceId, false);
        } catch (SQLException ex) {
            throw dbError("gagal memeriksa riwayat booking layanan kesehatan", ex);
        }
    }

    public boolean hasBookings(Connection c, int serviceId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (serviceId <= 0) {
            return false;
        }
        try {
            return hasBookings(c, serviceId, true);
        } catch (SQLException ex) {
            throw dbError("gagal memeriksa riwayat booking layanan kesehatan", ex);
        }
    }

    public HealthService insert(HealthService s) {
        if (s == null) {
            throw new IllegalArgumentException("Data layanan kesehatan tidak boleh null.");
        }

        String sql = "INSERT INTO health_service "
            + "(service_name, price, doctor_name, practice_days, time_slots, daily_quota, description, icon, card_color) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindServiceFields(ps, s);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal menyimpan layanan kesehatan baru.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Gagal memperoleh ID layanan kesehatan baru.");
                }
                int generatedId = keys.getInt(1);
                s.setServiceId(generatedId);
                return findById(c, generatedId, false).orElse(s);
            }
        } catch (SQLException ex) {
            throw dbError("gagal menambahkan layanan kesehatan", ex);
        }
    }

    public void update(HealthService s) {
        if (s == null || s.getServiceId() <= 0) {
            throw new IllegalArgumentException("Data pembaruan layanan kesehatan tidak valid.");
        }

        String sql = "UPDATE health_service "
            + "SET service_name = ?, price = ?, doctor_name = ?, practice_days = ?, time_slots = ?, "
            + "daily_quota = ?, description = ?, icon = ?, card_color = ? "
            + "WHERE service_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindServiceFields(ps, s);
            ps.setInt(10, s.getServiceId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data layanan kesehatan tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui layanan kesehatan", ex);
        }
    }

    public void delete(int serviceId) {
        if (serviceId <= 0) {
            throw new IllegalArgumentException("ID layanan kesehatan tidak valid.");
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            delete(c, serviceId);
        } catch (SQLException ex) {
            throw dbError("gagal menghapus layanan kesehatan", ex);
        }
    }

    public void delete(Connection c, int serviceId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (serviceId <= 0) {
            throw new IllegalArgumentException("ID layanan kesehatan tidak valid.");
        }
        String sql = "DELETE FROM health_service WHERE service_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data layanan kesehatan tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal menghapus layanan kesehatan", ex);
        }
    }

    private static void bindServiceFields(PreparedStatement ps, HealthService s) throws SQLException {
        BigDecimal price = s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO;
        String doctorName = nonBlankOrDefault(s.getDoctorName(), "drh. Clara Wijaya");
        String practiceDays = nonBlankOrDefault(s.getPracticeDays(), "Senin - Sabtu");
        String timeSlots = nonBlankOrDefault(s.getTimeSlots(), "09:00, 13:00, 16:00");
        int dailyQuota = s.getDailyQuota() > 0 ? s.getDailyQuota() : 5;
        String icon = nonBlankOrDefault(s.getIcon(), "+");
        String cardColor = nonBlankOrDefault(s.getCardColor(), "card-green");

        ps.setString(1, s.getServiceName());
        ps.setBigDecimal(2, price);
        ps.setString(3, doctorName);
        ps.setString(4, practiceDays);
        ps.setString(5, timeSlots);
        ps.setInt(6, dailyQuota);
        ps.setString(7, s.getDescription());
        ps.setString(8, icon);
        ps.setString(9, cardColor);
    }

    private boolean hasBookings(Connection c, int serviceId, boolean forUpdate) throws SQLException {
        String sql = "SELECT 1 FROM health_booking WHERE service_id = ? LIMIT 1"
            + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Optional<HealthService> findById(Connection c, int id, boolean forUpdate) throws SQLException {
        String sql = BASE_SELECT + "WHERE service_id = ? LIMIT 1" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private static HealthService mapRow(ResultSet rs) throws SQLException {
        HealthService service = new HealthService();
        service.setServiceId(rs.getInt("service_id"));
        service.setServiceName(rs.getString("service_name"));
        BigDecimal price = rs.getBigDecimal("price");
        service.setPrice(price != null ? price : BigDecimal.ZERO);
        service.setDoctorName(rs.getString("doctor_name"));
        service.setPracticeDays(rs.getString("practice_days"));
        service.setTimeSlots(rs.getString("time_slots"));
        service.setDailyQuota(rs.getInt("daily_quota"));
        service.setDescription(rs.getString("description"));
        service.setIcon(rs.getString("icon"));
        service.setCardColor(rs.getString("card_color"));
        service.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        service.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return service;
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static IllegalStateException dbError(String action, SQLException ex) {
        return new IllegalStateException("Kesalahan database (" + action + "): " + ex.getMessage(), ex);
    }
}
