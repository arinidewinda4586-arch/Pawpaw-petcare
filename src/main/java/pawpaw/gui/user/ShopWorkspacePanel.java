package pawpaw.gui.user;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.PhotoPanel;
import pawpaw.gui.common.ResponsiveCardGrid;
import pawpaw.gui.common.StatusBadgeRenderer;
import pawpaw.gui.common.UiKit;
import pawpaw.model.CartItem;
import pawpaw.model.Order;
import pawpaw.model.Product;
import pawpaw.model.User;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Customer shop workspace providing a website-styled product card grid, 5 category pill filters,
 * quick-add cart buttons, stock-validated cart inspector with checkout flow, and order history.
 */
public final class ShopWorkspacePanel extends PagePanel {

    private static final long serialVersionUID = 1L;
    private static final String[] CATEGORY_FILTERS = {
        "Semua", "Makanan", "Perawatan", "Mainan", "Perlengkapan"
    };

    private final transient AppWindow app;

    private final JLabel inlineErrorLabel;
    private final JPanel errorBannerSlot;
    private final JTabbedPane tabs;

    // Tab 1: Catalog & Cart widgets
    private final JTextField searchField;
    private final transient List<UiKit.PillFilterButton> categoryButtons = new ArrayList<>();
    private final JLabel catalogCountLabel;
    private final ResponsiveCardGrid catalogGrid;
    private final transient List<ProductCard> productCards = new ArrayList<>();
    private String activeCategoryFilter = "Semua";

    private final PhotoPanel previewPhoto;
    private final WorkspaceWidgets.WrappingHtmlLabel previewNameLabel;
    private final WorkspaceWidgets.DynamicStatusPill previewStockPill;
    private final JLabel previewMetaLabel;
    private final JTextArea previewDescArea;
    private final JButton addToCartButton;

    private final JLabel cartCountLabel;
    private final JPanel cartListStack;
    private final JLabel cartFeedbackLabel;
    private final JPanel cartFeedbackSlot;
    private final JLabel cartSubtotalValue;
    private final JButton clearCartButton;
    private final JButton checkoutButton;

    // Tab 2: Order history widgets
    private final JLabel ordersCountLabel;
    private final JButton viewInvoiceButton;
    private final JButton cancelOrderButton;
    private final WorkspaceWidgets.ReadOnlyTableModel ordersModel;
    private final JTable ordersTable;

    private transient List<Product> allProducts = new ArrayList<>();
    private transient List<Product> filteredProducts = new ArrayList<>();
    private transient List<Order> userOrders = new ArrayList<>();
    private transient Product selectedProduct;
    private transient Order selectedOrder;
    private String statusSummary = "Ruang kerja toko siap.";

    public ShopWorkspacePanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineErrorLabel = WorkspaceWidgets.createFeedbackBanner();
        errorBannerSlot = WorkspaceWidgets.createBannerSlot(inlineErrorLabel, 0);
        add(errorBannerSlot, BorderLayout.NORTH);

        // --- Tab 1: Katalog & Keranjang ---
        searchField = UiKit.searchField("Cari nama, kategori, atau deskripsi produk...");
        searchField.setPreferredSize(new Dimension(260, 36));
        WorkspaceWidgets.wireTextChange(searchField, this::applyCatalogFilter);

        catalogCountLabel = UiKit.label("0 produk", Theme.smallBold(), Theme.MUTED);

        JPanel searchRow = new JPanel(new BorderLayout(Theme.SPACE_12, 0));
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(catalogCountLabel, BorderLayout.EAST);

        JPanel categoryRow = new JPanel(WorkspaceWidgets.wrapFlowLayout(Theme.SPACE_8, Theme.SPACE_4));
        categoryRow.setOpaque(false);
        for (String category : CATEGORY_FILTERS) {
            UiKit.PillFilterButton pill = UiKit.pillFilterButton(category, null);
            if ("Semua".equals(category)) {
                pill.setSelectedPill(true);
            }
            pill.addActionListener(e -> selectCategoryFilter(category));
            categoryButtons.add(pill);
            categoryRow.add(pill);
        }

        JPanel catalogToolbar = UiKit.cardPanel(new BorderLayout(0, Theme.SPACE_8));
        catalogToolbar.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        catalogToolbar.add(searchRow, BorderLayout.NORTH);
        catalogToolbar.add(categoryRow, BorderLayout.SOUTH);

        catalogGrid = new ResponsiveCardGrid(185, 12, 12);
        catalogGrid.setEmptyState(
            "Tidak ada produk ditemukan",
            "Coba ubah filter kategori atau kata kunci pencarian."
        );
        JScrollPane catalogScroll = WorkspaceWidgets.verticalScrollPane(catalogGrid, Theme.BG);
        catalogScroll.setMinimumSize(new Dimension(415, 260));
        catalogScroll.setPreferredSize(new Dimension(430, 420));

