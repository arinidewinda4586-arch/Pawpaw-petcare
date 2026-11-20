package pawpaw.gui.admin;

import pawpaw.config.AppConfig;
import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.IdPillRenderer;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.gui.common.UiKit;
import pawpaw.model.Product;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Admin master workspace for managing shop products: search/category filter, dense sortable
 * table, live photo preview bar, modal create/edit dialog, and FK-guarded delete.
 */
public final class AdminProductsPanel extends PagePanel {

    private static final long serialVersionUID = 1L;
    private static final String[] CATEGORY_FILTERS = {
        "Semua", "Makanan", "Perawatan", "Mainan", "Perlengkapan"
    };
    private static final String[] FORM_CATEGORIES = {
        "Makanan", "Perawatan", "Mainan", "Perlengkapan"
    };

    private final transient AppWindow app;

    private final JLabel inlineErrorLabel;
    private final JTextField searchField;
    private final JComboBox<String> categoryCombo;
    private final JLabel countLabel;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JButton refreshButton;

    private final AdminWidgets.ReadOnlyTableModel productModel;
    private final JTable productTable;

    private final PhotoPanel previewPhoto;
    private final JLabel previewTitleLabel;
    private final JLabel previewMetaLabel;
    private final JTextArea previewDescArea;

    private transient List<Product> allProducts = new ArrayList<>();
    private transient List<Product> filteredProducts = new ArrayList<>();
    private transient Product selectedProduct;
    private String statusSummary = "Manajemen produk siap.";

    public AdminProductsPanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineErrorLabel = AdminWidgets.createFeedbackBanner();

        searchField = UiKit.searchField("Cari nama, slug, atau deskripsi...");
        searchField.setPreferredSize(new Dimension(205, 36));
        AdminWidgets.wireTextChange(searchField, this::applyFilter);

        categoryCombo = AdminWidgets.adminComboBox(CATEGORY_FILTERS);
        categoryCombo.addActionListener(e -> applyFilter());

        countLabel = UiKit.label("0 produk", Theme.smallBold(), Theme.Admin.TEXT_SOFT);

        addButton = UiKit.adminPrimaryButton("+ Tambah Produk", VectorIcons.Kind.PLUS);
        addButton.addActionListener(e -> openCreateDialog());

        editButton = UiKit.adminEditButton("Edit", VectorIcons.Kind.EDIT);
        editButton.setEnabled(false);
        editButton.addActionListener(e -> openEditDialog());

