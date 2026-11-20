package pawpaw.gui.admin;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.IdPillRenderer;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.StatusBadgeRenderer;
import pawpaw.gui.common.UiKit;
import pawpaw.gui.user.BookingCardDialog;
import pawpaw.model.HealthBooking;
import pawpaw.model.Payment;
import pawpaw.model.User;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Admin clinical operations workspace for managing health bookings, slot approvals/completions,
 * rejection reasons, synchronized payment status, appointment slips, and deletion.
 */
public final class AdminBookingsPanel extends PagePanel {

    private static final long serialVersionUID = 1L;
    private static final String[] STATUS_FILTERS = {
        "Semua", "Menunggu", "Disetujui", "Selesai", "Ditolak", "Dibatalkan"
    };
    private static final AdminWidgets.StatusOption[] EDITABLE_STATUS_OPTIONS = {
        new AdminWidgets.StatusOption("pending", "Menunggu"),
        new AdminWidgets.StatusOption("approved", "Disetujui"),
        new AdminWidgets.StatusOption("completed", "Selesai"),
        new AdminWidgets.StatusOption("rejected", "Ditolak")
    };
    private static final AdminWidgets.StatusOption[] CANCELLED_STATUS_OPTIONS = {
        new AdminWidgets.StatusOption("cancelled", "Dibatalkan")
    };

    private final transient AppWindow app;

    private final JLabel inlineFeedbackLabel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilterCombo;
    private final JLabel countLabel;
    private final JButton refreshButton;

    private final AdminWidgets.ReadOnlyTableModel bookingModel;
    private final JTable bookingTable;

    private final JLabel inspectorTitleLabel;
    private final JLabel inspectorScheduleLabel;
    private final AdminWidgets.StatusPill bookingStatusPill;
    private final JLabel ownerValueLabel;
    private final JLabel petValueLabel;
    private final JLabel serviceValueLabel;
    private final JLabel doctorValueLabel;
    private final JLabel priceValueLabel;
    private final JLabel paymentRefLabel;
    private final JLabel paymentMethodLabel;
    private final AdminWidgets.StatusPill paymentStatusPill;

    private final JTextField rejectionReasonField;
    private final JButton cardButton;
    private final JButton deleteButton;
    private final JLabel terminalNoteLabel;
    private JPanel terminalNoteRow;
    private final JComboBox<AdminWidgets.StatusOption> statusEditCombo;
    private final JButton saveStatusButton;

    private transient List<HealthBooking> allBookings = new ArrayList<>();
    private transient List<HealthBooking> filteredBookings = new ArrayList<>();
    private transient HealthBooking selectedBooking;
    private String statusSummary = "Booking kesehatan siap.";

    public AdminBookingsPanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineFeedbackLabel = AdminWidgets.createFeedbackBanner();

        searchField = UiKit.searchField("Cari pemilik, hewan, layanan, atau dokter...");
        searchField.setPreferredSize(new Dimension(225, 36));
        AdminWidgets.wireTextChange(searchField, this::applyFilter);

        statusFilterCombo = AdminWidgets.adminComboBox(STATUS_FILTERS);
        statusFilterCombo.addActionListener(e -> applyFilter());

        countLabel = UiKit.label("0 booking", Theme.smallBold(), Theme.Admin.TEXT_SOFT);

        refreshButton = UiKit.adminLightButton("Refresh", VectorIcons.Kind.REFRESH);
        refreshButton.addActionListener(e -> {
            refreshData();
            app.updateStatus(getStatusSummary());
        });

        JPanel toolbar = AdminWidgets.buildTransactionToolbar(
            searchField, statusFilterCombo, countLabel, refreshButton
        );
        JPanel northStack = AdminWidgets.buildNorthStack(inlineFeedbackLabel, toolbar);

        bookingModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] {
                "ID", "Jadwal", "Layanan", "Dokter",
                "Pemilik", "Hewan", "Status Booking", "Pembayaran"
            },
            new Class<?>[] {
                Integer.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class
            }
        );
        bookingTable = UiKit.adminTable(bookingModel);
        bookingTable.getColumnModel().getColumn(0).setCellRenderer(new IdPillRenderer());
        bookingTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        bookingTable.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer());
        configureColumns();
        AdminWidgets.wireTableSelection(bookingTable, this::onSelectionChanged);
        AdminWidgets.attachDoubleClick(bookingTable, this::openBookingCardDialog);

        // --- Bottom Inspector ---
        inspectorTitleLabel = UiKit.label("Pilih jadwal booking pada tabel", Theme.heading(), Theme.Admin.TEXT);
        inspectorScheduleLabel = UiKit.label("-", Theme.small(), Theme.Admin.TEXT_SOFT);
        bookingStatusPill = new AdminWidgets.StatusPill();

        ownerValueLabel = UiKit.label("-", Theme.bodyBold(), Theme.Admin.TEXT);
        petValueLabel = UiKit.label("-", Theme.body(), Theme.Admin.TEXT);
        serviceValueLabel = UiKit.label("-", Theme.bodyBold(), Theme.Admin.TEXT);
        doctorValueLabel = UiKit.label("-", Theme.body(), Theme.Admin.TEXT);

        priceValueLabel = UiKit.label(FormatUtil.rupiah(BigDecimal.ZERO), Theme.bodyBold(), Theme.Admin.BLUE);
        paymentRefLabel = UiKit.label("-", Theme.mono(), Theme.Admin.TEXT);
        paymentMethodLabel = UiKit.label("-", Theme.body(), Theme.Admin.TEXT);
        paymentStatusPill = new AdminWidgets.StatusPill();

        rejectionReasonField = UiKit.textField(
            "Alasan penolakan (opsional saat menolak; default: Jadwal penuh / tidak tersedia)"
        );
        rejectionReasonField.setEnabled(false);

        cardButton = UiKit.adminLightButton("Kartu Booking", VectorIcons.Kind.BOOKINGS);
        cardButton.setEnabled(false);
        cardButton.addActionListener(e -> openBookingCardDialog());

        deleteButton = UiKit.adminDeleteButton("Hapus Booking", VectorIcons.Kind.TRASH);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedBooking());

        terminalNoteLabel = UiKit.label("Status final terkunci", Theme.smallBold(), Theme.Admin.TEXT_SOFT);
        terminalNoteLabel.setVisible(false);

        statusEditCombo = AdminWidgets.adminComboBox(EDITABLE_STATUS_OPTIONS);
        statusEditCombo.setEnabled(false);

        saveStatusButton = UiKit.adminPrimaryButton("Simpan Status", VectorIcons.Kind.CHECK);
        saveStatusButton.setEnabled(false);
        saveStatusButton.addActionListener(e -> saveSelectedStatus());

        JPanel inspectorBody = buildInspectorContent();
        JSplitPane splitPane = AdminWidgets.buildMasterDetailSplit(bookingTable, inspectorBody);

        add(northStack, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    @Override
    protected Color defaultBackground() {
        return Theme.Admin.BG;
    }

    private JPanel buildInspectorContent() {
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(Theme.Admin.SURFACE);
        stack.setOpaque(true);
        stack.setBorder(BorderFactory.createEmptyBorder(
            Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16
        ));

        JPanel headerRow = new AdminWidgets.CompactSectionPanel(new BorderLayout(Theme.SPACE_12, 0));
        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);
        inspectorTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        inspectorScheduleLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleStack.add(inspectorTitleLabel);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(inspectorScheduleLabel);

        JPanel pillWrap = new JPanel(new GridBagLayout());
        pillWrap.setOpaque(false);
        pillWrap.add(bookingStatusPill);

        headerRow.add(titleStack, BorderLayout.CENTER);
        headerRow.add(pillWrap, BorderLayout.EAST);

        JPanel infoGrid = new AdminWidgets.CompactSectionPanel(new GridLayout(1, 2, Theme.SPACE_12, 0));

        JPanel patientCard = AdminWidgets.createInspectorSubCard(new GridBagLayout());
        int pRow = 0;
        AdminWidgets.addAttrRow(patientCard, pRow++, "Pemilik", ownerValueLabel);
        AdminWidgets.addAttrRow(patientCard, pRow++, "Hewan", petValueLabel);
        AdminWidgets.addAttrRow(patientCard, pRow++, "Layanan", serviceValueLabel);
        AdminWidgets.addAttrRow(patientCard, pRow, "Dokter", doctorValueLabel);

        JPanel paymentCard = AdminWidgets.createInspectorSubCard(new GridBagLayout());
        JPanel payPillHost = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        payPillHost.setOpaque(false);
        payPillHost.add(paymentStatusPill);
        int bRow = 0;
        AdminWidgets.addAttrRow(paymentCard, bRow++, "Biaya Layanan", priceValueLabel);
        AdminWidgets.addAttrRow(paymentCard, bRow++, "Referensi Bayar", paymentRefLabel);
        AdminWidgets.addAttrRow(paymentCard, bRow++, "Metode Bayar", paymentMethodLabel);
        AdminWidgets.addAttrRow(paymentCard, bRow, "Pembayaran", payPillHost);

        infoGrid.add(patientCard);
        infoGrid.add(paymentCard);

        JPanel reasonRow = new AdminWidgets.CompactSectionPanel(new BorderLayout(0, 4));
        reasonRow.add(AdminWidgets.adminFormLabel("Alasan Penolakan"), BorderLayout.NORTH);
        reasonRow.add(rejectionReasonField, BorderLayout.CENTER);

        terminalNoteRow = AdminWidgets.createNoteRow(terminalNoteLabel);

        JPanel statusControls = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0));
        statusControls.setOpaque(false);
        statusControls.add(AdminWidgets.adminFormLabel("Status:"));
        statusControls.add(statusEditCombo);
        statusControls.add(saveStatusButton);
        JPanel statusRow = new AdminWidgets.CompactSectionPanel(new BorderLayout());
        statusRow.add(statusControls, BorderLayout.WEST);

        JPanel utilityControls = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0));
        utilityControls.setOpaque(false);
        utilityControls.add(cardButton);
        utilityControls.add(deleteButton);
        JPanel utilityRow = new AdminWidgets.CompactSectionPanel(new BorderLayout());
        utilityRow.add(utilityControls, BorderLayout.WEST);

        headerRow.setAlignmentX(LEFT_ALIGNMENT);
        infoGrid.setAlignmentX(LEFT_ALIGNMENT);
        reasonRow.setAlignmentX(LEFT_ALIGNMENT);
        terminalNoteRow.setAlignmentX(LEFT_ALIGNMENT);
        statusRow.setAlignmentX(LEFT_ALIGNMENT);
        utilityRow.setAlignmentX(LEFT_ALIGNMENT);

        stack.add(headerRow);
        stack.add(Box.createVerticalStrut(Theme.SPACE_12));
        stack.add(infoGrid);
        stack.add(Box.createVerticalStrut(Theme.SPACE_12));
        stack.add(reasonRow);
        stack.add(Box.createVerticalStrut(Theme.SPACE_12));
        stack.add(terminalNoteRow);
        stack.add(statusRow);
        stack.add(Box.createVerticalStrut(Theme.SPACE_8));
        stack.add(utilityRow);
        return stack;
    }

    private void configureColumns() {
        bookingTable.getColumnModel().getColumn(0).setPreferredWidth(58);
        bookingTable.getColumnModel().getColumn(1).setPreferredWidth(132);
        bookingTable.getColumnModel().getColumn(2).setPreferredWidth(155);
        bookingTable.getColumnModel().getColumn(3).setPreferredWidth(135);
        bookingTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        bookingTable.getColumnModel().getColumn(5).setPreferredWidth(125);
        bookingTable.getColumnModel().getColumn(6).setPreferredWidth(115);
        bookingTable.getColumnModel().getColumn(7).setPreferredWidth(105);
    }

    @Override
    public String getPageKey() {
        return "admin.bookings";
    }

    @Override
    public String getPageTitle() {
        return "Booking Kesehatan";
    }

    @Override
    public String getPageSubtitle() {
        return "Kelola jadwal pemeriksaan pelanggan, status booking, dan pembayaran";
    }

    @Override
    public void refreshData() {
        int prevId = selectedBooking != null ? selectedBooking.getBookingId() : -1;
        AdminWidgets.hideBanner(inlineFeedbackLabel);

        if (!isAdminActive()) {
            allBookings = new ArrayList<>();
            filteredBookings = new ArrayList<>();
            bookingModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 booking");
            statusSummary = "Sesi admin tidak aktif.";
            return;
        }

        try {
            allBookings = new ArrayList<>(app.getHealthBookingService().listAllBookings());
            applyFilter(prevId);
            statusSummary = "Booking kesehatan dimuat: " + allBookings.size() + " jadwal.";
        } catch (RuntimeException ex) {
            allBookings = new ArrayList<>();
            filteredBookings = new ArrayList<>();
            bookingModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 booking");
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineFeedbackLabel, "Gagal memuat booking kesehatan: " + msg, true);
            statusSummary = "Gagal memuat booking kesehatan: " + msg;
        }
    }

    @Override
    public String getStatusSummary() {
        return statusSummary;
    }

    private boolean isAdminActive() {
        User user = app.getSession().getUser();
        return user != null && user.isAdmin();
    }

    private void applyFilter() {
        int currentId = selectedBooking != null ? selectedBooking.getBookingId() : -1;
        applyFilter(currentId);
    }

    private void applyFilter(int preferredBookingId) {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Object statusObj = statusFilterCombo.getSelectedItem();

        filteredBookings = new ArrayList<>();
        bookingModel.setRowCount(0);

        for (HealthBooking booking : allBookings) {
            if (!AdminWidgets.matchesStatusFilter(booking.getStatus(), statusObj) || !matchesQuery(booking, query)) {
                continue;
            }
            filteredBookings.add(booking);

            String serviceName = formatServiceName(booking);
            String owner = formatOwnerName(booking);
            String petSummary = formatPetSummary(booking);
            String paymentStatus = booking.getPayment() != null
                ? AdminWidgets.nonNull(booking.getPayment().getPaymentStatus())
                : "";

            bookingModel.addRow(new Object[] {
                Integer.valueOf(booking.getBookingId()),
                FormatUtil.dateTimeId(booking.getBookingDate()),
                serviceName,
                AdminWidgets.safeText(booking.getDoctorName()),
                owner,
                petSummary,
                AdminWidgets.safeText(booking.getStatus()),
                paymentStatus
            });
        }

        countLabel.setText(filteredBookings.size() + " / " + allBookings.size() + " booking");
        AdminWidgets.restoreTableSelection(
            bookingTable,
            filteredBookings,
            HealthBooking::getBookingId,
            preferredBookingId,
            this::onSelectionChanged,
            this::clearSelection
        );
    }

    private void onSelectionChanged() {
        HealthBooking booking = AdminWidgets.resolveSelectedItem(bookingTable, filteredBookings);
        if (booking == null) {
            clearSelection();
            return;
        }
        selectedBooking = booking;

        String serviceName = formatServiceName(booking);
        inspectorTitleLabel.setText("Booking #" + booking.getBookingId() + " — " + serviceName);
        inspectorScheduleLabel.setText("Jadwal Kunjungan: " + FormatUtil.dateTimeId(booking.getBookingDate()));
        bookingStatusPill.setStatus(booking.getStatus());

        ownerValueLabel.setText(formatOwnerName(booking));
        petValueLabel.setText(formatPetSummary(booking));
        serviceValueLabel.setText(serviceName);
        doctorValueLabel.setText(AdminWidgets.safeText(booking.getDoctorName()));

        BigDecimal fee = booking.getServicePrice() != null ? booking.getServicePrice() : BigDecimal.ZERO;
        Payment payment = booking.getPayment();
        BigDecimal displayAmount = (payment != null && payment.getAmount() != null) ? payment.getAmount() : fee;
        priceValueLabel.setText(FormatUtil.rupiah(displayAmount));
        paymentRefLabel.setText(payment != null ? AdminWidgets.safeText(payment.getReferenceNumber()) : "-");
        paymentMethodLabel.setText(payment != null ? AdminWidgets.safeText(payment.getPaymentMethod()) : "-");
        paymentStatusPill.setStatus(payment != null ? payment.getPaymentStatus() : "");

        rejectionReasonField.setText(AdminWidgets.nonNull(booking.getRejectionReason()));
        if (isCancelledBookingStatus(booking.getStatus())) {
            statusEditCombo.setModel(new DefaultComboBoxModel<>(CANCELLED_STATUS_OPTIONS));
            statusEditCombo.setSelectedIndex(0);
        } else {
            statusEditCombo.setModel(new DefaultComboBoxModel<>(EDITABLE_STATUS_OPTIONS));
            AdminWidgets.selectStatusOption(statusEditCombo, booking.getStatus());
        }

        boolean adminActive = isAdminActive();
        boolean terminal = isTerminalBookingStatus(booking.getStatus());
        cardButton.setEnabled(adminActive);
        deleteButton.setEnabled(adminActive);
        rejectionReasonField.setEnabled(adminActive && !terminal);
        statusEditCombo.setEnabled(adminActive && !terminal);
        saveStatusButton.setEnabled(adminActive && !terminal);
        AdminWidgets.setNoteRowVisible(terminalNoteRow, terminalNoteLabel, terminal, "Status final terkunci");
    }

    private void clearSelection() {
        selectedBooking = null;
        inspectorTitleLabel.setText("Pilih jadwal booking pada tabel");
        inspectorScheduleLabel.setText("-");
        bookingStatusPill.setStatus("");
        ownerValueLabel.setText("-");
        petValueLabel.setText("-");
        serviceValueLabel.setText("-");
        doctorValueLabel.setText("-");
        priceValueLabel.setText(FormatUtil.rupiah(BigDecimal.ZERO));
        paymentRefLabel.setText("-");
        paymentMethodLabel.setText("-");
        paymentStatusPill.setStatus("");
        rejectionReasonField.setText("");
        rejectionReasonField.setEnabled(false);
        cardButton.setEnabled(false);
        deleteButton.setEnabled(false);
        statusEditCombo.setModel(new DefaultComboBoxModel<>(EDITABLE_STATUS_OPTIONS));
        statusEditCombo.setSelectedIndex(0);
        statusEditCombo.setEnabled(false);
        saveStatusButton.setEnabled(false);
        AdminWidgets.setNoteRowVisible(terminalNoteRow, terminalNoteLabel, false, null);
    }

    private void openBookingCardDialog() {
        HealthBooking target = AdminWidgets.resolveSelectedItem(bookingTable, filteredBookings);
        if (!isAdminActive() || target == null) {
            return;
        }
        BookingCardDialog.show(app, target);
    }

    private void saveSelectedStatus() {
        AdminWidgets.hideBanner(inlineFeedbackLabel);
        HealthBooking target = AdminWidgets.resolveSelectedItem(bookingTable, filteredBookings);
        if (!isAdminActive() || target == null) {
            return;
        }
        if (isTerminalBookingStatus(target.getStatus())) {
            AdminWidgets.showBanner(
                inlineFeedbackLabel,
                "Booking berstatus final (" + FormatUtil.statusLabel(target.getStatus()) + ") sudah terkunci.",
                true
            );
            return;
        }
        Object item = statusEditCombo.getSelectedItem();
        if (!(item instanceof AdminWidgets.StatusOption option)) {
            return;
        }

        int bookingId = target.getBookingId();
        String targetCode = option.code();
        String reason = rejectionReasonField.getText();
        try {
            app.getHealthBookingService().updateBookingStatus(bookingId, targetCode, reason);
            refreshData();
            AdminWidgets.restoreTableSelection(
                bookingTable,
                filteredBookings,
                HealthBooking::getBookingId,
                bookingId,
                this::onSelectionChanged,
                this::clearSelection
            );
            String msg = "Status booking #" + bookingId + " diperbarui menjadi "
                + FormatUtil.statusLabel(targetCode) + ".";
            AdminWidgets.showBanner(inlineFeedbackLabel, msg, false);
            statusSummary = msg;
            app.updateStatus(msg);
        } catch (RuntimeException ex) {
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineFeedbackLabel, "Gagal memperbarui booking: " + msg, true);
            UiKit.showError(this, msg);
            app.updateStatus("Gagal memperbarui booking: " + msg);
        }
    }

    private void deleteSelectedBooking() {
        AdminWidgets.hideBanner(inlineFeedbackLabel);
        HealthBooking target = AdminWidgets.resolveSelectedItem(bookingTable, filteredBookings);
        if (!isAdminActive() || target == null) {
            return;
        }
        String serviceName = formatServiceName(target);
        boolean confirmed = UiKit.confirm(
            this,
            "Hapus Booking Kesehatan",
            "Yakin ingin menghapus booking #" + target.getBookingId() + " (" + serviceName + ")?"
        );
        if (!confirmed) {
            return;
        }

        int bookingId = target.getBookingId();
        try {
            app.getHealthBookingService().deleteBooking(bookingId);
            refreshData();
            String msg = "Booking #" + bookingId + " (" + serviceName + ") berhasil dihapus.";
            AdminWidgets.showBanner(inlineFeedbackLabel, msg, false);
            statusSummary = msg;
            app.updateStatus(msg);
        } catch (RuntimeException ex) {
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineFeedbackLabel, "Gagal menghapus booking: " + msg, true);
            UiKit.showError(this, msg);
            app.updateStatus("Gagal menghapus booking: " + msg);
        }
    }

    private static boolean isTerminalBookingStatus(String status) {
        if (status == null) {
            return false;
        }
        String clean = status.trim().toLowerCase(Locale.ROOT);
        return "completed".equals(clean) || "cancelled".equals(clean);
    }

    private static boolean isCancelledBookingStatus(String status) {
        return status != null && "cancelled".equalsIgnoreCase(status.trim());
    }

    private static String formatServiceName(HealthBooking booking) {
        return (booking.getServiceName() != null && !booking.getServiceName().isBlank())
            ? booking.getServiceName()
            : "Layanan #" + booking.getServiceId();
    }

    private static String formatOwnerName(HealthBooking booking) {
        return (booking.getOwnerName() != null && !booking.getOwnerName().isBlank())
            ? booking.getOwnerName()
            : AdminWidgets.safeText(booking.getUserName());
    }

    private static String formatPetSummary(HealthBooking booking) {
        String petName = AdminWidgets.safeText(booking.getPetName());
        String petSpecies = AdminWidgets.safeText(booking.getPetSpecies());
        return "-".equals(petSpecies) ? petName : petName + " (" + petSpecies + ")";
    }

    private static boolean matchesQuery(HealthBooking booking, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String ref = booking.getPayment() != null ? booking.getPayment().getReferenceNumber() : null;
        return AdminWidgets.containsIgnoreCase(booking.getOwnerName(), query)
            || AdminWidgets.containsIgnoreCase(booking.getUserName(), query)
            || AdminWidgets.containsIgnoreCase(booking.getPetName(), query)
            || AdminWidgets.containsIgnoreCase(booking.getPetSpecies(), query)
            || AdminWidgets.containsIgnoreCase(booking.getServiceName(), query)
            || AdminWidgets.containsIgnoreCase(booking.getDoctorName(), query)
            || AdminWidgets.containsIgnoreCase(ref, query);
    }
}
