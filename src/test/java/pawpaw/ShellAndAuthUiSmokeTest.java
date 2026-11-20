package pawpaw;

import pawpaw.config.DatabaseConfig;
import pawpaw.gui.auth.AuthPanel;
import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.DesktopShell;
import pawpaw.gui.common.IdPillRenderer;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.gui.common.ResponsiveCardGrid;
import pawpaw.gui.common.StatusBadgeRenderer;
import pawpaw.gui.common.UiKit;
import pawpaw.model.User;
import pawpaw.util.FormatUtil;
import pawpaw.util.ImageStore;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * EDT smoke tests for Theme, VectorIcons, common UI kit, PhotoPanel, DesktopShell, AuthPanel, and AppWindow.
 */
public class ShellAndAuthUiSmokeTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "ShellAndAuthUiSmokeTest",
            TestAssert.test("Theme defines dual-palette tokens and bundled Poppins font factories",
                ShellAndAuthUiSmokeTest::testThemeTokens),
            TestAssert.test("All VectorIcons.Kind entries render cleanly to 24x24 BufferedImage",
                ShellAndAuthUiSmokeTest::testAllVectorIconsRender),
            TestAssert.test("PhotoPanel paints valid, missing, and circular ring images without throwing",
                ShellAndAuthUiSmokeTest::testPhotoPanelRendering),
            TestAssert.test("UiKit dual-palette factories, IdPillRenderer, and StatusBadgeRenderer clip cleanly on EDT",
                ShellAndAuthUiSmokeTest::testUiKitAndStatusBadgeRenderer),
            TestAssert.test("ResponsiveCardGrid and UiKit scroll helpers reflow and expand cleanly on EDT",
                ShellAndAuthUiSmokeTest::testResponsiveCardGridAndScrollHelpers),
            TestAssert.test("ResponsiveCardGrid inside nested UiKit.verticalScrollPane updates scroll height on resize and empty transition",
                ShellAndAuthUiSmokeTest::testResponsiveCardGridInsideVerticalScrollPaneReflowAndEmptyTransition),
            TestAssert.test("PagePanel.defaultBackground and Main.applyUiDefaults support dual-palette Poppins UI",
                ShellAndAuthUiSmokeTest::testPagePanelBackgroundAndUiDefaults),
            TestAssert.test("AuthPanel uses cat-mascot single card, RememberStore, listener cleanup, and DB status",
                ShellAndAuthUiSmokeTest::testAuthPanelDbStatusAndErrorHandling),
            TestAssert.test("DesktopShell supports collapsible dual-palette sidebar, PagePanel navigation, and role switches",
                ShellAndAuthUiSmokeTest::testDesktopShellNavigationAndRoleModes),
            TestAssert.test("AppWindow manages defaultButton lifecycle, switches workspaces cleanly, and disposes",
                ShellAndAuthUiSmokeTest::testAppWindowLifecycle)
        );
    }

    private static void testThemeTokens() {
        TestAssert.assertTrue(Theme.isPoppinsLoaded(), "Bundled Poppins TTF fonts must load cleanly");
        TestAssert.assertEquals("Poppins", Theme.fontFamily(), "Theme.fontFamily() must resolve to Poppins");
        TestAssert.assertTrue(Theme.body().getFontName().toLowerCase(Locale.ROOT).contains("poppins"),
            "Theme.body() must use Poppins");
        TestAssert.assertTrue(Theme.bodyMedium().getFontName().toLowerCase(Locale.ROOT).contains("medium"),
            "Theme.bodyMedium() must derive from Poppins-Medium");
        TestAssert.assertTrue(Theme.bodyBold().getFontName().toLowerCase(Locale.ROOT).contains("semibold"),
            "Theme.bodyBold() must derive from Poppins-SemiBold");
        TestAssert.assertTrue(Theme.title().getFontName().toLowerCase(Locale.ROOT).contains("bold"),
            "Theme.title() must derive from Poppins-Bold");
        TestAssert.assertTrue(Theme.display().getFontName().toLowerCase(Locale.ROOT).contains("bold"),
            "Theme.display() must derive from Poppins-Bold");
        TestAssert.assertTrue(Theme.heading().getFontName().toLowerCase(Locale.ROOT).contains("semibold"),
            "Theme.heading() must derive from Poppins-SemiBold");

        TestAssert.assertEquals(24, Theme.display().getSize(), "Theme.display() size must be 24");
        TestAssert.assertEquals(20, Theme.title().getSize(), "Theme.title() size must be 20");
        TestAssert.assertEquals(15, Theme.heading().getSize(), "Theme.heading() size must be 15");
        TestAssert.assertEquals(13, Theme.body().getSize(), "Theme.body() size must be 13");
        TestAssert.assertEquals(13, Theme.bodyMedium().getSize(), "Theme.bodyMedium() size must be 13");
        TestAssert.assertEquals(13, Theme.bodyBold().getSize(), "Theme.bodyBold() size must be 13");
        TestAssert.assertEquals(12, Theme.small().getSize(), "Theme.small() size must be 12");
        TestAssert.assertEquals(12, Theme.smallBold().getSize(), "Theme.smallBold() size must be 12");
        TestAssert.assertEquals(11, Theme.badge().getSize(), "Theme.badge() size must be 11");
        TestAssert.assertEquals(22, Theme.kpiValue().getSize(), "Theme.kpiValue() size must be 22");

        TestAssert.assertEquals(new Color(0xEE, 0xF6, 0xFD), Theme.BG, "Theme.BG must be #EEF6FD");
        TestAssert.assertEquals(new Color(0xFF, 0xFF, 0xFF), Theme.SURFACE, "Theme.SURFACE must be #FFFFFF");
        TestAssert.assertEquals(new Color(0xF8, 0xFB, 0xFF), Theme.SURFACE_ALT, "Theme.SURFACE_ALT must be #F8FBFF");
        TestAssert.assertEquals(new Color(0xE4, 0xF0, 0xFB), Theme.STATUS_BAR_BG, "Theme.STATUS_BAR_BG must be #E4F0FB");
        TestAssert.assertEquals(new Color(0xD6, 0xE8, 0xFF), Theme.BORDER, "Theme.BORDER must be #D6E8FF");
        TestAssert.assertEquals(new Color(0xD9, 0xE7, 0xF4), Theme.BORDER_SUBTLE, "Theme.BORDER_SUBTLE must be #D9E7F4");
        TestAssert.assertEquals(new Color(0x0F, 0x28, 0x54), Theme.NAVY, "Theme.NAVY must be #0F2854");
        TestAssert.assertEquals(new Color(0x1C, 0x4D, 0x8D), Theme.BLUE, "Theme.BLUE must be #1C4D8D");
        TestAssert.assertEquals(new Color(0x16, 0x3E, 0x73), Theme.BLUE_HOVER, "Theme.BLUE_HOVER must be #163E73");
        TestAssert.assertEquals(new Color(0x49, 0x88, 0xC4), Theme.BANNER_BLUE, "Theme.BANNER_BLUE must be #4988C4");
        TestAssert.assertEquals(new Color(0xFF, 0xC9, 0x3C), Theme.GOLD, "Theme.GOLD must be #FFC93C");
        TestAssert.assertEquals(new Color(0xBD, 0xE8, 0xF5), Theme.CYAN_BTN, "Theme.CYAN_BTN must be #BDE8F5");
        TestAssert.assertEquals(new Color(0xA8, 0xDE, 0xEF), Theme.CYAN_BTN_HOVER, "Theme.CYAN_BTN_HOVER must be #A8DEEF");
        TestAssert.assertEquals(new Color(0x0F, 0x28, 0x54), Theme.INK, "Theme.INK must be #0F2854");
        TestAssert.assertEquals(new Color(0x15, 0x22, 0x32), Theme.INK_BODY, "Theme.INK_BODY must be #152232");
        TestAssert.assertEquals(new Color(0x49, 0x6A, 0x96), Theme.MUTED, "Theme.MUTED must be #496A96");
        TestAssert.assertEquals(new Color(0x4A, 0x55, 0x68), Theme.TEXT_GRAY, "Theme.TEXT_GRAY must be #4A5568");
        TestAssert.assertEquals(new Color(0xDC, 0xEA, 0xFA), Theme.PILL_IDLE_BG, "Theme.PILL_IDLE_BG must be #DCEAFA");
        TestAssert.assertEquals(new Color(0x52, 0x63, 0x9A), Theme.PILL_IDLE_FG, "Theme.PILL_IDLE_FG must be #52639A");
        TestAssert.assertEquals(new Color(0xEE, 0xF7, 0xFF), Theme.AUTH_CREAM, "Theme.AUTH_CREAM must be #EEF7FF");
        TestAssert.assertEquals(new Color(0xD6, 0xE8, 0xFF), Theme.AUTH_PEACH, "Theme.AUTH_PEACH must be #D6E8FF");
        TestAssert.assertEquals(new Color(0xBD, 0xE8, 0xF5), Theme.AUTH_PINK, "Theme.AUTH_PINK must be #BDE8F5");
        TestAssert.assertEquals(new Color(0xF8, 0xFB, 0xFF), Theme.AUTH_INPUT_BG, "Theme.AUTH_INPUT_BG must be #F8FBFF");
        TestAssert.assertEquals(new Color(0xFF, 0xD6, 0xD6), Theme.PASTEL_PINK, "Theme.PASTEL_PINK must be #FFD6D6");
        TestAssert.assertEquals(new Color(0xFF, 0xE2, 0xE2), Theme.PASTEL_LIGHT_PINK, "Theme.PASTEL_LIGHT_PINK must be #FFE2E2");
        TestAssert.assertEquals(new Color(0xD6, 0xE8, 0xFF), Theme.PASTEL_BLUE, "Theme.PASTEL_BLUE must be #D6E8FF");
        TestAssert.assertEquals(new Color(0xD8, 0xF5, 0xD0), Theme.PASTEL_GREEN, "Theme.PASTEL_GREEN must be #D8F5D0");
        TestAssert.assertEquals(new Color(0xE8, 0xD9, 0xFF), Theme.PASTEL_LAVENDER, "Theme.PASTEL_LAVENDER must be #E8D9FF");
        TestAssert.assertEquals(new Color(0xFF, 0xED, 0xD5), Theme.PASTEL_ORANGE, "Theme.PASTEL_ORANGE must be #FFEDD5");
        TestAssert.assertEquals(new Color(0xE6, 0xF6, 0xEF), Theme.SUCCESS_BG, "Theme.SUCCESS_BG must be #E6F6EF");
        TestAssert.assertEquals(new Color(0x14, 0x78, 0x4E), Theme.SUCCESS_FG, "Theme.SUCCESS_FG must be #14784E");
        TestAssert.assertEquals(new Color(0xFF, 0xF3, 0xDC), Theme.WARNING_BG, "Theme.WARNING_BG must be #FFF3DC");
        TestAssert.assertEquals(new Color(0xB2, 0x72, 0x00), Theme.WARNING_FG, "Theme.WARNING_FG must be #B27200");
        TestAssert.assertEquals(new Color(0xED, 0xF4, 0xFF), Theme.INFO_BG, "Theme.INFO_BG must be #EDF4FF");
        TestAssert.assertEquals(new Color(0x24, 0x58, 0xB7), Theme.INFO_FG, "Theme.INFO_FG must be #2458B7");
        TestAssert.assertEquals(new Color(0xFD, 0xE8, 0xE8), Theme.DANGER_BG, "Theme.DANGER_BG must be #FDE8E8");
        TestAssert.assertEquals(new Color(0xC5, 0x3C, 0x38), Theme.DANGER_FG, "Theme.DANGER_FG must be #C53C38");
        TestAssert.assertEquals(12, Theme.RADIUS, "Theme.RADIUS must be 12");
        TestAssert.assertEquals(16, Theme.INPUT_RADIUS, "Theme.INPUT_RADIUS must be 16");
        TestAssert.assertEquals(20, Theme.CARD_RADIUS, "Theme.CARD_RADIUS must be 20");
        TestAssert.assertEquals(28, Theme.AUTH_CARD_RADIUS, "Theme.AUTH_CARD_RADIUS must be 28");
        TestAssert.assertEquals(34, Theme.TABLE_ROW_HEIGHT, "Theme.TABLE_ROW_HEIGHT must be 34");

        // Admin palette tokens
        TestAssert.assertEquals(new Color(0x0F, 0x27, 0x47), Theme.Admin.NAVY, "Theme.Admin.NAVY must be #0F2747");
        TestAssert.assertEquals(new Color(0x19, 0x36, 0x5E), Theme.Admin.NAVY_HOVER, "Theme.Admin.NAVY_HOVER must be #19365E");
        TestAssert.assertEquals(new Color(0xB8, 0xC7, 0xE3), Theme.Admin.NAVY_TEXT, "Theme.Admin.NAVY_TEXT must be #B8C7E3");
        TestAssert.assertEquals(245, Theme.Admin.SIDEBAR_WIDTH, "Theme.Admin.SIDEBAR_WIDTH must be 245");
        TestAssert.assertEquals(84, Theme.Admin.SIDEBAR_COLLAPSED, "Theme.Admin.SIDEBAR_COLLAPSED must be 84");
        TestAssert.assertEquals(new Color(0x2F, 0x6F, 0xED), Theme.Admin.BLUE, "Theme.Admin.BLUE must be #2F6FED");
        TestAssert.assertEquals(new Color(0x1F, 0x56, 0xC4), Theme.Admin.BLUE_DARK, "Theme.Admin.BLUE_DARK must be #1F56C4");
        TestAssert.assertEquals(new Color(0xDC, 0xEB, 0xFF), Theme.Admin.BLUE_SOFT, "Theme.Admin.BLUE_SOFT must be #DCEBFF");
        TestAssert.assertEquals(new Color(0xF2, 0xF7, 0xFF), Theme.Admin.BG, "Theme.Admin.BG must be #F2F7FF");
        TestAssert.assertEquals(new Color(0xE3, 0xEC, 0xFA), Theme.Admin.LINE, "Theme.Admin.LINE must be #E3ECFA");
        TestAssert.assertEquals(new Color(0x14, 0x21, 0x3D), Theme.Admin.TEXT, "Theme.Admin.TEXT must be #14213D");
        TestAssert.assertEquals(new Color(0x6B, 0x7A, 0x99), Theme.Admin.TEXT_SOFT, "Theme.Admin.TEXT_SOFT must be #6B7A99");
        TestAssert.assertEquals(new Color(0xF1, 0xF6, 0xFF), Theme.Admin.TABLE_HEADER_BG, "Theme.Admin.TABLE_HEADER_BG must be #F1F6FF");
        TestAssert.assertEquals(new Color(0x31, 0x49, 0x6B), Theme.Admin.TABLE_HEADER_FG, "Theme.Admin.TABLE_HEADER_FG must be #31496B");
        TestAssert.assertEquals(new Color(0xED, 0xF4, 0xFF), Theme.Admin.ID_PILL_BG, "Theme.Admin.ID_PILL_BG must be #EDF4FF");
        TestAssert.assertEquals(new Color(0x24, 0x58, 0xB7), Theme.Admin.ID_PILL_FG, "Theme.Admin.ID_PILL_FG must be #2458B7");
        TestAssert.assertEquals(new Color(0xE6, 0xF6, 0xEF), Theme.Admin.BTN_EDIT_BG, "Theme.Admin.BTN_EDIT_BG must be #E6F6EF");
        TestAssert.assertEquals(new Color(0x14, 0x78, 0x4E), Theme.Admin.BTN_EDIT_FG, "Theme.Admin.BTN_EDIT_FG must be #14784E");
        TestAssert.assertEquals(new Color(0xFF, 0xF0, 0xEF), Theme.Admin.BTN_DELETE_BG, "Theme.Admin.BTN_DELETE_BG must be #FFF0EF");
        TestAssert.assertEquals(new Color(0xC5, 0x3C, 0x38), Theme.Admin.BTN_DELETE_FG, "Theme.Admin.BTN_DELETE_FG must be #C53C38");
        TestAssert.assertEquals(18, Theme.Admin.CARD_RADIUS, "Theme.Admin.CARD_RADIUS must be 18");
        TestAssert.assertEquals(12, Theme.Admin.BUTTON_RADIUS, "Theme.Admin.BUTTON_RADIUS must be 12");
        TestAssert.assertEquals(42, Theme.Admin.ICON_BOX_SIZE, "Theme.Admin.ICON_BOX_SIZE must be 42");

        // Health card color mapping
        TestAssert.assertEquals(Theme.PASTEL_GREEN, Theme.healthCardColor("card-green"), "card-green mapping");
        TestAssert.assertEquals(Theme.PASTEL_BLUE, Theme.healthCardColor("card-blue"), "card-blue mapping");
        TestAssert.assertEquals(Theme.PASTEL_LAVENDER, Theme.healthCardColor("card-purple"), "card-purple mapping");
        TestAssert.assertEquals(Theme.PASTEL_PINK, Theme.healthCardColor("card-pink"), "card-pink mapping");
        TestAssert.assertEquals(Theme.PASTEL_ORANGE, Theme.healthCardColor("card-orange"), "card-orange mapping");
        TestAssert.assertEquals(Theme.PASTEL_BLUE, Theme.healthCardColor("unknown"), "unknown card color fallback");
        TestAssert.assertEquals(Theme.PASTEL_BLUE, Theme.healthCardColor(null), "null card color fallback");
    }

    private static void testAllVectorIconsRender() throws Exception {
        runOnEdt(() -> {
            VectorIcons.Kind[] kinds = VectorIcons.Kind.values();
            TestAssert.assertEquals(21, kinds.length, "VectorIcons.Kind must contain all 21 required icons");

            for (VectorIcons.Kind kind : kinds) {
                Icon icon = VectorIcons.of(kind, 24, Theme.BLUE);
                TestAssert.assertNotNull(icon, "Icon must not be null for " + kind);
                TestAssert.assertEquals(24, icon.getIconWidth(), "Icon width must be 24 for " + kind);
                TestAssert.assertEquals(24, icon.getIconHeight(), "Icon height must be 24 for " + kind);

                BufferedImage canvas = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = canvas.createGraphics();
                icon.paintIcon(null, g, 0, 0);
                g.dispose();

                TestAssert.assertTrue(hasNonTransparentPixel(canvas),
                    "Vector icon must draw visible pixels onto 24x24 canvas for " + kind);
            }
        });
    }

    private static void testPhotoPanelRendering() throws Exception {
        ImageStore.clearCache();
        runOnEdt(() -> {
            PhotoPanel panel = new PhotoPanel();
            panel.setSize(120, 120);

            panel.setImagePath("assets/img/shop/whiskas.png");
            BufferedImage validOut = paintToImage(panel, 120, 120);
            TestAssert.assertTrue(hasNonTransparentPixel(validOut), "Valid image must paint onto PhotoPanel");

            panel.setImagePath("uploads/does-not-exist.png");
            BufferedImage missingOut = paintToImage(panel, 120, 120);
            TestAssert.assertTrue(hasNonTransparentPixel(missingOut),
                "Missing image must render vector paw placeholder cleanly");

            panel.setImagePath(null);
            BufferedImage nullOut = paintToImage(panel, 120, 120);
            TestAssert.assertTrue(hasNonTransparentPixel(nullOut),
                "Null image path must render vector paw placeholder cleanly");

            PhotoPanel avatar = new PhotoPanel(76, 76, Theme.CARD_RADIUS);
            avatar.setCircular(true);
            avatar.setRingBorder(Theme.PASTEL_PINK, 4);
            avatar.setSize(76, 76);
            avatar.setImagePath("assets/img/shop/whiskas.png");
            BufferedImage circularValid = paintToImage(avatar, 76, 76);
            TestAssert.assertTrue(hasNonTransparentPixel(circularValid),
                "Circular PhotoPanel with ring border must paint image cleanly");
            TestAssert.assertEquals(0, (circularValid.getRGB(0, 0) >>> 24),
                "Circular PhotoPanel outer corner must stay transparent");

            avatar.setImagePath(null);
            BufferedImage circularNull = paintToImage(avatar, 76, 76);
            TestAssert.assertTrue(hasNonTransparentPixel(circularNull),
                "Circular PhotoPanel placeholder must paint cleanly without throwing");
        });
    }

    private static void testUiKitAndStatusBadgeRenderer() throws Exception {
        runOnEdt(() -> {
            JButton primary = UiKit.primaryButton("Simpan");
            JButton secondary = UiKit.secondaryButton("Batal");
            JButton danger = UiKit.dangerButton("Hapus");
            JButton ghost = UiKit.ghostButton("Tutup");
            TestAssert.assertNotNull(primary, "primaryButton must not be null");
            TestAssert.assertNotNull(secondary, "secondaryButton must not be null");
            TestAssert.assertNotNull(danger, "dangerButton must not be null");
            TestAssert.assertNotNull(ghost, "ghostButton must not be null");

            UiKit.PillFilterButton pillBtn = UiKit.pillFilterButton(
                "Kucing",
                VectorIcons.of(VectorIcons.Kind.PAW, 16, Theme.INK)
            );
            TestAssert.assertFalse(pillBtn.isSelectedPill(), "PillFilterButton must start unselected");
            pillBtn.setSelectedPill(true);
            TestAssert.assertTrue(pillBtn.isSelectedPill(), "PillFilterButton must toggle selected state");
            pillBtn.setSize(120, 32);
            TestAssert.assertTrue(hasNonTransparentPixel(paintToImage(pillBtn, 120, 32)),
                "PillFilterButton must paint cleanly");

            JButton circleBtn = UiKit.circleIconButton(
                VectorIcons.Kind.CART,
                Theme.CYAN_BTN,
                Theme.CYAN_BTN_HOVER,
                Theme.INK,
                36
            );
            TestAssert.assertEquals(new Dimension(36, 36), circleBtn.getPreferredSize(),
                "circleIconButton preferred size must match diameter");
            circleBtn.setSize(36, 36);
            TestAssert.assertTrue(hasNonTransparentPixel(paintToImage(circleBtn, 36, 36)),
                "circleIconButton must paint cleanly");

            JButton adminPrimary = UiKit.adminPrimaryButton("Tambah", VectorIcons.Kind.PLUS);
            JButton adminLight = UiKit.adminLightButton("Refresh", VectorIcons.Kind.REFRESH);
            JButton adminEdit = UiKit.adminEditButton("Edit", VectorIcons.Kind.EDIT);
            JButton adminDelete = UiKit.adminDeleteButton("Hapus", VectorIcons.Kind.TRASH);
            TestAssert.assertEquals(Color.WHITE, adminPrimary.getForeground(), "adminPrimaryButton fg must be white");
            TestAssert.assertEquals(Theme.Admin.BTN_LIGHT_FG, adminLight.getForeground(),
                "adminLightButton fg must match Theme.Admin.BTN_LIGHT_FG");
            TestAssert.assertEquals(Theme.Admin.BTN_EDIT_FG, adminEdit.getForeground(),
                "adminEditButton fg must match Theme.Admin.BTN_EDIT_FG");
            TestAssert.assertEquals(Theme.Admin.BTN_DELETE_FG, adminDelete.getForeground(),
                "adminDeleteButton fg must match Theme.Admin.BTN_DELETE_FG");

            JTextField textField = UiKit.textField("Nama");
            JPasswordField passwordField = UiKit.passwordField("Sandi");
            JTextField searchField = UiKit.searchField("Cari...");
            JComboBox<String> combo = UiKit.comboBox(new String[] { "Semua", "Kucing" });
            JPanel card = UiKit.cardPanel();
            JPanel adminCard = UiKit.adminCardPanel();
            JPanel pastelCard = UiKit.pastelCardPanel(
                new BorderLayout(),
                Theme.PASTEL_GREEN,
                Theme.BORDER,
                Theme.CARD_RADIUS
            );
            JPanel kpi = UiKit.kpiCard("Produk", "10", "Total item aktif", Theme.BLUE, VectorIcons.Kind.SHOP);
            JPanel adminKpi = UiKit.adminKpiCard("Pesanan", "12", "Hari ini", VectorIcons.Kind.ORDERS);

            TestAssert.assertNotNull(textField, "textField must not be null");
            TestAssert.assertNotNull(passwordField, "passwordField must not be null");
            TestAssert.assertNotNull(searchField, "searchField must not be null");
            TestAssert.assertNotNull(combo, "comboBox must not be null");
            TestAssert.assertNotNull(card, "cardPanel must not be null");
            TestAssert.assertNotNull(adminCard, "adminCardPanel must not be null");
            TestAssert.assertNotNull(pastelCard, "pastelCardPanel must not be null");
            TestAssert.assertNotNull(kpi, "kpiCard must not be null");
            TestAssert.assertNotNull(adminKpi, "adminKpiCard must not be null");

            List<String> extensions = Arrays.asList(UiKit.supportedImageExtensions());
            TestAssert.assertTrue(extensions.contains("jpg") && extensions.contains("png")
                    && extensions.contains("jpeg") && extensions.contains("jfif"),
                "supportedImageExtensions must include jpg, jpeg, png, jfif");
            TestAssert.assertFalse(extensions.contains("webp"),
                "supportedImageExtensions must NOT include unsupported .webp");

            JComboBox<String> dynamicCombo = UiKit.comboBox();
            Dimension initialComboSize = dynamicCombo.getPreferredSize();
            TestAssert.assertEquals(36, initialComboSize.height, "comboBox initial preferred height must be 36");
            dynamicCombo.addItem("Pemeriksaan Kesehatan Lengkap & Vaksinasi Tahunan Anabul");
            Dimension grownComboSize = dynamicCombo.getPreferredSize();
            TestAssert.assertEquals(36, grownComboSize.height, "comboBox preferred height must stay 36 after addItem");
            TestAssert.assertTrue(grownComboSize.width > initialComboSize.width,
                "UiKit.comboBox preferred width must grow dynamically when long items are added ("
                    + initialComboSize.width + " -> " + grownComboSize.width + ")");

            DefaultTableModel model = new DefaultTableModel(new Object[][] {
                { 1, "Siti", "available" },
                { 2, "Milo", "pending" },
                { 3, "ORD-01", "shipped" },
                { 4, "ORD-02", "cancelled" }
            }, new Object[] { "ID", "Nama", "Status" });
            JTable table = UiKit.table(model);
            TestAssert.assertEquals(34, table.getRowHeight(), "UiKit.table rowHeight must be 34");
            TestAssert.assertNotNull(table.getRowSorter(), "UiKit.table must configure TableRowSorter");

            JTable adminTable = UiKit.adminTable(model);
            TestAssert.assertEquals(34, adminTable.getRowHeight(), "UiKit.adminTable rowHeight must be 34");
            TestAssert.assertEquals(Theme.Admin.TABLE_HEADER_BG, adminTable.getTableHeader().getBackground(),
                "UiKit.adminTable header must use Theme.Admin.TABLE_HEADER_BG");

            IdPillRenderer idRenderer = new IdPillRenderer();
            adminTable.getColumnModel().getColumn(0).setCellRenderer(idRenderer);
            Component idCell = idRenderer.getTableCellRendererComponent(adminTable, 42, false, false, 0, 0);
            TestAssert.assertEquals("#42", ((JLabel) idCell).getText(), "IdPillRenderer must prefix # to ID");
            idCell.setSize(72, 34);
            TestAssert.assertTrue(hasNonTransparentPixel(paintToImage(idCell, 72, 34)),
                "IdPillRenderer must paint pill cleanly");

            Component narrowId = idRenderer.getTableCellRendererComponent(adminTable, 123456, false, false, 1, 0);
            narrowId.setSize(28, 34);
            BufferedImage narrowIdImg = paintToImage(narrowId, 28, 34);
            int idBg = narrowId.getBackground().getRGB();
            TestAssert.assertEquals(idBg, narrowIdImg.getRGB(0, 17),
                "IdPillRenderer must clip cleanly inside left bound at 28px");
            TestAssert.assertEquals(idBg, narrowIdImg.getRGB(27, 17),
                "IdPillRenderer must clip cleanly inside right bound at 28px");

            StatusBadgeRenderer renderer = new StatusBadgeRenderer();
            table.getColumnModel().getColumn(2).setCellRenderer(renderer);
            JScrollPane scroll = UiKit.scrollPane(table);
            TestAssert.assertNotNull(scroll, "scrollPane must not be null");

            for (int r = 0; r < model.getRowCount(); r++) {
                String rawStatus = String.valueOf(model.getValueAt(r, 2));
                Component cell = renderer.getTableCellRendererComponent(table, rawStatus, r == 0, false, r, 2);
                cell.setSize(120, 34);
                BufferedImage cellImg = paintToImage(cell, 120, 34);
                TestAssert.assertTrue(hasNonTransparentPixel(cellImg),
                    "StatusBadgeRenderer must paint badge for status " + rawStatus);
                TestAssert.assertEquals(FormatUtil.statusLabel(rawStatus), ((JLabel) cell).getText(),
                    "StatusBadgeRenderer text must match FormatUtil.statusLabel");
            }

            Component narrowStatus = renderer.getTableCellRendererComponent(table, "adopted", false, false, 1, 2);
            narrowStatus.setSize(28, 34);
            BufferedImage narrowStatusImg = paintToImage(narrowStatus, 28, 34);
            int statusBg = narrowStatus.getBackground().getRGB();
            TestAssert.assertEquals(statusBg, narrowStatusImg.getRGB(0, 17),
                "StatusBadgeRenderer must clip cleanly inside left bound at 28px");
            TestAssert.assertEquals(statusBg, narrowStatusImg.getRGB(27, 17),
                "StatusBadgeRenderer must clip cleanly inside right bound at 28px");
        });
    }

    private static void testResponsiveCardGridAndScrollHelpers() throws Exception {
        runOnEdt(() -> {
            ResponsiveCardGrid grid = new ResponsiveCardGrid(185, 12, 12);
            TestAssert.assertTrue(grid.getScrollableTracksViewportWidth(),
                "ResponsiveCardGrid must track viewport width");
            TestAssert.assertFalse(grid.getScrollableTracksViewportHeight(),
                "ResponsiveCardGrid must not track viewport height");

            grid.setEmptyState("Tidak ada item", "Coba ubah kata kunci pencarian.");
            grid.setCards(List.of());
            TestAssert.assertEquals(0, grid.getCardCount(), "Empty ResponsiveCardGrid must report 0 cards");
            grid.setSize(420, 220);
            grid.doLayout();
            TestAssert.assertTrue(hasNonTransparentPixel(paintToImage(grid, 420, 220)),
                "Empty ResponsiveCardGrid must paint empty state cleanly");

            List<JPanel> cards = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                JPanel c = UiKit.cardPanel(new BorderLayout());
                c.setPreferredSize(new Dimension(170, 160 + (i % 2) * 20));
                c.add(new JLabel("Kartu " + (i + 1)), BorderLayout.CENTER);
                cards.add(c);
            }
            grid.setCards(cards);
            TestAssert.assertEquals(6, grid.getCardCount(), "ResponsiveCardGrid must track 6 cards");

            grid.setSize(420, 600);
            grid.doLayout();
            TestAssert.assertEquals(2, grid.getColumnCount(),
                "ResponsiveCardGrid at 420px with minCardWidth=185 must lay out 2 columns");
            TestAssert.assertEquals(cards.get(0).getHeight(), cards.get(1).getHeight(),
                "Cards in the same row must share equal row height");
            TestAssert.assertTrue(cards.get(1).getX() >= cards.get(0).getX() + cards.get(0).getWidth(),
                "Adjacent cards must not overlap horizontally");

            grid.setSize(760, 600);
            grid.doLayout();
            TestAssert.assertTrue(grid.getColumnCount() >= 3,
                "ResponsiveCardGrid at 760px must reflow to >= 3 columns (was " + grid.getColumnCount() + ")");

            // Ruling #1: Verify UiKit.verticalScrollPane & UiKit.verticalExpandingScrollPane
            JPanel compactContent = new JPanel();
            compactContent.setPreferredSize(new Dimension(200, 120));
            JScrollPane verticalScroll = UiKit.verticalScrollPane(compactContent, Theme.SURFACE);
            TestAssert.assertEquals(Theme.SURFACE, verticalScroll.getViewport().getBackground(),
                "verticalScrollPane must apply viewport background");
            Scrollable compactHost = (Scrollable) verticalScroll.getViewport().getView();
            TestAssert.assertTrue(compactHost.getScrollableTracksViewportWidth(),
                "verticalScrollPane host must track viewport width");
            TestAssert.assertFalse(compactHost.getScrollableTracksViewportHeight(),
                "verticalScrollPane host must not track viewport height");

            JFrame hostFrame = new JFrame("Scroll Test");
            try {
                hostFrame.addNotify();
                hostFrame.getContentPane().setLayout(new BorderLayout());
                hostFrame.getContentPane().add(verticalScroll, BorderLayout.CENTER);
                hostFrame.setSize(400, 420);
                hostFrame.validate();
                TestAssert.assertTrue(compactContent.getHeight() <= 140,
                    "verticalScrollPane must keep compact content anchored at top (" + compactContent.getHeight() + ")");

                JPanel expandingContent = new JPanel();
                expandingContent.setPreferredSize(new Dimension(200, 120));
                JScrollPane expandingScroll = UiKit.verticalExpandingScrollPane(expandingContent, Theme.Admin.BG);
                TestAssert.assertEquals(Theme.Admin.BG, expandingScroll.getViewport().getBackground(),
                    "verticalExpandingScrollPane must apply viewport background");
                Scrollable expandingHost = (Scrollable) expandingScroll.getViewport().getView();
                TestAssert.assertTrue(expandingHost.getScrollableTracksViewportWidth(),
                    "verticalExpandingScrollPane host must track viewport width");
                TestAssert.assertFalse(expandingHost.getScrollableTracksViewportHeight(),
                    "verticalExpandingScrollPane host must not track viewport height");

                hostFrame.getContentPane().removeAll();
                hostFrame.getContentPane().add(expandingScroll, BorderLayout.CENTER);
                hostFrame.validate();
                JViewport vp = expandingScroll.getViewport();
                TestAssert.assertTrue(((Component) expandingHost).getHeight() >= vp.getHeight(),
                    "verticalExpandingScrollPane must expand host to fill tall viewport");
                TestAssert.assertTrue(expandingContent.getHeight() >= vp.getHeight(),
                    "verticalExpandingScrollPane must expand center content to fill tall viewport");
            } finally {
                hostFrame.dispose();
            }
        });
    }

    private static void testResponsiveCardGridInsideVerticalScrollPaneReflowAndEmptyTransition() throws Exception {
        final JFrame[] frameHolder = new JFrame[1];
        final ResponsiveCardGrid[] gridHolder = new ResponsiveCardGrid[1];
        final JScrollPane[] scrollHolder = new JScrollPane[1];
        try {
            runOnEdt(() -> {
                ResponsiveCardGrid grid = new ResponsiveCardGrid(185, 12, 12);
                List<JPanel> cards = new ArrayList<>();
                for (int i = 0; i < 6; i++) {
                    JPanel c = UiKit.cardPanel(new BorderLayout());
                    c.setPreferredSize(new Dimension(170, 180));
                    c.add(new JLabel("Kartu " + (i + 1)), BorderLayout.CENTER);
                    cards.add(c);
                }
                grid.setCards(cards);

                JPanel wrapper = new JPanel(new BorderLayout(0, 8));
                wrapper.setOpaque(false);
                wrapper.add(UiKit.label("Header Katalog", Theme.heading(), Theme.INK), BorderLayout.NORTH);
                wrapper.add(grid, BorderLayout.CENTER);

                JScrollPane scroll = UiKit.verticalScrollPane(wrapper, Theme.BG);
                JFrame hostFrame = new JFrame("Nested Scroll Grid Test");
                hostFrame.addNotify();
                hostFrame.getContentPane().setLayout(new BorderLayout());
                hostFrame.getContentPane().add(scroll, BorderLayout.CENTER);
                hostFrame.setSize(460, 460);
                hostFrame.validate();

                frameHolder[0] = hostFrame;
                gridHolder[0] = grid;
                scrollHolder[0] = scroll;
            });

            // Drain any queued invokeLater(revalidate) and assert narrow 2-column layout height
            runOnEdt(() -> {
                frameHolder[0].validate();
                ResponsiveCardGrid grid = gridHolder[0];
                JScrollPane scroll = scrollHolder[0];
                TestAssert.assertEquals(2, grid.getColumnCount(),
                    "Nested ResponsiveCardGrid at 460px frame width must use 2 columns");
                TestAssert.assertTrue(grid.getHeight() >= 560,
                    "2-column 6-card grid height (" + grid.getHeight() + ") must cover all 3 rows");
                TestAssert.assertTrue(scroll.getViewport().getViewSize().height >= grid.getHeight(),
                    "Viewport viewSize height must cover full 3-row grid without clipping");
            });

            // Widen frame -> reflows to >=3 columns (2 rows, ~372px grid height)
            runOnEdt(() -> {
                frameHolder[0].setSize(860, 460);
                frameHolder[0].validate();
            });
            runOnEdt(() -> {
                frameHolder[0].validate();
                ResponsiveCardGrid grid = gridHolder[0];
                JScrollPane scroll = scrollHolder[0];
                TestAssert.assertTrue(grid.getColumnCount() >= 3,
                    "Widened nested ResponsiveCardGrid must reflow to >=3 columns (was " + grid.getColumnCount() + ")");
                TestAssert.assertTrue(grid.getHeight() <= 400,
                    "Widened 2-row grid height (" + grid.getHeight() + ") must shrink from 3-row height");
                TestAssert.assertTrue(scroll.getViewport().getViewSize().height <= 460,
                    "Viewport viewSize height (" + scroll.getViewport().getViewSize().height
                        + ") must shrink cleanly without dead scroll space on widen");
            });

            // Narrow back -> reflows back to 2 columns and expands scroll range
            runOnEdt(() -> {
                frameHolder[0].setSize(460, 460);
                frameHolder[0].validate();
            });
            runOnEdt(() -> {
                frameHolder[0].validate();
                ResponsiveCardGrid grid = gridHolder[0];
                JScrollPane scroll = scrollHolder[0];
                TestAssert.assertEquals(2, grid.getColumnCount(),
                    "Re-narrowed nested ResponsiveCardGrid must return to 2 columns");
                TestAssert.assertTrue(scroll.getViewport().getViewSize().height >= 560,
                    "Re-narrowed viewport viewSize height (" + scroll.getViewport().getViewSize().height
                        + ") must expand to fit all 3 rows");
            });

            // Transition populated -> empty state inside nested verticalScrollPane
            runOnEdt(() -> {
                ResponsiveCardGrid grid = gridHolder[0];
                grid.setEmptyState("Tidak ada hasil", "Ubah kata kunci atau filter.");
                grid.setCards(List.of());
                frameHolder[0].validate();
            });
            runOnEdt(() -> {
                frameHolder[0].validate();
                ResponsiveCardGrid grid = gridHolder[0];
                JScrollPane scroll = scrollHolder[0];
                TestAssert.assertEquals(0, grid.getCardCount(), "Empty transition must report 0 cards");
                TestAssert.assertTrue(grid.getHeight() <= 260,
                    "Empty-state grid height (" + grid.getHeight() + ") must shrink cleanly");
                TestAssert.assertTrue(scroll.getViewport().getViewSize().height <= 460,
                    "Viewport viewSize height (" + scroll.getViewport().getViewSize().height
                        + ") must reset cleanly on populated->empty transition");
            });
        } finally {
            if (frameHolder[0] != null) {
                runOnEdt(() -> frameHolder[0].dispose());
            }
        }
    }

    private static void testPagePanelBackgroundAndUiDefaults() throws Exception {
        runOnEdt(() -> {
            Main.applyUiDefaults();
            TestAssert.assertTrue(
                UIManager.getFont("OptionPane.messageFont").getFontName().toLowerCase(Locale.ROOT).contains("poppins"),
                "UIManager OptionPane.messageFont must use Poppins"
            );
            TestAssert.assertEquals(Theme.BG, UIManager.getColor("Panel.background"),
                "UIManager Panel.background must equal Theme.BG");

            PagePanel customerPage = new DummyPage("user.test", "Test", "Sub", "Sum", new AtomicInteger());
            TestAssert.assertEquals(Theme.BG, customerPage.getBackground(),
                "Default PagePanel must use Theme.BG");

            PagePanel adminPage = new PagePanel() {
                private static final long serialVersionUID = 1L;

                @Override
                protected Color defaultBackground() {
                    return Theme.Admin.BG;
                }

                @Override
                public String getPageKey() {
                    return "admin.test";
                }

                @Override
                public String getPageTitle() {
                    return "Admin";
                }

                @Override
                public String getPageSubtitle() {
                    return "Sub";
                }

                @Override
                public void refreshData() {
                }
            };
            TestAssert.assertEquals(Theme.Admin.BG, adminPage.getBackground(),
                "PagePanel overriding defaultBackground() must return Theme.Admin.BG even after applyUiDefaults()");
        });
    }

    private static void testAuthPanelDbStatusAndErrorHandling() throws Exception {
        try {
            DatabaseConfig.overrideForTest(
                "jdbc:mysql://127.0.0.1:33999/pet_care?connectTimeout=500",
                "root",
                ""
            );

            final JFrame[] frameHolder = new JFrame[1];
            final AuthPanel[] panelHolder = new AuthPanel[1];
            final int[] listenersBeforeHolder = new int[1];

            try {
                runOnEdt(() -> {
                    listenersBeforeHolder[0] = Toolkit.getDefaultToolkit()
                        .getAWTEventListeners(AWTEvent.MOUSE_MOTION_EVENT_MASK).length;

                    JFrame hostFrame = new JFrame("Auth Test");
                    frameHolder[0] = hostFrame;
                    AuthPanel authPanel = new AuthPanel();
                    panelHolder[0] = authPanel;
                    hostFrame.getContentPane().add(authPanel, BorderLayout.CENTER);
                    TestAssert.assertEquals("MySQL Terputus", authPanel.getDbStatusText(),
                        "AuthPanel must show MySQL Terputus when DB port is unreachable");

                    TestAssert.assertNull(findFirstTabbedPane(authPanel),
                        "AuthPanel must NOT use JTabbedPane in website-matched card design");
                    TestAssert.assertFalse(authPanel.isRegisterMode(),
                        "AuthPanel must start in login mode");
                    TestAssert.assertNotNull(findLabelByText(authPanel, "Selamat Datang Kembali!"),
                        "AuthPanel login face must display Selamat Datang Kembali!");

                    JRootPane rootPane = hostFrame.getRootPane();
                    TestAssert.assertNotNull(rootPane.getDefaultButton(),
                        "Login face must configure rootPane default button for Enter submission");
                    TestAssert.assertEquals("Masuk", rootPane.getDefaultButton().getText(),
                        "Login face default button must be Masuk");

                    // Dispatch zero-bounds mouse motion before addNotify without throwing
                    MouseEvent preDisplayMove = new MouseEvent(
                        authPanel, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 60, 60, 0, false
                    );
                    authPanel.dispatchEvent(preDisplayMove);

                    // Real animated flipTimer toggle: Login -> Register
                    JButton toRegisterBtn = findButtonByText(authPanel, "Daftar di sini");
                    TestAssert.assertNotNull(toRegisterBtn, "Login face must expose Daftar di sini button");
                    toRegisterBtn.doClick();
                    TestAssert.assertTrue(authPanel.isFlipAnimating(),
                        "Clicking Daftar di sini must start animated flipTimer");
                });

                awaitFlipAnimationStop(panelHolder[0]);

                runOnEdt(() -> {
                    AuthPanel authPanel = panelHolder[0];
                    JRootPane rootPane = frameHolder[0].getRootPane();
                    TestAssert.assertFalse(authPanel.isFlipAnimating(),
                        "flipTimer must stop after Login -> Register transition completes");
                    TestAssert.assertTrue(authPanel.isRegisterMode(),
                        "Animated flip to Register must activate register mode");
                    TestAssert.assertNotNull(findLabelByText(authPanel, "Gabung Yuk!"),
                        "Register face must display Gabung Yuk!");
                    TestAssert.assertNotNull(rootPane.getDefaultButton(),
                        "Register face must configure rootPane default button for Enter submission");
                    TestAssert.assertEquals("Daftar & Masuk", rootPane.getDefaultButton().getText(),
                        "Register face default button must be Daftar & Masuk");

                    // Real animated flipTimer toggle: Register -> Login
                    JButton toLoginBtn = findButtonByText(authPanel, "Masuk di sini");
                    TestAssert.assertNotNull(toLoginBtn, "Register face must expose Masuk di sini button");
                    toLoginBtn.doClick();
                    TestAssert.assertTrue(authPanel.isFlipAnimating(),
                        "Clicking Masuk di sini must start animated flipTimer");
                });

                awaitFlipAnimationStop(panelHolder[0]);

                runOnEdt(() -> {
                    JFrame hostFrame = frameHolder[0];
                    AuthPanel authPanel = panelHolder[0];
                    JRootPane rootPane = hostFrame.getRootPane();
                    TestAssert.assertFalse(authPanel.isFlipAnimating(),
                        "flipTimer must stop after Register -> Login transition completes");
                    TestAssert.assertFalse(authPanel.isRegisterMode(),
                        "Animated flip back must restore login mode");
                    TestAssert.assertEquals("Masuk", rootPane.getDefaultButton().getText(),
                        "Switching back to login face must restore Masuk default button");

                    // Synchronous immediate switch helpers remain deterministic
                    authPanel.showRegisterFaceImmediate();
                    TestAssert.assertTrue(authPanel.isRegisterMode(),
                        "showRegisterFaceImmediate must synchronously switch to register mode");
                    authPanel.showLoginFaceImmediate();
                    TestAssert.assertFalse(authPanel.isRegisterMode(),
                        "showLoginFaceImmediate must synchronously switch to login mode");

                    hostFrame.addNotify();
                    int listenersDuring = Toolkit.getDefaultToolkit()
                        .getAWTEventListeners(AWTEvent.MOUSE_MOTION_EVENT_MASK).length;
                    TestAssert.assertEquals(listenersBeforeHolder[0] + 1, listenersDuring,
                        "AuthPanel MascotView must register 1 MOUSE_MOTION AWTEventListener on addNotify()");

                    // Zero-bounds AWTEvent dispatch while listener is attached must not throw
                    authPanel.setSize(0, 0);
                    authPanel.validate();
                    for (AWTEventListener l : Toolkit.getDefaultToolkit()
                            .getAWTEventListeners(AWTEvent.MOUSE_MOTION_EVENT_MASK)) {
                        l.eventDispatched(new MouseEvent(
                            authPanel, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 10, 10, 0, false
                        ));
                    }

                    // Validate Login face + demoHintPill at 1040x680 and 1440x1080
                    authPanel.showLoginFaceImmediate();
                    JLabel demoHintLine1 = findLabelByText(authPanel, "Akun demo: admin@pawpaw.com / admin123");
                    TestAssert.assertNotNull(demoHintLine1, "AuthPanel login face must display wrapped demo hint");
                    JComponent demoHintPill = (JComponent) demoHintLine1.getParent();
                    JButton loginSubmitBtn = findButtonByText(authPanel, "Masuk");
                    TestAssert.assertNotNull(loginSubmitBtn, "Login submit button must exist");
                    Container loginFlipCard = loginSubmitBtn.getParent().getParent().getParent();

                    for (int[] size : new int[][] { { 1040, 680 }, { 1440, 1080 } }) {
                        hostFrame.setSize(size[0], size[1]);
                        hostFrame.validate();

                        TestAssert.assertTrue(demoHintPill.isVisible() && demoHintPill.getWidth() > 0 && demoHintPill.getHeight() > 0,
                            "demoHintPill must be visible with positive bounds at " + size[0] + "x" + size[1]);
                        Insets pillInsets = demoHintPill.getInsets();
                        int pillInnerW = demoHintPill.getWidth() - pillInsets.left - pillInsets.right;
                        for (Component child : demoHintPill.getComponents()) {
                            if (child instanceof JLabel lbl) {
                                TestAssert.assertTrue(lbl.getPreferredSize().width <= pillInnerW,
                                    "Demo hint label '" + lbl.getText() + "' pref width ("
                                        + lbl.getPreferredSize().width + ") must fit inside demoHintPill inner width ("
                                        + pillInnerW + ") at " + size[0] + "x" + size[1]);
                                TestAssert.assertTrue(lbl.getX() >= pillInsets.left
                                        && lbl.getX() + lbl.getWidth() <= demoHintPill.getWidth() - pillInsets.right,
                                    "Demo hint label bounds (" + lbl.getX() + ".." + (lbl.getX() + lbl.getWidth())
                                        + ") must stay inside demoHintPill (" + pillInsets.left + ".."
                                        + (demoHintPill.getWidth() - pillInsets.right) + ") at " + size[0] + "x" + size[1]);
                            }
                        }

                        BufferedImage loginImg = paintToImage(authPanel, size[0], size[1]);
                        TestAssert.assertTrue(hasNonTransparentPixel(loginImg),
                            "AuthPanel login face must paint cleanly at " + size[0] + "x" + size[1]);
                        TestAssert.assertTrue(loginImg.getRGB(10, 10) != Color.WHITE.getRGB(),
                            "AuthPanel corner must paint radial backdrop at " + size[0] + "x" + size[1]);
                        Point cardPt = SwingUtilities.convertPoint(loginFlipCard, 12, 24, authPanel);
                        TestAssert.assertEquals(Theme.SURFACE.getRGB(), loginImg.getRGB(cardPt.x, cardPt.y),
                            "AuthPanel card interior must paint white surface at " + size[0] + "x" + size[1]);
                    }

                    // Inline error & Register card fit at 1040x680 and 1440x1080
                    authPanel.attemptLoginForTest("admin@pawpaw.com", "admin123");
                    String errorMsg = authPanel.getInlineErrorText();
                    TestAssert.assertTrue(errorMsg != null && !errorMsg.isBlank(),
                        "AuthPanel must show inline error when login fails against unreachable DB");

                    authPanel.attemptRegisterForTest("Tester", "invalid-email", "0812", "123", "456");
                    TestAssert.assertTrue(authPanel.isRegisterMode(),
                        "attemptRegisterForTest must keep register face active on validation error");
                    String regError = authPanel.getInlineErrorText();
                    TestAssert.assertTrue(regError != null && !regError.isBlank(),
                        "AuthPanel must show inline error on Register validation failure");

                    JButton regSubmitBtn = findButtonByText(authPanel, "Daftar & Masuk");
                    TestAssert.assertNotNull(regSubmitBtn, "Register submit button must exist");
                    Container regFlipCard = regSubmitBtn.getParent().getParent().getParent();

                    for (int[] size : new int[][] { { 1040, 680 }, { 1440, 1080 } }) {
                        hostFrame.setSize(size[0], size[1]);
                        hostFrame.validate();

                        TestAssert.assertTrue(regSubmitBtn.getHeight() >= 30 && regSubmitBtn.getWidth() >= 180,
                            "Register submit button must lay out cleanly without squashing at "
                                + size[0] + "x" + size[1] + " (" + regSubmitBtn.getWidth() + "x" + regSubmitBtn.getHeight() + ")");
                        TestAssert.assertTrue(regFlipCard.getHeight() >= regFlipCard.getPreferredSize().height,
                            "Register card height (" + regFlipCard.getHeight()
                                + ") must fit preferred height (" + regFlipCard.getPreferredSize().height
                                + ") at " + size[0] + "x" + size[1]);
                        Point submitInCard = SwingUtilities.convertPoint(regSubmitBtn, 0, 0, regFlipCard);
                        TestAssert.assertTrue(submitInCard.y + regSubmitBtn.getHeight() <= regFlipCard.getHeight(),
                            "Register submit button must remain inside Register card bounds at " + size[0] + "x" + size[1]);

                        BufferedImage authImg = paintToImage(authPanel, size[0], size[1]);
                        TestAssert.assertTrue(hasNonTransparentPixel(authImg),
                            "AuthPanel radial backdrop, peeking mascot, and Register card must paint cleanly at "
                                + size[0] + "x" + size[1]);
                        TestAssert.assertTrue(authImg.getRGB(10, 10) != Color.WHITE.getRGB(),
                            "AuthPanel top-left corner must paint radial sky-blue backdrop at " + size[0] + "x" + size[1]);
                        Point cardPt = SwingUtilities.convertPoint(regFlipCard, 12, 24, authPanel);
                        TestAssert.assertEquals(Theme.SURFACE.getRGB(), authImg.getRGB(cardPt.x, cardPt.y),
                            "AuthPanel Register card interior must paint solid white card surface at "
                                + size[0] + "x" + size[1]);
                    }
                    assertNoEmojiGlyphs(authPanel);
                });
            } finally {
                if (frameHolder[0] != null) {
                    runOnEdt(() -> {
                        frameHolder[0].dispose();
                        int listenersAfter = Toolkit.getDefaultToolkit()
                            .getAWTEventListeners(AWTEvent.MOUSE_MOTION_EVENT_MASK).length;
                        TestAssert.assertEquals(listenersBeforeHolder[0], listenersAfter,
                            "AuthPanel MascotView must remove its AWTEventListener cleanly on hostFrame.dispose()");
                    });
                }
            }

            DatabaseConfig.resetConfig();
            if (DatabaseConfig.testConnection()) {
                runOnEdt(() -> {
                    class InMemoryRememberStore implements AuthPanel.RememberStore {
                        private boolean enabled;
                        private String identity = "";

                        @Override
                        public boolean isRememberEnabled() {
                            return enabled;
                        }

                        @Override
                        public String getRememberedIdentity() {
                            return identity;
                        }

                        @Override
                        public void save(boolean enabled, String identity) {
                            this.enabled = enabled;
                            this.identity = identity != null ? identity : "";
                        }
                    }

                    InMemoryRememberStore memoryStore = new InMemoryRememberStore();
                    AtomicReference<User> authedUser = new AtomicReference<>();
                    AuthPanel authPanel = new AuthPanel(
                        new pawpaw.service.AuthService(),
                        authedUser::set,
                        memoryStore
                    );
                    TestAssert.assertEquals("MySQL Terhubung: pet_care", authPanel.getDbStatusText(),
                        "AuthPanel must show MySQL Terhubung: pet_care when DB is reachable");

                    authPanel.setRememberMeSelected(true);
                    authPanel.attemptLoginForTest("admin@pawpaw.com", "admin123");
                    TestAssert.assertNotNull(authedUser.get(),
                        "Live login with admin@pawpaw.com/admin123 must succeed");
                    TestAssert.assertTrue(memoryStore.isRememberEnabled(),
                        "RememberStore must persist enabled flag when Ingat saya is checked");
                    TestAssert.assertEquals("admin@pawpaw.com", memoryStore.getRememberedIdentity(),
                        "RememberStore must persist identity when Ingat saya is checked");

                    authPanel.resetForm();
                    TestAssert.assertEquals("admin@pawpaw.com", authPanel.getIdentityFieldText(),
                        "resetForm() must prefill remembered identity");
                    TestAssert.assertEquals(0, authPanel.getPasswordLength(),
                        "resetForm() must never prefill or retain password");
                    TestAssert.assertTrue(authPanel.isRememberMeSelected(),
                        "resetForm() must keep Ingat saya checked when enabled in store");

                    authedUser.set(null);
                    authPanel.setRememberMeSelected(false);
                    authPanel.attemptLoginForTest("admin@pawpaw.com", "admin123");
                    TestAssert.assertNotNull(authedUser.get(),
                        "Live login with unchecked Ingat saya must succeed");
                    TestAssert.assertFalse(memoryStore.isRememberEnabled(),
                        "RememberStore must clear enabled flag when Ingat saya is unchecked");
                    TestAssert.assertEquals("", memoryStore.getRememberedIdentity(),
                        "RememberStore must clear saved identity when Ingat saya is unchecked");

                    authPanel.resetForm();
                    TestAssert.assertEquals("", authPanel.getIdentityFieldText(),
                        "resetForm() must leave identity empty after unchecking Ingat saya");
                    TestAssert.assertFalse(authPanel.isRememberMeSelected(),
                        "resetForm() must keep Ingat saya unchecked after clearing");
                });
            }
        } finally {
            DatabaseConfig.resetConfig();
        }
    }

    private static void testDesktopShellNavigationAndRoleModes() throws Exception {
        runOnEdt(() -> {
            JFrame hostFrame = new JFrame("Test Shell");
            try {
                DesktopShell shell = new DesktopShell();
                hostFrame.getContentPane().add(shell, BorderLayout.CENTER);
                hostFrame.setSize(1100, 720);

                AtomicInteger dashRefreshCount = new AtomicInteger();
                AtomicInteger prodRefreshCount = new AtomicInteger();

                PagePanel adminDashboard = new DummyPage(
                    "admin.dashboard",
                    "Dashboard Admin",
                    "Ringkasan metrik toko dan klinik",
                    "Statistik aktif",
                    dashRefreshCount
                );
                PagePanel adminProducts = new DummyPage(
                    "admin.products",
                    "Produk",
                    "Katalog dan stok produk",
                    "10 produk dimuat",
                    prodRefreshCount
                );
                PagePanel userDashboard = new DummyPage(
                    "user.dashboard",
                    "Beranda",
                    "Ringkasan akun pelanggan",
                    "Selamat datang",
                    new AtomicInteger()
                );

                shell.registerPage(adminDashboard);
                shell.registerPage(adminProducts);
                shell.registerPage(userDashboard);

                User admin = new User();
                admin.setUserId(11);
                admin.setName("Admin PawPaw");
                admin.setEmail("admin@pawpaw.com");
                admin.setRole("admin");

                shell.applySession(admin, true, false);
                TestAssert.assertTrue(shell.isAdminPaletteActive(),
                    "Admin session must activate Admin palette");
                TestAssert.assertFalse(shell.isSidebarCollapsed(),
                    "DesktopShell sidebar must start expanded");
                TestAssert.assertEquals(245, shell.getSidebarWidth(),
                    "Expanded Admin sidebar width must be 245px");
                TestAssert.assertTrue(shell.isSwitchModeButtonVisible(),
                    "Admin mode must display switch mode button");
                TestAssert.assertEquals("Mode Pelanggan", shell.getSwitchModeButtonText(),
                    "Admin mode must display Mode Pelanggan button");

                shell.navigateTo("admin.products");
                TestAssert.assertEquals("admin.products", shell.getActivePageKey(),
                    "navigateTo must switch active page key");
                TestAssert.assertEquals(1, prodRefreshCount.get(),
                    "navigateTo must trigger refreshData() on target page");
                TestAssert.assertEquals("10 produk dimuat", shell.getStatusSummaryText(),
                    "navigateTo must update status bar summary from PagePanel");
                TestAssert.assertNotNull(adminProducts.getToolbarAccessory().getParent(),
                    "Toolbar accessory of active PagePanel must be mounted in header bar");

                hostFrame.addNotify();
                Container sidebarComp = (Container) ((BorderLayout) shell.getLayout()).getLayoutComponent(BorderLayout.WEST);

                for (int[] size : new int[][] { { 1040, 680 }, { 1440, 1080 } }) {
                    hostFrame.setSize(size[0], size[1]);
                    hostFrame.validate();
                    TestAssert.assertEquals(245, sidebarComp.getWidth(),
                        "Laid-out expanded Admin sidebar width must be 245px at " + size[0] + "x" + size[1]);
                    assertTopBarHealthy(shell, "Katalog dan stok produk", false, size[0], size[1]);
                }

                shell.toggleSidebar();
                hostFrame.validate();
                TestAssert.assertTrue(shell.isSidebarCollapsed(),
                    "toggleSidebar() must collapse Admin sidebar");
                TestAssert.assertEquals(84, shell.getSidebarWidth(),
                    "Collapsed Admin sidebar width must be 84px");
                TestAssert.assertEquals(84, sidebarComp.getWidth(),
                    "Laid-out collapsed Admin sidebar width must be 84px");
                TestAssert.assertEquals("Mode Pelanggan", shell.getSwitchModeButtonText(),
                    "getSwitchModeButtonText() must keep Mode Pelanggan while collapsed");
                assertCollapsedSidebarButtons(sidebarComp);

                shell.toggleSidebar();
                hostFrame.validate();
                TestAssert.assertFalse(shell.isSidebarCollapsed(),
                    "Second toggleSidebar() must expand Admin sidebar back");
                TestAssert.assertEquals(245, shell.getSidebarWidth(),
                    "Re-expanded Admin sidebar width must return to 245px");
                TestAssert.assertEquals(245, sidebarComp.getWidth(),
                    "Laid-out re-expanded Admin sidebar width must return to 245px");
                TestAssert.assertNotNull(findButtonByText(sidebarComp, "Produk"),
                    "Re-expanded Admin sidebar must restore nav button text");
                TestAssert.assertNotNull(findButtonByText(sidebarComp, "Mode Pelanggan"),
                    "Re-expanded Admin sidebar must restore switch mode button text");

                shell.refreshActivePage();
                TestAssert.assertEquals(2, prodRefreshCount.get(),
                    "refreshActivePage must invoke refreshData() on active page");

                shell.applySession(admin, false, true);
                TestAssert.assertFalse(shell.isAdminPaletteActive(),
                    "Customer preview mode must deactivate Admin palette");
                TestAssert.assertEquals(220, shell.getSidebarWidth(),
                    "Expanded Customer sidebar width must be 220px");
                TestAssert.assertTrue(shell.isSwitchModeButtonVisible(),
                    "Admin in User preview mode must display return-to-admin button");
                TestAssert.assertEquals("Kembali ke Panel Admin", shell.getSwitchModeButtonText(),
                    "Admin in User mode must display Kembali ke Panel Admin");

                User customer = new User();
                customer.setUserId(1);
                customer.setName("Jesika Putri");
                customer.setEmail("jesika@pawpaw.com");
                customer.setRole("user");

                shell.applySession(customer, false, false);
                shell.navigateTo("user.dashboard");
                shell.setCartCount(3);
                for (int[] size : new int[][] { { 1040, 680 }, { 1440, 1080 } }) {
                    hostFrame.setSize(size[0], size[1]);
                    hostFrame.validate();
                    TestAssert.assertFalse(shell.isAdminPaletteActive(),
                        "Customer session must deactivate Admin palette");
                    TestAssert.assertEquals(220, shell.getSidebarWidth(),
                        "Expanded Customer sidebar width must be 220px");
                    TestAssert.assertEquals(220, sidebarComp.getWidth(),
                        "Laid-out expanded Customer sidebar width must be 220px at " + size[0] + "x" + size[1]);
                    TestAssert.assertFalse(shell.isSwitchModeButtonVisible(),
                        "Regular user mode must hide Admin/Customer switch button");
                    assertTopBarHealthy(shell, "Ringkasan akun pelanggan", true, size[0], size[1]);
                    TestAssert.assertNotNull(findButtonByText(shell, "Keranjang (3)"),
                        "Customer mode must display updated cart button count");
                }

                shell.toggleSidebar();
                hostFrame.validate();
                TestAssert.assertTrue(shell.isSidebarCollapsed(),
                    "toggleSidebar() must collapse Customer sidebar");
                TestAssert.assertEquals(84, shell.getSidebarWidth(),
                    "Collapsed Customer sidebar width must be 84px");
                TestAssert.assertEquals(84, sidebarComp.getWidth(),
                    "Laid-out collapsed Customer sidebar width must be 84px");
                assertCollapsedSidebarButtons(sidebarComp);

                shell.setSidebarCollapsed(false);
                hostFrame.validate();
                TestAssert.assertFalse(shell.isSidebarCollapsed(),
                    "setSidebarCollapsed(false) must restore expanded Customer sidebar");
                TestAssert.assertEquals(220, shell.getSidebarWidth(),
                    "Re-expanded Customer sidebar width must return to 220px");
                TestAssert.assertNotNull(findButtonByText(sidebarComp, "Beranda"),
                    "Re-expanded Customer sidebar must restore nav button text");

                shell.updateDbStatus(false, "Connection refused");
                TestAssert.assertEquals("MySQL Terputus", shell.getDbStatusText(),
                    "DesktopShell status bar must show MySQL Terputus when disconnected");

                shell.updateDbStatus(true, "pet_care");
                TestAssert.assertEquals("MySQL Terhubung: pet_care", shell.getDbStatusText(),
                    "DesktopShell status bar must show MySQL Terhubung: pet_care when connected");
                assertNoEmojiGlyphs(shell);
            } finally {
                hostFrame.dispose();
            }
        });
    }

    private static void testAppWindowLifecycle() throws Exception {
        runOnEdt(() -> {
            AppWindow window = new AppWindow();
            try {
                TestAssert.assertNotNull(window.getSession(), "AppWindow must expose SessionState");
                TestAssert.assertNotNull(window.getAuthService(), "AppWindow must expose AuthService");
                TestAssert.assertNotNull(window.getProfileService(), "AppWindow must expose ProfileService");
                TestAssert.assertNotNull(window.getAdoptionService(), "AppWindow must expose AdoptionService");
                TestAssert.assertNotNull(window.getShopService(), "AppWindow must expose ShopService");
                TestAssert.assertNotNull(window.getHealthBookingService(), "AppWindow must expose HealthBookingService");
                TestAssert.assertNotNull(window.getAdminDashboardService(), "AppWindow must expose AdminDashboardService");

                JButton initialDefault = window.getRootPane().getDefaultButton();
                TestAssert.assertNotNull(initialDefault, "AppWindow must start on AuthPanel with Masuk default button");
                TestAssert.assertEquals("Masuk", initialDefault.getText(),
                    "Initial AppWindow default button must be Masuk");

                User admin = new User();
                admin.setUserId(11);
                admin.setName("Admin PawPaw");
                admin.setEmail("admin@pawpaw.com");
                admin.setRole("admin");

                window.onLoginSuccess(admin);
                TestAssert.assertTrue(window.getSession().isLoggedIn(), "Session must be logged in");
                TestAssert.assertFalse(window.getSession().isAdminPreviewAsUser(),
                    "Admin login must land in Admin workspace first");
                TestAssert.assertNull(window.getRootPane().getDefaultButton(),
                    "openAdminWorkspace() must clear rootPane defaultButton");

                window.openUserWorkspace();
                TestAssert.assertTrue(window.getSession().isAdminPreviewAsUser(),
                    "openUserWorkspace by admin must enable adminPreviewAsUser");
                TestAssert.assertNull(window.getRootPane().getDefaultButton(),
                    "openUserWorkspace() must clear rootPane defaultButton");

                window.navigate("user.shop");
                window.refreshCartBadge();
                window.updateStatus("Mode pelanggan aktif");

                window.openAdminWorkspace();
                TestAssert.assertFalse(window.getSession().isAdminPreviewAsUser(),
                    "openAdminWorkspace must exit adminPreviewAsUser");
                TestAssert.assertNull(window.getRootPane().getDefaultButton(),
                    "Returning to openAdminWorkspace() must keep rootPane defaultButton null");

                window.showAuth();
                TestAssert.assertFalse(window.getSession().isLoggedIn(), "showAuth must log out session");
                JButton restoredDefault = window.getRootPane().getDefaultButton();
                TestAssert.assertNotNull(restoredDefault,
                    "showAuth() must restore AuthPanel default button");
                TestAssert.assertEquals("Masuk", restoredDefault.getText(),
                    "showAuth() must restore Masuk as default button");
            } finally {
                window.dispose();
            }
        });
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

    private static BufferedImage paintToImage(Component component, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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

    private static JTabbedPane findFirstTabbedPane(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTabbedPane tp) {
                return tp;
            }
            if (child instanceof Container nested) {
                JTabbedPane found = findFirstTabbedPane(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JLabel findLabelByText(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel lbl && text.equals(lbl.getText())) {
                return lbl;
            }
            if (child instanceof Container nested) {
                JLabel found = findLabelByText(nested, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void awaitFlipAnimationStop(AuthPanel authPanel) throws Exception {
        long deadlineNanos = System.nanoTime() + 2_000_000_000L;
        final boolean[] animating = { true };
        while (System.nanoTime() < deadlineNanos) {
            runOnEdt(() -> animating[0] = authPanel.isFlipAnimating());
            if (!animating[0]) {
                return;
            }
            Thread.sleep(20L);
        }
        throw new AssertionError("AuthPanel flipTimer did not complete within 2000ms");
    }

    private static void assertTopBarHealthy(
            DesktopShell shell,
            String expectedSubtitle,
            boolean expectCartVisible,
            int w,
            int h) {
        String ctx = "DesktopShell@" + w + "x" + h;
        JLabel subtitleLabel = findLabelByText(shell, expectedSubtitle);
        TestAssert.assertNotNull(subtitleLabel, ctx + " header subtitle label must be present");
        Container titleBox = subtitleLabel.getParent();
        JComponent topBar = (JComponent) titleBox.getParent();
        Insets barInsets = topBar.getInsets();
        int topBarPrefInner = topBar.getPreferredSize().height - barInsets.top - barInsets.bottom;
        int topBarActualInner = topBar.getHeight() - barInsets.top - barInsets.bottom;
        int titlePrefH = titleBox.getPreferredSize().height;
        TestAssert.assertTrue(topBarPrefInner >= titlePrefH,
            ctx + " topBar preferred inner height (" + topBarPrefInner
                + ") must fit titleBox (" + titlePrefH + ") without clipping");
        TestAssert.assertTrue(topBarActualInner >= titlePrefH,
            ctx + " topBar laid-out inner height (" + topBarActualInner
                + ") must fit titleBox (" + titlePrefH + ") without clipping");

        JButton cartBtn = findButtonStartingWith(topBar, "Keranjang (");
        TestAssert.assertNotNull(cartBtn, ctx + " topBar must contain cart button");
        if (expectCartVisible) {
            TestAssert.assertTrue(cartBtn.isVisible(), ctx + " cart button must be visible in Customer mode");
            TestAssert.assertTrue(cartBtn.getWidth() > 0 && cartBtn.getHeight() > 0,
                ctx + " cart button must have positive size");
            Rectangle titleRect = SwingUtilities.convertRectangle(titleBox.getParent(), titleBox.getBounds(), topBar);
            Rectangle cartRect = SwingUtilities.convertRectangle(cartBtn.getParent(), cartBtn.getBounds(), topBar);
            TestAssert.assertTrue(cartRect.x >= titleRect.x + titleRect.width,
                ctx + " cart button (" + cartRect.x + ") must not overlap titleBox right edge ("
                    + (titleRect.x + titleRect.width) + ")");
        } else {
            TestAssert.assertFalse(cartBtn.isVisible(), ctx + " cart button must be hidden in Admin mode");
        }
    }

    private static void assertCollapsedSidebarButtons(Container sidebarComp) {
        List<JButton> visibleButtons = new ArrayList<>();
        collectVisibleButtons(sidebarComp, visibleButtons);
        TestAssert.assertTrue(visibleButtons.size() >= 3,
            "Collapsed sidebar must keep visible logo, nav, and action buttons");
        for (JButton btn : visibleButtons) {
            TestAssert.assertEquals("", btn.getText(),
                "Collapsed sidebar button must clear visible text while retaining tooltip");
            String tooltip = btn.getToolTipText();
            TestAssert.assertTrue(tooltip != null && !tooltip.isBlank(),
                "Collapsed sidebar button must preserve non-blank tooltip");
        }
    }

    private static void collectVisibleButtons(Container root, List<JButton> out) {
        for (Component child : root.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            if (child instanceof JButton btn) {
                out.add(btn);
            }
            if (child instanceof Container nested) {
                collectVisibleButtons(nested, out);
            }
        }
    }

    private static JButton findButtonStartingWith(Container root, String prefix) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton btn && btn.getText() != null && btn.getText().startsWith(prefix)) {
                return btn;
            }
            if (child instanceof Container nested) {
                JButton found = findButtonStartingWith(nested, prefix);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton findButtonByText(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton btn && text.equals(btn.getText())) {
                return btn;
            }
            if (child instanceof Container nested) {
                JButton found = findButtonByText(nested, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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

    private static final class DummyPage extends PagePanel {
        private static final long serialVersionUID = 1L;
        private final String key;
        private final String title;
        private final String subtitle;
        private final String summary;
        private final transient AtomicInteger refreshes;
        private final JLabel accessory = new JLabel("Aksesori");

        DummyPage(String key, String title, String subtitle, String summary, AtomicInteger refreshes) {
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
            this.summary = summary;
            this.refreshes = refreshes;
            setLayout(new BorderLayout());
            add(new JLabel(title), BorderLayout.CENTER);
        }

        @Override
        public String getPageKey() {
            return key;
        }

        @Override
        public String getPageTitle() {
            return title;
        }

        @Override
        public String getPageSubtitle() {
            return subtitle;
        }

        @Override
        public void refreshData() {
            refreshes.incrementAndGet();
        }

        @Override
        public JComponent getToolbarAccessory() {
            return accessory;
        }

        @Override
        public String getStatusSummary() {
            return summary;
        }
    }
}
