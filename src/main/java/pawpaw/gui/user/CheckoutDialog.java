package pawpaw.gui.user;

import pawpaw.config.AppConfig;
import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.UiKit;
import pawpaw.model.CartItem;
import pawpaw.model.Order;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Modal customer checkout dialog prefilled from session user with dynamic pickup/delivery
 * shipping calculation, payment method selection, scrollable body, and inline validation.
 */
public final class CheckoutDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String OPTION_PICKUP = "Ambil di Toko (Rp 0)";
    private static final String OPTION_DELIVERY = "Pengantaran (Rp 15.000)";
    private static final String[] DELIVERY_OPTIONS = { OPTION_PICKUP, OPTION_DELIVERY };
    private static final String[] PAYMENT_OPTIONS = { "Transfer Bank", "QRIS", "Tunai / COD" };

    private final transient AppWindow app;

    private final JLabel itemCountValue;
    private final JLabel subtotalValue;
    private final JLabel shippingFeeValue;
    private final JLabel totalValue;

    private final JTextField recipientNameField;
    private final JTextField recipientPhoneField;
    private final JComboBox<String> deliveryCombo;
    private final JLabel addressLabel;
    private final JTextArea shippingAddressArea;
    private final JComboBox<String> paymentCombo;
    private final JLabel inlineErrorLabel;
    private final JButton submitButton;

    private transient Order createdOrder;

    public CheckoutDialog(AppWindow app) {
        super(app, "Checkout Pesanan", ModalityType.APPLICATION_MODAL);
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        // --- Summary Card ---
        itemCountValue = UiKit.label("0 item", Theme.bodyBold(), Theme.INK);
        subtotalValue = UiKit.label(FormatUtil.rupiah(BigDecimal.ZERO), Theme.body(), Theme.INK);
        shippingFeeValue = UiKit.label(FormatUtil.rupiah(BigDecimal.ZERO), Theme.body(), Theme.INK);
        totalValue = UiKit.label(FormatUtil.rupiah(BigDecimal.ZERO), Theme.heading(), Theme.BLUE);

        JPanel summaryCard = UiKit.cardPanel(new GridBagLayout());
        int sRow = 0;
        WorkspaceWidgets.addAttrRow(summaryCard, sRow++, "Jumlah Item", itemCountValue);
        WorkspaceWidgets.addAttrRow(summaryCard, sRow++, "Subtotal Produk", subtotalValue);
        WorkspaceWidgets.addAttrRow(summaryCard, sRow++, "Ongkos Kirim", shippingFeeValue);
        WorkspaceWidgets.addAttrRow(summaryCard, sRow, "Total Pembayaran", totalValue);

        // --- Form Card ---
        recipientNameField = UiKit.textField("Nama lengkap penerima");
        recipientPhoneField = UiKit.textField("08xxxxxxxxxx");
        deliveryCombo = UiKit.comboBox(DELIVERY_OPTIONS);
        addressLabel = UiKit.formLabel("Alamat Pengantaran");
        shippingAddressArea = WorkspaceWidgets.createEditableTextArea(3);
        paymentCombo = UiKit.comboBox(PAYMENT_OPTIONS);
        inlineErrorLabel = WorkspaceWidgets.createFeedbackBanner();

        prefillFromSession();

        deliveryCombo.addActionListener(e -> updateDeliveryAndTotals());

        JScrollPane addressScroll = UiKit.scrollPane(shippingAddressArea);
        addressScroll.setPreferredSize(new Dimension(420, 74));

        JPanel addressGroup = new JPanel(new BorderLayout(0, 4));
        addressGroup.setOpaque(false);
        addressGroup.add(addressLabel, BorderLayout.NORTH);
        addressGroup.add(addressScroll, BorderLayout.CENTER);

        JPanel formCard = UiKit.cardPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int fRow = 0;
        WorkspaceWidgets.addFieldGroup(formCard, gbc, fRow++, "Nama Penerima", recipientNameField);
        WorkspaceWidgets.addFieldGroup(formCard, gbc, fRow++, "Nomor Telepon", recipientPhoneField);
        WorkspaceWidgets.addFieldGroup(formCard, gbc, fRow++, "Metode Pengiriman", deliveryCombo);

        gbc.gridy = fRow++;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        formCard.add(addressGroup, gbc);

        WorkspaceWidgets.addFieldGroup(formCard, gbc, fRow++, "Metode Pembayaran", paymentCombo);

        gbc.gridy = fRow;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(4, 0, 0, 0);
        formCard.add(inlineErrorLabel, gbc);

        updateDeliveryAndTotals();

        // --- Footer Actions ---
        JButton cancelButton = UiKit.secondaryButton("Batal", VectorIcons.Kind.CLOSE);
        cancelButton.addActionListener(e -> dispose());

        submitButton = UiKit.primaryButton("Konfirmasi & Bayar", VectorIcons.Kind.CHECK);
        submitButton.addActionListener(e -> submitCheckout());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
        footer.setOpaque(false);
        footer.add(cancelButton);
        footer.add(submitButton);

        JPanel contentStack = new JPanel();
        contentStack.setLayout(new BoxLayout(contentStack, BoxLayout.Y_AXIS));
        contentStack.setOpaque(false);
        summaryCard.setAlignmentX(LEFT_ALIGNMENT);
        formCard.setAlignmentX(LEFT_ALIGNMENT);
        contentStack.add(summaryCard);
        contentStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        contentStack.add(formCard);

        JScrollPane bodyScroll = WorkspaceWidgets.verticalScrollPane(contentStack, Theme.BG);

        JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(
            Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16
        ));
        root.add(bodyScroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(submitButton);
        WorkspaceWidgets.registerEscapeClose(this);

        setMinimumSize(new Dimension(500, 480));
        setPreferredSize(new Dimension(540, 610));
        pack();
        setLocationRelativeTo(app);
    }

    public static Order show(AppWindow app) {
        if (app == null || app.getSession().getUser() == null || app.getSession().getCart().isEmpty()) {
            return null;
        }
        CheckoutDialog dialog = new CheckoutDialog(app);
        dialog.setVisible(true);
        return dialog.getCreatedOrder();
    }

    public Order getCreatedOrder() {
        return createdOrder;
    }

    public String getInlineErrorText() {
        return inlineErrorLabel.isVisible() ? inlineErrorLabel.getText() : "";
    }

    private void prefillFromSession() {
        User user = app.getSession().getUser();
        if (user == null) {
            return;
        }
        recipientNameField.setText(WorkspaceWidgets.nonNull(user.getName()));
        recipientPhoneField.setText(WorkspaceWidgets.nonNull(user.getPhone()));
        shippingAddressArea.setText(WorkspaceWidgets.nonNull(user.getAddress()));
    }

    private boolean isDeliverySelected() {
        Object selected = deliveryCombo.getSelectedItem();
        return selected != null && OPTION_DELIVERY.equals(selected.toString());
    }

    private void updateDeliveryAndTotals() {
        boolean delivery = isDeliverySelected();
        BigDecimal subtotal = app.getSession().getCartSubtotal();
        BigDecimal shippingFee = delivery ? AppConfig.SHIPPING_FEE_DELIVERY : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(shippingFee);

        int count = app.getSession().getCartCount();
        itemCountValue.setText(count + " item");
        subtotalValue.setText(FormatUtil.rupiah(subtotal));
        shippingFeeValue.setText(FormatUtil.rupiah(shippingFee));
        totalValue.setText(FormatUtil.rupiah(total));

        if (delivery) {
            addressLabel.setText("Alamat Pengantaran (Wajib diisi)");
            shippingAddressArea.setEditable(true);
            shippingAddressArea.setEnabled(true);
            shippingAddressArea.setBackground(Theme.SURFACE);
        } else {
            addressLabel.setText("Alamat Pengantaran (Tidak wajib untuk Ambil di Toko)");
            shippingAddressArea.setEditable(false);
            shippingAddressArea.setEnabled(false);
            shippingAddressArea.setBackground(Theme.SURFACE_ALT);
        }
    }

    private void submitCheckout() {
        WorkspaceWidgets.hideBanner(inlineErrorLabel);

        User user = app.getSession().getUser();
        if (user == null) {
            showInlineError("Sesi pengguna tidak aktif. Silakan masuk kembali.");
            return;
        }
        List<CartItem> cart = app.getSession().getCart();
        if (cart.isEmpty()) {
            showInlineError("Keranjang belanja masih kosong.");
            return;
        }

        boolean delivery = isDeliverySelected();
        String deliveryMethod = delivery ? "delivery" : "pickup";
        String shippingAddress = delivery ? shippingAddressArea.getText() : "";
        Object paymentObj = paymentCombo.getSelectedItem();
        String paymentMethod = paymentObj != null ? paymentObj.toString() : "Transfer Bank";

        try {
            Order order = app.getShopService().checkout(
                user.getUserId(),
                cart,
                recipientNameField.getText(),
                recipientPhoneField.getText(),
                deliveryMethod,
                shippingAddress,
                paymentMethod
            );
            this.createdOrder = order;
            app.getSession().clearCart();
            app.refreshCartBadge();
            dispose();
        } catch (RuntimeException ex) {
            showInlineError(WorkspaceWidgets.safeErrorMessage(ex));
        }
    }

    private void showInlineError(String message) {
        WorkspaceWidgets.showBanner(inlineErrorLabel, message, true);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> {
            if (inlineErrorLabel.isShowing()) {
                inlineErrorLabel.scrollRectToVisible(inlineErrorLabel.getBounds());
            }
        });
    }
}
