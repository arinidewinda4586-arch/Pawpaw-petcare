package pawpaw.gui.user;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.gui.common.ResponsiveCardGrid;
import pawpaw.gui.common.StatusBadgeRenderer;
import pawpaw.gui.common.UiKit;
import pawpaw.model.AdoptionRequest;
import pawpaw.model.Animal;
import pawpaw.model.User;
import pawpaw.util.FormatUtil;
import pawpaw.util.ImageStore;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Customer adoption workspace with website-styled animal card grid, 5 icon species filter pills,
 * detail inspector, and personal adoption request history with self-cancellation for pending requests.
 */
public final class AdoptionWorkspacePanel extends PagePanel {

    private static final long serialVersionUID = 1L;

    private record SpeciesOption(String label, String iconPath) {
    }

    private static final List<SpeciesOption> SPECIES_OPTIONS = List.of(
        new SpeciesOption("Semua", "assets/img/all.png"),
        new SpeciesOption("Kucing", "assets/img/kucing.png"),
        new SpeciesOption("Anjing", "assets/img/dog.png"),
        new SpeciesOption("Kelinci", "assets/img/kelinci.png"),
        new SpeciesOption("Lainnya", "assets/img/other.png")
    );

    private final transient AppWindow app;

    private final JLabel inlineErrorLabel;
    private final JTabbedPane tabs;

    // Catalog tab state & widgets
    private final JTextField searchField;
    private final transient List<UiKit.PillFilterButton> speciesButtons = new ArrayList<>();
    private final JLabel catalogCountLabel;
    private final ResponsiveCardGrid catalogGrid;
    private final transient List<AnimalCard> animalCards = new ArrayList<>();
    private String activeSpeciesFilter = "Semua";

    private final PhotoPanel inspectorPhoto;
    private final JLabel inspectorNameLabel;
    private final WorkspaceWidgets.DynamicStatusPill inspectorStatusPill;
    private final JLabel inspectorIdValue;
    private final JLabel inspectorSpeciesValue;
    private final JLabel inspectorBreedValue;
    private final JLabel inspectorGenderValue;
    private final JLabel inspectorAgeValue;
    private final JTextArea inspectorDescArea;
    private final JLabel inspectorHintLabel;
    private final JButton adoptButton;

    // History tab state & widgets
    private final JLabel historyCountLabel;
    private final JButton cancelRequestButton;
    private final WorkspaceWidgets.ReadOnlyTableModel historyModel;
    private final JTable historyTable;
    private final JLabel historyDetailTitle;
    private final JTextArea historyReasonArea;
    private final JTextArea historyAdminNoteArea;

    private transient List<Animal> allAnimals = new ArrayList<>();
    private transient List<Animal> filteredAnimals = new ArrayList<>();
    private transient List<AdoptionRequest> userRequests = new ArrayList<>();
    private transient Animal selectedAnimal;
    private transient AdoptionRequest selectedRequest;
    private String statusSummary = "Ruang kerja adopsi siap.";

    public AdoptionWorkspacePanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineErrorLabel = WorkspaceWidgets.createFeedbackBanner();
        add(inlineErrorLabel, BorderLayout.NORTH);

