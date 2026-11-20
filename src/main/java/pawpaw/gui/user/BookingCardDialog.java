package pawpaw.gui.user;

import pawpaw.gui.common.UiKit;
import pawpaw.model.HealthBooking;
import pawpaw.model.Payment;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Structured desktop health appointment slip modal showing service, doctor, owner,
 * pet details, schedule, booking/payment statuses, and optional rejection/cancellation reason.
 */
public final class BookingCardDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public BookingCardDialog(Window owner, HealthBooking booking) {
        super(owner, "Kartu Booking Kesehatan", ModalityType.APPLICATION_MODAL);
        HealthBooking safeBooking = Objects.requireNonNull(booking, "HealthBooking tidak boleh null.");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        // --- Header Card ---
        JPanel headerCard = UiKit.cardPanel(new BorderLayout(Theme.SPACE_12, 0));
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_12, 0));
        headerLeft.setOpaque(false);
        headerLeft.add(WorkspaceWidgets.iconBadge(
            VectorIcons.Kind.BOOKINGS,
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
        titleStack.add(UiKit.label(
            "Kartu Booking #" + safeBooking.getBookingId(),
            Theme.heading(),
            Theme.INK
        ));
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(UiKit.label(
            "Jadwal: " + FormatUtil.dateTimeId(safeBooking.getBookingDate()),
            Theme.small(),
            Theme.MUTED
        ));
        headerLeft.add(titleStack);

        WorkspaceWidgets.DynamicStatusPill bookingStatusPill = new WorkspaceWidgets.DynamicStatusPill();
        bookingStatusPill.setStatus(safeBooking.getStatus());
        JPanel pillSlot = new JPanel(new GridBagLayout());
        pillSlot.setOpaque(false);
        pillSlot.add(bookingStatusPill);

        headerCard.add(headerLeft, BorderLayout.CENTER);
        headerCard.add(pillSlot, BorderLayout.EAST);

        // --- Appointment Info Card ---
        JPanel infoCard = UiKit.cardPanel(new GridBagLayout());
        String serviceName = (safeBooking.getServiceName() != null && !safeBooking.getServiceName().isBlank())
            ? safeBooking.getServiceName()
            : "Layanan #" + safeBooking.getServiceId();
        String petName = WorkspaceWidgets.safeText(safeBooking.getPetName());
        String petSpecies = WorkspaceWidgets.safeText(safeBooking.getPetSpecies());
        String petSummary = "-".equals(petSpecies) ? petName : petName + " (" + petSpecies + ")";
        BigDecimal price = safeBooking.getServicePrice() != null ? safeBooking.getServicePrice() : BigDecimal.ZERO;

        int row = 0;
        WorkspaceWidgets.addAttrRow(infoCard, row++, "Layanan",
            UiKit.label(serviceName, Theme.bodyBold(), Theme.INK));
        WorkspaceWidgets.addAttrRow(infoCard, row++, "Dokter",
            UiKit.label(WorkspaceWidgets.safeText(safeBooking.getDoctorName()), Theme.body(), Theme.INK));
        WorkspaceWidgets.addAttrRow(infoCard, row++, "Pemilik",
            UiKit.label(WorkspaceWidgets.safeText(safeBooking.getOwnerName()), Theme.body(), Theme.INK));
        WorkspaceWidgets.addAttrRow(infoCard, row++, "Hewan",
            UiKit.label(petSummary, Theme.body(), Theme.INK));
        WorkspaceWidgets.addAttrRow(infoCard, row++, "Jadwal Kunjungan",
            UiKit.label(FormatUtil.dateTimeId(safeBooking.getBookingDate()), Theme.bodyBold(), Theme.INK));
        WorkspaceWidgets.addAttrRow(infoCard, row, "Biaya Layanan",
            UiKit.label(FormatUtil.rupiah(price), Theme.bodyBold(), Theme.BLUE));

        // --- Payment Info Card ---
        Payment payment = safeBooking.getPayment();
        JPanel paymentCard = UiKit.cardPanel(new GridBagLayout());
        WorkspaceWidgets.DynamicStatusPill payStatusPill = new WorkspaceWidgets.DynamicStatusPill();
        payStatusPill.setStatus(payment != null ? payment.getPaymentStatus() : "");
        JPanel payPillWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        payPillWrap.setOpaque(false);
        payPillWrap.add(payStatusPill);

        int pRow = 0;
        WorkspaceWidgets.addAttrRow(paymentCard, pRow++, "Referensi Bayar",
            UiKit.label(payment != null ? WorkspaceWidgets.safeText(payment.getReferenceNumber()) : "-", Theme.mono(), Theme.INK));
        WorkspaceWidgets.addAttrRow(paymentCard, pRow++, "Metode Bayar",
            UiKit.label(payment != null ? WorkspaceWidgets.safeText(payment.getPaymentMethod()) : "-", Theme.body(), Theme.INK));
        WorkspaceWidgets.addAttrRow(paymentCard, pRow++, "Status Pembayaran", payPillWrap);
        WorkspaceWidgets.addAttrRow(paymentCard, pRow, "Nominal",
            UiKit.label(
                FormatUtil.rupiah(payment != null && payment.getAmount() != null ? payment.getAmount() : price),
                Theme.bodyBold(),
                Theme.INK
            ));

        // --- Body Stack ---
        JPanel bodyStack = new JPanel();
        bodyStack.setLayout(new BoxLayout(bodyStack, BoxLayout.Y_AXIS));
        bodyStack.setOpaque(false);
        headerCard.setAlignmentX(LEFT_ALIGNMENT);
        infoCard.setAlignmentX(LEFT_ALIGNMENT);
        paymentCard.setAlignmentX(LEFT_ALIGNMENT);
        bodyStack.add(headerCard);
        bodyStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        bodyStack.add(infoCard);
        bodyStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        bodyStack.add(paymentCard);

        String reason = safeBooking.getRejectionReason();
        if (reason != null && !reason.isBlank()) {
            JPanel reasonCard = UiKit.pastelCardPanel(
                new BorderLayout(0, Theme.SPACE_8),
                Theme.DANGER_BG,
                new Color(0xFC, 0xA5, 0xA5),
                Theme.CARD_RADIUS
            );
            reasonCard.setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_12, Theme.SPACE_16, Theme.SPACE_12, Theme.SPACE_16
            ));
            reasonCard.setAlignmentX(LEFT_ALIGNMENT);
            reasonCard.add(UiKit.label("Catatan / Alasan Pembatalan & Penolakan", Theme.smallBold(), Theme.DANGER_FG),
                BorderLayout.NORTH);
            JTextArea reasonArea = WorkspaceWidgets.createReadOnlyTextArea(2, Theme.body(), Theme.DANGER_BG);
            reasonArea.setForeground(Theme.DANGER_FG);
            reasonArea.setBorder(BorderFactory.createEmptyBorder());
            reasonArea.setText(reason.trim());
            reasonCard.add(reasonArea, BorderLayout.CENTER);

            bodyStack.add(Box.createVerticalStrut(Theme.SPACE_12));
            bodyStack.add(reasonCard);
        }

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

        setMinimumSize(new Dimension(500, 440));
        setPreferredSize(new Dimension(540, 540));
        pack();
        setLocationRelativeTo(owner);
    }

    public static void show(Component parent, HealthBooking booking) {
        if (booking == null) {
            return;
        }
        Window owner = parent instanceof Window w
            ? w
            : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        BookingCardDialog dialog = new BookingCardDialog(owner, booking);
        dialog.setVisible(true);
    }
}
