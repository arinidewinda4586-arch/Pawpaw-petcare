package pawpaw;

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
import pawpaw.service.ShopService;
import pawpaw.util.FormatUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for ProductDao, OrderDao, PaymentDao, and ShopService against live MySQL pet_care.
 */
public class ShopServiceTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "ShopServiceTest",
            TestAssert.test("Product CRUD validates inputs, auto-generates unique slugs, and cleans up",
                ShopServiceTest::testProductCrudAndSlugGeneration),
            TestAssert.test("checkout validates delivery/fields before DB and rolls back cleanly on insufficient stock",
                ShopServiceTest::testCheckoutValidationAndStockRollback),
            TestAssert.test("checkout delivery flow, delete guard, invoice ownership, and user cancel restore",
                ShopServiceTest::testDeliveryCheckoutDeleteGuardAndUserCancel),
            TestAssert.test("admin order transitions, revenue sum, processing->cancelled restore, and completed lock",
                ShopServiceTest::testAdminTransitionsRevenueAndCompletedLock),
            TestAssert.test("PaymentDao booking lifecycle preserves null paid_at and toggles markPaidNow/clearPaidAt",
                ShopServiceTest::testPaymentDaoBookingPaidAtLifecycle),
            TestAssert.test("concurrent reverse-order checkouts serialize on ascending product_id without deadlock or oversell",
                ShopServiceTest::testConcurrentReverseCartCheckoutsNoDeadlock)
        );
    }

    private static void testProductCrudAndSlugGeneration() {
        ProductDao productDao = new ProductDao();
        OrderDao orderDao = new OrderDao();
        PaymentDao paymentDao = new PaymentDao();
        ShopService shopService = new ShopService(productDao, orderDao, paymentDao);
        List<Integer> cleanupProductIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            int initialCount = productDao.countAll();
            TestAssert.assertTrue(initialCount >= 10, "Seed database must have at least 10 products");

            Product blankName = validTestProduct("Temp-" + tag, new BigDecimal("10000.00"), 5);
            blankName.setProductName("   ");
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.createProduct(blankName),
                "Blank productName must be rejected"
            );

            Product blankCat = validTestProduct("Temp-" + tag, new BigDecimal("10000.00"), 5);
            blankCat.setCategory(" ");
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.createProduct(blankCat),
                "Blank category must be rejected"
            );

            Product negPrice = validTestProduct("Temp-" + tag, new BigDecimal("-1.00"), 5);
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.createProduct(negPrice),
                "Negative price must be rejected"
            );

            Product negStock = validTestProduct("Temp-" + tag, new BigDecimal("10000.00"), -1);
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.createProduct(negStock),
                "Negative stock must be rejected"
            );

            String rawName = "  Snack Kucing Salmon " + tag + "!  ";
            Product firstInput = validTestProduct(rawName, new BigDecimal("45000"), 10);
            firstInput.setImage("   ");
            firstInput.setSlug(null);
            firstInput.setDescription("  Camilan bergizi  ");

            Product p1 = shopService.createProduct(firstInput);
            cleanupProductIds.add(p1.getProductId());

            String expectedBaseSlug = FormatUtil.slugify(rawName);
            TestAssert.assertTrue(p1.getProductId() > 0, "Created product must have generated ID");
            TestAssert.assertEquals("Snack Kucing Salmon " + tag + "!", p1.getProductName(), "Product name must be trimmed");
            TestAssert.assertEquals(expectedBaseSlug, p1.getSlug(), "Slug must be generated via FormatUtil.slugify");
            TestAssert.assertEquals(
                AppConfig.DEFAULT_PRODUCT_IMAGE,
                p1.getImage(),
                "Blank image must default to AppConfig.DEFAULT_PRODUCT_IMAGE"
            );
            TestAssert.assertTrue(productDao.existsBySlug(p1.getSlug(), null), "existsBySlug must find created slug");
            TestAssert.assertFalse(productDao.existsBySlug(p1.getSlug(), p1.getProductId()), "existsBySlug must exclude own ID");

            Product secondInput = validTestProduct("Snack Kucing Salmon " + tag + "!", new BigDecimal("47000"), 8);
            secondInput.setSlug(null);
            Product p2 = shopService.createProduct(secondInput);
            cleanupProductIds.add(p2.getProductId());
            TestAssert.assertEquals(
                expectedBaseSlug + "-1",
                p2.getSlug(),
                "Duplicate product name must append counter suffix to slug"
            );

            TestAssert.assertEquals(initialCount + 2, productDao.countAll(), "Product count must increase by 2");
            TestAssert.assertTrue(
                shopService.listProducts().stream().anyMatch(p -> p.getProductId() == p1.getProductId()),
                "listProducts must contain newly created product"
            );

            p1.setProductName("Snack Kucing Tuna " + tag);
            p1.setPrice(new BigDecimal("48000.00"));
            p1.setStock(12);
            p1.setImage("");
            p1.setDescription(null);
            shopService.updateProduct(p1);

            Optional<Product> reloaded = productDao.findById(p1.getProductId());
            TestAssert.assertTrue(reloaded.isPresent(), "Updated product must exist");
            TestAssert.assertEquals("Snack Kucing Tuna " + tag, reloaded.get().getProductName(), "Updated name must persist");
            assertMoneyEquals(new BigDecimal("48000.00"), reloaded.get().getPrice(), "Updated price must persist");
            TestAssert.assertEquals(12, reloaded.get().getStock(), "Updated stock must persist");
            TestAssert.assertEquals(
                AppConfig.DEFAULT_PRODUCT_IMAGE,
                reloaded.get().getImage(),
                "Blank image on update must default to AppConfig.DEFAULT_PRODUCT_IMAGE"
            );
            TestAssert.assertEquals(
                "Camilan bergizi",
                reloaded.get().getDescription(),
                "Null description on update must preserve existing description"
            );

            shopService.deleteProduct(p2.getProductId());
            cleanupProductIds.remove(Integer.valueOf(p2.getProductId()));
            TestAssert.assertTrue(productDao.findById(p2.getProductId()).isEmpty(), "Deleted product must be gone");

            shopService.deleteProduct(p1.getProductId());
            cleanupProductIds.remove(Integer.valueOf(p1.getProductId()));
            TestAssert.assertEquals(initialCount, productDao.countAll(), "Product count must return to initial count");

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.deleteProduct(p1.getProductId()),
                "Deleting missing product must throw IllegalArgumentException"
            );
        } finally {
            cleanupProducts(productDao, cleanupProductIds);
        }
    }

    private static void testCheckoutValidationAndStockRollback() {
        ProductDao productDao = new ProductDao();
        OrderDao orderDao = new OrderDao();
        PaymentDao paymentDao = new PaymentDao();
        ShopService shopService = new ShopService(productDao, orderDao, paymentDao);
        List<Integer> cleanupProductIds = new ArrayList<>();
        List<Integer> cleanupOrderIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            Product productA = shopService.createProduct(validTestProduct("RollA-" + tag, new BigDecimal("20000.00"), 5));
            cleanupProductIds.add(productA.getProductId());
            Product productB = shopService.createProduct(validTestProduct("RollB-" + tag, new BigDecimal("12000.00"), 2));
            cleanupProductIds.add(productB.getProductId());

            int ordersBefore = orderDao.countAll();

            shopService.validateCartQuantity(productA.getProductId(), 5);
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.validateCartQuantity(productA.getProductId(), 0),
                "validateCartQuantity must reject qty <= 0"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> shopService.validateCartQuantity(productA.getProductId(), 6),
                "validateCartQuantity must reject qty > stock"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.validateCartQuantity(9999999, 1),
                "validateCartQuantity must reject missing product"
            );

            // Pre-DB validation checks (Review Focus #4)
            Product ghost = new Product();
            ghost.setProductId(9999999);
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.checkout(
                    1,
                    List.of(new CartItem(ghost, 1)),
                    "Jesika Putri",
                    "081210000001",
                    "delivery",
                    "   ",
                    "Transfer Bank"
                ),
                "Delivery checkout with blank shipping address must be rejected before DB access"
            );

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.checkout(1, List.of(), "Jesika", "0812", "pickup", "", "QRIS"),
                "Empty cart must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.checkout(1, List.of(new CartItem(productA, 1)), " ", "0812", "pickup", "", "QRIS"),
                "Blank recipientName must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.checkout(1, List.of(new CartItem(productA, 1)), "Jesika", " ", "pickup", "", "QRIS"),
                "Blank recipientPhone must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.checkout(1, List.of(new CartItem(productA, 1)), "Jesika", "0812", "express", "Alamat", "QRIS"),
                "Unsupported deliveryMethod must be rejected"
            );

            // Insufficient stock single item: qty 6 > stock 5 (Review Focus #4)
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> shopService.checkout(
                    1,
                    List.of(new CartItem(productA, 6)),
                    "Jesika Putri",
                    "081210000001",
                    "pickup",
                    "",
                    "QRIS"
                ),
                "Checkout exceeding stock (6 > 5) must throw IllegalStateException"
            );

            // Multi-item rollback: productA qty 2 ok, productB qty 3 > stock 2 -> full rollback
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> shopService.checkout(
                    1,
                    List.of(new CartItem(productA, 2), new CartItem(productB, 3)),
                    "Jesika Putri",
                    "081210000001",
                    "delivery",
                    "Jl. Subrantas No. 10",
                    "Transfer Bank"
                ),
                "Multi-item checkout with one out-of-stock item must roll back completely"
            );

            TestAssert.assertEquals(ordersBefore, orderDao.countAll(), "Failed checkouts must create zero orphan orders");
            TestAssert.assertEquals(5, productDao.findById(productA.getProductId()).orElseThrow().getStock(), "Stock A must remain 5");
            TestAssert.assertEquals(2, productDao.findById(productB.getProductId()).orElseThrow().getStock(), "Stock B must remain 2");
        } finally {
            cleanupOrdersAndProducts(orderDao, productDao, cleanupOrderIds, cleanupProductIds);
        }
    }

    private static void testDeliveryCheckoutDeleteGuardAndUserCancel() {
        ProductDao productDao = new ProductDao();
        OrderDao orderDao = new OrderDao();
        PaymentDao paymentDao = new PaymentDao();
        ShopService shopService = new ShopService(productDao, orderDao, paymentDao);
        List<Integer> cleanupProductIds = new ArrayList<>();
        List<Integer> cleanupOrderIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            Product productA = shopService.createProduct(validTestProduct("ShopA-" + tag, new BigDecimal("25000.00"), 5));
            cleanupProductIds.add(productA.getProductId());

            // Deliberately pass duplicate cart entries for productA (1 + 1 = 2)
            Order order = shopService.checkout(
                1,
                List.of(new CartItem(productA, 1), new CartItem(productA, 1)),
                "  Jesika Putri  ",
                " 081210000001 ",
                "delivery",
                " Jl. Subrantas Panam No. 10 ",
                "Transfer Bank"
            );
            cleanupOrderIds.add(order.getOrderId());

            TestAssert.assertTrue(order.getOrderId() > 0, "Order must have generated orderId");
            TestAssert.assertNotNull(order.getOrderCode(), "Order must have generated orderCode");
            TestAssert.assertTrue(
                order.getOrderCode().matches("ORD-\\d{6}-[A-Z0-9]{6}"),
                "orderCode must match ORD-yyMMdd-XXXXXX, was: " + order.getOrderCode()
            );
            TestAssert.assertEquals("pending", order.getStatus(), "New order must have status 'pending'");
            TestAssert.assertEquals("delivery", order.getDeliveryMethod(), "Delivery method must persist");
            TestAssert.assertEquals("Jl. Subrantas Panam No. 10", order.getShippingAddress(), "Shipping address must be trimmed");
            assertMoneyEquals(AppConfig.SHIPPING_FEE_DELIVERY, order.getShippingFee(), "Delivery shippingFee must be 15000");
            assertMoneyEquals(new BigDecimal("65000.00"), order.getTotalPrice(), "Total must be 2*25000 + 15000 = 65000");
            TestAssert.assertEquals(1, order.getDetails().size(), "Duplicate product entries must merge into 1 OrderDetail");
            OrderDetail detail = order.getDetails().get(0);
            TestAssert.assertEquals(2, detail.getQuantity(), "Merged OrderDetail quantity must be 2");
            assertMoneyEquals(new BigDecimal("25000.00"), detail.getUnitPrice(), "Unit price must match product price");
            assertMoneyEquals(new BigDecimal("50000.00"), detail.getSubtotal(), "Line subtotal must be 50000");

            Payment payment = order.getPayment();
            TestAssert.assertNotNull(payment, "Order must have linked Payment");
            TestAssert.assertEquals("BERHASIL", payment.getStatus(), "Checkout payment status must be BERHASIL");
            TestAssert.assertEquals("paid", payment.getPaymentStatus(), "Checkout paymentStatus must be paid");
            TestAssert.assertEquals("TRANSFER_BANK", payment.getMethod(), "Payment method code must be TRANSFER_BANK");
            TestAssert.assertEquals("Transfer Bank", payment.getPaymentMethod(), "Payment display method must persist");
            TestAssert.assertEquals("PAY-" + order.getOrderCode(), payment.getReferenceNumber(), "Reference number must be PAY-<orderCode>");
            assertMoneyEquals(new BigDecimal("65000.00"), payment.getAmount(), "Payment amount must equal order totalPrice");
            TestAssert.assertNotNull(payment.getPaidAt(), "Checkout payment paidAt must be populated");

            TestAssert.assertEquals(3, productDao.findById(productA.getProductId()).orElseThrow().getStock(), "Stock must decrement 5 -> 3");
            TestAssert.assertTrue(productDao.hasOrderDetails(productA.getProductId()), "hasOrderDetails must be true");

            // FK delete guard
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> shopService.deleteProduct(productA.getProductId()),
                "deleteProduct on ordered product must throw IllegalStateException"
            );

            // Invoice ownership check
            TestAssert.assertThrows(
                SecurityException.class,
                () -> shopService.getOrderInvoice(order.getOrderId(), 2, false),
                "Non-owner non-admin requesting invoice must throw SecurityException"
            );
            Order ownerInvoice = shopService.getOrderInvoice(order.getOrderId(), 1, false);
            TestAssert.assertEquals(order.getOrderCode(), ownerInvoice.getOrderCode(), "Owner must load invoice");
            Order adminInvoice = shopService.getOrderInvoice(order.getOrderId(), 11, true);
            TestAssert.assertEquals(order.getOrderCode(), adminInvoice.getOrderCode(), "Admin must load invoice");

            // Wrong user cannot cancel
            TestAssert.assertThrows(
                SecurityException.class,
                () -> shopService.cancelUserOrder(2, order.getOrderId()),
                "Cancelling another user's order must throw SecurityException"
            );

            // Owner cancels pending order -> stock restored 3 -> 5, status cancelled, payment GAGAL/failed
            shopService.cancelUserOrder(1, order.getOrderId());

            Order cancelled = orderDao.findByIdWithDetails(order.getOrderId()).orElseThrow();
            TestAssert.assertEquals("cancelled", cancelled.getStatus(), "Cancelled order status must be 'cancelled'");
            TestAssert.assertNotNull(cancelled.getPayment(), "Cancelled order must retain payment");
            TestAssert.assertEquals("GAGAL", cancelled.getPayment().getStatus(), "Cancelled order payment status must be GAGAL");
            TestAssert.assertEquals("failed", cancelled.getPayment().getPaymentStatus(), "Cancelled order paymentStatus must be failed");
            TestAssert.assertEquals(5, productDao.findById(productA.getProductId()).orElseThrow().getStock(), "Stock must restore 3 -> 5");

            // Terminal cancelled lock
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> shopService.cancelUserOrder(1, order.getOrderId()),
                "Re-cancelling cancelled order must throw IllegalStateException"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> shopService.updateOrderStatus(order.getOrderId(), "processing"),
                "Admin moving terminal cancelled order to processing must throw IllegalStateException"
            );
        } finally {
            cleanupOrdersAndProducts(orderDao, productDao, cleanupOrderIds, cleanupProductIds);
        }
    }

    private static void testAdminTransitionsRevenueAndCompletedLock() {
        ProductDao productDao = new ProductDao();
        OrderDao orderDao = new OrderDao();
        PaymentDao paymentDao = new PaymentDao();
        ShopService shopService = new ShopService(productDao, orderDao, paymentDao);
        List<Integer> cleanupProductIds = new ArrayList<>();
        List<Integer> cleanupOrderIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            BigDecimal baselineRevenue = orderDao.sumRevenue();
            TestAssert.assertNotNull(baselineRevenue, "sumRevenue must never return null");

            Product product = shopService.createProduct(validTestProduct("AdminFlow-" + tag, new BigDecimal("30000.00"), 5));
            cleanupProductIds.add(product.getProductId());

            // Second checkout (qty 2, pickup, shippingFee = 0)
            Order order2 = shopService.checkout(
                2,
                List.of(new CartItem(product, 2)),
                "Arin Maharani",
                "081210000002",
                "pickup",
                "",
                "QRIS"
            );
            cleanupOrderIds.add(order2.getOrderId());

            assertMoneyEquals(BigDecimal.ZERO, order2.getShippingFee(), "Pickup shippingFee must be 0");
            assertMoneyEquals(new BigDecimal("60000.00"), order2.getTotalPrice(), "Pickup total must be 60000");
            TestAssert.assertEquals("-", order2.getShippingAddress(), "Pickup blank shippingAddress must normalize to '-'");
            TestAssert.assertEquals("QRIS", order2.getPayment().getMethod(), "QRIS method code must be QRIS");
            TestAssert.assertEquals(3, productDao.findById(product.getProductId()).orElseThrow().getStock(), "Stock must be 3");

            // Pending order is NOT included in sumRevenue
            assertMoneyEquals(baselineRevenue, orderDao.sumRevenue(), "Pending order must not be counted in sumRevenue");

            // Admin sets processing -> included in revenue, user cancel blocked
            shopService.updateOrderStatus(order2.getOrderId(), "processing");
            TestAssert.assertEquals("processing", orderDao.findByIdWithDetails(order2.getOrderId()).orElseThrow().getStatus(), "Status must become processing");
            assertMoneyEquals(
                baselineRevenue.add(new BigDecimal("60000.00")),
                orderDao.sumRevenue(),
                "Processing order must be included in sumRevenue"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> shopService.cancelUserOrder(2, order2.getOrderId()),
                "User cannot cancel non-pending (processing) order"
            );

            // Admin cancels active processing order -> restores stock 3 -> 5, payment GAGAL/failed, revenue drops back
            shopService.updateOrderStatus(order2.getOrderId(), "cancelled");
            Order cancelledOrder2 = orderDao.findByIdWithDetails(order2.getOrderId()).orElseThrow();
            TestAssert.assertEquals("cancelled", cancelledOrder2.getStatus(), "Admin cancel must set status cancelled");
            TestAssert.assertEquals("GAGAL", cancelledOrder2.getPayment().getStatus(), "Admin cancel must set payment GAGAL");
            TestAssert.assertEquals("failed", cancelledOrder2.getPayment().getPaymentStatus(), "Admin cancel must set paymentStatus failed");
            TestAssert.assertEquals(5, productDao.findById(product.getProductId()).orElseThrow().getStock(), "Admin cancel must restore stock 3 -> 5");
            assertMoneyEquals(baselineRevenue, orderDao.sumRevenue(), "Cancelled order must be excluded from sumRevenue");

            // Third checkout (qty 1) -> stock 5 -> 4
            Order order3 = shopService.checkout(
                3,
                List.of(new CartItem(product, 1)),
                "Farras Akbar",
                "081210000003",
                "pickup",
                null,
                "Transfer Bank"
            );
            cleanupOrderIds.add(order3.getOrderId());
            TestAssert.assertEquals(4, productDao.findById(product.getProductId()).orElseThrow().getStock(), "Stock must decrement 5 -> 4");

            shopService.updateOrderStatus(order3.getOrderId(), "completed");
            TestAssert.assertEquals("completed", orderDao.findByIdWithDetails(order3.getOrderId()).orElseThrow().getStatus(), "Status must become completed");
            assertMoneyEquals(
                baselineRevenue.add(new BigDecimal("30000.00")),
                orderDao.sumRevenue(),
                "Completed order must be included in sumRevenue"
            );

            // Terminal completed lock
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> shopService.updateOrderStatus(order3.getOrderId(), "cancelled"),
                "Terminal completed order must reject transition to cancelled"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> shopService.updateOrderStatus(order3.getOrderId(), "processing"),
                "Terminal completed order must reject transition to processing"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> shopService.updateOrderStatus(order3.getOrderId(), "archived"),
                "Invalid target order status must throw IllegalArgumentException"
            );
            TestAssert.assertEquals(4, productDao.findById(product.getProductId()).orElseThrow().getStock(), "Stock must remain 4 after rejected cancel");

            // Verify list & recent queries
            TestAssert.assertTrue(
                shopService.listUserOrders(3).stream().anyMatch(o -> o.getOrderId() == order3.getOrderId()),
                "listUserOrders must include user's order"
            );
            TestAssert.assertTrue(
                shopService.listAllOrders().stream().anyMatch(o -> o.getOrderId() == order3.getOrderId()),
                "listAllOrders must include order3"
            );
            TestAssert.assertEquals(0, orderDao.findRecent(0).size(), "findRecent(0) must return empty list");
            TestAssert.assertEquals(3, orderDao.findRecent(3).size(), "findRecent(3) must return 3 orders");
        } finally {
            cleanupOrdersAndProducts(orderDao, productDao, cleanupOrderIds, cleanupProductIds);
        }
    }

    private static void testPaymentDaoBookingPaidAtLifecycle() throws Exception {
        PaymentDao paymentDao = new PaymentDao();
        Integer tempBookingId = null;

        try (Connection c = DatabaseConfig.getConnection()) {
            // Insert temporary health_booking using seed user_id=1 and service_id=1
            String sql = "INSERT INTO health_booking (booking_date, status, user_id, service_id, owner_name, pet_name, pet_species) "
                + "VALUES (?, 'pending', 1, 1, 'Test Owner', 'Test Pet', 'Kucing')";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().plusDays(2)));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    TestAssert.assertTrue(keys.next(), "Temp health_booking must generate ID");
                    tempBookingId = keys.getInt(1);
                }
            }

            Payment bookingPayment = new Payment();
            bookingPayment.setBookingId(tempBookingId);
            bookingPayment.setOrderId(null);
            bookingPayment.setAmount(new BigDecimal("100000.00"));
            bookingPayment.setMethod("TUNAI");
            bookingPayment.setStatus("MENUNGGU");
            bookingPayment.setPaidAt(null);
            bookingPayment.setReferenceNumber("PAY-BKG-" + tempBookingId);
            bookingPayment.setPaymentMethod("Klinik / COD");
            bookingPayment.setPaymentStatus("pending");

            Payment saved = paymentDao.insert(c, bookingPayment);
            TestAssert.assertTrue(saved.getPaymentId() > 0, "Booking payment must have generated ID");
            TestAssert.assertNull(saved.getPaidAt(), "Booking payment with null paidAt MUST store SQL NULL (not CURRENT_TIMESTAMP)");
            TestAssert.assertNull(saved.getOrderId(), "Booking payment orderId must be null");
            TestAssert.assertEquals(tempBookingId, saved.getBookingId(), "Booking payment bookingId must match");

            Optional<Payment> loadedOpt = paymentDao.findByBookingId(c, tempBookingId);
            TestAssert.assertTrue(loadedOpt.isPresent(), "findByBookingId must find booking payment");
            TestAssert.assertNull(loadedOpt.get().getPaidAt(), "Reloaded booking payment paidAt must still be null");

            // Mark paid now
            paymentDao.updateByBookingId(c, tempBookingId, "BERHASIL", "paid", true, false);
            Payment paid = paymentDao.findByBookingId(c, tempBookingId).orElseThrow();
            TestAssert.assertEquals("BERHASIL", paid.getStatus(), "Status must become BERHASIL");
            TestAssert.assertEquals("paid", paid.getPaymentStatus(), "paymentStatus must become paid");
            TestAssert.assertNotNull(paid.getPaidAt(), "markPaidNow=true must set paidAt");

            // Reactivate -> clearPaidAt
            paymentDao.updateByBookingId(c, tempBookingId, "MENUNGGU", "pending", false, true);
            Payment reset = paymentDao.findByBookingId(c, tempBookingId).orElseThrow();
            TestAssert.assertEquals("MENUNGGU", reset.getStatus(), "Status must reset to MENUNGGU");
            TestAssert.assertEquals("pending", reset.getPaymentStatus(), "paymentStatus must reset to pending");
            TestAssert.assertNull(reset.getPaidAt(), "clearPaidAt=true must clear paidAt back to null");

            final int bookingIdFinal = tempBookingId;
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> paymentDao.updateByBookingId(c, bookingIdFinal, "BERHASIL", "paid", true, true),
                "Setting both markPaidNow and clearPaidAt must throw IllegalArgumentException"
            );
        } finally {
            if (tempBookingId != null) {
                try (Connection c = DatabaseConfig.getConnection();
                     PreparedStatement ps = c.prepareStatement("DELETE FROM health_booking WHERE booking_id = ?")) {
                    ps.setInt(1, tempBookingId);
                    ps.executeUpdate();
                } catch (Exception ex) {
                    System.err.println("[CLEANUP WARN] Failed deleting temp health_booking: " + ex.getMessage());
                }
            }
        }
    }

    private static void testConcurrentReverseCartCheckoutsNoDeadlock() throws Exception {
        ProductDao productDao = new ProductDao();
        OrderDao orderDao = new OrderDao();
        PaymentDao paymentDao = new PaymentDao();
        ShopService shopService = new ShopService(productDao, orderDao, paymentDao);
        List<Integer> cleanupProductIds = new ArrayList<>();
        List<Integer> cleanupOrderIds = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            String tag = uniqueTag();
            Product p1 = shopService.createProduct(validTestProduct("Conc1-" + tag, new BigDecimal("15000.00"), 1));
            cleanupProductIds.add(p1.getProductId());
            Product p2 = shopService.createProduct(validTestProduct("Conc2-" + tag, new BigDecimal("25000.00"), 1));
            cleanupProductIds.add(p2.getProductId());

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Future<Object> f1 = pool.submit(() -> runConcurrentCheckout(
                shopService, 1, List.of(new CartItem(p1, 1), new CartItem(p2, 1)), ready, start
            ));
            Future<Object> f2 = pool.submit(() -> runConcurrentCheckout(
                shopService, 2, List.of(new CartItem(p2, 1), new CartItem(p1, 1)), ready, start
            ));

            TestAssert.assertTrue(ready.await(5, TimeUnit.SECONDS), "Concurrent threads must reach barrier");
            start.countDown();

            Object r1 = f1.get(15, TimeUnit.SECONDS);
            Object r2 = f2.get(15, TimeUnit.SECONDS);

            int successes = 0;
            Throwable failure = null;
            for (Object res : List.of(r1, r2)) {
                if (res instanceof Order o) {
                    successes++;
                    cleanupOrderIds.add(o.getOrderId());
                } else if (res instanceof Throwable t) {
                    failure = t;
                }
            }

            TestAssert.assertEquals(1, successes, "Exactly one concurrent checkout must succeed");
            TestAssert.assertTrue(
                failure instanceof IllegalStateException,
                "Losing concurrent checkout must fail with IllegalStateException (no deadlock), got: " + failure
            );
            TestAssert.assertEquals(0, productDao.findById(p1.getProductId()).orElseThrow().getStock(), "Stock P1 must be 0");
            TestAssert.assertEquals(0, productDao.findById(p2.getProductId()).orElseThrow().getStock(), "Stock P2 must be 0");
        } finally {
            pool.shutdownNow();
            cleanupOrdersAndProducts(orderDao, productDao, cleanupOrderIds, cleanupProductIds);
        }
    }

    private static Object runConcurrentCheckout(
            ShopService shopService,
            int userId,
            List<CartItem> cart,
            CountDownLatch ready,
            CountDownLatch start) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            return shopService.checkout(userId, cart, "Pembeli " + userId, "08123456789", "pickup", "", "QRIS");
        } catch (Throwable t) {
            return t;
        }
    }

    private static Product validTestProduct(String name, BigDecimal price, int stock) {
        Product p = new Product();
        p.setProductName(name);
        p.setPrice(price);
        p.setStock(stock);
        p.setCategory("makanan");
        p.setImage(AppConfig.DEFAULT_PRODUCT_IMAGE);
        p.setDescription("Produk pengujian sementara.");
        return p;
    }

    private static void assertMoneyEquals(BigDecimal expected, BigDecimal actual, String message) {
        if (expected == null || actual == null || expected.compareTo(actual) != 0) {
            throw new AssertionError(message + " | expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    private static String uniqueTag() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static void cleanupOrdersAndProducts(
            OrderDao orderDao,
            ProductDao productDao,
            List<Integer> orderIds,
            List<Integer> productIds) {
        for (int orderId : orderIds) {
            try {
                orderDao.delete(orderId);
            } catch (Exception ex) {
                System.err.println("[CLEANUP WARN] Failed deleting order_id=" + orderId + ": " + ex.getMessage());
            }
        }
        cleanupProducts(productDao, productIds);
    }

    private static void cleanupProducts(ProductDao productDao, List<Integer> productIds) {
        for (int productId : productIds) {
            try {
                if (productDao.findById(productId).isPresent()) {
                    productDao.delete(productId);
                }
            } catch (Exception ex) {
                System.err.println("[CLEANUP WARN] Failed deleting product_id=" + productId + ": " + ex.getMessage());
            }
        }
    }
}
