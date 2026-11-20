package pawpaw;

import pawpaw.config.DatabaseConfig;
import pawpaw.dao.HealthBookingDao;
import pawpaw.dao.HealthServiceDao;
import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.ResponsiveCardGrid;
import pawpaw.gui.common.UiKit;
import pawpaw.gui.user.BookingCardDialog;
import pawpaw.gui.user.CheckoutDialog;
import pawpaw.gui.user.HealthWorkspacePanel;
import pawpaw.gui.user.OrderInvoiceDialog;
import pawpaw.gui.user.ShopWorkspacePanel;
import pawpaw.model.HealthBooking;
import pawpaw.model.HealthService;
import pawpaw.model.Order;
import pawpaw.model.Product;
import pawpaw.model.User;
import pawpaw.util.Theme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * EDT smoke tests for ShopWorkspacePanel, CheckoutDialog, OrderInvoiceDialog,
 * HealthWorkspacePanel, BookingCardDialog, and AppWindow route registrations.
 */
public class UserShopHealthPanelsSmokeTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "UserShopHealthPanelsSmokeTest",
            TestAssert.test("Shop and Health workspaces instantiate, refresh against live pet_care DB, filter/gate cleanly on EDT",
                UserShopHealthPanelsSmokeTest::testShopAndHealthLiveOnEdt),
            TestAssert.test("OrderInvoiceDialog, BookingCardDialog, and CheckoutDialog construct, scroll, toggle, and validate without blocking",
                UserShopHealthPanelsSmokeTest::testDialogsConstructAndValidateCleanly),
            TestAssert.test("Shop and Health workspaces guard against null session and unreachable DB without crashing EDT",
                UserShopHealthPanelsSmokeTest::testNullSessionAndDbFailureGuards),
            TestAssert.test("AppWindow mounts ShopWorkspacePanel & HealthWorkspacePanel with responsive card grids at 1040x680 and 1440x1080",
                UserShopHealthPanelsSmokeTest::testAppWindowRegistrationAndScrollableLayout),
            TestAssert.test("HealthWorkspacePanel disables Simpan Booking when remaining slot quota reaches 0",
                UserShopHealthPanelsSmokeTest::testZeroQuotaSlotPreventsSubmission),
            TestAssert.test("HealthWorkspacePanel preserves quota-load error message and disables submit when DB fails",
                UserShopHealthPanelsSmokeTest::testQuotaLoadFailurePreservesErrorHint)
        );
    }

    private static void testShopAndHealthLiveOnEdt() throws Exception {
        runOnEdt(() -> {
            AppWindow window = new AppWindow();
            try {
                User user = createSeedCustomer();
                window.onLoginSuccess(user);

                ShopWorkspacePanel shop = new ShopWorkspacePanel(window);
                HealthWorkspacePanel health = new HealthWorkspacePanel(window);

                TestAssert.assertEquals("user.shop", shop.getPageKey(), "Shop pageKey must match");
                TestAssert.assertEquals("user.health", health.getPageKey(), "Health pageKey must match");

                shop.refreshData();
                health.refreshData();

                assertNonBlank(shop.getStatusSummary(), "ShopWorkspacePanel statusSummary must be non-empty");
                assertNonBlank(health.getStatusSummary(), "HealthWorkspacePanel statusSummary must be non-empty");

                // --- ShopWorkspacePanel checks ---
                JTabbedPane shopTabs = findFirst(shop, JTabbedPane.class);
                TestAssert.assertNotNull(shopTabs, "ShopWorkspacePanel must contain JTabbedPane");
                TestAssert.assertEquals(2, shopTabs.getTabCount(), "ShopWorkspacePanel must have 2 tabs");
                TestAssert.assertEquals("Katalog & Keranjang", shopTabs.getTitleAt(0), "First shop tab must be Katalog & Keranjang");
                TestAssert.assertEquals("Riwayat Pesanan", shopTabs.getTitleAt(1), "Second shop tab must be Riwayat Pesanan");

                Container shopCatalogTab = (Container) shopTabs.getComponentAt(0);
                ResponsiveCardGrid shopGrid = findFirst(shopCatalogTab, ResponsiveCardGrid.class);
                TestAssert.assertNotNull(shopGrid, "Katalog & Keranjang tab must use ResponsiveCardGrid");
                TestAssert.assertTrue(findAll(shopCatalogTab, JTable.class).isEmpty(),
                    "Katalog & Keranjang tab must NOT use a catalog JTable");
                TestAssert.assertTrue(findAll(shopCatalogTab, JComboBox.class).isEmpty(),
                    "Katalog & Keranjang tab must NOT use a JComboBox category dropdown");

                List<JTable> shopTables = findAll(shop, JTable.class);
                TestAssert.assertEquals(1, shopTables.size(), "ShopWorkspacePanel must retain only the Riwayat Pesanan JTable");
                JTable orderTable = shopTables.get(0);
                TestAssert.assertEquals(34, orderTable.getRowHeight(), "Order table rowHeight must be 34");

                int totalProducts = shop.getDisplayedProductCardCount();
                TestAssert.assertTrue(totalProducts >= 10, "Seed shop catalog must display at least 10 product cards");
                TestAssert.assertTrue(orderTable.getRowCount() >= 1, "Seed userId=1 must have at least 1 order");

                List<UiKit.PillFilterButton> categoryPills = findAll(shopCatalogTab, UiKit.PillFilterButton.class);
                TestAssert.assertEquals(5, categoryPills.size(),
                    "Shop catalog must provide 5 category PillFilterButtons");
                UiKit.PillFilterButton semuaCatPill = findPillByText(shopCatalogTab, "Semua");
                UiKit.PillFilterButton makananPill = findPillByText(shopCatalogTab, "Makanan");
                UiKit.PillFilterButton perawatanPill = findPillByText(shopCatalogTab, "Perawatan");
                UiKit.PillFilterButton mainanPill = findPillByText(shopCatalogTab, "Mainan");
                UiKit.PillFilterButton perlengkapanPill = findPillByText(shopCatalogTab, "Perlengkapan");
                TestAssert.assertNotNull(semuaCatPill, "Semua category pill must exist");
                TestAssert.assertNotNull(makananPill, "Makanan category pill must exist");
                TestAssert.assertNotNull(perawatanPill, "Perawatan category pill must exist");
                TestAssert.assertNotNull(mainanPill, "Mainan category pill must exist");
                TestAssert.assertNotNull(perlengkapanPill, "Perlengkapan category pill must exist");
                TestAssert.assertTrue(semuaCatPill.isSelectedPill(), "Semua category pill must be selected initially");

                makananPill.doClick();
                int makananCount = shop.getDisplayedProductCardCount();
                TestAssert.assertTrue(makananCount > 0 && makananCount < totalProducts,
                    "Filtering by Makanan pill must narrow product cards (" + makananCount + " of " + totalProducts + ")");
                TestAssert.assertTrue(makananPill.isSelectedPill(), "Makanan pill must become selected on click");
                TestAssert.assertFalse(semuaCatPill.isSelectedPill(), "Semua pill must deselect when Makanan is clicked");

                semuaCatPill.doClick();
                TestAssert.assertEquals(totalProducts, shop.getDisplayedProductCardCount(),
                    "Clicking Semua pill must restore all product cards");
                TestAssert.assertTrue(semuaCatPill.isSelectedPill(), "Semua pill must become selected on click");

                JTextField shopSearch = findFirst(shopCatalogTab, JTextField.class);
                TestAssert.assertNotNull(shopSearch, "Shop search field must exist");
                shopSearch.setText("Whiskas");
                int whiskasCount = shop.getDisplayedProductCardCount();
                TestAssert.assertTrue(whiskasCount >= 1 && whiskasCount < totalProducts,
                    "Search query Whiskas must filter product cards");
                shopSearch.setText("");
                TestAssert.assertEquals(totalProducts, shop.getDisplayedProductCardCount(),
                    "Clearing search query must restore all product cards");

                // Click 2nd ProductCard inside ResponsiveCardGrid and verify inspector updates
                List<Product> liveProducts = window.getShopService().listProducts();
                TestAssert.assertTrue(shopGrid.getComponentCount() >= 2 && liveProducts.size() >= 2,
                    "Shop catalog grid must hold at least 2 product cards");
                Product secondProduct = liveProducts.get(1);
                Component secondProdCard = shopGrid.getComponent(1);
                secondProdCard.dispatchEvent(new MouseEvent(
                    secondProdCard,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    InputEvent.BUTTON1_DOWN_MASK,
                    12,
                    12,
                    1,
                    false,
                    MouseEvent.BUTTON1
                ));
                TestAssert.assertNotNull(findLabelContaining(shop, secondProduct.getProductName()),
                    "Clicking 2nd product card must update right preview inspector to " + secondProduct.getProductName());

                // Quick-add circular button on ProductCard
                JButton quickAddBtn = findButtonByActionCommandPrefix(shopGrid, "add-to-cart:");
                TestAssert.assertNotNull(quickAddBtn,
                    "ProductCard must expose circular quick-add button with actionCommand 'add-to-cart:<id>'");
                quickAddBtn.doClick();
                TestAssert.assertEquals(1, window.getSession().getCartCount(),
                    "Clicking circular add-to-cart button on ProductCard must increment cart count to 1");

                JButton addCartBtn = findButtonByText(shop, "+ Tambah ke Keranjang");
                JButton clearCartBtn = findButtonByText(shop, "Kosongkan");
                JButton checkoutBtn = findButtonByText(shop, "Checkout");
                TestAssert.assertNotNull(addCartBtn, "+ Tambah ke Keranjang button must exist");
                TestAssert.assertNotNull(clearCartBtn, "Kosongkan button must exist");
                TestAssert.assertNotNull(checkoutBtn, "Checkout button must exist");
                TestAssert.assertTrue(checkoutBtn.isEnabled(), "Checkout must be enabled when cart has items");
                TestAssert.assertTrue(clearCartBtn.isEnabled(), "Kosongkan must be enabled when cart has items");

                addCartBtn.doClick();
                TestAssert.assertEquals(2, window.getSession().getCartCount(),
                    "Clicking + Tambah ke Keranjang must increment session cart count to 2");

                JButton incCartBtn = findButtonByText(shop, "+");
                TestAssert.assertNotNull(incCartBtn, "Cart inspector + button must exist");
                incCartBtn.doClick();
                TestAssert.assertEquals(3, window.getSession().getCartCount(),
                    "Clicking cart + button must increment cart count to 3");

                JButton decCartBtn = findButtonByText(shop, "-");
                TestAssert.assertNotNull(decCartBtn, "Cart inspector - button must exist");
                decCartBtn.doClick();
                TestAssert.assertEquals(2, window.getSession().getCartCount(),
                    "Clicking cart - button must decrement cart count to 2");

                // Validate stock boundary via ShopService.validateCartQuantity
                Product selectedProd = window.getSession().getCart().get(0).getProduct();
                window.getSession().updateCartQty(selectedProd.getProductId(), selectedProd.getStock());
                addCartBtn.doClick();
                TestAssert.assertEquals(selectedProd.getStock(), window.getSession().getCartCount(),
                    "Adding beyond product stock must be blocked by ShopService.validateCartQuantity");

                clearCartBtn.doClick();
                TestAssert.assertEquals(0, window.getSession().getCartCount(),
                    "Clicking Kosongkan must clear session cart");
                TestAssert.assertFalse(checkoutBtn.isEnabled(), "Checkout must be disabled after clearing cart");

                // Order #1 in seed DB has status 'processing', so Lihat Struk is enabled and Batalkan Pesanan is disabled.
                orderTable.setRowSelectionInterval(0, 0);
                JButton invoiceBtn = findButtonByText(shop, "Lihat Struk");
                JButton cancelOrderBtn = findButtonByText(shop, "Batalkan Pesanan");
                TestAssert.assertNotNull(invoiceBtn, "Lihat Struk button must exist");
                TestAssert.assertNotNull(cancelOrderBtn, "Batalkan Pesanan button must exist");
                TestAssert.assertTrue(invoiceBtn.isEnabled(), "Lihat Struk must be enabled when order row selected");
                TestAssert.assertFalse(cancelOrderBtn.isEnabled(),
                    "Batalkan Pesanan must be disabled for non-pending seed order #1 (processing)");

                // --- HealthWorkspacePanel checks ---
                JTabbedPane healthTabs = findFirst(health, JTabbedPane.class);
                TestAssert.assertNotNull(healthTabs, "HealthWorkspacePanel must contain JTabbedPane");
                TestAssert.assertEquals(2, healthTabs.getTabCount(), "HealthWorkspacePanel must have 2 tabs");
                TestAssert.assertEquals("Booking Layanan", healthTabs.getTitleAt(0), "First health tab must be Booking Layanan");
                TestAssert.assertEquals("Riwayat Booking", healthTabs.getTitleAt(1), "Second health tab must be Riwayat Booking");

                Container healthBookingTab = (Container) healthTabs.getComponentAt(0);
                ResponsiveCardGrid healthGrid = findFirst(healthBookingTab, ResponsiveCardGrid.class);
                TestAssert.assertNotNull(healthGrid, "Booking Layanan tab must use ResponsiveCardGrid");
                TestAssert.assertTrue(findAll(healthBookingTab, JTable.class).isEmpty(),
                    "Booking Layanan tab must NOT use a service JTable");

                List<JTable> healthTables = findAll(health, JTable.class);
                TestAssert.assertEquals(1, healthTables.size(), "HealthWorkspacePanel must retain only the Riwayat Booking JTable");
                JTable bookingTable = healthTables.get(0);
                TestAssert.assertEquals(34, bookingTable.getRowHeight(), "Booking table rowHeight must be 34");
                TestAssert.assertTrue(health.getDisplayedServiceCardCount() >= 10,
                    "Seed clinic must display at least 10 health service cards");
                TestAssert.assertTrue(bookingTable.getRowCount() >= 1, "Seed userId=1 must have at least 1 health booking");
                TestAssert.assertNotNull(findLabelContaining(healthGrid, "WIB"),
                    "HealthServiceCard must render white HH:mm WIB time-slot chips");

                assertNoEmojiGlyphs(health);
                assertNoEmojiGlyphs(shop);

                // Click 2nd service card and confirm booking summary updates
                List<HealthService> liveServices = window.getHealthBookingService().listServices();
                TestAssert.assertTrue(healthGrid.getComponentCount() >= 2 && liveServices.size() >= 2,
                    "Health service grid must hold at least 2 service cards");
                HealthService secondService = liveServices.get(1);
                Component secondSvcCard = healthGrid.getComponent(1);
                secondSvcCard.dispatchEvent(new MouseEvent(
                    secondSvcCard,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    InputEvent.BUTTON1_DOWN_MASK,
                    12,
                    12,
                    1,
                    false,
                    MouseEvent.BUTTON1
                ));
                TestAssert.assertNotNull(findLabelContaining(health, secondService.getDoctorName()),
                    "Clicking 2nd service card must update right booking summary doctor label");

                JTextField healthSearch = findFirst(healthBookingTab, JTextField.class);
                TestAssert.assertNotNull(healthSearch, "Health search field must exist");
                int totalServices = health.getDisplayedServiceCardCount();
                healthSearch.setText("Vaksin");
                int vaksinCount = health.getDisplayedServiceCardCount();
                TestAssert.assertTrue(vaksinCount >= 1 && vaksinCount < totalServices,
                    "Search query Vaksin must filter service cards (" + vaksinCount + " of " + totalServices + ")");
                healthSearch.setText("");
                TestAssert.assertEquals(totalServices, health.getDisplayedServiceCardCount(),
                    "Clearing health search query must restore all service cards");

                // Pet dropdown, owner prefill, date spinner, and dynamic slot quota dropdown checks
                List<JComboBox<?>> healthCombos = findCombos(health);
                TestAssert.assertTrue(healthCombos.size() >= 2,
                    "HealthWorkspacePanel must have pet selector and time slot combos");

                JComboBox<?> petCombo = findComboWithItemContaining(healthCombos, "Hewan Baru");
                TestAssert.assertNotNull(petCombo, "Pet selector combo must include Hewan Baru option");
                JComboBox<?> slotCombo = findComboWithItemContaining(healthCombos, "(sisa ");
                TestAssert.assertNotNull(slotCombo, "Time slot combo must display remaining quota format e.g. '09:00 (sisa 6/6)'");

                List<JTextField> healthFields = findAll(health, JTextField.class);
                boolean hasOwnerPrefill = healthFields.stream().anyMatch(f -> "Jesika Putri".equals(f.getText()));
                TestAssert.assertTrue(hasOwnerPrefill, "Health booking form must prefill owner name Jesika Putri");

                boolean hasMiloPrefill = healthFields.stream().anyMatch(f -> "Milo".equals(f.getText()));
                TestAssert.assertTrue(hasMiloPrefill, "Selecting existing pet must prefill Milo");

                // Switch to Hewan Baru (isi manual) -> clears pet name field and enables manual entry
                petCombo.setSelectedIndex(petCombo.getItemCount() - 1);
                boolean miloCleared = healthFields.stream().noneMatch(f -> "Milo".equals(f.getText()));
                TestAssert.assertTrue(miloCleared, "Selecting Hewan Baru must clear prefilled pet name");

                // Switch back to Milo -> restores Milo
                petCombo.setSelectedIndex(0);
                boolean miloRestored = healthFields.stream().anyMatch(f -> "Milo".equals(f.getText()));
                TestAssert.assertTrue(miloRestored, "Switching back to Milo must restore Milo");

                JSpinner dateSpinner = findFirst(health, JSpinner.class);
                TestAssert.assertNotNull(dateSpinner, "Health booking form must include date JSpinner");
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 3);
                dateSpinner.setValue(cal.getTime());
                TestAssert.assertTrue(slotCombo.getItemCount() > 0,
                    "Updating date JSpinner must reload time slot options");

                // Seed booking #1 has status 'pending', so both Kartu Booking and Batalkan Booking must be enabled
                bookingTable.setRowSelectionInterval(0, 0);
                JButton cardBtn = findButtonByText(health, "Kartu Booking");
                JButton cancelBookingBtn = findButtonByText(health, "Batalkan Booking");
                TestAssert.assertNotNull(cardBtn, "Kartu Booking button must exist");
                TestAssert.assertNotNull(cancelBookingBtn, "Batalkan Booking button must exist");
                TestAssert.assertTrue(cardBtn.isEnabled(), "Kartu Booking must be enabled when booking selected");
                TestAssert.assertTrue(cancelBookingBtn.isEnabled(),
                    "Batalkan Booking must be enabled for pending seed booking #1");
            } finally {
                window.dispose();
            }
        });
    }

    private static void testDialogsConstructAndValidateCleanly() throws Exception {
        runOnEdt(() -> {
            OrderInvoiceDialog.show(null, null);
            BookingCardDialog.show(null, null);

            AppWindow window = new AppWindow();
            try {
                User user = createSeedCustomer();
                window.getSession().login(user);

                Order seedOrder = window.getShopService().getOrderInvoice(1, 1, false);
                OrderInvoiceDialog invoiceDialog = new OrderInvoiceDialog(window, seedOrder);
                try {
                    TestAssert.assertEquals(Theme.BG, invoiceDialog.getContentPane().getBackground(),
                        "OrderInvoiceDialog root must use Theme.BG");
                    JTable itemsTable = findFirst(invoiceDialog.getContentPane(), JTable.class);
                    TestAssert.assertNotNull(itemsTable, "OrderInvoiceDialog must display line-item JTable");
                    TestAssert.assertTrue(itemsTable.getRowCount() >= 2,
                        "Seed order #1 invoice must list at least 2 line items");
                    TestAssert.assertNotNull(findLabelContaining(invoiceDialog.getContentPane(), "ORD-2026-0001"),
                        "OrderInvoiceDialog must display order code ORD-2026-0001");
                    TestAssert.assertNotNull(findLabelContaining(invoiceDialog.getContentPane(), "PAY-ORD-0001"),
                        "OrderInvoiceDialog must display payment reference PAY-ORD-0001");
                    assertNoEmojiGlyphs(invoiceDialog.getContentPane());
                } finally {
                    invoiceDialog.dispose();
                }

                HealthBooking seedBooking = window.getHealthBookingService().getBookingCard(1, 1, false);
                BookingCardDialog cardDialog = new BookingCardDialog(window, seedBooking);
                try {
                    TestAssert.assertEquals(Theme.BG, cardDialog.getContentPane().getBackground(),
                        "BookingCardDialog root must use Theme.BG");
                    TestAssert.assertNotNull(findLabelContaining(cardDialog.getContentPane(), "Pemeriksaan Umum"),
                        "BookingCardDialog must display service name Pemeriksaan Umum");
                    TestAssert.assertNotNull(findLabelContaining(cardDialog.getContentPane(), "drh. Clara Wijaya"),
                        "BookingCardDialog must display doctor name");
                    TestAssert.assertNotNull(findLabelContaining(cardDialog.getContentPane(), "Milo"),
                        "BookingCardDialog must display pet name Milo");
                    TestAssert.assertNotNull(findLabelContaining(cardDialog.getContentPane(), "PAY-BKG-0001"),
                        "BookingCardDialog must display payment reference PAY-BKG-0001");
                    TestAssert.assertNull(findLabelContaining(cardDialog.getContentPane(), "Alasan Pembatalan"),
                        "BookingCardDialog must omit rejection/cancellation banner when reason is blank");
                    assertNoEmojiGlyphs(cardDialog.getContentPane());
                } finally {
                    cardDialog.dispose();
                }

                seedBooking.setStatus("rejected");
                seedBooking.setRejectionReason("Dokter sedang cuti mendadak");
                BookingCardDialog rejectedCard = new BookingCardDialog(window, seedBooking);
                try {
                    JTextArea reasonArea = findFirst(rejectedCard.getContentPane(), JTextArea.class);
                    TestAssert.assertNotNull(reasonArea, "BookingCardDialog must display rejectionReason when present");
                    TestAssert.assertEquals("Dokter sedang cuti mendadak", reasonArea.getText(),
                        "BookingCardDialog must display exact rejectionReason");
                } finally {
                    rejectedCard.dispose();
                }

                Product sample = new Product();
                sample.setProductId(1);
                sample.setProductName("Whiskas Adult Tuna 1 kg");
                sample.setPrice(new BigDecimal("65000.00"));
                sample.setStock(50);
                window.getSession().addToCart(sample, 1);

                CheckoutDialog checkoutDialog = new CheckoutDialog(window);
                try {
                    TestAssert.assertEquals(Theme.BG, checkoutDialog.getContentPane().getBackground(),
                        "CheckoutDialog root must use Theme.BG");
                    assertScrollableTracksViewportWidth(checkoutDialog.getContentPane(), "CheckoutDialog");
                    TestAssert.assertTrue(checkoutDialog.getHeight() > 0 && checkoutDialog.getHeight() <= 620,
                        "CheckoutDialog height (" + checkoutDialog.getHeight() + "px) must stay <= 620px");

                    List<JTextField> fields = findAll(checkoutDialog.getContentPane(), JTextField.class);
                    JTextField recipientNameField = fields.stream()
                        .filter(f -> "Jesika Putri".equals(f.getText()))
                        .findFirst()
                        .orElse(null);
                    TestAssert.assertNotNull(recipientNameField, "CheckoutDialog must prefill recipient name Jesika Putri");

                    JComboBox<?> deliveryCombo = findComboWithItemContaining(
                        findCombos(checkoutDialog.getContentPane()), "Pengantaran"
                    );
                    TestAssert.assertNotNull(deliveryCombo, "CheckoutDialog must have delivery method combo");
                    JTextArea addressArea = findFirst(checkoutDialog.getContentPane(), JTextArea.class);
                    TestAssert.assertNotNull(addressArea, "CheckoutDialog must have shipping address JTextArea");

                    deliveryCombo.setSelectedIndex(0);
                    TestAssert.assertNotNull(findLabelContaining(checkoutDialog.getContentPane(), "Rp 0"),
                        "Pickup option must display Rp 0 shipping fee");
                    TestAssert.assertFalse(addressArea.isEnabled(),
                        "Pickup option must disable shipping address requirement");

                    deliveryCombo.setSelectedIndex(1);
                    TestAssert.assertNotNull(findLabelContaining(checkoutDialog.getContentPane(), "Rp 15.000"),
                        "Delivery option must update shipping fee to Rp 15.000");
                    TestAssert.assertTrue(addressArea.isEnabled(),
                        "Delivery option must enable shipping address requirement");

                    recipientNameField.setText("   ");
                    JButton confirmBtn = findButtonByText(checkoutDialog.getContentPane(), "Konfirmasi & Bayar");
                    TestAssert.assertNotNull(confirmBtn, "CheckoutDialog must have Konfirmasi & Bayar button");
                    confirmBtn.doClick();
                    checkoutDialog.validate();

                    TestAssert.assertNull(checkoutDialog.getCreatedOrder(),
                        "Blank recipient name must fail validation without creating order");
                    assertNonBlank(checkoutDialog.getInlineErrorText(),
                        "CheckoutDialog must display inline error on validation failure");
                    TestAssert.assertTrue(checkoutDialog.getHeight() <= 620,
                        "CheckoutDialog height (" + checkoutDialog.getHeight() + "px) must remain <= 620px on error");
                    assertNoEmojiGlyphs(checkoutDialog.getContentPane());
                } finally {
                    checkoutDialog.dispose();
                }
            } finally {
                window.dispose();
            }
        });
    }

    private static void testNullSessionAndDbFailureGuards() throws Exception {
        try {
            runOnEdt(() -> {
                AppWindow window = new AppWindow();
                try {
                    ShopWorkspacePanel shop = new ShopWorkspacePanel(window);
                    HealthWorkspacePanel health = new HealthWorkspacePanel(window);

                    shop.refreshData();
                    health.refreshData();

                    assertNonBlank(shop.getStatusSummary(), "Null session shop statusSummary must be non-empty");
                    assertNonBlank(health.getStatusSummary(), "Null session health statusSummary must be non-empty");
                    TestAssert.assertTrue(shop.getDisplayedProductCardCount() >= 10,
                        "Null session shop catalog must still display public products");
                    TestAssert.assertTrue(health.getDisplayedServiceCardCount() >= 10,
                        "Null session health catalog must still display public services");
                } finally {
                    window.dispose();
                }
            });

            DatabaseConfig.overrideForTest(
                "jdbc:mysql://127.0.0.1:33999/pet_care?connectTimeout=500",
                "root",
                ""
            );

            runOnEdt(() -> {
                AppWindow window = new AppWindow();
                try {
                    window.getSession().login(createSeedCustomer());
                    ShopWorkspacePanel shop = new ShopWorkspacePanel(window);
                    HealthWorkspacePanel health = new HealthWorkspacePanel(window);

                    shop.refreshData();
                    health.refreshData();

                    assertNonBlank(shop.getStatusSummary(), "DB-error shop statusSummary must be non-empty");
                    assertNonBlank(health.getStatusSummary(), "DB-error health statusSummary must be non-empty");
                    TestAssert.assertEquals(0, shop.getDisplayedProductCardCount(),
                        "DB-error shop panel must reset product cards to 0");
                    TestAssert.assertEquals(0, health.getDisplayedServiceCardCount(),
                        "DB-error health panel must reset service cards to 0");
                } finally {
                    window.dispose();
                }
            });
        } finally {
            DatabaseConfig.resetConfig();
        }
    }

    private static void testAppWindowRegistrationAndScrollableLayout() throws Exception {
        final AppWindow[] windowHolder = new AppWindow[1];
        try {
            runOnEdt(() -> {
                AppWindow window = new AppWindow();
                windowHolder[0] = window;
                window.onLoginSuccess(createSeedCustomer());
                window.addNotify();

                // 1. ShopWorkspacePanel at 1040x680
                window.navigate("user.shop");
                window.setSize(1040, 680);
                window.validate();
            });

            // Flush ResponsiveCardGrid invokeLater revalidations after first layout
            runOnEdt(() -> {
                AppWindow window = windowHolder[0];
                window.validate();
                TestAssert.assertEquals("user.shop", window.getShell().getActivePageKey(),
                    "Navigating to user.shop must activate ShopWorkspacePanel");
                ShopWorkspacePanel mountedShop = findFirst(window.getShell(), ShopWorkspacePanel.class);
                TestAssert.assertNotNull(mountedShop, "AppWindow must register real ShopWorkspacePanel for user.shop");
                assertScrollableTracksViewportWidth(mountedShop, "ShopWorkspacePanel");

                int shopCols1040 = mountedShop.getCatalogColumnCount();
                TestAssert.assertTrue(shopCols1040 >= 2,
                    "Shop catalog grid at 1040x680 must show >= 2 columns (was " + shopCols1040 + ")");

                JTextField shopSearchField = findFirst(mountedShop, JTextField.class);
                JLabel shopCountLabel = findLabelContaining(mountedShop, "produk");
                List<UiKit.PillFilterButton> shopPills = findAll(mountedShop, UiKit.PillFilterButton.class);
                List<Component> shopToolbarItems = new ArrayList<>();
                shopToolbarItems.add(shopSearchField);
                shopToolbarItems.add(shopCountLabel);
                shopToolbarItems.addAll(shopPills);
                assertComponentsNonOverlapping(mountedShop, shopToolbarItems, "Shop toolbar@1040x680");

                JLabel previewMetaLbl = findLabelContaining(mountedShop, "|  Stok:");
                TestAssert.assertNotNull(previewMetaLbl, "Preview metadata label must exist");
                Container infoStack = previewMetaLbl.getParent();
                TestAssert.assertTrue(infoStack.getWidth() >= 200,
                    "Preview card infoStack width (" + infoStack.getWidth() + "px) must be >= 200px at 1040x680");

                // Add a cart item at 1040x680 so cartFeedbackSlot is visible and assert cart inspector does not collapse
                JButton addCartBtn = findButtonByText(mountedShop, "+ Tambah ke Keranjang");
                TestAssert.assertNotNull(addCartBtn, "+ Tambah ke Keranjang button must exist");
                addCartBtn.doClick();
                window.validate();

                JLabel feedbackLbl = findLabelContaining(mountedShop, "Ditambahkan:");
                TestAssert.assertNotNull(feedbackLbl, "Cart feedback banner must appear after adding item at 1040x680");
                TestAssert.assertTrue(feedbackLbl.isVisible(), "Cart feedback banner must be visible at 1040x680");

                JLabel cartHeaderLbl = findLabelContaining(mountedShop, "Keranjang Belanja");
                JLabel cartUnitLbl = findLabelContaining(mountedShop, "/ unit");
                TestAssert.assertNotNull(cartHeaderLbl, "Keranjang Belanja header label must exist");
                TestAssert.assertNotNull(cartUnitLbl, "Added cart item row must render unit price label at 1040x680");

                JScrollPane inspectorScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, cartUnitLbl);
                TestAssert.assertNotNull(inspectorScroll, "Cart inspector list must be hosted inside a JScrollPane");
                int inspectorVpH = inspectorScroll.getViewport().getHeight();
                TestAssert.assertTrue(inspectorVpH >= 150,
                    "Cart inspector scroll viewport height (" + inspectorVpH + "px) must be >= 150px at 1040x680");
                TestAssert.assertTrue(cartHeaderLbl.getHeight() > 0 && cartHeaderLbl.getHeight() >= cartHeaderLbl.getPreferredSize().height,
                    "Keranjang Belanja header height (" + cartHeaderLbl.getHeight() + "px) must not be clipped at 1040x680");
                TestAssert.assertTrue(cartUnitLbl.getHeight() > 0 && cartUnitLbl.getWidth() > 0,
                    "Added cart item row label must lay out with positive size (" + cartUnitLbl.getWidth() + "x" + cartUnitLbl.getHeight() + ")");

                JButton clearCartBtn = findButtonByText(mountedShop, "Kosongkan");
                TestAssert.assertNotNull(clearCartBtn, "Kosongkan button must exist");
                clearCartBtn.doClick();
                window.validate();

                // Resize Shop to 1440x1080
                window.setSize(1440, 1080);
                window.validate();
            });

            runOnEdt(() -> {
                AppWindow window = windowHolder[0];
                window.validate();
                ShopWorkspacePanel mountedShop = findFirst(window.getShell(), ShopWorkspacePanel.class);
                int shopCols1440 = mountedShop.getCatalogColumnCount();
                TestAssert.assertTrue(shopCols1440 >= 3,
                    "Shop catalog grid at 1440x1080 must show >= 3 columns (was " + shopCols1440 + ")");

                // 2. HealthWorkspacePanel at 1040x680
                window.navigate("user.health");
                window.setSize(1040, 680);
                window.validate();
            });

            runOnEdt(() -> {
                AppWindow window = windowHolder[0];
                window.validate();
                TestAssert.assertEquals("user.health", window.getShell().getActivePageKey(),
                    "Navigating to user.health must activate HealthWorkspacePanel");
                HealthWorkspacePanel mountedHealth = findFirst(window.getShell(), HealthWorkspacePanel.class);
                TestAssert.assertNotNull(mountedHealth, "AppWindow must register real HealthWorkspacePanel for user.health");
                assertScrollableTracksViewportWidth(mountedHealth, "HealthWorkspacePanel");

                int healthCols1040 = mountedHealth.getCatalogColumnCount();
                TestAssert.assertTrue(healthCols1040 >= 2,
                    "Health catalog grid at 1040x680 must show >= 2 columns (was " + healthCols1040 + ")");

                JTextField healthSearchField = findFirst(mountedHealth, JTextField.class);
                JLabel healthCountLabel = findLabelContaining(mountedHealth, "layanan");
                assertComponentsNonOverlapping(
                    mountedHealth,
                    List.of(healthSearchField, healthCountLabel),
                    "Health toolbar@1040x680"
                );

                // Verify long title card "Kunjungan Dokter ke Rumah" has non-overlapping icon, price pill, and wrapped title
                ResponsiveCardGrid healthGrid = findFirst(mountedHealth, ResponsiveCardGrid.class);
                TestAssert.assertNotNull(healthGrid, "Health grid must exist");
                Container longTitleCard = findCardWithLabel(healthGrid, "Kunjungan Dokter ke Rumah");
                TestAssert.assertNotNull(longTitleCard,
                    "Health grid must render 'Kunjungan Dokter ke Rumah' service card");
                JLabel longTitleLabel = findLabelContaining(longTitleCard, "Kunjungan Dokter ke Rumah");
                JLabel longPricePill = findLabelContaining(longTitleCard, "Rp ");
                JLabel longIconBadge = findSizedIconLabel(longTitleCard, 36, 36);
                JLabel longWibChip = findLabelContaining(longTitleCard, "WIB");
                TestAssert.assertNotNull(longTitleLabel, "Long service card must have title JLabel");
                TestAssert.assertNotNull(longPricePill, "Long service card must have price pill JLabel");
                TestAssert.assertNotNull(longIconBadge, "Long service card must have 36x36 vector icon badge");
                TestAssert.assertNotNull(longWibChip, "Long service card must have WIB time-slot chip");
                assertComponentsNonOverlapping(
                    longTitleCard,
                    List.of(longIconBadge, longPricePill, longTitleLabel, longWibChip),
                    "HealthServiceCard(Kunjungan Dokter ke Rumah)@1040x680"
                );
                Rectangle iconBounds = SwingUtilities.convertRectangle(
                    longIconBadge.getParent(), longIconBadge.getBounds(), longTitleCard);
                Rectangle priceBounds = SwingUtilities.convertRectangle(
                    longPricePill.getParent(), longPricePill.getBounds(), longTitleCard);
                Rectangle titleBounds = SwingUtilities.convertRectangle(
                    longTitleLabel.getParent(), longTitleLabel.getBounds(), longTitleCard);
                TestAssert.assertTrue(
                    titleBounds.y >= Math.max(iconBounds.y + iconBounds.height, priceBounds.y + priceBounds.height),
                    "HealthServiceCard Row 2 title (y=" + titleBounds.y
                        + ") must sit cleanly below Row 1 icon/price"
                );
                TestAssert.assertTrue(
                    titleBounds.height >= 28,
                    "HealthServiceCard wrapped title height (" + titleBounds.height + "px) must wrap cleanly without clipping"
                );

                JButton submitBookingBtn = findButtonByText(mountedHealth, "Simpan Booking");
                TestAssert.assertNotNull(submitBookingBtn, "Simpan Booking button must exist");
                TestAssert.assertTrue(submitBookingBtn.getHeight() > 0 && submitBookingBtn.getHeight() <= 44,
                    "Simpan Booking button height (" + submitBookingBtn.getHeight() + "px) must stay compact");

                // Resize Health to 1440x1080
                window.setSize(1440, 1080);
                window.validate();
            });

            runOnEdt(() -> {
                AppWindow window = windowHolder[0];
                window.validate();
                HealthWorkspacePanel mountedHealth = findFirst(window.getShell(), HealthWorkspacePanel.class);
                int healthCols1440 = mountedHealth.getCatalogColumnCount();
                TestAssert.assertTrue(healthCols1440 >= 3,
                    "Health catalog grid at 1440x1080 must show >= 3 columns (was " + healthCols1440 + ")");
            });
        } finally {
            if (windowHolder[0] != null) {
                runOnEdt(() -> windowHolder[0].dispose());
            }
        }
    }

    private static void testZeroQuotaSlotPreventsSubmission() throws Exception {
        HealthServiceDao serviceDao = new HealthServiceDao();
        HealthBookingDao bookingDao = new HealthBookingDao();
        final int[] createdIds = new int[2];

        try {
            runOnEdt(() -> {
                AppWindow window = new AppWindow();
                try {
                    User user = createSeedCustomer();
                    window.getSession().login(user);

                    String tag = UUID.randomUUID().toString().substring(0, 8);
                    HealthService svc = new HealthService();
                    svc.setServiceName("UjiKuotaZero-" + tag);
                    svc.setPrice(new BigDecimal("85000.00"));
                    svc.setDoctorName("drh. Uji");
                    svc.setPracticeDays("Senin");
                    svc.setTimeSlots("09:00");
                    svc.setDailyQuota(1);
                    svc.setDescription("Uji kuota habis");
                    HealthService createdSvc = window.getHealthBookingService().createService(svc);
                    createdIds[0] = createdSvc.getServiceId();

                    LocalDate targetDate = LocalDate.now().plusDays(4);
                    HealthBooking booking = window.getHealthBookingService().createBooking(
                        1, createdSvc.getServiceId(), 1, "Jesika Putri", "Milo", "Kucing", targetDate, "09:00"
                    );
                    createdIds[1] = booking.getBookingId();

                    HealthWorkspacePanel health = new HealthWorkspacePanel(window);
                    health.refreshData();

                    TestAssert.assertTrue(health.selectServiceById(createdSvc.getServiceId()),
                        "Temporary zero-quota service #" + createdSvc.getServiceId() + " must be selectable via selectServiceById");

                    JSpinner dateSpinner = findFirst(health, JSpinner.class);
                    TestAssert.assertNotNull(dateSpinner, "Date spinner must exist");
                    dateSpinner.setValue(Date.from(targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));

                    JComboBox<?> slotCombo = findComboWithItemContaining(
                        findCombos(health), "09:00 (sisa 0/1)"
                    );
                    TestAssert.assertNotNull(slotCombo,
                        "Time slot combo must display '09:00 (sisa 0/1)' when quota is exhausted");

                    JButton submitBtn = findButtonByText(health, "Simpan Booking");
                    TestAssert.assertNotNull(submitBtn, "Simpan Booking button must exist");
                    TestAssert.assertFalse(submitBtn.isEnabled(),
                        "Simpan Booking must be disabled when selected slot quota is 0");
                } finally {
                    window.dispose();
                }
            });
        } finally {
            if (createdIds[1] > 0) {
                try {
                    bookingDao.delete(createdIds[1]);
                } catch (Exception ignored) {
                }
            }
            if (createdIds[0] > 0) {
                try {
                    serviceDao.delete(createdIds[0]);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void testQuotaLoadFailurePreservesErrorHint() throws Exception {
        try {
            runOnEdt(() -> {
                AppWindow window = new AppWindow();
                try {
                    window.getSession().login(createSeedCustomer());
                    HealthWorkspacePanel health = new HealthWorkspacePanel(window);
                    health.refreshData();

                    JButton submitBtn = findButtonByText(health, "Simpan Booking");
                    TestAssert.assertNotNull(submitBtn, "Simpan Booking button must exist");
                    TestAssert.assertTrue(submitBtn.isEnabled(),
                        "Simpan Booking must initially be enabled on reachable seed DB");

                    // Break DB before triggering slot reload via date change
                    DatabaseConfig.overrideForTest(
                        "jdbc:mysql://127.0.0.1:33999/pet_care?connectTimeout=500",
                        "root",
                        ""
                    );
                    JSpinner dateSpinner = findFirst(health, JSpinner.class);
                    TestAssert.assertNotNull(dateSpinner, "Date spinner must exist");
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_MONTH, 7);
                    dateSpinner.setValue(cal.getTime());

                    TestAssert.assertFalse(submitBtn.isEnabled(),
                        "Simpan Booking must be disabled when slot quota load fails");
                    JLabel errorHint = findLabelContaining(health, "Gagal memuat kuota slot");
                    TestAssert.assertNotNull(errorHint,
                        "quotaHintLabel must retain 'Gagal memuat kuota slot' when DB call throws");
                } finally {
                    window.dispose();
                }
            });
        } finally {
            DatabaseConfig.resetConfig();
        }
    }

    private static void assertComponentsNonOverlapping(
            Container root,
            List<? extends Component> components,
            String context) {
        List<Rectangle> rects = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Component c : components) {
            TestAssert.assertNotNull(c, context + ": expected component missing");
            TestAssert.assertTrue(c.isVisible(), context + ": component must be visible");
            TestAssert.assertTrue(c.getWidth() > 0 && c.getHeight() > 0,
                context + ": component must have positive size (" + c.getWidth() + "x" + c.getHeight() + ")");
            Rectangle r = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), root);
            rects.add(r);
            names.add(describeComponent(c));
        }
        for (int i = 0; i < rects.size(); i++) {
            for (int j = i + 1; j < rects.size(); j++) {
                TestAssert.assertFalse(rects.get(i).intersects(rects.get(j)),
                    context + ": " + names.get(i) + " " + rects.get(i)
                        + " overlaps " + names.get(j) + " " + rects.get(j));
            }
        }
    }

    private static String describeComponent(Component c) {
        if (c instanceof JButton b) {
            return "Button(" + b.getText() + ")";
        }
        if (c instanceof JLabel l) {
            return "Label(" + l.getText() + ")";
        }
        return c.getClass().getSimpleName();
    }

    private static Container findCardWithLabel(ResponsiveCardGrid grid, String labelFragment) {
        for (Component child : grid.getComponents()) {
            if (child instanceof Container card && findLabelContaining(card, labelFragment) != null) {
                return card;
            }
        }
        return null;
    }

    private static JLabel findSizedIconLabel(Container root, int width, int height) {
        for (JLabel lbl : findAll(root, JLabel.class)) {
            if (lbl.getIcon() != null && lbl.getWidth() == width && lbl.getHeight() == height) {
                return lbl;
            }
        }
        return null;
    }

    private static void assertScrollableTracksViewportWidth(Container root, String panelName) {
        List<JScrollPane> scrolls = findAll(root, JScrollPane.class);
        boolean foundTrackingScrollable = false;
        for (JScrollPane sp : scrolls) {
            Component view = sp.getViewport().getView();
            if (view instanceof Scrollable s && s.getScrollableTracksViewportWidth()) {
                foundTrackingScrollable = true;
                break;
            }
        }
        TestAssert.assertTrue(foundTrackingScrollable,
            panelName + " must host right-hand stack inside a Scrollable with getScrollableTracksViewportWidth() == true");
    }

    private static void assertNoEmojiGlyphs(Container root) {
        for (JLabel lbl : findAll(root, JLabel.class)) {
            assertCleanText(lbl.getText(), "JLabel");
        }
        for (JButton btn : findAll(root, JButton.class)) {
            assertCleanText(btn.getText(), "JButton");
        }
        for (JTable table : findAll(root, JTable.class)) {
            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    Object val = table.getValueAt(r, c);
                    if (val != null) {
                        assertCleanText(val.toString(), "JTable cell");
                    }
                }
            }
        }
    }

    private static void assertCleanText(String text, String source) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            int cp = text.codePointAt(i);
            if (Character.isSupplementaryCodePoint(cp)
                    || (cp >= 0x2600 && cp <= 0x27BF)
                    || (cp >= 0xFE00 && cp <= 0xFE0F)) {
                throw new AssertionError("Disallowed emoji glyph in " + source + ": " + text);
            }
            if (Character.isHighSurrogate(text.charAt(i))) {
                i++;
            }
        }
    }

    private static User createSeedCustomer() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("jesika_putri");
        user.setName("Jesika Putri");
        user.setEmail("jesika@pawpaw.com");
        user.setPhone("081210000001");
        user.setAddress("Jl. Subrantas Panam No. 10, Pekanbaru");
        user.setBio("Pecinta kucing dan pelanggan setia PawPaw");
        user.setAvatar("assets/img/jesika.jfif");
        user.setRole("user");
        return user;
    }

    private static void assertNonBlank(String value, String message) {
        TestAssert.assertTrue(value != null && !value.isBlank(), message + " (was: <" + value + ">)");
    }

    private static List<JComboBox<?>> findCombos(Container root) {
        List<JComboBox<?>> result = new ArrayList<>();
        for (JComboBox<?> combo : findAll(root, JComboBox.class)) {
            result.add(combo);
        }
        return result;
    }

    private static JComboBox<?> findComboWithItemContaining(List<JComboBox<?>> combos, String needle) {
        for (JComboBox<?> combo : combos) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                Object item = combo.getItemAt(i);
                if (item != null && item.toString().contains(needle)) {
                    return combo;
                }
            }
        }
        return null;
    }

    private static JLabel findLabelContaining(Container root, String needle) {
        for (JLabel lbl : findAll(root, JLabel.class)) {
            if (lbl.getText() != null && lbl.getText().contains(needle)) {
                return lbl;
            }
        }
        return null;
    }

    private static <T extends Component> T findFirst(Container root, Class<T> type) {
        List<T> all = findAll(root, type);
        return all.isEmpty() ? null : all.get(0);
    }

    private static <T extends Component> List<T> findAll(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collect(root, type, result);
        return result;
    }

    private static <T extends Component> void collect(Container root, Class<T> type, List<T> out) {
        if (root == null) {
            return;
        }
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                out.add(type.cast(child));
            }
            if (child instanceof Container nested) {
                collect(nested, type, out);
            }
        }
    }

    private static JButton findButtonByText(Container root, String text) {
        for (JButton btn : findAll(root, JButton.class)) {
            if (text.equals(btn.getText())) {
                return btn;
            }
        }
        return null;
    }

    private static JButton findButtonByActionCommandPrefix(Container root, String prefix) {
        for (JButton btn : findAll(root, JButton.class)) {
            String cmd = btn.getActionCommand();
            if (cmd != null && cmd.startsWith(prefix)) {
                return btn;
            }
        }
        return null;
    }

    private static UiKit.PillFilterButton findPillByText(Container root, String text) {
        for (UiKit.PillFilterButton pill : findAll(root, UiKit.PillFilterButton.class)) {
            if (text.equals(pill.getText())) {
                return pill;
            }
        }
        return null;
    }

    private static void runOnEdt(TestAssert.ThrowingRunnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                action.run();
            } catch (RuntimeException | Error ex) {
                throw ex;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            return;
        }
        Throwable[] error = new Throwable[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    action.run();
                } catch (Throwable t) {
                    error[0] = t;
                }
            });
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof Error err) {
                throw err;
            }
            if (cause instanceof Exception e) {
                throw e;
            }
            throw new RuntimeException(cause);
        }
        if (error[0] != null) {
            if (error[0] instanceof Error err) {
                throw err;
            }
            if (error[0] instanceof Exception e) {
                throw e;
            }
            throw new RuntimeException(error[0]);
        }
    }
}
