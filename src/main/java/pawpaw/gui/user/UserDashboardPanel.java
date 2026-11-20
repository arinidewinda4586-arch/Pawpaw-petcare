package pawpaw.gui.user;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.PagePanel;
import pawpaw.gui.common.UiKit;
import pawpaw.model.User;
import pawpaw.service.ProfileService;
import pawpaw.util.FormatUtil;
import pawpaw.util.ImageStore;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Customer home workspace displaying greeting header, 4 website-matched pastel quick menu cards,
 * personal KPIs, and recent activities inside an expanding vertical scroll viewport.
 */
public final class UserDashboardPanel extends PagePanel {

    private static final long serialVersionUID = 1L;

    private final transient AppWindow app;

    private final JLabel inlineErrorLabel;
    private final JPanel errorBannerSlot;
    private final JLabel greetingTitleLabel;
    private final UiKit.KpiCard petsCard;
    private final UiKit.KpiCard ordersCard;
    private final UiKit.KpiCard adoptionsCard;
    private final UiKit.KpiCard bookingsCard;

    private final WorkspaceWidgets.ReadOnlyTableModel activityModel;
    private final JTable activityTable;
    private final JLabel activityHintLabel;

    private String statusSummary = "Beranda pelanggan siap.";

    public UserDashboardPanel(AppWindow app) {
        this.app = Objects.requireNonNull(app, "AppWindow tidak boleh null.");

        inlineErrorLabel = WorkspaceWidgets.createFeedbackBanner();
        errorBannerSlot = WorkspaceWidgets.createBannerSlot(inlineErrorLabel, Theme.SPACE_12);

        greetingTitleLabel = UiKit.label("Halo, Sahabat PawPaw!", Theme.title(), Theme.INK);
        JLabel greetingSubtitleLabel = UiKit.label(
            "Pilih layanan untuk merawat sahabat berbulumu hari ini.",
            Theme.small(),
            Theme.MUTED
        );
        JPanel greetingPanel = new WorkspaceWidgets.CompactSectionPanel(new BorderLayout(0, 2));
        greetingPanel.add(greetingTitleLabel, BorderLayout.NORTH);
        greetingPanel.add(greetingSubtitleLabel, BorderLayout.CENTER);

        JPanel menuGrid = new WorkspaceWidgets.CompactSectionPanel(new GridLayout(1, 4, 14, 0));
        menuGrid.add(new PastelMenuCard(
            "Adopsi",
            "Temukan sahabat baru",
            Theme.PASTEL_PINK,
            "assets/img/meng.png",
            VectorIcons.Kind.PAW,
            () -> app.navigate("user.adoption")
        ));
        menuGrid.add(new PastelMenuCard(
            "Toko",
            "Pakan & aksesori",
            Theme.PASTEL_BLUE,
            "assets/img/food.png",
            VectorIcons.Kind.SHOP,
            () -> app.navigate("user.shop")
        ));
        menuGrid.add(new PastelMenuCard(
            "Cek Kesehatan",
            "Konsultasi dokter",
            Theme.PASTEL_GREEN,
            "assets/img/vet.png",
            VectorIcons.Kind.HEALTH,
            () -> app.navigate("user.health")
        ));
        menuGrid.add(new PastelMenuCard(
            "Jadwalkan Vaksin",
            "Imunisasi rutin",
            Theme.PASTEL_LAVENDER,
            "assets/img/vaksin.png",
            VectorIcons.Kind.SERVICES,
            () -> app.navigate("user.health")
        ));

        petsCard = UiKit.kpiCard(
            "Hewan Peliharaan",
            "0",
            "Hewan saya yang terdaftar",
            Theme.BLUE,
            VectorIcons.Kind.PAW
        );
        ordersCard = UiKit.kpiCard(
            "Pesanan",
            "0",
            "Total transaksi toko",
            Theme.SUCCESS_FG,
            VectorIcons.Kind.ORDERS
        );
        adoptionsCard = UiKit.kpiCard(
            "Pengajuan Adopsi",
            "0",
            "Permohonan adopsi Anda",
            Theme.WARNING_FG,
            VectorIcons.Kind.ADOPTION
        );
        bookingsCard = UiKit.kpiCard(
            "Booking Kesehatan",
            "0",
            "Janji layanan klinik",
            Theme.INFO_FG,
            VectorIcons.Kind.BOOKINGS
        );

        JPanel kpiGrid = new WorkspaceWidgets.CompactSectionPanel(new GridLayout(1, 4, Theme.SPACE_12, 0));
        kpiGrid.add(petsCard);
        kpiGrid.add(ordersCard);
        kpiGrid.add(adoptionsCard);
        kpiGrid.add(bookingsCard);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setOpaque(false);
        errorBannerSlot.setAlignmentX(LEFT_ALIGNMENT);
        greetingPanel.setAlignmentX(LEFT_ALIGNMENT);
        menuGrid.setAlignmentX(LEFT_ALIGNMENT);
        kpiGrid.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(errorBannerSlot);
        northStack.add(greetingPanel);
        northStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        northStack.add(menuGrid);
        northStack.add(Box.createVerticalStrut(Theme.SPACE_12));
        northStack.add(kpiGrid);

        activityModel = new WorkspaceWidgets.ReadOnlyTableModel(
            new Object[] { "Kategori", "Aktivitas", "Keterangan", "Tanggal" }
        );
        activityTable = UiKit.table(activityModel);
        configureColumnWidths();

        JPanel tableHeader = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
        tableHeader.setOpaque(false);
        tableHeader.add(UiKit.label("Aktivitas Terbaru", Theme.heading(), Theme.INK), BorderLayout.WEST);
        activityHintLabel = UiKit.label(
            "Menampilkan hingga 5 aktivitas terbaru.",
            Theme.small(),
            Theme.MUTED
        );
        tableHeader.add(activityHintLabel, BorderLayout.EAST);

        JScrollPane tableScroll = UiKit.scrollPane(activityTable);
        tableScroll.setPreferredSize(new Dimension(0, 220));
        tableScroll.setMinimumSize(new Dimension(0, 160));

        JPanel activityCard = UiKit.cardPanel(new BorderLayout(0, Theme.SPACE_12));
        activityCard.add(tableHeader, BorderLayout.NORTH);
        activityCard.add(tableScroll, BorderLayout.CENTER);

        JPanel dashboardBody = new JPanel(new BorderLayout(0, Theme.SPACE_16));
        dashboardBody.setOpaque(false);
        dashboardBody.add(northStack, BorderLayout.NORTH);
        dashboardBody.add(activityCard, BorderLayout.CENTER);

        add(WorkspaceWidgets.verticalExpandingScrollPane(dashboardBody, Theme.BG), BorderLayout.CENTER);
    }

