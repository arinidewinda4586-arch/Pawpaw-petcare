package pawpaw.dao;

import pawpaw.config.DatabaseConfig;
import pawpaw.model.HealthBooking;
import pawpaw.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC Data Access Object for the {@code health_booking} table, joining service, user, and payment details.
 */
public class HealthBookingDao {

    private static final DateTimeFormatter SLOT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String BASE_SELECT =
        "SELECT hb.booking_id, hb.booking_date, hb.status, hb.user_id, hb.service_id, hb.pet_id, "
            + "hb.owner_name, hb.pet_name, hb.pet_species, hb.rejection_reason, hb.created_at, hb.updated_at, "
            + "hs.service_name, hs.doctor_name, hs.price AS service_price, "
            + "u.name AS user_name "
            + "FROM health_booking hb "
            + "LEFT JOIN health_service hs ON hs.service_id = hb.service_id "
            + "LEFT JOIN `user` u ON u.user_id = hb.user_id ";

    private static final String LOCK_SELECT =
        "SELECT booking_id, booking_date, status, user_id, service_id, pet_id, "
            + "owner_name, pet_name, pet_species, rejection_reason, created_at, updated_at "
            + "FROM health_booking WHERE booking_id = ? LIMIT 1 FOR UPDATE";

    private static final String PAYMENT_SELECT_PREFIX =
        "SELECT payment_id, method, amount, status, created_at, paid_at, reference_number, "
            + "order_id, booking_id, payment_date, payment_method, payment_status, updated_at "
            + "FROM payment WHERE booking_id IN (";

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    public List<HealthBooking> findAllWithDetails() {
        String sql = BASE_SELECT + "ORDER BY hb.booking_date DESC, hb.booking_id DESC";
        try (Connection c = DatabaseConfig.getConnection()) {
            return queryJoinedBookings(c, sql, null);
        } catch (SQLException ex) {
            throw dbError("gagal memuat semua booking kesehatan", ex);
        }
    }

    public List<HealthBooking> findByUserIdWithDetails(int userId) {
        if (userId <= 0) {
            return List.of();
        }
        String sql = BASE_SELECT + "WHERE hb.user_id = ? ORDER BY hb.booking_date DESC, hb.booking_id DESC";
        try (Connection c = DatabaseConfig.getConnection()) {
            return queryJoinedBookings(c, sql, ps -> ps.setInt(1, userId));
        } catch (SQLException ex) {
            throw dbError("gagal memuat riwayat booking kesehatan pengguna", ex);
        }
    }

    public Optional<HealthBooking> findByIdWithDetails(int bookingId) {
        if (bookingId <= 0) {
            return Optional.empty();
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            return findByIdWithDetails(c, bookingId);
        } catch (SQLException ex) {
            throw dbError("gagal memuat detail booking kesehatan", ex);
        }
    }

    public Optional<HealthBooking> findByIdWithDetails(Connection c, int bookingId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (bookingId <= 0) {
            return Optional.empty();
        }
        String sql = BASE_SELECT + "WHERE hb.booking_id = ? LIMIT 1";
        try {
            List<HealthBooking> rows = queryJoinedBookings(c, sql, ps -> ps.setInt(1, bookingId));
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (SQLException ex) {
            throw dbError("gagal memuat detail booking kesehatan", ex);
        }
    }

    public Optional<Integer> findServiceIdByBookingId(int bookingId) {
        if (bookingId <= 0) {
            return Optional.empty();
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            return findServiceIdByBookingId(c, bookingId);
        } catch (SQLException ex) {
            throw dbError("gagal membaca ID layanan booking kesehatan", ex);
        }
    }

    public Optional<Integer> findServiceIdByBookingId(Connection c, int bookingId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (bookingId <= 0) {
            return Optional.empty();
        }
        String sql = "SELECT service_id FROM health_booking WHERE booking_id = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt("service_id")) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw dbError("gagal membaca ID layanan booking kesehatan", ex);
        }
    }

    public Optional<HealthBooking> findByIdForUpdate(Connection c, int bookingId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (bookingId <= 0) {
            return Optional.empty();
        }
        try (PreparedStatement ps = c.prepareStatement(LOCK_SELECT)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapBaseRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw dbError("gagal mengunci data booking kesehatan", ex);
        }
    }

