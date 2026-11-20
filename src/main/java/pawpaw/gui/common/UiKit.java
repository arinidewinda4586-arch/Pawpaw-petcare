package pawpaw.gui.common;

import pawpaw.util.ImageStore;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.ViewportLayout;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared Pure-Swing factory for web-styled dual-palette controls, rounded cards,
 * KPI summaries, tables, pill renderers, scrollable hosts, and dialogs.
 */
public final class UiKit {

    private static final int FIELD_HEIGHT = 36;
    private static final int CUSTOMER_BUTTON_RADIUS = 14;
    private static final String[] SUPPORTED_IMAGE_EXTENSIONS = { "jpg", "jpeg", "png", "jfif" };

    private UiKit() {
    }

    public static JButton primaryButton(String text) {
        return primaryButton(text, null);
    }

    public static JButton primaryButton(String text, VectorIcons.Kind icon) {
        return new StyledButton(
            text,
            icon,
            Theme.BLUE,
            Theme.NAVY,
            Theme.NAVY,
            Theme.NAVY,
            Color.WHITE,
            Theme.GOLD,
            null,
            CUSTOMER_BUTTON_RADIUS
        );
    }

    public static JButton secondaryButton(String text) {
        return secondaryButton(text, null);
    }

    public static JButton secondaryButton(String text, VectorIcons.Kind icon) {
        return new StyledButton(
            text,
            icon,
            Theme.SURFACE,
            Theme.SURFACE,
            Theme.SURFACE_ALT,
            Theme.BORDER,
            Theme.INK,
            Theme.INK,
            Theme.BORDER,
            CUSTOMER_BUTTON_RADIUS
        );
    }

    public static JButton dangerButton(String text) {
        return dangerButton(text, null);
    }

    public static JButton dangerButton(String text, VectorIcons.Kind icon) {
        return new StyledButton(
            text,
            icon,
            Theme.DANGER_SOLID,
            Theme.DANGER_SOLID,
            Theme.DANGER_HOVER,
            Theme.DANGER_FG,
            Color.WHITE,
            Color.WHITE,
            null,
            CUSTOMER_BUTTON_RADIUS
        );
    }

    public static JButton ghostButton(String text) {
        return ghostButton(text, null);
    }

    public static JButton ghostButton(String text, VectorIcons.Kind icon) {
        return new StyledButton(
            text,
            icon,
            new Color(0, 0, 0, 0),
            new Color(0, 0, 0, 0),
            Theme.SELECTION_BG,
            Theme.tint(Theme.BLUE, 36),
            Theme.BLUE,
            Theme.BLUE,
            null,
            CUSTOMER_BUTTON_RADIUS
        );
    }

    public static PillFilterButton pillFilterButton(String text, Icon icon) {
        return new PillFilterButton(text, icon);
    }

    public static JButton circleIconButton(
            VectorIcons.Kind icon,
            Color bg,
            Color hoverBg,
            Color fg,
            int diameter) {
        return new CircleIconButton(icon, bg, hoverBg, fg, diameter);
    }

    public static JButton adminPrimaryButton(String text, VectorIcons.Kind icon) {
        return new StyledButton(
            text,
            icon,
            Theme.Admin.BLUE,
            Theme.Admin.BLUE,
            Theme.Admin.BLUE_DARK,
            Theme.Admin.NAVY,
            Color.WHITE,
            Color.WHITE,
            null,
            Theme.Admin.BUTTON_RADIUS
        );
    }

    public static JButton adminLightButton(String text, VectorIcons.Kind icon) {
        return new StyledButton(
            text,
            icon,
            Theme.Admin.BTN_LIGHT_BG,
            Theme.Admin.BTN_LIGHT_BG,
            Theme.Admin.BTN_LIGHT_HOVER,
            Theme.Admin.BLUE_SOFT,
            Theme.Admin.BTN_LIGHT_FG,
            Theme.Admin.BTN_LIGHT_FG,
            null,
            Theme.Admin.BUTTON_RADIUS
        );
    }

    public static JButton adminEditButton(String text, VectorIcons.Kind icon) {
        return new StyledButton(
            text,
            icon,
            Theme.Admin.BTN_EDIT_BG,
            Theme.Admin.BTN_EDIT_BG,
            Theme.Admin.BTN_EDIT_HOVER,
            Theme.Admin.BTN_EDIT_HOVER,
            Theme.Admin.BTN_EDIT_FG,
            Theme.Admin.BTN_EDIT_FG,
            null,
            Theme.Admin.BUTTON_RADIUS
        );
    }

