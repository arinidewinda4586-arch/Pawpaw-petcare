package pawpaw.gui.admin;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.IdPillRenderer;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.StatusBadgeRenderer;
import pawpaw.gui.common.UiKit;
import pawpaw.model.AdoptionRequest;
import pawpaw.model.DashboardStats;
import pawpaw.model.HealthBooking;
import pawpaw.model.Order;
import pawpaw.model.User;
import pawpaw.service.AdminDashboardService;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Admin overview dashboard presenting highlighted revenue, 6 operational KPI cards,
 * quick-create actions, and tabbed latest-5 activity tables for orders, adoptions, and bookings.
 */
public final class AdminDashboardPanel extends PagePanel {

    private static final long serialVersionUID = 1L;

    private final transient AppWindow app;

    private final JLabel inlineErrorLabel;
    private final JLabel revenueValueLabel;
    private final JLabel revenueSubtitleLabel;

    private final UiKit.KpiCard productsCard;
    private final UiKit.KpiCard ordersCard;
    private final UiKit.KpiCard animalsCard;
    private final UiKit.KpiCard adoptionsCard;
    private final UiKit.KpiCard bookingsCard;
    private final UiKit.KpiCard customersCard;

    private final JButton quickAddProductButton;
    private final JButton quickAddAnimalButton;
    private final JButton quickAddServiceButton;

    private final JTabbedPane recentTabs;
    private final AdminWidgets.ReadOnlyTableModel recentOrdersModel;
    private final AdminWidgets.ReadOnlyTableModel recentAdoptionsModel;
    private final AdminWidgets.ReadOnlyTableModel recentBookingsModel;
    private final JLabel recentHintLabel;

    private String statusSummary = "Dashboard admin siap.";

    public AdminDashboardPanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineErrorLabel = AdminWidgets.createFeedbackBanner();

        // --- Top Hero Strip: Highlighted Revenue Card + Quick Action Card ---
        revenueValueLabel = UiKit.label(FormatUtil.rupiah(BigDecimal.ZERO), Theme.kpiValue(), Theme.GOLD);
        revenueSubtitleLabel = UiKit.label(
            "Pesanan dibayar, diproses, dikirim & selesai",
            Theme.small(),
            Theme.Admin.NAVY_TEXT
        );
        JPanel revenueCard = buildRevenueHeroCard();

        quickAddProductButton = UiKit.adminPrimaryButton("+ Produk", VectorIcons.Kind.PLUS);
        quickAddProductButton.addActionListener(e -> runQuickAddProduct());

        quickAddAnimalButton = UiKit.adminLightButton("+ Hewan", VectorIcons.Kind.PLUS);
        quickAddAnimalButton.addActionListener(e -> runQuickAddAnimal());

        quickAddServiceButton = UiKit.adminLightButton("+ Layanan", VectorIcons.Kind.PLUS);
        quickAddServiceButton.addActionListener(e -> runQuickAddService());

        JPanel quickActionCard = buildQuickActionCard();

        JPanel topStrip = new JPanel(new GridBagLayout());
        topStrip.setOpaque(false);
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.gridy = 0;
        topGbc.fill = GridBagConstraints.BOTH;
        topGbc.weighty = 1.0;

        topGbc.gridx = 0;
        topGbc.weightx = 0.45;
        topGbc.insets = new Insets(0, 0, 0, Theme.SPACE_12);
        topStrip.add(revenueCard, topGbc);

        topGbc.gridx = 1;
        topGbc.weightx = 0.55;
        topGbc.insets = new Insets(0, 0, 0, 0);
        topStrip.add(quickActionCard, topGbc);

        // --- 6 Operational KPI Cards (2 rows x 3 columns for crisp 1040x680 fit) ---
        productsCard = UiKit.adminKpiCard("Produk", "0", "Total produk di katalog", VectorIcons.Kind.SHOP);
        ordersCard = UiKit.adminKpiCard("Pesanan", "0", "Total transaksi pesanan", VectorIcons.Kind.ORDERS);
        animalsCard = UiKit.adminKpiCard("Hewan Adopsi", "0", "Hewan terdaftar di katalog", VectorIcons.Kind.PAW);
        adoptionsCard = UiKit.adminKpiCard(
            "Pengajuan Adopsi", "0", "Permohonan adopsi masuk", VectorIcons.Kind.ADOPTION
        );
        bookingsCard = UiKit.adminKpiCard(
            "Booking Kesehatan", "0", "Jadwal kunjungan klinik", VectorIcons.Kind.BOOKINGS
        );
        customersCard = UiKit.adminKpiCard(
            "Pelanggan", "0", "Akun pelanggan terdaftar", VectorIcons.Kind.PROFILE
        );

