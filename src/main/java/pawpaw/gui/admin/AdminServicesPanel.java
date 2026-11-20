package pawpaw.gui.admin;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.IdPillRenderer;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.UiKit;
import pawpaw.model.HealthService;
import pawpaw.model.User;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Admin master workspace for clinic health services: search filter, dense sortable table,
 * clean vector-icon detail strip (never rendering raw DB emoji icons), modal create/edit dialog,
 * and FK-guarded deletion.
 */
public final class AdminServicesPanel extends PagePanel {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_DOCTOR = "drh. Clara Wijaya";
    private static final String DEFAULT_DAYS = "Senin - Sabtu";
    private static final String DEFAULT_SLOTS = "09:00, 13:00, 16:00";
    private static final ColorOption[] CARD_COLOR_OPTIONS = {
        new ColorOption("card-green", "Hijau Pastel"),
        new ColorOption("card-blue", "Biru Pastel"),
        new ColorOption("card-purple", "Ungu Pastel"),
        new ColorOption("card-pink", "Merah Muda Pastel"),
        new ColorOption("card-orange", "Oranye Pastel")
    };

    private record ColorOption(String code, String label) implements java.io.Serializable {
        @Override
        public String toString() {
            return label;
        }
    }

    private final transient AppWindow app;

    private final JLabel inlineErrorLabel;
    private final JTextField searchField;
    private final JLabel countLabel;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JButton refreshButton;

    private final AdminWidgets.ReadOnlyTableModel serviceModel;
    private final JTable serviceTable;

    private final JLabel previewTitleLabel;
    private final JLabel previewMetaLabel;
    private final JTextArea previewDescArea;

    private transient List<HealthService> allServices = new ArrayList<>();
    private transient List<HealthService> filteredServices = new ArrayList<>();
    private transient HealthService selectedService;
    private String statusSummary = "Manajemen layanan kesehatan siap.";

    public AdminServicesPanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineErrorLabel = AdminWidgets.createFeedbackBanner();

        searchField = UiKit.searchField("Cari layanan, dokter, hari praktik, atau deskripsi...");
        searchField.setPreferredSize(new Dimension(270, 36));
        AdminWidgets.wireTextChange(searchField, this::applyFilter);

        countLabel = UiKit.label("0 layanan", Theme.smallBold(), Theme.Admin.TEXT_SOFT);

        addButton = UiKit.adminPrimaryButton("+ Tambah Layanan", VectorIcons.Kind.PLUS);
        addButton.addActionListener(e -> openCreateDialog());

        editButton = UiKit.adminEditButton("Edit", VectorIcons.Kind.EDIT);
        editButton.setEnabled(false);
        editButton.addActionListener(e -> openEditDialog());