        previewPhoto = new PhotoPanel(52, 52, 12);
        previewPhoto.setMinimumSize(new Dimension(52, 52));
        previewPhoto.setPreferredSize(new Dimension(52, 52));
        previewNameLabel = new WorkspaceWidgets.WrappingHtmlLabel("Pilih Produk", Theme.bodyBold(), Theme.INK, 140);
        previewStockPill = new WorkspaceWidgets.DynamicStatusPill();
        previewMetaLabel = UiKit.label("-", Theme.smallBold(), Theme.MUTED);
        previewDescArea = WorkspaceWidgets.createReadOnlyTextArea(2, Theme.small(), Theme.SURFACE);
        addToCartButton = UiKit.primaryButton("+ Tambah ke Keranjang", VectorIcons.Kind.CART);
        addToCartButton.setEnabled(false);
        addToCartButton.addActionListener(e -> addSelectedProductToCart());

        cartCountLabel = UiKit.label("0 item", Theme.smallBold(), Theme.MUTED);
        cartListStack = new JPanel();
        cartListStack.setLayout(new BoxLayout(cartListStack, BoxLayout.Y_AXIS));
        cartListStack.setOpaque(false);

        cartFeedbackLabel = WorkspaceWidgets.createFeedbackBanner();
        cartFeedbackSlot = WorkspaceWidgets.createBannerSlot(cartFeedbackLabel, Theme.SPACE_8);
        cartSubtotalValue = UiKit.label(FormatUtil.rupiah(BigDecimal.ZERO), Theme.heading(), Theme.BLUE);
        clearCartButton = UiKit.secondaryButton("Kosongkan", VectorIcons.Kind.TRASH);
        clearCartButton.setEnabled(false);
        clearCartButton.addActionListener(e -> clearCart());

        checkoutButton = UiKit.primaryButton("Checkout", VectorIcons.Kind.CHECK);
        checkoutButton.setEnabled(false);
        checkoutButton.addActionListener(e -> openCheckoutDialog());

        JPanel cartInspectorCard = buildCartInspectorCard();