        deleteButton = UiKit.adminDeleteButton("Hapus", VectorIcons.Kind.TRASH);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedProduct());

        refreshButton = UiKit.adminLightButton("Refresh", VectorIcons.Kind.REFRESH);
        refreshButton.addActionListener(e -> {
            refreshData();
            app.updateStatus(getStatusSummary());
        });

        JPanel row1Left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0));
        row1Left.setOpaque(false);
        row1Left.add(searchField);
        row1Left.add(AdminWidgets.adminFormLabel("Kategori:"));
        row1Left.add(categoryCombo);

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

        productModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] { "ID", "Nama Produk", "Kategori", "Harga", "Stok", "Slug" },
            new Class<?>[] {
                Integer.class, String.class, String.class, BigDecimal.class, Integer.class, String.class
            }
        );
        productTable = UiKit.adminTable(productModel);
        productTable.getColumnModel().getColumn(0).setCellRenderer(new IdPillRenderer());
        productTable.getColumnModel().getColumn(3).setCellRenderer(new AdminWidgets.CurrencyCellRenderer());
        configureColumns();
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });
        if (productTable.getRowSorter() != null) {
            productTable.getRowSorter().addRowSorterListener(e -> onSelectionChanged());
        }
        AdminWidgets.attachDoubleClick(productTable, this::openEditDialog);

        previewPhoto = new PhotoPanel(64, 64, Theme.Admin.BUTTON_RADIUS);
        previewTitleLabel = UiKit.label("Pilih produk pada tabel", Theme.bodyBold(), Theme.Admin.TEXT);
        previewMetaLabel = UiKit.label("-", Theme.small(), Theme.Admin.TEXT_SOFT);
        previewDescArea = AdminWidgets.createReadOnlyTextArea(2);

        JPanel previewCard = buildPreviewCard();

        JPanel centerHost = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        centerHost.setOpaque(false);
        centerHost.add(AdminWidgets.adminTableCard(productTable), BorderLayout.CENTER);
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
        productTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(105);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(115);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(65);
        productTable.getColumnModel().getColumn(5).setPreferredWidth(145);
    }

    public static boolean openProductDialog(AppWindow app, Product existing) {
        if (app == null || !app.getSession().isAdmin()) {
            return false;
        }
        ProductFormDialog dialog = new ProductFormDialog(app, existing);
        dialog.setVisible(true);
        return dialog.isSaved();
    }

    @Override
    public String getPageKey() {
        return "admin.products";
    }

    @Override
    public String getPageTitle() {
        return "Manajemen Produk";
    }

    @Override
    public String getPageSubtitle() {
        return "Kelola katalog produk toko, kategori, harga, stok, dan foto";
    }

    @Override
    public void refreshData() {
        int prevId = selectedProduct != null ? selectedProduct.getProductId() : -1;
        AdminWidgets.hideBanner(inlineErrorLabel);

        if (!isAdminActive()) {
            allProducts = new ArrayList<>();
            filteredProducts = new ArrayList<>();
            productModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 produk");
            addButton.setEnabled(false);
            statusSummary = "Sesi admin tidak aktif.";
            return;
        }

        addButton.setEnabled(true);
        try {
            allProducts = new ArrayList<>(app.getShopService().listProducts());
            applyFilter(prevId);
            statusSummary = "Katalog produk dimuat: " + allProducts.size() + " produk.";
        } catch (RuntimeException ex) {
            allProducts = new ArrayList<>();
            filteredProducts = new ArrayList<>();
            productModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 produk");
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineErrorLabel, "Gagal memuat daftar produk: " + msg, true);
            statusSummary = "Gagal memuat produk: " + msg;
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
        int currentId = selectedProduct != null ? selectedProduct.getProductId() : -1;
        applyFilter(currentId);
    }

    private void applyFilter(int preferredProductId) {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Object catObj = categoryCombo.getSelectedItem();
        String catFilter = catObj == null ? "Semua" : catObj.toString();

        filteredProducts = new ArrayList<>();
        productModel.setRowCount(0);

        for (Product product : allProducts) {
            if (!matchesCategory(product, catFilter) || !matchesQuery(product, query)) {
                continue;
            }
            filteredProducts.add(product);
            productModel.addRow(new Object[] {
                Integer.valueOf(product.getProductId()),
                AdminWidgets.safeText(product.getProductName()),
                AdminWidgets.capitalizeWords(product.getCategory()),
                product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO,
                Integer.valueOf(product.getStock()),
                AdminWidgets.safeText(product.getSlug())
            });
        }

        countLabel.setText(filteredProducts.size() + " / " + allProducts.size() + " produk");
        restoreSelection(preferredProductId);
    }

    private void restoreSelection(int preferredProductId) {
        if (filteredProducts.isEmpty()) {
            clearSelection();
            return;
        }
        int targetModelRow = 0;
        if (preferredProductId > 0) {
            for (int i = 0; i < filteredProducts.size(); i++) {
                if (filteredProducts.get(i).getProductId() == preferredProductId) {
                    targetModelRow = i;
                    break;
                }
            }
        }
        int viewRow = productTable.convertRowIndexToView(targetModelRow);
        if (viewRow >= 0 && viewRow < productTable.getRowCount()) {
            productTable.setRowSelectionInterval(viewRow, viewRow);
            onSelectionChanged();
        } else {
            clearSelection();
        }
    }

    private Product resolveSelectedProduct() {
        int viewRow = productTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= productTable.getRowCount()) {
            return null;
        }
        int modelRow = productTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= filteredProducts.size()) {
            return null;
        }
        return filteredProducts.get(modelRow);
    }

    private void onSelectionChanged() {
        Product product = resolveSelectedProduct();
        if (product == null) {
            clearSelection();
            return;
        }
        selectedProduct = product;

        previewPhoto.setImagePath(product.getImage());
        previewTitleLabel.setText(
            "#" + product.getProductId() + " — " + AdminWidgets.safeText(product.getProductName())
        );
        previewMetaLabel.setText(
            AdminWidgets.capitalizeWords(product.getCategory())
                + "  |  " + FormatUtil.rupiah(product.getPrice())
                + "  |  Stok: " + product.getStock()
                + "  |  Slug: " + AdminWidgets.safeText(product.getSlug())
        );
        String desc = (product.getDescription() == null || product.getDescription().isBlank())
            ? "Belum ada deskripsi produk."
            : product.getDescription();
        previewDescArea.setText(desc);
        previewDescArea.setCaretPosition(0);

        boolean enabled = isAdminActive();
        editButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    private void clearSelection() {
        selectedProduct = null;
        previewPhoto.setImagePath(null);
        previewTitleLabel.setText("Pilih produk pada tabel");
        previewMetaLabel.setText("-");
        previewDescArea.setText("");
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void openCreateDialog() {
        if (!isAdminActive()) {
            return;
        }
        if (openProductDialog(app, null)) {
            refreshData();
            app.updateStatus("Produk baru berhasil ditambahkan.");
        }
    }

    private void openEditDialog() {
        Product target = resolveSelectedProduct();
        if (!isAdminActive() || target == null) {
            return;
        }
        int targetId = target.getProductId();
        if (openProductDialog(app, target)) {
            refreshData();
            restoreSelection(targetId);
            app.updateStatus("Data produk #" + targetId + " berhasil diperbarui.");
        }
    }

    private void deleteSelectedProduct() {
        Product target = resolveSelectedProduct();
        if (!isAdminActive() || target == null) {
            return;
        }
        boolean confirmed = UiKit.confirm(
            this,
            "Hapus Produk",
            "Yakin ingin menghapus produk \"" + AdminWidgets.safeText(target.getProductName()) + "\"?"
        );
        if (!confirmed) {
            return;
        }
        try {
            app.getShopService().deleteProduct(target.getProductId());
            refreshData();
            statusSummary = "Produk \"" + AdminWidgets.safeText(target.getProductName()) + "\" berhasil dihapus.";
            app.updateStatus(statusSummary);
        } catch (RuntimeException ex) {
            String msg = AdminWidgets.safeErrorMessage(ex);
            UiKit.showError(this, msg);
            app.updateStatus("Gagal menghapus produk: " + msg);
        }
    }

    private static boolean matchesCategory(Product product, String categoryFilter) {
        if ("Semua".equalsIgnoreCase(categoryFilter)) {
            return true;
        }
        String raw = product.getCategory() == null ? "" : product.getCategory().trim();
        return categoryFilter.equalsIgnoreCase(raw);
    }

    private static boolean matchesQuery(Product product, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return AdminWidgets.containsIgnoreCase(product.getProductName(), query)
            || AdminWidgets.containsIgnoreCase(product.getCategory(), query)
            || AdminWidgets.containsIgnoreCase(product.getSlug(), query)
            || AdminWidgets.containsIgnoreCase(product.getDescription(), query);
    }

    /**
     * Modal Create/Edit dialog for {@link Product} master records with photo upload preview,
     * width-tracking scrollable form body, and inline validation.
     */
    public static final class ProductFormDialog extends JDialog {

        private static final long serialVersionUID = 1L;

        private final transient AppWindow app;
        private final transient Product existing;

        private final JTextField nameField;
        private final JComboBox<String> categoryCombo;
        private final JTextField priceField;
        private final JTextField stockField;
        private final PhotoPanel photoPreview;
        private final JTextField imagePathField;
        private final JTextArea descriptionArea;
        private final JLabel inlineErrorLabel;

        private boolean saved;

        public ProductFormDialog(AppWindow app, Product existing) {
            super(
                app,
                existing == null ? "Tambah Produk Baru" : "Edit Produk",
                ModalityType.APPLICATION_MODAL
            );
            this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");
            this.existing = existing;

            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setResizable(true);

            nameField = UiKit.textField("Contoh: Whiskas Adult Tuna 1 kg");
            categoryCombo = AdminWidgets.adminComboBox(FORM_CATEGORIES);
            categoryCombo.setEditable(true);
            priceField = UiKit.textField("Contoh: 65000");
            stockField = UiKit.textField("Contoh: 50");

            photoPreview = new PhotoPanel(84, 84, Theme.Admin.CARD_RADIUS);
            imagePathField = UiKit.textField(AppConfig.DEFAULT_PRODUCT_IMAGE);
            AdminWidgets.wireTextChange(imagePathField, () -> photoPreview.setImagePath(imagePathField.getText()));

            JButton choosePhotoButton = UiKit.adminLightButton("Pilih Foto", VectorIcons.Kind.IMAGE);
            choosePhotoButton.addActionListener(e -> chooseProductPhoto());

            JButton defaultPhotoButton = UiKit.adminLightButton("Default", null);
            defaultPhotoButton.addActionListener(e -> {
                imagePathField.setText(AppConfig.DEFAULT_PRODUCT_IMAGE);
                photoPreview.setImagePath(AppConfig.DEFAULT_PRODUCT_IMAGE);
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
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Nama Produk", nameField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Kategori", categoryCombo);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Harga (Rp)", priceField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Stok", stockField);
            AdminWidgets.addFieldGroup(formCard, gbc, row++, "Foto Produk", photoRow);
            AdminWidgets.addScrollAreaGroup(formCard, gbc, row++, "Deskripsi Produk", descriptionArea, 80);

            gbc.gridy = row;
            gbc.weighty = 1.0;
            gbc.insets = new Insets(4, 0, 0, 0);
            formCard.add(inlineErrorLabel, gbc);

            JButton cancelButton = UiKit.adminLightButton("Batal", VectorIcons.Kind.CLOSE);
            cancelButton.addActionListener(e -> dispose());

            JButton saveButton = UiKit.adminPrimaryButton("Simpan Produk", VectorIcons.Kind.CHECK);
            saveButton.addActionListener(e -> submitForm());

            AdminWidgets.finalizeFormDialog(this, app, formCard, cancelButton, saveButton, 520, 520);
        }

        public boolean isSaved() {
            return saved;
        }

        public String getInlineErrorText() {
            return inlineErrorLabel.isVisible() ? inlineErrorLabel.getText() : "";
        }

        private void prefillFields() {
            if (existing == null) {
                categoryCombo.setSelectedItem("Makanan");
                imagePathField.setText(AppConfig.DEFAULT_PRODUCT_IMAGE);
                photoPreview.setImagePath(AppConfig.DEFAULT_PRODUCT_IMAGE);
                return;
            }
            nameField.setText(AdminWidgets.nonNull(existing.getProductName()));
            selectCategory(existing.getCategory());
            priceField.setText(AdminWidgets.formatPlainDecimal(existing.getPrice()));
            stockField.setText(String.valueOf(existing.getStock()));
            String img = (existing.getImage() == null || existing.getImage().isBlank())
                ? AppConfig.DEFAULT_PRODUCT_IMAGE
                : existing.getImage();
            imagePathField.setText(img);
            photoPreview.setImagePath(img);
            descriptionArea.setText(AdminWidgets.nonNull(existing.getDescription()));
            descriptionArea.setCaretPosition(0);
        }

        private void selectCategory(String rawCategory) {
            if (rawCategory == null || rawCategory.isBlank()) {
                categoryCombo.setSelectedItem("Makanan");
                return;
            }
            String trimmed = rawCategory.trim();
            for (String item : FORM_CATEGORIES) {
                if (item.equalsIgnoreCase(trimmed)) {
                    categoryCombo.setSelectedItem(item);
                    return;
                }
            }
            categoryCombo.setSelectedItem(trimmed);
        }

        private void chooseProductPhoto() {
            String uploadedPath = UiKit.chooseAndCopyImage(this, "products", "prod");
            if (uploadedPath != null && !uploadedPath.isBlank()) {
                imagePathField.setText(uploadedPath);
                photoPreview.setImagePath(uploadedPath);
            }
        }

        private void submitForm() {
            AdminWidgets.hideBanner(inlineErrorLabel);
            if (!app.getSession().isAdmin()) {
                AdminWidgets.showDialogError(this, inlineErrorLabel, "Hanya administrator yang dapat menyimpan produk.");
                return;
            }

            BigDecimal parsedPrice;
            String rawPrice = priceField.getText() == null ? "" : priceField.getText().trim();
            if (rawPrice.isEmpty()) {
                AdminWidgets.showDialogError(this, inlineErrorLabel, "Harga produk wajib diisi.");
                return;
            }
            try {
                parsedPrice = new BigDecimal(rawPrice);
            } catch (NumberFormatException ex) {
                AdminWidgets.showDialogError(
                    this,
                    inlineErrorLabel,
                    "Format harga produk tidak valid (contoh angka: 65000)."
                );
                return;
            }

            int parsedStock;
            String rawStock = stockField.getText() == null ? "" : stockField.getText().trim();
            if (rawStock.isEmpty()) {
                AdminWidgets.showDialogError(this, inlineErrorLabel, "Stok produk wajib diisi.");
                return;
            }
            try {
                parsedStock = Integer.parseInt(rawStock);
            } catch (NumberFormatException ex) {
                AdminWidgets.showDialogError(
                    this,
                    inlineErrorLabel,
                    "Stok produk harus berupa bilangan bulat (contoh: 50)."
                );
                return;
            }

            Object catObj = categoryCombo.isEditable()
                ? categoryCombo.getEditor().getItem()
                : categoryCombo.getSelectedItem();
            String rawCategory = catObj == null ? "" : catObj.toString().trim();
            String normalizedCategory = rawCategory.toLowerCase(Locale.ROOT);

            Product target = new Product();
            if (existing != null) {
                target.setProductId(existing.getProductId());
                target.setSlug(existing.getSlug());
            }
            target.setProductName(nameField.getText());
            target.setCategory(normalizedCategory);
            target.setPrice(parsedPrice);
            target.setStock(parsedStock);
            target.setImage(imagePathField.getText());
            target.setDescription(descriptionArea.getText());

            try {
                if (existing == null) {
                    app.getShopService().createProduct(target);
                } else {
                    app.getShopService().updateProduct(target);
                }
                saved = true;
                dispose();
            } catch (RuntimeException ex) {
                AdminWidgets.showDialogError(this, inlineErrorLabel, AdminWidgets.safeErrorMessage(ex));
            }
        }
    }
}
