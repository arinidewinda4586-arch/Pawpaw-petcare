package pawpaw.dao;

import pawpaw.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC Data Access Object for the {@code payment} table supporting both order and health booking payments.
 */
public class PaymentDao {

    private static final String BASE_SELECT =
        "SELECT payment_id, method, amount, status, created_at, paid_at, reference_number, "
            + "order_id, booking_id, payment_date, payment_method, payment_status, updated_at "
            + "FROM payment ";

    public Optional<Payment> findByOrderId(Connection c, int orderId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (orderId <= 0) {
            return Optional.empty();
        }
        return findSingle(
            c,
            BASE_SELECT + "WHERE order_id = ? ORDER BY payment_id DESC LIMIT 1",
            orderId,
            "gagal memuat pembayaran pesanan"
        );
    }

    public Optional<Payment> findByBookingId(Connection c, int bookingId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (bookingId <= 0) {
            return Optional.empty();
        }
        return findSingle(
            c,
            BASE_SELECT + "WHERE booking_id = ? ORDER BY payment_id DESC LIMIT 1",
            bookingId,
            "gagal memuat pembayaran booking"
        );
    }

    public Payment insert(Connection c, Payment payment) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (payment == null) {
            throw new IllegalArgumentException("Data pembayaran tidak boleh null.");
        }

        String method = nonBlankOrDefault(payment.getMethod(), "TRANSFER_BANK");
        BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        String status = nonBlankOrDefault(payment.getStatus(), "BERHASIL");
        LocalDateTime paymentDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDateTime.now();
        String paymentMethod = nonBlankOrDefault(payment.getPaymentMethod(), "Simulasi Checkout");
        String paymentStatus = nonBlankOrDefault(payment.getPaymentStatus(), "paid");

        // Always bind paid_at explicitly so null stays SQL NULL rather than triggering MySQL DEFAULT current_timestamp().
        String sql = "INSERT INTO payment "
            + "(method, amount, status, paid_at, reference_number, order_id, booking_id, payment_date, payment_method, payment_status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, method);
            ps.setBigDecimal(2, amount);
            ps.setString(3, status);
            if (payment.getPaidAt() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(payment.getPaidAt()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            ps.setString(5, payment.getReferenceNumber());
            setNullableInt(ps, 6, payment.getOrderId());
            setNullableInt(ps, 7, payment.getBookingId());
            ps.setTimestamp(8, Timestamp.valueOf(paymentDate));
            ps.setString(9, paymentMethod);
            ps.setString(10, paymentStatus);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal menyimpan data pembayaran.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Gagal memperoleh ID pembayaran baru.");
                }
                int generatedId = keys.getInt(1);
                payment.setPaymentId(generatedId);
                return findSingle(
                    c,
                    BASE_SELECT + "WHERE payment_id = ? LIMIT 1",
                    generatedId,
                    "gagal memuat ulang pembayaran baru"
                ).orElse(payment);
            }
        } catch (SQLException ex) {
            throw dbError("gagal menyimpan pembayaran", ex);
        }
    }

    public void updateByOrderId(Connection c, int orderId, String status, String paymentStatus) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (orderId <= 0 || isBlank(status) || isBlank(paymentStatus)) {
            throw new IllegalArgumentException("Parameter pembaruan pembayaran pesanan tidak valid.");
        }
        String sql = "UPDATE payment SET status = ?, payment_status = ? WHERE order_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.trim());
            ps.setString(2, paymentStatus.trim());
            ps.setInt(3, orderId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui pembayaran pesanan", ex);
        }
    }

    public void updateByBookingId(
            Connection c,
            int bookingId,
            String status,
            String paymentStatus,
            boolean markPaidNow,
            boolean clearPaidAt) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (bookingId <= 0 || isBlank(status) || isBlank(paymentStatus)) {
            throw new IllegalArgumentException("Parameter pembaruan pembayaran booking tidak valid.");
        }
        if (markPaidNow && clearPaidAt) {
            throw new IllegalArgumentException("markPaidNow dan clearPaidAt tidak boleh keduanya bernilai true.");
        }

        String sql;
        if (markPaidNow) {
            sql = "UPDATE payment SET status = ?, payment_status = ?, paid_at = CURRENT_TIMESTAMP WHERE booking_id = ?";
        } else if (clearPaidAt) {
            sql = "UPDATE payment SET status = ?, payment_status = ?, paid_at = NULL WHERE booking_id = ?";
        } else {
            sql = "UPDATE payment SET status = ?, payment_status = ? WHERE booking_id = ?";
        }

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.trim());
            ps.setString(2, paymentStatus.trim());
            ps.setInt(3, bookingId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui pembayaran booking", ex);
        }
    }

    static Map<Integer, Payment> findByOrderIds(Connection c, List<Integer> orderIds) throws SQLException {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", Collections.nCopies(orderIds.size(), "?"));
        String sql = BASE_SELECT + "WHERE order_id IN (" + placeholders + ") ORDER BY payment_id DESC";
        Map<Integer, Payment> byOrderId = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < orderIds.size(); i++) {
                ps.setInt(i + 1, orderIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment payment = mapRow(rs);
                    if (payment.getOrderId() != null) {
                        byOrderId.putIfAbsent(payment.getOrderId(), payment);
                    }
                }
            }
        }
        return byOrderId;
    }

    private Optional<Payment> findSingle(Connection c, String sql, int id, String action) {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw dbError(action, ex);
        }
    }

    private static Payment mapRow(ResultSet rs) throws SQLException {
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

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value != null && value > 0) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static IllegalStateException dbError(String action, SQLException ex) {
        return new IllegalStateException("Kesalahan database (" + action + "): " + ex.getMessage(), ex);
    }
}
