package pawpaw;

import pawpaw.config.DatabaseConfig;
import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.gui.common.ResponsiveCardGrid;
import pawpaw.gui.common.UiKit;
import pawpaw.gui.user.AdoptionFormDialog;
import pawpaw.gui.user.AdoptionWorkspacePanel;
import pawpaw.gui.user.ProfileWorkspacePanel;
import pawpaw.gui.user.UserDashboardPanel;
import pawpaw.model.Animal;
import pawpaw.model.User;
import pawpaw.util.Theme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * EDT smoke tests for UserDashboardPanel, AdoptionWorkspacePanel, AdoptionFormDialog,
 * ProfileWorkspacePanel, and their AppWindow route registrations.
 */
public class UserCorePanelsSmokeTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "UserCorePanelsSmokeTest",
            TestAssert.test("User core panels instantiate, refresh against live pet_care DB, and filter/gate cleanly on EDT",
                UserCorePanelsSmokeTest::testLiveUserPanelsOnEdt),
            TestAssert.test("User core panels guard against null session and unreachable DB without crashing EDT or bleeding banner margins",
                UserCorePanelsSmokeTest::testNullSessionAndDbFailureGuards),
            TestAssert.test("AdoptionFormDialog prefills session user, validates inline without blocking, and guards null input",
                UserCorePanelsSmokeTest::testAdoptionFormDialogPrefillAndValidation),
            TestAssert.test("AppWindow registers user.dashboard, user.adoption, user.profile and pastel menu shortcuts navigate cleanly",
                UserCorePanelsSmokeTest::testAppWindowRegistrationAndSessionHeader),
            TestAssert.test("User core panels reflow cleanly at 1040x680 and 1440x1080 with compact DynamicStatusPill",
                UserCorePanelsSmokeTest::testResponsiveLayoutsAndInspectorPill)
        );
    }

    private static void testLiveUserPanelsOnEdt() throws Exception {
        runOnEdt(() -> {
            AppWindow window = new AppWindow();
            try {
                User user = createSeedCustomer();
                window.onLoginSuccess(user);

                UserDashboardPanel dashboard = new UserDashboardPanel(window);
                AdoptionWorkspacePanel adoption = new AdoptionWorkspacePanel(window);
                ProfileWorkspacePanel profile = new ProfileWorkspacePanel(window);

                TestAssert.assertEquals("user.dashboard", dashboard.getPageKey(), "Dashboard pageKey must match");
                TestAssert.assertEquals("user.adoption", adoption.getPageKey(), "Adoption pageKey must match");
                TestAssert.assertEquals("user.profile", profile.getPageKey(), "Profile pageKey must match");

                dashboard.refreshData();
                adoption.refreshData();
                profile.refreshData();

                assertNonBlank(dashboard.getStatusSummary(), "UserDashboardPanel statusSummary must be non-empty");
                assertNonBlank(adoption.getStatusSummary(), "AdoptionWorkspacePanel statusSummary must be non-empty");
                assertNonBlank(profile.getStatusSummary(), "ProfileWorkspacePanel statusSummary must be non-empty");

                // Dashboard assertions: greeting, 4 pastel menu cards, 4 KPI cards, 1 dense activity table, zero emojis
                TestAssert.assertNotNull(findButtonByText(dashboard, "Adopsi"), "Dashboard must have Adopsi PastelMenuCard");
                TestAssert.assertNotNull(findButtonByText(dashboard, "Toko"), "Dashboard must have Toko PastelMenuCard");
                TestAssert.assertNotNull(findButtonByText(dashboard, "Cek Kesehatan"),
                    "Dashboard must have Cek Kesehatan PastelMenuCard");
                TestAssert.assertNotNull(findButtonByText(dashboard, "Jadwalkan Vaksin"),
                    "Dashboard must have Jadwalkan Vaksin PastelMenuCard");
                TestAssert.assertNotNull(findLabelContaining(dashboard, "Halo, Jesika Putri"),
                    "Dashboard greeting must display Halo, Jesika Putri");
                TestAssert.assertEquals(4, findAll(dashboard, UiKit.KpiCard.class).size(),
                    "UserDashboardPanel must contain 4 KPI cards");

                List<JTable> dashTables = findAll(dashboard, JTable.class);
                TestAssert.assertEquals(1, dashTables.size(), "UserDashboardPanel must have 1 recent activity JTable");
                TestAssert.assertEquals(34, dashTables.get(0).getRowHeight(), "Dashboard table rowHeight must be 34");
                TestAssert.assertTrue(dashTables.get(0).getRowCount() >= 1,
                    "Seed userId=1 must have at least 1 recent activity row");
                assertNoEmojiGlyphs(dashboard);

                // Adoption assertions: Tab 1 ResponsiveCardGrid + 5 icon species pills (no catalog JTable/JComboBox),
                // Tab 2 styled history JTable >= 1, filters & button gating
                JTabbedPane adoptionTabs = findFirst(adoption, JTabbedPane.class);
                TestAssert.assertNotNull(adoptionTabs, "AdoptionWorkspacePanel must contain JTabbedPane");
                TestAssert.assertEquals(2, adoptionTabs.getTabCount(), "AdoptionWorkspacePanel must have 2 tabs");
                TestAssert.assertEquals("Katalog Hewan", adoptionTabs.getTitleAt(0), "First tab must be Katalog Hewan");
                TestAssert.assertEquals("Riwayat Pengajuan", adoptionTabs.getTitleAt(1), "Second tab must be Riwayat Pengajuan");

                Container catalogTab = (Container) adoptionTabs.getComponentAt(0);
                TestAssert.assertNotNull(findFirst(catalogTab, ResponsiveCardGrid.class),
                    "Katalog Hewan tab must use ResponsiveCardGrid");
                TestAssert.assertTrue(findAll(catalogTab, JTable.class).isEmpty(),
                    "Katalog Hewan tab must NOT use a catalog JTable");
                TestAssert.assertTrue(findAll(catalogTab, JComboBox.class).isEmpty(),
                    "Katalog Hewan tab must NOT use a JComboBox species dropdown");

                List<JTable> adoptionTables = findAll(adoption, JTable.class);
                TestAssert.assertEquals(1, adoptionTables.size(),
                    "AdoptionWorkspacePanel must retain only the Riwayat Pengajuan JTable");
                JTable historyTable = adoptionTables.get(0);
                TestAssert.assertEquals(34, historyTable.getRowHeight(), "History table rowHeight must be 34");
                TestAssert.assertTrue(historyTable.getRowCount() >= 1,
                    "Seed userId=1 must have at least 1 adoption history row");

                int totalAnimals = adoption.getDisplayedAnimalCardCount();
                TestAssert.assertTrue(totalAnimals >= 10, "Seed catalog must display at least 10 animal cards");

                List<UiKit.PillFilterButton> speciesPills = findAll(catalogTab, UiKit.PillFilterButton.class);
                TestAssert.assertEquals(5, speciesPills.size(),
                    "Adoption catalog must provide 5 species PillFilterButtons");
                UiKit.PillFilterButton semuaPill = findPillByText(catalogTab, "Semua");
                UiKit.PillFilterButton kucingPill = findPillByText(catalogTab, "Kucing");
                UiKit.PillFilterButton anjingPill = findPillByText(catalogTab, "Anjing");
                UiKit.PillFilterButton kelinciPill = findPillByText(catalogTab, "Kelinci");
                UiKit.PillFilterButton lainnyaPill = findPillByText(catalogTab, "Lainnya");
                TestAssert.assertNotNull(semuaPill, "Semua pill must exist");
                TestAssert.assertNotNull(kucingPill, "Kucing pill must exist");
                TestAssert.assertNotNull(anjingPill, "Anjing pill must exist");
                TestAssert.assertNotNull(kelinciPill, "Kelinci pill must exist");
                TestAssert.assertNotNull(lainnyaPill, "Lainnya pill must exist");
                for (UiKit.PillFilterButton pill : speciesPills) {
                    TestAssert.assertNotNull(pill.getIcon(), "Species pill '" + pill.getText() + "' must have an icon");
                }
                TestAssert.assertTrue(semuaPill.isSelectedPill(), "Semua species pill must be selected initially");

                JButton adoptBtn = findButtonByText(adoption, "Ajukan Adopsi");
                JButton cancelBtn = findButtonByText(adoption, "Batalkan Pengajuan");
                TestAssert.assertNotNull(adoptBtn, "Ajukan Adopsi button must exist");
                TestAssert.assertNotNull(cancelBtn, "Batalkan Pengajuan button must exist");
                TestAssert.assertTrue(adoptBtn.isEnabled(),
                    "Ajukan Adopsi must be enabled when an available seed animal is auto-selected");

                // Click 2nd AnimalCard inside ResponsiveCardGrid and verify inspector updates
                List<Animal> liveAnimals = window.getAdoptionService().listAnimals();
                ResponsiveCardGrid catalogGrid = findFirst(catalogTab, ResponsiveCardGrid.class);
                TestAssert.assertNotNull(catalogGrid, "Catalog ResponsiveCardGrid must exist");
                TestAssert.assertTrue(catalogGrid.getComponentCount() >= 2 && liveAnimals.size() >= 2,
                    "Catalog grid must hold at least 2 animal cards");
                Animal secondAnimal = liveAnimals.get(1);
                Component secondCard = catalogGrid.getComponent(1);
                secondCard.dispatchEvent(new java.awt.event.MouseEvent(
                    secondCard,
                    java.awt.event.MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    java.awt.event.InputEvent.BUTTON1_DOWN_MASK,
                    10,
                    10,
                    1,
                    false,
                    java.awt.event.MouseEvent.BUTTON1
                ));
                TestAssert.assertNotNull(findLabelByText(adoption, "#" + secondAnimal.getAnimalId()),
                    "Clicking 2nd animal card must update inspector ID label to #" + secondAnimal.getAnimalId());
                TestAssert.assertNotNull(findLabelByText(adoption, secondAnimal.getNama()),
                    "Clicking 2nd animal card must update inspector name label to " + secondAnimal.getNama());

                historyTable.setRowSelectionInterval(0, 0);
                TestAssert.assertTrue(cancelBtn.isEnabled(),
                    "Batalkan Pengajuan must be enabled when a pending seed request is selected");

                kucingPill.doClick();
                int catCount = adoption.getDisplayedAnimalCardCount();
                TestAssert.assertTrue(catCount > 0 && catCount < totalAnimals,
                    "Filtering by Kucing pill must narrow catalog cards (" + catCount + " of " + totalAnimals + ")");
                TestAssert.assertTrue(kucingPill.isSelectedPill(), "Kucing pill must become selected on click");
                TestAssert.assertFalse(semuaPill.isSelectedPill(), "Semua pill must deselect when Kucing is clicked");

                semuaPill.doClick();
                TestAssert.assertEquals(totalAnimals, adoption.getDisplayedAnimalCardCount(),
                    "Clicking Semua pill must restore all animal cards");
                TestAssert.assertTrue(semuaPill.isSelectedPill(), "Semua pill must become selected on click");

                JTextField searchField = findFirst(catalogTab, JTextField.class);
                TestAssert.assertNotNull(searchField, "Catalog search field must exist");
                searchField.setText("Anggora");
                int anggoraCount = adoption.getDisplayedAnimalCardCount();
                TestAssert.assertTrue(anggoraCount >= 1 && anggoraCount < totalAnimals,
                    "Search query Anggora must filter catalog cards (" + anggoraCount + " of " + totalAnimals + ")");

                searchField.setText("zzzz_tidak_ada");
                TestAssert.assertEquals(0, adoption.getDisplayedAnimalCardCount(),
                    "Unmatched search query must yield 0 displayed cards cleanly");
                TestAssert.assertFalse(adoptBtn.isEnabled(),
                    "Ajukan Adopsi must disable when catalog filter matches 0 animals");

                searchField.setText("");
                TestAssert.assertEquals(totalAnimals, adoption.getDisplayedAnimalCardCount(),
                    "Clearing search query must restore all catalog cards");
                assertNoEmojiGlyphs(adoption);

                // Profile assertions: hero header with circular avatar, name, role badge, stat chips, 3 tabs, pets table >= 1
                JTabbedPane profileTabs = findFirst(profile, JTabbedPane.class);
                TestAssert.assertNotNull(profileTabs, "ProfileWorkspacePanel must contain JTabbedPane");
                TestAssert.assertEquals(3, profileTabs.getTabCount(), "ProfileWorkspacePanel must have 3 tabs");
                TestAssert.assertEquals("Data Profil", profileTabs.getTitleAt(0), "First profile tab must be Data Profil");
                TestAssert.assertEquals("Hewan Peliharaan Saya", profileTabs.getTitleAt(1),
                    "Second profile tab must be Hewan Peliharaan Saya");
                TestAssert.assertEquals("Keamanan / Sandi", profileTabs.getTitleAt(2),
                    "Third profile tab must be Keamanan / Sandi");

                List<PhotoPanel> profilePhotos = findAll(profile, PhotoPanel.class);
                TestAssert.assertTrue(profilePhotos.stream().anyMatch(PhotoPanel::isCircular),
                    "ProfileWorkspacePanel hero header must include a circular PhotoPanel avatar");
                TestAssert.assertNotNull(findLabelByText(profile, "Jesika Putri"),
                    "Profile hero header must display user name Jesika Putri");
                TestAssert.assertNotNull(findLabelContaining(profile, "@jesika_putri"),
                    "Profile hero header must display @username handle");
                TestAssert.assertNotNull(findLabelByText(profile, "Pelanggan"),
                    "Profile hero header must display Pelanggan role badge");
                JLabel hewanChipTitle = findLabelByText(profile, "Hewan");
                JLabel pesananChipTitle = findLabelByText(profile, "Pesanan");
                JLabel activityChipTitle = findLabelByText(profile, "Adopsi & Booking");
                TestAssert.assertNotNull(hewanChipTitle, "Profile hero header must display Hewan stat chip");
                TestAssert.assertNotNull(pesananChipTitle, "Profile hero header must display Pesanan stat chip");
                TestAssert.assertNotNull(activityChipTitle, "Profile hero header must display Adopsi & Booking stat chip");

                var expectedSummary = window.getProfileService().loadSummary(user.getUserId());
                TestAssert.assertNotNull(
                    findLabelByText(hewanChipTitle.getParent(), String.valueOf(expectedSummary.pets().size())),
                    "Hewan stat chip value must match live pets count"
                );
                TestAssert.assertNotNull(
                    findLabelByText(pesananChipTitle.getParent(), String.valueOf(expectedSummary.ordersCount())),
                    "Pesanan stat chip value must match live orders count"
                );
                TestAssert.assertNotNull(
                    findLabelByText(
                        activityChipTitle.getParent(),
                        String.valueOf(expectedSummary.adoptionsCount() + expectedSummary.bookingsCount())
                    ),
                    "Adopsi & Booking stat chip value must match combined live adoption + booking count"
                );

                List<JTable> profileTables = findAll(profile, JTable.class);
                TestAssert.assertEquals(1, profileTables.size(), "ProfileWorkspacePanel must have 1 pets JTable");
                TestAssert.assertEquals(34, profileTables.get(0).getRowHeight(), "Pets table rowHeight must be 34");
                TestAssert.assertTrue(profileTables.get(0).getRowCount() >= 1,
                    "Seed userId=1 must have at least 1 pet row");

                List<JTextField> profileFields = findAll((Container) profileTabs.getComponentAt(0), JTextField.class);
                boolean foundJesika = profileFields.stream().anyMatch(f -> "Jesika Putri".equals(f.getText()));
                TestAssert.assertTrue(foundJesika, "Profile tab must prefill user name Jesika Putri");

                // Security tab inline validation check
                profileTabs.setSelectedIndex(2);
                JButton changePwBtn = findButtonByText((Container) profileTabs.getComponentAt(2), "Ubah Kata Sandi");
                TestAssert.assertNotNull(changePwBtn, "Ubah Kata Sandi button must exist");
                changePwBtn.doClick();
                boolean hasVisibleErrorLabel = findAll((Container) profileTabs.getComponentAt(2), JLabel.class).stream()
                    .anyMatch(l -> l.isVisible() && l.getText() != null && !l.getText().isBlank());
                TestAssert.assertTrue(hasVisibleErrorLabel,
                    "Submitting empty change-password form must display inline error banner");
                assertNoEmojiGlyphs(profile);
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
                    UserDashboardPanel dashboard = new UserDashboardPanel(window);
                    AdoptionWorkspacePanel adoption = new AdoptionWorkspacePanel(window);
                    ProfileWorkspacePanel profile = new ProfileWorkspacePanel(window);

                    dashboard.refreshData();
                    adoption.refreshData();
                    profile.refreshData();

                    assertNonBlank(dashboard.getStatusSummary(), "Null session dashboard statusSummary must be non-empty");
                    assertNonBlank(adoption.getStatusSummary(), "Null session adoption statusSummary must be non-empty");
                    assertNonBlank(profile.getStatusSummary(), "Null session profile statusSummary must be non-empty");
                    TestAssert.assertTrue(adoption.getDisplayedAnimalCardCount() >= 10,
                        "Null session adoption catalog still loads public animals cleanly");
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
                    UserDashboardPanel dashboard = new UserDashboardPanel(window);
                    AdoptionWorkspacePanel adoption = new AdoptionWorkspacePanel(window);
                    ProfileWorkspacePanel profile = new ProfileWorkspacePanel(window);

                    dashboard.refreshData();
                    adoption.refreshData();
                    profile.refreshData();

                    assertNonBlank(dashboard.getStatusSummary(), "DB-error dashboard statusSummary must be non-empty");
                    assertNonBlank(adoption.getStatusSummary(), "DB-error adoption statusSummary must be non-empty");
                    assertNonBlank(profile.getStatusSummary(), "DB-error profile statusSummary must be non-empty");
                    TestAssert.assertEquals(0, adoption.getDisplayedAnimalCardCount(),
                        "DB-error adoption panel must reset catalog cards to 0");

                    // Verify UserDashboardPanel error banner has zero opaque outer margin bleed
                    javax.swing.JFrame hostFrame = new javax.swing.JFrame("Dashboard Error Test");
                    try {
                        hostFrame.addNotify();
                        hostFrame.getContentPane().setLayout(new java.awt.BorderLayout());
                        hostFrame.getContentPane().add(dashboard, java.awt.BorderLayout.CENTER);
                        hostFrame.setSize(900, 650);
                        hostFrame.validate();

                        JLabel errBanner = findLabelContaining(dashboard, "Gagal memuat ringkasan beranda");
                        TestAssert.assertNotNull(errBanner, "Dashboard DB-error banner must be present");
                        TestAssert.assertTrue(errBanner.isVisible(), "Dashboard DB-error banner must be visible");

                        Container parent = errBanner.getParent();
                        TestAssert.assertNotNull(parent, "Error banner must be hosted inside a wrapper container");
                        BufferedImage dashImg = paintToImage(dashboard, dashboard.getWidth(), dashboard.getHeight());
                        Point bannerOrigin = SwingUtilities.convertPoint(
                            errBanner.getParent(),
                            errBanner.getLocation(),
                            dashboard
                        );
                        int sampleY = bannerOrigin.y + errBanner.getHeight() + 4;
                        int sampleX = bannerOrigin.x + 20;
                        TestAssert.assertTrue(sampleY < dashboard.getHeight() && sampleX < dashboard.getWidth(),
                            "Sample point below error banner must be within dashboard bounds");
                        TestAssert.assertEquals(Theme.BG.getRGB(), dashImg.getRGB(sampleX, sampleY),
                            "Space directly below error banner must be Theme.BG without opaque 12px margin bleed");
                    } finally {
                        hostFrame.dispose();
                    }
                } finally {
                    window.dispose();
                }
            });
        } finally {
            DatabaseConfig.resetConfig();
        }
    }

    private static void testAdoptionFormDialogPrefillAndValidation() throws Exception {
        runOnEdt(() -> {
            TestAssert.assertFalse(AdoptionFormDialog.show(null, null),
                "AdoptionFormDialog.show(null, null) must return false cleanly");

            AppWindow window = new AppWindow();
            try {
                User user = createSeedCustomer();
                window.getSession().login(user);

                Animal animal = new Animal();
                animal.setAnimalId(1);
                animal.setNama("Siti");
                animal.setSpecies("kucing");
                animal.setBreed("Anggora");
                animal.setGender("Betina");
                animal.setAge("1 Tahun");
                animal.setStatus("available");

                AdoptionFormDialog dialog = new AdoptionFormDialog(window, animal);
                try {
                    TestAssert.assertEquals(Theme.BG, dialog.getContentPane().getBackground(),
                        "AdoptionFormDialog root must use Theme.BG");
                    List<JTextField> fields = findAll(dialog.getContentPane(), JTextField.class);
                    boolean hasName = fields.stream().anyMatch(f -> "Jesika Putri".equals(f.getText()));
                    boolean hasEmail = fields.stream().anyMatch(f -> "jesika@pawpaw.com".equals(f.getText()));
                    TestAssert.assertTrue(hasName, "AdoptionFormDialog must prefill applicant name");
                    TestAssert.assertTrue(hasEmail, "AdoptionFormDialog must prefill applicant email");

                    JButton submitBtn = findButtonByText(dialog.getContentPane(), "Kirim Pengajuan");
                    TestAssert.assertNotNull(submitBtn, "AdoptionFormDialog must have Kirim Pengajuan button");
                    submitBtn.doClick();

                    TestAssert.assertFalse(dialog.isSubmitted(),
                        "Blank adoption reason must fail inline validation without submitting");
                    assertNonBlank(dialog.getInlineErrorText(),
                        "AdoptionFormDialog must show inline error when reason is empty");
                    assertNoEmojiGlyphs(dialog.getContentPane());
                } finally {
                    dialog.dispose();
                }
            } finally {
                window.dispose();
            }
        });
    }

    private static void testAppWindowRegistrationAndSessionHeader() throws Exception {
        runOnEdt(() -> {
            AppWindow window = new AppWindow();
            try {
                User user = createSeedCustomer();
                window.onLoginSuccess(user);
                window.addNotify();
                window.setSize(1040, 680);
                window.validate();

                TestAssert.assertEquals("user.dashboard", window.getShell().getActivePageKey(),
                    "Customer login must navigate to user.dashboard");

                UserDashboardPanel liveDashboard = findFirst(window.getShell(), UserDashboardPanel.class);
                TestAssert.assertNotNull(liveDashboard, "AppWindow must mount UserDashboardPanel");

                JButton quickAdoptionBtn = findButtonByText(liveDashboard, "Adopsi");
                TestAssert.assertNotNull(quickAdoptionBtn, "Dashboard PastelMenuCard Adopsi must exist");
                quickAdoptionBtn.doClick();
                window.validate();
                TestAssert.assertEquals("user.adoption", window.getShell().getActivePageKey(),
                    "Clicking Adopsi PastelMenuCard must route to user.adoption");
                assertNonBlank(window.getShell().getStatusSummaryText(),
                    "Shell status summary must be populated on user.adoption");

                window.navigate("user.dashboard");
                window.validate();
                JButton quickShopBtn = findButtonByText(liveDashboard, "Toko");
                TestAssert.assertNotNull(quickShopBtn, "Dashboard PastelMenuCard Toko must exist");
                quickShopBtn.doClick();
                window.validate();
                TestAssert.assertEquals("user.shop", window.getShell().getActivePageKey(),
                    "Clicking Toko PastelMenuCard must route to user.shop");

                window.navigate("user.dashboard");
                window.validate();
                JButton quickHealthBtn = findButtonByText(liveDashboard, "Cek Kesehatan");
                TestAssert.assertNotNull(quickHealthBtn, "Dashboard PastelMenuCard Cek Kesehatan must exist");
                quickHealthBtn.doClick();
                window.validate();
                TestAssert.assertEquals("user.health", window.getShell().getActivePageKey(),
                    "Clicking Cek Kesehatan PastelMenuCard must route to user.health");

                window.navigate("user.dashboard");
                window.validate();
                JButton quickVaccineBtn = findButtonByText(liveDashboard, "Jadwalkan Vaksin");
                TestAssert.assertNotNull(quickVaccineBtn, "Dashboard PastelMenuCard Jadwalkan Vaksin must exist");
                quickVaccineBtn.doClick();
                window.validate();
                TestAssert.assertEquals("user.health", window.getShell().getActivePageKey(),
                    "Clicking Jadwalkan Vaksin PastelMenuCard must route to user.health");

                window.navigate("user.profile");
                window.validate();
                TestAssert.assertEquals("user.profile", window.getShell().getActivePageKey(),
                    "Navigating to user.profile must activate ProfileWorkspacePanel");
                assertNonBlank(window.getShell().getStatusSummaryText(),
                    "Shell status summary must be populated on user.profile");

                User updated = createSeedCustomer();
                updated.setName("Jesika Diperbarui");
                window.getSession().setUser(updated);
                window.refreshSessionHeader();

                JLabel updatedHeaderLabel = findLabelByText(window.getShell(), "Jesika Diperbarui");
                TestAssert.assertNotNull(updatedHeaderLabel,
                    "refreshSessionHeader must update sidebar user name label");
            } finally {
                window.dispose();
            }
        });
    }

    private static void testResponsiveLayoutsAndInspectorPill() throws Exception {
        final AppWindow[] windowHolder = new AppWindow[1];
        try {
            runOnEdt(() -> {
                AppWindow window = new AppWindow();
                windowHolder[0] = window;
                window.onLoginSuccess(createSeedCustomer());
                window.addNotify();

                // 1. UserDashboardPanel at 1040x680 and 1440x1080
                window.navigate("user.dashboard");
                window.setSize(1040, 680);
                window.validate();

                UserDashboardPanel dashboard = findFirst(window.getShell(), UserDashboardPanel.class);
                TestAssert.assertNotNull(dashboard, "AppWindow must mount UserDashboardPanel");
                JScrollPane dashScroll = findFirst(dashboard, JScrollPane.class);
                TestAssert.assertNotNull(dashScroll, "UserDashboardPanel must use outer JScrollPane");
                Scrollable dashHost = (Scrollable) dashScroll.getViewport().getView();
                TestAssert.assertTrue(dashHost.getScrollableTracksViewportWidth(),
                    "Dashboard scroll host must track viewport width");
                TestAssert.assertFalse(dashHost.getScrollableTracksViewportHeight(),
                    "Dashboard scroll host must not track viewport height");

                List<JButton> menuCards = List.of(
                    findButtonByText(dashboard, "Adopsi"),
                    findButtonByText(dashboard, "Toko"),
                    findButtonByText(dashboard, "Cek Kesehatan"),
                    findButtonByText(dashboard, "Jadwalkan Vaksin")
                );
                assertComponentsNonOverlapping(dashboard, menuCards, "Dashboard PastelMenuCards@1040x680");

                window.setSize(1440, 1080);
                window.validate();
                JViewport dashVp = dashScroll.getViewport();
                TestAssert.assertTrue(((Component) dashHost).getHeight() >= dashVp.getHeight(),
                    "Dashboard ExpandingViewportLayout must expand view on 1440x1080 ("
                        + ((Component) dashHost).getHeight() + " vs vp " + dashVp.getHeight() + ")");
                TestAssert.assertTrue(hasNonTransparentPixel(paintToImage(dashboard, dashboard.getWidth(), dashboard.getHeight())),
                    "UserDashboardPanel must paint cleanly at 1440x1080");

                // 2. AdoptionWorkspacePanel at 1040x680 and 1440x1080
                window.navigate("user.adoption");
                window.setSize(1040, 680);
                window.validate();
            });

            // Flush any queued ResponsiveCardGrid invokeLater revalidations after first layout
            runOnEdt(() -> {
                AppWindow window = windowHolder[0];
                window.validate();
                AdoptionWorkspacePanel adoptionPanel = findFirst(window.getShell(), AdoptionWorkspacePanel.class);
                TestAssert.assertNotNull(adoptionPanel, "AppWindow must mount AdoptionWorkspacePanel");

                int cols1040 = adoptionPanel.getCatalogColumnCount();
                TestAssert.assertTrue(cols1040 >= 2,
                    "Adoption catalog grid at 1040x680 must show >= 2 columns (was " + cols1040 + ")");

                JTextField searchField = findFirst(adoptionPanel, JTextField.class);
                JLabel countLabel = findLabelContaining(adoptionPanel, "Menampilkan ");
                List<UiKit.PillFilterButton> pills = findAll(adoptionPanel, UiKit.PillFilterButton.class);
                List<Component> toolbarItems = new ArrayList<>();
                toolbarItems.add(searchField);
                toolbarItems.add(countLabel);
                toolbarItems.addAll(pills);
                assertComponentsNonOverlapping(adoptionPanel, toolbarItems, "Adoption toolbar@1040x680");

                // Switch to 1440x1080
                window.setSize(1440, 1080);
                window.validate();
            });

            runOnEdt(() -> {
                AppWindow window = windowHolder[0];
                window.validate();
                AdoptionWorkspacePanel adoptionPanel = findFirst(window.getShell(), AdoptionWorkspacePanel.class);
                int cols1440 = adoptionPanel.getCatalogColumnCount();
                TestAssert.assertTrue(cols1440 >= 3,
                    "Adoption catalog grid at 1440x1080 must show >= 3 columns (was " + cols1440 + ")");

                // Verify ALL DynamicStatusPills (including inspector pill) stay compact <= 32px at 1440x1080
                List<JLabel> statusPills = findAllLabelsByText(adoptionPanel, "Tersedia");
                TestAssert.assertTrue(!statusPills.isEmpty(), "Must find DynamicStatusPill showing Tersedia");
                for (JLabel statusPill : statusPills) {
                    int actualHeight = statusPill.getHeight();
                    int prefHeight = statusPill.getPreferredSize().height;
                    TestAssert.assertTrue(actualHeight > 0,
                        "DynamicStatusPill must have positive height on 1440x1080");
                    TestAssert.assertTrue(actualHeight <= 32,
                        "DynamicStatusPill height (" + actualHeight + "px) must stay compact (<= 32px) on 1440x1080");
                    TestAssert.assertEquals(prefHeight, actualHeight,
                        "DynamicStatusPill height must match compact preferred height");
                }

                // 3. ProfileWorkspacePanel at 1040x680 and 1440x1080
                window.navigate("user.profile");
                window.setSize(1040, 680);
                window.validate();

                ProfileWorkspacePanel profilePanel = findFirst(window.getShell(), ProfileWorkspacePanel.class);
                TestAssert.assertNotNull(profilePanel, "AppWindow must mount ProfileWorkspacePanel");
                List<Component> heroItems = collectProfileHeroItems(profilePanel);
                assertComponentsNonOverlapping(profilePanel, heroItems, "Profile hero@1040x680");

                window.setSize(1440, 1080);
                window.validate();
                assertComponentsNonOverlapping(profilePanel, heroItems, "Profile hero@1440x1080");
                TestAssert.assertTrue(hasNonTransparentPixel(paintToImage(profilePanel, profilePanel.getWidth(), profilePanel.getHeight())),
                    "ProfileWorkspacePanel must paint cleanly at 1440x1080");
            });
        } finally {
            if (windowHolder[0] != null) {
                runOnEdt(() -> windowHolder[0].dispose());
            }
        }
    }

    private static List<Component> collectProfileHeroItems(ProfileWorkspacePanel profilePanel) {
        List<Component> items = new ArrayList<>();
        PhotoPanel avatar = findAll(profilePanel, PhotoPanel.class).stream()
            .filter(PhotoPanel::isCircular)
            .findFirst()
            .orElse(null);
        TestAssert.assertNotNull(avatar, "Profile hero circular avatar must exist");
        items.add(avatar);
        items.add(findLabelByText(profilePanel, "Jesika Putri"));
        items.add(findLabelContaining(profilePanel, "@jesika_putri"));
        items.add(findLabelByText(profilePanel, "Pelanggan"));
        items.add(findLabelByText(profilePanel, "Hewan"));
        items.add(findLabelByText(profilePanel, "Pesanan"));
        items.add(findLabelByText(profilePanel, "Adopsi & Booking"));
        return items;
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

    private static UiKit.PillFilterButton findPillByText(Container root, String text) {
        for (UiKit.PillFilterButton pill : findAll(root, UiKit.PillFilterButton.class)) {
            if (text.equals(pill.getText())) {
                return pill;
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

    private static List<JLabel> findAllLabelsByText(Container root, String text) {
        List<JLabel> matches = new ArrayList<>();
        for (JLabel lbl : findAll(root, JLabel.class)) {
            if (text.equals(lbl.getText())) {
                matches.add(lbl);
            }
        }
        return matches;
    }

    private static JLabel findLabelContaining(Container root, String fragment) {
        for (JLabel lbl : findAll(root, JLabel.class)) {
            if (lbl.getText() != null && lbl.getText().contains(fragment)) {
                return lbl;
            }
        }
        return null;
    }

    private static BufferedImage paintToImage(JComponent component, int width, int height) {
        BufferedImage img = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        component.paint(g);
        g.dispose();
        return img;
    }

    private static boolean hasNonTransparentPixel(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void assertNoEmojiGlyphs(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel lbl) {
                assertCleanText(lbl.getText(), "JLabel");
            } else if (child instanceof JButton btn) {
                assertCleanText(btn.getText(), "JButton");
            }
            if (child instanceof Container nested) {
                assertNoEmojiGlyphs(nested);
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