    private void configureColumnWidths() {
        activityTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        activityTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        activityTable.getColumnModel().getColumn(2).setPreferredWidth(360);
        activityTable.getColumnModel().getColumn(3).setPreferredWidth(150);
    }

    @Override
    public String getPageKey() {
        return "user.dashboard";
    }

    @Override
    public String getPageTitle() {
        return "Beranda Pelanggan";
    }

    @Override
    public String getPageSubtitle() {
        return "Ringkasan peliharaan, pesanan, pengajuan adopsi, dan booking aktif";
    }

    @Override
    public void refreshData() {
        WorkspaceWidgets.hideBannerInSlot(errorBannerSlot, inlineErrorLabel);
        User user = app.getSession().getUser();
        if (user == null) {
            greetingTitleLabel.setText("Halo, Sahabat PawPaw!");
            resetSummaryView();
            activityHintLabel.setText("Masuk untuk melihat aktivitas akun Anda.");
            statusSummary = "Belum ada sesi pengguna aktif.";
            revalidate();
            repaint();
            return;
        }

        String displayName = (user.getName() != null && !user.getName().isBlank())
            ? user.getName().trim()
            : "Sahabat PawPaw";
        greetingTitleLabel.setText("Halo, " + displayName + "!");

        try {
            ProfileService.ProfileSummary summary = app.getProfileService().loadSummary(user.getUserId());
            int petCount = summary.pets().size();
            petsCard.setValue(String.valueOf(petCount));
            ordersCard.setValue(String.valueOf(summary.ordersCount()));
            adoptionsCard.setValue(String.valueOf(summary.adoptionsCount()));
            bookingsCard.setValue(String.valueOf(summary.bookingsCount()));

            activityModel.setRowCount(0);
            for (ProfileService.ActivityItem item : summary.recentActivities()) {
                activityModel.addRow(new Object[] {
                    WorkspaceWidgets.safeText(item.category()),
                    WorkspaceWidgets.safeText(item.title()),
                    WorkspaceWidgets.safeText(item.description()),
                    FormatUtil.dateTimeId(item.date())
                });
            }

            if (summary.recentActivities().isEmpty()) {
                activityHintLabel.setText("Belum ada aktivitas transaksi atau layanan terbaru.");
            } else {
                activityHintLabel.setText(
                    "Menampilkan " + summary.recentActivities().size() + " aktivitas terbaru."
                );
            }

            statusSummary = "Beranda dimuat: "
                + petCount + " hewan, "
                + summary.ordersCount() + " pesanan, "
                + summary.adoptionsCount() + " adopsi, "
                + summary.bookingsCount() + " booking.";
        } catch (RuntimeException ex) {
            resetSummaryView();
            String msg = WorkspaceWidgets.safeErrorMessage(ex);
            WorkspaceWidgets.showBannerInSlot(
                errorBannerSlot,
                inlineErrorLabel,
                "Gagal memuat ringkasan beranda: " + msg,
                true
            );
            activityHintLabel.setText("Data aktivitas tidak tersedia saat koneksi bermasalah.");
            statusSummary = "Gagal memuat beranda: " + msg;
        }
        revalidate();
        repaint();
    }