    public static JButton adminDeleteButton(String text, VectorIcons.Kind icon) {
        return new StyledButton(
            text,
            icon,
            Theme.Admin.BTN_DELETE_BG,
            Theme.Admin.BTN_DELETE_BG,
            Theme.Admin.BTN_DELETE_HOVER,
            Theme.Admin.BTN_DELETE_HOVER,
            Theme.Admin.BTN_DELETE_FG,
            Theme.Admin.BTN_DELETE_FG,
            null,
            Theme.Admin.BUTTON_RADIUS
        );
    }

    public static JTextField textField(String placeholder) {
        return new PlaceholderTextField(placeholder, null);
    }

    public static JTextField searchField(String placeholder) {
        Icon searchIcon = VectorIcons.of(VectorIcons.Kind.SEARCH, 15, Theme.MUTED);
        return new PlaceholderTextField(placeholder, searchIcon);
    }

    public static JPasswordField passwordField(String placeholder) {
        return new PlaceholderPasswordField(placeholder);
    }

    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> combo = items == null
            ? new DynamicWidthComboBox<>()
            : new DynamicWidthComboBox<>(new DefaultComboBoxModel<>(items));
        combo.setFont(Theme.body());
        combo.setForeground(Theme.INK);
        combo.setBackground(Theme.SURFACE);
        combo.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        return combo;
    }

    public static <T> JComboBox<T> comboBox() {
        return comboBox(null);
    }

    public static JPanel cardPanel() {
        return cardPanel(new BorderLayout(Theme.SPACE_12, Theme.SPACE_12));
    }

    public static JPanel cardPanel(LayoutManager layout) {
        return new RoundedCardPanel(
            layout,
            Theme.SURFACE,
            Theme.BORDER_SUBTLE,
            Theme.CARD_RADIUS,
            Theme.tint(Theme.NAVY, 15)
        );
    }

    public static JPanel adminCardPanel() {
        return adminCardPanel(new BorderLayout(Theme.SPACE_12, Theme.SPACE_12));
    }

    public static JPanel adminCardPanel(LayoutManager layout) {
        return new RoundedCardPanel(
            layout,
            Theme.Admin.SURFACE,
            Theme.Admin.LINE,
            Theme.Admin.CARD_RADIUS,
            Theme.tint(Theme.Admin.NAVY, 15)
        );
    }

    public static JPanel pastelCardPanel(LayoutManager layout, Color fill, Color border, int radius) {
        return new RoundedCardPanel(
            layout,
            fill != null ? fill : Theme.SURFACE,
            border,
            Math.max(0, radius),
            Theme.tint(Theme.NAVY, 12)
        );
    }

    public static KpiCard kpiCard(
            String title,
            String value,
            String subtitle,
            Color accentColor,
            VectorIcons.Kind icon) {
        Color safeAccent = accentColor != null ? accentColor : Theme.BLUE;
        return new KpiCard(
            title,
            value,
            subtitle,
            Theme.SURFACE,
            Theme.BORDER_SUBTLE,
            Theme.INK,
            Theme.MUTED,
            safeAccent,
            Theme.tint(safeAccent, 28),
            34,
            10,
            icon
        );
    }

    public static KpiCard adminKpiCard(
            String title,
            String value,
            String subtitle,
            VectorIcons.Kind icon) {
        return new KpiCard(
            title,
            value,
            subtitle,
            Theme.Admin.SURFACE,
            Theme.Admin.LINE,
            Theme.Admin.TEXT,
            Theme.Admin.TEXT_SOFT,
            Theme.Admin.BLUE,
            Theme.Admin.BLUE_SOFT,
            Theme.Admin.ICON_BOX_SIZE,
            12,
            icon
        );
    }

    public static JTable table(TableModel model) {
        return buildStyledTable(
            model,
            Theme.INK_BODY,
            Theme.SURFACE,
            Theme.SURFACE_ALT,
            Theme.SELECTION_BG,
            Theme.NAVY,
            Theme.BORDER,
            Theme.SURFACE_ALT,
            Theme.MUTED,
            Theme.bodyBold()
        );
    }

    public static JTable adminTable(TableModel model) {
        return buildStyledTable(
            model,
            Theme.Admin.TEXT,
            Theme.Admin.SURFACE,
            Theme.Admin.SURFACE_ALT,
            Theme.Admin.BLUE_SOFT,
            Theme.Admin.TEXT,
            Theme.Admin.LINE,
            Theme.Admin.TABLE_HEADER_BG,
            Theme.Admin.TABLE_HEADER_FG,
            Theme.smallBold()
        );
    }

    private static JTable buildStyledTable(
            TableModel model,
            Color fg,
            Color bg,
            Color altBg,
            Color selectionBg,
            Color selectionFg,
            Color gridColor,
            Color headerBg,
            Color headerFg,
            Font headerFont) {
        Objects.requireNonNull(model, "TableModel tidak boleh null.");
        JTable table = new JTable(model);
        table.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        table.setFont(Theme.body());
        table.setForeground(fg);
        table.setBackground(bg);
        table.setSelectionBackground(selectionBg);
        table.setSelectionForeground(selectionFg);
        table.setGridColor(gridColor);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowSorter(new TableRowSorter<>(model));

        ZebraCellRenderer zebra = new ZebraCellRenderer(bg, altBg, fg);
        table.setDefaultRenderer(Object.class, zebra);
        table.setDefaultRenderer(Number.class, zebra);

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setReorderingAllowed(false);
            header.setBackground(headerBg);
            header.setForeground(headerFg);
            header.setFont(headerFont);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
            header.setDefaultRenderer(new CleanHeaderRenderer(headerBg, headerFg, gridColor, headerFont));
        }
        return table;
    }

    public static JScrollPane scrollPane(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public static JScrollPane verticalScrollPane(JComponent content, Color viewportBg) {
        return createVerticalScrollPane(
            content,
            viewportBg != null ? viewportBg : Theme.SURFACE,
            false
        );
    }

    public static JScrollPane verticalExpandingScrollPane(JComponent content, Color viewportBg) {
        return createVerticalScrollPane(
            content,
            viewportBg != null ? viewportBg : Theme.BG,
            true
        );
    }

    private static JScrollPane createVerticalScrollPane(
            JComponent content,
            Color viewportBg,
            boolean expandToViewportHeight) {
        JScrollPane scroll = new JScrollPane();
        if (expandToViewportHeight) {
            JViewport expandingViewport = new JViewport();
            expandingViewport.setLayout(new ExpandingViewportLayout());
            scroll.setViewport(expandingViewport);
        }
        scroll.setViewportView(new ScrollableStackHost(content, expandToViewportHeight));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(viewportBg);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public static String[] supportedImageExtensions() {
        return SUPPORTED_IMAGE_EXTENSIONS.clone();
    }

    public static String chooseAndCopyImage(Component parent, String subfolder, String prefix) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih Gambar");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Berkas Gambar (JPG, JPEG, PNG, JFIF)",
            supportedImageExtensions()
        ));

        int choice = chooser.showOpenDialog(parent);
        if (choice != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return null;
        }
        try {
            return ImageStore.copyUploadedImage(chooser.getSelectedFile(), subfolder, prefix);
        } catch (IOException | RuntimeException ex) {
            showError(parent, "Gagal mengunggah gambar: " + ex.getMessage());
            return null;
        }
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message != null && !message.isBlank() ? message : "Terjadi kesalahan tak terduga.",
            "Terjadi Kesalahan",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(
            parent,
            message,
            "Informasi",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static boolean confirm(Component parent, String title, String message) {
        int res = JOptionPane.showConfirmDialog(
            parent,
            message,
            title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return res == JOptionPane.YES_OPTION;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font != null ? font : Theme.body());
        lbl.setForeground(color != null ? color : Theme.INK);
        return lbl;
    }

    public static JLabel formLabel(String text) {
        return label(text, Theme.smallBold(), Theme.INK);
    }

    public static JLabel pillLabel(String text, Color bg, Color fg) {
        return new PillLabel(text, bg, fg);
    }

    static void paintPillBadge(
            Graphics g,
            int w,
            int h,
            Color cellBg,
            Color pillBg,
            Color pillFg,
            Font font,
            String text,
            int horizontalAlignment,
            int pillPadX,
            int minWidth) {
        if (w <= 0 || h <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(cellBg != null ? cellBg : Theme.SURFACE);
            g2.fillRect(0, 0, w, h);

            if (w < 6 || h < 6) {
                return;
            }

            Font safeFont = font != null ? font : Theme.badge();
            g2.setFont(safeFont);
            FontMetrics fm = g2.getFontMetrics();

            String safeText = text == null ? "" : text;
            int outerMarginX = Math.min(6, Math.max(2, w / 8));
            int maxPillW = Math.max(4, w - outerMarginX * 2);
            int pillH = h >= 10 ? Math.min(h - 8, 22) : Math.max(4, h - 2);

            int desiredW = fm.stringWidth(safeText) + pillPadX * 2;
            int clampedMinW = Math.min(maxPillW, minWidth);
            int pillW = Math.max(clampedMinW, Math.min(maxPillW, desiredW));

            int pillX;
            if (horizontalAlignment == SwingConstants.LEFT) {
                pillX = Math.min(8, Math.max(outerMarginX, w - pillW - outerMarginX));
            } else if (horizontalAlignment == SwingConstants.RIGHT) {
                pillX = Math.max(outerMarginX, w - pillW - Math.min(8, outerMarginX));
            } else {
                pillX = (w - pillW) / 2;
            }
            int pillY = (h - pillH) / 2;

            g2.setColor(pillBg != null ? pillBg : Theme.NEUTRAL_BG);
            g2.fill(new RoundRectangle2D.Double(pillX, pillY, pillW, pillH, pillH, pillH));

            int innerPad = Math.min(pillPadX, Math.max(2, pillW / 6));
            int maxTextW = Math.max(0, pillW - innerPad * 2);
            String fitted = fitTextWithEllipsis(safeText, safeFont, fm, maxTextW);
            if (!fitted.isEmpty()) {
                int fittedW = fm.stringWidth(fitted);
                int textX = pillX + Math.max(innerPad, (pillW - fittedW) / 2);
                int textY = pillY + ((pillH - fm.getHeight()) / 2) + fm.getAscent();

                Shape prevClip = g2.getClip();
                g2.clip(new RoundRectangle2D.Double(pillX + 1, pillY, Math.max(0, pillW - 2), pillH, pillH, pillH));
                g2.setColor(pillFg != null ? pillFg : Theme.INK);
                g2.drawString(fitted, textX, textY);
                g2.setClip(prevClip);
            }
        } finally {
            g2.dispose();
        }
    }

    private static String fitTextWithEllipsis(String text, Font font, FontMetrics fm, int maxWidth) {
        if (maxWidth <= 0 || text.isEmpty()) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = font.canDisplay('\u2026') ? "\u2026" : "...";
        int ellipsisW = fm.stringWidth(ellipsis);
        if (ellipsisW > maxWidth) {
            for (int len = text.length(); len > 0; len--) {
                String candidate = text.substring(0, len);
                if (fm.stringWidth(candidate) <= maxWidth) {
                    return candidate;
                }
            }
            return "";
        }
        for (int len = text.length() - 1; len > 0; len--) {
            String candidate = text.substring(0, len) + ellipsis;
            if (fm.stringWidth(candidate) <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
    }

    private static Border createRoundedFieldBorder(boolean withLeadingIcon) {
        int leftPad = withLeadingIcon ? 32 : 12;
        return BorderFactory.createEmptyBorder(6, leftPad, 6, 12);
    }

    private static void attachFocusRepaint(JComponent field) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.repaint();
            }
        });
    }

    private static void paintRoundedTextComponent(
            Graphics g,
            JComponent c,
            Color idleBg,
            String placeholder,
            boolean showPlaceholder,
            Icon leadingIcon,
            Insets insets,
            Consumer<Graphics> superPainter) {
        paintRoundedFieldBackground(g, c, idleBg);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(new RoundRectangle2D.Double(
                2.0,
                2.0,
                Math.max(1.0, c.getWidth() - 4.0),
                Math.max(1.0, c.getHeight() - 4.0),
                Math.max(2, Theme.INPUT_RADIUS - 2),
                Math.max(2, Theme.INPUT_RADIUS - 2)
            ));
            superPainter.accept(g2);
        } finally {
            g2.dispose();
        }
        if (leadingIcon != null) {
            int iy = (c.getHeight() - leadingIcon.getIconHeight()) / 2;
            leadingIcon.paintIcon(c, g, 11, iy);
        }
        if (showPlaceholder) {
            paintPlaceholderText(g, c, placeholder, insets);
        }
        paintRoundedFieldBorder(g, c);
    }

    private static void paintRoundedFieldBackground(Graphics g, JComponent c, Color idleBg) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill;
            if (!c.isEnabled()) {
                fill = Theme.SURFACE_ALT;
            } else if (c.isFocusOwner()) {
                fill = Theme.SURFACE;
            } else {
                fill = idleBg != null ? idleBg : Theme.AUTH_INPUT_BG;
            }
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(
                1.0,
                1.0,
                Math.max(1, c.getWidth() - 2),
                Math.max(1, c.getHeight() - 2),
                Theme.INPUT_RADIUS,
                Theme.INPUT_RADIUS
            ));
        } finally {
            g2.dispose();
        }
    }

    private static void paintRoundedFieldBorder(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean focused = c.isFocusOwner() && c.isEnabled();
            g2.setStroke(new BasicStroke(focused ? 2.0f : 1.5f));
            g2.setColor(focused ? Theme.BANNER_BLUE : Theme.BORDER);
            double offset = focused ? 1.0 : 0.75;
            g2.draw(new RoundRectangle2D.Double(
                offset,
                offset,
                Math.max(1.0, c.getWidth() - offset * 2.0),
                Math.max(1.0, c.getHeight() - offset * 2.0),
                Theme.INPUT_RADIUS,
                Theme.INPUT_RADIUS
            ));
        } finally {
            g2.dispose();
        }
    }

    private static void paintPlaceholderText(Graphics g, JComponent c, String placeholder, Insets insets) {
        if (placeholder == null || placeholder.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(c.getFont());
            g2.setColor(Theme.MUTED);
            FontMetrics fm = g2.getFontMetrics();
            int y = insets.top + ((c.getHeight() - insets.top - insets.bottom - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(placeholder, insets.left, y);
        } finally {
            g2.dispose();
        }
    }

    private static final class StyledButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color bgTop;
        private final Color bgBottom;
        private final Color hoverBg;
        private final Color pressedBg;
        private final Color fg;
        private final Color hoverFg;
        private final Color borderColor;
        private final int radius;
        private final transient Icon normalIcon;
        private final transient Icon hoverIcon;
        private final transient Icon disabledIcon;

        StyledButton(
                String text,
                VectorIcons.Kind iconKind,
                Color bgTop,
                Color bgBottom,
                Color hoverBg,
                Color pressedBg,
                Color fg,
                Color hoverFg,
                Color borderColor,
                int radius) {
            super(text);
            this.bgTop = bgTop;
            this.bgBottom = bgBottom;
            this.hoverBg = hoverBg;
            this.pressedBg = pressedBg;
            this.fg = fg;
            this.hoverFg = hoverFg != null ? hoverFg : fg;
            this.borderColor = borderColor;
            this.radius = radius;

            Color disabledColor = borderColor != null ? Theme.MUTED : Color.WHITE;
            this.normalIcon = iconKind != null ? VectorIcons.of(iconKind, 16, fg) : null;
            this.hoverIcon = iconKind != null ? VectorIcons.of(iconKind, 16, this.hoverFg) : null;
            this.disabledIcon = iconKind != null ? VectorIcons.of(iconKind, 16, disabledColor) : null;

            setFont(Theme.bodyBold());
            setForeground(fg);
            setIcon(normalIcon);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setIconTextGap(8);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

            getModel().addChangeListener(e -> syncVisualState());
        }

        private void syncVisualState() {
            Color targetFg;
            Icon targetIcon;
            if (!isEnabled()) {
                targetFg = borderColor != null ? Theme.MUTED : Color.WHITE;
                targetIcon = disabledIcon;
            } else if (getModel().isRollover()) {
                targetFg = hoverFg;
                targetIcon = hoverIcon;
            } else {
                targetFg = fg;
                targetIcon = normalIcon;
            }
            if (!Objects.equals(getForeground(), targetFg)) {
                setForeground(targetFg);
            }
            if (getIcon() != targetIcon) {
                setIcon(targetIcon);
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            syncVisualState();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                if (w > 1 && h > 1) {
                    Shape shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, radius, radius);
                    if (!isEnabled()) {
                        Color disabledFill = borderColor != null ? Theme.SURFACE_ALT : new Color(0x94, 0xA3, 0xB8);
                        if (disabledFill.getAlpha() > 0) {
                            g2.setColor(disabledFill);
                            g2.fill(shape);
                        }
                    } else if (getModel().isPressed()) {
                        if (pressedBg.getAlpha() > 0) {
                            g2.setColor(pressedBg);
                            g2.fill(shape);
                        }
                    } else if (getModel().isRollover()) {
                        if (hoverBg.getAlpha() > 0) {
                            g2.setColor(hoverBg);
                            g2.fill(shape);
                        }
                    } else if (bgTop.getAlpha() > 0 || bgBottom.getAlpha() > 0) {
                        if (bgTop.equals(bgBottom)) {
                            g2.setColor(bgTop);
                        } else {
                            g2.setPaint(new GradientPaint(0, 0, bgTop, 0, h, bgBottom));
                        }
                        g2.fill(shape);
                    }

                    if (borderColor != null) {
                        g2.setStroke(new BasicStroke(isFocusOwner() ? 2.0f : 1.5f));
                        g2.setColor(isFocusOwner() ? Theme.BLUE : borderColor);
                        g2.draw(new RoundRectangle2D.Double(0.75, 0.75, w - 1.5, h - 1.5, radius, radius));
                    } else if (isFocusOwner()) {
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.setColor(Theme.GOLD);
                        g2.draw(new RoundRectangle2D.Double(1.0, 1.0, w - 2.0, h - 2.0, radius, radius));
                    }
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Rounded pill filter button for category and species toggles.
     */
    public static final class PillFilterButton extends JButton {
        private static final long serialVersionUID = 1L;
        private boolean selectedPill;

        PillFilterButton(String text, Icon icon) {
            super(text, icon);
            setFont(Theme.smallBold());
            setForeground(Theme.PILL_IDLE_FG);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setIconTextGap(6);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        }

        public boolean isSelectedPill() {
            return selectedPill;
        }

        public void setSelectedPill(boolean selectedPill) {
            this.selectedPill = selectedPill;
            setForeground(selectedPill ? Color.WHITE : Theme.PILL_IDLE_FG);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                if (w > 1 && h > 1) {
                    Color fill;
                    if (!isEnabled()) {
                        fill = Theme.SURFACE_ALT;
                    } else if (selectedPill) {
                        fill = getModel().isRollover() ? Theme.BLUE_HOVER : Theme.BLUE;
                    } else if (getModel().isPressed() || getModel().isRollover()) {
                        fill = Theme.BORDER;
                    } else {
                        fill = Theme.PILL_IDLE_BG;
                    }
                    g2.setColor(fill);
                    g2.fill(new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, h, h));
                    if (isFocusOwner()) {
                        g2.setColor(selectedPill ? Theme.GOLD : Theme.BLUE);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.draw(new RoundRectangle2D.Double(1.0, 1.0, w - 2.0, h - 2.0, h, h));
                    }
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class CircleIconButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color bg;
        private final Color hoverBg;
        private final transient Icon normalIcon;
        private final transient Icon disabledIcon;

        CircleIconButton(VectorIcons.Kind kind, Color bg, Color hoverBg, Color fg, int diameter) {
            super();
            int safeD = Math.max(24, diameter);
            this.bg = bg != null ? bg : Theme.CYAN_BTN;
            this.hoverBg = hoverBg != null ? hoverBg : Theme.CYAN_BTN_HOVER;
            Color safeFg = fg != null ? fg : Theme.INK;
            int iconSize = Math.max(12, Math.round(safeD * 0.48f));
            this.normalIcon = kind != null ? VectorIcons.of(kind, iconSize, safeFg) : null;
            this.disabledIcon = kind != null ? VectorIcons.of(kind, iconSize, Theme.MUTED) : null;

            Dimension fixed = new Dimension(safeD, safeD);
            setPreferredSize(fixed);
            setMinimumSize(fixed);
            setMaximumSize(fixed);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(BorderFactory.createEmptyBorder());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                if (w > 1 && h > 1) {
                    Color fill;
                    if (!isEnabled()) {
                        fill = Theme.SURFACE_ALT;
                    } else if (getModel().isPressed() || getModel().isRollover()) {
                        fill = hoverBg;
                    } else {
                        fill = bg;
                    }
                    g2.setColor(fill);
                    g2.fill(new Ellipse2D.Double(0.5, 0.5, w - 1.0, h - 1.0));
                    if (isFocusOwner()) {
                        g2.setColor(Theme.BLUE);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.draw(new Ellipse2D.Double(1.0, 1.0, w - 2.0, h - 2.0));
                    }
                    Icon icon = isEnabled() ? normalIcon : disabledIcon;
                    if (icon != null) {
                        int ix = (w - icon.getIconWidth()) / 2;
                        int iy = (h - icon.getIconHeight()) / 2;
                        icon.paintIcon(this, g2, ix, iy);
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class RoundedCardPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color borderColor;
        private final int radius;
        private final Color shadowColor;

        RoundedCardPanel(
                LayoutManager layout,
                Color fill,
                Color borderColor,
                int radius,
                Color shadowColor) {
            super(layout);
            this.borderColor = borderColor;
            this.radius = Math.max(0, radius);
            this.shadowColor = shadowColor != null ? shadowColor : Theme.tint(Theme.NAVY, 15);
            setOpaque(false);
            setBackground(fill != null ? fill : Theme.SURFACE);
            setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_16,
                Theme.SPACE_16,
                Theme.SPACE_16 + 2,
                Theme.SPACE_16 + 2
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintSoftRoundedCard(
                g,
                getWidth(),
                getHeight(),
                getBackground(),
                borderColor,
                shadowColor,
                radius
            );
            super.paintComponent(g);
        }
    }

    private static void paintSoftRoundedCard(
            Graphics g,
            int w,
            int h,
            Color fill,
            Color borderColor,
            Color shadowColor,
            int radius) {
        if (w <= 3 || h <= 3) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double arc = Math.min(radius * 2.0, Math.min(w, h));
            double cardW = Math.max(1.0, w - 3.0);
            double cardH = Math.max(1.0, h - 3.0);

            if (shadowColor != null && shadowColor.getAlpha() > 0) {
                g2.setColor(shadowColor);
                g2.fill(new RoundRectangle2D.Double(1.5, 2.5, cardW, cardH, arc, arc));
            }
            g2.setColor(fill != null ? fill : Theme.SURFACE);
            g2.fill(new RoundRectangle2D.Double(0.5, 0.5, cardW, cardH, arc, arc));

            if (borderColor != null) {
                g2.setStroke(new BasicStroke(1.0f));
                g2.setColor(borderColor);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, cardW, cardH, arc, arc));
            }
        } finally {
            g2.dispose();
        }
    }

    private static final class PlaceholderTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;
        private final transient Icon leadingIcon;

        PlaceholderTextField(String placeholder, Icon leadingIcon) {
            this.placeholder = placeholder;
            this.leadingIcon = leadingIcon;
            setOpaque(false);
            setFont(Theme.body());
            setForeground(Theme.INK);
            setBackground(Theme.AUTH_INPUT_BG);
            setCaretColor(Theme.INK);
            setBorder(createRoundedFieldBorder(leadingIcon != null));
            setPreferredSize(new Dimension(200, FIELD_HEIGHT));
            setMinimumSize(new Dimension(80, FIELD_HEIGHT));
            attachFocusRepaint(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintRoundedTextComponent(
                g,
                this,
                getBackground(),
                placeholder,
                getText().isEmpty(),
                leadingIcon,
                getInsets(),
                super::paintComponent
            );
        }
    }

    private static final class PlaceholderPasswordField extends JPasswordField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        PlaceholderPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(Theme.body());
            setForeground(Theme.INK);
            setBackground(Theme.AUTH_INPUT_BG);
            setCaretColor(Theme.INK);
            setBorder(createRoundedFieldBorder(false));
            setPreferredSize(new Dimension(200, FIELD_HEIGHT));
            setMinimumSize(new Dimension(80, FIELD_HEIGHT));
            attachFocusRepaint(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintRoundedTextComponent(
                g,
                this,
                getBackground(),
                placeholder,
                getPassword().length == 0,
                null,
                getInsets(),
                super::paintComponent
            );
        }
    }

    private static final class DynamicWidthComboBox<T> extends JComboBox<T> {
        private static final long serialVersionUID = 1L;

        DynamicWidthComboBox() {
            super();
        }

        DynamicWidthComboBox(DefaultComboBoxModel<T> model) {
            super(model);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(120, d.width), FIELD_HEIGHT);
        }
    }

    private static final class ZebraCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final Color evenBg;
        private final Color oddBg;
        private final Color defaultFg;

        ZebraCellRenderer(Color evenBg, Color oddBg, Color defaultFg) {
            this.evenBg = evenBg;
            this.oddBg = oddBg;
            this.defaultFg = defaultFg;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(Theme.body());
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(row % 2 == 0 ? evenBg : oddBg);
                setForeground(defaultFg);
            }
            if (value instanceof Number) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            return this;
        }
    }

    private static final class CleanHeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final Color headerBg;
        private final Color headerFg;
        private final Color lineColor;
        private final Font headerFont;

        CleanHeaderRenderer(Color headerBg, Color headerFg, Color lineColor, Font headerFont) {
            this.headerBg = headerBg;
            this.headerFg = headerFg;
            this.lineColor = lineColor;
            this.headerFont = headerFont;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(headerFont);
            setBackground(headerBg);
            setForeground(headerFg);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, lineColor),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
            ));
            return this;
        }
    }

    private static final class PillLabel extends JLabel {
        private static final long serialVersionUID = 1L;
        private final Color pillBg;

        PillLabel(String text, Color pillBg, Color pillFg) {
            super(text, SwingConstants.CENTER);
            this.pillBg = pillBg != null ? pillBg : Theme.NEUTRAL_BG;
            setFont(Theme.badge());
            setForeground(pillFg != null ? pillFg : Theme.INK);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(pillBg);
                int h = getHeight();
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), h, h, h));
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Reusable rounded KPI summary card with vector icon badge and live value updates.
     */
    public static final class KpiCard extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color cardFill;
        private final Color borderColor;
        private final JLabel valueLabel;
        private final JLabel subtitleLabel;

        KpiCard(
                String title,
                String value,
                String subtitle,
                Color cardFill,
                Color borderColor,
                Color valueColor,
                Color mutedColor,
                Color iconFg,
                Color iconBg,
                int iconBoxSize,
                int iconBoxRadius,
                VectorIcons.Kind icon) {
            super(new BorderLayout(Theme.SPACE_8, Theme.SPACE_8));
            this.cardFill = cardFill != null ? cardFill : Theme.SURFACE;
            this.borderColor = borderColor != null ? borderColor : Theme.BORDER_SUBTLE;

            setOpaque(false);
            setBackground(this.cardFill);
            setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 18));

            JPanel topRow = new JPanel(new BorderLayout(Theme.SPACE_8, 0));
            topRow.setOpaque(false);
            JLabel titleLabel = label(title != null ? title : "", Theme.smallBold(), mutedColor);
            topRow.add(titleLabel, BorderLayout.CENTER);

            if (icon != null) {
                JLabel iconBadge = new IconBadge(icon, iconFg, iconBg, iconBoxSize, iconBoxRadius);
                topRow.add(iconBadge, BorderLayout.EAST);
            }

            valueLabel = label(value != null ? value : "0", Theme.kpiValue(), valueColor);
            subtitleLabel = label(subtitle != null ? subtitle : "", Theme.small(), mutedColor);

            add(topRow, BorderLayout.NORTH);
            add(valueLabel, BorderLayout.CENTER);
            add(subtitleLabel, BorderLayout.SOUTH);
        }

        public void setValue(String value) {
            valueLabel.setText(value != null ? value : "0");
        }

        public void setSubtitle(String subtitle) {
            subtitleLabel.setText(subtitle != null ? subtitle : "");
        }

        @Override
        protected void paintComponent(Graphics g) {
            paintSoftRoundedCard(
                g,
                getWidth(),
                getHeight(),
                cardFill,
                borderColor,
                Theme.tint(Theme.NAVY, 14),
                Theme.Admin.CARD_RADIUS
            );
            super.paintComponent(g);
        }
    }

    private static final class IconBadge extends JLabel {
        private static final long serialVersionUID = 1L;
        private final Color tintBg;
        private final int cornerRadius;

        IconBadge(VectorIcons.Kind kind, Color fg, Color tintBg, int boxSize, int cornerRadius) {
            super(VectorIcons.of(kind, Math.max(14, boxSize / 2), fg), SwingConstants.CENTER);
            this.tintBg = tintBg != null ? tintBg : Theme.tint(fg, 28);
            this.cornerRadius = cornerRadius;
            Dimension fixed = new Dimension(boxSize, boxSize);
            setPreferredSize(fixed);
            setMinimumSize(fixed);
            setMaximumSize(fixed);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(tintBg);
                g2.fill(new RoundRectangle2D.Double(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    cornerRadius,
                    cornerRadius
                ));
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Anchors vertical stacks at {@link BorderLayout#NORTH} (or expands {@link BorderLayout#CENTER}
     * via {@link ExpandingViewportLayout} when viewport exceeds preferred height) while tracking
     * viewport width so layouts never clip horizontally.
     */
    private static final class ScrollableStackHost extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        ScrollableStackHost(JComponent content, boolean expandToViewportHeight) {
            super(new BorderLayout());
            setOpaque(false);
            if (content != null) {
                add(content, expandToViewportHeight ? BorderLayout.CENTER : BorderLayout.NORTH);
            }
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect != null ? Math.max(16, visibleRect.height - 16) : 64;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * Extends {@link ViewportLayout} to fill excess vertical viewport space on tall screens
     * while keeping {@link Scrollable#getScrollableTracksViewportHeight()} {@code false} so
     * {@link javax.swing.ScrollPaneLayout} toggles the vertical scrollbar accurately in one pass.
     */
    private static final class ExpandingViewportLayout extends ViewportLayout {
        private static final long serialVersionUID = 1L;

        @Override
        public void layoutContainer(Container parent) {
            super.layoutContainer(parent);
            if (parent instanceof JViewport vp) {
                Component view = vp.getView();
                int vpHeight = vp.getHeight();
                if (view != null && vpHeight > 0 && view.getHeight() < vpHeight) {
                    vp.setViewSize(new Dimension(view.getWidth(), vpHeight));
                    view.validate();
                }
            }
        }
    }
}
