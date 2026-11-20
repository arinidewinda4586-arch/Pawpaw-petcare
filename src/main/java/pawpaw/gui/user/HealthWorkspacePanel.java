package pawpaw.gui.user;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.ResponsiveCardGrid;
import pawpaw.gui.common.StatusBadgeRenderer;
import pawpaw.gui.common.UiKit;
import pawpaw.model.HealthBooking;
import pawpaw.model.HealthService;
import pawpaw.model.Pet;
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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Customer health booking workspace displaying pastel service cards with vector icons and
 * WIB time-slot chips, live per-slot quotas, pet selector, and booking history slips.
 */
public final class HealthWorkspacePanel extends PagePanel {

    private static final long serialVersionUID = 1L;
    private static final String[] PET_SPECIES_OPTIONS = {
        "Kucing", "Anjing", "Kelinci", "Burung", "Hamster", "Lainnya"
    };

    private record PetChoice(Pet pet, String manualLabel) {
        @Override
        public String toString() {
            if (pet != null) {
                String name = WorkspaceWidgets.safeText(pet.getPetName());
                String species = WorkspaceWidgets.safeText(pet.getSpecies());
                return "-".equals(species) ? name : name + " (" + species + ")";
            }
            return manualLabel != null ? manualLabel : "Hewan Baru (isi manual)";
        }
    }

    private record TimeSlotChoice(String slot, int remaining, int totalQuota) {
        @Override
        public String toString() {
            return slot + " (sisa " + remaining + "/" + totalQuota + ")";
        }
    }

    private final transient AppWindow app;

    private final JLabel inlineErrorLabel;
    private final JPanel errorBannerSlot;
    private final JTabbedPane tabs;

    // Tab 1: Services card grid & booking form
    private final JTextField searchField;
    private final JLabel serviceCountLabel;
    private final ResponsiveCardGrid serviceGrid;
    private final transient List<HealthServiceCard> serviceCards = new ArrayList<>();

    private final TintedSummaryPanel serviceSummaryCard;
    private final WorkspaceWidgets.VectorIconBadge selectedServiceIconBadge;
    private final WorkspaceWidgets.WrappingHtmlLabel selectedServiceNameLabel;
    private final JLabel selectedServicePriceLabel;
    private final JLabel selectedDoctorValue;
    private final JLabel selectedDaysValue;
    private final JLabel selectedQuotaValue;
    private final JTextArea serviceDescArea;

    private final JComboBox<PetChoice> petCombo;
    private final JTextField ownerNameField;
    private final JTextField petNameField;
    private final JComboBox<String> petSpeciesCombo;
    private final JSpinner dateSpinner;
    private final JComboBox<TimeSlotChoice> timeSlotCombo;
    private final JLabel formFeedbackLabel;
    private final JPanel formFeedbackSlot;
    private final JLabel quotaHintLabel;
    private final JButton submitBookingButton;

    // Tab 2: Bookings history
    private final JLabel bookingsCountLabel;
    private final JButton bookingCardButton;
    private final JButton cancelBookingButton;
    private final WorkspaceWidgets.ReadOnlyTableModel bookingsModel;
    private final JTable bookingsTable;

    private transient List<HealthService> allServices = new ArrayList<>();
    private transient List<HealthService> filteredServices = new ArrayList<>();
    private transient List<Pet> userPets = new ArrayList<>();
    private transient List<HealthBooking> userBookings = new ArrayList<>();
    private transient HealthService selectedService;
    private transient HealthBooking selectedBooking;
    private boolean updatingPetFields;
    private String statusSummary = "Ruang kerja layanan kesehatan siap.";

    public HealthWorkspacePanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineErrorLabel = WorkspaceWidgets.createFeedbackBanner();
        errorBannerSlot = WorkspaceWidgets.createBannerSlot(inlineErrorLabel, 0);
        add(errorBannerSlot, BorderLayout.NORTH);

        // --- Tab 1: Booking Layanan ---
        searchField = UiKit.searchField("Cari layanan, dokter, atau hari praktik...");
        searchField.setPreferredSize(new Dimension(260, 36));
        WorkspaceWidgets.wireTextChange(searchField, this::applyServiceFilter);

        serviceCountLabel = UiKit.label("0 layanan", Theme.smallBold(), Theme.MUTED);

        JPanel serviceToolbar = UiKit.cardPanel(new BorderLayout(Theme.SPACE_12, 0));
        serviceToolbar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        serviceToolbar.add(searchField, BorderLayout.CENTER);
        serviceToolbar.add(serviceCountLabel, BorderLayout.EAST);

        serviceGrid = new ResponsiveCardGrid(190, 12, 12);
        serviceGrid.setEmptyState(
            "Tidak ada layanan ditemukan",
            "Coba ubah kata kunci pencarian layanan kesehatan."
        );
        JScrollPane serviceScroll = WorkspaceWidgets.verticalScrollPane(serviceGrid, Theme.BG);
        serviceScroll.setMinimumSize(new Dimension(415, 260));
        serviceScroll.setPreferredSize(new Dimension(430, 420));