    @Override
    public String getStatusSummary() {
        return statusSummary;
    }

    private void resetSummaryView() {
        petsCard.setValue("0");
        ordersCard.setValue("0");
        adoptionsCard.setValue("0");
        bookingsCard.setValue("0");
        activityModel.setRowCount(0);
    }

    /**
     * Custom Java2D pastel quick-navigation button matching website {@code home.blade.php} cards.
     */
    private static final class PastelMenuCard extends JButton {
        private static final long serialVersionUID = 1L;
        private final String subtitle;
        private final Color pastelBg;
        private final Color hoverBg;
        private final transient BufferedImage imageLarge;
        private final transient BufferedImage imageCompact;
        private final transient Icon fallbackIcon;

        PastelMenuCard(
                String title,
                String subtitle,
                Color pastelBg,
                String imagePath,
                VectorIcons.Kind fallbackKind,
                Runnable onClick) {
            super(title);
            this.subtitle = subtitle != null ? subtitle : "";
            this.pastelBg = pastelBg != null ? pastelBg : Theme.PASTEL_BLUE;
            this.hoverBg = blendLighter(this.pastelBg, 0.14f);
            this.imageLarge = ImageStore.loadScaledFit(imagePath, 54, 54, false);
            this.imageCompact = ImageStore.loadScaledFit(imagePath, 42, 42, false);
            this.fallbackIcon = VectorIcons.of(fallbackKind, 34, Theme.INK);

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(170, 102));
            setMinimumSize(new Dimension(145, 94));
            setToolTipText(title + " - " + this.subtitle);

            if (onClick != null) {
                addActionListener(e -> onClick.run());
            }
            getModel().addChangeListener(e -> repaint());
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 3 || h <= 3) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                boolean active = getModel().isRollover() || getModel().isPressed() || isFocusOwner();
                double cardW = Math.max(1.0, w - 3.0);
                double cardH = Math.max(1.0, h - 3.0);
                double arc = Math.min(Theme.CARD_RADIUS * 2.0, Math.min(cardW, cardH));

                g2.setColor(Theme.tint(Theme.NAVY, active ? 26 : 14));
                g2.fill(new RoundRectangle2D.Double(1.5, 2.5, cardW, cardH, arc, arc));

                g2.setColor(getModel().isRollover() ? hoverBg : pastelBg);
                g2.fill(new RoundRectangle2D.Double(0.5, 0.5, cardW, cardH, arc, arc));