        deleteButton = UiKit.adminDeleteButton("Hapus", VectorIcons.Kind.TRASH);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedService());

        refreshButton = UiKit.adminLightButton("Refresh", VectorIcons.Kind.REFRESH);
        refreshButton.addActionListener(e -> {
            refreshData();
            app.updateStatus(getStatusSummary());
        });

        JPanel toolbar = AdminWidgets.buildMasterToolbar(
            searchField, countLabel, addButton, editButton, deleteButton, refreshButton
        );

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setOpaque(false);
        inlineErrorLabel.setAlignmentX(LEFT_ALIGNMENT);
        toolbar.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(inlineErrorLabel);
        northStack.add(toolbar);

        serviceModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] { "ID", "Layanan", "Dokter", "Hari Praktik", "Slot Jam", "Kuota/Sesi", "Harga" },
            new Class<?>[] {
                Integer.class, String.class, String.class, String.class, String.class, Integer.class, BigDecimal.class
            }
        );
        serviceTable = UiKit.adminTable(serviceModel);
        serviceTable.getColumnModel().getColumn(0).setCellRenderer(new IdPillRenderer());
        serviceTable.getColumnModel().getColumn(6).setCellRenderer(new AdminWidgets.CurrencyCellRenderer());
        configureColumns();
        serviceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });
        if (serviceTable.getRowSorter() != null) {
            serviceTable.getRowSorter().addRowSorterListener(e -> onSelectionChanged());
        }
        AdminWidgets.attachDoubleClick(serviceTable, this::openEditDialog);

        previewTitleLabel = UiKit.label("Pilih layanan pada tabel", Theme.bodyBold(), Theme.Admin.TEXT);
        previewMetaLabel = UiKit.label("-", Theme.small(), Theme.Admin.TEXT_SOFT);
        previewDescArea = AdminWidgets.createReadOnlyTextArea(2);

        JPanel previewCard = buildPreviewCard();

        JPanel centerHost = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        centerHost.setOpaque(false);
        centerHost.add(AdminWidgets.adminTableCard(serviceTable), BorderLayout.CENTER);
        centerHost.add(previewCard, BorderLayout.SOUTH);

        add(northStack, BorderLayout.NORTH);
        add(centerHost, BorderLayout.CENTER);
    }

    @Override
    protected Color defaultBackground() {
        return Theme.Admin.BG;
    }

    private JPanel buildPreviewCard() {
        JPanel card = UiKit.adminCardPanel(new BorderLayout(Theme.SPACE_12, 0));

        JLabel iconBox = new JLabel(VectorIcons.of(VectorIcons.Kind.HEALTH, 24, Theme.Admin.BLUE), SwingConstants.CENTER) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.Admin.BLUE_SOFT);
                    g2.fill(new RoundRectangle2D.Double(
                        0, 0, getWidth(), getHeight(), Theme.Admin.BUTTON_RADIUS, Theme.Admin.BUTTON_RADIUS
                    ));
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(56, 56));

        JPanel textTop = new JPanel();
        textTop.setLayout(new BoxLayout(textTop, BoxLayout.Y_AXIS));
        textTop.setOpaque(false);
        previewTitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        previewMetaLabel.setAlignmentX(LEFT_ALIGNMENT);
        textTop.add(previewTitleLabel);
        textTop.add(Box.createVerticalStrut(2));
        textTop.add(previewMetaLabel);

        JScrollPane descScroll = AdminWidgets.adminScrollPane(previewDescArea);
        descScroll.setPreferredSize(new Dimension(260, 48));

        JPanel infoCol = new JPanel(new BorderLayout(0, 6));
        infoCol.setOpaque(false);
        infoCol.add(textTop, BorderLayout.NORTH);
        infoCol.add(descScroll, BorderLayout.CENTER);

        card.add(iconBox, BorderLayout.WEST);
        card.add(infoCol, BorderLayout.CENTER);
        return card;
    }

    private void configureColumns() {
        serviceTable.getColumnModel().getColumn(0).setPreferredWidth(48);
        serviceTable.getColumnModel().getColumn(1).setPreferredWidth(155);
        serviceTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        serviceTable.getColumnModel().getColumn(3).setPreferredWidth(105);
        serviceTable.getColumnModel().getColumn(4).setPreferredWidth(145);
        serviceTable.getColumnModel().getColumn(5).setPreferredWidth(72);
        serviceTable.getColumnModel().getColumn(6).setPreferredWidth(105);
    }

    public static boolean openServiceDialog(AppWindow app, HealthService existing) {
        if (app == null || !app.getSession().isAdmin()) {
            return false;
        }
        ServiceFormDialog dialog = new ServiceFormDialog(app, existing);
        dialog.setVisible(true);
        return dialog.isSaved();
    }

    @Override
    public String getPageKey() {
        return "admin.services";
    }

    @Override
    public String getPageTitle() {
        return "Layanan Kesehatan";
    }

    @Override
    public String getPageSubtitle() {
        return "Kelola jenis layanan klinik, dokter praktik, jam sesi, dan kuota harian";
    }

    @Override
    public void refreshData() {
        int prevId = selectedService != null ? selectedService.getServiceId() : -1;
        AdminWidgets.hideBanner(inlineErrorLabel);

        if (!isAdminActive()) {
            allServices = new ArrayList<>();
            filteredServices = new ArrayList<>();
            serviceModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 layanan");
            addButton.setEnabled(false);
            statusSummary = "Sesi admin tidak aktif.";
            return;
        }

        addButton.setEnabled(true);
        try {
            allServices = new ArrayList<>(app.getHealthBookingService().listServices());
            applyFilter(prevId);
            statusSummary = "Daftar layanan klinik dimuat: " + allServices.size() + " layanan.";
        } catch (RuntimeException ex) {
            allServices = new ArrayList<>();
            filteredServices = new ArrayList<>();
            serviceModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 layanan");
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineErrorLabel, "Gagal memuat layanan kesehatan: " + msg, true);
            statusSummary = "Gagal memuat layanan kesehatan: " + msg;
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
        int currentId = selectedService != null ? selectedService.getServiceId() : -1;
        applyFilter(currentId);
    }

    private void applyFilter(int preferredServiceId) {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase(Locale.ROOT);

        filteredServices = new ArrayList<>();
        serviceModel.setRowCount(0);

        for (HealthService service : allServices) {
            if (!matchesQuery(service, query)) {
                continue;
            }
            filteredServices.add(service);
            serviceModel.addRow(new Object[] {
                Integer.valueOf(service.getServiceId()),
                AdminWidgets.safeText(service.getServiceName()),
                AdminWidgets.safeText(service.getDoctorName()),
                AdminWidgets.safeText(service.getPracticeDays()),
                AdminWidgets.safeText(service.getTimeSlots()),
                Integer.valueOf(Math.max(1, service.getDailyQuota())),
                service.getPrice() != null ? service.getPrice() : BigDecimal.ZERO
            });
        }

        countLabel.setText(filteredServices.size() + " / " + allServices.size() + " layanan");
        restoreSelection(preferredServiceId);
    }

    private void restoreSelection(int preferredServiceId) {
        if (filteredServices.isEmpty()) {
            clearSelection();
            return;
        }
        int targetModelRow = 0;
        if (preferredServiceId > 0) {
            for (int i = 0; i < filteredServices.size(); i++) {
                if (filteredServices.get(i).getServiceId() == preferredServiceId) {
                    targetModelRow = i;
                    break;
                }
            }
        }
        int viewRow = serviceTable.convertRowIndexToView(targetModelRow);
        if (viewRow >= 0 && viewRow < serviceTable.getRowCount()) {
            serviceTable.setRowSelectionInterval(viewRow, viewRow);
            onSelectionChanged();
        } else {
            clearSelection();
        }
    }

    private HealthService resolveSelectedService() {
        int viewRow = serviceTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= serviceTable.getRowCount()) {
            return null;
        }
        int modelRow = serviceTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= filteredServices.size()) {
            return null;
        }
        return filteredServices.get(modelRow);
    }

    private void onSelectionChanged() {
        HealthService service = resolveSelectedService();
        if (service == null) {
            clearSelection();
            return;
        }
        selectedService = service;

        previewTitleLabel.setText(
            "#" + service.getServiceId() + " — " + AdminWidgets.safeText(service.getServiceName())
        );
        previewMetaLabel.setText(
            AdminWidgets.safeText(service.getDoctorName())
                + "  |  " + AdminWidgets.safeText(service.getPracticeDays())
                + "  |  Slot: " + AdminWidgets.safeText(service.getTimeSlots())
                + "  |  Kuota/sesi: " + Math.max(1, service.getDailyQuota())
                + "  |  " + FormatUtil.rupiah(service.getPrice())
        );
        String desc = (service.getDescription() == null || service.getDescription().isBlank())
            ? "Belum ada deskripsi layanan."
            : service.getDescription();
        previewDescArea.setText(desc);
        previewDescArea.setCaretPosition(0);

        boolean enabled = isAdminActive();
        editButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    private void clearSelection() {
        selectedService = null;
        previewTitleLabel.setText("Pilih layanan pada tabel");
        previewMetaLabel.setText("-");
        previewDescArea.setText("");
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void openCreateDialog() {
        if (!isAdminActive()) {
            return;
        }
        if (openServiceDialog(app, null)) {
            refreshData();
            app.updateStatus("Layanan kesehatan baru berhasil ditambahkan.");
        }
    }

    private void openEditDialog() {
        HealthService target = resolveSelectedService();
        if (!isAdminActive() || target == null) {
            return;
        }
        int targetId = target.getServiceId();
        if (openServiceDialog(app, target)) {
            refreshData();
            restoreSelection(targetId);
            app.updateStatus("Data layanan kesehatan #" + targetId + " berhasil diperbarui.");
        }
    }

    private void deleteSelectedService() {
        HealthService target = resolveSelectedService();
        if (!isAdminActive() || target == null) {
            return;
        }
        boolean confirmed = UiKit.confirm(
            this,
            "Hapus Layanan Kesehatan",
            "Yakin ingin menghapus layanan \"" + AdminWidgets.safeText(target.getServiceName()) + "\"?"
        );
        if (!confirmed) {
            return;
        }
        try {
            app.getHealthBookingService().deleteService(target.getServiceId());
            refreshData();
            statusSummary = "Layanan \"" + AdminWidgets.safeText(target.getServiceName()) + "\" berhasil dihapus.";
            app.updateStatus(statusSummary);
        } catch (RuntimeException ex) {
            String msg = AdminWidgets.safeErrorMessage(ex);
            UiKit.showError(this, msg);
            app.updateStatus("Gagal menghapus layanan: " + msg);
        }
    }

    private static boolean matchesQuery(HealthService service, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return AdminWidgets.containsIgnoreCase(service.getServiceName(), query)
            || AdminWidgets.containsIgnoreCase(service.getDoctorName(), query)
            || AdminWidgets.containsIgnoreCase(service.getPracticeDays(), query)
            || AdminWidgets.containsIgnoreCase(service.getTimeSlots(), query)
            || AdminWidgets.containsIgnoreCase(service.getDescription(), query);
    }

    /**
     * Modal Create/Edit dialog for {@link HealthService} master records with slot CSV input,
     * width-tracking scrollable form body, and inline validation.
     */
    public static final class ServiceFormDialog extends JDialog {

        private static final long serialVersionUID = 1L;

        private final transient AppWindow app;
        private final transient HealthService existing;

        private final JTextField serviceNameField;
        private final JTextField doctorNameField;
        private final JTextField practiceDaysField;
        private final JTextField timeSlotsField;
        private final JTextField dailyQuotaField;
        private final JTextField priceField;
        private final JComboBox<ColorOption> cardColorCombo;
        private final JTextArea descriptionArea;
        private final JLabel inlineErrorLabel;

        private boolean saved;

        public ServiceFormDialog(AppWindow app, HealthService existing) {
            super(
                app,
                existing == null ? "Tambah Layanan Kesehatan" : "Edit Layanan Kesehatan",
                ModalityType.APPLICATION_MODAL
            );
            this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");
            this.existing = existing;

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setResizable(true);

            serviceNameField = UiKit.textField("Contoh: Pemeriksaan Umum");
            doctorNameField = UiKit.textField(DEFAULT_DOCTOR);
            practiceDaysField = UiKit.textField(DEFAULT_DAYS);
            timeSlotsField = UiKit.textField("09:00, 13:00, 16:00");
            dailyQuotaField = UiKit.textField("Contoh: 5");
            priceField = UiKit.textField("Contoh: 100000");
            cardColorCombo = AdminWidgets.adminComboBox(CARD_COLOR_OPTIONS);
            descriptionArea = AdminWidgets.createEditableTextArea(3);
            inlineErrorLabel = AdminWidgets.createFeedbackBanner();

            prefillFields();

            JPanel formCard = new JPanel(new GridBagLayout());
            formCard.setBackground(Theme.Admin.SURFACE);
            formCard.setOpaque(true);
            formCard.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16
            ));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;

            int row = 0;
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Nama Layanan Kesehatan", serviceNameField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Dokter Praktik", doctorNameField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Hari Praktik", practiceDaysField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Slot Jam (pisahkan dengan koma, HH:mm)", timeSlotsField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Kuota Harian per Sesi", dailyQuotaField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Harga Layanan (Rp)", priceField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Warna Kartu", cardColorCombo);
            AdminWidgets.addScrollAreaGroup(formCard, gbc, row++, "Deskripsi Layanan", descriptionArea, 80);

            gbc.gridy = row;
            gbc.weighty = 1.0;
            gbc.insets = new Insets(4, 0, 0, 0);
            formCard.add(inlineErrorLabel, gbc);

            JButton cancelButton = UiKit.adminLightButton("Batal", VectorIcons.Kind.CLOSE);
            cancelButton.addActionListener(e -> dispose());

            JButton saveButton = UiKit.adminPrimaryButton("Simpan Layanan", VectorIcons.Kind.CHECK);
            saveButton.addActionListener(e -> submitForm());

            AdminWidgets.finalizeFormDialog(this, app, formCard, cancelButton, saveButton, 520, 540);
        }

        public boolean isSaved() {
            return saved;
        }

        public String getInlineErrorText() {
            return inlineErrorLabel.isVisible() ? inlineErrorLabel.getText() : "";
        }

        private void prefillFields() {
            if (existing == null) {
                doctorNameField.setText(DEFAULT_DOCTOR);
                practiceDaysField.setText(DEFAULT_DAYS);
                timeSlotsField.setText(DEFAULT_SLOTS);
                dailyQuotaField.setText("5");
                selectCardColor("card-green");
                return;
            }
            serviceNameField.setText(AdminWidgets.nonNull(existing.getServiceName()));
            doctorNameField.setText(AdminWidgets.nonNull(existing.getDoctorName()));
            practiceDaysField.setText(AdminWidgets.nonNull(existing.getPracticeDays()));
            timeSlotsField.setText(AdminWidgets.nonNull(existing.getTimeSlots()));
            dailyQuotaField.setText(String.valueOf(Math.max(1, existing.getDailyQuota())));
            priceField.setText(AdminWidgets.formatPlainDecimal(existing.getPrice()));
            selectCardColor(existing.getCardColor());
            descriptionArea.setText(AdminWidgets.nonNull(existing.getDescription()));
            descriptionArea.setCaretPosition(0);
        }

        private void selectCardColor(String rawColor) {
            String clean = rawColor == null ? "" : rawColor.trim().toLowerCase(Locale.ROOT);
            for (int i = 0; i < cardColorCombo.getItemCount(); i++) {
                ColorOption option = cardColorCombo.getItemAt(i);
                if (option != null && option.code().equalsIgnoreCase(clean)) {
                    cardColorCombo.setSelectedIndex(i);
                    return;
                }
            }
            cardColorCombo.setSelectedIndex(0);
        }

        private void submitForm() {
            AdminWidgets.hideBanner(inlineErrorLabel);
            if (!app.getSession().isAdmin()) {
                AdminWidgets.showDialogError(
                    this,
                    inlineErrorLabel,
                    "Hanya administrator yang dapat menyimpan layanan kesehatan."
                );
                return;
            }

            int parsedQuota;
            String rawQuota = dailyQuotaField.getText() == null ? "" : dailyQuotaField.getText().trim();
            if (rawQuota.isEmpty()) {
                AdminWidgets.showDialogError(this, inlineErrorLabel, "Kuota harian per sesi wajib diisi.");
                return;
            }
            try {
                parsedQuota = Integer.parseInt(rawQuota);
            } catch (NumberFormatException ex) {
                AdminWidgets.showDialogError(
                    this,
                    inlineErrorLabel,
                    "Kuota harian harus berupa bilangan bulat (contoh: 5)."
                );
                return;
            }

            BigDecimal parsedPrice;
            String rawPrice = priceField.getText() == null ? "" : priceField.getText().trim();
            if (rawPrice.isEmpty()) {
                AdminWidgets.showDialogError(this, inlineErrorLabel, "Harga layanan kesehatan wajib diisi.");
                return;
            }
            try {
                parsedPrice = new BigDecimal(rawPrice);
            } catch (NumberFormatException ex) {
                AdminWidgets.showDialogError(
                    this,
                    inlineErrorLabel,
                    "Format harga layanan tidak valid (contoh angka: 100000)."
                );
                return;
            }

            Object colorObj = cardColorCombo.getSelectedItem();
            String cardColorCode = colorObj instanceof ColorOption opt ? opt.code() : "card-green";

            HealthService target = new HealthService();
            if (existing != null) {
                target.setServiceId(existing.getServiceId());
                // Preserve DB icon metadata without ever rendering emoji glyphs in Swing UI.
                target.setIcon(existing.getIcon());
            } else {
                target.setIcon("+");
            }
            target.setCardColor(cardColorCode);
            target.setServiceName(serviceNameField.getText());
            target.setDoctorName(doctorNameField.getText());
            target.setPracticeDays(practiceDaysField.getText());
            target.setTimeSlots(timeSlotsField.getText());
            target.setDailyQuota(parsedQuota);
            target.setPrice(parsedPrice);
            target.setDescription(descriptionArea.getText());

            try {
                if (existing == null) {
                    app.getHealthBookingService().createService(target);
                } else {
                    app.getHealthBookingService().updateService(target);
                }
                saved = true;
                dispose();
            } catch (RuntimeException ex) {
                AdminWidgets.showDialogError(this, inlineErrorLabel, AdminWidgets.safeErrorMessage(ex));
            }
        }
    }
}