        selectedServiceIconBadge = WorkspaceWidgets.iconBadge(
            VectorIcons.Kind.HEALTH,
            32,
            16,
            9,
            Theme.SURFACE,
            Theme.BLUE,
            Theme.tint(Theme.NAVY, 24)
        );
        selectedServiceNameLabel = new WorkspaceWidgets.WrappingHtmlLabel(
            "Pilih Layanan Kesehatan",
            Theme.heading(),
            Theme.INK,
            160
        );
        selectedServicePriceLabel = UiKit.pillLabel(
            FormatUtil.rupiah(BigDecimal.ZERO),
            Theme.SURFACE,
            Theme.BLUE
        );
        selectedDoctorValue = UiKit.label("-", Theme.body(), Theme.INK);
        selectedDaysValue = UiKit.label("-", Theme.body(), Theme.INK);
        selectedQuotaValue = UiKit.label("-", Theme.body(), Theme.INK);
        serviceDescArea = WorkspaceWidgets.createReadOnlyTextArea(3, Theme.small(), Theme.SURFACE);
        serviceSummaryCard = new TintedSummaryPanel();

        petCombo = UiKit.comboBox();
        petCombo.addActionListener(e -> onPetChoiceChanged());

        ownerNameField = UiKit.textField("Nama lengkap pemilik hewan");
        petNameField = UiKit.textField("Masukkan nama hewan");
        petSpeciesCombo = UiKit.comboBox(PET_SPECIES_OPTIONS);
        petSpeciesCombo.setEditable(true);