    public int countActiveInSlot(Connection c, int serviceId, LocalDateTime dateTime) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (serviceId <= 0 || dateTime == null) {
            throw new IllegalArgumentException("Parameter pemeriksaan kuota slot tidak valid.");
        }
        String sql = "SELECT COUNT(*) FROM health_booking "
            + "WHERE service_id = ? AND booking_date = ? AND status IN ('pending', 'approved') FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            ps.setTimestamp(2, Timestamp.valueOf(dateTime));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw dbError("gagal menghitung jumlah booking aktif pada slot", ex);
        }
    }

    public Map<String, Integer> countActiveSlotsForDate(int serviceId, LocalDate date) {
        if (serviceId <= 0 || date == null) {
            return Map.of();
        }
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime nextDayStart = date.plusDays(1).atStartOfDay();
        String sql = "SELECT booking_date FROM health_booking "
            + "WHERE service_id = ? AND booking_date >= ? AND booking_date < ? "
            + "AND status IN ('pending', 'approved')";
        Map<String, Integer> counts = new HashMap<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, serviceId);
            ps.setTimestamp(2, Timestamp.valueOf(dayStart));
            ps.setTimestamp(3, Timestamp.valueOf(nextDayStart));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime dt = toLocalDateTime(rs.getTimestamp("booking_date"));
                    if (dt != null) {
                        String slot = SLOT_TIME_FORMAT.format(dt.toLocalTime());
                        counts.merge(slot, 1, Integer::sum);
                    }
                }
            }
            return counts;
        } catch (SQLException ex) {
            throw dbError("gagal menghitung kuota slot booking per tanggal", ex);
        }
    }

    public HealthBooking insert(Connection c, HealthBooking b) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (b == null) {
            throw new IllegalArgumentException("Data booking kesehatan tidak boleh null.");
        }
        if (b.getBookingDate() == null) {
            throw new IllegalArgumentException("Tanggal dan jam booking tidak boleh null.");
        }

        String status = (b.getStatus() == null || b.getStatus().isBlank()) ? "pending" : b.getStatus().trim();
        b.setStatus(status);

        String sql = "INSERT INTO health_booking "
            + "(booking_date, status, user_id, service_id, pet_id, owner_name, pet_name, pet_species, rejection_reason) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(b.getBookingDate()));
            ps.setString(2, status);
            ps.setInt(3, b.getUserId());
            ps.setInt(4, b.getServiceId());
            if (b.getPetId() != null && b.getPetId() > 0) {
                ps.setInt(5, b.getPetId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, b.getOwnerName());
            ps.setString(7, b.getPetName());
            ps.setString(8, b.getPetSpecies());
            ps.setString(9, b.getRejectionReason());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal menyimpan booking layanan kesehatan baru.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Gagal memperoleh ID booking kesehatan baru.");
                }
                int generatedId = keys.getInt(1);
                b.setBookingId(generatedId);
                return findByIdWithDetails(c, generatedId).orElse(b);
            }
        } catch (SQLException ex) {
            throw dbError("gagal menyimpan booking kesehatan", ex);
        }
    }

    public void updateStatus(Connection c, int bookingId, String status, String rejectionReason) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (bookingId <= 0 || status == null || status.isBlank()) {
            throw new IllegalArgumentException("Data pembaruan status booking kesehatan tidak valid.");
        }
        String sql = "UPDATE health_booking SET status = ?, rejection_reason = ? WHERE booking_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.trim());
            ps.setString(2, rejectionReason);
            ps.setInt(3, bookingId);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Data booking kesehatan tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui status booking kesehatan", ex);
        }
    }

    public void delete(int bookingId) {
        if (bookingId <= 0) {
            throw new IllegalArgumentException("ID booking kesehatan tidak valid.");
        }
        String sql = "DELETE FROM health_booking WHERE booking_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data booking kesehatan tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal menghapus booking kesehatan", ex);
        }
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM health_booking";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw dbError("gagal menghitung jumlah booking kesehatan", ex);
        }
    }

    public List<HealthBooking> findRecent(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String sql = BASE_SELECT
            + "ORDER BY COALESCE(hb.booking_date, hb.created_at) DESC, hb.booking_id DESC LIMIT ?";
        try (Connection c = DatabaseConfig.getConnection()) {
            return queryJoinedBookings(c, sql, ps -> ps.setInt(1, limit));
        } catch (SQLException ex) {
            throw dbError("gagal memuat booking kesehatan terbaru", ex);
        }
    }

    private List<HealthBooking> queryJoinedBookings(Connection c, String sql, StatementBinder binder)
            throws SQLException {
        Map<Integer, HealthBooking> byId = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HealthBooking booking = mapJoinedRow(rs);
                    byId.put(booking.getBookingId(), booking);
                }
            }
        }
        if (byId.isEmpty()) {
            return List.of();
        }
        attachPayments(c, byId, new ArrayList<>(byId.keySet()));
        return new ArrayList<>(byId.values());
    }

    private void attachPayments(Connection c, Map<Integer, HealthBooking> byId, List<Integer> bookingIds)
            throws SQLException {
        String placeholders = String.join(",", Collections.nCopies(bookingIds.size(), "?"));
        String sql = PAYMENT_SELECT_PREFIX + placeholders + ") ORDER BY payment_id DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < bookingIds.size(); i++) {
                ps.setInt(i + 1, bookingIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment payment = mapPaymentRow(rs);
                    Integer bookingId = payment.getBookingId();
                    if (bookingId != null) {
                        HealthBooking booking = byId.get(bookingId);
                        if (booking != null && booking.getPayment() == null) {
                            booking.setPayment(payment);
                        }
                    }
                }
            }
        }
    }

    private static HealthBooking mapBaseRow(ResultSet rs) throws SQLException {
        HealthBooking booking = new HealthBooking();
        booking.setBookingId(rs.getInt("booking_id"));
        booking.setBookingDate(toLocalDateTime(rs.getTimestamp("booking_date")));
        booking.setStatus(rs.getString("status"));
        booking.setUserId(rs.getInt("user_id"));
        booking.setServiceId(rs.getInt("service_id"));

        int petIdVal = rs.getInt("pet_id");
        booking.setPetId(rs.wasNull() ? null : petIdVal);

        booking.setOwnerName(rs.getString("owner_name"));
        booking.setPetName(rs.getString("pet_name"));
        booking.setPetSpecies(rs.getString("pet_species"));
        booking.setRejectionReason(rs.getString("rejection_reason"));
        booking.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        booking.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return booking;
    }

    private static HealthBooking mapJoinedRow(ResultSet rs) throws SQLException {
        HealthBooking booking = mapBaseRow(rs);
        booking.setServiceName(rs.getString("service_name"));
        booking.setDoctorName(rs.getString("doctor_name"));
        BigDecimal servicePrice = rs.getBigDecimal("service_price");
        booking.setServicePrice(servicePrice != null ? servicePrice : BigDecimal.ZERO);
        booking.setUserName(rs.getString("user_name"));
        return booking;
    }

    private static Payment mapPaymentRow(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(rs.getInt("payment_id"));
        payment.setMethod(rs.getString("method"));
        BigDecimal amount = rs.getBigDecimal("amount");
        payment.setAmount(amount != null ? amount : BigDecimal.ZERO);
        payment.setStatus(rs.getString("status"));
        payment.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        payment.setPaidAt(toLocalDateTime(rs.getTimestamp("paid_at")));
        payment.setReferenceNumber(rs.getString("reference_number"));

        int orderIdVal = rs.getInt("order_id");
        payment.setOrderId(rs.wasNull() ? null : orderIdVal);

        int bookingIdVal = rs.getInt("booking_id");
        payment.setBookingId(rs.wasNull() ? null : bookingIdVal);

        payment.setPaymentDate(toLocalDateTime(rs.getTimestamp("payment_date")));
        payment.setPaymentMethod(rs.getString("payment_method"));
        payment.setPaymentStatus(rs.getString("payment_status"));
        payment.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return payment;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static IllegalStateException dbError(String action, SQLException ex) {
        return new IllegalStateException("Kesalahan database (" + action + "): " + ex.getMessage(), ex);
    }
}
