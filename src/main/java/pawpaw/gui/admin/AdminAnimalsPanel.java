package pawpaw.gui.admin;

import pawpaw.config.AppConfig;
import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.IdPillRenderer;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.gui.common.StatusBadgeRenderer;
import pawpaw.gui.common.UiKit;
import pawpaw.model.Animal;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Admin master workspace for adoption catalog animals: search, species/status filters,
 * dense sortable table with status badges, photo preview bar, modal create/edit dialog, and delete.
 */
public final class AdminAnimalsPanel extends PagePanel {

    private static final long serialVersionUID = 1L;
    private static final String[] SPECIES_FILTERS = {
        "Semua", "Kucing", "Anjing", "Kelinci", "Lainnya"
    };
    private static final String[] STATUS_FILTERS = {
        "Semua", "Tersedia", "Sudah Diadopsi", "Tidak Tersedia"
    };
    private static final String[] FORM_SPECIES = {
        "Kucing", "Anjing", "Kelinci", "Lainnya"
    };
    private static final String[] FORM_GENDERS = {
        "Jantan", "Betina"
    };
    private static final AdminWidgets.StatusOption[] FORM_STATUS_OPTIONS = {
        new AdminWidgets.StatusOption("available", "Tersedia"),
        new AdminWidgets.StatusOption("adopted", "Sudah Diadopsi"),
        new AdminWidgets.StatusOption("unavailable", "Tidak Tersedia")
    };

    private final transient AppWindow app;

    private final JLabel inlineErrorLabel;
    private final JTextField searchField;
    private final JComboBox<String> speciesCombo;
    private final JComboBox<String> statusCombo;
    private final JLabel countLabel;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JButton refreshButton;

    private final AdminWidgets.ReadOnlyTableModel animalModel;
    private final JTable animalTable;

    private final PhotoPanel previewPhoto;
    private final JLabel previewTitleLabel;
    private final JLabel previewMetaLabel;
    private final JTextArea previewDescArea;

    private transient List<Animal> allAnimals = new ArrayList<>();
    private transient List<Animal> filteredAnimals = new ArrayList<>();
    private transient Animal selectedAnimal;
    private String statusSummary = "Manajemen hewan adopsi siap.";

    public AdminAnimalsPanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineErrorLabel = AdminWidgets.createFeedbackBanner();

        searchField = UiKit.searchField("Cari nama, ras, atau deskripsi...");
        searchField.setPreferredSize(new Dimension(180, 36));
        AdminWidgets.wireTextChange(searchField, this::applyFilter);

        speciesCombo = AdminWidgets.adminComboBox(SPECIES_FILTERS);
        speciesCombo.addActionListener(e -> applyFilter());

        statusCombo = AdminWidgets.adminComboBox(STATUS_FILTERS);
        statusCombo.addActionListener(e -> applyFilter());

        countLabel = UiKit.label("0 hewan", Theme.smallBold(), Theme.Admin.TEXT_SOFT);

        addButton = UiKit.adminPrimaryButton("+ Tambah Hewan", VectorIcons.Kind.PLUS);
        addButton.addActionListener(e -> openCreateDialog());

        editButton = UiKit.adminEditButton("Edit", VectorIcons.Kind.EDIT);
        editButton.setEnabled(false);
        editButton.addActionListener(e -> openEditDialog());

