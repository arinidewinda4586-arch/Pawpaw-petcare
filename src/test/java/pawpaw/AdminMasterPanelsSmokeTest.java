package pawpaw;

import pawpaw.config.DatabaseConfig;
import pawpaw.gui.admin.AdminAnimalsPanel;
import pawpaw.gui.admin.AdminDashboardPanel;
import pawpaw.gui.admin.AdminProductsPanel;
import pawpaw.gui.admin.AdminServicesPanel;
import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.IdPillRenderer;
import pawpaw.model.Animal;
import pawpaw.model.HealthService;
import pawpaw.model.Product;
import pawpaw.model.User;
import pawpaw.util.Theme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SortOrder;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * EDT smoke tests for AdminDashboardPanel, AdminProductsPanel, AdminAnimalsPanel,
 * AdminServicesPanel, their modal CRUD dialogs, and AppWindow route registrations.
 */
public class AdminMasterPanelsSmokeTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "AdminMasterPanelsSmokeTest",
            TestAssert.test("Admin dashboard and master CRUD panels instantiate, refresh against live pet_care DB, and filter/gate cleanly on EDT",
                AdminMasterPanelsSmokeTest::testAdminMasterPanelsLiveOnEdt),
            TestAssert.test("Admin dashboard and master panels guard against null session and unreachable DB without crashing EDT",
                AdminMasterPanelsSmokeTest::testNullSessionAndDbFailureGuards),
            TestAssert.test("Admin modal CRUD dialogs prefill, wrap forms in width-tracking Scrollable, and validate inline cleanly",
                AdminMasterPanelsSmokeTest::testModalDialogsConstructScrollablePrefillAndValidate),
            TestAssert.test("AppWindow registers admin.dashboard, admin.products, admin.animals, admin.services without overlap at 1040x680 and 1440x1080",
                AdminMasterPanelsSmokeTest::testAppWindowRegistrationAndResponsiveLayout),
            TestAssert.test("Modal CRUD dialogs create/edit against live MySQL, sorted selection stays accurate, and cleanup restores DB",
                AdminMasterPanelsSmokeTest::testLiveDialogCrudRoundTripAndSortedSelection)
        );
    }

    private static void testAdminMasterPanelsLiveOnEdt() throws Exception {
        runOnEdt(() -> {
            AppWindow window = new AppWindow();
            try {
                window.onLoginSuccess(createSeedAdmin());

                AdminDashboardPanel dashboard = new AdminDashboardPanel(window);
                AdminProductsPanel products = new AdminProductsPanel(window);
                AdminAnimalsPanel animals = new AdminAnimalsPanel(window);
                AdminServicesPanel services = new AdminServicesPanel(window);

                TestAssert.assertEquals("admin.dashboard", dashboard.getPageKey(), "Dashboard pageKey must match");
                TestAssert.assertEquals("admin.products", products.getPageKey(), "Products pageKey must match");
                TestAssert.assertEquals("admin.animals", animals.getPageKey(), "Animals pageKey must match");
                TestAssert.assertEquals("admin.services", services.getPageKey(), "Services pageKey must match");

                dashboard.refreshData();
                products.refreshData();
                animals.refreshData();
                services.refreshData();

                assertNonBlank(dashboard.getStatusSummary(), "AdminDashboardPanel statusSummary must be non-empty");
                assertNonBlank(products.getStatusSummary(), "AdminProductsPanel statusSummary must be non-empty");
                assertNonBlank(animals.getStatusSummary(), "AdminAnimalsPanel statusSummary must be non-empty");
                assertNonBlank(services.getStatusSummary(), "AdminServicesPanel statusSummary must be non-empty");

                assertNoEmojiGlyphs(dashboard);
                assertNoEmojiGlyphs(products);
                assertNoEmojiGlyphs(animals);
                assertNoEmojiGlyphs(services);

                TestAssert.assertEquals(Theme.Admin.BG, dashboard.getBackground(),
                    "AdminDashboardPanel canvas must use Theme.Admin.BG");
                TestAssert.assertEquals(Theme.Admin.BG, products.getBackground(),
                    "AdminProductsPanel canvas must use Theme.Admin.BG");
                TestAssert.assertEquals(Theme.Admin.BG, animals.getBackground(),
                    "AdminAnimalsPanel canvas must use Theme.Admin.BG");
                TestAssert.assertEquals(Theme.Admin.BG, services.getBackground(),
                    "AdminServicesPanel canvas must use Theme.Admin.BG");

                // --- AdminDashboardPanel checks ---
                TestAssert.assertNotNull(findLabelContaining(dashboard, "Total Pendapatan"),
                    "Dashboard must show highlighted Total Pendapatan KPI card");
                JLabel revValueLabel = findLabelContaining(dashboard, "Rp ");
                TestAssert.assertNotNull(revValueLabel,
                    "Dashboard revenue KPI must display formatted Rupiah value");
                TestAssert.assertEquals(Theme.GOLD, revValueLabel.getForeground(),
                    "Dashboard revenue KPI value must use Theme.GOLD");
                TestAssert.assertEquals(Theme.Admin.NAVY, revValueLabel.getParent().getBackground(),
                    "Dashboard revenue hero card must use Theme.Admin.NAVY");
                for (String kpiTitle : List.of(
                    "Produk", "Pesanan", "Hewan Adopsi", "Pengajuan Adopsi", "Booking Kesehatan", "Pelanggan"
                )) {
                    TestAssert.assertNotNull(findLabelByText(dashboard, kpiTitle),
                        "Dashboard must include KPI card: " + kpiTitle);
                }

                JButton quickProd = findButtonByText(dashboard, "+ Produk");
                JButton quickAnim = findButtonByText(dashboard, "+ Hewan");
                JButton quickSvc = findButtonByText(dashboard, "+ Layanan");
                TestAssert.assertNotNull(quickProd, "Quick action + Produk button must exist");
                TestAssert.assertNotNull(quickAnim, "Quick action + Hewan button must exist");
                TestAssert.assertNotNull(quickSvc, "Quick action + Layanan button must exist");
                TestAssert.assertTrue(quickProd.isEnabled() && quickAnim.isEnabled() && quickSvc.isEnabled(),
                    "Quick action buttons must be enabled for authenticated admin");
                TestAssert.assertEquals(Color.WHITE, quickProd.getForeground(),
                    "Quick action + Produk must use white adminPrimaryButton foreground");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, quickAnim.getForeground(),
                    "Quick action + Hewan must use Theme.Admin.BTN_LIGHT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, quickSvc.getForeground(),
                    "Quick action + Layanan must use Theme.Admin.BTN_LIGHT_FG");

                JTabbedPane dashTabs = findFirst(dashboard, JTabbedPane.class);
                TestAssert.assertNotNull(dashTabs, "Dashboard must contain JTabbedPane of recent activities");
                TestAssert.assertEquals(3, dashTabs.getTabCount(), "Dashboard must have 3 recent activity tabs");
                TestAssert.assertEquals("Pesanan Terbaru", dashTabs.getTitleAt(0), "Tab 0 must be Pesanan Terbaru");
                TestAssert.assertEquals("Adopsi Terbaru", dashTabs.getTitleAt(1), "Tab 1 must be Adopsi Terbaru");
                TestAssert.assertEquals("Booking Terbaru", dashTabs.getTitleAt(2), "Tab 2 must be Booking Terbaru");

                List<JTable> dashTables = findAll(dashboard, JTable.class);
                TestAssert.assertEquals(3, dashTables.size(), "Dashboard must contain 3 recent activity tables");
                for (JTable t : dashTables) {
                    TestAssert.assertEquals(34, t.getRowHeight(), "Dashboard table rowHeight must be 34");
                    TestAssert.assertEquals(Theme.Admin.TABLE_HEADER_BG, t.getTableHeader().getBackground(),
                        "Dashboard table header must use Theme.Admin.TABLE_HEADER_BG");
                    TestAssert.assertTrue(t.getRowCount() >= 1 && t.getRowCount() <= 5,
                        "Recent activity table must show 1..5 rows on seed DB");
                }
                TestAssert.assertTrue(
                    dashTables.get(1).getColumnModel().getColumn(0).getCellRenderer() instanceof IdPillRenderer,
                    "Dashboard recent adoptions col 0 must use IdPillRenderer"
                );
                TestAssert.assertTrue(
                    dashTables.get(2).getColumnModel().getColumn(0).getCellRenderer() instanceof IdPillRenderer,
                    "Dashboard recent bookings col 0 must use IdPillRenderer"
                );

                // --- AdminProductsPanel checks ---
                JTable productTable = findFirst(products, JTable.class);
                TestAssert.assertNotNull(productTable, "AdminProductsPanel must contain JTable");
                TestAssert.assertEquals(34, productTable.getRowHeight(), "Products table rowHeight must be 34");
                TestAssert.assertEquals(Theme.Admin.TABLE_HEADER_BG, productTable.getTableHeader().getBackground(),
                    "Products table header must use Theme.Admin.TABLE_HEADER_BG");
                TestAssert.assertTrue(
                    productTable.getColumnModel().getColumn(0).getCellRenderer() instanceof IdPillRenderer,
                    "AdminProductsPanel col 0 must use IdPillRenderer"
                );
                int totalProducts = productTable.getRowCount();
                TestAssert.assertTrue(totalProducts >= 10, "Seed DB must display at least 10 products");

                @SuppressWarnings("unchecked")
                JComboBox<String> prodCatCombo = (JComboBox<String>) findFirst(products, JComboBox.class);
                TestAssert.assertNotNull(prodCatCombo, "Products category combo must exist");
                prodCatCombo.setSelectedItem("Makanan");
                int makananCount = productTable.getRowCount();
                TestAssert.assertTrue(makananCount > 0 && makananCount < totalProducts,
                    "Makanan category filter must narrow products");
                prodCatCombo.setSelectedItem("Semua");
                TestAssert.assertEquals(totalProducts, productTable.getRowCount(),
                    "Resetting category filter to Semua must restore all products");

                JTextField prodSearch = findFirst(products, JTextField.class);
                TestAssert.assertNotNull(prodSearch, "Products search field must exist");
                prodSearch.setText("Whiskas");
                TestAssert.assertTrue(productTable.getRowCount() >= 1 && productTable.getRowCount() < totalProducts,
                    "Search Whiskas must filter products");
                prodSearch.setText("");
                TestAssert.assertEquals(totalProducts, productTable.getRowCount(),
                    "Clearing products search must restore all rows");

                JButton prodAdd = findButtonByPrefix(products, "+ Tambah");
                JButton prodEdit = findButtonByText(products, "Edit");
                JButton prodDel = findButtonByText(products, "Hapus");
                JButton prodRefresh = findButtonByText(products, "Refresh");
                TestAssert.assertNotNull(prodAdd, "Products + Tambah button must exist");
                TestAssert.assertNotNull(prodEdit, "Products Edit button must exist");
                TestAssert.assertNotNull(prodDel, "Products Hapus button must exist");
                TestAssert.assertNotNull(prodRefresh, "Products Refresh button must exist");
                TestAssert.assertEquals(Color.WHITE, prodAdd.getForeground(),
                    "Products + Tambah button must use white adminPrimaryButton foreground");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, prodRefresh.getForeground(),
                    "Products Refresh button must use Theme.Admin.BTN_LIGHT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_EDIT_FG, prodEdit.getForeground(),
                    "Products Edit button must use Theme.Admin.BTN_EDIT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_DELETE_FG, prodDel.getForeground(),
                    "Products Hapus button must use Theme.Admin.BTN_DELETE_FG");
                productTable.setRowSelectionInterval(0, 0);
                TestAssert.assertTrue(prodEdit.isEnabled() && prodDel.isEnabled(),
                    "Products Edit/Hapus must be enabled when a row is selected");
                productTable.clearSelection();
                TestAssert.assertFalse(prodEdit.isEnabled() || prodDel.isEnabled(),
                    "Products Edit/Hapus must be disabled when selection is cleared");

                // FK delete guard verification for seed ordered product #1
                TestAssert.assertThrows(IllegalStateException.class,
                    () -> window.getShopService().deleteProduct(1),
                    "Deleting ordered product #1 must throw IllegalStateException FK guard");

                // --- AdminAnimalsPanel checks ---
                JTable animalTable = findFirst(animals, JTable.class);
                TestAssert.assertNotNull(animalTable, "AdminAnimalsPanel must contain JTable");
                TestAssert.assertEquals(34, animalTable.getRowHeight(), "Animals table rowHeight must be 34");
                TestAssert.assertEquals(Theme.Admin.TABLE_HEADER_BG, animalTable.getTableHeader().getBackground(),
                    "Animals table header must use Theme.Admin.TABLE_HEADER_BG");
                TestAssert.assertTrue(
                    animalTable.getColumnModel().getColumn(0).getCellRenderer() instanceof IdPillRenderer,
                    "AdminAnimalsPanel col 0 must use IdPillRenderer"
                );
                int totalAnimals = animalTable.getRowCount();
                TestAssert.assertTrue(totalAnimals >= 10, "Seed DB must display at least 10 animals");

                List<JComboBox<?>> animalCombos = findCombos(animals);
                TestAssert.assertTrue(animalCombos.size() >= 2,
                    "AdminAnimalsPanel must have species and status filter combos");
                JComboBox<?> speciesFilter = findComboWithItem(animalCombos, "Kucing");
                JComboBox<?> statusFilter = findComboWithItem(animalCombos, "Tersedia");
                TestAssert.assertNotNull(speciesFilter, "Animals species filter must exist");
                TestAssert.assertNotNull(statusFilter, "Animals status filter must exist");

                speciesFilter.setSelectedItem("Kucing");
                int catCount = animalTable.getRowCount();
                TestAssert.assertTrue(catCount > 0 && catCount < totalAnimals,
                    "Kucing filter must narrow animals");
                speciesFilter.setSelectedItem("Semua");
                TestAssert.assertEquals(totalAnimals, animalTable.getRowCount(),
                    "Resetting species filter must restore all animals");

                statusFilter.setSelectedItem("Tidak Tersedia");
                TestAssert.assertTrue(animalTable.getRowCount() < totalAnimals,
                    "Status filter Tidak Tersedia must narrow seed animals");
                statusFilter.setSelectedItem("Semua");
                TestAssert.assertEquals(totalAnimals, animalTable.getRowCount(),
                    "Resetting status filter must restore all animals");

                JTextField animalSearch = findFirst(animals, JTextField.class);
                TestAssert.assertNotNull(animalSearch, "Animals search field must exist");
                animalSearch.setText("Anggora");
                TestAssert.assertTrue(animalTable.getRowCount() >= 1 && animalTable.getRowCount() < totalAnimals,
                    "Search Anggora must filter animals");
                animalSearch.setText("");
                TestAssert.assertEquals(totalAnimals, animalTable.getRowCount(),
                    "Clearing animals search must restore all rows");

                JButton animalAdd = findButtonByPrefix(animals, "+ Tambah");
                JButton animalEdit = findButtonByText(animals, "Edit");
                JButton animalDel = findButtonByText(animals, "Hapus");
                JButton animalRefresh = findButtonByText(animals, "Refresh");
                TestAssert.assertNotNull(animalAdd, "Animals + Tambah button must exist");
                TestAssert.assertNotNull(animalEdit, "Animals Edit button must exist");
                TestAssert.assertNotNull(animalDel, "Animals Hapus button must exist");
                TestAssert.assertNotNull(animalRefresh, "Animals Refresh button must exist");
                TestAssert.assertEquals(Color.WHITE, animalAdd.getForeground(),
                    "Animals + Tambah button must use white adminPrimaryButton foreground");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, animalRefresh.getForeground(),
                    "Animals Refresh button must use Theme.Admin.BTN_LIGHT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_EDIT_FG, animalEdit.getForeground(),
                    "Animals Edit button must use Theme.Admin.BTN_EDIT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_DELETE_FG, animalDel.getForeground(),
                    "Animals Hapus button must use Theme.Admin.BTN_DELETE_FG");
                animalTable.setRowSelectionInterval(0, 0);
                TestAssert.assertTrue(animalEdit.isEnabled() && animalDel.isEnabled(),
                    "Animals Edit/Hapus must be enabled when a row is selected");
                animalTable.clearSelection();
                TestAssert.assertFalse(animalEdit.isEnabled() || animalDel.isEnabled(),
                    "Animals Edit/Hapus must be disabled when selection is cleared");

                // --- AdminServicesPanel checks ---
                JTable serviceTable = findFirst(services, JTable.class);
                TestAssert.assertNotNull(serviceTable, "AdminServicesPanel must contain JTable");
                TestAssert.assertEquals(34, serviceTable.getRowHeight(), "Services table rowHeight must be 34");
                TestAssert.assertEquals(Theme.Admin.TABLE_HEADER_BG, serviceTable.getTableHeader().getBackground(),
                    "Services table header must use Theme.Admin.TABLE_HEADER_BG");
                TestAssert.assertTrue(
                    serviceTable.getColumnModel().getColumn(0).getCellRenderer() instanceof IdPillRenderer,
                    "AdminServicesPanel col 0 must use IdPillRenderer"
                );
                int totalServices = serviceTable.getRowCount();
                TestAssert.assertTrue(totalServices >= 10, "Seed DB must display at least 10 health services");

                JTextField serviceSearch = findFirst(services, JTextField.class);
                TestAssert.assertNotNull(serviceSearch, "Services search field must exist");
                serviceSearch.setText("Vaksin");
                TestAssert.assertTrue(serviceTable.getRowCount() >= 1 && serviceTable.getRowCount() < totalServices,
                    "Search Vaksin must filter health services");
                serviceSearch.setText("");
                TestAssert.assertEquals(totalServices, serviceTable.getRowCount(),
                    "Clearing services search must restore all rows");

                JButton serviceAdd = findButtonByPrefix(services, "+ Tambah");
                JButton serviceEdit = findButtonByText(services, "Edit");
                JButton serviceDel = findButtonByText(services, "Hapus");
                JButton serviceRefresh = findButtonByText(services, "Refresh");
                TestAssert.assertNotNull(serviceAdd, "Services + Tambah button must exist");
                TestAssert.assertNotNull(serviceEdit, "Services Edit button must exist");
                TestAssert.assertNotNull(serviceDel, "Services Hapus button must exist");
                TestAssert.assertNotNull(serviceRefresh, "Services Refresh button must exist");
                TestAssert.assertEquals(Color.WHITE, serviceAdd.getForeground(),
                    "Services + Tambah button must use white adminPrimaryButton foreground");
                TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, serviceRefresh.getForeground(),
                    "Services Refresh button must use Theme.Admin.BTN_LIGHT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_EDIT_FG, serviceEdit.getForeground(),
                    "Services Edit button must use Theme.Admin.BTN_EDIT_FG");
                TestAssert.assertEquals(Theme.Admin.BTN_DELETE_FG, serviceDel.getForeground(),
                    "Services Hapus button must use Theme.Admin.BTN_DELETE_FG");
                serviceTable.setRowSelectionInterval(0, 0);
                TestAssert.assertTrue(serviceEdit.isEnabled() && serviceDel.isEnabled(),
                    "Services Edit/Hapus must be enabled when a row is selected");
                serviceTable.clearSelection();
                TestAssert.assertFalse(serviceEdit.isEnabled() || serviceDel.isEnabled(),
                    "Services Edit/Hapus must be disabled when selection is cleared");

                // FK delete guard verification for seed booked service #1
                TestAssert.assertThrows(IllegalStateException.class,
                    () -> window.getHealthBookingService().deleteService(1),
                    "Deleting booked health service #1 must throw IllegalStateException FK guard");
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
                    AdminDashboardPanel dashboard = new AdminDashboardPanel(window);
                    AdminProductsPanel products = new AdminProductsPanel(window);
                    AdminAnimalsPanel animals = new AdminAnimalsPanel(window);
                    AdminServicesPanel services = new AdminServicesPanel(window);

                    dashboard.refreshData();
                    products.refreshData();
                    animals.refreshData();
                    services.refreshData();

                    assertNonBlank(dashboard.getStatusSummary(), "Null session dashboard statusSummary must be non-empty");
                    assertNonBlank(products.getStatusSummary(), "Null session products statusSummary must be non-empty");
                    assertNonBlank(animals.getStatusSummary(), "Null session animals statusSummary must be non-empty");
                    assertNonBlank(services.getStatusSummary(), "Null session services statusSummary must be non-empty");
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
                    AdminDashboardPanel dashboard = new AdminDashboardPanel(window);
                    AdminProductsPanel products = new AdminProductsPanel(window);
                    AdminAnimalsPanel animals = new AdminAnimalsPanel(window);
                    AdminServicesPanel services = new AdminServicesPanel(window);

                    dashboard.refreshData();
                    products.refreshData();
                    animals.refreshData();
                    services.refreshData();

                    assertNonBlank(dashboard.getStatusSummary(), "DB-error dashboard statusSummary must be non-empty");
                    assertNonBlank(products.getStatusSummary(), "DB-error products statusSummary must be non-empty");
                    assertNonBlank(animals.getStatusSummary(), "DB-error animals statusSummary must be non-empty");
                    assertNonBlank(services.getStatusSummary(), "DB-error services statusSummary must be non-empty");
                } finally {
                    window.dispose();
                }
            });
        } finally {
            DatabaseConfig.resetConfig();
        }
    }

    private static void testModalDialogsConstructScrollablePrefillAndValidate() throws Exception {
        runOnEdt(() -> {
            TestAssert.assertFalse(AdminProductsPanel.openProductDialog(null, null),
                "openProductDialog(null, null) must return false");
            TestAssert.assertFalse(AdminAnimalsPanel.openAnimalDialog(null, null),
                "openAnimalDialog(null, null) must return false");
            TestAssert.assertFalse(AdminServicesPanel.openServiceDialog(null, null),
                "openServiceDialog(null, null) must return false");

            AppWindow window = new AppWindow();
            try {
                // Non-admin user cannot open admin CRUD dialogs
                User customer = new User();
                customer.setUserId(1);
                customer.setRole("user");
                window.getSession().login(customer);
                TestAssert.assertFalse(AdminProductsPanel.openProductDialog(window, null),
                    "Non-admin openProductDialog must return false");
                TestAssert.assertFalse(AdminAnimalsPanel.openAnimalDialog(window, null),
                    "Non-admin openAnimalDialog must return false");
                TestAssert.assertFalse(AdminServicesPanel.openServiceDialog(window, null),
                    "Non-admin openServiceDialog must return false");

                window.getSession().login(createSeedAdmin());

                // Product dialog create validation + edit prefill
                AdminProductsPanel.ProductFormDialog createProdDialog =
                    new AdminProductsPanel.ProductFormDialog(window, null);
                try {
                    TestAssert.assertEquals(Theme.Admin.BG, createProdDialog.getContentPane().getBackground(),
                        "ProductFormDialog root background must use Theme.Admin.BG");
                    assertScrollableTracksViewportWidth(createProdDialog.getContentPane(), "ProductFormDialog");
                    TestAssert.assertNotNull(createProdDialog.getRootPane().getDefaultButton(),
                        "ProductFormDialog must set default Enter submit button");

                    JButton choosePhotoBtn = findButtonByText(createProdDialog.getContentPane(), "Pilih Foto");
                    JButton cancelBtn = findButtonByText(createProdDialog.getContentPane(), "Batal");
                    TestAssert.assertNotNull(choosePhotoBtn, "ProductFormDialog must have Pilih Foto button");
                    TestAssert.assertNotNull(cancelBtn, "ProductFormDialog must have Batal button");
                    TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, choosePhotoBtn.getForeground(),
                        "ProductFormDialog Pilih Foto must use Theme.Admin.BTN_LIGHT_FG");
                    TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, cancelBtn.getForeground(),
                        "ProductFormDialog Batal must use Theme.Admin.BTN_LIGHT_FG");

                    JButton saveBtn = findButtonByText(createProdDialog.getContentPane(), "Simpan Produk");
                    TestAssert.assertNotNull(saveBtn, "ProductFormDialog must have Simpan Produk button");
                    saveBtn.doClick();
                    TestAssert.assertFalse(createProdDialog.isSaved(),
                        "Empty product form must not save");
                    assertNonBlank(createProdDialog.getInlineErrorText(),
                        "ProductFormDialog must show inline error banner when product name is blank");
                } finally {
                    createProdDialog.dispose();
                }

                Product sampleProd = new Product();
                sampleProd.setProductId(1);
                sampleProd.setProductName("Whiskas Adult Tuna 1 kg");
                sampleProd.setCategory("makanan");
                sampleProd.setPrice(new BigDecimal("65000.00"));
                sampleProd.setStock(50);
                sampleProd.setImage("assets/img/shop/whiskas.png");
                sampleProd.setDescription("Makanan kucing dewasa.");
                AdminProductsPanel.ProductFormDialog editProdDialog =
                    new AdminProductsPanel.ProductFormDialog(window, sampleProd);
                try {
                    boolean prefilled = findAll(editProdDialog.getContentPane(), JTextField.class).stream()
                        .anyMatch(f -> "Whiskas Adult Tuna 1 kg".equals(f.getText()));
                    TestAssert.assertTrue(prefilled, "ProductFormDialog edit mode must prefill productName");
                } finally {
                    editProdDialog.dispose();
                }

                // Animal dialog create validation + edit prefill
                AdminAnimalsPanel.AnimalFormDialog createAnimalDialog =
                    new AdminAnimalsPanel.AnimalFormDialog(window, null);
                try {
                    TestAssert.assertEquals(Theme.Admin.BG, createAnimalDialog.getContentPane().getBackground(),
                        "AnimalFormDialog root background must use Theme.Admin.BG");
                    assertScrollableTracksViewportWidth(createAnimalDialog.getContentPane(), "AnimalFormDialog");
                    TestAssert.assertNotNull(createAnimalDialog.getRootPane().getDefaultButton(),
                        "AnimalFormDialog must set default Enter submit button");

                    List<JComboBox<?>> animalDialogCombos = findCombos(createAnimalDialog.getContentPane());
                    JComboBox<?> animalStatusCombo = findComboWithItem(animalDialogCombos, "Tersedia (available)");
                    TestAssert.assertNotNull(animalStatusCombo,
                        "AnimalFormDialog status combo must display Indonesian label Tersedia (available)");
                    TestAssert.assertFalse(animalStatusCombo.isEditable(),
                        "AnimalFormDialog status combo must remain non-editable");
                    TestAssert.assertNull(findComboWithItem(animalDialogCombos, "available"),
                        "AnimalFormDialog status combo must NOT display raw English 'available'");
                    TestAssert.assertNotNull(findComboWithItem(animalDialogCombos, "Sudah Diadopsi (adopted)"),
                        "AnimalFormDialog status combo must include Sudah Diadopsi (adopted)");
                    TestAssert.assertNotNull(findComboWithItem(animalDialogCombos, "Tidak Tersedia (unavailable)"),
                        "AnimalFormDialog status combo must include Tidak Tersedia (unavailable)");

                    JButton saveBtn = findButtonByText(createAnimalDialog.getContentPane(), "Simpan Hewan");
                    TestAssert.assertNotNull(saveBtn, "AnimalFormDialog must have Simpan Hewan button");
                    saveBtn.doClick();
                    TestAssert.assertFalse(createAnimalDialog.isSaved(),
                        "Empty animal form must not save");
                    assertNonBlank(createAnimalDialog.getInlineErrorText(),
                        "AnimalFormDialog must show inline error banner when animal name is blank");
                } finally {
                    createAnimalDialog.dispose();
                }

                Animal sampleAnimal = new Animal();
                sampleAnimal.setAnimalId(1);
                sampleAnimal.setNama("Siti");
                sampleAnimal.setSpecies("kucing");
                sampleAnimal.setBreed("Anggora");
                sampleAnimal.setGender("Betina");
                sampleAnimal.setAge("1 Tahun");
                sampleAnimal.setStatus("adopted");
                sampleAnimal.setImage("assets/img/anggora.jpg");
                AdminAnimalsPanel.AnimalFormDialog editAnimalDialog =
                    new AdminAnimalsPanel.AnimalFormDialog(window, sampleAnimal);
                try {
                    boolean prefilled = findAll(editAnimalDialog.getContentPane(), JTextField.class).stream()
                        .anyMatch(f -> "Siti".equals(f.getText()));
                    TestAssert.assertTrue(prefilled, "AnimalFormDialog edit mode must prefill nama");
                    JComboBox<?> editStatusCombo = findComboWithItem(
                        findCombos(editAnimalDialog.getContentPane()), "Sudah Diadopsi (adopted)"
                    );
                    TestAssert.assertNotNull(editStatusCombo, "AnimalFormDialog edit mode status combo must exist");
                    TestAssert.assertEquals("Sudah Diadopsi (adopted)",
                        editStatusCombo.getSelectedItem().toString(),
                        "AnimalFormDialog must prefill Sudah Diadopsi (adopted) for status=adopted");
                } finally {
                    editAnimalDialog.dispose();
                }

                // HealthService dialog create validation + edit prefill
                AdminServicesPanel.ServiceFormDialog createSvcDialog =
                    new AdminServicesPanel.ServiceFormDialog(window, null);
                try {
                    TestAssert.assertEquals(Theme.Admin.BG, createSvcDialog.getContentPane().getBackground(),
                        "ServiceFormDialog root background must use Theme.Admin.BG");
                    assertScrollableTracksViewportWidth(createSvcDialog.getContentPane(), "ServiceFormDialog");
                    TestAssert.assertNotNull(createSvcDialog.getRootPane().getDefaultButton(),
                        "ServiceFormDialog must set default Enter submit button");

                    TestAssert.assertNotNull(findLabelContaining(createSvcDialog.getContentPane(), "Warna Kartu"),
                        "ServiceFormDialog must include Warna Kartu label");
                    List<JComboBox<?>> svcCombos = findCombos(createSvcDialog.getContentPane());
                    JComboBox<?> colorCombo = findComboWithItem(svcCombos, "Hijau Pastel");
                    TestAssert.assertNotNull(colorCombo,
                        "ServiceFormDialog must include Warna Kartu combo containing Hijau Pastel");
                    TestAssert.assertFalse(colorCombo.isEditable(),
                        "ServiceFormDialog Warna Kartu combo must be non-editable");
                    TestAssert.assertNotNull(findComboWithItem(svcCombos, "Oranye Pastel"),
                        "ServiceFormDialog Warna Kartu combo must include Oranye Pastel");

                    JButton saveBtn = findButtonByText(createSvcDialog.getContentPane(), "Simpan Layanan");
                    TestAssert.assertNotNull(saveBtn, "ServiceFormDialog must have Simpan Layanan button");
                    saveBtn.doClick();
                    TestAssert.assertFalse(createSvcDialog.isSaved(),
                        "Empty service form must not save");
                    assertNonBlank(createSvcDialog.getInlineErrorText(),
                        "ServiceFormDialog must show inline error banner when service name is blank");
                } finally {
                    createSvcDialog.dispose();
                }

                HealthService sampleSvc = new HealthService();
                sampleSvc.setServiceId(1);
                sampleSvc.setServiceName("Pemeriksaan Umum");
                sampleSvc.setDoctorName("drh. Clara Wijaya");
                sampleSvc.setPracticeDays("Senin - Sabtu");
                sampleSvc.setTimeSlots("09:00, 11:00, 14:00, 16:00");
                sampleSvc.setDailyQuota(8);
                sampleSvc.setPrice(new BigDecimal("100000.00"));
                sampleSvc.setIcon("\uD83E\uDE7A");
                sampleSvc.setCardColor("card-blue");
                AdminServicesPanel.ServiceFormDialog editSvcDialog =
                    new AdminServicesPanel.ServiceFormDialog(window, sampleSvc);
                try {
                    assertNoEmojiGlyphs(editSvcDialog.getContentPane());
                    boolean prefilled = findAll(editSvcDialog.getContentPane(), JTextField.class).stream()
                        .anyMatch(f -> "Pemeriksaan Umum".equals(f.getText()));
                    TestAssert.assertTrue(prefilled, "ServiceFormDialog edit mode must prefill serviceName");
                    JComboBox<?> editColorCombo = findComboWithItem(
                        findCombos(editSvcDialog.getContentPane()), "Biru Pastel"
                    );
                    TestAssert.assertNotNull(editColorCombo, "ServiceFormDialog edit mode must have Warna Kartu combo");
                    TestAssert.assertEquals("Biru Pastel",
                        editColorCombo.getSelectedItem().toString(),
                        "ServiceFormDialog must prefill Biru Pastel for card-blue");
                } finally {
                    editSvcDialog.dispose();
                }
            } finally {
                window.dispose();
            }
        });
    }

    private static void testAppWindowRegistrationAndResponsiveLayout() throws Exception {
        runOnEdt(() -> {
            AppWindow window = new AppWindow();
            try {
                window.onLoginSuccess(createSeedAdmin());
                window.addNotify();

                // Admin login must route directly to admin.dashboard
                TestAssert.assertEquals("admin.dashboard", window.getShell().getActivePageKey(),
                    "Admin login must activate admin.dashboard");
                AdminDashboardPanel mountedDash = findFirst(window.getShell(), AdminDashboardPanel.class);
                TestAssert.assertNotNull(mountedDash, "AppWindow must mount AdminDashboardPanel for admin.dashboard");
                assertNonBlank(window.getShell().getStatusSummaryText(),
                    "Shell status summary must be populated on admin.dashboard");

                for (int[] size : new int[][] { { 1040, 680 }, { 1440, 1080 } }) {
                    window.setSize(size[0], size[1]);

                    window.navigate("admin.dashboard");
                    window.validate();
                    assertDashboardLayoutHealthy(mountedDash, size[0], size[1], false);

                    window.navigate("admin.products");
                    window.validate();
                    TestAssert.assertEquals("admin.products", window.getShell().getActivePageKey(),
                        "Navigating to admin.products must activate AdminProductsPanel");
                    AdminProductsPanel mountedProducts = findFirst(window.getShell(), AdminProductsPanel.class);
                    TestAssert.assertNotNull(mountedProducts, "AppWindow must mount AdminProductsPanel");
                    assertToolbarNoOverlap(mountedProducts, "produk", size[0], size[1]);

                    window.navigate("admin.animals");
                    window.validate();
                    TestAssert.assertEquals("admin.animals", window.getShell().getActivePageKey(),
                        "Navigating to admin.animals must activate AdminAnimalsPanel");
                    AdminAnimalsPanel mountedAnimals = findFirst(window.getShell(), AdminAnimalsPanel.class);
                    TestAssert.assertNotNull(mountedAnimals, "AppWindow must mount AdminAnimalsPanel");
                    assertToolbarNoOverlap(mountedAnimals, "hewan", size[0], size[1]);

                    window.navigate("admin.services");
                    window.validate();
                    TestAssert.assertEquals("admin.services", window.getShell().getActivePageKey(),
                        "Navigating to admin.services must activate AdminServicesPanel");
                    AdminServicesPanel mountedServices = findFirst(window.getShell(), AdminServicesPanel.class);
                    TestAssert.assertNotNull(mountedServices, "AppWindow must mount AdminServicesPanel");
                    assertToolbarNoOverlap(mountedServices, "layanan", size[0], size[1]);
                }

                // Verify single-step resize 1440x1080 -> 1040x680 -> 1440x1080 updates outer ScrollPane scrollbar and expansion cleanly
                window.navigate("admin.dashboard");
                window.setSize(1440, 1080);
                window.validate();
                JScrollPane outerScroll = findWidthTrackingScrollPane(mountedDash);
                TestAssert.assertNotNull(outerScroll, "AdminDashboardPanel must host outer width-tracking JScrollPane");
                JViewport outerVp = outerScroll.getViewport();
                Container hostView = (Container) outerVp.getView();
                Component dashBody = hostView.getComponent(0);
                TestAssert.assertTrue(dashBody.getHeight() >= outerVp.getHeight(),
                    "Dashboard body (" + dashBody.getHeight() + "px) must expand to fill 1440x1080 viewport ("
                        + outerVp.getHeight() + "px)");
                TestAssert.assertFalse(outerScroll.getVerticalScrollBar().isVisible(),
                    "At 1440x1080 outer vertical scrollbar must stay hidden");

                window.setSize(1040, 680);
                window.validate();
                TestAssert.assertTrue(dashBody.getPreferredSize().height > outerVp.getHeight(),
                    "At 1040x680 dashboard preferred height (" + dashBody.getPreferredSize().height
                        + "px) must exceed viewport height (" + outerVp.getHeight() + "px)");
                TestAssert.assertTrue(outerScroll.getVerticalScrollBar().isVisible(),
                    "Shrinking 1440x1080 -> 1040x680 in a single validate() must show outer vertical scrollbar");
                assertDashboardLayoutHealthy(mountedDash, 1040, 680, false);

                window.setSize(1440, 1080);
                window.validate();
                TestAssert.assertTrue(dashBody.getHeight() >= outerVp.getHeight(),
                    "Resizing back 1040x680 -> 1440x1080 must re-expand dashboard body ("
                        + dashBody.getHeight() + "px >= " + outerVp.getHeight() + "px)");
                TestAssert.assertFalse(outerScroll.getVerticalScrollBar().isVisible(),
                    "Resizing back 1040x680 -> 1440x1080 must hide outer vertical scrollbar");

                // Also verify recent-activity JTable viewport keeps >= 100px height at 1040x680 when inlineErrorLabel is visible
                try {
                    DatabaseConfig.overrideForTest(
                        "jdbc:mysql://127.0.0.1:33999/pet_care?connectTimeout=500",
                        "root",
                        ""
                    );
                    window.setSize(1040, 680);
                    window.navigate("admin.dashboard");
                    window.validate();
                    assertDashboardLayoutHealthy(mountedDash, 1040, 680, true);
                } finally {
                    DatabaseConfig.resetConfig();
                }
            } finally {
                window.dispose();
            }
        });
    }

    private static void testLiveDialogCrudRoundTripAndSortedSelection() throws Exception {
        final int[] createdIds = new int[3]; // [0]=productId, [1]=animalId, [2]=serviceId
        AppWindow cleanupApp = new AppWindow();
        try {
            runOnEdt(() -> {
                AppWindow window = new AppWindow();
                try {
                    window.onLoginSuccess(createSeedAdmin());
                    String tag = UUID.randomUUID().toString().substring(0, 8);

                    // 1. Product create + sort + edit
                    String prodName = "UjiProduk-" + tag;
                    AdminProductsPanel.ProductFormDialog prodDialog =
                        new AdminProductsPanel.ProductFormDialog(window, null);
                    try {
                        TestAssert.assertTrue(prodDialog.getHeight() <= 640,
                            "ProductFormDialog height (" + prodDialog.getHeight() + ") must fit comfortably on 680px screen");
                        List<JTextField> fields = findAll(prodDialog.getContentPane(), JTextField.class);
                        fields.get(0).setText(prodName);      // nameField
                        fields.get(2).setText("88000");       // priceField
                        fields.get(3).setText("14");          // stockField
                        JButton saveBtn = findButtonByText(prodDialog.getContentPane(), "Simpan Produk");
                        TestAssert.assertNotNull(saveBtn, "Simpan Produk button must exist");
                        saveBtn.doClick();
                        TestAssert.assertTrue(prodDialog.isSaved(),
                            "Valid ProductFormDialog submission must succeed (error: " + prodDialog.getInlineErrorText() + ")");
                    } finally {
                        prodDialog.dispose();
                    }

                    Product createdProd = window.getShopService().listProducts().stream()
                        .filter(p -> prodName.equals(p.getProductName()))
                        .findFirst()
                        .orElse(null);
                    TestAssert.assertNotNull(createdProd, "Created product must persist in MySQL");
                    createdIds[0] = createdProd.getProductId();

                    AdminProductsPanel productsPanel = new AdminProductsPanel(window);
                    productsPanel.refreshData();
                    JTable prodTable = findFirst(productsPanel, JTable.class);
                    TestAssert.assertNotNull(prodTable, "Products table must exist");
                    // Sort ascending by Price (col 3) so convertRowIndexToModel mapping is exercised
                    prodTable.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(3, SortOrder.ASCENDING)));
                    prodTable.setRowSelectionInterval(0, 0);
                    int modelRow = prodTable.convertRowIndexToModel(0);
                    Object sortedName = prodTable.getModel().getValueAt(modelRow, 1);
                    JLabel previewTitle = findLabelContaining(productsPanel, sortedName.toString());
                    TestAssert.assertNotNull(previewTitle,
                        "Sorted selection preview must match model row via convertRowIndexToModel");

                    String updatedProdName = "UjiProdukEdit-" + tag;
                    AdminProductsPanel.ProductFormDialog editProdDialog =
                        new AdminProductsPanel.ProductFormDialog(window, createdProd);
                    try {
                        List<JTextField> fields = findAll(editProdDialog.getContentPane(), JTextField.class);
                        fields.get(0).setText(updatedProdName);
                        findButtonByText(editProdDialog.getContentPane(), "Simpan Produk").doClick();
                        TestAssert.assertTrue(editProdDialog.isSaved(), "Product edit dialog must save cleanly");
                    } finally {
                        editProdDialog.dispose();
                    }
                    productsPanel.refreshData();
                    TestAssert.assertNotNull(
                        window.getShopService().listProducts().stream()
                            .filter(p -> p.getProductId() == createdIds[0] && updatedProdName.equals(p.getProductName()))
                            .findFirst()
                            .orElse(null),
                        "Updated product name must persist in MySQL"
                    );

                    // 2. Animal create + edit (verifying Indonesian StatusOption persistence)
                    String animalName = "UjiHewan-" + tag;
                    AdminAnimalsPanel.AnimalFormDialog animDialog =
                        new AdminAnimalsPanel.AnimalFormDialog(window, null);
                    try {
                        TestAssert.assertTrue(animDialog.getHeight() <= 640,
                            "AnimalFormDialog height (" + animDialog.getHeight() + ") must fit comfortably on 680px screen");
                        List<JTextField> fields = findAll(animDialog.getContentPane(), JTextField.class);
                        fields.get(0).setText(animalName);   // nameField
                        fields.get(2).setText("Persia");     // breedField (index 1 is editable species combo editor!)
                        JComboBox<?> statusCombo = findComboWithItem(
                            findCombos(animDialog.getContentPane()), "Tersedia (available)"
                        );
                        TestAssert.assertNotNull(statusCombo, "AnimalFormDialog must have Indonesian status combo");
                        selectComboItemExact(statusCombo, "Tidak Tersedia (unavailable)");
                        findButtonByText(animDialog.getContentPane(), "Simpan Hewan").doClick();
                        TestAssert.assertTrue(animDialog.isSaved(),
                            "Valid AnimalFormDialog submission must succeed (error: " + animDialog.getInlineErrorText() + ")");
                    } finally {
                        animDialog.dispose();
                    }

                    Animal createdAnimal = window.getAdoptionService().listAnimals().stream()
                        .filter(a -> animalName.equals(a.getNama()))
                        .findFirst()
                        .orElse(null);
                    TestAssert.assertNotNull(createdAnimal, "Created animal must persist in MySQL");
                    TestAssert.assertEquals("unavailable", createdAnimal.getStatus(),
                        "Selected Indonesian status option must persist exact DB code 'unavailable'");
                    createdIds[1] = createdAnimal.getAnimalId();

                    AdminAnimalsPanel.AnimalFormDialog editAnimalCrudDialog =
                        new AdminAnimalsPanel.AnimalFormDialog(window, createdAnimal);
                    try {
                        JComboBox<?> editStatusCombo = findComboWithItem(
                            findCombos(editAnimalCrudDialog.getContentPane()), "Sudah Diadopsi (adopted)"
                        );
                        TestAssert.assertEquals("Tidak Tersedia (unavailable)",
                            editStatusCombo.getSelectedItem().toString(),
                            "AnimalFormDialog edit mode must prefill Tidak Tersedia (unavailable)");
                        selectComboItemExact(editStatusCombo, "Sudah Diadopsi (adopted)");
                        findButtonByText(editAnimalCrudDialog.getContentPane(), "Simpan Hewan").doClick();
                        TestAssert.assertTrue(editAnimalCrudDialog.isSaved(),
                            "Animal edit dialog must save cleanly");
                    } finally {
                        editAnimalCrudDialog.dispose();
                    }

                    Animal updatedAnimal = window.getAdoptionService().listAnimals().stream()
                        .filter(a -> a.getAnimalId() == createdIds[1])
                        .findFirst()
                        .orElse(null);
                    TestAssert.assertNotNull(updatedAnimal, "Updated animal must remain in MySQL");
                    TestAssert.assertEquals("adopted", updatedAnimal.getStatus(),
                        "Updated Indonesian status option must persist exact DB code 'adopted'");

                    // 3. HealthService create + edit (verifying pastel cardColor persistence)
                    String svcName = "UjiLayanan-" + tag;
                    AdminServicesPanel.ServiceFormDialog svcDialog =
                        new AdminServicesPanel.ServiceFormDialog(window, null);
                    try {
                        TestAssert.assertTrue(svcDialog.getHeight() <= 640,
                            "ServiceFormDialog height (" + svcDialog.getHeight() + ") must fit comfortably on 680px screen");
                        List<JTextField> fields = findAll(svcDialog.getContentPane(), JTextField.class);
                        fields.get(0).setText(svcName);      // serviceNameField
                        fields.get(5).setText("135000");     // priceField
                        JComboBox<?> colorCombo = findComboWithItem(
                            findCombos(svcDialog.getContentPane()), "Oranye Pastel"
                        );
                        TestAssert.assertNotNull(colorCombo, "ServiceFormDialog must have Oranye Pastel option");
                        selectComboItemExact(colorCombo, "Oranye Pastel");
                        findButtonByText(svcDialog.getContentPane(), "Simpan Layanan").doClick();
                        TestAssert.assertTrue(svcDialog.isSaved(),
                            "Valid ServiceFormDialog submission must succeed (error: " + svcDialog.getInlineErrorText() + ")");
                    } finally {
                        svcDialog.dispose();
                    }

                    HealthService createdSvc = window.getHealthBookingService().listServices().stream()
                        .filter(s -> svcName.equals(s.getServiceName()))
                        .findFirst()
                        .orElse(null);
                    TestAssert.assertNotNull(createdSvc, "Created health service must persist in MySQL");
                    TestAssert.assertEquals("card-orange", createdSvc.getCardColor(),
                        "Oranye Pastel selection must persist cardColor=card-orange in MySQL");
                    createdIds[2] = createdSvc.getServiceId();

                    AdminServicesPanel.ServiceFormDialog editSvcCrudDialog =
                        new AdminServicesPanel.ServiceFormDialog(window, createdSvc);
                    try {
                        JComboBox<?> editColorCombo = findComboWithItem(
                            findCombos(editSvcCrudDialog.getContentPane()), "Ungu Pastel"
                        );
                        TestAssert.assertEquals("Oranye Pastel",
                            editColorCombo.getSelectedItem().toString(),
                            "ServiceFormDialog edit mode must prefill Oranye Pastel for card-orange");
                        selectComboItemExact(editColorCombo, "Ungu Pastel");
                        findButtonByText(editSvcCrudDialog.getContentPane(), "Simpan Layanan").doClick();
                        TestAssert.assertTrue(editSvcCrudDialog.isSaved(),
                            "Service edit dialog must save cleanly");
                    } finally {
                        editSvcCrudDialog.dispose();
                    }

                    HealthService updatedSvc = window.getHealthBookingService().listServices().stream()
                        .filter(s -> s.getServiceId() == createdIds[2])
                        .findFirst()
                        .orElse(null);
                    TestAssert.assertNotNull(updatedSvc, "Updated health service must remain in MySQL");
                    TestAssert.assertEquals("card-purple", updatedSvc.getCardColor(),
                        "Ungu Pastel selection must persist cardColor=card-purple in MySQL");

                    AdminDashboardPanel dash = new AdminDashboardPanel(window);
                    dash.refreshData();
                    assertNonBlank(dash.getStatusSummary(), "Dashboard refresh after CRUD must succeed");
                } finally {
                    window.dispose();
                }
            });
        } finally {
            if (createdIds[0] > 0) {
                try {
                    cleanupApp.getShopService().deleteProduct(createdIds[0]);
                } catch (Exception ignored) {
                }
            }
            if (createdIds[1] > 0) {
                try {
                    cleanupApp.getAdoptionService().deleteAnimal(createdIds[1]);
                } catch (Exception ignored) {
                }
            }
            if (createdIds[2] > 0) {
                try {
                    cleanupApp.getHealthBookingService().deleteService(createdIds[2]);
                } catch (Exception ignored) {
                }
            }
            cleanupApp.dispose();
        }
    }

    private static void assertToolbarNoOverlap(Container panel, String countWord, int w, int h) {
        JTextField search = findFirst(panel, JTextField.class);
        JLabel count = findLabelContaining(panel, countWord);
        TestAssert.assertNotNull(search, "Toolbar search field must exist in " + panel.getClass().getSimpleName());
        TestAssert.assertNotNull(count, "Toolbar count label (" + countWord + ") must exist in " + panel.getClass().getSimpleName());

        Container row = count.getParent();
        Rectangle searchRect = SwingUtilities.convertRectangle(search.getParent(), search.getBounds(), row);
        Rectangle countRect = SwingUtilities.convertRectangle(count.getParent(), count.getBounds(), row);
        TestAssert.assertTrue(searchRect.x + searchRect.width <= countRect.x,
            panel.getClass().getSimpleName() + " search field overlaps count label at " + w + "x" + h);

        for (JComboBox<?> combo : findAll(panel, JComboBox.class)) {
            Rectangle comboRect = SwingUtilities.convertRectangle(combo.getParent(), combo.getBounds(), row);
            TestAssert.assertTrue(comboRect.x + comboRect.width <= countRect.x,
                panel.getClass().getSimpleName() + " filter combo overlaps count label at " + w + "x" + h);
        }

        JButton addBtn = findButtonByPrefix(panel, "+ Tambah");
        JButton refreshBtn = findButtonByText(panel, "Refresh");
        TestAssert.assertNotNull(addBtn, "+ Tambah button must exist in " + panel.getClass().getSimpleName());
        TestAssert.assertNotNull(refreshBtn, "Refresh button must exist in " + panel.getClass().getSimpleName());
        TestAssert.assertTrue(addBtn.getWidth() > 0 && addBtn.getHeight() > 0,
            "+ Tambah button must have positive laid-out bounds at " + w + "x" + h);
        Rectangle addRect = SwingUtilities.convertRectangle(addBtn.getParent(), addBtn.getBounds(), refreshBtn.getParent());
        TestAssert.assertTrue(addRect.x + addRect.width <= refreshBtn.getX() || addRect.y + addRect.height <= refreshBtn.getY(),
            panel.getClass().getSimpleName() + " + Tambah overlaps Refresh at " + w + "x" + h);
    }

    private static void assertDashboardLayoutHealthy(
            AdminDashboardPanel dash,
            int w,
            int h,
            boolean expectErrorBanner) {
        String context = "AdminDashboardPanel@" + w + "x" + h + (expectErrorBanner ? "[errorBanner]" : "");
        assertScrollableTracksViewportWidth(dash, context);

        if (expectErrorBanner) {
            JLabel errorLbl = findLabelContaining(dash, "Gagal memuat ringkasan dashboard");
            TestAssert.assertNotNull(errorLbl, context + " inline error banner must exist");
            TestAssert.assertTrue(errorLbl.isVisible() && errorLbl.getHeight() > 0,
                context + " inline error banner must be visible with positive height");
        }

        List<JTable> tables = findAll(dash, JTable.class);
        TestAssert.assertEquals(3, tables.size(), context + " must contain 3 recent-activity tables");
        JTable recentOrdersTable = tables.get(0);
        Container ordersViewport = recentOrdersTable.getParent();
        TestAssert.assertTrue(ordersViewport.getWidth() > 0, context + " table viewport width must be > 0");
        TestAssert.assertTrue(ordersViewport.getHeight() >= 100,
            context + " recentOrdersTable viewport height (" + ordersViewport.getHeight() + "px) must be >= 100px");

        JLabel revSubtitle = findLabelContaining(dash, "Pesanan dibayar");
        TestAssert.assertNotNull(revSubtitle, context + " revenue subtitle label must exist");
        int subtitleTextWidth = revSubtitle.getFontMetrics(revSubtitle.getFont()).stringWidth(revSubtitle.getText());
        TestAssert.assertTrue(revSubtitle.getWidth() >= subtitleTextWidth,
            context + " revenue subtitle (" + subtitleTextWidth + "px) clipped in " + revSubtitle.getWidth() + "px label");

        JButton quickProd = findButtonByText(dash, "+ Produk");
        JButton quickAnim = findButtonByText(dash, "+ Hewan");
        JButton quickSvc = findButtonByText(dash, "+ Layanan");
        TestAssert.assertNotNull(quickProd, context + " + Produk button must exist");
        TestAssert.assertNotNull(quickAnim, context + " + Hewan button must exist");
        TestAssert.assertNotNull(quickSvc, context + " + Layanan button must exist");
        TestAssert.assertTrue(quickProd.getWidth() > 0 && quickAnim.getWidth() > 0 && quickSvc.getWidth() > 0,
            context + " quick action buttons must have positive widths");
        TestAssert.assertTrue(quickProd.getX() + quickProd.getWidth() <= quickAnim.getX(),
            context + " + Produk overlaps + Hewan");
        TestAssert.assertTrue(quickAnim.getX() + quickAnim.getWidth() <= quickSvc.getX(),
            context + " + Hewan overlaps + Layanan");

        Container buttonsRow = quickProd.getParent();
        Container quickCard = buttonsRow.getParent().getParent();
        Rectangle svcInCard = SwingUtilities.convertRectangle(buttonsRow, quickSvc.getBounds(), quickCard);
        TestAssert.assertTrue(svcInCard.x + svcInCard.width <= quickCard.getWidth(),
            context + " + Layanan right edge (" + (svcInCard.x + svcInCard.width)
                + ") exceeds quickActionCard width (" + quickCard.getWidth() + ")");
    }

    private static JScrollPane findWidthTrackingScrollPane(Container root) {
        for (JScrollPane sp : findAll(root, JScrollPane.class)) {
            Component view = sp.getViewport().getView();
            if (view instanceof Scrollable s && s.getScrollableTracksViewportWidth()) {
                return sp;
            }
        }
        return null;
    }

    private static void assertScrollableTracksViewportWidth(Container root, String dialogName) {
        TestAssert.assertNotNull(findWidthTrackingScrollPane(root),
            dialogName + " must wrap form content in a Scrollable with getScrollableTracksViewportWidth() == true");
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
        admin.setBio("Administrator Sistem PawPaw Pet Care");
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

    private static JComboBox<?> findComboWithItem(List<JComboBox<?>> combos, String exactItem) {
        for (JComboBox<?> combo : combos) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                Object item = combo.getItemAt(i);
                if (item != null && exactItem.equals(item.toString())) {
                    return combo;
                }
            }
        }
        return null;
    }

    private static void selectComboItemExact(JComboBox<?> combo, String exactItem) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            if (item != null && exactItem.equals(item.toString())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        throw new AssertionError("Combo item '" + exactItem + "' not found");
    }

    private static JLabel findLabelContaining(Container root, String needle) {
        for (JLabel lbl : findAll(root, JLabel.class)) {
            if (lbl.getText() != null && lbl.getText().contains(needle)) {
                return lbl;
            }
        }
        return null;
    }

    private static JLabel findLabelByText(Container root, String text) {
        for (JLabel lbl : findAll(root, JLabel.class)) {
            if (text.equals(lbl.getText())) {
                return lbl;
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

    private static JButton findButtonByPrefix(Container root, String prefix) {
        for (JButton btn : findAll(root, JButton.class)) {
            if (btn.getText() != null && btn.getText().startsWith(prefix)) {
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