        JPanel kpiGrid = new JPanel(new GridLayout(2, 3, Theme.SPACE_12, Theme.SPACE_8));
        kpiGrid.setOpaque(false);
        kpiGrid.add(productsCard);
        kpiGrid.add(ordersCard);
        kpiGrid.add(animalsCard);
        kpiGrid.add(adoptionsCard);
        kpiGrid.add(bookingsCard);
        kpiGrid.add(customersCard);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setOpaque(false);
        inlineErrorLabel.setAlignmentX(LEFT_ALIGNMENT);
        topStrip.setAlignmentX(LEFT_ALIGNMENT);
        kpiGrid.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(inlineErrorLabel);
        northStack.add(topStrip);
        northStack.add(Box.createVerticalStrut(Theme.SPACE_8));
        northStack.add(kpiGrid);

        // --- Tabbed Recent Activity Tables (latest 5 each) ---
        recentOrdersModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] { "Kode Pesanan", "Pelanggan", "Tanggal", "Total", "Status" },
            new Class<?>[] { String.class, String.class, String.class, BigDecimal.class, String.class }
        );
        JTable recentOrdersTable = UiKit.adminTable(recentOrdersModel);
        recentOrdersTable.getColumnModel().getColumn(3).setCellRenderer(new AdminWidgets.CurrencyCellRenderer());
        recentOrdersTable.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
        configureOrderColumns(recentOrdersTable);

        recentAdoptionsModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] { "ID", "Pemohon", "Hewan", "Tanggal", "Status" },
            new Class<?>[] { Integer.class, String.class, String.class, String.class, String.class }
        );
        JTable recentAdoptionsTable = UiKit.adminTable(recentAdoptionsModel);
        recentAdoptionsTable.getColumnModel().getColumn(0).setCellRenderer(new IdPillRenderer());
        recentAdoptionsTable.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
        configureAdoptionColumns(recentAdoptionsTable);

        recentBookingsModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] { "ID", "Pemilik / Hewan", "Layanan", "Jadwal", "Status" },
            new Class<?>[] { Integer.class, String.class, String.class, String.class, String.class }
        );
        JTable recentBookingsTable = UiKit.adminTable(recentBookingsModel);
        recentBookingsTable.getColumnModel().getColumn(0).setCellRenderer(new IdPillRenderer());
        recentBookingsTable.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
        configureBookingColumns(recentBookingsTable);

        recentTabs = new JTabbedPane();
        recentTabs.setFont(Theme.bodyBold());
        recentTabs.setBackground(Theme.Admin.SURFACE);
        recentTabs.setForeground(Theme.Admin.TEXT);
        recentTabs.addTab("Pesanan Terbaru", AdminWidgets.adminScrollPane(recentOrdersTable));
        recentTabs.addTab("Adopsi Terbaru", AdminWidgets.adminScrollPane(recentAdoptionsTable));
        recentTabs.addTab("Booking Terbaru", AdminWidgets.adminScrollPane(recentBookingsTable));

        JPanel recentHeader = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
        recentHeader.setOpaque(false);
        recentHeader.add(UiKit.label("Aktivitas Terbaru", Theme.heading(), Theme.Admin.TEXT), BorderLayout.WEST);
        recentHintLabel = UiKit.label("Menampilkan hingga 5 entri terbaru per modul.", Theme.small(), Theme.Admin.TEXT_SOFT);
        recentHeader.add(recentHintLabel, BorderLayout.EAST);

        JPanel recentCard = UiKit.adminCardPanel(new BorderLayout(0, Theme.SPACE_8));
        recentCard.setBorder(BorderFactory.createEmptyBorder(
            Theme.SPACE_12, Theme.SPACE_16, Theme.SPACE_12 + 2, Theme.SPACE_16 + 2
        ));
        recentCard.setPreferredSize(new Dimension(640, 275));
        recentCard.setMinimumSize(new Dimension(320, 260));
        recentCard.add(recentHeader, BorderLayout.NORTH);
        recentCard.add(recentTabs, BorderLayout.CENTER);

        JPanel dashboardBody = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        dashboardBody.setOpaque(false);
        dashboardBody.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        dashboardBody.add(northStack, BorderLayout.NORTH);
        dashboardBody.add(recentCard, BorderLayout.CENTER);

        add(AdminWidgets.verticalExpandingScrollPane(dashboardBody, Theme.Admin.BG), BorderLayout.CENTER);
    }

    @Override
    protected Color defaultBackground() {
        return Theme.Admin.BG;
    }

    private JPanel buildRevenueHeroCard() {
        JPanel card = UiKit.pastelCardPanel(
            new BorderLayout(Theme.SPACE_8, 4),
            Theme.Admin.NAVY,
            Theme.Admin.BLUE,
            Theme.Admin.CARD_RADIUS
        );
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));

        JPanel topRow = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
        topRow.setOpaque(false);
        topRow.add(UiKit.label("Total Pendapatan", Theme.smallBold(), Color.WHITE), BorderLayout.CENTER);
        JLabel iconLabel = new JLabel(VectorIcons.of(VectorIcons.Kind.ORDERS, 18, Theme.GOLD), SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(26, 26));
        topRow.add(iconLabel, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(revenueValueLabel, BorderLayout.CENTER);
        card.add(revenueSubtitleLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildQuickActionCard() {
        JPanel card = UiKit.adminCardPanel(new BorderLayout(0, Theme.SPACE_8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));

        JPanel header = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
        header.setOpaque(false);
        header.add(UiKit.label("Aksi Cepat", Theme.heading(), Theme.Admin.TEXT), BorderLayout.WEST);
        header.add(UiKit.label("Tambah data master", Theme.small(), Theme.Admin.TEXT_SOFT), BorderLayout.EAST);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setOpaque(false);
        buttons.add(quickAddProductButton);
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(quickAddAnimalButton);
        buttons.add(Box.createHorizontalStrut(6));
        buttons.add(quickAddServiceButton);

        JPanel buttonsWrap = new JPanel(new BorderLayout());
        buttonsWrap.setOpaque(false);
        buttonsWrap.add(buttons, BorderLayout.WEST);

        card.add(header, BorderLayout.NORTH);
        card.add(buttonsWrap, BorderLayout.CENTER);
        return card;
    }

    private static void configureOrderColumns(JTable table) {
        table.getColumnModel().getColumn(0).setPreferredWidth(145);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(145);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(115);
    }

    private static void configureAdoptionColumns(JTable table) {
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(175);
        table.getColumnModel().getColumn(2).setPreferredWidth(185);
        table.getColumnModel().getColumn(3).setPreferredWidth(145);
        table.getColumnModel().getColumn(4).setPreferredWidth(115);
    }

    private static void configureBookingColumns(JTable table) {
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(195);
        table.getColumnModel().getColumn(2).setPreferredWidth(185);
        table.getColumnModel().getColumn(3).setPreferredWidth(145);
        table.getColumnModel().getColumn(4).setPreferredWidth(115);
    }

    @Override
    public String getPageKey() {
        return "admin.dashboard";
    }

    @Override
    public String getPageTitle() {
        return "Dashboard Admin";
    }

    @Override
    public String getPageSubtitle() {
        return "Statistik operasional, pendapatan, aksi cepat, dan aktivitas terbaru";
    }

    @Override
    public void refreshData() {
        AdminWidgets.hideBanner(inlineErrorLabel);
        User user = app.getSession().getUser();
        if (user == null || !user.isAdmin()) {
            resetOverviewView();
            setQuickActionsEnabled(false);
            recentHintLabel.setText("Masuk sebagai admin untuk melihat statistik operasional.");
            statusSummary = "Sesi admin tidak aktif.";
            return;
        }

        setQuickActionsEnabled(true);
        try {
            AdminDashboardService.DashboardOverview overview = app.getAdminDashboardService().loadOverview();
            DashboardStats stats = overview.stats() != null ? overview.stats() : new DashboardStats();

            revenueValueLabel.setText(FormatUtil.rupiah(stats.getRevenue()));
            productsCard.setValue(String.valueOf(stats.getProductsCount()));
            ordersCard.setValue(String.valueOf(stats.getOrdersCount()));
            animalsCard.setValue(String.valueOf(stats.getAnimalsCount()));
            adoptionsCard.setValue(String.valueOf(stats.getAdoptionsCount()));
            bookingsCard.setValue(String.valueOf(stats.getBookingsCount()));
            customersCard.setValue(String.valueOf(stats.getUsersCount()));

            recentOrdersModel.setRowCount(0);
            for (Order order : overview.recentOrders()) {
                String code = (order.getOrderCode() != null && !order.getOrderCode().isBlank())
                    ? order.getOrderCode()
                    : "#" + order.getOrderId();
                String customer = (order.getUserName() != null && !order.getUserName().isBlank())
                    ? order.getUserName()
                    : AdminWidgets.safeText(order.getRecipientName());
                recentOrdersModel.addRow(new Object[] {
                    code,
                    customer,
                    FormatUtil.dateTimeId(order.getOrderDate()),
                    order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO,
                    AdminWidgets.safeText(order.getStatus())
                });
            }

            recentAdoptionsModel.setRowCount(0);
            for (AdoptionRequest req : overview.recentAdoptions()) {
                String applicant = (req.getApplicantName() != null && !req.getApplicantName().isBlank())
                    ? req.getApplicantName()
                    : AdminWidgets.safeText(req.getUserName());
                String animalName = (req.getAnimalNama() != null && !req.getAnimalNama().isBlank())
                    ? req.getAnimalNama()
                    : "Hewan #" + req.getAnimalId();
                String species = AdminWidgets.capitalizeWords(req.getAnimalSpecies());
                String animalDisplay = "-".equals(species) ? animalName : animalName + " (" + species + ")";
                recentAdoptionsModel.addRow(new Object[] {
                    Integer.valueOf(req.getAdoptionRequestId()),
                    applicant,
                    animalDisplay,
                    FormatUtil.dateTimeId(req.getRequestDate()),
                    AdminWidgets.safeText(req.getStatus())
                });
            }

            recentBookingsModel.setRowCount(0);
            for (HealthBooking booking : overview.recentBookings()) {
                String owner = (booking.getOwnerName() != null && !booking.getOwnerName().isBlank())
                    ? booking.getOwnerName()
                    : AdminWidgets.safeText(booking.getUserName());
                String pet = AdminWidgets.safeText(booking.getPetName());
                String ownerPet = "-".equals(pet) ? owner : owner + " / " + pet;
                recentBookingsModel.addRow(new Object[] {
                    Integer.valueOf(booking.getBookingId()),
                    ownerPet,
                    AdminWidgets.safeText(booking.getServiceName()),
                    FormatUtil.dateTimeId(booking.getBookingDate()),
                    AdminWidgets.safeText(booking.getStatus())
                });
            }

            recentHintLabel.setText(
                "Terbaru: " + overview.recentOrders().size() + " pesanan, "
                    + overview.recentAdoptions().size() + " adopsi, "
                    + overview.recentBookings().size() + " booking."
            );

            statusSummary = "Dashboard admin: "
                + FormatUtil.rupiah(stats.getRevenue()) + " pendapatan, "
                + stats.getProductsCount() + " produk, "
                + stats.getOrdersCount() + " pesanan, "
                + stats.getAnimalsCount() + " hewan, "
                + stats.getAdoptionsCount() + " adopsi, "
                + stats.getBookingsCount() + " booking, "
                + stats.getUsersCount() + " pelanggan.";
        } catch (RuntimeException ex) {
            resetOverviewView();
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineErrorLabel, "Gagal memuat ringkasan dashboard: " + msg, true);
            recentHintLabel.setText("Data aktivitas terbaru tidak tersedia saat koneksi bermasalah.");
            statusSummary = "Gagal memuat dashboard: " + msg;
        }
        revalidate();
        repaint();
    }

    @Override
    public String getStatusSummary() {
        return statusSummary;
    }

    private void runQuickAddProduct() {
        if (AdminProductsPanel.openProductDialog(app, null)) {
            refreshData();
            app.updateStatus(getStatusSummary());
        }
    }

    private void runQuickAddAnimal() {
        if (AdminAnimalsPanel.openAnimalDialog(app, null)) {
            refreshData();
            app.updateStatus(getStatusSummary());
        }
    }

    private void runQuickAddService() {
        if (AdminServicesPanel.openServiceDialog(app, null)) {
            refreshData();
            app.updateStatus(getStatusSummary());
        }
    }

    private void setQuickActionsEnabled(boolean enabled) {
        quickAddProductButton.setEnabled(enabled);
        quickAddAnimalButton.setEnabled(enabled);
        quickAddServiceButton.setEnabled(enabled);
    }

    private void resetOverviewView() {
        revenueValueLabel.setText(FormatUtil.rupiah(BigDecimal.ZERO));
        productsCard.setValue("0");
        ordersCard.setValue("0");
        animalsCard.setValue("0");
        adoptionsCard.setValue("0");
        bookingsCard.setValue("0");
        customersCard.setValue("0");
        recentOrdersModel.setRowCount(0);
        recentAdoptionsModel.setRowCount(0);
        recentBookingsModel.setRowCount(0);
    }
}