                g2.setStroke(new BasicStroke(active ? 2.0f : 1.0f));
                g2.setColor(active ? Theme.BLUE : Theme.tint(Theme.NAVY, 24));
                double offset = active ? 1.0 : 0.5;
                g2.draw(new RoundRectangle2D.Double(
                    offset,
                    offset,
                    Math.max(1.0, w - offset * 2.0 - 1.0),
                    Math.max(1.0, h - offset * 2.0 - 1.0),
                    arc,
                    arc
                ));

                boolean compact = w < 215;
                int leftPad = compact ? 12 : 16;
                int gap = compact ? 10 : 12;
                int rightPad = compact ? 10 : 14;
                BufferedImage art = compact ? imageCompact : imageLarge;
                int artW = art != null ? art.getWidth() : fallbackIcon.getIconWidth();
                int artH = art != null ? art.getHeight() : fallbackIcon.getIconHeight();
                int artY = (h - artH) / 2;

                if (art != null) {
                    g2.drawImage(art, leftPad, artY, null);
                } else {
                    fallbackIcon.paintIcon(this, g2, leftPad, artY);
                }

                int textX = leftPad + artW + gap;
                int availTextW = Math.max(24, w - textX - rightPad);

                Font titleFont = Theme.heading().deriveFont(compact ? 13.5f : 15f);
                Font subFont = Theme.small().deriveFont(compact ? 11f : 12f);

                g2.setFont(titleFont);
                FontMetrics titleFm = g2.getFontMetrics();
                List<String> titleLines = wrapWords(getText(), titleFm, availTextW, 2);

                g2.setFont(subFont);
                FontMetrics subFm = g2.getFontMetrics();
                List<String> subLines = wrapWords(subtitle, subFm, availTextW, 2);

                int titleBlockH = titleLines.size() * titleFm.getHeight();
                int subBlockH = subLines.isEmpty() ? 0 : (3 + subLines.size() * subFm.getHeight());
                int totalTextH = titleBlockH + subBlockH;
                int curY = Math.max(8, (h - totalTextH) / 2);

                g2.setFont(titleFont);
                g2.setColor(Theme.INK);
                for (String line : titleLines) {
                    g2.drawString(line, textX, curY + titleFm.getAscent());
                    curY += titleFm.getHeight();
                }

                if (!subLines.isEmpty()) {
                    curY += 3;
                    g2.setFont(subFont);
                    g2.setColor(Theme.MUTED);
                    for (String line : subLines) {
                        g2.drawString(line, textX, curY + subFm.getAscent());
                        curY += subFm.getHeight();
                    }
                }
            } finally {
                g2.dispose();
            }
        }

        private static Color blendLighter(Color base, float whiteRatio) {
            int r = Math.min(255, Math.round(base.getRed() + (255 - base.getRed()) * whiteRatio));
            int g = Math.min(255, Math.round(base.getGreen() + (255 - base.getGreen()) * whiteRatio));
            int b = Math.min(255, Math.round(base.getBlue() + (255 - base.getBlue()) * whiteRatio));
            return new Color(r, g, b);
        }

        private static List<String> wrapWords(String text, FontMetrics fm, int maxWidth, int maxLines) {
            List<String> lines = new ArrayList<>();
            if (text == null || text.isBlank() || maxWidth <= 0) {
                return lines;
            }
            String[] words = text.trim().split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (fm.stringWidth(candidate) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                        current.setLength(0);
                        if (lines.size() >= maxLines) {
                            break;
                        }
                    }
                    if (fm.stringWidth(word) <= maxWidth) {
                        current.append(word);
                    } else {
                        current.append(trimToWidth(word, fm, maxWidth));
                    }
                }
            }
            if (!current.isEmpty() && lines.size() < maxLines) {
                lines.add(current.toString());
            }
            return lines;
        }

        private static String trimToWidth(String word, FontMetrics fm, int maxWidth) {
            for (int len = word.length(); len > 0; len--) {
                String s = word.substring(0, len);
                if (fm.stringWidth(s) <= maxWidth) {
                    return s;
                }
            }
            return "";
        }
    }
}
