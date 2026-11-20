package pawpaw.gui.user;

import pawpaw.gui.common.UiKit;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.View;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Package-private Pure-Swing layout, table, badge, and validation banner helpers
 * shared across the customer workspace panels and modal dialogs.
 */
final class WorkspaceWidgets {

    private WorkspaceWidgets() {
    }

    static JScrollPane verticalScrollPane(JComponent content) {
        return UiKit.verticalScrollPane(content, Theme.SURFACE);
    }

    static JScrollPane verticalScrollPane(JComponent content, Color viewportBg) {
        return UiKit.verticalScrollPane(content, viewportBg);
    }

    static JScrollPane verticalExpandingScrollPane(JComponent content, Color viewportBg) {
        return UiKit.verticalExpandingScrollPane(content, viewportBg);
    }

    static LayoutManager wrapFlowLayout(int hgap, int vgap) {
        return new WrapFlowLayout(FlowLayout.LEFT, hgap, vgap);
    }

    static JLabel createFeedbackBanner() {
        JLabel label = new JLabel("");
        label.setFont(Theme.small());
        label.setForeground(Theme.DANGER_FG);
        label.setOpaque(true);
        label.setVisible(false);
        return label;
    }

    static JPanel createBannerSlot(JLabel banner, int bottomGap) {
        JPanel slot = new JPanel(new BorderLayout());
        slot.setOpaque(false);
        if (bottomGap > 0) {
            slot.setBorder(BorderFactory.createEmptyBorder(0, 0, bottomGap, 0));
        }
        slot.add(banner, BorderLayout.CENTER);
        slot.setVisible(false);
        return slot;
    }

    static void showBanner(JLabel target, String message, boolean error) {
        if (target == null) {
            return;
        }
        Color bg = error ? Theme.DANGER_BG : Theme.SUCCESS_BG;
        Color fg = error ? Theme.DANGER_FG : Theme.SUCCESS_FG;
        Color border = error ? new Color(0xFC, 0xA5, 0xA5) : new Color(0x86, 0xEF, 0xAC);

        target.setBackground(bg);
        target.setForeground(fg);
        target.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        target.setText("<html>" + escapeHtml(safeText(message)) + "</html>");
        target.setVisible(true);
    }

    static void showBannerInSlot(JPanel slot, JLabel target, String message, boolean error) {
        showBanner(target, message, error);
        if (slot != null) {
            slot.setVisible(true);
        }
    }

    static void hideBanner(JLabel target) {
        if (target == null) {
            return;
        }
        target.setText("");
        target.setVisible(false);
    }

    static void hideBannerInSlot(JPanel slot, JLabel target) {
        hideBanner(target);
        if (slot != null) {
            slot.setVisible(false);
        }
    }

    static JTextArea createReadOnlyTextArea(int rows, Font font, Color bg) {
        JTextArea area = new JTextArea(rows, 22);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(font != null ? font : Theme.small());
        area.setForeground(Theme.INK);
        area.setBackground(bg != null ? bg : Theme.SURFACE_ALT);
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return area;
    }