        Date defaultDate = toDate(LocalDate.now().plusDays(1));
        dateSpinner = new JSpinner(new SpinnerDateModel(defaultDate, null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateEditor.getTextField().setColumns(10);
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setFont(Theme.body());
        dateSpinner.setPreferredSize(new Dimension(200, 36));
        dateSpinner.addChangeListener(e -> reloadTimeSlotsForSelection());

        timeSlotCombo = UiKit.comboBox();
        timeSlotCombo.addActionListener(e -> updateSubmitButtonState());

        formFeedbackLabel = WorkspaceWidgets.createFeedbackBanner();
        formFeedbackSlot = WorkspaceWidgets.createBannerSlot(formFeedbackLabel, 0);
        quotaHintLabel = UiKit.label(
            "<html>Pilih layanan kesehatan pada katalog di sebelah kiri.</html>",
            Theme.small(),
            Theme.MUTED
        );
        submitBookingButton = UiKit.primaryButton("Simpan Booking", VectorIcons.Kind.CHECK);
        submitBookingButton.setEnabled(false);
        submitBookingButton.addActionListener(e -> submitBooking());

        JPanel bookingFormCard = buildBookingFormCard();

        JSplitPane bookingSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, serviceScroll, bookingFormCard);
        bookingSplit.setResizeWeight(0.68);
        bookingSplit.setDividerSize(8);
        bookingSplit.setBorder(BorderFactory.createEmptyBorder());
        bookingSplit.setContinuousLayout(true);
        bookingSplit.setOpaque(false);

        JPanel bookingTab = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        bookingTab.setOpaque(false);
        bookingTab.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, 0, 0, 0));
        bookingTab.add(serviceToolbar, BorderLayout.NORTH);
        bookingTab.add(bookingSplit, BorderLayout.CENTER);

        // --- Tab 2: Riwayat Booking ---
        bookingsCountLabel = UiKit.label("0 booking", Theme.smallBold(), Theme.MUTED);
        bookingCardButton = UiKit.secondaryButton("Kartu Booking", VectorIcons.Kind.BOOKINGS);
        bookingCardButton.setEnabled(false);
        bookingCardButton.addActionListener(e -> openSelectedBookingCard());

        cancelBookingButton = UiKit.dangerButton("Batalkan Booking", VectorIcons.Kind.CLOSE);
        cancelBookingButton.setEnabled(false);
        cancelBookingButton.addActionListener(e -> cancelSelectedBooking());

        JPanel historyActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
        historyActions.setOpaque(false);
        historyActions.add(bookingCardButton);
        historyActions.add(cancelBookingButton);

        JPanel historyToolbar = UiKit.cardPanel(new BorderLayout(Theme.SPACE_12, 0));
        historyToolbar.add(bookingsCountLabel, BorderLayout.WEST);
        historyToolbar.add(historyActions, BorderLayout.EAST);

        bookingsModel = new WorkspaceWidgets.ReadOnlyTableModel(
            new Object[] { "ID", "Layanan", "Dokter", "Hewan", "Jadwal", "Status Booking", "Pembayaran" },
            new Class<?>[] {
                Integer.class, String.class, String.class, String.class, String.class, String.class, String.class
            }
        );
        bookingsTable = UiKit.table(bookingsModel);
        bookingsTable.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
        bookingsTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        configureBookingColumns();
        bookingsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onBookingSelectionChanged();
            }
        });

        JPanel historyTab = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        historyTab.setOpaque(false);
        historyTab.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, 0, 0, 0));
        historyTab.add(historyToolbar, BorderLayout.NORTH);
        historyTab.add(UiKit.scrollPane(bookingsTable), BorderLayout.CENTER);

        tabs = new JTabbedPane();
        tabs.setFont(Theme.bodyBold());
        tabs.setBackground(Theme.SURFACE);
        tabs.setForeground(Theme.INK);
        tabs.addTab("Booking Layanan", bookingTab);
        tabs.addTab("Riwayat Booking", historyTab);

        add(tabs, BorderLayout.CENTER);
    }

    public int getDisplayedServiceCardCount() {
        return serviceGrid.getCardCount();
    }

    public int getCatalogColumnCount() {
        return serviceGrid.getColumnCount();
    }

    public boolean selectServiceById(int serviceId) {
        for (HealthService service : filteredServices) {
            if (service.getServiceId() == serviceId) {
                selectService(service);
                return true;
            }
        }
        return false;
    }

    private JPanel buildBookingFormCard() {
        JPanel card = UiKit.cardPanel(new BorderLayout(0, Theme.SPACE_12));
        card.setPreferredSize(new Dimension(345, 0));
        card.setMinimumSize(new Dimension(310, 0));

        // Selected service summary block (tinted to match active HealthServiceCard pastel color)
        JPanel iconSlot = new JPanel(new GridBagLayout());
        iconSlot.setOpaque(false);
        iconSlot.add(selectedServiceIconBadge);

        JPanel priceSlot = new JPanel(new GridBagLayout());
        priceSlot.setOpaque(false);
        priceSlot.add(selectedServicePriceLabel);

        JPanel headerRow = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
        headerRow.setOpaque(false);
        headerRow.add(iconSlot, BorderLayout.WEST);
        headerRow.add(priceSlot, BorderLayout.EAST);

        JPanel topSummary = new JPanel(new BorderLayout(0, 6));
        topSummary.setOpaque(false);
        topSummary.add(headerRow, BorderLayout.NORTH);
        topSummary.add(selectedServiceNameLabel, BorderLayout.CENTER);

        JPanel attrsGrid = new JPanel(new GridBagLayout());
        attrsGrid.setOpaque(false);
        WorkspaceWidgets.addAttrRow(attrsGrid, 0, "Dokter", selectedDoctorValue);
        WorkspaceWidgets.addAttrRow(attrsGrid, 1, "Hari Praktik", selectedDaysValue);
        WorkspaceWidgets.addAttrRow(attrsGrid, 2, "Kuota / Sesi", selectedQuotaValue);

        JScrollPane descScroll = UiKit.scrollPane(serviceDescArea);
        descScroll.setPreferredSize(new Dimension(240, 54));

        serviceSummaryCard.add(topSummary, BorderLayout.NORTH);
        serviceSummaryCard.add(attrsGrid, BorderLayout.CENTER);
        serviceSummaryCard.add(descScroll, BorderLayout.SOUTH);

        JPanel summaryWrap = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout());
        summaryWrap.add(serviceSummaryCard, BorderLayout.CENTER);

        // Booking form fields
        JPanel fieldsPanel = new WorkspaceWidgets.CompactSectionPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        WorkspaceWidgets.addFieldGroup(fieldsPanel, gbc, row++, "Pilih Hewan Peliharaan", petCombo);
        WorkspaceWidgets.addFieldGroup(fieldsPanel, gbc, row++, "Nama Pemilik", ownerNameField);
        WorkspaceWidgets.addFieldGroup(fieldsPanel, gbc, row++, "Nama Hewan", petNameField);
        WorkspaceWidgets.addFieldGroup(fieldsPanel, gbc, row++, "Jenis Hewan", petSpeciesCombo);
        WorkspaceWidgets.addFieldGroup(fieldsPanel, gbc, row++, "Tanggal Kunjungan (YYYY-MM-DD)", dateSpinner);
        WorkspaceWidgets.addFieldGroup(fieldsPanel, gbc, row, "Sesi Jam & Sisa Kuota", timeSlotCombo);

        JPanel formStack = new JPanel();
        formStack.setLayout(new BoxLayout(formStack, BoxLayout.Y_AXIS));
        formStack.setOpaque(false);
        summaryWrap.setAlignmentX(LEFT_ALIGNMENT);
        fieldsPanel.setAlignmentX(LEFT_ALIGNMENT);
        formFeedbackSlot.setAlignmentX(LEFT_ALIGNMENT);
        formStack.add(summaryWrap);
        formStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        formStack.add(fieldsPanel);
        formStack.add(Box.createVerticalStrut(Theme.SPACE_8));
        formStack.add(formFeedbackSlot);

        JPanel footer = new JPanel(new BorderLayout(0, Theme.SPACE_8));
        footer.setOpaque(false);
        footer.add(quotaHintLabel, BorderLayout.NORTH);
        footer.add(submitBookingButton, BorderLayout.SOUTH);

        card.add(WorkspaceWidgets.verticalScrollPane(formStack, Theme.SURFACE), BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private void configureBookingColumns() {
        bookingsTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        bookingsTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        bookingsTable.getColumnModel().getColumn(2).setPreferredWidth(155);
        bookingsTable.getColumnModel().getColumn(3).setPreferredWidth(135);
        bookingsTable.getColumnModel().getColumn(4).setPreferredWidth(145);
        bookingsTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        bookingsTable.getColumnModel().getColumn(6).setPreferredWidth(115);
    }

    @Override
    public String getPageKey() {
        return "user.health";
    }

    @Override
    public String getPageTitle() {
        return "Layanan Kesehatan";
    }

    @Override
    public String getPageSubtitle() {
        return "Pilih layanan dokter hewan, tanggal, kuota sesi, dan riwayat booking";
    }

    @Override
    public void refreshData() {
        int prevServiceId = selectedService != null ? selectedService.getServiceId() : -1;
        int prevBookingId = selectedBooking != null ? selectedBooking.getBookingId() : -1;
        WorkspaceWidgets.hideBannerInSlot(errorBannerSlot, inlineErrorLabel);
        WorkspaceWidgets.hideBannerInSlot(formFeedbackSlot, formFeedbackLabel);

        User user = app.getSession().getUser();
        try {
            allServices = new ArrayList<>(app.getHealthBookingService().listServices());
            if (user != null) {
                userPets = new ArrayList<>(app.getProfileService().listPets(user.getUserId()));
                userBookings = new ArrayList<>(app.getHealthBookingService().listUserBookings(user.getUserId()));
                ownerNameField.setText(WorkspaceWidgets.nonNull(user.getName()));
            } else {
                userPets = new ArrayList<>();
                userBookings = new ArrayList<>();
                ownerNameField.setText("");
            }

            populatePetCombo();
            applyServiceFilter(prevServiceId);
            populateBookingsTable(prevBookingId);

            if (user == null) {
                statusSummary = "Layanan kesehatan: " + allServices.size()
                    + " layanan tersedia. Masuk untuk membuat janji booking.";
            } else {
                statusSummary = "Layanan kesehatan: " + allServices.size()
                    + " layanan, " + userBookings.size() + " riwayat booking Anda.";
            }
        } catch (RuntimeException ex) {
            allServices = new ArrayList<>();
            filteredServices = new ArrayList<>();
            serviceCards.clear();
            serviceGrid.setCards(List.of());
            serviceCountLabel.setText("0 layanan");
            userPets = new ArrayList<>();
            userBookings = new ArrayList<>();
            bookingsModel.setRowCount(0);
            populatePetCombo();
            clearServiceSelection();
            clearBookingSelection();
            String msg = WorkspaceWidgets.safeErrorMessage(ex);
            WorkspaceWidgets.showBannerInSlot(
                errorBannerSlot,
                inlineErrorLabel,
                "Gagal memuat layanan kesehatan: " + msg,
                true
            );
            statusSummary = "Gagal memuat layanan kesehatan: " + msg;
        }
    }

    @Override
    public String getStatusSummary() {
        return statusSummary;
    }

    private void populatePetCombo() {
        updatingPetFields = true;
        try {
            DefaultComboBoxModel<PetChoice> model = new DefaultComboBoxModel<>();
            for (Pet pet : userPets) {
                model.addElement(new PetChoice(pet, null));
            }
            model.addElement(new PetChoice(null, "Hewan Baru (isi manual)"));
            petCombo.setModel(model);
            if (model.getSize() > 0) {
                petCombo.setSelectedIndex(0);
            }
        } finally {
            updatingPetFields = false;
        }
        onPetChoiceChanged();
    }

    private void onPetChoiceChanged() {
        if (updatingPetFields) {
            return;
        }
        Object item = petCombo.getSelectedItem();
        if (item instanceof PetChoice choice && choice.pet() != null) {
            Pet pet = choice.pet();
            petNameField.setText(WorkspaceWidgets.nonNull(pet.getPetName()));
            petNameField.setEditable(false);
            petNameField.setBackground(Theme.SURFACE_ALT);
            petSpeciesCombo.setSelectedItem(WorkspaceWidgets.nonNull(pet.getSpecies()));
            petSpeciesCombo.setEnabled(false);
        } else {
            petNameField.setText("");
            petNameField.setEditable(true);
            petNameField.setBackground(Theme.SURFACE);
            petSpeciesCombo.setEnabled(true);
            petSpeciesCombo.setSelectedItem("Kucing");
        }
    }

    private void applyServiceFilter() {
        int currentId = selectedService != null ? selectedService.getServiceId() : -1;
        applyServiceFilter(currentId);
    }

    private void applyServiceFilter(int preferredServiceId) {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase(Locale.ROOT);

        filteredServices = new ArrayList<>();
        serviceCards.clear();
        for (HealthService service : allServices) {
            if (!matchesQuery(service, query)) {
                continue;
            }
            filteredServices.add(service);
            serviceCards.add(new HealthServiceCard(service));
        }

        serviceGrid.setCards(serviceCards);
        serviceCountLabel.setText(
            "Menampilkan " + filteredServices.size() + " dari " + allServices.size() + " layanan"
        );
        restoreServiceSelection(preferredServiceId);
    }

    private void restoreServiceSelection(int preferredServiceId) {
        if (filteredServices.isEmpty()) {
            clearServiceSelection();
            return;
        }
        HealthService target = filteredServices.get(0);
        if (preferredServiceId > 0) {
            for (HealthService candidate : filteredServices) {
                if (candidate.getServiceId() == preferredServiceId) {
                    target = candidate;
                    break;
                }
            }
        }
        selectService(target);
    }

    private void selectService(HealthService service) {
        if (service == null) {
            clearServiceSelection();
            return;
        }
        selectedService = service;
        for (HealthServiceCard card : serviceCards) {
            card.setSelected(card.service.getServiceId() == service.getServiceId());
        }

        serviceSummaryCard.setFillColor(Theme.healthCardColor(service.getCardColor()));
        selectedServiceIconBadge.setKind(resolveServiceIconKind(service));
        selectedServiceNameLabel.setPlainText(WorkspaceWidgets.safeText(service.getServiceName()));
        selectedServicePriceLabel.setText(FormatUtil.rupiah(service.getPrice()));
        selectedDoctorValue.setText(WorkspaceWidgets.safeText(service.getDoctorName()));
        selectedDaysValue.setText(WorkspaceWidgets.safeText(service.getPracticeDays()));
        selectedQuotaValue.setText(service.getDailyQuota() + " pasien / sesi");
        String desc = (service.getDescription() == null || service.getDescription().isBlank())
            ? "Belum ada deskripsi layanan."
            : service.getDescription();
        serviceDescArea.setText(desc);
        serviceDescArea.setCaretPosition(0);

        reloadTimeSlotsForSelection();
    }

    private void clearServiceSelection() {
        selectedService = null;
        for (HealthServiceCard card : serviceCards) {
            card.setSelected(false);
        }
        serviceSummaryCard.setFillColor(Theme.SURFACE_ALT);
        selectedServiceIconBadge.setKind(VectorIcons.Kind.HEALTH);
        selectedServiceNameLabel.setPlainText("Pilih Layanan Kesehatan");
        selectedServicePriceLabel.setText(FormatUtil.rupiah(BigDecimal.ZERO));
        selectedDoctorValue.setText("-");
        selectedDaysValue.setText("-");
        selectedQuotaValue.setText("-");
        serviceDescArea.setText("");
        timeSlotCombo.setModel(new DefaultComboBoxModel<>());
        updateSubmitButtonState();
    }

    private void reloadTimeSlotsForSelection() {
        DefaultComboBoxModel<TimeSlotChoice> slotModel = new DefaultComboBoxModel<>();
        if (selectedService == null) {
            timeSlotCombo.setModel(slotModel);
            updateSubmitButtonState();
            return;
        }
        LocalDate selectedDate = getSelectedLocalDate();
        int totalQuota = Math.max(1, selectedService.getDailyQuota());
        try {
            Map<String, Integer> remainingMap = app.getHealthBookingService().getRemainingQuotaBySlot(
                selectedService.getServiceId(),
                selectedDate
            );
            for (Map.Entry<String, Integer> entry : remainingMap.entrySet()) {
                int remaining = Math.max(0, entry.getValue() != null ? entry.getValue() : 0);
                slotModel.addElement(new TimeSlotChoice(entry.getKey(), remaining, totalQuota));
            }
            timeSlotCombo.setModel(slotModel);
            if (slotModel.getSize() > 0) {
                int firstAvailable = 0;
                for (int i = 0; i < slotModel.getSize(); i++) {
                    if (slotModel.getElementAt(i).remaining() > 0) {
                        firstAvailable = i;
                        break;
                    }
                }
                timeSlotCombo.setSelectedIndex(firstAvailable);
            }
        } catch (RuntimeException ex) {
            timeSlotCombo.setModel(slotModel);
            submitBookingButton.setEnabled(false);
            quotaHintLabel.setText(
                "<html>Gagal memuat kuota slot: "
                    + WorkspaceWidgets.escapeHtml(WorkspaceWidgets.safeErrorMessage(ex)) + "</html>"
            );
            quotaHintLabel.setForeground(Theme.DANGER_FG);
            return;
        }
        updateSubmitButtonState();
    }

    private void updateSubmitButtonState() {
        User user = app.getSession().getUser();
        Object slotObj = timeSlotCombo.getSelectedItem();
        if (selectedService == null) {
            submitBookingButton.setEnabled(false);
            quotaHintLabel.setText("<html>Pilih layanan kesehatan pada katalog di sebelah kiri.</html>");
            quotaHintLabel.setForeground(Theme.MUTED);
            return;
        }
        if (!(slotObj instanceof TimeSlotChoice slotChoice)) {
            submitBookingButton.setEnabled(false);
            quotaHintLabel.setText("<html>Tidak ada slot jam praktik tersedia untuk layanan ini.</html>");
            quotaHintLabel.setForeground(Theme.DANGER_FG);
            return;
        }
        if (slotChoice.remaining() <= 0) {
            submitBookingButton.setEnabled(false);
            quotaHintLabel.setText(
                "<html>Kuota sesi " + slotChoice.slot() + " penuh (0/" + slotChoice.totalQuota()
                    + "). Silakan pilih jam atau tanggal lain.</html>"
            );
            quotaHintLabel.setForeground(Theme.DANGER_FG);
            return;
        }
        if (user == null) {
            submitBookingButton.setEnabled(false);
            quotaHintLabel.setText("<html>Masuk terlebih dahulu untuk mengirim booking.</html>");
            quotaHintLabel.setForeground(Theme.MUTED);
            return;
        }
        submitBookingButton.setEnabled(true);
        quotaHintLabel.setText(
            "<html>Tersedia " + slotChoice.remaining() + " dari " + slotChoice.totalQuota()
                + " kuota pada sesi " + slotChoice.slot() + ".</html>"
        );
        quotaHintLabel.setForeground(Theme.SUCCESS_FG);
    }

    private void submitBooking() {
        WorkspaceWidgets.hideBannerInSlot(formFeedbackSlot, formFeedbackLabel);
        User user = app.getSession().getUser();
        if (user == null) {
            WorkspaceWidgets.showBannerInSlot(
                formFeedbackSlot,
                formFeedbackLabel,
                "Silakan masuk terlebih dahulu.",
                true
            );
            return;
        }
        if (selectedService == null) {
            WorkspaceWidgets.showBannerInSlot(
                formFeedbackSlot,
                formFeedbackLabel,
                "Pilih layanan kesehatan terlebih dahulu.",
                true
            );
            return;
        }
        Object slotObj = timeSlotCombo.getSelectedItem();
        if (!(slotObj instanceof TimeSlotChoice slotChoice)) {
            WorkspaceWidgets.showBannerInSlot(
                formFeedbackSlot,
                formFeedbackLabel,
                "Slot jam kunjungan wajib dipilih.",
                true
            );
            return;
        }
        if (slotChoice.remaining() <= 0) {
            WorkspaceWidgets.showBannerInSlot(
                formFeedbackSlot,
                formFeedbackLabel,
                "Kuota sesi " + slotChoice.slot() + " sudah penuh. Pilih jam atau tanggal lain.",
                true
            );
            return;
        }

        Object petObj = petCombo.getSelectedItem();
        Integer petIdOrNull = (petObj instanceof PetChoice choice && choice.pet() != null)
            ? choice.pet().getPetId()
            : null;
        Object rawSpecies = petSpeciesCombo.isEditable()
            ? petSpeciesCombo.getEditor().getItem()
            : petSpeciesCombo.getSelectedItem();
        String petSpecies = rawSpecies != null ? rawSpecies.toString() : "";
        LocalDate date = getSelectedLocalDate();

        try {
            HealthBooking created = app.getHealthBookingService().createBooking(
                user.getUserId(),
                selectedService.getServiceId(),
                petIdOrNull,
                ownerNameField.getText(),
                petNameField.getText(),
                petSpecies,
                date,
                slotChoice.slot()
            );
            refreshData();
            tabs.setSelectedIndex(1);
            restoreBookingSelection(created.getBookingId());
            statusSummary = "Booking #" + created.getBookingId() + " untuk "
                + WorkspaceWidgets.safeText(created.getServiceName()) + " berhasil dibuat.";
            app.updateStatus(statusSummary);
            BookingCardDialog.show(this, created);
        } catch (RuntimeException ex) {
            WorkspaceWidgets.showBannerInSlot(
                formFeedbackSlot,
                formFeedbackLabel,
                WorkspaceWidgets.safeErrorMessage(ex),
                true
            );
            reloadTimeSlotsForSelection();
        }
    }

    private void populateBookingsTable(int preferredBookingId) {
        bookingsModel.setRowCount(0);
        for (HealthBooking booking : userBookings) {
            String serviceName = (booking.getServiceName() != null && !booking.getServiceName().isBlank())
                ? booking.getServiceName()
                : "Layanan #" + booking.getServiceId();
            String petName = WorkspaceWidgets.safeText(booking.getPetName());
            String petSpecies = WorkspaceWidgets.safeText(booking.getPetSpecies());
            String petSummary = "-".equals(petSpecies) ? petName : petName + " (" + petSpecies + ")";
            String paymentStatus = booking.getPayment() != null
                ? WorkspaceWidgets.nonNull(booking.getPayment().getPaymentStatus())
                : "";

            bookingsModel.addRow(new Object[] {
                Integer.valueOf(booking.getBookingId()),
                serviceName,
                WorkspaceWidgets.safeText(booking.getDoctorName()),
                petSummary,
                FormatUtil.dateTimeId(booking.getBookingDate()),
                WorkspaceWidgets.safeText(booking.getStatus()),
                paymentStatus
            });
        }
        bookingsCountLabel.setText(userBookings.size() + " riwayat booking kesehatan");
        restoreBookingSelection(preferredBookingId);
    }

    private void restoreBookingSelection(int preferredBookingId) {
        if (userBookings.isEmpty()) {
            clearBookingSelection();
            return;
        }
        int targetModelRow = 0;
        if (preferredBookingId > 0) {
            for (int i = 0; i < userBookings.size(); i++) {
                if (userBookings.get(i).getBookingId() == preferredBookingId) {
                    targetModelRow = i;
                    break;
                }
            }
        }
        int viewRow = bookingsTable.convertRowIndexToView(targetModelRow);
        if (viewRow >= 0 && viewRow < bookingsTable.getRowCount()) {
            bookingsTable.setRowSelectionInterval(viewRow, viewRow);
            onBookingSelectionChanged();
        } else {
            clearBookingSelection();
        }
    }

    private void onBookingSelectionChanged() {
        int viewRow = bookingsTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= bookingsTable.getRowCount()) {
            clearBookingSelection();
            return;
        }
        int modelRow = bookingsTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= userBookings.size()) {
            clearBookingSelection();
            return;
        }
        HealthBooking booking = userBookings.get(modelRow);
        selectedBooking = booking;
        bookingCardButton.setEnabled(true);
        cancelBookingButton.setEnabled("pending".equalsIgnoreCase(booking.getStatus()));
    }

    private void clearBookingSelection() {
        selectedBooking = null;
        bookingCardButton.setEnabled(false);
        cancelBookingButton.setEnabled(false);
    }

    private void openSelectedBookingCard() {
        User user = app.getSession().getUser();
        if (user == null || selectedBooking == null) {
            return;
        }
        try {
            HealthBooking card = app.getHealthBookingService().getBookingCard(
                selectedBooking.getBookingId(),
                user.getUserId(),
                user.isAdmin()
            );
            BookingCardDialog.show(this, card);
        } catch (RuntimeException ex) {
            UiKit.showError(this, WorkspaceWidgets.safeErrorMessage(ex));
        }
    }

    private void cancelSelectedBooking() {
        User user = app.getSession().getUser();
        if (user == null || selectedBooking == null || !"pending".equalsIgnoreCase(selectedBooking.getStatus())) {
            return;
        }
        boolean confirmed = UiKit.confirm(
            this,
            "Batalkan Booking Kesehatan",
            "Yakin ingin membatalkan booking #" + selectedBooking.getBookingId()
                + " (" + WorkspaceWidgets.safeText(selectedBooking.getServiceName()) + ")?"
        );
        if (!confirmed) {
            return;
        }
        try {
            app.getHealthBookingService().cancelUserBooking(user.getUserId(), selectedBooking.getBookingId());
            refreshData();
            app.updateStatus(getStatusSummary());
            UiKit.showInfo(this, "Booking kesehatan berhasil dibatalkan.");
        } catch (RuntimeException ex) {
            UiKit.showError(this, WorkspaceWidgets.safeErrorMessage(ex));
        }
    }

    private LocalDate getSelectedLocalDate() {
        Object value = dateSpinner.getValue();
        if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.now().plusDays(1);
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static boolean matchesQuery(HealthService service, String lowerQuery) {
        if (lowerQuery.isEmpty()) {
            return true;
        }
        return WorkspaceWidgets.containsIgnoreCase(service.getServiceName(), lowerQuery)
            || WorkspaceWidgets.containsIgnoreCase(service.getDoctorName(), lowerQuery)
            || WorkspaceWidgets.containsIgnoreCase(service.getPracticeDays(), lowerQuery)
            || WorkspaceWidgets.containsIgnoreCase(service.getDescription(), lowerQuery);
    }

    /**
     * Maps service metadata to a clean Java2D {@link VectorIcons.Kind} so raw database emojis
     * are never rendered anywhere in Swing UI.
     */
    private static VectorIcons.Kind resolveServiceIconKind(HealthService service) {
        String name = service != null && service.getServiceName() != null
            ? service.getServiceName().toLowerCase(Locale.ROOT)
            : "";
        if (name.contains("vaksin") || name.contains("obat")) {
            return VectorIcons.Kind.SHIELD;
        }
        if (name.contains("nutrisi")) {
            return VectorIcons.Kind.PAW;
        }
        if (name.contains("kunjungan") || name.contains("rumah")) {
            return VectorIcons.Kind.ADOPTION;
        }
        if (name.contains("gigi") || name.contains("telinga") || name.contains("kulit") || name.contains("mata")) {
            return VectorIcons.Kind.SERVICES;
        }
        return VectorIcons.Kind.HEALTH;
    }

    /**
     * Rounded summary card inside the booking inspector that dynamically adopts the selected
     * service's pastel color.
     */
    private static final class TintedSummaryPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private Color fillColor = Theme.SURFACE_ALT;

        TintedSummaryPanel() {
            super(new BorderLayout(0, Theme.SPACE_8));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        }

        void setFillColor(Color fillColor) {
            this.fillColor = fillColor != null ? fillColor : Theme.SURFACE_ALT;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w > 2 && h > 2) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    double arc = 32.0;
                    g2.setColor(fillColor);
                    g2.fill(new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, arc, arc));
                    g2.setColor(Theme.tint(Theme.NAVY, 28));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, arc, arc));
                } finally {
                    g2.dispose();
                }
            }
            super.paintComponent(g);
        }
    }

    /**
     * Website-matched pastel health service card with 2-row header (icon badge + price pill on row 1,
     * full-width wrapped HTML title on row 2), doctor/quota metadata, and white HH:mm WIB chips.
     */
    private final class HealthServiceCard extends JPanel {
        private static final long serialVersionUID = 1L;

        private final transient HealthService service;
        private final Color pastelFill;
        private final Color hoverFill;
        private boolean selected;
        private boolean hovered;

        HealthServiceCard(HealthService service) {
            super(new BorderLayout(0, 8));
            this.service = service;
            this.pastelFill = Theme.healthCardColor(service.getCardColor());
            this.hoverFill = WorkspaceWidgets.blendLighter(this.pastelFill, 0.14f);

            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

            // Row 1: 36x36 white vector icon badge (left) + white Rp price pill (right)
            JLabel iconBadge = WorkspaceWidgets.iconBadge(
                resolveServiceIconKind(service),
                36,
                18,
                10,
                Theme.SURFACE,
                Theme.BLUE,
                Theme.tint(Theme.NAVY, 24)
            );
            JLabel pricePill = UiKit.pillLabel(
                FormatUtil.rupiah(service.getPrice()),
                Theme.SURFACE,
                Theme.BLUE
            );
            JPanel priceSlot = new JPanel(new GridBagLayout());
            priceSlot.setOpaque(false);
            priceSlot.add(pricePill);

            JPanel row1 = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
            row1.setOpaque(false);
            row1.add(iconBadge, BorderLayout.WEST);
            row1.add(priceSlot, BorderLayout.EAST);

            // Row 2: Full-width wrapped HTML title (never collides with price pill)
            WorkspaceWidgets.WrappingHtmlLabel titleLabel = new WorkspaceWidgets.WrappingHtmlLabel(
                WorkspaceWidgets.safeText(service.getServiceName()),
                Theme.title().deriveFont(14f),
                Theme.INK,
                166
            );

            JPanel topSection = new JPanel(new BorderLayout(0, 8));
            topSection.setOpaque(false);
            topSection.add(row1, BorderLayout.NORTH);
            topSection.add(titleLabel, BorderLayout.CENTER);

            // Row 3: Doctor name + practice days & daily quota
            WorkspaceWidgets.WrappingHtmlLabel doctorLabel = new WorkspaceWidgets.WrappingHtmlLabel(
                WorkspaceWidgets.safeText(service.getDoctorName()),
                Theme.smallBold(),
                Theme.INK,
                166
            );
            String practiceMeta = WorkspaceWidgets.safeText(service.getPracticeDays())
                + " \u2022 Kuota " + Math.max(1, service.getDailyQuota()) + "/sesi";
            WorkspaceWidgets.WrappingHtmlLabel practiceLabel = new WorkspaceWidgets.WrappingHtmlLabel(
                practiceMeta,
                Theme.badge(),
                Theme.MUTED,
                166
            );

            JPanel metaPanel = new JPanel(new BorderLayout(0, 2));
            metaPanel.setOpaque(false);
            metaPanel.add(doctorLabel, BorderLayout.NORTH);
            metaPanel.add(practiceLabel, BorderLayout.SOUTH);

            // Row 4: White HH:mm WIB time-slot chips
            JPanel chipsPanel = new JPanel(WorkspaceWidgets.wrapFlowLayout(4, 4));
            chipsPanel.setOpaque(false);
            List<String> slots = app.getHealthBookingService().parseTimeSlots(service.getTimeSlots());
            if (slots.isEmpty()) {
                JLabel flexChip = UiKit.pillLabel("Jadwal fleksibel", Theme.SURFACE, Theme.BLUE);
                flexChip.setFont(Theme.bodyMedium().deriveFont(11f));
                chipsPanel.add(flexChip);
            } else {
                for (String slot : slots) {
                    JLabel chip = UiKit.pillLabel(slot + " WIB", Theme.SURFACE, Theme.BLUE);
                    chip.setFont(Theme.bodyMedium().deriveFont(11f));
                    chipsPanel.add(chip);
                }
            }

            JPanel bodySection = new JPanel(new BorderLayout(0, 6));
            bodySection.setOpaque(false);
            bodySection.add(metaPanel, BorderLayout.NORTH);
            bodySection.add(chipsPanel, BorderLayout.CENTER);

            setToolTipText(
                WorkspaceWidgets.safeText(service.getServiceName())
                    + " - " + WorkspaceWidgets.safeText(service.getDoctorName())
            );

            add(topSection, BorderLayout.NORTH);
            add(bodySection, BorderLayout.CENTER);

            WorkspaceWidgets.attachCardInteraction(
                this,
                () -> selectService(service),
                h -> {
                    hovered = h;
                    repaint();
                }
            );
        }

        void setSelected(boolean selected) {
            if (this.selected != selected) {
                this.selected = selected;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            WorkspaceWidgets.paintInteractiveCard(
                g,
                getWidth(),
                getHeight(),
                Theme.CARD_RADIUS,
                pastelFill,
                hoverFill,
                Theme.tint(Theme.NAVY, 26),
                Theme.tint(Theme.BLUE, 120),
                hovered,
                selected,
                2.5f
            );
            super.paintComponent(g);
        }
    }
}
