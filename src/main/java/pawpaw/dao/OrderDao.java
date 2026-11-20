package pawpaw.dao;

import pawpaw.config.DatabaseConfig;
import pawpaw.model.Order;
import pawpaw.model.OrderDetail;
import pawpaw.model.Payment;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC Data Access Object for {@code orders} and {@code order_detail} aggregates.
 */
public class OrderDao {

    private static final String BASE_SELECT =
        "SELECT o.order_id, o.order_date, o.total_price, o.status, o.user_id, o.order_code, "
            + "o.recipient_name, o.recipient_phone, o.delivery_method, o.shipping_address, "
            + "o.shipping_fee, o.created_at, o.updated_at, u.name AS user_name "
            + "FROM orders o "
            + "LEFT JOIN `user` u ON u.user_id = o.user_id ";

    private static final String LOCK_SELECT =
        "SELECT order_id, order_date, total_price, status, user_id, order_code, "
            + "recipient_name, recipient_phone, delivery_method, shipping_address, "
            + "shipping_fee, created_at, updated_at "
            + "FROM orders WHERE order_id = ? LIMIT 1 FOR UPDATE";

    private static final String DETAIL_SELECT =
        "SELECT od.order_detail_id, od.quantity, od.order_id, od.product_id, od.unit_price, "
            + "od.subtotal, od.created_at, od.updated_at, "
            + "p.product_name, p.image AS product_image "
            + "FROM order_detail od "
            + "LEFT JOIN product p ON p.product_id = od.product_id ";

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    public List<Order> findAllWithDetails() {
        String sql = BASE_SELECT + "ORDER BY o.order_date DESC, o.order_id DESC";
        try (Connection c = DatabaseConfig.getConnection()) {
            return queryJoinedOrders(c, sql, null);
        } catch (SQLException ex) {
            throw dbError("gagal memuat semua pesanan", ex);
        }
    }

    public List<Order> findByUserIdWithDetails(int userId) {
        if (userId <= 0) {
            return List.of();
        }
        String sql = BASE_SELECT + "WHERE o.user_id = ? ORDER BY o.order_id DESC";
        try (Connection c = DatabaseConfig.getConnection()) {
            return queryJoinedOrders(c, sql, ps -> ps.setInt(1, userId));
        } catch (SQLException ex) {
            throw dbError("gagal memuat riwayat pesanan pengguna", ex);
        }
    }

    public Optional<Order> findByIdWithDetails(int orderId) {
        if (orderId <= 0) {
            return Optional.empty();
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            return findByIdWithDetails(c, orderId);
        } catch (SQLException ex) {
            throw dbError("gagal memuat detail pesanan", ex);
        }
    }

    public Optional<Order> findByIdWithDetails(Connection c, int orderId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (orderId <= 0) {
            return Optional.empty();
        }
        String sql = BASE_SELECT + "WHERE o.order_id = ? LIMIT 1";
        try {
            List<Order> rows = queryJoinedOrders(c, sql, ps -> ps.setInt(1, orderId));
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (SQLException ex) {
            throw dbError("gagal memuat detail pesanan", ex);
        }
    }

    public Optional<Order> findByIdForUpdate(Connection c, int orderId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (orderId <= 0) {
            return Optional.empty();
        }
        try (PreparedStatement ps = c.prepareStatement(LOCK_SELECT)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapBaseOrderRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw dbError("gagal mengunci data pesanan", ex);
        }
    }

    public boolean existsByOrderCode(Connection c, String orderCode) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (orderCode == null || orderCode.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM orders WHERE order_code = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, orderCode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw dbError("gagal memeriksa kode pesanan", ex);
        }
    }

    public Order insertOrder(Connection c, Order order) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (order == null) {
            throw new IllegalArgumentException("Data pesanan tidak boleh null.");
        }

