package pawpaw.service;

import pawpaw.config.AppConfig;
import pawpaw.config.DatabaseConfig;
import pawpaw.dao.OrderDao;
import pawpaw.dao.PaymentDao;
import pawpaw.dao.ProductDao;
import pawpaw.model.CartItem;
import pawpaw.model.Order;
import pawpaw.model.OrderDetail;
import pawpaw.model.Payment;
import pawpaw.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Business service managing product catalog, shopping cart validation, checkout transactions, and order lifecycle.
 */
public class ShopService {

    private static final Set<String> VALID_DELIVERY_METHODS = Set.of("pickup", "delivery");
    private static final Set<String> VALID_ORDER_STATUSES = Set.of(
        "pending", "paid", "processing", "shipped", "completed", "cancelled"
    );
    private static final BigDecimal MAX_DECIMAL_AMOUNT = new BigDecimal("9999999999.99");
    private static final DateTimeFormatter ORDER_CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String ORDER_CODE_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProductDao productDao;
    private final OrderDao orderDao;
    private final PaymentDao paymentDao;

    public ShopService() {
        this(new ProductDao(), new OrderDao(), new PaymentDao());
    }

    public ShopService(ProductDao productDao, OrderDao orderDao, PaymentDao paymentDao) {
        this.productDao = Objects.requireNonNull(productDao, "ProductDao tidak boleh null.");
        this.orderDao = Objects.requireNonNull(orderDao, "OrderDao tidak boleh null.");
        this.paymentDao = Objects.requireNonNull(paymentDao, "PaymentDao tidak boleh null.");
    }

    public List<Product> listProducts() {
        return productDao.findAll();
    }

    public Product createProduct(Product p) {
        Product normalized = validateAndNormalizeProduct(p, "", null);
        return productDao.insert(normalized);
    }

    public void updateProduct(Product p) {
        if (p == null || p.getProductId() <= 0) {
            throw new IllegalArgumentException("ID produk tidak valid.");
        }
        Product existing = productDao.findById(p.getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Data produk tidak ditemukan."));
        Product normalized = validateAndNormalizeProduct(p, existing.getDescription(), existing.getSlug());
        productDao.update(normalized);
    }

    public void deleteProduct(int productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException("ID produk tidak valid.");
        }
        if (productDao.hasOrderDetails(productId)) {
            throw new IllegalStateException(
                "Produk tidak dapat dihapus karena sudah tercatat dalam riwayat pesanan pelanggan."
            );
        }
        productDao.delete(productId);
    }

    public void validateCartQuantity(int productId, int desiredQty) {
        if (productId <= 0) {
            throw new IllegalArgumentException("ID produk tidak valid.");
        }
        if (desiredQty <= 0) {
            throw new IllegalArgumentException("Jumlah produk harus minimal 1.");
        }
        Product product = productDao.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Produk tidak ditemukan."));
        if (product.getStock() <= 0) {
            throw new IllegalStateException("Stok produk " + product.getProductName() + " sedang habis.");
        }
        if (desiredQty > product.getStock()) {
            throw new IllegalStateException(
                "Stok produk " + product.getProductName() + " hanya tersisa " + product.getStock() + "."
            );
        }
    }

