package pawpaw;

import pawpaw.config.DatabaseConfig;
import pawpaw.dao.OrderDao;
import pawpaw.gui.admin.AdminAdoptionsPanel;
import pawpaw.gui.admin.AdminBookingsPanel;
import pawpaw.gui.admin.AdminOrdersPanel;
import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.IdPillRenderer;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.model.AdoptionRequest;
import pawpaw.model.Animal;
import pawpaw.model.CartItem;
import pawpaw.model.HealthBooking;
import pawpaw.model.HealthService;
import pawpaw.model.Order;
import pawpaw.model.Product;
import pawpaw.model.User;
import pawpaw.util.Theme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.Scrollable;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * EDT smoke tests for AdminOrdersPanel, AdminAdoptionsPanel, AdminBookingsPanel,
 * terminal lock UI rules, full 12-route AppWindow wiring, and Admin/Customer mode switching.
 */
public class AdminTransactionPanelsSmokeTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "AdminTransactionPanelsSmokeTest",
            TestAssert.test("Admin transaction panels instantiate, refresh against live pet_care DB, and filter cleanly on EDT",
                AdminTransactionPanelsSmokeTest::testAdminTransactionPanelsLiveOnEdt),
            TestAssert.test("Admin transaction panels guard against null session and unreachable DB without crashing EDT",
                AdminTransactionPanelsSmokeTest::testNullSessionAndDbFailureGuards),
            TestAssert.test("AppWindow registers all 12 real routes, switches Admin<->Customer modes, and fits 1040x680 & 1440x1080",
                AdminTransactionPanelsSmokeTest::testAppWindowFullRegistrationSwitchAndResponsiveLayout),
            TestAssert.test("Terminal lock UI disables edits for completed/cancelled/approved transactions and sorted selection stays accurate",
                AdminTransactionPanelsSmokeTest::testTerminalLockStatesAndSortedSelection)
        );
    }

    private static void testAdminTransactionPanelsLiveOnEdt() throws Exception {
        runOnEdt(() -> {
            AppWindow window = new AppWindow();
            try {
                window.onLoginSuccess(createSeedAdmin());

                AdminOrdersPanel orders = new AdminOrdersPanel(window);
                AdminAdoptionsPanel adoptions = new AdminAdoptionsPanel(window);
                AdminBookingsPanel bookings = new AdminBookingsPanel(window);

                TestAssert.assertEquals("admin.orders", orders.getPageKey(), "Orders pageKey must match");
                TestAssert.assertEquals("admin.adoptions", adoptions.getPageKey(), "Adoptions pageKey must match");
                TestAssert.assertEquals("admin.bookings", bookings.getPageKey(), "Bookings pageKey must match");

                orders.refreshData();
                adoptions.refreshData();
                bookings.refreshData();

                assertNonBlank(orders.getStatusSummary(), "AdminOrdersPanel statusSummary must be non-empty");
                assertNonBlank(adoptions.getStatusSummary(), "AdminAdoptionsPanel statusSummary must be non-empty");
                assertNonBlank(bookings.getStatusSummary(), "AdminBookingsPanel statusSummary must be non-empty");

                assertNoEmojiGlyphs(orders);
                assertNoEmojiGlyphs(adoptions);
                assertNoEmojiGlyphs(bookings);

                TestAssert.assertEquals(Theme.Admin.BG, orders.getBackground(),
                    "AdminOrdersPanel canvas must use Theme.Admin.BG");
                TestAssert.assertEquals(Theme.Admin.BG, adoptions.getBackground(),
                    "AdminAdoptionsPanel canvas must use Theme.Admin.BG");
                TestAssert.assertEquals(Theme.Admin.BG, bookings.getBackground(),
                    "AdminBookingsPanel canvas must use Theme.Admin.BG");

                // --- AdminOrdersPanel checks ---
                List<JTable> orderTables = findAll(orders, JTable.class);
                TestAssert.assertEquals(2, orderTables.size(),
                    "AdminOrdersPanel must have master orders table and inspector line-items table");
                JTable ordersTable = orderTables.get(0);
                JTable lineItemsTable = orderTables.get(1);
                TestAssert.assertEquals(34, ordersTable.getRowHeight(), "Orders table rowHeight must be 34");
                TestAssert.assertEquals(Theme.Admin.TABLE_HEADER_BG, ordersTable.getTableHeader().getBackground(),
                    "Orders master table header must use Theme.Admin.TABLE_HEADER_BG");
                TestAssert.assertEquals(Theme.Admin.TABLE_HEADER_BG, lineItemsTable.getTableHeader().getBackground(),
                    "Orders line-items table header must use Theme.Admin.TABLE_HEADER_BG");
                int totalOrders = ordersTable.getRowCount();
                TestAssert.assertTrue(totalOrders >= 10, "Seed DB must display at least 10 orders");
                TestAssert.assertTrue(lineItemsTable.getRowCount() >= 1,
                    "Selected seed order must populate line-items table in inspector");

                List<JComboBox<?>> orderCombos = findCombos(orders);
                JComboBox<?> orderFilterCombo = findComboWithItem(orderCombos, "Semua");
                JComboBox<?> orderEditCombo = findComboWithItemContaining(orderCombos, "completed");
                TestAssert.assertNotNull(orderFilterCombo, "Orders status filter combo must exist");
                TestAssert.assertNotNull(orderEditCombo, "Orders status edit combo must exist");

                orderFilterCombo.setSelectedItem("Diproses");
                TestAssert.assertTrue(ordersTable.getRowCount() >= 1,
                    "Orders Diproses filter must match seed processing orders");
                orderFilterCombo.setSelectedItem("Selesai");
                TestAssert.assertTrue(ordersTable.getRowCount() < totalOrders,
                    "Orders Selesai filter must narrow seed processing orders");
                orderFilterCombo.setSelectedItem("Semua");
                TestAssert.assertEquals(totalOrders, ordersTable.getRowCount(),
                    "Resetting Orders filter to Semua must restore all orders");

                JTextField orderSearch = findFirst(orders, JTextField.class);
                TestAssert.assertNotNull(orderSearch, "Orders search field must exist");
                orderSearch.setText("ORD-2026-0001");
                TestAssert.assertEquals(1, ordersTable.getRowCount(),
                    "Searching ORD-2026-0001 must match exactly 1 seed order");
                orderSearch.setText("");
                TestAssert.assertEquals(totalOrders, ordersTable.getRowCount(),
                    "Clearing Orders search must restore all rows");

                JButton orderRefreshBtn = findButtonByText(orders, "Refresh");
                JButton invoiceBtn = findButtonByText(orders, "Lihat Struk");
                JButton saveOrderBtn = findButtonByText(orders, "Simpan Status");
                TestAssert.assertNotNull(orderRefreshBtn, "Orders Refresh button must exist");
                TestAssert.assertNotNull(invoiceBtn, "Orders Lihat Struk button must exist");
                TestAssert.assertNotNull(saveOrderBtn, "Orders Simpan Status button must exist");
                TestAssert.assertTrue(invoiceBtn.isEnabled(), "Lihat Struk must be enabled when order selected");
                TestAssert.assertTrue(saveOrderBtn.isEnabled(),
                    "Simpan Status must be enabled for active processing seed order");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, orderRefreshBtn.getForeground(),
                    "Orders Refresh button must use Theme.Admin.BTN_LIGHT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, invoiceBtn.getForeground(),
                    "Orders Lihat Struk button must use Theme.Admin.BTN_LIGHT_FG");
                TestAssert.assertEquals(Color.WHITE, saveOrderBtn.getForeground(),
                    "Orders Simpan Status button must use white adminPrimaryButton foreground");

                // --- AdminAdoptionsPanel checks ---
                JTable adoptionsTable = findFirst(adoptions, JTable.class);
                TestAssert.assertNotNull(adoptionsTable, "AdminAdoptionsPanel must contain JTable");
                TestAssert.assertEquals(34, adoptionsTable.getRowHeight(), "Adoptions table rowHeight must be 34");
                TestAssert.assertEquals(Theme.Admin.TABLE_HEADER_BG, adoptionsTable.getTableHeader().getBackground(),
                    "Adoptions table header must use Theme.Admin.TABLE_HEADER_BG");
                TestAssert.assertTrue(
                    adoptionsTable.getColumnModel().getColumn(0).getCellRenderer() instanceof IdPillRenderer,
                    "AdminAdoptionsPanel col 0 must use IdPillRenderer"
                );
                int totalAdoptions = adoptionsTable.getRowCount();
                TestAssert.assertTrue(totalAdoptions >= 10, "Seed DB must display at least 10 adoption requests");
                TestAssert.assertNotNull(findFirst(adoptions, PhotoPanel.class),
                    "AdminAdoptionsPanel inspector must contain animal PhotoPanel");

                JComboBox<?> adoptionFilterCombo = findComboWithItem(findCombos(adoptions), "Semua");
                TestAssert.assertNotNull(adoptionFilterCombo, "Adoptions status filter combo must exist");
                adoptionFilterCombo.setSelectedItem("Menunggu");
                TestAssert.assertTrue(adoptionsTable.getRowCount() >= 1,
                    "Adoptions Menunggu filter must match seed pending requests");
                adoptionFilterCombo.setSelectedItem("Disetujui");
                TestAssert.assertTrue(adoptionsTable.getRowCount() < totalAdoptions,
                    "Adoptions Disetujui filter must narrow seed pending requests");
                adoptionFilterCombo.setSelectedItem("Semua");
                TestAssert.assertEquals(totalAdoptions, adoptionsTable.getRowCount(),
                    "Resetting Adoptions filter to Semua must restore all requests");

                JTextField adoptionSearch = findFirst(adoptions, JTextField.class);
                TestAssert.assertNotNull(adoptionSearch, "Adoptions search field must exist");
                adoptionSearch.setText("Jesika");
                TestAssert.assertTrue(adoptionsTable.getRowCount() >= 1 && adoptionsTable.getRowCount() < totalAdoptions,
                    "Searching Jesika must filter adoption requests");
                adoptionSearch.setText("");
                TestAssert.assertEquals(totalAdoptions, adoptionsTable.getRowCount(),
                    "Clearing Adoptions search must restore all rows");

                JButton adoptionRefreshBtn = findButtonByText(adoptions, "Refresh");
                JButton approveBtn = findButtonByText(adoptions, "Setujui");
                JButton rejectBtn = findButtonByText(adoptions, "Tolak");
                JButton pendingBtn = findButtonByText(adoptions, "Pending");
                TestAssert.assertNotNull(adoptionRefreshBtn, "Adoptions Refresh button must exist");
                TestAssert.assertNotNull(approveBtn, "Adoptions Setujui button must exist");
                TestAssert.assertNotNull(rejectBtn, "Adoptions Tolak button must exist");
                TestAssert.assertNotNull(pendingBtn, "Adoptions Pending button must exist");
                TestAssert.assertTrue(approveBtn.isEnabled() && rejectBtn.isEnabled() && pendingBtn.isEnabled(),
                    "Setujui/Tolak/Pending must be enabled for seed pending request with available animal");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, adoptionRefreshBtn.getForeground(),
                    "Adoptions Refresh button must use Theme.Admin.BTN_LIGHT_FG");
                TestAssert.assertEquals(Color.WHITE, approveBtn.getForeground(),
                    "Adoptions Setujui button must use white adminPrimaryButton foreground");
                TestAssert.assertEquals(Theme.Admin.BTN_DELETE_FG, rejectBtn.getForeground(),
                    "Adoptions Tolak button must use Theme.Admin.BTN_DELETE_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, pendingBtn.getForeground(),
                    "Adoptions Pending button must use Theme.Admin.BTN_LIGHT_FG");

                // --- AdminBookingsPanel checks ---
                JTable bookingsTable = findFirst(bookings, JTable.class);
                TestAssert.assertNotNull(bookingsTable, "AdminBookingsPanel must contain JTable");
                TestAssert.assertEquals(34, bookingsTable.getRowHeight(), "Bookings table rowHeight must be 34");
                TestAssert.assertEquals(Theme.Admin.TABLE_HEADER_BG, bookingsTable.getTableHeader().getBackground(),
                    "Bookings table header must use Theme.Admin.TABLE_HEADER_BG");
                TestAssert.assertTrue(
                    bookingsTable.getColumnModel().getColumn(0).getCellRenderer() instanceof IdPillRenderer,
                    "AdminBookingsPanel col 0 must use IdPillRenderer"
                );
                int totalBookings = bookingsTable.getRowCount();
                TestAssert.assertTrue(totalBookings >= 10, "Seed DB must display at least 10 health bookings");

                List<JComboBox<?>> bookingCombos = findCombos(bookings);
                JComboBox<?> bookingFilterCombo = findComboWithItem(bookingCombos, "Semua");
                JComboBox<?> bookingEditCombo = findComboWithItemContaining(bookingCombos, "completed");
                TestAssert.assertNotNull(bookingFilterCombo, "Bookings status filter combo must exist");
                TestAssert.assertNotNull(bookingEditCombo, "Bookings status edit combo must exist");

                bookingFilterCombo.setSelectedItem("Menunggu");
                TestAssert.assertTrue(bookingsTable.getRowCount() >= 1,
                    "Bookings Menunggu filter must match seed pending bookings");
                bookingFilterCombo.setSelectedItem("Selesai");
                TestAssert.assertTrue(bookingsTable.getRowCount() < totalBookings,
                    "Bookings Selesai filter must narrow seed pending bookings");
                bookingFilterCombo.setSelectedItem("Semua");
                TestAssert.assertEquals(totalBookings, bookingsTable.getRowCount(),
                    "Resetting Bookings filter to Semua must restore all bookings");

                JTextField bookingSearch = findFirst(bookings, JTextField.class);
                TestAssert.assertNotNull(bookingSearch, "Bookings search field must exist");
                bookingSearch.setText("Milo");
                TestAssert.assertTrue(bookingsTable.getRowCount() >= 1 && bookingsTable.getRowCount() < totalBookings,
                    "Searching Milo must filter health bookings");
                bookingSearch.setText("");
                TestAssert.assertEquals(totalBookings, bookingsTable.getRowCount(),
                    "Clearing Bookings search must restore all rows");

                JButton bookingRefreshBtn = findButtonByText(bookings, "Refresh");
                JButton cardBtn = findButtonByText(bookings, "Kartu Booking");
                JButton deleteBtn = findButtonByText(bookings, "Hapus Booking");
                JButton saveBookingBtn = findButtonByText(bookings, "Simpan Status");
                TestAssert.assertNotNull(bookingRefreshBtn, "Bookings Refresh button must exist");
                TestAssert.assertNotNull(cardBtn, "Bookings Kartu Booking button must exist");
                TestAssert.assertNotNull(deleteBtn, "Bookings Hapus Booking button must exist");
                TestAssert.assertNotNull(saveBookingBtn, "Bookings Simpan Status button must exist");
                TestAssert.assertTrue(cardBtn.isEnabled() && deleteBtn.isEnabled() && saveBookingBtn.isEnabled(),
                    "Kartu Booking, Hapus Booking, Simpan Status must be enabled for seed pending booking");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, bookingRefreshBtn.getForeground(),
                    "Bookings Refresh button must use Theme.Admin.BTN_LIGHT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, cardBtn.getForeground(),
                    "Bookings Kartu Booking button must use Theme.Admin.BTN_LIGHT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_DELETE_FG, deleteBtn.getForeground(),
                    "Bookings Hapus Booking button must use Theme.Admin.BTN_DELETE_FG");
                TestAssert.assertEquals(Color.WHITE, saveBookingBtn.getForeground(),
                    "Bookings Simpan Status button must use white adminPrimaryButton foreground");
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
                    AdminOrdersPanel orders = new AdminOrdersPanel(window);
                    AdminAdoptionsPanel adoptions = new AdminAdoptionsPanel(window);
                    AdminBookingsPanel bookings = new AdminBookingsPanel(window);

                    orders.refreshData();
                    adoptions.refreshData();
                    bookings.refreshData();

                    assertNonBlank(orders.getStatusSummary(), "Null session orders statusSummary must be non-empty");
                    assertNonBlank(adoptions.getStatusSummary(), "Null session adoptions statusSummary must be non-empty");
                    assertNonBlank(bookings.getStatusSummary(), "Null session bookings statusSummary must be non-empty");

                    TestAssert.assertFalse(findButtonByText(orders, "Simpan Status").isEnabled(),
                        "Orders Simpan Status must be disabled when session is null");
                    TestAssert.assertFalse(findButtonByText(adoptions, "Setujui").isEnabled(),
                        "Adoptions Setujui must be disabled when session is null");
                    TestAssert.assertFalse(findButtonByText(bookings, "Simpan Status").isEnabled(),
                        "Bookings Simpan Status must be disabled when session is null");
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
                    window.getSession().login(createSeedAdmin());
                    AdminOrdersPanel orders = new AdminOrdersPanel(window);
                    AdminAdoptionsPanel adoptions = new AdminAdoptionsPanel(window);
                    AdminBookingsPanel bookings = new AdminBookingsPanel(window);

                    orders.refreshData();
                    adoptions.refreshData();
                    bookings.refreshData();

                    assertNonBlank(orders.getStatusSummary(), "DB-error orders statusSummary must be non-empty");
                    assertNonBlank(adoptions.getStatusSummary(), "DB-error adoptions statusSummary must be non-empty");
                    assertNonBlank(bookings.getStatusSummary(), "DB-error bookings statusSummary must be non-empty");
                } finally {
                    window.dispose();
                }
            });
        } finally {
            DatabaseConfig.resetConfig();
        }
    }

    private static void testAppWindowFullRegistrationSwitchAndResponsiveLayout() throws Exception {
        runOnEdt(() -> {
            AppWindow window = new AppWindow();
            try {
                window.onLoginSuccess(createSeedAdmin());
                window.addNotify();

                // Admin login starts in admin.dashboard
                TestAssert.assertEquals("admin.dashboard", window.getShell().getActivePageKey(),
                    "Admin login must route to admin.dashboard");
                TestAssert.assertFalse(window.getSession().isAdminPreviewAsUser(),
                    "Admin login must start with adminPreviewAsUser == false");
                TestAssert.assertEquals("Mode Pelanggan", window.getShell().getSwitchModeButtonText(),
                    "Admin mode switch button must read Mode Pelanggan");

                for (int[] size : new int[][] { { 1040, 680 }, { 1440, 1080 } }) {
                    window.setSize(size[0], size[1]);

                    window.navigate("admin.orders");
                    window.validate();
                    TestAssert.assertEquals("admin.orders", window.getShell().getActivePageKey(),
                        "Navigating to admin.orders must activate AdminOrdersPanel");
                    AdminOrdersPanel mountedOrders = findFirst(window.getShell(), AdminOrdersPanel.class);
                    TestAssert.assertNotNull(mountedOrders, "AppWindow must mount AdminOrdersPanel");
                    assertTransactionLayoutHealthy(mountedOrders, "pesanan", size[0], size[1]);

                    window.navigate("admin.adoptions");
                    window.validate();
                    TestAssert.assertEquals("admin.adoptions", window.getShell().getActivePageKey(),
                        "Navigating to admin.adoptions must activate AdminAdoptionsPanel");
                    AdminAdoptionsPanel mountedAdoptions = findFirst(window.getShell(), AdminAdoptionsPanel.class);
                    TestAssert.assertNotNull(mountedAdoptions, "AppWindow must mount AdminAdoptionsPanel");
                    assertTransactionLayoutHealthy(mountedAdoptions, "pengajuan", size[0], size[1]);

                    window.navigate("admin.bookings");
                    window.validate();
                    TestAssert.assertEquals("admin.bookings", window.getShell().getActivePageKey(),
                        "Navigating to admin.bookings must activate AdminBookingsPanel");
                    AdminBookingsPanel mountedBookings = findFirst(window.getShell(), AdminBookingsPanel.class);
                    TestAssert.assertNotNull(mountedBookings, "AppWindow must mount AdminBookingsPanel");
                    assertTransactionLayoutHealthy(mountedBookings, "booking", size[0], size[1]);
                }

                // Switch Admin -> Customer preview workspace -> verify 5 user routes -> switch back
                window.openUserWorkspace();
                TestAssert.assertTrue(window.getSession().isAdminPreviewAsUser(),
                    "openUserWorkspace must enable adminPreviewAsUser for admin");
                TestAssert.assertEquals("user.dashboard", window.getShell().getActivePageKey(),
                    "openUserWorkspace must route to user.dashboard");
                TestAssert.assertEquals("Kembali ke Panel Admin", window.getShell().getSwitchModeButtonText(),
                    "User preview mode switch button must read Kembali ke Panel Admin");

                for (String userRoute : List.of(
                    "user.dashboard", "user.adoption", "user.shop", "user.health", "user.profile"
                )) {
                    window.navigate(userRoute);
                    TestAssert.assertEquals(userRoute, window.getShell().getActivePageKey(),
                        "Route " + userRoute + " must activate cleanly");
                }

                window.openAdminWorkspace();
                TestAssert.assertFalse(window.getSession().isAdminPreviewAsUser(),
                    "openAdminWorkspace must clear adminPreviewAsUser");
                TestAssert.assertEquals("admin.dashboard", window.getShell().getActivePageKey(),
                    "openAdminWorkspace must return to admin.dashboard");
                TestAssert.assertEquals("Mode Pelanggan", window.getShell().getSwitchModeButtonText(),
                    "Returning to Admin mode must restore Mode Pelanggan switch button");

                // Ensure all 7 admin routes are real concrete PagePanel subclasses (none PlaceholderPagePanel)
                for (String adminRoute : List.of(
                    "admin.dashboard", "admin.products", "admin.orders",
                    "admin.animals", "admin.adoptions", "admin.services", "admin.bookings"
                )) {
                    window.navigate(adminRoute);
                    TestAssert.assertEquals(adminRoute, window.getShell().getActivePageKey(),
                        "Route " + adminRoute + " must activate cleanly");
                    PagePanel active = findVisiblePagePanel(window.getShell());
                    TestAssert.assertNotNull(active, "Active PagePanel must exist for " + adminRoute);
                    TestAssert.assertFalse(active.getClass().getSimpleName().contains("Placeholder"),
                        "Route " + adminRoute + " must not use PlaceholderPagePanel");
                }
            } finally {
                window.dispose();
            }
        });
    }

    private static void testTerminalLockStatesAndSortedSelection() throws Exception {
        final int[] ids = new int[7]; // [0]=productId, [1]=orderId, [2]=animalId, [3]=serviceId, [4]=bookingId, [5]=cancelAnimalId, [6]=cancelBookingId
        AppWindow cleanupApp = new AppWindow();
        OrderDao orderDao = new OrderDao();
        try {
            runOnEdt(() -> {
                AppWindow window = new AppWindow();
                try {
                    window.onLoginSuccess(createSeedAdmin());
                    String tag = UUID.randomUUID().toString().substring(0, 8);

                    // 1. Order interactive UI status update (pending -> shipped -> completed) + terminal lock + sorted selection
                    Product tempProd = new Product();
                    tempProd.setProductName("UjiOrderProd-" + tag);
                    tempProd.setCategory("makanan");
                    tempProd.setPrice(new BigDecimal("42000.00"));
                    tempProd.setStock(20);
                    Product savedProd = window.getShopService().createProduct(tempProd);
                    ids[0] = savedProd.getProductId();

                    Order createdOrder = window.getShopService().checkout(
                        1,
                        List.of(new CartItem(savedProd, 1)),
                        "Jesika Uji-" + tag,
                        "081200001111",
                        "pickup",
                        "-",
                        "Transfer Bank"
                    );
                    ids[1] = createdOrder.getOrderId();

                    AdminOrdersPanel ordersPanel = new AdminOrdersPanel(window);
                    ordersPanel.refreshData();
                    JTable orderTable = findAll(ordersPanel, JTable.class).get(0);
                    orderTable.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

                    int targetViewRow = findRowContaining(orderTable, createdOrder.getOrderCode());
                    TestAssert.assertTrue(targetViewRow >= 0,
                        "Pending temporary order must appear in AdminOrdersPanel");
                    orderTable.setRowSelectionInterval(targetViewRow, targetViewRow);

                    JLabel orderTitleMatch = findLabelContaining(ordersPanel, createdOrder.getOrderCode());
                    TestAssert.assertNotNull(orderTitleMatch,
                        "Sorted selection in AdminOrdersPanel must sync inspector to " + createdOrder.getOrderCode());

                    JComboBox<?> orderEditCombo = findComboWithItemContaining(
                        findCombos(ordersPanel), "completed");
                    TestAssert.assertNotNull(orderEditCombo, "Order status edit combo must exist");
                    JButton saveOrderBtn = findButtonByText(ordersPanel, "Simpan Status");
                    TestAssert.assertTrue(saveOrderBtn.isEnabled() && orderEditCombo.isEnabled(),
                        "Pending order must enable status combo and Simpan Status");

                    selectComboItemContaining(orderEditCombo, "shipped");
                    saveOrderBtn.doClick();
                    Order afterShip = window.getShopService().getOrderInvoice(ids[1], 11, true);
                    TestAssert.assertEquals("shipped", afterShip.getStatus(),
                        "Clicking Simpan Status with shipped must persist in DB");

                    selectComboItemContaining(orderEditCombo, "completed");
                    saveOrderBtn.doClick();
                    Order afterComplete = window.getShopService().getOrderInvoice(ids[1], 11, true);
                    TestAssert.assertEquals("completed", afterComplete.getStatus(),
                        "Clicking Simpan Status with completed must persist in DB");

                    TestAssert.assertNotNull(findVisibleLabelContaining(ordersPanel, "Status final terkunci"),
                        "Completed order must display visible 'Status final terkunci' note");
                    TestAssert.assertFalse(saveOrderBtn.isEnabled(),
                        "Simpan Status must be disabled for completed order");
                    TestAssert.assertTrue(findButtonByText(ordersPanel, "Lihat Struk").isEnabled(),
                        "Lihat Struk must remain enabled for completed order");
                    TestAssert.assertFalse(orderEditCombo.isEnabled(),
                        "Order status edit combo must be disabled for completed order");

                    // 2. Adoption interactive Tolak/Pending + terminal lock (approved + cancelled) + competing auto-rejected disabled Setujui
                    Animal tempAnimal = new Animal();
                    tempAnimal.setNama("UjiAdopsi-" + tag);
                    tempAnimal.setSpecies("kucing");
                    tempAnimal.setBreed("Domestik");
                    tempAnimal.setGender("Jantan");
                    tempAnimal.setAge("1 Tahun");
                    tempAnimal.setStatus("available");
                    Animal savedAnimal = window.getAdoptionService().createAnimal(tempAnimal);
                    ids[2] = savedAnimal.getAnimalId();

                    AdoptionRequest reqPrimary = window.getAdoptionService().submitRequest(
                        1, ids[2], "PemohonSatu-" + tag, "satu." + tag + "@example.com",
                        "08120001", "Jl. Satu", "Sayang kucing"
                    );
                    AdoptionRequest reqSecondary = window.getAdoptionService().submitRequest(
                        2, ids[2], "PemohonDua-" + tag, "dua." + tag + "@example.com",
                        "08120002", "Jl. Dua", "Ingin merawat"
                    );

                    AdminAdoptionsPanel adoptionsPanel = new AdminAdoptionsPanel(window);
                    adoptionsPanel.refreshData();
                    JTable adoptionTable = findFirst(adoptionsPanel, JTable.class);
                    adoptionTable.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

                    int primaryViewRow = findRowContaining(adoptionTable, "PemohonSatu-" + tag);
                    TestAssert.assertTrue(primaryViewRow >= 0, "Primary adoption request must appear in table");
                    adoptionTable.setRowSelectionInterval(primaryViewRow, primaryViewRow);

                    List<JTextField> adoptionFields = findAll(adoptionsPanel, JTextField.class);
                    TestAssert.assertTrue(adoptionFields.size() >= 2,
                        "AdminAdoptionsPanel must have searchField and Catatan Admin JTextField");
                    JTextField adminNoteField = adoptionFields.get(1);
                    adminNoteField.setText("Perlu verifikasi rumah " + tag);
                    findButtonByText(adoptionsPanel, "Tolak").doClick();

                    AdoptionRequest afterReject = window.getAdoptionService().listAllRequests().stream()
                        .filter(r -> r.getAdoptionRequestId() == reqPrimary.getAdoptionRequestId())
                        .findFirst()
                        .orElseThrow();
                    TestAssert.assertEquals("rejected", afterReject.getStatus(),
                        "Clicking Tolak must update adoption status to rejected");
                    TestAssert.assertEquals("Perlu verifikasi rumah " + tag, afterReject.getAdminNote(),
                        "Clicking Tolak must persist adminNote");

                    findButtonByText(adoptionsPanel, "Pending").doClick();
                    AdoptionRequest afterPending = window.getAdoptionService().listAllRequests().stream()
                        .filter(r -> r.getAdoptionRequestId() == reqPrimary.getAdoptionRequestId())
                        .findFirst()
                        .orElseThrow();
                    TestAssert.assertEquals("pending", afterPending.getStatus(),
                        "Clicking Pending must reopen rejected request to pending");

                    window.getAdoptionService().updateRequestStatus(
                        reqPrimary.getAdoptionRequestId(), "approved", "Disetujui uji"
                    );
                    adoptionsPanel.refreshData();
                    int approvedViewRow = findRowContaining(adoptionTable, "PemohonSatu-" + tag);
                    adoptionTable.setRowSelectionInterval(approvedViewRow, approvedViewRow);

                    TestAssert.assertNotNull(findVisibleLabelContaining(adoptionsPanel, "Status final terkunci"),
                        "Approved adoption request must display visible 'Status final terkunci' note");
                    TestAssert.assertFalse(findButtonByText(adoptionsPanel, "Setujui").isEnabled(),
                        "Setujui must be disabled for approved adoption request");
                    TestAssert.assertFalse(findButtonByText(adoptionsPanel, "Tolak").isEnabled(),
                        "Tolak must be disabled for approved adoption request");
                    TestAssert.assertFalse(findButtonByText(adoptionsPanel, "Pending").isEnabled(),
                        "Pending must be disabled for approved adoption request");
                    TestAssert.assertFalse(adminNoteField.isEnabled(),
                        "Catatan Admin field must be disabled for approved adoption request");

                    // Competing auto-rejected request for already-adopted animal must keep Setujui disabled
                    int rejectedViewRow = findRowContaining(adoptionTable, "PemohonDua-" + tag);
                    TestAssert.assertTrue(rejectedViewRow >= 0, "Auto-rejected request must appear in table");
                    adoptionTable.setRowSelectionInterval(rejectedViewRow, rejectedViewRow);
                    TestAssert.assertFalse(findButtonByText(adoptionsPanel, "Setujui").isEnabled(),
                        "Setujui must be disabled when animalStatus != available");

                    // Cancelled adoption request terminal lock
                    Animal cancelAnimal = new Animal();
                    cancelAnimal.setNama("UjiCancel-" + tag);
                    cancelAnimal.setSpecies("kucing");
                    cancelAnimal.setBreed("Domestik");
                    cancelAnimal.setGender("Betina");
                    cancelAnimal.setAge("1 Tahun");
                    cancelAnimal.setStatus("available");
                    Animal savedCancelAnimal = window.getAdoptionService().createAnimal(cancelAnimal);
                    ids[5] = savedCancelAnimal.getAnimalId();
                    AdoptionRequest reqCancelled = window.getAdoptionService().submitRequest(
                        1, ids[5], "PemohonBatal-" + tag, "batal." + tag + "@example.com",
                        "08120003", "Jl. Batal", "Ingin adopsi"
                    );
                    window.getAdoptionService().cancelUserRequest(1, reqCancelled.getAdoptionRequestId());
                    adoptionsPanel.refreshData();
                    int cancelledAdoptionRow = findRowContaining(adoptionTable, "PemohonBatal-" + tag);
                    TestAssert.assertTrue(cancelledAdoptionRow >= 0, "Cancelled adoption request must appear in table");
                    adoptionTable.setRowSelectionInterval(cancelledAdoptionRow, cancelledAdoptionRow);
                    TestAssert.assertNotNull(findVisibleLabelContaining(adoptionsPanel, "Status final terkunci"),
                        "Cancelled adoption request must display visible 'Status final terkunci' note");
                    TestAssert.assertFalse(findButtonByText(adoptionsPanel, "Setujui").isEnabled(),
                        "Setujui must be disabled for cancelled adoption request");
                    TestAssert.assertFalse(findButtonByText(adoptionsPanel, "Tolak").isEnabled(),
                        "Tolak must be disabled for cancelled adoption request");
                    TestAssert.assertFalse(findButtonByText(adoptionsPanel, "Pending").isEnabled(),
                        "Pending must be disabled for cancelled adoption request");
                    TestAssert.assertFalse(adminNoteField.isEnabled(),
                        "Catatan Admin must be disabled for cancelled adoption request");

                    // 3. HealthBooking interactive UI update (rejected -> approved -> completed) + terminal lock + cancelled lock
                    HealthService tempSvc = new HealthService();
                    tempSvc.setServiceName("UjiBookingSvc-" + tag);
                    tempSvc.setPrice(new BigDecimal("95000.00"));
                    tempSvc.setDoctorName("drh. Uji");
                    tempSvc.setPracticeDays("Senin - Jumat");
                    tempSvc.setTimeSlots("09:00");
                    tempSvc.setDailyQuota(3);
                    HealthService savedSvc = window.getHealthBookingService().createService(tempSvc);
                    ids[3] = savedSvc.getServiceId();

                    HealthBooking createdBooking = window.getHealthBookingService().createBooking(
                        1, ids[3], 1, "PemilikUji-" + tag, "Milo", "Kucing", LocalDate.now().plusDays(5), "09:00"
                    );
                    ids[4] = createdBooking.getBookingId();

                    AdminBookingsPanel bookingsPanel = new AdminBookingsPanel(window);
                    bookingsPanel.refreshData();
                    JTable bookingTable = findFirst(bookingsPanel, JTable.class);
                    bookingTable.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

                    int bookingViewRow = findRowContaining(bookingTable, "PemilikUji-" + tag);
                    TestAssert.assertTrue(bookingViewRow >= 0, "Pending temporary booking must appear in table");
                    bookingTable.setRowSelectionInterval(bookingViewRow, bookingViewRow);

                    JComboBox<?> bookingEditCombo = findComboWithItemContaining(
                        findCombos(bookingsPanel), "completed");
                    TestAssert.assertNotNull(bookingEditCombo, "Booking status edit combo must exist");
                    JButton saveBookingBtn = findButtonByText(bookingsPanel, "Simpan Status");
                    List<JTextField> bookingFields = findAll(bookingsPanel, JTextField.class);
                    TestAssert.assertTrue(bookingFields.size() >= 2,
                        "AdminBookingsPanel must have searchField and Alasan Penolakan JTextField");
                    JTextField reasonField = bookingFields.get(1);

                    reasonField.setText("Dokter tugas luar " + tag);
                    selectComboItemContaining(bookingEditCombo, "rejected");
                    saveBookingBtn.doClick();
                    HealthBooking afterBookingReject = window.getHealthBookingService().getBookingCard(ids[4], 11, true);
                    TestAssert.assertEquals("rejected", afterBookingReject.getStatus(),
                        "Clicking Simpan Status with rejected must persist");
                    TestAssert.assertEquals("Dokter tugas luar " + tag, afterBookingReject.getRejectionReason(),
                        "Clicking Simpan Status with rejected must persist rejectionReason");
                    TestAssert.assertEquals("failed", afterBookingReject.getPayment().getPaymentStatus(),
                        "Rejected booking must sync payment_status=failed");

                    selectComboItemContaining(bookingEditCombo, "completed");
                    saveBookingBtn.doClick();
                    HealthBooking afterBookingComplete = window.getHealthBookingService().getBookingCard(ids[4], 11, true);
                    TestAssert.assertEquals("completed", afterBookingComplete.getStatus(),
                        "Clicking Simpan Status with completed must persist");
                    TestAssert.assertEquals("paid", afterBookingComplete.getPayment().getPaymentStatus(),
                        "Completed booking must sync payment_status=paid");

                    TestAssert.assertNotNull(findLabelContaining(bookingsPanel, "PemilikUji-" + tag),
                        "Sorted selection in AdminBookingsPanel must sync inspector to PemilikUji-" + tag);
                    TestAssert.assertNotNull(findVisibleLabelContaining(bookingsPanel, "Status final terkunci"),
                        "Completed booking must display visible 'Status final terkunci' note");
                    TestAssert.assertFalse(saveBookingBtn.isEnabled(),
                        "Simpan Status must be disabled for completed booking");
                    TestAssert.assertTrue(findButtonByText(bookingsPanel, "Kartu Booking").isEnabled(),
                        "Kartu Booking must remain enabled for completed booking");
                    TestAssert.assertTrue(findButtonByText(bookingsPanel, "Hapus Booking").isEnabled(),
                        "Hapus Booking must remain enabled for completed booking");
                    TestAssert.assertFalse(bookingEditCombo.isEnabled(),
                        "Booking status edit combo must be disabled for completed booking");
                    TestAssert.assertFalse(reasonField.isEnabled(),
                        "Alasan Penolakan field must be disabled for completed booking");

                    // Cancelled booking terminal lock
                    HealthBooking cancelBooking = window.getHealthBookingService().createBooking(
                        1, ids[3], 1, "PemilikBatal-" + tag, "Milo", "Kucing", LocalDate.now().plusDays(6), "09:00"
                    );
                    ids[6] = cancelBooking.getBookingId();
                    window.getHealthBookingService().cancelUserBooking(1, ids[6]);
                    bookingsPanel.refreshData();
                    int cancelledBookingRow = findRowContaining(bookingTable, "PemilikBatal-" + tag);
                    TestAssert.assertTrue(cancelledBookingRow >= 0, "Cancelled booking must appear in table");
                    bookingTable.setRowSelectionInterval(cancelledBookingRow, cancelledBookingRow);
                    TestAssert.assertNotNull(findVisibleLabelContaining(bookingsPanel, "Status final terkunci"),
                        "Cancelled booking must display visible 'Status final terkunci' note");
                    TestAssert.assertFalse(saveBookingBtn.isEnabled(),
                        "Simpan Status must be disabled for cancelled booking");
                    TestAssert.assertFalse(bookingEditCombo.isEnabled(),
                        "Booking status edit combo must be disabled for cancelled booking");
                    Object cancelledSelected = bookingEditCombo.getSelectedItem();
                    TestAssert.assertNotNull(cancelledSelected,
                        "Disabled status combo must display a selected item for cancelled booking");
                    String cancelledDisplay = cancelledSelected.toString();
                    TestAssert.assertTrue(cancelledDisplay.contains("Dibatalkan"),
                        "Disabled status combo for cancelled booking must contain Dibatalkan (was: " + cancelledDisplay + ")");
                    TestAssert.assertFalse(cancelledDisplay.contains("Menunggu"),
                        "Disabled status combo for cancelled booking must NOT display Menunggu (was: " + cancelledDisplay + ")");
                    TestAssert.assertEquals(1, bookingEditCombo.getItemCount(),
                        "Cancelled booking status combo must contain only the read-only cancelled option");
                    TestAssert.assertFalse(reasonField.isEnabled(),
                        "Alasan Penolakan field must be disabled for cancelled booking");
                    TestAssert.assertTrue(findButtonByText(bookingsPanel, "Hapus Booking").isEnabled(),
                        "Hapus Booking must remain enabled for cancelled booking");

                    // Clearing selection after a cancelled row must restore editable 4-option model (still disabled)
                    bookingTable.clearSelection();
                    TestAssert.assertFalse(bookingEditCombo.isEnabled(),
                        "Cleared selection must keep booking status combo disabled");
                    TestAssert.assertEquals(4, bookingEditCombo.getItemCount(),
                        "Cleared selection must restore 4 editable status options");
                    assertComboHasNoCancelledOption(bookingEditCombo, "cleared selection");

                    // Re-select cancelled row -> switch directly to active pending row -> must re-enable with 4 editable options and NO cancelled
                    bookingTable.setRowSelectionInterval(cancelledBookingRow, cancelledBookingRow);
                    TestAssert.assertTrue(bookingEditCombo.getSelectedItem() != null
                            && bookingEditCombo.getSelectedItem().toString().contains("Dibatalkan"),
                        "Re-selecting cancelled booking must again show Dibatalkan");
                    int activePendingRow = findRowWithStatus(bookingTable, 6, "pending");
                    TestAssert.assertTrue(activePendingRow >= 0,
                        "Seed DB must contain an active pending booking row to verify editable model restoration");
                    bookingTable.setRowSelectionInterval(activePendingRow, activePendingRow);
                    TestAssert.assertTrue(bookingEditCombo.isEnabled(),
                        "Selecting active pending booking after cancelled must re-enable bookingEditCombo");
                    TestAssert.assertEquals(4, bookingEditCombo.getItemCount(),
                        "Active pending booking must restore 4 editable status options");
                    assertComboHasNoCancelledOption(bookingEditCombo, "active pending booking");
                    TestAssert.assertTrue(bookingEditCombo.getSelectedItem() != null
                            && bookingEditCombo.getSelectedItem().toString().contains("Menunggu"),
                        "Active pending booking must select Menunggu (pending) in restored combo");

                    // 4. Verify mounted 1040x680 layout on terminal & non-available rows across all 3 panels
                    window.addNotify();
                    window.setSize(1040, 680);

                    window.navigate("admin.orders");
                    AdminOrdersPanel mountedOrders = findFirst(window.getShell(), AdminOrdersPanel.class);
                    JTable mountedOrderTable = findAll(mountedOrders, JTable.class).get(0);
                    int completedOrdRow = findRowContaining(mountedOrderTable, createdOrder.getOrderCode());
                    TestAssert.assertTrue(completedOrdRow >= 0, "Mounted orders table must show completed temp order");
                    mountedOrderTable.setRowSelectionInterval(completedOrdRow, completedOrdRow);
                    window.validate();
                    TestAssert.assertNotNull(findVisibleLabelContaining(mountedOrders, "Status final terkunci"),
                        "Mounted AdminOrdersPanel must show visible terminal lock note at 1040x680");
                    assertTransactionLayoutHealthy(mountedOrders, "pesanan", 1040, 680);

                    window.navigate("admin.adoptions");
                    AdminAdoptionsPanel mountedAdoptions = findFirst(window.getShell(), AdminAdoptionsPanel.class);
                    JTable mountedAdoptTable = findFirst(mountedAdoptions, JTable.class);
                    int approvedAdoptRow = findRowContaining(mountedAdoptTable, "PemohonSatu-" + tag);
                    TestAssert.assertTrue(approvedAdoptRow >= 0, "Mounted adoptions table must show approved temp request");
                    mountedAdoptTable.setRowSelectionInterval(approvedAdoptRow, approvedAdoptRow);
                    window.validate();
                    TestAssert.assertNotNull(findVisibleLabelContaining(mountedAdoptions, "Status final terkunci"),
                        "Mounted AdminAdoptionsPanel must show visible terminal lock note at 1040x680");
                    assertTransactionLayoutHealthy(mountedAdoptions, "pengajuan", 1040, 680);

                    int autoRejAdoptRow = findRowContaining(mountedAdoptTable, "PemohonDua-" + tag);
                    TestAssert.assertTrue(autoRejAdoptRow >= 0, "Mounted adoptions table must show auto-rejected request");
                    mountedAdoptTable.setRowSelectionInterval(autoRejAdoptRow, autoRejAdoptRow);
                    window.validate();
                    TestAssert.assertNotNull(findVisibleLabelContaining(mountedAdoptions, "Hewan berstatus"),
                        "Mounted AdminAdoptionsPanel must show visible non-available animal warning at 1040x680");
                    assertTransactionLayoutHealthy(mountedAdoptions, "pengajuan", 1040, 680);

                    window.navigate("admin.bookings");
                    AdminBookingsPanel mountedBookings = findFirst(window.getShell(), AdminBookingsPanel.class);
                    JTable mountedBookingTable = findFirst(mountedBookings, JTable.class);
                    int completedBookingRow = findRowContaining(mountedBookingTable, "PemilikUji-" + tag);
                    TestAssert.assertTrue(completedBookingRow >= 0, "Mounted bookings table must show completed temp booking");
                    mountedBookingTable.setRowSelectionInterval(completedBookingRow, completedBookingRow);
                    window.validate();
                    TestAssert.assertNotNull(findVisibleLabelContaining(mountedBookings, "Status final terkunci"),
                        "Mounted AdminBookingsPanel must show visible terminal lock note at 1040x680");
                    assertTransactionLayoutHealthy(mountedBookings, "booking", 1040, 680);

                    int cancelledBookingMountedRow = findRowContaining(mountedBookingTable, "PemilikBatal-" + tag);
                    TestAssert.assertTrue(cancelledBookingMountedRow >= 0, "Mounted bookings table must show cancelled temp booking");
                    mountedBookingTable.setRowSelectionInterval(cancelledBookingMountedRow, cancelledBookingMountedRow);
                    window.validate();
                    TestAssert.assertNotNull(findVisibleLabelContaining(mountedBookings, "Status final terkunci"),
                        "Mounted AdminBookingsPanel must show visible terminal lock note for cancelled booking at 1040x680");
                    assertTransactionLayoutHealthy(mountedBookings, "booking", 1040, 680);
                } finally {
                    window.dispose();
                }
            });
        } finally {
            if (ids[1] > 0) {
                try {
                    orderDao.delete(ids[1]);
                } catch (Exception ignored) {
                }
            }
            if (ids[0] > 0) {
                try {
                    cleanupApp.getShopService().deleteProduct(ids[0]);
                } catch (Exception ignored) {
                }
            }
            if (ids[2] > 0) {
                try {
                    cleanupApp.getAdoptionService().deleteAnimal(ids[2]);
                } catch (Exception ignored) {
                }
            }
            if (ids[5] > 0) {
                try {
                    cleanupApp.getAdoptionService().deleteAnimal(ids[5]);
                } catch (Exception ignored) {
                }
            }
            if (ids[4] > 0) {
                try {
                    cleanupApp.getHealthBookingService().deleteBooking(ids[4]);
                } catch (Exception ignored) {
                }
            }
            if (ids[6] > 0) {
                try {
                    cleanupApp.getHealthBookingService().deleteBooking(ids[6]);
                } catch (Exception ignored) {
                }
            }
            if (ids[3] > 0) {
                try {
                    cleanupApp.getHealthBookingService().deleteService(ids[3]);
                } catch (Exception ignored) {
                }
            }
            cleanupApp.dispose();
        }
    }

    private static void selectComboItemContaining(JComboBox<?> combo, String needle) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            if (item != null && item.toString().contains(needle)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        throw new AssertionError("Combo item containing '" + needle + "' not found");
    }

    private static void assertTransactionLayoutHealthy(Container panel, String countWord, int w, int h) {
        String context = panel.getClass().getSimpleName() + "@" + w + "x" + h;

        JTextField search = findFirst(panel, JTextField.class);
        JComboBox<?> filterCombo = findComboWithItem(findCombos(panel), "Semua");
        JLabel count = findLabelContaining(panel, countWord);
        JButton refreshBtn = findButtonByText(panel, "Refresh");
        TestAssert.assertNotNull(search, context + " search field must exist");
        TestAssert.assertNotNull(filterCombo, context + " status filter combo must exist");
        TestAssert.assertNotNull(count, context + " count label (" + countWord + ") must exist");
        TestAssert.assertNotNull(refreshBtn, context + " Refresh button must exist");

        Container leftRow = search.getParent();
        Container rightRow = count.getParent();
        Container toolbarCard = leftRow.getParent();
        TestAssert.assertEquals(toolbarCard, rightRow.getParent(),
            context + " toolbar leftRow and rightRow must share the same toolbarCard");

        assertSiblingBoundsContainedAndNonOverlapping(leftRow, context + ".toolbarLeft");
        assertSiblingBoundsContainedAndNonOverlapping(rightRow, context + ".toolbarRight");
        assertSiblingBoundsContainedAndNonOverlapping(toolbarCard, context + ".toolbarCard");

        Rectangle searchRect = SwingUtilities.convertRectangle(leftRow, search.getBounds(), toolbarCard);
        Rectangle filterRect = SwingUtilities.convertRectangle(leftRow, filterCombo.getBounds(), toolbarCard);
        Rectangle countRect = SwingUtilities.convertRectangle(rightRow, count.getBounds(), toolbarCard);
        Rectangle refreshRect = SwingUtilities.convertRectangle(rightRow, refreshBtn.getBounds(), toolbarCard);
        TestAssert.assertTrue(searchRect.x + searchRect.width <= countRect.x,
            context + " search field overlaps count label");
        TestAssert.assertTrue(filterRect.x + filterRect.width <= countRect.x,
            context + " status filter combo (" + (filterRect.x + filterRect.width)
                + ") overlaps count label (" + countRect.x + ")");
        TestAssert.assertTrue(countRect.x + countRect.width <= refreshRect.x,
            context + " count label overlaps Refresh button");

        JSplitPane split = findFirst(panel, JSplitPane.class);
        TestAssert.assertNotNull(split, context + " must use JSplitPane master-detail layout");
        TestAssert.assertEquals(JSplitPane.VERTICAL_SPLIT, split.getOrientation(),
            context + " JSplitPane must be vertical split");

        JScrollPane inspectorScroll = null;
        for (JScrollPane sp : findAll(panel, JScrollPane.class)) {
            Component view = sp.getViewport().getView();
            if (view instanceof Scrollable s && s.getScrollableTracksViewportWidth()) {
                inspectorScroll = sp;
                break;
            }
        }
        TestAssert.assertNotNull(inspectorScroll,
            context + " bottom inspector must use width-tracking verticalScrollPane");

        JTable masterTable = findFirst(panel, JTable.class);
        TestAssert.assertNotNull(masterTable, context + " master table must exist");
        TestAssert.assertTrue(masterTable.getParent().getHeight() >= 100,
            context + " master table viewport height (" + masterTable.getParent().getHeight() + "px) must be >= 100px");
        TestAssert.assertTrue(split.getBottomComponent().getHeight() >= 150,
            context + " bottom inspector height (" + split.getBottomComponent().getHeight() + "px) must be >= 150px");

        Container scrollView = (Container) inspectorScroll.getViewport().getView();
        assertInspectorTreeHealthy(scrollView, context + ".inspector");
    }

    private static void assertInspectorTreeHealthy(Container container, String context) {
        if (container == null || !container.isVisible()) {
            return;
        }
        assertSiblingBoundsContainedAndNonOverlapping(container, context);
        for (Component child : container.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            if (child instanceof JScrollPane || child instanceof JTable || child instanceof JComboBox<?>) {
                continue;
            }
            if (child instanceof JPanel nested) {
                assertInspectorTreeHealthy(nested, context + "/" + nested.getClass().getSimpleName());
            }
        }
    }

    private static void assertSiblingBoundsContainedAndNonOverlapping(Container container, String context) {
        if (container == null || !container.isVisible()) {
            return;
        }
        int cw = container.getWidth();
        int ch = container.getHeight();
        Insets insets = container instanceof JComponent jc ? jc.getInsets() : new Insets(0, 0, 0, 0);
        int maxRight = cw - insets.right;

        List<Rectangle> seen = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            Rectangle b = child.getBounds();
            String label = child.getClass().getSimpleName();
            if (child instanceof JButton btn) {
                label = "JButton[" + btn.getText() + "]";
            } else if (child instanceof JLabel lbl) {
                label = "JLabel[" + lbl.getText() + "]";
            }
            TestAssert.assertTrue(b.width > 0 && b.height > 0,
                context + " child " + label + " must have positive size (was " + b.width + "x" + b.height + ")");
            TestAssert.assertTrue(b.x >= insets.left && b.x + b.width <= maxRight,
                context + " child " + label + " (" + b.x + ".." + (b.x + b.width)
                    + ") exceeds container width bounds (" + insets.left + ".." + maxRight + ", cw=" + cw + ")");
            TestAssert.assertTrue(b.y >= 0 && b.y + b.height <= ch,
                context + " child " + label + " vertical bounds (" + b.y + ".." + (b.y + b.height)
                    + ") exceed container height " + ch);
            for (int i = 0; i < seen.size(); i++) {
                Rectangle prev = seen.get(i);
                TestAssert.assertFalse(b.intersects(prev),
                    context + " overlap between " + names.get(i) + " " + prev + " and " + label + " " + b);
            }
            seen.add(b);
            names.add(label);
        }
    }

    private static int findRowContaining(JTable table, String needle) {
        for (int r = 0; r < table.getRowCount(); r++) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                Object val = table.getValueAt(r, c);
                if (val != null && val.toString().contains(needle)) {
                    return r;
                }
            }
        }
        return -1;
    }

    private static int findRowWithStatus(JTable table, int statusCol, String expectedStatus) {
        for (int r = 0; r < table.getRowCount(); r++) {
            Object val = table.getValueAt(r, statusCol);
            if (val != null && expectedStatus.equalsIgnoreCase(val.toString().trim())) {
                return r;
            }
        }
        return -1;
    }

    private static void assertComboHasNoCancelledOption(JComboBox<?> combo, String context) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            String text = item == null ? "" : item.toString().toLowerCase(Locale.ROOT);
            TestAssert.assertFalse(text.contains("cancelled") || text.contains("dibatalkan"),
                "Booking edit combo (" + context + ") must NEVER contain cancelled option (found: " + item + ")");
        }
    }

    private static PagePanel findVisiblePagePanel(Container root) {
        for (PagePanel p : findAll(root, PagePanel.class)) {
            if (p.isVisible()) {
                return p;
            }
        }
        return null;
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

    private static User createSeedAdmin() {
        User admin = new User();
        admin.setUserId(11);
        admin.setUsername("admin_pawpaw");
        admin.setName("Admin PawPaw");
        admin.setEmail("admin@pawpaw.com");
        admin.setPhone("081122334455");
        admin.setAddress("Jl. Kemang Raya No. 88, Jakarta Selatan");
        admin.setRole("admin");
        return admin;
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

    private static JComboBox<?> findComboWithItem(List<JComboBox<?>> combos, String exact) {
        for (JComboBox<?> combo : combos) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                Object item = combo.getItemAt(i);
                if (item != null && exact.equals(item.toString())) {
                    return combo;
                }
            }
        }
        return null;
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

    private static JLabel findVisibleLabelContaining(Container root, String needle) {
        for (JLabel lbl : findAll(root, JLabel.class)) {
            if (lbl.isShowing() || lbl.isVisible()) {
                if (lbl.isVisible() && lbl.getText() != null && lbl.getText().contains(needle)) {
                    return lbl;
                }
            }
        }
        return null;
    }

    private static JButton findButtonByText(Container root, String text) {
        for (JButton btn : findAll(root, JButton.class)) {
            if (text.equals(btn.getText())) {
                return btn;
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