    static JTextArea createEditableTextArea(int rows) {
        JTextArea area = new JTextArea(rows, 28);
        area.setEditable(true);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.body());
        area.setForeground(Theme.INK);
        area.setBackground(Theme.AUTH_INPUT_BG);
        area.setCaretColor(Theme.INK);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return area;
    }

    static void addAttrRow(JPanel grid, int row, String labelText, JComponent valueComponent) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(3, 0, 3, 12);

        JLabel keyLabel = new JLabel(labelText);
        keyLabel.setFont(Theme.smallBold());
        keyLabel.setForeground(Theme.MUTED);
        grid.add(keyLabel, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1.0;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.anchor = GridBagConstraints.WEST;
        right.insets = new Insets(3, 0, 3, 0);
        grid.add(valueComponent, right);
    }

    static void addFieldGroup(
            JPanel container,
            GridBagConstraints gbc,
            int rowIndex,
            String labelText,
            JComponent field) {
        gbc.gridy = rowIndex;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(0, 0, 10, 0);

        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(Theme.smallBold());
        lbl.setForeground(Theme.INK);
        group.add(lbl, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        container.add(group, gbc);
    }

    static void registerEscapeClose(JDialog dialog) {
        if (dialog == null) {
            return;
        }
        dialog.getRootPane().registerKeyboardAction(
            e -> dialog.dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    static void wireTextChange(JTextField field, Runnable onChange) {
        if (field == null || onChange == null) {
            return;
        }
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onChange.run();
            }
        });
        field.addActionListener(e -> onChange.run());
    }

    static void attachCardInteraction(
            JPanel card,
            Runnable onSelect,
            Consumer<Boolean> onHoverChanged) {
        MouseAdapter cardAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && onSelect != null) {
                    onSelect.run();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (onHoverChanged != null) {
                    onHoverChanged.accept(Boolean.TRUE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (onHoverChanged != null) {
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                    if (!card.contains(p)) {
                        onHoverChanged.accept(Boolean.FALSE);
                    }
                }
            }
        };
        MouseAdapter buttonHoverAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (onHoverChanged != null) {
                    onHoverChanged.accept(Boolean.TRUE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (onHoverChanged != null) {
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                    if (!card.contains(p)) {
                        onHoverChanged.accept(Boolean.FALSE);
                    }
                }
            }
        };
        attachCardListeners(card, cardAdapter, buttonHoverAdapter);
    }

    private static void attachCardListeners(
            Component comp,
            MouseAdapter cardAdapter,
            MouseAdapter buttonHoverAdapter) {
        if (comp instanceof AbstractButton) {
            comp.addMouseListener(buttonHoverAdapter);
            return;
        }
        comp.addMouseListener(cardAdapter);
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachCardListeners(child, cardAdapter, buttonHoverAdapter);
            }
        }
    }

    static void paintInteractiveCard(
            Graphics g,
            int w,
            int h,
            int radius,
            Color fill,
            Color hoverFill,
            Color idleBorder,
            Color hoverBorder,
            boolean hovered,
            boolean selected,
            float selectedStrokeWidth) {
        if (w <= 3 || h <= 3) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double arc = Math.min(radius * 2.0, Math.min(w, h));
            double cardW = Math.max(1.0, w - 3.0);
            double cardH = Math.max(1.0, h - 3.0);

            g2.setColor(Theme.tint(Theme.NAVY, selected ? 26 : (hovered ? 20 : 12)));
            g2.fill(new RoundRectangle2D.Double(1.5, 2.5, cardW, cardH, arc, arc));

            Color activeFill = hovered && !selected && hoverFill != null ? hoverFill : fill;
            g2.setColor(activeFill != null ? activeFill : Theme.SURFACE);
            g2.fill(new RoundRectangle2D.Double(0.5, 0.5, cardW, cardH, arc, arc));

            if (selected) {
                float stroke = Math.max(1.5f, selectedStrokeWidth);
                double inset = stroke * 0.5 + 0.2;
                g2.setStroke(new BasicStroke(stroke));
                g2.setColor(Theme.BLUE);
                g2.draw(new RoundRectangle2D.Double(
                    inset,
                    inset,
                    Math.max(1.0, w - inset * 2.0 - 1.0),
                    Math.max(1.0, h - inset * 2.0 - 1.0),
                    arc,
                    arc
                ));
            } else {
                g2.setStroke(new BasicStroke(1.0f));
                g2.setColor(hovered && hoverBorder != null ? hoverBorder : idleBorder);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, cardW, cardH, arc, arc));
            }
        } finally {
            g2.dispose();
        }
    }

    static Color blendLighter(Color base, float whiteRatio) {
        Color safe = base != null ? base : Theme.SURFACE;
        int r = Math.min(255, Math.round(safe.getRed() + (255 - safe.getRed()) * whiteRatio));
        int g = Math.min(255, Math.round(safe.getGreen() + (255 - safe.getGreen()) * whiteRatio));
        int b = Math.min(255, Math.round(safe.getBlue() + (255 - safe.getBlue()) * whiteRatio));
        return new Color(r, g, b);
    }

    static VectorIconBadge iconBadge(
            VectorIcons.Kind kind,
            int boxSize,
            int iconSize,
            int radius,
            Color fill,
            Color iconColor,
            Color borderColor) {
        return new VectorIconBadge(kind, boxSize, iconSize, radius, fill, iconColor, borderColor);
    }

    static boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    static String capitalizeWords(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String clean = value.trim();
        return clean.substring(0, 1).toUpperCase(Locale.ROOT) + clean.substring(1);
    }

    static String safeText(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    static String nonNull(String value) {
        return value == null ? "" : value;
    }

    static String safeErrorMessage(RuntimeException ex) {
        if (ex == null) {
            return "Terjadi kesalahan tak terduga.";
        }
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? ex.getClass().getSimpleName() : msg;
    }

    static String escapeHtml(String raw) {
        return nonNull(raw).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Multi-row wrapping {@link FlowLayout} that reports accurate wrapped preferred height
     * for its container width without stretching horizontally.
     */
    private static final class WrapFlowLayout extends FlowLayout {
        private static final long serialVersionUID = 1L;
        private int lastLaidOutWidth = -1;
        private int lastLaidOutHeight = -1;

        WrapFlowLayout(int align, int hgap, int vgap) {
            super(align, Math.max(0, hgap), Math.max(0, vgap));
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return computeLayoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension wrapped = computeLayoutSize(target, false);
            int minChildW = computeMaxChildMinWidth(target);
            Insets insets = target.getInsets();
            int minW = minChildW + insets.left + insets.right + getHgap() * 2;
            return new Dimension(Math.max(1, minW), wrapped.height);
        }

        @Override
        public void layoutContainer(Container target) {
            super.layoutContainer(target);
            if (target.isDisplayable() && target.getWidth() > 0) {
                int desiredH = preferredLayoutSize(target).height;
                if (lastLaidOutWidth != target.getWidth() || lastLaidOutHeight != desiredH) {
                    lastLaidOutWidth = target.getWidth();
                    lastLaidOutHeight = desiredH;
                    if (Math.abs(desiredH - target.getHeight()) > 1 && target.getParent() != null) {
                        SwingUtilities.invokeLater(target::revalidate);
                    }
                }
            }
        }

        private Dimension computeLayoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                Insets insets = target.getInsets();
                int hgap = Math.max(0, getHgap());
                int vgap = Math.max(0, getVgap());
                int availWidth = resolveTargetWidth(target);
                int maxWidth = availWidth > 0
                    ? Math.max(1, availWidth - (insets.left + insets.right + hgap * 2))
                    : Integer.MAX_VALUE;

                int maxRowWidth = 0;
                int totalHeight = insets.top + insets.bottom + vgap * 2;
                int rowWidth = 0;
                int rowHeight = 0;
                boolean hasVisible = false;

                int count = target.getComponentCount();
                for (int i = 0; i < count; i++) {
                    Component comp = target.getComponent(i);
                    if (!comp.isVisible()) {
                        continue;
                    }
                    hasVisible = true;
                    Dimension d = preferred ? comp.getPreferredSize() : comp.getMinimumSize();
                    if (rowWidth == 0 || rowWidth + hgap + d.width <= maxWidth) {
                        if (rowWidth > 0) {
                            rowWidth += hgap;
                        }
                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    } else {
                        maxRowWidth = Math.max(maxRowWidth, rowWidth);
                        totalHeight += rowHeight + vgap;
                        rowWidth = d.width;
                        rowHeight = d.height;
                    }
                }

                maxRowWidth = Math.max(maxRowWidth, rowWidth);
                if (hasVisible) {
                    totalHeight += rowHeight;
                } else {
                    totalHeight = insets.top + insets.bottom;
                }
                int prefW = Math.max(1, maxRowWidth + insets.left + insets.right + hgap * 2);
                return new Dimension(prefW, Math.max(1, totalHeight));
            }
        }

        private static int resolveTargetWidth(Container target) {
            int width = target.getWidth();
            if (width > 0) {
                return width;
            }
            Container parent = target.getParent();
            if (parent != null && parent.getWidth() > 0) {
                Insets pin = parent.getInsets();
                return Math.max(1, parent.getWidth() - pin.left - pin.right);
            }
            return 0;
        }

        private static int computeMaxChildMinWidth(Container target) {
            synchronized (target.getTreeLock()) {
                int maxW = 0;
                int count = target.getComponentCount();
                for (int i = 0; i < count; i++) {
                    Component comp = target.getComponent(i);
                    if (comp.isVisible()) {
                        maxW = Math.max(maxW, comp.getMinimumSize().width);
                    }
                }
                return maxW;
            }
        }
    }

    static final class CompactSectionPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        CompactSectionPanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension pref = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, pref.height);
        }
    }

    static final class ReadOnlyTableModel extends DefaultTableModel {
        private static final long serialVersionUID = 1L;
        private final Class<?>[] columnTypes;

        ReadOnlyTableModel(Object[] columnNames) {
            this(columnNames, null);
        }

        ReadOnlyTableModel(Object[] columnNames, Class<?>[] columnTypes) {
            super(columnNames, 0);
            this.columnTypes = columnTypes;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnTypes != null
                    && columnIndex >= 0
                    && columnIndex < columnTypes.length
                    && columnTypes[columnIndex] != null) {
                return columnTypes[columnIndex];
            }
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    static final class CurrencyCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

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
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (isSelected) {
                setBackground(table != null ? table.getSelectionBackground() : Theme.SELECTION_BG);
                setForeground(table != null ? table.getSelectionForeground() : Theme.NAVY);
            } else {
                setBackground(row % 2 == 0 ? Theme.SURFACE : Theme.SURFACE_ALT);
                setForeground(Theme.INK);
            }
            if (value instanceof BigDecimal amount) {
                setText(FormatUtil.rupiah(amount));
            } else if (value instanceof Number num) {
                setText(FormatUtil.rupiah(num.longValue()));
            } else {
                setText(FormatUtil.rupiah(BigDecimal.ZERO));
            }
            return this;
        }
    }

    static final class DynamicStatusPill extends JLabel {
        private static final long serialVersionUID = 1L;
        private Theme.StatusColors colors = Theme.statusColors("");

        DynamicStatusPill() {
            super("-", SwingConstants.CENTER);
            setFont(Theme.badge());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            setStatus("");
        }

        void setStatus(String rawStatus) {
            this.colors = Theme.statusColors(rawStatus);
            setText(FormatUtil.statusLabel(rawStatus));
            setForeground(colors.fg());
            revalidate();
            repaint();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(colors.bg());
                int h = getHeight();
                int pillH = Math.min(h, getPreferredSize().height);
                int pillY = (h - pillH) / 2;
                g2.fill(new RoundRectangle2D.Double(0, pillY, getWidth(), pillH, pillH, pillH));
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    static final class WrappingHtmlLabel extends JLabel {
        private static final long serialVersionUID = 1L;
        private final int fallbackWrapWidth;

        WrappingHtmlLabel(String plainText, Font font, Color color, int fallbackWrapWidth) {
            super("<html>" + escapeHtml(plainText) + "</html>");
            this.fallbackWrapWidth = Math.max(80, fallbackWrapWidth);
            setFont(font != null ? font : Theme.body());
            setForeground(color != null ? color : Theme.INK);
            setVerticalAlignment(SwingConstants.TOP);
        }

        void setPlainText(String plainText) {
            setText("<html>" + escapeHtml(plainText) + "</html>");
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension base = super.getPreferredSize();
            Object prop = getClientProperty(BasicHTML.propertyKey);
            if (prop instanceof View view) {
                Insets in = getInsets();
                int targetW = getWidth() > 0 ? getWidth() : fallbackWrapWidth;
                int availW = Math.max(40, targetW - in.left - in.right);
                view.setSize(availW, Float.MAX_VALUE);
                int prefH = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + in.top + in.bottom;
                int prefW = Math.min(base.width, targetW);
                return new Dimension(Math.max(1, prefW), Math.max(1, prefH));
            }
            return base;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension pref = getPreferredSize();
            return new Dimension(Math.min(pref.width, fallbackWrapWidth), pref.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Object prop = getClientProperty(BasicHTML.propertyKey);
            if (prop instanceof View view) {
                if (isOpaque()) {
                    g.setColor(getBackground());
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                Insets in = getInsets();
                int availW = Math.max(1, getWidth() - in.left - in.right);
                int availH = Math.max(1, getHeight() - in.top - in.bottom);
                Rectangle paintBounds = new Rectangle(in.left, in.top, availW, availH);
                view.setSize(availW, availH);
                view.paint(g, paintBounds);
                return;
            }
            super.paintComponent(g);
        }
    }

    static final class VectorIconBadge extends JLabel {
        private static final long serialVersionUID = 1L;
        private final int iconSize;
        private final int radius;
        private final Color fillColor;
        private final Color iconColor;
        private final Color borderColor;

        VectorIconBadge(
                VectorIcons.Kind kind,
                int boxSize,
                int iconSize,
                int radius,
                Color fillColor,
                Color iconColor,
                Color borderColor) {
            super("", SwingConstants.CENTER);
            this.iconSize = Math.max(12, iconSize);
            this.radius = Math.max(4, radius);
            this.fillColor = fillColor != null ? fillColor : Theme.SURFACE;
            this.iconColor = iconColor != null ? iconColor : Theme.BLUE;
            this.borderColor = borderColor;
            Dimension fixed = new Dimension(boxSize, boxSize);
            setPreferredSize(fixed);
            setMinimumSize(fixed);
            setMaximumSize(fixed);
            setOpaque(false);
            setKind(kind);
        }

        void setKind(VectorIcons.Kind kind) {
            Icon icon = VectorIcons.of(kind != null ? kind : VectorIcons.Kind.HEALTH, iconSize, iconColor);
            setIcon(icon);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                double arc = Math.min(radius * 2.0, Math.min(w, h));
                g2.setColor(fillColor);
                g2.fill(new RoundRectangle2D.Double(0.5, 0.5, Math.max(1.0, w - 1.0), Math.max(1.0, h - 1.0), arc, arc));
                if (borderColor != null) {
                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, Math.max(1.0, w - 1.0), Math.max(1.0, h - 1.0), arc, arc));
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