    public Order checkout(
            int userId,
            List<CartItem> cart,
            String recipientName,
            String recipientPhone,
            String deliveryMethod,
            String shippingAddress,
            String paymentMethod) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID pengguna tidak valid.");
        }
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Keranjang belanja tidak boleh kosong.");
        }

        // Aggregate duplicate productId entries and sort ascending to enforce deadlock-free lock order.
        Map<Integer, Integer> qtyByProductId = aggregateSortedCart(cart);

        String cleanName = AuthService.requireNonBlank(
            recipientName,
            "Nama penerima wajib diisi.",
            150,
            "Nama penerima maksimal 150 karakter."
        );
        String cleanPhone = AuthService.requireNonBlank(
            recipientPhone,
            "Nomor telepon penerima wajib diisi.",
            30,
            "Nomor telepon maksimal 30 karakter."
        );

        String cleanDelivery = normalizeToken(deliveryMethod);
        if (!VALID_DELIVERY_METHODS.contains(cleanDelivery)) {
            throw new IllegalArgumentException("Metode pengiriman harus salah satu dari: pickup, delivery.");
        }

        String cleanAddress;
        if ("delivery".equals(cleanDelivery)) {
            cleanAddress = AuthService.requireNonBlank(
                shippingAddress,
                "Alamat pengantaran wajib diisi jika memilih metode pengantaran.",
                1000,
                "Alamat pengantaran maksimal 1000 karakter."
            );
        } else {
            cleanAddress = (shippingAddress == null || shippingAddress.isBlank()) ? "-" : shippingAddress.trim();
        }

        String cleanPaymentMethod = normalizePaymentDisplayMethod(paymentMethod);
        String resolvedPaymentCode = resolvePaymentMethodCode(cleanPaymentMethod);

        return runInTransaction(c -> {
            BigDecimal itemsSubtotal = BigDecimal.ZERO;
            List<OrderDetail> lineItems = new ArrayList<>(qtyByProductId.size());

            for (Map.Entry<Integer, Integer> entry : qtyByProductId.entrySet()) {
                int productId = entry.getKey();
                int qty = entry.getValue();
                Product product = productDao.findByIdForUpdate(c, productId)
                    .orElseThrow(() -> new IllegalArgumentException("Produk #" + productId + " tidak ditemukan."));
                if (product.getStock() < qty) {
                    throw new IllegalStateException(
                        "Stok produk " + product.getProductName()
                            + " tidak mencukupi (tersisa " + product.getStock() + ")."
                    );
                }

                BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                itemsSubtotal = itemsSubtotal.add(lineSubtotal);

                OrderDetail detail = new OrderDetail();
                detail.setProductId(productId);
                detail.setQuantity(qty);
                detail.setUnitPrice(unitPrice);
                detail.setSubtotal(lineSubtotal);
                lineItems.add(detail);
            }

            BigDecimal shippingFee = "delivery".equals(cleanDelivery)
                ? AppConfig.SHIPPING_FEE_DELIVERY
                : BigDecimal.ZERO;
            BigDecimal totalPrice = itemsSubtotal.add(shippingFee);
            LocalDateTime now = LocalDateTime.now();
            String orderCode = generateUniqueOrderCode(c, now);

            Order newOrder = new Order();
            newOrder.setOrderDate(now);
            newOrder.setTotalPrice(totalPrice);
            newOrder.setStatus("pending");
            newOrder.setUserId(userId);
            newOrder.setOrderCode(orderCode);
            newOrder.setRecipientName(cleanName);
            newOrder.setRecipientPhone(cleanPhone);
            newOrder.setDeliveryMethod(cleanDelivery);
            newOrder.setShippingAddress(cleanAddress);
            newOrder.setShippingFee(shippingFee);

            Order savedOrder = orderDao.insertOrder(c, newOrder);

            for (OrderDetail detail : lineItems) {
                detail.setOrderId(savedOrder.getOrderId());
                orderDao.insertDetail(c, detail);
                productDao.adjustStock(c, detail.getProductId(), -detail.getQuantity());
            }

            Payment payment = new Payment();
            payment.setOrderId(savedOrder.getOrderId());
            payment.setBookingId(null);
            payment.setAmount(totalPrice);
            payment.setMethod(resolvedPaymentCode);
            payment.setStatus("BERHASIL");
            payment.setReferenceNumber("PAY-" + orderCode);
            payment.setPaymentMethod(cleanPaymentMethod);
            payment.setPaymentStatus("paid");
            payment.setPaidAt(now);
            payment.setPaymentDate(now);
            paymentDao.insert(c, payment);

            return orderDao.findByIdWithDetails(c, savedOrder.getOrderId()).orElse(savedOrder);
        });
    }

    public List<Order> listUserOrders(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID pengguna tidak valid.");
        }
        return orderDao.findByUserIdWithDetails(userId);
    }

    public List<Order> listAllOrders() {
        return orderDao.findAllWithDetails();
    }

    public Order getOrderInvoice(int orderId, int requesterUserId, boolean isAdmin) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("ID pesanan tidak valid.");
        }
        Order order = orderDao.findByIdWithDetails(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Pesanan tidak ditemukan."));
        if (!isAdmin && order.getUserId() != requesterUserId) {
            throw new SecurityException("Anda tidak memiliki izin melihat struk pesanan milik pengguna lain.");
        }
        return order;
    }

    public void cancelUserOrder(int userId, int orderId) {
        if (userId <= 0 || orderId <= 0) {
            throw new IllegalArgumentException("ID pengguna atau pesanan tidak valid.");
        }

        runInTransaction(c -> {
            Order order = orderDao.findByIdForUpdate(c, orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pesanan tidak ditemukan."));
            if (order.getUserId() != userId) {
                throw new SecurityException("Anda tidak memiliki izin membatalkan pesanan milik pengguna lain.");
            }
            String currentStatus = normalizeToken(order.getStatus());
            if (!"pending".equals(currentStatus)) {
                throw new IllegalStateException(
                    "Hanya pesanan berstatus Menunggu Konfirmasi (pending) yang dapat dibatalkan."
                );
            }
            cancelOrderLocked(c, orderId);
            return null;
        });
    }

    public void updateOrderStatus(int orderId, String targetStatus) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("ID pesanan tidak valid.");
        }
        String cleanTarget = normalizeToken(targetStatus);
        if (!VALID_ORDER_STATUSES.contains(cleanTarget)) {
            throw new IllegalArgumentException(
                "Status pesanan harus salah satu dari: pending, paid, processing, shipped, completed, cancelled."
            );
        }

        runInTransaction(c -> {
            Order order = orderDao.findByIdForUpdate(c, orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pesanan tidak ditemukan."));
            String currentStatus = normalizeToken(order.getStatus());
            if ("completed".equals(currentStatus) || "cancelled".equals(currentStatus)) {
                throw new IllegalStateException(
                    "Pesanan berstatus final (" + currentStatus + ") sudah terkunci dan tidak dapat diubah."
                );
            }

            if ("cancelled".equals(cleanTarget)) {
                cancelOrderLocked(c, orderId);
            } else {
                orderDao.updateStatus(c, orderId, cleanTarget);
            }
            return null;
        });
    }

    private void cancelOrderLocked(Connection c, int orderId) {
        List<OrderDetail> details = orderDao.findDetailsByOrderId(c, orderId);
        Map<Integer, Integer> restoreByProductId = new TreeMap<>();
        for (OrderDetail detail : details) {
            if (detail.getProductId() > 0 && detail.getQuantity() > 0) {
                restoreByProductId.merge(detail.getProductId(), detail.getQuantity(), Math::addExact);
            }
        }
        // Lock product rows in ascending product_id order before adjusting stock.
        for (Map.Entry<Integer, Integer> entry : restoreByProductId.entrySet()) {
            int productId = entry.getKey();
            int restoreQty = entry.getValue();
            productDao.findByIdForUpdate(c, productId);
            productDao.adjustStock(c, productId, restoreQty);
        }
        orderDao.updateStatus(c, orderId, "cancelled");
        paymentDao.updateByOrderId(c, orderId, "GAGAL", "failed");
    }

    private Map<Integer, Integer> aggregateSortedCart(List<CartItem> cart) {
        Map<Integer, Integer> qtyByProductId = new TreeMap<>();
        for (CartItem item : cart) {
            if (item == null || item.getProduct() == null || item.getProduct().getProductId() <= 0) {
                throw new IllegalArgumentException("Item keranjang tidak valid.");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Jumlah item keranjang harus minimal 1.");
            }
            qtyByProductId.merge(item.getProduct().getProductId(), item.getQuantity(), Math::addExact);
        }
        return qtyByProductId;
    }

    private String generateUniqueOrderCode(Connection c, LocalDateTime now) {
        String datePart = ORDER_CODE_DATE_FMT.format(now);
        for (int attempt = 0; attempt < 25; attempt++) {
            StringBuilder suffix = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                suffix.append(ORDER_CODE_CHARS.charAt(RANDOM.nextInt(ORDER_CODE_CHARS.length())));
            }
            String candidate = "ORD-" + datePart + "-" + suffix;
            if (!orderDao.existsByOrderCode(c, candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Gagal menghasilkan kode pesanan unik.");
    }

    private static Product validateAndNormalizeProduct(
            Product input,
            String fallbackDescription,
            String fallbackSlug) {
        if (input == null) {
            throw new IllegalArgumentException("Data produk tidak boleh null.");
        }
        String productName = AuthService.requireNonBlank(
            input.getProductName(),
            "Nama produk wajib diisi.",
            150,
            "Nama produk maksimal 150 karakter."
        );
        String category = AuthService.requireNonBlank(
            input.getCategory(),
            "Kategori produk wajib diisi.",
            100,
            "Kategori produk maksimal 100 karakter."
        );

        if (input.getPrice() == null) {
            throw new IllegalArgumentException("Harga produk wajib diisi.");
        }
        if (input.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Harga produk tidak boleh negatif.");
        }
        if (input.getPrice().compareTo(MAX_DECIMAL_AMOUNT) > 0) {
            throw new IllegalArgumentException("Harga produk melebihi batas maksimal.");
        }
        BigDecimal price = input.getPrice().setScale(2, RoundingMode.HALF_UP);

        if (input.getStock() < 0) {
            throw new IllegalArgumentException("Stok produk tidak boleh negatif.");
        }

        String image = (input.getImage() == null || input.getImage().isBlank())
            ? AppConfig.DEFAULT_PRODUCT_IMAGE
            : input.getImage().trim().replace('\\', '/');
        if (image.length() > 255) {
            throw new IllegalArgumentException("Path gambar produk maksimal 255 karakter.");
        }

        String description = input.getDescription() == null
            ? (fallbackDescription == null ? "" : fallbackDescription)
            : input.getDescription().trim();

        String slug = (input.getSlug() == null || input.getSlug().isBlank())
            ? fallbackSlug
            : input.getSlug().trim();

        Product normalized = new Product();
        normalized.setProductId(input.getProductId());
        normalized.setProductName(productName);
        normalized.setCategory(category);
        normalized.setPrice(price);
        normalized.setStock(input.getStock());
        normalized.setSlug(slug);
        normalized.setImage(image);
        normalized.setDescription(description);
        return normalized;
    }

    private static String normalizePaymentDisplayMethod(String paymentMethod) {
        String clean = (paymentMethod == null || paymentMethod.isBlank())
            ? "Simulasi Checkout"
            : paymentMethod.trim();
        if (clean.length() > 50) {
            throw new IllegalArgumentException("Metode pembayaran maksimal 50 karakter.");
        }
        return clean;
    }

    private static String resolvePaymentMethodCode(String displayMethod) {
        String lower = displayMethod.toLowerCase(Locale.ROOT);
        if (lower.contains("qris")) {
            return "QRIS";
        }
        if (lower.contains("cod") || lower.contains("tunai") || lower.contains("cash")) {
            return "TUNAI";
        }
        if (lower.contains("wallet") || lower.contains("gopay") || lower.contains("ovo") || lower.contains("dana")) {
            return "E_WALLET";
        }
        return "TRANSFER_BANK";
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static <T> T runInTransaction(DatabaseConfig.SqlWork<T> work) {
        try {
            return DatabaseConfig.inTransaction(work);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Kesalahan transaksi toko: " + ex.getMessage(), ex);
        }
    }
}