        JSplitPane catalogSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, catalogScroll, cartInspectorCard);
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

        // --- Tab 2: Riwayat Pesanan ---
        ordersCountLabel = UiKit.label("0 pesanan", Theme.smallBold(), Theme.MUTED);
        viewInvoiceButton = UiKit.secondaryButton("Lihat Struk", VectorIcons.Kind.ORDERS);
        viewInvoiceButton.setEnabled(false);
        viewInvoiceButton.addActionListener(e -> openSelectedOrderInvoice());

        cancelOrderButton = UiKit.dangerButton("Batalkan Pesanan", VectorIcons.Kind.CLOSE);
        cancelOrderButton.setEnabled(false);
        cancelOrderButton.addActionListener(e -> cancelSelectedOrder());

        JPanel historyActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
        historyActions.setOpaque(false);
        historyActions.add(viewInvoiceButton);
        historyActions.add(cancelOrderButton);

        JPanel historyToolbar = UiKit.cardPanel(new BorderLayout(Theme.SPACE_12, 0));
        historyToolbar.add(ordersCountLabel, BorderLayout.WEST);
        historyToolbar.add(historyActions, BorderLayout.EAST);

        ordersModel = new WorkspaceWidgets.ReadOnlyTableModel(
            new Object[] { "Kode Pesanan", "Tanggal", "Metode", "Total", "Status Pesanan", "Pembayaran" },
            new Class<?>[] { String.class, String.class, String.class, BigDecimal.class, String.class, String.class }
        );
        ordersTable = UiKit.table(ordersModel);
        ordersTable.getColumnModel().getColumn(3).setCellRenderer(new WorkspaceWidgets.CurrencyCellRenderer());
        ordersTable.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
        ordersTable.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
        configureOrderColumns();
        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onOrderSelectionChanged();
            }
        });

        JPanel historyTab = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        historyTab.setOpaque(false);
        historyTab.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, 0, 0, 0));
        historyTab.add(historyToolbar, BorderLayout.NORTH);
        historyTab.add(UiKit.scrollPane(ordersTable), BorderLayout.CENTER);

        tabs = new JTabbedPane();
        tabs.setFont(Theme.bodyBold());
        tabs.setBackground(Theme.SURFACE);
        tabs.setForeground(Theme.INK);
        tabs.addTab("Katalog & Keranjang", catalogTab);
        tabs.addTab("Riwayat Pesanan", historyTab);

        add(tabs, BorderLayout.CENTER);
        refreshCartInspector();
    }

    public int getDisplayedProductCardCount() {
        return catalogGrid.getCardCount();
    }

    public int getCatalogColumnCount() {
        return catalogGrid.getColumnCount();
    }

    private void selectCategoryFilter(String category) {
        this.activeCategoryFilter = category != null ? category : "Semua";
        for (UiKit.PillFilterButton btn : categoryButtons) {
            btn.setSelectedPill(this.activeCategoryFilter.equals(btn.getText()));
        }
        applyCatalogFilter();
    }

    private JPanel buildProductPreviewHeader() {
        JPanel previewCard = UiKit.pastelCardPanel(
            new BorderLayout(0, Theme.SPACE_8),
            Theme.SURFACE_ALT,
            Theme.BORDER,
            14
        );
        previewCard.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel photoSlot = new JPanel(new BorderLayout());
        photoSlot.setOpaque(false);
        photoSlot.add(previewPhoto, BorderLayout.NORTH);

        JPanel titleRow = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
        titleRow.setOpaque(false);
        titleRow.add(previewNameLabel, BorderLayout.CENTER);
        JPanel pillWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pillWrap.setOpaque(false);
        pillWrap.add(previewStockPill);
        titleRow.add(pillWrap, BorderLayout.EAST);

        JPanel headerRow = new JPanel(new BorderLayout(10, 0));
        headerRow.setOpaque(false);
        headerRow.add(photoSlot, BorderLayout.WEST);
        headerRow.add(titleRow, BorderLayout.CENTER);

        JPanel infoStack = new JPanel(new BorderLayout(0, 6));
        infoStack.setOpaque(false);
        infoStack.add(headerRow, BorderLayout.NORTH);
        infoStack.add(previewMetaLabel, BorderLayout.SOUTH);

        JScrollPane descScroll = UiKit.scrollPane(previewDescArea);
        descScroll.setPreferredSize(new Dimension(200, 44));
        descScroll.setMinimumSize(new Dimension(160, 38));

        JPanel bottomStack = new JPanel(new BorderLayout(0, Theme.SPACE_8));
        bottomStack.setOpaque(false);
        bottomStack.add(descScroll, BorderLayout.CENTER);
        bottomStack.add(addToCartButton, BorderLayout.SOUTH);

        previewCard.add(infoStack, BorderLayout.NORTH);
        previewCard.add(bottomStack, BorderLayout.CENTER);
        return previewCard;
    }

    private JPanel buildCartInspectorCard() {
        JPanel card = UiKit.cardPanel(new BorderLayout(0, Theme.SPACE_12));
        card.setPreferredSize(new Dimension(340, 0));
        card.setMinimumSize(new Dimension(290, 0));

        JPanel previewWrap = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout());
        previewWrap.add(buildProductPreviewHeader(), BorderLayout.CENTER);

        JPanel cartHeader = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout(Theme.SPACE_8, 0));
        cartHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(0, 0, Theme.SPACE_8, 0)
        ));
        cartHeader.add(UiKit.label("Keranjang Belanja", Theme.heading(), Theme.INK), BorderLayout.WEST);
        cartHeader.add(cartCountLabel, BorderLayout.EAST);

        JPanel inspectorStack = new JPanel();
        inspectorStack.setLayout(new BoxLayout(inspectorStack, BoxLayout.Y_AXIS));
        inspectorStack.setOpaque(false);
        previewWrap.setAlignmentX(LEFT_ALIGNMENT);
        cartHeader.setAlignmentX(LEFT_ALIGNMENT);
        cartListStack.setAlignmentX(LEFT_ALIGNMENT);
        inspectorStack.add(previewWrap);
        inspectorStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        inspectorStack.add(cartHeader);
        inspectorStack.add(Box.createVerticalStrut(Theme.SPACE_8));
        inspectorStack.add(cartListStack);

        JScrollPane inspectorScroll = WorkspaceWidgets.verticalScrollPane(inspectorStack, Theme.SURFACE);

        JPanel subtotalRow = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout(Theme.SPACE_8, 0));
        subtotalRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(Theme.SPACE_8, 0, 0, 0)
        ));
        subtotalRow.add(UiKit.label("Subtotal", Theme.bodyBold(), Theme.MUTED), BorderLayout.WEST);
        subtotalRow.add(cartSubtotalValue, BorderLayout.EAST);

        JPanel buttonsRow = new WorkspaceWidgets.CompactSectionPanel(new GridLayout(1, 2, Theme.SPACE_8, 0));
        buttonsRow.add(clearCartButton);
        buttonsRow.add(checkoutButton);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        cartFeedbackSlot.setAlignmentX(LEFT_ALIGNMENT);
        subtotalRow.setAlignmentX(LEFT_ALIGNMENT);
        buttonsRow.setAlignmentX(LEFT_ALIGNMENT);
        footer.add(cartFeedbackSlot);
        footer.add(subtotalRow);
        footer.add(Box.createVerticalStrut(Theme.SPACE_8));
        footer.add(buttonsRow);

        card.add(inspectorScroll, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private void configureOrderColumns() {
        ordersTable.getColumnModel().getColumn(0).setPreferredWidth(145);
        ordersTable.getColumnModel().getColumn(1).setPreferredWidth(145);
        ordersTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        ordersTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        ordersTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        ordersTable.getColumnModel().getColumn(5).setPreferredWidth(115);
    }

    @Override
    public String getPageKey() {
        return "user.shop";
    }

    @Override
    public String getPageTitle() {
        return "Toko PawPaw";
    }

    @Override
    public String getPageSubtitle() {
        return "Belanja kebutuhan hewan, keranjang, checkout, dan riwayat pesanan";
    }

    @Override
    public void refreshData() {
        int prevProductId = selectedProduct != null ? selectedProduct.getProductId() : -1;
        int prevOrderId = selectedOrder != null ? selectedOrder.getOrderId() : -1;
        WorkspaceWidgets.hideBannerInSlot(errorBannerSlot, inlineErrorLabel);
        WorkspaceWidgets.hideBannerInSlot(cartFeedbackSlot, cartFeedbackLabel);

        User user = app.getSession().getUser();
        try {
            allProducts = new ArrayList<>(app.getShopService().listProducts());
            syncCartProductSnapshots(allProducts);
            userOrders = user != null
                ? new ArrayList<>(app.getShopService().listUserOrders(user.getUserId()))
                : new ArrayList<>();

            applyCatalogFilter(prevProductId);
            populateOrdersTable(prevOrderId);
            refreshCartInspector();
            app.refreshCartBadge();

            if (user == null) {
                statusSummary = "Katalog toko: " + allProducts.size()
                    + " produk dimuat. Masuk untuk melakukan checkout.";
            } else {
                statusSummary = "Toko: " + allProducts.size() + " produk, "
                    + app.getSession().getCartCount() + " item di keranjang, "
                    + userOrders.size() + " pesanan.";
            }
        } catch (RuntimeException ex) {
            allProducts = new ArrayList<>();
            filteredProducts = new ArrayList<>();
            productCards.clear();
            catalogGrid.setCards(List.of());
            catalogCountLabel.setText("0 produk");
            userOrders = new ArrayList<>();
            ordersModel.setRowCount(0);
            clearProductPreview();
            clearOrderSelection();
            refreshCartInspector();
            String msg = WorkspaceWidgets.safeErrorMessage(ex);
            WorkspaceWidgets.showBannerInSlot(errorBannerSlot, inlineErrorLabel, "Gagal memuat data toko: " + msg, true);
            statusSummary = "Gagal memuat data toko: " + msg;
        }
    }

    @Override
    public String getStatusSummary() {
        return statusSummary;
    }

    private void syncCartProductSnapshots(List<Product> freshProducts) {
        Map<Integer, Product> byId = new LinkedHashMap<>();
        for (Product p : freshProducts) {
            byId.put(p.getProductId(), p);
        }
        for (CartItem item : app.getSession().getCart()) {
            if (item.getProduct() != null) {
                Product latest = byId.get(item.getProduct().getProductId());
                if (latest != null) {
                    item.setProduct(latest);
                }
            }
        }
    }

    private void applyCatalogFilter() {
        int currentId = selectedProduct != null ? selectedProduct.getProductId() : -1;
        applyCatalogFilter(currentId);
    }

    private void applyCatalogFilter(int preferredProductId) {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase(Locale.ROOT);

        filteredProducts = new ArrayList<>();
        productCards.clear();
        for (Product product : allProducts) {
            if (!matchesCategory(product, activeCategoryFilter)) {
                continue;
            }
            if (!matchesQuery(product, query)) {
                continue;
            }
            filteredProducts.add(product);
            productCards.add(new ProductCard(product));
        }

        catalogGrid.setCards(productCards);
        catalogCountLabel.setText(
            "Menampilkan " + filteredProducts.size() + " dari " + allProducts.size() + " produk"
        );
        restoreProductSelection(preferredProductId);
    }

    private void restoreProductSelection(int preferredProductId) {
        if (filteredProducts.isEmpty()) {
            clearProductPreview();
            return;
        }
        Product target = filteredProducts.get(0);
        if (preferredProductId > 0) {
            for (Product candidate : filteredProducts) {
                if (candidate.getProductId() == preferredProductId) {
                    target = candidate;
                    break;
                }
            }
        }
        selectProduct(target);
    }

    private void selectProduct(Product product) {
        if (product == null) {
            clearProductPreview();
            return;
        }
        selectedProduct = product;
        for (ProductCard card : productCards) {
            card.setSelected(card.product.getProductId() == product.getProductId());
        }

        previewPhoto.setImagePath(product.getImage());
        previewNameLabel.setPlainText(WorkspaceWidgets.safeText(product.getProductName()));
        previewStockPill.setStatus(product.getStock() > 0 ? "available" : "habis");
        previewMetaLabel.setText(
            WorkspaceWidgets.capitalizeWords(product.getCategory())
                + "  |  " + FormatUtil.rupiah(product.getPrice())
                + "  |  Stok: " + product.getStock()
        );
        String desc = (product.getDescription() == null || product.getDescription().isBlank())
            ? "Belum ada deskripsi produk."
            : product.getDescription();
        previewDescArea.setText(desc);
        previewDescArea.setCaretPosition(0);

        addToCartButton.setEnabled(product.getStock() > 0);
    }

    private void clearProductPreview() {
        selectedProduct = null;
        for (ProductCard card : productCards) {
            card.setSelected(false);
        }
        previewPhoto.setImagePath(null);
        previewNameLabel.setPlainText("Pilih Produk");
        previewStockPill.setStatus("");
        previewMetaLabel.setText("-");
        previewDescArea.setText("");
        addToCartButton.setEnabled(false);
    }

    private void addSelectedProductToCart() {
        addProductToCart(selectedProduct);
    }

    private void addProductToCart(Product product) {
        WorkspaceWidgets.hideBannerInSlot(cartFeedbackSlot, cartFeedbackLabel);
        if (product == null) {
            return;
        }
        int productId = product.getProductId();
        int desiredQty = getCartQuantityForProduct(productId) + 1;
        try {
            app.getShopService().validateCartQuantity(productId, desiredQty);
            app.getSession().addToCart(product, 1);
            app.refreshCartBadge();
            refreshCartInspector();
            WorkspaceWidgets.showBannerInSlot(
                cartFeedbackSlot,
                cartFeedbackLabel,
                "Ditambahkan: " + product.getProductName(),
                false
            );
            statusSummary = "Keranjang diperbarui (" + app.getSession().getCartCount() + " item).";
            app.updateStatus(statusSummary);
        } catch (RuntimeException ex) {
            WorkspaceWidgets.showBannerInSlot(
                cartFeedbackSlot,
                cartFeedbackLabel,
                WorkspaceWidgets.safeErrorMessage(ex),
                true
            );
        }
    }

    private void refreshCartInspector() {
        cartListStack.removeAll();
        List<CartItem> cart = app.getSession().getCart();

        if (cart.isEmpty()) {
            JPanel emptyCard = UiKit.pastelCardPanel(new BorderLayout(), Theme.SURFACE_ALT, Theme.BORDER, 14);
            emptyCard.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, Theme.SPACE_12, Theme.SPACE_12, Theme.SPACE_12));
            JLabel emptyLbl = UiKit.label(
                "<html>Keranjang belanja masih kosong. Klik tombol keranjang pada produk atau <b>+ Tambah ke Keranjang</b>.</html>",
                Theme.small(),
                Theme.MUTED
            );
            emptyCard.add(emptyLbl, BorderLayout.CENTER);
            JPanel emptyWrap = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout());
            emptyWrap.add(emptyCard, BorderLayout.CENTER);
            emptyWrap.setAlignmentX(LEFT_ALIGNMENT);
            cartListStack.add(emptyWrap);
        } else {
            for (int i = 0; i < cart.size(); i++) {
                CartItem item = cart.get(i);
                JPanel rowCard = buildCartItemRow(item);
                rowCard.setAlignmentX(LEFT_ALIGNMENT);
                cartListStack.add(rowCard);
                if (i < cart.size() - 1) {
                    cartListStack.add(Box.createVerticalStrut(Theme.SPACE_8));
                }
            }
        }

        int totalQty = app.getSession().getCartCount();
        cartCountLabel.setText(totalQty + " item");
        cartSubtotalValue.setText(FormatUtil.rupiah(app.getSession().getCartSubtotal()));

        boolean hasItems = !cart.isEmpty();
        clearCartButton.setEnabled(hasItems);
        checkoutButton.setEnabled(hasItems && app.getSession().getUser() != null);

        cartListStack.revalidate();
        cartListStack.repaint();
    }

    private JPanel buildCartItemRow(CartItem item) {
        Product p = item.getProduct();
        int productId = p != null ? p.getProductId() : 0;
        String name = p != null ? WorkspaceWidgets.safeText(p.getProductName()) : "Produk";
        BigDecimal unitPrice = (p != null && p.getPrice() != null) ? p.getPrice() : BigDecimal.ZERO;

        JPanel rowCard = UiKit.pastelCardPanel(
            new BorderLayout(0, Theme.SPACE_8),
            Theme.SURFACE_ALT,
            Theme.BORDER,
            14
        );
        rowCard.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel topRow = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
        topRow.setOpaque(false);
        topRow.add(UiKit.label("<html><b>" + WorkspaceWidgets.escapeHtml(name) + "</b></html>",
            Theme.body(), Theme.INK), BorderLayout.CENTER);
        topRow.add(UiKit.label(FormatUtil.rupiah(item.getSubtotal()), Theme.bodyBold(), Theme.BLUE), BorderLayout.EAST);

        JPanel qtyControls = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_4, 0));
        qtyControls.setOpaque(false);

        JButton minusBtn = UiKit.secondaryButton("-");
        minusBtn.setToolTipText("Kurangi jumlah");
        minusBtn.addActionListener(e -> updateCartQuantity(productId, item.getQuantity() - 1));

        JLabel qtyLabel = UiKit.label(String.valueOf(item.getQuantity()), Theme.bodyBold(), Theme.INK);
        qtyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        qtyLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        JButton plusBtn = UiKit.secondaryButton("+");
        plusBtn.setToolTipText("Tambah jumlah");
        plusBtn.addActionListener(e -> updateCartQuantity(productId, item.getQuantity() + 1));

        qtyControls.add(minusBtn);
        qtyControls.add(qtyLabel);
        qtyControls.add(plusBtn);

        JButton removeBtn = UiKit.ghostButton("Hapus", VectorIcons.Kind.TRASH);
        removeBtn.addActionListener(e -> removeCartItem(productId));

        JPanel bottomRow = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(qtyControls, BorderLayout.WEST);
        bottomRow.add(removeBtn, BorderLayout.EAST);

        JLabel unitLabel = UiKit.label(
            FormatUtil.rupiah(unitPrice) + " / unit",
            Theme.small(),
            Theme.MUTED
        );

        JPanel centerBox = new JPanel(new BorderLayout(0, 4));
        centerBox.setOpaque(false);
        centerBox.add(topRow, BorderLayout.NORTH);
        centerBox.add(unitLabel, BorderLayout.SOUTH);

        rowCard.add(centerBox, BorderLayout.NORTH);
        rowCard.add(bottomRow, BorderLayout.SOUTH);

        JPanel wrapper = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout());
        wrapper.add(rowCard, BorderLayout.CENTER);
        return wrapper;
    }

    private void updateCartQuantity(int productId, int nextQty) {
        WorkspaceWidgets.hideBannerInSlot(cartFeedbackSlot, cartFeedbackLabel);
        if (nextQty <= 0) {
            removeCartItem(productId);
            return;
        }
        try {
            app.getShopService().validateCartQuantity(productId, nextQty);
            app.getSession().updateCartQty(productId, nextQty);
            app.refreshCartBadge();
            refreshCartInspector();
            app.updateStatus("Keranjang diperbarui (" + app.getSession().getCartCount() + " item).");
        } catch (RuntimeException ex) {
            WorkspaceWidgets.showBannerInSlot(
                cartFeedbackSlot,
                cartFeedbackLabel,
                WorkspaceWidgets.safeErrorMessage(ex),
                true
            );
        }
    }

    private void removeCartItem(int productId) {
        WorkspaceWidgets.hideBannerInSlot(cartFeedbackSlot, cartFeedbackLabel);
        app.getSession().removeFromCart(productId);
        app.refreshCartBadge();
        refreshCartInspector();
        app.updateStatus("Item dihapus dari keranjang.");
    }

    private void clearCart() {
        WorkspaceWidgets.hideBannerInSlot(cartFeedbackSlot, cartFeedbackLabel);
        app.getSession().clearCart();
        app.refreshCartBadge();
        refreshCartInspector();
        app.updateStatus("Keranjang belanja dikosongkan.");
    }

    private void openCheckoutDialog() {
        WorkspaceWidgets.hideBannerInSlot(cartFeedbackSlot, cartFeedbackLabel);
        User user = app.getSession().getUser();
        if (user == null) {
            WorkspaceWidgets.showBannerInSlot(
                cartFeedbackSlot,
                cartFeedbackLabel,
                "Silakan masuk terlebih dahulu untuk checkout.",
                true
            );
            return;
        }
        if (app.getSession().getCart().isEmpty()) {
            WorkspaceWidgets.showBannerInSlot(
                cartFeedbackSlot,
                cartFeedbackLabel,
                "Keranjang belanja masih kosong.",
                true
            );
            return;
        }
        Order created = CheckoutDialog.show(app);
        if (created != null) {
            refreshData();
            tabs.setSelectedIndex(1);
            selectOrderById(created.getOrderId());
            statusSummary = "Pesanan " + WorkspaceWidgets.safeText(created.getOrderCode()) + " berhasil dibuat.";
            app.updateStatus(statusSummary);
            OrderInvoiceDialog.show(this, created);
        }
    }

    private int getCartQuantityForProduct(int productId) {
        for (CartItem item : app.getSession().getCart()) {
            if (item.getProduct() != null && item.getProduct().getProductId() == productId) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    private void populateOrdersTable(int preferredOrderId) {
        ordersModel.setRowCount(0);
        for (Order order : userOrders) {
            String code = (order.getOrderCode() != null && !order.getOrderCode().isBlank())
                ? order.getOrderCode()
                : "#" + order.getOrderId();
            String method = "delivery".equalsIgnoreCase(order.getDeliveryMethod())
                ? "Pengantaran"
                : "Ambil di Toko";
            String paymentStatus = order.getPayment() != null
                ? WorkspaceWidgets.nonNull(order.getPayment().getPaymentStatus())
                : "";

            ordersModel.addRow(new Object[] {
                code,
                FormatUtil.dateTimeId(order.getOrderDate()),
                method,
                order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO,
                WorkspaceWidgets.safeText(order.getStatus()),
                paymentStatus
            });
        }
        ordersCountLabel.setText(userOrders.size() + " riwayat pesanan");
        restoreOrderSelection(preferredOrderId);
    }

    private void restoreOrderSelection(int preferredOrderId) {
        if (userOrders.isEmpty()) {
            clearOrderSelection();
            return;
        }
        int targetModelRow = 0;
        if (preferredOrderId > 0) {
            for (int i = 0; i < userOrders.size(); i++) {
                if (userOrders.get(i).getOrderId() == preferredOrderId) {
                    targetModelRow = i;
                    break;
                }
            }
        }
        int viewRow = ordersTable.convertRowIndexToView(targetModelRow);
        if (viewRow >= 0 && viewRow < ordersTable.getRowCount()) {
            ordersTable.setRowSelectionInterval(viewRow, viewRow);
            onOrderSelectionChanged();
        } else {
            clearOrderSelection();
        }
    }

    private void selectOrderById(int orderId) {
        restoreOrderSelection(orderId);
    }

    private void onOrderSelectionChanged() {
        int viewRow = ordersTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= ordersTable.getRowCount()) {
            clearOrderSelection();
            return;
        }
        int modelRow = ordersTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= userOrders.size()) {
            clearOrderSelection();
            return;
        }
        Order order = userOrders.get(modelRow);
        selectedOrder = order;
        viewInvoiceButton.setEnabled(true);
        cancelOrderButton.setEnabled("pending".equalsIgnoreCase(order.getStatus()));
    }

    private void clearOrderSelection() {
        selectedOrder = null;
        viewInvoiceButton.setEnabled(false);
        cancelOrderButton.setEnabled(false);
    }

    private void openSelectedOrderInvoice() {
        User user = app.getSession().getUser();
        if (user == null || selectedOrder == null) {
            return;
        }
        try {
            Order invoice = app.getShopService().getOrderInvoice(
                selectedOrder.getOrderId(),
                user.getUserId(),
                user.isAdmin()
            );
            OrderInvoiceDialog.show(this, invoice);
        } catch (RuntimeException ex) {
            UiKit.showError(this, WorkspaceWidgets.safeErrorMessage(ex));
        }
    }

    private void cancelSelectedOrder() {
        User user = app.getSession().getUser();
        if (user == null || selectedOrder == null || !"pending".equalsIgnoreCase(selectedOrder.getStatus())) {
            return;
        }
        String code = (selectedOrder.getOrderCode() != null && !selectedOrder.getOrderCode().isBlank())
            ? selectedOrder.getOrderCode()
            : "#" + selectedOrder.getOrderId();
        boolean confirmed = UiKit.confirm(
            this,
            "Batalkan Pesanan",
            "Yakin ingin membatalkan pesanan " + code + "? Stok produk akan dikembalikan."
        );
        if (!confirmed) {
            return;
        }
        try {
            app.getShopService().cancelUserOrder(user.getUserId(), selectedOrder.getOrderId());
            refreshData();
            app.updateStatus(getStatusSummary());
            UiKit.showInfo(this, "Pesanan " + code + " berhasil dibatalkan.");
        } catch (RuntimeException ex) {
            UiKit.showError(this, WorkspaceWidgets.safeErrorMessage(ex));
        }
    }

    private static boolean matchesCategory(Product product, String categoryFilter) {
        if ("Semua".equalsIgnoreCase(categoryFilter)) {
            return true;
        }
        String raw = product.getCategory() == null ? "" : product.getCategory().trim().toLowerCase(Locale.ROOT);
        return categoryFilter.trim().toLowerCase(Locale.ROOT).equals(raw);
    }

    private static boolean matchesQuery(Product product, String lowerQuery) {
        if (lowerQuery.isEmpty()) {
            return true;
        }
        return WorkspaceWidgets.containsIgnoreCase(product.getProductName(), lowerQuery)
            || WorkspaceWidgets.containsIgnoreCase(product.getCategory(), lowerQuery)
            || WorkspaceWidgets.containsIgnoreCase(product.getDescription(), lowerQuery);
    }

    /**
     * Website-styled product card rendered inside {@link ResponsiveCardGrid} with circular quick-add button.
     */
    private final class ProductCard extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int CARD_RADIUS = 22;

        private final transient Product product;
        private boolean selected;
        private boolean hovered;

        ProductCard(Product product) {
            super(new BorderLayout(0, 8));
            this.product = product;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 12, 10));

            PhotoPanel photo = new PhotoPanel(160, 116, 14);
            photo.setImagePath(product.getImage());
            photo.setPreferredSize(new Dimension(160, 116));
            photo.setMinimumSize(new Dimension(120, 100));

            JPanel badgesRow = new JPanel(WorkspaceWidgets.wrapFlowLayout(4, 2));
            badgesRow.setOpaque(false);
            badgesRow.add(UiKit.pillLabel(
                WorkspaceWidgets.capitalizeWords(product.getCategory()),
                Theme.INFO_BG,
                Theme.INFO_FG
            ));
            boolean inStock = product.getStock() > 0;
            badgesRow.add(UiKit.pillLabel(
                inStock ? "Stok: " + product.getStock() : "Habis",
                inStock ? Theme.SUCCESS_BG : Theme.DANGER_BG,
                inStock ? Theme.SUCCESS_FG : Theme.DANGER_FG
            ));

            WorkspaceWidgets.WrappingHtmlLabel nameLabel = new WorkspaceWidgets.WrappingHtmlLabel(
                WorkspaceWidgets.safeText(product.getProductName()),
                Theme.bodyBold(),
                Theme.INK,
                160
            );

            JPanel centerInfo = new JPanel(new BorderLayout(0, 4));
            centerInfo.setOpaque(false);
            centerInfo.add(badgesRow, BorderLayout.NORTH);
            centerInfo.add(nameLabel, BorderLayout.CENTER);

            JLabel priceLabel = UiKit.label(
                FormatUtil.rupiah(product.getPrice()),
                Theme.heading(),
                Theme.BLUE
            );

            JButton quickAddBtn = UiKit.circleIconButton(
                VectorIcons.Kind.CART,
                Theme.CYAN_BTN,
                Theme.CYAN_BTN_HOVER,
                Theme.INK,
                36
            );
            quickAddBtn.setActionCommand("add-to-cart:" + product.getProductId());
            quickAddBtn.setToolTipText(
                inStock
                    ? "Tambah " + WorkspaceWidgets.safeText(product.getProductName()) + " ke keranjang"
                    : "Stok produk habis"
            );
            quickAddBtn.setEnabled(inStock);
            quickAddBtn.addActionListener(e -> {
                selectProduct(product);
                addProductToCart(product);
            });

            JPanel bottomRow = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
            bottomRow.setOpaque(false);
            bottomRow.add(priceLabel, BorderLayout.CENTER);
            bottomRow.add(quickAddBtn, BorderLayout.EAST);

            setToolTipText(
                WorkspaceWidgets.safeText(product.getProductName())
                    + " - " + FormatUtil.rupiah(product.getPrice())
            );

            add(photo, BorderLayout.NORTH);
            add(centerInfo, BorderLayout.CENTER);
            add(bottomRow, BorderLayout.SOUTH);

            WorkspaceWidgets.attachCardInteraction(
                this,
                () -> selectProduct(product),
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
                CARD_RADIUS,
                Theme.SURFACE,
                Theme.SURFACE_ALT,
                Theme.BORDER_SUBTLE,
                Theme.BORDER,
                hovered,
                selected,
                2.2f
            );
            super.paintComponent(g);
        }
    }
}
