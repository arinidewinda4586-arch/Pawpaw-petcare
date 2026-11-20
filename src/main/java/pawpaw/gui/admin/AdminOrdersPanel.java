package pawpaw.gui.admin;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.StatusBadgeRenderer;
import pawpaw.gui.common.UiKit;
import pawpaw.gui.user.OrderInvoiceDialog;
import pawpaw.model.Order;
import pawpaw.model.OrderDetail;
import pawpaw.model.Payment;
import pawpaw.model.User;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
 * Admin transaction workspace for inspecting customer shop orders, payment status,
 * line items, invoices, and advancing or locking order lifecycle transitions.
 */
public final class AdminOrdersPanel extends PagePanel {

    private static final long serialVersionUID = 1L;
    private static final String[] STATUS_FILTERS = {
        "Semua", "Menunggu", "Dibayar", "Diproses", "Dikirim", "Selesai", "Dibatalkan"
    };
    private static final AdminWidgets.StatusOption[] EDITABLE_STATUS_OPTIONS = {
        new AdminWidgets.StatusOption("pending", "Menunggu"),
        new AdminWidgets.StatusOption("paid", "Dibayar"),
        new AdminWidgets.StatusOption("processing", "Diproses"),
        new AdminWidgets.StatusOption("shipped", "Dikirim"),
        new AdminWidgets.StatusOption("completed", "Selesai"),
        new AdminWidgets.StatusOption("cancelled", "Dibatalkan")
    };

    private final transient AppWindow app;

    private final JLabel inlineFeedbackLabel;
    private final JTextField searchField;
    private final JComboBox<String> statusFilterCombo;
    private final JLabel countLabel;
    private final JButton refreshButton;

    private final AdminWidgets.ReadOnlyTableModel orderModel;
    private final JTable orderTable;

    private final JLabel inspectorTitleLabel;
    private final JLabel inspectorDateLabel;
    private final AdminWidgets.StatusPill orderStatusPill;
    private final JLabel recipientValueLabel;
    private final JLabel phoneValueLabel;
    private final JLabel deliveryValueLabel;
    private final JLabel addressValueLabel;
    private final JLabel paymentRefLabel;
    private final JLabel paymentMethodLabel;
    private final AdminWidgets.StatusPill paymentStatusPill;
    private final JLabel totalValueLabel;

    private final AdminWidgets.ReadOnlyTableModel itemsModel;
    private final JTable itemsTable;
    private final JButton invoiceButton;
    private final JLabel terminalNoteLabel;
    private JPanel terminalNoteRow;
    private final JComboBox<AdminWidgets.StatusOption> statusEditCombo;
    private final JButton saveStatusButton;

    private transient List<Order> allOrders = new ArrayList<>();
    private transient List<Order> filteredOrders = new ArrayList<>();
    private transient Order selectedOrder;
    private String statusSummary = "Manajemen pesanan siap.";

    public AdminOrdersPanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineFeedbackLabel = AdminWidgets.createFeedbackBanner();

        searchField = UiKit.searchField("Cari kode pesanan, pelanggan, atau penerima...");
        searchField.setPreferredSize(new Dimension(225, 36));
        AdminWidgets.wireTextChange(searchField, this::applyFilter);

        statusFilterCombo = AdminWidgets.adminComboBox(STATUS_FILTERS);
        statusFilterCombo.addActionListener(e -> applyFilter());

        countLabel = UiKit.label("0 pesanan", Theme.smallBold(), Theme.Admin.TEXT_SOFT);

        refreshButton = UiKit.adminLightButton("Refresh", VectorIcons.Kind.REFRESH);
        refreshButton.addActionListener(e -> {
            refreshData();
            app.updateStatus(getStatusSummary());
        });

        JPanel toolbar = AdminWidgets.buildTransactionToolbar(
            searchField, statusFilterCombo, countLabel, refreshButton
        );
        JPanel northStack = AdminWidgets.buildNorthStack(inlineFeedbackLabel, toolbar);

        orderModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] {
                "Kode Pesanan", "Tanggal", "Pelanggan", "Penerima",
                "Metode", "Total", "Status Pesanan", "Pembayaran"
            },
            new Class<?>[] {
                String.class, String.class, String.class, String.class,
                String.class, BigDecimal.class, String.class, String.class
            }
        );
        orderTable = UiKit.adminTable(orderModel);
        orderTable.getColumnModel().getColumn(5).setCellRenderer(new AdminWidgets.CurrencyCellRenderer());
        orderTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        orderTable.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer());
        configureOrderColumns();
        AdminWidgets.wireTableSelection(orderTable, this::onSelectionChanged);
        AdminWidgets.attachDoubleClick(orderTable, this::openInvoiceDialog);

        // --- Bottom Detail Inspector ---
        inspectorTitleLabel = UiKit.label("Pilih pesanan pada tabel", Theme.heading(), Theme.Admin.TEXT);
        inspectorDateLabel = UiKit.label("-", Theme.small(), Theme.Admin.TEXT_SOFT);
        orderStatusPill = new AdminWidgets.StatusPill();

        recipientValueLabel = UiKit.label("-", Theme.bodyBold(), Theme.Admin.TEXT);
        phoneValueLabel = UiKit.label("-", Theme.body(), Theme.Admin.TEXT);
        deliveryValueLabel = UiKit.label("-", Theme.body(), Theme.Admin.TEXT);
        addressValueLabel = UiKit.label("-", Theme.small(), Theme.Admin.TEXT);

        paymentRefLabel = UiKit.label("-", Theme.mono(), Theme.Admin.TEXT);
        paymentMethodLabel = UiKit.label("-", Theme.body(), Theme.Admin.TEXT);
        paymentStatusPill = new AdminWidgets.StatusPill();
        totalValueLabel = UiKit.label(FormatUtil.rupiah(BigDecimal.ZERO), Theme.bodyBold(), Theme.Admin.BLUE);

        itemsModel = new AdminWidgets.ReadOnlyTableModel(
            new Object[] { "Produk", "Qty", "Harga Satuan", "Subtotal" },
            new Class<?>[] { String.class, Integer.class, BigDecimal.class, BigDecimal.class }
        );
        itemsTable = UiKit.adminTable(itemsModel);
        AdminWidgets.CurrencyCellRenderer currencyRenderer = new AdminWidgets.CurrencyCellRenderer();
        itemsTable.getColumnModel().getColumn(2).setCellRenderer(currencyRenderer);
        itemsTable.getColumnModel().getColumn(3).setCellRenderer(currencyRenderer);
        configureItemColumns();

        invoiceButton = UiKit.adminLightButton("Lihat Struk", VectorIcons.Kind.ORDERS);
        invoiceButton.setEnabled(false);
        invoiceButton.addActionListener(e -> openInvoiceDialog());

        terminalNoteLabel = UiKit.label("Status final terkunci", Theme.smallBold(), Theme.Admin.TEXT_SOFT);
        terminalNoteLabel.setVisible(false);

        statusEditCombo = AdminWidgets.adminComboBox(EDITABLE_STATUS_OPTIONS);
        statusEditCombo.setEnabled(false);

        saveStatusButton = UiKit.adminPrimaryButton("Simpan Status", VectorIcons.Kind.CHECK);
        saveStatusButton.setEnabled(false);
        saveStatusButton.addActionListener(e -> saveSelectedStatus());

        JPanel inspectorBody = buildInspectorContent();
        JSplitPane splitPane = AdminWidgets.buildMasterDetailSplit(orderTable, inspectorBody);

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
        inspectorDateLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleStack.add(inspectorTitleLabel);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(inspectorDateLabel);

        JPanel pillWrap = new JPanel(new GridBagLayout());
        pillWrap.setOpaque(false);
        pillWrap.add(orderStatusPill);

        headerRow.add(titleStack, BorderLayout.CENTER);
        headerRow.add(pillWrap, BorderLayout.EAST);

        JPanel infoGrid = new AdminWidgets.CompactSectionPanel(new GridLayout(1, 2, Theme.SPACE_12, 0));

        JPanel shippingCard = AdminWidgets.createInspectorSubCard(new GridBagLayout());
        int sRow = 0;
        AdminWidgets.addAttrRow(shippingCard, sRow++, "Penerima", recipientValueLabel);
        AdminWidgets.addAttrRow(shippingCard, sRow++, "Telepon", phoneValueLabel);
        AdminWidgets.addAttrRow(shippingCard, sRow++, "Pengiriman", deliveryValueLabel);
        AdminWidgets.addAttrRow(shippingCard, sRow, "Alamat", addressValueLabel);

        JPanel paymentCard = AdminWidgets.createInspectorSubCard(new GridBagLayout());
        JPanel payPillHost = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        payPillHost.setOpaque(false);
        payPillHost.add(paymentStatusPill);
        int pRow = 0;
        AdminWidgets.addAttrRow(paymentCard, pRow++, "Referensi", paymentRefLabel);
        AdminWidgets.addAttrRow(paymentCard, pRow++, "Metode Bayar", paymentMethodLabel);
        AdminWidgets.addAttrRow(paymentCard, pRow++, "Pembayaran", payPillHost);
        AdminWidgets.addAttrRow(paymentCard, pRow, "Total Tagihan", totalValueLabel);

        infoGrid.add(shippingCard);
        infoGrid.add(paymentCard);

        JPanel itemsSection = new AdminWidgets.CompactSectionPanel(new BorderLayout(0, 6));
        itemsSection.add(UiKit.label("Item Pesanan", Theme.smallBold(), Theme.Admin.TEXT), BorderLayout.NORTH);
        JScrollPane itemsScroll = AdminWidgets.adminScrollPane(itemsTable);
        itemsScroll.setPreferredSize(new Dimension(420, 128));
        itemsSection.add(itemsScroll, BorderLayout.CENTER);

        terminalNoteRow = AdminWidgets.createNoteRow(terminalNoteLabel);

        JPanel actionBar = new AdminWidgets.CompactSectionPanel(new BorderLayout(Theme.SPACE_12, 0));
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftActions.setOpaque(false);
        leftActions.add(invoiceButton);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
        rightActions.setOpaque(false);
        rightActions.add(AdminWidgets.adminFormLabel("Status:"));
        rightActions.add(statusEditCombo);
        rightActions.add(saveStatusButton);

        actionBar.add(leftActions, BorderLayout.WEST);
        actionBar.add(rightActions, BorderLayout.EAST);

        headerRow.setAlignmentX(LEFT_ALIGNMENT);
        infoGrid.setAlignmentX(LEFT_ALIGNMENT);
        itemsSection.setAlignmentX(LEFT_ALIGNMENT);
        terminalNoteRow.setAlignmentX(LEFT_ALIGNMENT);
        actionBar.setAlignmentX(LEFT_ALIGNMENT);

        stack.add(headerRow);
        stack.add(Box.createVerticalStrut(Theme.SPACE_12));
        stack.add(infoGrid);
        stack.add(Box.createVerticalStrut(Theme.SPACE_12));
        stack.add(itemsSection);
        stack.add(Box.createVerticalStrut(Theme.SPACE_12));
        stack.add(terminalNoteRow);
        stack.add(actionBar);
        return stack;
    }

    private void configureOrderColumns() {
        orderTable.getColumnModel().getColumn(0).setPreferredWidth(135);
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(135);
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(135);
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(135);
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        orderTable.getColumnModel().getColumn(6).setPreferredWidth(115);
        orderTable.getColumnModel().getColumn(7).setPreferredWidth(105);
    }

    private void configureItemColumns() {
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(55);
        itemsTable.getColumnModel().getColumn(2).setPreferredWidth(115);
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(120);
    }

    @Override
    public String getPageKey() {
        return "admin.orders";
    }

    @Override
    public String getPageTitle() {
        return "Manajemen Pesanan";
    }

    @Override
    public String getPageSubtitle() {
        return "Pantau detail pesanan pelanggan, pembayaran, dan perbarui status";
    }

    @Override
    public void refreshData() {
        int prevId = selectedOrder != null ? selectedOrder.getOrderId() : -1;
        AdminWidgets.hideBanner(inlineFeedbackLabel);

        if (!isAdminActive()) {
            allOrders = new ArrayList<>();
            filteredOrders = new ArrayList<>();
            orderModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 pesanan");
            statusSummary = "Sesi admin tidak aktif.";
            return;
        }

        try {
            allOrders = new ArrayList<>(app.getShopService().listAllOrders());
            applyFilter(prevId);
            statusSummary = "Daftar pesanan dimuat: " + allOrders.size() + " transaksi.";
        } catch (RuntimeException ex) {
            allOrders = new ArrayList<>();
            filteredOrders = new ArrayList<>();
            orderModel.setRowCount(0);
            clearSelection();
            countLabel.setText("0 pesanan");
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineFeedbackLabel, "Gagal memuat daftar pesanan: " + msg, true);
            statusSummary = "Gagal memuat pesanan: " + msg;
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
        int currentId = selectedOrder != null ? selectedOrder.getOrderId() : -1;
        applyFilter(currentId);
    }

    private void applyFilter(int preferredOrderId) {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Object statusObj = statusFilterCombo.getSelectedItem();

        filteredOrders = new ArrayList<>();
        orderModel.setRowCount(0);

        for (Order order : allOrders) {
            if (!AdminWidgets.matchesStatusFilter(order.getStatus(), statusObj) || !matchesQuery(order, query)) {
                continue;
            }
            filteredOrders.add(order);
            String code = formatOrderCode(order);
            String customer = (order.getUserName() != null && !order.getUserName().isBlank())
                ? order.getUserName()
                : AdminWidgets.safeText(order.getRecipientName());
            String method = "delivery".equalsIgnoreCase(order.getDeliveryMethod())
                ? "Pengantaran"
                : "Ambil di Toko";
            String paymentStatus = order.getPayment() != null
                ? AdminWidgets.nonNull(order.getPayment().getPaymentStatus())
                : "";

            orderModel.addRow(new Object[] {
                code,
                FormatUtil.dateTimeId(order.getOrderDate()),
                customer,
                AdminWidgets.safeText(order.getRecipientName()),
                method,
                order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO,
                AdminWidgets.safeText(order.getStatus()),
                paymentStatus
            });
        }

        countLabel.setText(filteredOrders.size() + " / " + allOrders.size() + " pesanan");
        AdminWidgets.restoreTableSelection(
            orderTable,
            filteredOrders,
            Order::getOrderId,
            preferredOrderId,
            this::onSelectionChanged,
            this::clearSelection
        );
    }

    private void onSelectionChanged() {
        Order order = AdminWidgets.resolveSelectedItem(orderTable, filteredOrders);
        if (order == null) {
            clearSelection();
            return;
        }
        selectedOrder = order;

        String code = formatOrderCode(order);
        String customer = (order.getUserName() != null && !order.getUserName().isBlank())
            ? order.getUserName()
            : AdminWidgets.safeText(order.getRecipientName());
        inspectorTitleLabel.setText(code + " — " + customer);
        inspectorDateLabel.setText("Tanggal Pesanan: " + FormatUtil.dateTimeId(order.getOrderDate()));
        orderStatusPill.setStatus(order.getStatus());

        recipientValueLabel.setText(AdminWidgets.safeText(order.getRecipientName()));
        phoneValueLabel.setText(AdminWidgets.safeText(order.getRecipientPhone()));

        BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        String deliveryDesc = "delivery".equalsIgnoreCase(order.getDeliveryMethod())
            ? "Pengantaran (Ongkir " + FormatUtil.rupiah(shippingFee) + ")"
            : "Ambil di Toko";
        deliveryValueLabel.setText(deliveryDesc);

        String address = AdminWidgets.safeText(order.getShippingAddress());
        addressValueLabel.setText("<html>" + AdminWidgets.escapeHtml(address) + "</html>");
        addressValueLabel.setToolTipText(address);

        Payment payment = order.getPayment();
        paymentRefLabel.setText(payment != null ? AdminWidgets.safeText(payment.getReferenceNumber()) : "-");
        paymentMethodLabel.setText(payment != null ? AdminWidgets.safeText(payment.getPaymentMethod()) : "-");
        paymentStatusPill.setStatus(payment != null ? payment.getPaymentStatus() : "");
        totalValueLabel.setText(FormatUtil.rupiah(order.getTotalPrice()));

        itemsModel.setRowCount(0);
        List<OrderDetail> details = order.getDetails() != null ? order.getDetails() : List.of();
        for (OrderDetail detail : details) {
            String prodName = (detail.getProductName() != null && !detail.getProductName().isBlank())
                ? detail.getProductName()
                : "Produk #" + detail.getProductId();
            itemsModel.addRow(new Object[] {
                prodName,
                Integer.valueOf(detail.getQuantity()),
                detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO,
                detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO
            });
        }

        AdminWidgets.selectStatusOption(statusEditCombo, order.getStatus());

        boolean adminActive = isAdminActive();
        boolean terminal = isTerminalOrderStatus(order.getStatus());
        invoiceButton.setEnabled(adminActive);
        statusEditCombo.setEnabled(adminActive && !terminal);
        saveStatusButton.setEnabled(adminActive && !terminal);
        AdminWidgets.setNoteRowVisible(terminalNoteRow, terminalNoteLabel, terminal, "Status final terkunci");
    }

    private void clearSelection() {
        selectedOrder = null;
        inspectorTitleLabel.setText("Pilih pesanan pada tabel");
        inspectorDateLabel.setText("-");
        orderStatusPill.setStatus("");
        recipientValueLabel.setText("-");
        phoneValueLabel.setText("-");
        deliveryValueLabel.setText("-");
        addressValueLabel.setText("-");
        addressValueLabel.setToolTipText(null);
        paymentRefLabel.setText("-");
        paymentMethodLabel.setText("-");
        paymentStatusPill.setStatus("");
        totalValueLabel.setText(FormatUtil.rupiah(BigDecimal.ZERO));
        itemsModel.setRowCount(0);
        invoiceButton.setEnabled(false);
        statusEditCombo.setEnabled(false);
        saveStatusButton.setEnabled(false);
        AdminWidgets.setNoteRowVisible(terminalNoteRow, terminalNoteLabel, false, null);
    }

    private void openInvoiceDialog() {
        Order target = AdminWidgets.resolveSelectedItem(orderTable, filteredOrders);
        if (!isAdminActive() || target == null) {
            return;
        }
        OrderInvoiceDialog.show(app, target);
    }

    private void saveSelectedStatus() {
        AdminWidgets.hideBanner(inlineFeedbackLabel);
        Order target = AdminWidgets.resolveSelectedItem(orderTable, filteredOrders);
        if (!isAdminActive() || target == null) {
            return;
        }
        if (isTerminalOrderStatus(target.getStatus())) {
            AdminWidgets.showBanner(
                inlineFeedbackLabel,
                "Pesanan berstatus final (" + FormatUtil.statusLabel(target.getStatus()) + ") sudah terkunci.",
                true
            );
            return;
        }
        Object item = statusEditCombo.getSelectedItem();
        if (!(item instanceof AdminWidgets.StatusOption option)) {
            return;
        }

        String targetCode = option.code();
        String code = formatOrderCode(target);
        if ("cancelled".equalsIgnoreCase(targetCode)) {
            boolean confirmed = UiKit.confirm(
                this,
                "Batalkan Pesanan",
                "Yakin ingin membatalkan pesanan " + code + "?\n"
                    + "Stok produk akan dikembalikan dan status pembayaran akan ditandai GAGAL."
            );
            if (!confirmed) {
                return;
            }
        }

        int orderId = target.getOrderId();
        try {
            app.getShopService().updateOrderStatus(orderId, targetCode);
            refreshData();
            AdminWidgets.restoreTableSelection(
                orderTable,
                filteredOrders,
                Order::getOrderId,
                orderId,
                this::onSelectionChanged,
                this::clearSelection
            );
            String msg = "Status pesanan " + code + " diperbarui menjadi " + FormatUtil.statusLabel(targetCode) + ".";
            AdminWidgets.showBanner(inlineFeedbackLabel, msg, false);
            statusSummary = msg;
            app.updateStatus(msg);
        } catch (RuntimeException ex) {
            String msg = AdminWidgets.safeErrorMessage(ex);
            AdminWidgets.showBanner(inlineFeedbackLabel, "Gagal memperbarui pesanan: " + msg, true);
            UiKit.showError(this, msg);
            app.updateStatus("Gagal memperbarui pesanan: " + msg);
        }
    }

    private static boolean isTerminalOrderStatus(String status) {
        if (status == null) {
            return false;
        }
        String clean = status.trim().toLowerCase(Locale.ROOT);
        return "completed".equals(clean) || "cancelled".equals(clean);
    }

    private static String formatOrderCode(Order order) {
        return (order.getOrderCode() != null && !order.getOrderCode().isBlank())
            ? order.getOrderCode()
            : "#" + order.getOrderId();
    }

    private static boolean matchesQuery(Order order, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String ref = order.getPayment() != null ? order.getPayment().getReferenceNumber() : null;
        return AdminWidgets.containsIgnoreCase(formatOrderCode(order), query)
            || AdminWidgets.containsIgnoreCase(order.getUserName(), query)
            || AdminWidgets.containsIgnoreCase(order.getRecipientName(), query)
            || AdminWidgets.containsIgnoreCase(order.getRecipientPhone(), query)
            || AdminWidgets.containsIgnoreCase(ref, query);
    }
}
