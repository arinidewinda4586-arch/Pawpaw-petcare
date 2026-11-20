package pawpaw.gui.user;

import pawpaw.gui.common.UiKit;
import pawpaw.model.Order;
import pawpaw.model.OrderDetail;
import pawpaw.model.Payment;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Structured desktop order invoice modal displaying recipient details, line items,
 * shipping fee, total price, and linked payment reference/status with {@code app.css} tokens.
 */
public final class OrderInvoiceDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public OrderInvoiceDialog(Window owner, Order order) {
        super(owner, "Struk Pesanan", ModalityType.APPLICATION_MODAL);
        Order safeOrder = Objects.requireNonNull(order, "Order tidak boleh null.");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        String code = (safeOrder.getOrderCode() != null && !safeOrder.getOrderCode().isBlank())
            ? safeOrder.getOrderCode()
            : "#" + safeOrder.getOrderId();

        // --- Header Card ---
        JPanel headerCard = UiKit.cardPanel(new BorderLayout(Theme.SPACE_12, 0));
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_12, 0));
        headerLeft.setOpaque(false);
        headerLeft.add(WorkspaceWidgets.iconBadge(
            VectorIcons.Kind.ORDERS,
            40,
            20,
            12,
            Theme.INFO_BG,
            Theme.BLUE,
            Theme.BORDER
        ));

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);
        titleStack.add(UiKit.label("Struk Pesanan " + code, Theme.heading(), Theme.INK));
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(UiKit.label(
            "Tanggal Transaksi: " + FormatUtil.dateTimeId(safeOrder.getOrderDate()),
            Theme.small(),
            Theme.MUTED
        ));
        headerLeft.add(titleStack);

        WorkspaceWidgets.DynamicStatusPill orderStatusPill = new WorkspaceWidgets.DynamicStatusPill();
        orderStatusPill.setStatus(safeOrder.getStatus());
        JPanel pillSlot = new JPanel(new GridBagLayout());
        pillSlot.setOpaque(false);
        pillSlot.add(orderStatusPill);

        headerCard.add(headerLeft, BorderLayout.CENTER);
        headerCard.add(pillSlot, BorderLayout.EAST);

        // --- Recipient & Delivery Card ---
        JPanel recipientCard = UiKit.cardPanel(new GridBagLayout());
        String deliveryLabel = "delivery".equalsIgnoreCase(safeOrder.getDeliveryMethod())
            ? "Pengantaran"
            : "Ambil di Toko";
        int row = 0;
        WorkspaceWidgets.addAttrRow(recipientCard, row++, "Penerima",
            UiKit.label(WorkspaceWidgets.safeText(safeOrder.getRecipientName()), Theme.bodyBold(), Theme.INK));
        WorkspaceWidgets.addAttrRow(recipientCard, row++, "Telepon",
            UiKit.label(WorkspaceWidgets.safeText(safeOrder.getRecipientPhone()), Theme.body(), Theme.INK));
        WorkspaceWidgets.addAttrRow(recipientCard, row++, "Metode Kirim",
            UiKit.label(deliveryLabel, Theme.body(), Theme.INK));
        WorkspaceWidgets.addAttrRow(recipientCard, row, "Alamat Pengiriman",
            UiKit.label("<html>" + WorkspaceWidgets.escapeHtml(
                WorkspaceWidgets.safeText(safeOrder.getShippingAddress())) + "</html>", Theme.body(), Theme.INK));

        // --- Line Items Card ---
        WorkspaceWidgets.ReadOnlyTableModel itemsModel = new WorkspaceWidgets.ReadOnlyTableModel(
            new Object[] { "Produk", "Qty", "Harga Satuan", "Subtotal" },
            new Class<?>[] { String.class, Integer.class, BigDecimal.class, BigDecimal.class }
        );
        BigDecimal itemsSubtotal = BigDecimal.ZERO;
        List<OrderDetail> details = safeOrder.getDetails() != null ? safeOrder.getDetails() : List.of();
        for (OrderDetail detail : details) {
            String productName = (detail.getProductName() != null && !detail.getProductName().isBlank())
                ? detail.getProductName()
                : "Produk #" + detail.getProductId();
            BigDecimal unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal lineSub = detail.getSubtotal() != null ? detail.getSubtotal() : BigDecimal.ZERO;
            itemsSubtotal = itemsSubtotal.add(lineSub);
            itemsModel.addRow(new Object[] {
                productName,
                Integer.valueOf(detail.getQuantity()),
                unitPrice,
                lineSub
            });
        }
        BigDecimal shippingFee = safeOrder.getShippingFee() != null ? safeOrder.getShippingFee() : BigDecimal.ZERO;
        BigDecimal totalPrice = safeOrder.getTotalPrice() != null ? safeOrder.getTotalPrice() : BigDecimal.ZERO;
        if (details.isEmpty() && totalPrice.compareTo(shippingFee) >= 0) {
            itemsSubtotal = totalPrice.subtract(shippingFee);
        }

        JTable itemsTable = UiKit.table(itemsModel);
        WorkspaceWidgets.CurrencyCellRenderer currencyRenderer = new WorkspaceWidgets.CurrencyCellRenderer();
        itemsTable.getColumnModel().getColumn(2).setCellRenderer(currencyRenderer);
        itemsTable.getColumnModel().getColumn(3).setCellRenderer(currencyRenderer);
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(230);
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(55);
        itemsTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(115);

        JScrollPane itemsScroll = UiKit.scrollPane(itemsTable);
        itemsScroll.setPreferredSize(new Dimension(520, 160));

        JPanel itemsCard = UiKit.cardPanel(new BorderLayout(0, Theme.SPACE_8));
        itemsCard.add(UiKit.label("Rincian Item Pesanan", Theme.smallBold(), Theme.INK), BorderLayout.NORTH);
        itemsCard.add(itemsScroll, BorderLayout.CENTER);

        // --- Payment & Summary Split Card ---
        JPanel bottomGrid = new JPanel(new GridLayout(1, 2, Theme.SPACE_12, 0));
        bottomGrid.setOpaque(false);

        Payment payment = safeOrder.getPayment();
        JPanel paymentCard = UiKit.cardPanel(new GridBagLayout());
        JLabel refLabel = UiKit.label(
            payment != null ? WorkspaceWidgets.safeText(payment.getReferenceNumber()) : "-",
            Theme.mono(),
            Theme.INK
        );
        JLabel payMethodLabel = UiKit.label(
            payment != null ? WorkspaceWidgets.safeText(payment.getPaymentMethod()) : "-",
            Theme.body(),
            Theme.INK
        );
        WorkspaceWidgets.DynamicStatusPill payStatusPill = new WorkspaceWidgets.DynamicStatusPill();
        payStatusPill.setStatus(payment != null ? payment.getPaymentStatus() : "");
        JPanel payPillWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        payPillWrap.setOpaque(false);
        payPillWrap.add(payStatusPill);
        JLabel paidAtLabel = UiKit.label(
            payment != null ? FormatUtil.dateTimeId(payment.getPaidAt()) : "-",
            Theme.small(),
            Theme.MUTED
        );

        int pRow = 0;
        WorkspaceWidgets.addAttrRow(paymentCard, pRow++, "Referensi", refLabel);
        WorkspaceWidgets.addAttrRow(paymentCard, pRow++, "Metode Bayar", payMethodLabel);
        WorkspaceWidgets.addAttrRow(paymentCard, pRow++, "Pembayaran", payPillWrap);
        WorkspaceWidgets.addAttrRow(paymentCard, pRow, "Waktu Bayar", paidAtLabel);

        JPanel totalsCard = UiKit.cardPanel(new GridBagLayout());
        int tRow = 0;
        WorkspaceWidgets.addAttrRow(totalsCard, tRow++, "Subtotal Produk",
            UiKit.label(FormatUtil.rupiah(itemsSubtotal), Theme.body(), Theme.INK));
        WorkspaceWidgets.addAttrRow(totalsCard, tRow++, "Biaya Pengiriman",
            UiKit.label(FormatUtil.rupiah(shippingFee), Theme.body(), Theme.INK));
        WorkspaceWidgets.addAttrRow(totalsCard, tRow, "Total Pembayaran",
            UiKit.label(FormatUtil.rupiah(totalPrice), Theme.heading(), Theme.BLUE));

        bottomGrid.add(paymentCard);
        bottomGrid.add(totalsCard);

        // --- Stack & Root ---
        JPanel bodyStack = new JPanel();
        bodyStack.setLayout(new BoxLayout(bodyStack, BoxLayout.Y_AXIS));
        bodyStack.setOpaque(false);
        headerCard.setAlignmentX(LEFT_ALIGNMENT);
        recipientCard.setAlignmentX(LEFT_ALIGNMENT);
        itemsCard.setAlignmentX(LEFT_ALIGNMENT);
        bottomGrid.setAlignmentX(LEFT_ALIGNMENT);
        bodyStack.add(headerCard);
        bodyStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        bodyStack.add(recipientCard);
        bodyStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        bodyStack.add(itemsCard);
        bodyStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        bodyStack.add(bottomGrid);

        JScrollPane bodyScroll = WorkspaceWidgets.verticalScrollPane(bodyStack, Theme.BG);

        JButton closeButton = UiKit.primaryButton("Tutup", VectorIcons.Kind.CLOSE);
        closeButton.addActionListener(e -> dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(closeButton);

        JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(
            Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16
        ));
        root.add(bodyScroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(closeButton);
        WorkspaceWidgets.registerEscapeClose(this);

        setMinimumSize(new Dimension(600, 500));
        setPreferredSize(new Dimension(640, 590));
        pack();
        setLocationRelativeTo(owner);
    }

    public static void show(Component parent, Order order) {
        if (order == null) {
            return;
        }
        Window owner = parent instanceof Window w
            ? w
            : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        OrderInvoiceDialog dialog = new OrderInvoiceDialog(owner, order);
        dialog.setVisible(true);
    }
}