        // --- Tab 1: Katalog Hewan ---
        searchField = UiKit.searchField("Cari nama, ras, atau deskripsi...");
        searchField.setPreferredSize(new Dimension(280, 36));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyCatalogFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyCatalogFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyCatalogFilter();
            }
        });
        searchField.addActionListener(e -> applyCatalogFilter());

        catalogCountLabel = UiKit.label("0 hewan", Theme.smallBold(), Theme.MUTED);

        JPanel searchRow = new JPanel(new BorderLayout(Theme.SPACE_12, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(catalogCountLabel, BorderLayout.EAST);

        JPanel speciesRow = new JPanel(WorkspaceWidgets.wrapFlowLayout(Theme.SPACE_8, Theme.SPACE_4));
        speciesRow.setOpaque(false);
        for (SpeciesOption opt : SPECIES_OPTIONS) {
            UiKit.PillFilterButton pill = UiKit.pillFilterButton(opt.label(), loadSpeciesIcon(opt.iconPath()));
            if ("Semua".equals(opt.label())) {
                pill.setSelectedPill(true);
            }
            pill.addActionListener(e -> selectSpeciesFilter(opt.label()));
            speciesButtons.add(pill);
            speciesRow.add(pill);
        }

        JPanel catalogToolbar = UiKit.cardPanel(new BorderLayout(0, Theme.SPACE_8));
        catalogToolbar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        catalogToolbar.add(searchRow, BorderLayout.NORTH);
        catalogToolbar.add(speciesRow, BorderLayout.SOUTH);

        catalogGrid = new ResponsiveCardGrid(185, 12, 12);
        catalogGrid.setEmptyState(
            "Tidak ada hewan ditemukan",
            "Coba ubah filter spesies atau kata kunci pencarian."
        );
        JScrollPane catalogScroll = WorkspaceWidgets.verticalScrollPane(catalogGrid, Theme.BG);
        catalogScroll.setMinimumSize(new Dimension(415, 260));
        catalogScroll.setPreferredSize(new Dimension(430, 420));

        inspectorPhoto = new PhotoPanel(260, 170, Theme.CARD_RADIUS);
        inspectorNameLabel = UiKit.label("Pilih Hewan", Theme.heading(), Theme.INK);
        inspectorStatusPill = new WorkspaceWidgets.DynamicStatusPill();
        inspectorIdValue = UiKit.label("-", Theme.bodyBold(), Theme.INK);
        inspectorSpeciesValue = UiKit.label("-", Theme.body(), Theme.INK);
        inspectorBreedValue = UiKit.label("-", Theme.body(), Theme.INK);
        inspectorGenderValue = UiKit.label("-", Theme.body(), Theme.INK);
        inspectorAgeValue = UiKit.label("-", Theme.body(), Theme.INK);

        inspectorDescArea = WorkspaceWidgets.createReadOnlyTextArea(4, Theme.body(), Theme.SURFACE_ALT);
        inspectorDescArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        inspectorHintLabel = UiKit.label(
            "<html>Pilih salah satu hewan di katalog untuk melihat detail dan mengajukan adopsi.</html>",
            Theme.small(),
            Theme.MUTED
        );
        adoptButton = UiKit.primaryButton("Ajukan Adopsi", VectorIcons.Kind.ADOPTION);
        adoptButton.setEnabled(false);
        adoptButton.addActionListener(e -> openAdoptionFormForSelected());

        JPanel inspectorCard = buildInspectorCard();

        JSplitPane catalogSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, catalogScroll, inspectorCard);
        catalogSplit.setResizeWeight(0.68);
        catalogSplit.setDividerSize(8);
        catalogSplit.setBorder(BorderFactory.createEmptyBorder());
        catalogSplit.setContinuousLayout(true);
        catalogSplit.setOpaque(false);

        JPanel catalogTab = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        catalogTab.setOpaque(false);
        catalogTab.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, 0, 0, 0));
        catalogTab.add(catalogToolbar, BorderLayout.NORTH);
        catalogTab.add(catalogSplit, BorderLayout.CENTER);

        // --- Tab 2: Riwayat Pengajuan ---
        historyCountLabel = UiKit.label("0 pengajuan", Theme.smallBold(), Theme.MUTED);
        cancelRequestButton = UiKit.dangerButton("Batalkan Pengajuan", VectorIcons.Kind.CLOSE);
        cancelRequestButton.setEnabled(false);
        cancelRequestButton.addActionListener(e -> cancelSelectedRequest());

        JPanel historyToolbar = UiKit.cardPanel(new BorderLayout(Theme.SPACE_12, 0));
        historyToolbar.add(historyCountLabel, BorderLayout.WEST);
        historyToolbar.add(cancelRequestButton, BorderLayout.EAST);

        historyModel = new WorkspaceWidgets.ReadOnlyTableModel(
            new Object[] { "ID", "Hewan", "Jenis / Ras", "Tanggal", "Status", "Catatan Admin" },
            new Class<?>[] { Integer.class, String.class, String.class, String.class, String.class, String.class }
        );
        historyTable = UiKit.table(historyModel);
        historyTable.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
        configureHistoryColumns();
        historyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onHistorySelectionChanged();
            }
        });

        historyDetailTitle = UiKit.label("Detail Pengajuan Terpilih", Theme.smallBold(), Theme.INK);
        historyReasonArea = WorkspaceWidgets.createReadOnlyTextArea(2, Theme.small(), Theme.SURFACE_ALT);
        historyAdminNoteArea = WorkspaceWidgets.createReadOnlyTextArea(2, Theme.small(), Theme.SURFACE_ALT);

        JPanel historyDetailCard = buildHistoryDetailCard();

        JPanel historyTab = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        historyTab.setOpaque(false);
        historyTab.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, 0, 0, 0));
        historyTab.add(historyToolbar, BorderLayout.NORTH);
        historyTab.add(UiKit.scrollPane(historyTable), BorderLayout.CENTER);
        historyTab.add(historyDetailCard, BorderLayout.SOUTH);

        tabs = new JTabbedPane();
        tabs.setFont(Theme.bodyBold());
        tabs.setBackground(Theme.SURFACE);
        tabs.setForeground(Theme.INK);
        tabs.addTab("Katalog Hewan", catalogTab);
        tabs.addTab("Riwayat Pengajuan", historyTab);

        add(tabs, BorderLayout.CENTER);
    }

    public int getDisplayedAnimalCardCount() {
        return catalogGrid.getCardCount();
    }

    public int getCatalogColumnCount() {
        return catalogGrid.getColumnCount();
    }

    private static Icon loadSpeciesIcon(String path) {
        BufferedImage img = ImageStore.loadScaledFit(path, 18, 18, false);
        return img != null ? new ImageIcon(img) : VectorIcons.of(VectorIcons.Kind.PAW, 16, Theme.INK);
    }

    private void selectSpeciesFilter(String speciesLabel) {
        this.activeSpeciesFilter = speciesLabel != null ? speciesLabel : "Semua";
        for (UiKit.PillFilterButton btn : speciesButtons) {
            btn.setSelectedPill(this.activeSpeciesFilter.equals(btn.getText()));
        }
        applyCatalogFilter();
    }

    private JPanel buildInspectorCard() {
        JPanel card = UiKit.cardPanel(new BorderLayout(0, Theme.SPACE_12));
        card.setPreferredSize(new Dimension(300, 0));
        card.setMinimumSize(new Dimension(265, 0));

        JPanel photoWrap = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout());
        photoWrap.add(inspectorPhoto, BorderLayout.CENTER);

        JPanel pillSlot = new JPanel(new GridBagLayout());
        pillSlot.setOpaque(false);
        pillSlot.add(inspectorStatusPill);

        JPanel titleRow = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout(Theme.SPACE_8, 0));
        titleRow.add(inspectorNameLabel, BorderLayout.CENTER);
        titleRow.add(pillSlot, BorderLayout.EAST);

        JPanel attrsGrid = new WorkspaceWidgets.CompactSectionPanel(new GridBagLayout());
        WorkspaceWidgets.addAttrRow(attrsGrid, 0, "ID Hewan", inspectorIdValue);
        WorkspaceWidgets.addAttrRow(attrsGrid, 1, "Jenis", inspectorSpeciesValue);
        WorkspaceWidgets.addAttrRow(attrsGrid, 2, "Ras", inspectorBreedValue);
        WorkspaceWidgets.addAttrRow(attrsGrid, 3, "Kelamin", inspectorGenderValue);
        WorkspaceWidgets.addAttrRow(attrsGrid, 4, "Usia", inspectorAgeValue);

        JPanel descWrap = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout(0, 6));
        descWrap.add(UiKit.formLabel("Deskripsi & Karakter"), BorderLayout.NORTH);
        JScrollPane descScroll = UiKit.scrollPane(inspectorDescArea);
        descScroll.setPreferredSize(new Dimension(240, 96));
        descWrap.add(descScroll, BorderLayout.CENTER);

        JPanel bodyStack = new JPanel();
        bodyStack.setLayout(new BoxLayout(bodyStack, BoxLayout.Y_AXIS));
        bodyStack.setOpaque(false);
        photoWrap.setAlignmentX(LEFT_ALIGNMENT);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);
        attrsGrid.setAlignmentX(LEFT_ALIGNMENT);
        descWrap.setAlignmentX(LEFT_ALIGNMENT);
        bodyStack.add(photoWrap);
        bodyStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        bodyStack.add(titleRow);
        bodyStack.add(Box.createVerticalStrut(Theme.SPACE_8));
        bodyStack.add(attrsGrid);
        bodyStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        bodyStack.add(descWrap);

        JPanel footer = new JPanel(new BorderLayout(0, Theme.SPACE_8));
        footer.setOpaque(false);
        footer.add(inspectorHintLabel, BorderLayout.NORTH);
        footer.add(adoptButton, BorderLayout.SOUTH);

        card.add(WorkspaceWidgets.verticalScrollPane(bodyStack), BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildHistoryDetailCard() {
        JPanel card = UiKit.cardPanel(new BorderLayout(0, Theme.SPACE_8));

        JPanel columns = new JPanel(new GridLayout(1, 2, Theme.SPACE_12, 0));
        columns.setOpaque(false);

        JPanel reasonCol = new JPanel(new BorderLayout(0, 4));
        reasonCol.setOpaque(false);
        reasonCol.add(UiKit.formLabel("Alasan Pengajuan"), BorderLayout.NORTH);
        JScrollPane reasonScroll = UiKit.scrollPane(historyReasonArea);
        reasonScroll.setPreferredSize(new Dimension(240, 64));
        reasonCol.add(reasonScroll, BorderLayout.CENTER);

        JPanel noteCol = new JPanel(new BorderLayout(0, 4));
        noteCol.setOpaque(false);
        noteCol.add(UiKit.formLabel("Catatan Admin"), BorderLayout.NORTH);
        JScrollPane noteScroll = UiKit.scrollPane(historyAdminNoteArea);
        noteScroll.setPreferredSize(new Dimension(240, 64));
        noteCol.add(noteScroll, BorderLayout.CENTER);

        columns.add(reasonCol);
        columns.add(noteCol);

        card.add(historyDetailTitle, BorderLayout.NORTH);
        card.add(columns, BorderLayout.CENTER);
        return card;
    }

    private void configureHistoryColumns() {
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(145);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(115);
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(220);
    }

    @Override
    public String getPageKey() {
        return "user.adoption";
    }

    @Override
    public String getPageTitle() {
        return "Adopsi Hewan";
    }

    @Override
    public String getPageSubtitle() {
        return "Katalog hewan adopsi dan riwayat pengajuan Anda";
    }

    @Override
    public void refreshData() {
        int prevAnimalId = selectedAnimal != null ? selectedAnimal.getAnimalId() : -1;
        int prevRequestId = selectedRequest != null ? selectedRequest.getAdoptionRequestId() : -1;
        WorkspaceWidgets.hideBanner(inlineErrorLabel);

        User user = app.getSession().getUser();
        try {
            allAnimals = new ArrayList<>(app.getAdoptionService().listAnimals());
            userRequests = user != null
                ? new ArrayList<>(app.getAdoptionService().listUserRequests(user.getUserId()))
                : new ArrayList<>();

            applyCatalogFilter(prevAnimalId);
            populateHistoryTable(prevRequestId);

            long availableCount = allAnimals.stream()
                .filter(a -> "available".equalsIgnoreCase(a.getStatus()))
                .count();
            if (user == null) {
                statusSummary = "Katalog adopsi: " + allAnimals.size()
                    + " hewan (" + availableCount + " tersedia). Belum ada sesi pengguna aktif.";
            } else {
                statusSummary = "Katalog adopsi: " + allAnimals.size()
                    + " hewan (" + availableCount + " tersedia), "
                    + userRequests.size() + " pengajuan Anda.";
            }
        } catch (RuntimeException ex) {
            allAnimals = new ArrayList<>();
            filteredAnimals = new ArrayList<>();
            animalCards.clear();
            catalogGrid.setCards(List.of());
            catalogCountLabel.setText("0 hewan");
            userRequests = new ArrayList<>();
            historyModel.setRowCount(0);
            clearCatalogInspector();
            clearHistoryInspector();
            String msg = WorkspaceWidgets.safeErrorMessage(ex);
            WorkspaceWidgets.showBanner(inlineErrorLabel, "Gagal memuat data adopsi: " + msg, true);
            statusSummary = "Gagal memuat data adopsi: " + msg;
        }
    }

    @Override
    public String getStatusSummary() {
        return statusSummary;
    }

    private void applyCatalogFilter() {
        int currentId = selectedAnimal != null ? selectedAnimal.getAnimalId() : -1;
        applyCatalogFilter(currentId);
    }

    private void applyCatalogFilter(int preferredAnimalId) {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase(Locale.ROOT);

        filteredAnimals = new ArrayList<>();
        animalCards.clear();
        for (Animal animal : allAnimals) {
            if (!matchesSpecies(animal, activeSpeciesFilter)) {
                continue;
            }
            if (!matchesQuery(animal, query)) {
                continue;
            }
            filteredAnimals.add(animal);
            animalCards.add(new AnimalCard(animal));
        }

        catalogGrid.setCards(animalCards);
        catalogCountLabel.setText(
            "Menampilkan " + filteredAnimals.size() + " dari " + allAnimals.size() + " hewan"
        );
        restoreCatalogSelection(preferredAnimalId);
    }

    private void restoreCatalogSelection(int preferredAnimalId) {
        if (filteredAnimals.isEmpty()) {
            clearCatalogInspector();
            return;
        }
        Animal target = filteredAnimals.get(0);
        if (preferredAnimalId > 0) {
            for (Animal candidate : filteredAnimals) {
                if (candidate.getAnimalId() == preferredAnimalId) {
                    target = candidate;
                    break;
                }
            }
        }
        selectAnimal(target);
    }

    private void selectAnimal(Animal animal) {
        if (animal == null) {
            clearCatalogInspector();
            return;
        }
        selectedAnimal = animal;
        for (AnimalCard card : animalCards) {
            card.setSelected(card.animal.getAnimalId() == animal.getAnimalId());
        }

        inspectorPhoto.setImagePath(animal.getImage());
        inspectorNameLabel.setText(WorkspaceWidgets.safeText(animal.getNama()));
        inspectorStatusPill.setStatus(animal.getStatus());
        inspectorIdValue.setText("#" + animal.getAnimalId());
        inspectorSpeciesValue.setText(WorkspaceWidgets.capitalizeWords(animal.getSpecies()));
        inspectorBreedValue.setText(WorkspaceWidgets.safeText(animal.getBreed()));
        inspectorGenderValue.setText(WorkspaceWidgets.safeText(animal.getGender()));
        inspectorAgeValue.setText(WorkspaceWidgets.safeText(animal.getAge()));
        String desc = (animal.getDescription() == null || animal.getDescription().isBlank())
            ? "Belum ada deskripsi tambahan."
            : animal.getDescription();
        inspectorDescArea.setText(desc);
        inspectorDescArea.setCaretPosition(0);

        boolean available = "available".equalsIgnoreCase(animal.getStatus());
        adoptButton.setEnabled(available);
        if (available) {
            inspectorHintLabel.setText("<html>Hewan ini tersedia dan siap menerima pengajuan adopsi.</html>");
        } else {
            inspectorHintLabel.setText(
                "<html>Status saat ini \"" + WorkspaceWidgets.escapeHtml(FormatUtil.statusLabel(animal.getStatus()))
                    + "\" - tidak dapat diajukan adopsi.</html>"
            );
        }
    }

    private void clearCatalogInspector() {
        selectedAnimal = null;
        for (AnimalCard card : animalCards) {
            card.setSelected(false);
        }
        inspectorPhoto.setImagePath(null);
        inspectorNameLabel.setText("Pilih Hewan");
        inspectorStatusPill.setStatus("");
        inspectorIdValue.setText("-");
        inspectorSpeciesValue.setText("-");
        inspectorBreedValue.setText("-");
        inspectorGenderValue.setText("-");
        inspectorAgeValue.setText("-");
        inspectorDescArea.setText("");
        adoptButton.setEnabled(false);
        inspectorHintLabel.setText("<html>Pilih salah satu hewan pada katalog untuk melihat detail.</html>");
    }

    private void populateHistoryTable(int preferredRequestId) {
        historyModel.setRowCount(0);
        for (AdoptionRequest req : userRequests) {
            String animalName = (req.getAnimalNama() != null && !req.getAnimalNama().isBlank())
                ? req.getAnimalNama()
                : "Hewan #" + req.getAnimalId();
            String species = WorkspaceWidgets.capitalizeWords(req.getAnimalSpecies());
            String breed = WorkspaceWidgets.safeText(req.getAnimalBreed());
            String speciesBreed = "-".equals(species) && "-".equals(breed)
                ? "-"
                : species + " / " + breed;
            String note = (req.getAdminNote() == null || req.getAdminNote().isBlank())
                ? "-"
                : req.getAdminNote();

            historyModel.addRow(new Object[] {
                Integer.valueOf(req.getAdoptionRequestId()),
                animalName,
                speciesBreed,
                FormatUtil.dateTimeId(req.getRequestDate()),
                WorkspaceWidgets.safeText(req.getStatus()),
                note
            });
        }
        historyCountLabel.setText(userRequests.size() + " pengajuan adopsi");
        restoreHistorySelection(preferredRequestId);
    }

    private void restoreHistorySelection(int preferredRequestId) {
        if (userRequests.isEmpty()) {
            clearHistoryInspector();
            return;
        }
        int targetModelRow = 0;
        if (preferredRequestId > 0) {
            for (int i = 0; i < userRequests.size(); i++) {
                if (userRequests.get(i).getAdoptionRequestId() == preferredRequestId) {
                    targetModelRow = i;
                    break;
                }
            }
        }
        int viewRow = historyTable.convertRowIndexToView(targetModelRow);
        if (viewRow >= 0 && viewRow < historyTable.getRowCount()) {
            historyTable.setRowSelectionInterval(viewRow, viewRow);
            onHistorySelectionChanged();
        } else {
            clearHistoryInspector();
        }
    }

    private void onHistorySelectionChanged() {
        int viewRow = historyTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= historyTable.getRowCount()) {
            clearHistoryInspector();
            return;
        }
        int modelRow = historyTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= userRequests.size()) {
            clearHistoryInspector();
            return;
        }
        AdoptionRequest req = userRequests.get(modelRow);
        selectedRequest = req;

        String animalLabel = (req.getAnimalNama() != null && !req.getAnimalNama().isBlank())
            ? req.getAnimalNama()
            : "Hewan #" + req.getAnimalId();
        historyDetailTitle.setText(
            "Pengajuan #" + req.getAdoptionRequestId() + " - " + animalLabel
                + " (" + FormatUtil.statusLabel(req.getStatus()) + ")"
        );
        historyReasonArea.setText(WorkspaceWidgets.safeText(req.getReason()));
        historyReasonArea.setCaretPosition(0);
        historyAdminNoteArea.setText(
            (req.getAdminNote() == null || req.getAdminNote().isBlank()) ? "-" : req.getAdminNote()
        );
        historyAdminNoteArea.setCaretPosition(0);

        boolean canCancel = "pending".equalsIgnoreCase(req.getStatus());
        cancelRequestButton.setEnabled(canCancel);
    }

    private void clearHistoryInspector() {
        selectedRequest = null;
        historyDetailTitle.setText("Detail Pengajuan Terpilih");
        historyReasonArea.setText("");
        historyAdminNoteArea.setText("");
        cancelRequestButton.setEnabled(false);
    }

    private void openAdoptionFormForSelected() {
        User user = app.getSession().getUser();
        if (user == null) {
            UiKit.showError(this, "Silakan masuk terlebih dahulu untuk mengajukan adopsi.");
            return;
        }
        if (selectedAnimal == null || !"available".equalsIgnoreCase(selectedAnimal.getStatus())) {
            return;
        }
        String animalName = WorkspaceWidgets.safeText(selectedAnimal.getNama());
        if (AdoptionFormDialog.show(app, selectedAnimal)) {
            refreshData();
            app.updateStatus(getStatusSummary());
            tabs.setSelectedIndex(1);
            UiKit.showInfo(
                this,
                "Pengajuan adopsi untuk " + animalName + " berhasil dikirim dan menunggu verifikasi admin."
            );
        }
    }

    private void cancelSelectedRequest() {
        User user = app.getSession().getUser();
        if (user == null) {
            UiKit.showError(this, "Silakan masuk terlebih dahulu.");
            return;
        }
        if (selectedRequest == null || !"pending".equalsIgnoreCase(selectedRequest.getStatus())) {
            return;
        }
        String animalName = (selectedRequest.getAnimalNama() != null && !selectedRequest.getAnimalNama().isBlank())
            ? selectedRequest.getAnimalNama()
            : "Hewan #" + selectedRequest.getAnimalId();
        boolean confirmed = UiKit.confirm(
            this,
            "Batalkan Pengajuan Adopsi",
            "Yakin ingin membatalkan pengajuan adopsi #" + selectedRequest.getAdoptionRequestId()
                + " untuk " + animalName + "?"
        );
        if (!confirmed) {
            return;
        }
        try {
            app.getAdoptionService().cancelUserRequest(user.getUserId(), selectedRequest.getAdoptionRequestId());
            refreshData();
            app.updateStatus(getStatusSummary());
            UiKit.showInfo(this, "Pengajuan adopsi berhasil dibatalkan.");
        } catch (RuntimeException ex) {
            UiKit.showError(this, WorkspaceWidgets.safeErrorMessage(ex));
        }
    }

    private static boolean matchesSpecies(Animal animal, String speciesFilter) {
        if ("Semua".equalsIgnoreCase(speciesFilter)) {
            return true;
        }
        String raw = animal.getSpecies() == null ? "" : animal.getSpecies().trim().toLowerCase(Locale.ROOT);
        String target = speciesFilter.trim().toLowerCase(Locale.ROOT);
        if ("lainnya".equals(target)) {
            return "lainnya".equals(raw)
                || (!"kucing".equals(raw) && !"anjing".equals(raw) && !"kelinci".equals(raw));
        }
        return target.equals(raw);
    }

    private static boolean matchesQuery(Animal animal, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return WorkspaceWidgets.containsIgnoreCase(animal.getNama(), query)
            || WorkspaceWidgets.containsIgnoreCase(animal.getSpecies(), query)
            || WorkspaceWidgets.containsIgnoreCase(animal.getBreed(), query)
            || WorkspaceWidgets.containsIgnoreCase(animal.getGender(), query)
            || WorkspaceWidgets.containsIgnoreCase(animal.getAge(), query)
            || WorkspaceWidgets.containsIgnoreCase(animal.getDescription(), query);
    }

    /**
     * Website-styled adoptable animal card rendered inside {@link ResponsiveCardGrid}.
     */
    private final class AnimalCard extends JPanel {
        private static final long serialVersionUID = 1L;
        private final transient Animal animal;
        private boolean selected;
        private boolean hovered;

        AnimalCard(Animal animal) {
            super(new BorderLayout(0, 8));
            this.animal = animal;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 12, 10));

            PhotoPanel photo = new PhotoPanel(160, 136, 14);
            photo.setImagePath(animal.getImage());
            photo.setPreferredSize(new Dimension(160, 136));
            photo.setMinimumSize(new Dimension(120, 120));

            WorkspaceWidgets.DynamicStatusPill statusPill = new WorkspaceWidgets.DynamicStatusPill();
            statusPill.setStatus(animal.getStatus());

            JPanel pillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            pillRow.setOpaque(false);
            pillRow.add(statusPill);

            JLabel nameLabel = UiKit.label(
                WorkspaceWidgets.safeText(animal.getNama()),
                Theme.heading(),
                Theme.INK
            );
            String speciesBreed = WorkspaceWidgets.capitalizeWords(animal.getSpecies())
                + " \u2022 " + WorkspaceWidgets.safeText(animal.getBreed());
            JLabel speciesLabel = UiKit.label(speciesBreed, Theme.small(), Theme.MUTED);
            String genderAge = WorkspaceWidgets.safeText(animal.getGender())
                + " \u2022 " + WorkspaceWidgets.safeText(animal.getAge());
            JLabel ageLabel = UiKit.label(genderAge, Theme.badge(), Theme.MUTED);
            setToolTipText(WorkspaceWidgets.safeText(animal.getNama()) + " - " + speciesBreed);

            JPanel infoStack = new JPanel();
            infoStack.setLayout(new BoxLayout(infoStack, BoxLayout.Y_AXIS));
            infoStack.setOpaque(false);
            pillRow.setAlignmentX(LEFT_ALIGNMENT);
            nameLabel.setAlignmentX(LEFT_ALIGNMENT);
            speciesLabel.setAlignmentX(LEFT_ALIGNMENT);
            ageLabel.setAlignmentX(LEFT_ALIGNMENT);

            infoStack.add(pillRow);
            infoStack.add(Box.createVerticalStrut(6));
            infoStack.add(nameLabel);
            infoStack.add(Box.createVerticalStrut(2));
            infoStack.add(speciesLabel);
            infoStack.add(Box.createVerticalStrut(2));
            infoStack.add(ageLabel);

            add(photo, BorderLayout.NORTH);
            add(infoStack, BorderLayout.CENTER);

            MouseAdapter clickAndHover = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        selectAnimal(animal);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            };
            attachMouseHandlerRecursively(this, clickAndHover);
        }

        void setSelected(boolean selected) {
            if (this.selected != selected) {
                this.selected = selected;
                repaint();
            }
        }

        private void attachMouseHandlerRecursively(Component comp, MouseAdapter handler) {
            comp.addMouseListener(handler);
            if (comp instanceof Container container) {
                for (Component child : container.getComponents()) {
                    attachMouseHandlerRecursively(child, handler);
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w > 3 && h > 3) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    double arc = Theme.CARD_RADIUS * 2.0;
                    double cardW = Math.max(1.0, w - 3.0);
                    double cardH = Math.max(1.0, h - 3.0);

                    g2.setColor(Theme.tint(Theme.NAVY, selected ? 26 : (hovered ? 20 : 12)));
                    g2.fill(new RoundRectangle2D.Double(1.5, 2.5, cardW, cardH, arc, arc));

                    g2.setColor(hovered && !selected ? Theme.SURFACE_ALT : Theme.SURFACE);
                    g2.fill(new RoundRectangle2D.Double(0.5, 0.5, cardW, cardH, arc, arc));

                    if (selected) {
                        g2.setStroke(new BasicStroke(2.2f));
                        g2.setColor(Theme.BLUE);
                        g2.draw(new RoundRectangle2D.Double(1.2, 1.2, Math.max(1.0, w - 3.4), Math.max(1.0, h - 3.4), arc, arc));
                    } else {
                        g2.setStroke(new BasicStroke(1.0f));
                        g2.setColor(hovered ? Theme.BORDER : Theme.BORDER_SUBTLE);
                        g2.draw(new RoundRectangle2D.Double(0.5, 0.5, cardW, cardH, arc, arc));
                    }
                } finally {
                    g2.dispose();
                }
            }
            super.paintComponent(g);
        }
    }
}