        deleteButton = UiKit.adminDeleteButton("Hapus", VectorIcons.Kind.TRASH);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedAnimal());

        refreshButton = UiKit.adminLightButton("Refresh", VectorIcons.Kind.REFRESH);
        refreshButton.addActionListener(e -> {
            refreshData();
            app.updateStatus(getStatusSummary());
        });

        JPanel row1Left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0));
        row1Left.setOpaque(false);
        row1Left.add(searchField);
        row1Left.add(AdminWidgets.adminFormLabel("Jenis:"));
        row1Left.add(speciesCombo);
        row1Left.add(AdminWidgets.adminFormLabel("Status:"));
        row1Left.add(statusCombo);

        JPanel toolbar = AdminWidgets.buildMasterToolbar(
            row1Left, countLabel, addButton, editButton, deleteButton, refreshButton
        );

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setOpaque(false);
        inlineErrorLabel.setAlignmentX(LEFT_ALIGNMENT);
        toolbar.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(inlineErrorLabel);
        northStack.add(toolbar);

        animalModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] { "ID", "Nama", "Jenis", "Ras", "Gender", "Umur", "Status" },
            new Class<?>[] {
                Integer.class, String.class, String.class, String.class, String.class, String.class, String.class
            }
        );
        animalTable = UiKit.adminTable(animalModel);
        animalTable.getColumnModel().getColumn(0).setCellRenderer(new IdPillRenderer());
        animalTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        configureColumns();
        animalTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });
        if (animalTable.getRowSorter() != null) {
            animalTable.getRowSorter().addRowSorterListener(e -> onSelectionChanged());
        }
        AdminWidgets.attachDoubleClick(animalTable, this::openEditDialog);

        previewPhoto = new PhotoPanel(64, 64, Theme.Admin.BUTTON_RADIUS);
        previewTitleLabel = UiKit.label("Pilih hewan pada tabel", Theme.bodyBold(), Theme.Admin.TEXT);
        previewMetaLabel = UiKit.label("-", Theme.small(), Theme.Admin.TEXT_SOFT);
        previewDescArea = AdminWidgets.createReadOnlyTextArea(2);

        JPanel previewCard = buildPreviewCard();

        JPanel centerHost = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        centerHost.setOpaque(false);
        centerHost.add(AdminWidgets.adminTableCard(animalTable), BorderLayout.CENTER);
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

        card.add(previewPhoto, BorderLayout.WEST);
        card.add(infoCol, BorderLayout.CENTER);
        return card;
    }

    private void configureColumns() {
        animalTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        animalTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        animalTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        animalTable.getColumnModel().getColumn(3).setPreferredWidth(135);
        animalTable.getColumnModel().getColumn(4).setPreferredWidth(85);
        animalTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        animalTable.getColumnModel().getColumn(6).setPreferredWidth(130);
    }

    public static boolean openAnimalDialog(AppWindow app, Animal existing) {
        if (app == null || !app.getSession().isAdmin()) {
            return false;
        }
        AnimalFormDialog dialog = new AnimalFormDialog(app, existing);
        dialog.setVisible(true);
        return dialog.isSaved();
    }

    @Override
    public String getPageKey() {
        return "admin.animals";
    }

    @Override
    public String getPageTitle() {
        return "Manajemen Hewan Adopsi";
    }

    @Override
    public String getPageSubtitle() {
        return "Kelola data hewan adopsi, foto, deskripsi, dan status ketersediaan";
    }

    @Override
    public void refreshData() {
        int prevId = selectedAnimal != null ? selectedAnimal.getAnimalId() : -1;
        AdminWidgets.hideBanner(inlineErrorLabel);

        if (!isAdminActive()) {
            allAnimals = new ArrayList<>();
            filteredAnimals = new ArrayList<>();
            animalModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 hewan");
            addButton.setEnabled(false);
            statusSummary = "Sesi admin tidak aktif.";
            return;
        }

        addButton.setEnabled(true);
        try {
            allAnimals = new ArrayList<>(app.getAdoptionService().listAnimals());
            applyFilter(prevId);
            long available = allAnimals.stream()
                .filter(a -> "available".equalsIgnoreCase(a.getStatus()))
                .count();
            statusSummary = "Katalog hewan adopsi: " + allAnimals.size() + " hewan (" + available + " tersedia).";
        } catch (RuntimeException ex) {
            allAnimals = new ArrayList<>();
            filteredAnimals = new ArrayList<>();
            animalModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 hewan");
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineErrorLabel, "Gagal memuat data hewan adopsi: " + msg, true);
            statusSummary = "Gagal memuat hewan adopsi: " + msg;
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
        int currentId = selectedAnimal != null ? selectedAnimal.getAnimalId() : -1;
        applyFilter(currentId);
    }

    private void applyFilter(int preferredAnimalId) {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Object speciesObj = speciesCombo.getSelectedItem();
        String speciesFilter = speciesObj == null ? "Semua" : speciesObj.toString();
        Object statusObj = statusCombo.getSelectedItem();
        String statusFilter = statusObj == null ? "Semua" : statusObj.toString();

        filteredAnimals = new ArrayList<>();
        animalModel.setRowCount(0);

        for (Animal animal : allAnimals) {
            if (!matchesSpecies(animal, speciesFilter)
                    || !matchesStatus(animal, statusFilter)
                    || !matchesQuery(animal, query)) {
                continue;
            }
            filteredAnimals.add(animal);
            animalModel.addRow(new Object[] {
                Integer.valueOf(animal.getAnimalId()),
                AdminWidgets.safeText(animal.getNama()),
                AdminWidgets.capitalizeWords(animal.getSpecies()),
                AdminWidgets.safeText(animal.getBreed()),
                AdminWidgets.safeText(animal.getGender()),
                AdminWidgets.safeText(animal.getAge()),
                AdminWidgets.safeText(animal.getStatus())
            });
        }

        countLabel.setText(filteredAnimals.size() + " / " + allAnimals.size() + " hewan");
        restoreSelection(preferredAnimalId);
    }

    private void restoreSelection(int preferredAnimalId) {
        if (filteredAnimals.isEmpty()) {
            clearSelection();
            return;
        }
        int targetModelRow = 0;
        if (preferredAnimalId > 0) {
            for (int i = 0; i < filteredAnimals.size(); i++) {
                if (filteredAnimals.get(i).getAnimalId() == preferredAnimalId) {
                    targetModelRow = i;
                    break;
                }
            }
        }
        int viewRow = animalTable.convertRowIndexToView(targetModelRow);
        if (viewRow >= 0 && viewRow < animalTable.getRowCount()) {
            animalTable.setRowSelectionInterval(viewRow, viewRow);
            onSelectionChanged();
        } else {
            clearSelection();
        }
    }

    private Animal resolveSelectedAnimal() {
        int viewRow = animalTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= animalTable.getRowCount()) {
            return null;
        }
        int modelRow = animalTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= filteredAnimals.size()) {
            return null;
        }
        return filteredAnimals.get(modelRow);
    }

    private void onSelectionChanged() {
        Animal animal = resolveSelectedAnimal();
        if (animal == null) {
            clearSelection();
            return;
        }
        selectedAnimal = animal;

        previewPhoto.setImagePath(animal.getImage());
        previewTitleLabel.setText(
            "#" + animal.getAnimalId() + " — " + AdminWidgets.safeText(animal.getNama())
        );
        previewMetaLabel.setText(
            AdminWidgets.capitalizeWords(animal.getSpecies())
                + "  |  " + AdminWidgets.safeText(animal.getBreed())
                + "  |  " + AdminWidgets.safeText(animal.getGender())
                + "  |  " + AdminWidgets.safeText(animal.getAge())
                + "  |  Status: " + FormatUtil.statusLabel(animal.getStatus())
        );
        String desc = (animal.getDescription() == null || animal.getDescription().isBlank())
            ? "Belum ada deskripsi hewan."
            : animal.getDescription();
        previewDescArea.setText(desc);
        previewDescArea.setCaretPosition(0);

        boolean enabled = isAdminActive();
        editButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    private void clearSelection() {
        selectedAnimal = null;
        previewPhoto.setImagePath(null);
        previewTitleLabel.setText("Pilih hewan pada tabel");
        previewMetaLabel.setText("-");
        previewDescArea.setText("");
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void openCreateDialog() {
        if (!isAdminActive()) {
            return;
        }
        if (openAnimalDialog(app, null)) {
            refreshData();
            app.updateStatus("Data hewan adopsi baru berhasil ditambahkan.");
        }
    }

    private void openEditDialog() {
        Animal target = resolveSelectedAnimal();
        if (!isAdminActive() || target == null) {
            return;
        }
        int targetId = target.getAnimalId();
        if (openAnimalDialog(app, target)) {
            refreshData();
            restoreSelection(targetId);
            app.updateStatus("Data hewan adopsi #" + targetId + " berhasil diperbarui.");
        }
    }

    private void deleteSelectedAnimal() {
        Animal target = resolveSelectedAnimal();
        if (!isAdminActive() || target == null) {
            return;
        }
        boolean confirmed = UiKit.confirm(
            this,
            "Hapus Hewan Adopsi",
            "Yakin ingin menghapus data hewan \"" + AdminWidgets.safeText(target.getNama()) + "\"?"
        );
        if (!confirmed) {
            return;
        }
        try {
            app.getAdoptionService().deleteAnimal(target.getAnimalId());
            refreshData();
            statusSummary = "Data hewan \"" + AdminWidgets.safeText(target.getNama()) + "\" berhasil dihapus.";
            app.updateStatus(statusSummary);
        } catch (RuntimeException ex) {
            String msg = AdminWidgets.safeErrorMessage(ex);
            UiKit.showError(this, msg);
            app.updateStatus("Gagal menghapus hewan: " + msg);
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

    private static boolean matchesStatus(Animal animal, String statusFilter) {
        if ("Semua".equalsIgnoreCase(statusFilter)) {
            return true;
        }
        String raw = animal.getStatus() == null ? "" : animal.getStatus().trim().toLowerCase(Locale.ROOT);
        return switch (statusFilter) {
            case "Tersedia" -> "available".equals(raw);
            case "Sudah Diadopsi" -> "adopted".equals(raw);
            case "Tidak Tersedia" -> "unavailable".equals(raw);
            default -> true;
        };
    }

    private static boolean matchesQuery(Animal animal, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return AdminWidgets.containsIgnoreCase(animal.getNama(), query)
            || AdminWidgets.containsIgnoreCase(animal.getSpecies(), query)
            || AdminWidgets.containsIgnoreCase(animal.getBreed(), query)
            || AdminWidgets.containsIgnoreCase(animal.getGender(), query)
            || AdminWidgets.containsIgnoreCase(animal.getAge(), query)
            || AdminWidgets.containsIgnoreCase(animal.getDescription(), query);
    }

    /**
     * Modal Create/Edit dialog for adoption catalog {@link Animal} records with photo upload
     * preview, width-tracking scrollable form body, and inline validation.
     */
    public static final class AnimalFormDialog extends JDialog {

        private static final long serialVersionUID = 1L;

        private final transient AppWindow app;
        private final transient Animal existing;

        private final JTextField nameField;
        private final JComboBox<String> speciesCombo;
        private final JTextField breedField;
        private final JComboBox<String> genderCombo;
        private final JTextField ageField;
        private final JComboBox<AdminWidgets.StatusOption> statusCombo;
        private final PhotoPanel photoPreview;
        private final JTextField imagePathField;
        private final JTextArea descriptionArea;
        private final JLabel inlineErrorLabel;

        private boolean saved;

        public AnimalFormDialog(AppWindow app, Animal existing) {
            super(
                app,
                existing == null ? "Tambah Hewan Adopsi" : "Edit Hewan Adopsi",
                ModalityType.APPLICATION_MODAL
            );
            this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");
            this.existing = existing;

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setResizable(true);

            nameField = UiKit.textField("Nama hewan adopsi");
            speciesCombo = AdminWidgets.adminComboBox(FORM_SPECIES);
            speciesCombo.setEditable(true);
            breedField = UiKit.textField("Contoh: Anggora, Domestik, Golden");
            genderCombo = AdminWidgets.adminComboBox(FORM_GENDERS);
            ageField = UiKit.textField("Contoh: 1 Tahun, 8 Bulan");
            statusCombo = AdminWidgets.adminComboBox(FORM_STATUS_OPTIONS);

            photoPreview = new PhotoPanel(84, 84, Theme.Admin.CARD_RADIUS);
            imagePathField = UiKit.textField(AppConfig.DEFAULT_ANIMAL_IMAGE);
            AdminWidgets.wireTextChange(imagePathField, () -> photoPreview.setImagePath(imagePathField.getText()));

            JButton choosePhotoButton = UiKit.adminLightButton("Pilih Foto", VectorIcons.Kind.IMAGE);
            choosePhotoButton.addActionListener(e -> chooseAnimalPhoto());

            JButton defaultPhotoButton = UiKit.adminLightButton("Default", null);
            defaultPhotoButton.addActionListener(e -> {
                imagePathField.setText(AppConfig.DEFAULT_ANIMAL_IMAGE);
                photoPreview.setImagePath(AppConfig.DEFAULT_ANIMAL_IMAGE);
            });

            descriptionArea = AdminWidgets.createEditableTextArea(3);
            inlineErrorLabel = AdminWidgets.createFeedbackBanner();

            prefillFields();

            JPanel photoActionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0));
            photoActionRow.setOpaque(false);
            photoActionRow.add(choosePhotoButton);
            photoActionRow.add(defaultPhotoButton);

            JPanel photoRight = new JPanel(new BorderLayout(0, Theme.SPACE_8));
            photoRight.setOpaque(false);
            photoRight.add(imagePathField, BorderLayout.NORTH);
            photoRight.add(photoActionRow, BorderLayout.SOUTH);

            JPanel photoRow = new JPanel(new BorderLayout(Theme.SPACE_12, 0));
            photoRow.setOpaque(false);
            photoRow.add(photoPreview, BorderLayout.WEST);
            photoRow.add(photoRight, BorderLayout.CENTER);

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
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Nama Hewan", nameField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Jenis Hewan", speciesCombo);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Ras", breedField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Jenis Kelamin", genderCombo);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Usia", ageField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Status Ketersediaan", statusCombo);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Foto Hewan", photoRow);
            AdminWidgets.addScrollAreaGroup(formCard, gbc, row++, "Deskripsi & Karakter", descriptionArea, 80);

            gbc.gridy = row;
            gbc.weighty = 1.0;
            gbc.insets = new Insets(4, 0, 0, 0);
            formCard.add(inlineErrorLabel, gbc);

            JButton cancelButton = UiKit.adminLightButton("Batal", VectorIcons.Kind.CLOSE);
            cancelButton.addActionListener(e -> dispose());

            JButton saveButton = UiKit.adminPrimaryButton("Simpan Hewan", VectorIcons.Kind.CHECK);
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
                speciesCombo.setSelectedItem("Kucing");
                genderCombo.setSelectedItem("Jantan");
                ageField.setText("1 Tahun");
                AdminWidgets.selectStatusOption(statusCombo, "available");
                imagePathField.setText(AppConfig.DEFAULT_ANIMAL_IMAGE);
                photoPreview.setImagePath(AppConfig.DEFAULT_ANIMAL_IMAGE);
                return;
            }
            nameField.setText(AdminWidgets.nonNull(existing.getNama()));
            selectSpecies(existing.getSpecies());
            breedField.setText(AdminWidgets.nonNull(existing.getBreed()));
            selectGender(existing.getGender());
            ageField.setText(AdminWidgets.nonNull(existing.getAge()));
            AdminWidgets.selectStatusOption(statusCombo, existing.getStatus());

            String img = (existing.getImage() == null || existing.getImage().isBlank())
                ? AppConfig.DEFAULT_ANIMAL_IMAGE
                : existing.getImage();
            imagePathField.setText(img);
            photoPreview.setImagePath(img);
            descriptionArea.setText(AdminWidgets.nonNull(existing.getDescription()));
            descriptionArea.setCaretPosition(0);
        }

        private void selectSpecies(String rawSpecies) {
            if (rawSpecies == null || rawSpecies.isBlank()) {
                speciesCombo.setSelectedItem("Kucing");
                return;
            }
            String trimmed = rawSpecies.trim();
            for (String item : FORM_SPECIES) {
                if (item.equalsIgnoreCase(trimmed)) {
                    speciesCombo.setSelectedItem(item);
                    return;
                }
            }
            speciesCombo.setSelectedItem(trimmed);
        }

        private void selectGender(String rawGender) {
            if (rawGender != null) {
                for (String item : FORM_GENDERS) {
                    if (item.equalsIgnoreCase(rawGender.trim())) {
                        genderCombo.setSelectedItem(item);
                        return;
                    }
                }
            }
            genderCombo.setSelectedItem("Jantan");
        }

        private void chooseAnimalPhoto() {
            String uploadedPath = UiKit.chooseAndCopyImage(this, "animals", "animal");
            if (uploadedPath != null && !uploadedPath.isBlank()) {
                imagePathField.setText(uploadedPath);
                photoPreview.setImagePath(uploadedPath);
            }
        }

        private void submitForm() {
            AdminWidgets.hideBanner(inlineErrorLabel);
            if (!app.getSession().isAdmin()) {
                AdminWidgets.showDialogError(
                    this,
                    inlineErrorLabel,
                    "Hanya administrator yang dapat menyimpan data hewan."
                );
                return;
            }

            Object speciesObj = speciesCombo.isEditable()
                ? speciesCombo.getEditor().getItem()
                : speciesCombo.getSelectedItem();
            String rawSpecies = speciesObj == null ? "" : speciesObj.toString().trim().toLowerCase(Locale.ROOT);
            Object genderObj = genderCombo.getSelectedItem();
            Object statusObj = statusCombo.getSelectedItem();
            String statusCode = statusObj instanceof AdminWidgets.StatusOption opt
                ? opt.code()
                : "available";

            Animal target = new Animal();
            if (existing != null) {
                target.setAnimalId(existing.getAnimalId());
            }
            target.setNama(nameField.getText());
            target.setSpecies(rawSpecies);
            target.setBreed(breedField.getText());
            target.setGender(genderObj == null ? "Jantan" : genderObj.toString());
            target.setAge(ageField.getText());
            target.setStatus(statusCode);
            target.setImage(imagePathField.getText());
            target.setDescription(descriptionArea.getText());

            try {
                if (existing == null) {
                    app.getAdoptionService().createAnimal(target);
                } else {
                    app.getAdoptionService().updateAnimal(target);
                }
                saved = true;
                dispose();
            } catch (RuntimeException ex) {
                AdminWidgets.showDialogError(this, inlineErrorLabel, AdminWidgets.safeErrorMessage(ex));
            }
        }
    }
}