        LocalDateTime orderDate = order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now();
        order.setOrderDate(orderDate);
        String status = (order.getStatus() == null || order.getStatus().isBlank()) ? "pending" : order.getStatus();
        order.setStatus(status);
        String deliveryMethod = (order.getDeliveryMethod() == null || order.getDeliveryMethod().isBlank())
            ? "pickup"
            : order.getDeliveryMethod();
        order.setDeliveryMethod(deliveryMethod);
        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        order.setShippingFee(shippingFee);
        BigDecimal totalPrice = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        order.setTotalPrice(totalPrice);

        String sql = "INSERT INTO orders "
            + "(order_date, total_price, status, user_id, order_code, recipient_name, recipient_phone, "
            + "delivery_method, shipping_address, shipping_fee) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(orderDate));
            ps.setBigDecimal(2, totalPrice);
            ps.setString(3, status);
            ps.setInt(4, order.getUserId());
            ps.setString(5, order.getOrderCode());
            ps.setString(6, order.getRecipientName());
            ps.setString(7, order.getRecipientPhone());
            ps.setString(8, deliveryMethod);
            ps.setString(9, order.getShippingAddress());
            ps.setBigDecimal(10, shippingFee);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal menyimpan pesanan baru.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Gagal memperoleh ID pesanan baru.");
                }
                order.setOrderId(keys.getInt(1));
                return order;
            }
        } catch (SQLException ex) {
            throw dbError("gagal menyimpan pesanan baru", ex);
        }
    }

    public void insertDetail(Connection c, OrderDetail detail) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (detail == null) {
            throw new IllegalArgumentException("Data item pesanan tidak boleh null.");
        }

        BigDecimal unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal subtotal = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
        String sql = "INSERT INTO order_detail (quantity, order_id, product_id, unit_price, subtotal) "
            + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detail.getQuantity());
            ps.setInt(2, detail.getOrderId());
            ps.setInt(3, detail.getProductId());
            ps.setBigDecimal(4, unitPrice);
            ps.setBigDecimal(5, subtotal);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal menyimpan detail item pesanan.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    detail.setOrderDetailId(keys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw dbError("gagal menyimpan detail pesanan", ex);
        }
    }

    public List<OrderDetail> findDetailsByOrderId(Connection c, int orderId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (orderId <= 0) {
            return List.of();
        }
        String sql = DETAIL_SELECT + "WHERE od.order_id = ? ORDER BY od.order_detail_id ASC";
        List<OrderDetail> details = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    details.add(mapDetailRow(rs));
                }
            }
            return details;
        } catch (SQLException ex) {
            throw dbError("gagal memuat detail item pesanan", ex);
        }
    }

    public void updateStatus(Connection c, int orderId, String status) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (orderId <= 0 || status == null || status.isBlank()) {
            throw new IllegalArgumentException("Data pembaruan status pesanan tidak valid.");
        }
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.trim());
            ps.setInt(2, orderId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Pesanan tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui status pesanan", ex);
        }
    }

    /**
     * Deletes an order row by ID (cascading to {@code order_detail} and {@code payment}; intended for test cleanup).
     */
    public void delete(int orderId) {
        if (orderId <= 0) {
            return;
        }
        String sql = "DELETE FROM orders WHERE order_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw dbError("gagal menghapus pesanan", ex);
        }
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM orders";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw dbError("gagal menghitung jumlah pesanan", ex);
        }
    }

    public BigDecimal sumRevenue() {
        String sql = "SELECT COALESCE(SUM(total_price), 0) FROM orders "
            + "WHERE status IN ('paid', 'processing', 'shipped', 'completed')";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return BigDecimal.ZERO;
            }
            BigDecimal sum = rs.getBigDecimal(1);
            return sum != null ? sum : BigDecimal.ZERO;
        } catch (SQLException ex) {
            throw dbError("gagal menghitung total pendapatan pesanan", ex);
        }
    }

    public List<Order> findRecent(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String sql = BASE_SELECT
            + "ORDER BY COALESCE(o.order_date, o.created_at) DESC, o.order_id DESC LIMIT ?";
        try (Connection c = DatabaseConfig.getConnection()) {
            return queryJoinedOrders(c, sql, ps -> ps.setInt(1, limit));
        } catch (SQLException ex) {
            throw dbError("gagal memuat pesanan terbaru", ex);
        }
    }

    private List<Order> queryJoinedOrders(Connection c, String sql, StatementBinder binder) throws SQLException {
        Map<Integer, Order> ordersById = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order order = mapJoinedOrderRow(rs);
                    ordersById.put(order.getOrderId(), order);
                }
            }
        }
        if (ordersById.isEmpty()) {
            return List.of();
        }

        List<Integer> orderIds = new ArrayList<>(ordersById.keySet());
        attachDetails(c, ordersById, orderIds);
        Map<Integer, Payment> paymentsByOrderId = PaymentDao.findByOrderIds(c, orderIds);
        for (Map.Entry<Integer, Payment> entry : paymentsByOrderId.entrySet()) {
            Order order = ordersById.get(entry.getKey());
            if (order != null) {
                order.setPayment(entry.getValue());
            }
        }
        return new ArrayList<>(ordersById.values());
    }

    private void attachDetails(Connection c, Map<Integer, Order> ordersById, List<Integer> orderIds)
            throws SQLException {
        String placeholders = String.join(",", Collections.nCopies(orderIds.size(), "?"));
        String sql = DETAIL_SELECT + "WHERE od.order_id IN (" + placeholders + ") ORDER BY od.order_detail_id ASC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < orderIds.size(); i++) {
                ps.setInt(i + 1, orderIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderDetail detail = mapDetailRow(rs);
                    Order parent = ordersById.get(detail.getOrderId());
                    if (parent != null) {
                        parent.getDetails().add(detail);
                    }
                }
            }
        }
    }

    private static Order mapBaseOrderRow(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setOrderDate(toLocalDateTime(rs.getTimestamp("order_date")));
        BigDecimal total = rs.getBigDecimal("total_price");
        order.setTotalPrice(total != null ? total : BigDecimal.ZERO);
        order.setStatus(rs.getString("status"));
        order.setUserId(rs.getInt("user_id"));
        order.setOrderCode(rs.getString("order_code"));
        order.setRecipientName(rs.getString("recipient_name"));
        order.setRecipientPhone(rs.getString("recipient_phone"));
        order.setDeliveryMethod(rs.getString("delivery_method"));
        order.setShippingAddress(rs.getString("shipping_address"));
        BigDecimal shippingFee = rs.getBigDecimal("shipping_fee");
        order.setShippingFee(shippingFee != null ? shippingFee : BigDecimal.ZERO);
        order.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        order.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return order;
    }

    private static Order mapJoinedOrderRow(ResultSet rs) throws SQLException {
        Order order = mapBaseOrderRow(rs);
        order.setUserName(rs.getString("user_name"));
        return order;
    }

    private static OrderDetail mapDetailRow(ResultSet rs) throws SQLException {
        OrderDetail detail = new OrderDetail();
        detail.setOrderDetailId(rs.getInt("order_detail_id"));
        detail.setQuantity(rs.getInt("quantity"));
        detail.setOrderId(rs.getInt("order_id"));
        detail.setProductId(rs.getInt("product_id"));
        BigDecimal unitPrice = rs.getBigDecimal("unit_price");
        detail.setUnitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO);
        BigDecimal subtotal = rs.getBigDecimal("subtotal");
        detail.setSubtotal(subtotal != null ? subtotal : BigDecimal.ZERO);
        detail.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        detail.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        detail.setProductName(rs.getString("product_name"));
        detail.setProductImage(rs.getString("product_image"));
        return detail;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static IllegalStateException dbError(String action, SQLException ex) {
        return new IllegalStateException("Kesalahan database (" + action + "): " + ex.getMessage(), ex);
    }
}
