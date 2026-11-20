package pawpaw.gui.admin;

import pawpaw.gui.common.AppWindow;
import pawpaw.gui.common.UiKit;
import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Package-private Pure-Swing layout, table, toolbar, and modal dialog helpers shared
 * across Admin dashboard, master data CRUD panels, and transaction workspaces.
 */
final class AdminWidgets {

    private static final int MAX_DIALOG_HEIGHT = 600;

    private AdminWidgets() {
    }

    static JScrollPane verticalScrollPane(JComponent content) {
        return UiKit.verticalScrollPane(content, Theme.Admin.SURFACE);
    }

    static JScrollPane verticalExpandingScrollPane(JComponent content, Color viewportBg) {
        return UiKit.verticalExpandingScrollPane(content, viewportBg != null ? viewportBg : Theme.Admin.BG);
    }

    static JScrollPane adminScrollPane(Component view) {
        JScrollPane scroll = UiKit.scrollPane(view);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.Admin.LINE, 1));
        scroll.getViewport().setBackground(Theme.Admin.SURFACE);
        return scroll;
    }

    static JPanel adminTableCard(JTable table) {
        JPanel card = UiKit.adminCardPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 10, 10));
        JScrollPane scroll = adminScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    static JLabel adminFormLabel(String text) {
        return UiKit.label(text, Theme.smallBold(), Theme.Admin.TEXT);
    }

    static <T> JComboBox<T> adminComboBox(T[] items) {
        JComboBox<T> combo = UiKit.comboBox(items);
        combo.setForeground(Theme.Admin.TEXT);
        combo.setBackground(Theme.Admin.SURFACE);
        combo.setBorder(BorderFactory.createLineBorder(Theme.Admin.LINE, 1));
        return combo;
    }

    static JPanel buildMasterToolbar(
            JComponent row1Left,
            JLabel countLabel,
            JButton addButton,
            JButton editButton,
            JButton deleteButton,
            JButton refreshButton) {
        JPanel toolbarCard = UiKit.adminCardPanel(new BorderLayout(0, Theme.SPACE_12));

        JPanel row1 = new JPanel(new BorderLayout(Theme.SPACE_12, 0));
        row1.setOpaque(false);
        row1.add(row1Left, BorderLayout.WEST);
        row1.add(countLabel, BorderLayout.EAST);

        JPanel row2Left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0));
        row2Left.setOpaque(false);
        row2Left.add(addButton);
        row2Left.add(editButton);
        row2Left.add(deleteButton);

        JPanel row2 = new JPanel(new BorderLayout(Theme.SPACE_12, 0));
        row2.setOpaque(false);
        row2.add(row2Left, BorderLayout.WEST);
        row2.add(refreshButton, BorderLayout.EAST);

        toolbarCard.add(row1, BorderLayout.NORTH);
        toolbarCard.add(row2, BorderLayout.SOUTH);
        return toolbarCard;
    }

    static JPanel buildTransactionToolbar(
            JTextField searchField,
            JComboBox<String> statusCombo,
            JLabel countLabel,
            JButton refreshButton) {
        JPanel toolbarCard = UiKit.adminCardPanel(new BorderLayout(Theme.SPACE_12, 0));

        JPanel leftRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0));
        leftRow.setOpaque(false);
        leftRow.add(searchField);
        leftRow.add(adminFormLabel("Status:"));
        leftRow.add(statusCombo);

        JPanel rightRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_12, 0));
        rightRow.setOpaque(false);
        rightRow.add(countLabel);
        rightRow.add(refreshButton);

        toolbarCard.add(leftRow, BorderLayout.WEST);
        toolbarCard.add(rightRow, BorderLayout.EAST);
        return toolbarCard;
    }

    static JPanel buildNorthStack(JLabel feedbackBanner, JPanel toolbar) {
        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setOpaque(false);
        if (feedbackBanner != null) {
            feedbackBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
            northStack.add(feedbackBanner);
        }
        if (toolbar != null) {
            toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
            northStack.add(toolbar);
        }
        return northStack;
    }

    static JPanel createInspectorSubCard(LayoutManager layout) {
        JPanel card = UiKit.pastelCardPanel(
            layout,
            Theme.Admin.SURFACE_ALT,
            Theme.Admin.LINE,
            Theme.Admin.CARD_RADIUS
        );
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        return card;
    }

    static JPanel createNoteRow(JLabel noteLabel) {
        JPanel row = new CompactSectionPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.SPACE_8, 0));
        row.add(noteLabel);
        row.setVisible(false);
        return row;
    }

    static void setNoteRowVisible(JPanel noteRow, JLabel noteLabel, boolean visible, String text) {
        if (noteLabel != null) {
            if (text != null) {
                noteLabel.setText(text);
            }
            noteLabel.setVisible(visible);
        }
        if (noteRow != null) {
            noteRow.setVisible(visible);
            Container parent = noteRow.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        }
    }

    static void wireTableSelection(JTable table, Runnable onSelectionChanged) {
        if (table == null || onSelectionChanged == null) {
            return;
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged.run();
            }
        });
        if (table.getRowSorter() != null) {
            table.getRowSorter().addRowSorterListener(e -> onSelectionChanged.run());
        }
    }

    static JSplitPane buildMasterDetailSplit(JTable masterTable, JComponent inspectorContent) {
        JScrollPane topScroll = adminScrollPane(masterTable);
        topScroll.setMinimumSize(new Dimension(320, 150));
        topScroll.setPreferredSize(new Dimension(480, 225));

        JPanel inspectorHost = UiKit.adminCardPanel(new BorderLayout());
        inspectorHost.setBorder(BorderFactory.createEmptyBorder(8, 8, 10, 10));
        inspectorHost.setMinimumSize(new Dimension(320, 195));
        inspectorHost.add(verticalScrollPane(inspectorContent), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topScroll, inspectorHost);
        split.setOpaque(false);
        split.setBackground(Theme.Admin.BG);
        split.setResizeWeight(0.45);
        split.setDividerSize(8);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        return split;
    }

    static void addAttrRow(JPanel grid, int row, String labelText, JComponent valueComponent) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(3, 0, 3, 12);

        JLabel keyLabel = new JLabel(labelText);
        keyLabel.setFont(Theme.smallBold());
        keyLabel.setForeground(Theme.Admin.TEXT_SOFT);
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

    private static final Map<String, String> STATUS_FILTER_TO_DB = Map.of(
        "Menunggu", "pending",
        "Dibayar", "paid",
        "Diproses", "processing",
        "Dikirim", "shipped",
        "Disetujui", "approved",
        "Selesai", "completed",
        "Ditolak", "rejected",
        "Dibatalkan", "cancelled"
    );

    static boolean matchesStatusFilter(String actualStatus, Object selectedFilter) {
        String filterLabel = selectedFilter == null ? "Semua" : selectedFilter.toString();
        if ("Semua".equalsIgnoreCase(filterLabel)) {
            return true;
        }
        String expectedDb = STATUS_FILTER_TO_DB.get(filterLabel);
        if (expectedDb == null) {
            return true;
        }
        String cleanActual = actualStatus == null ? "" : actualStatus.trim().toLowerCase(Locale.ROOT);
        return expectedDb.equals(cleanActual);
    }

    static <T> T resolveSelectedItem(JTable table, List<T> filteredItems) {
        if (table == null || filteredItems == null) {
            return null;
        }
        int viewRow = table.getSelectedRow();
        if (viewRow < 0 || viewRow >= table.getRowCount()) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= filteredItems.size()) {
            return null;
        }
        return filteredItems.get(modelRow);
    }

    static <T> void restoreTableSelection(
            JTable table,
            List<T> filteredItems,
            ToIntFunction<T> idExtractor,
            int preferredId,
            Runnable onSelected,
            Runnable onEmpty) {
        if (filteredItems == null || filteredItems.isEmpty()) {
            onEmpty.run();
            return;
        }
        int targetModelRow = 0;
        if (preferredId > 0) {
            for (int i = 0; i < filteredItems.size(); i++) {
                if (idExtractor.applyAsInt(filteredItems.get(i)) == preferredId) {
                    targetModelRow = i;
                    break;
                }
            }
        }
        int viewRow = table.convertRowIndexToView(targetModelRow);
        if (viewRow >= 0 && viewRow < table.getRowCount()) {
            table.setRowSelectionInterval(viewRow, viewRow);
            onSelected.run();
        } else {
            onEmpty.run();
        }
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

    static void attachDoubleClick(JTable table, Runnable action) {
        if (table == null || action == null) {
            return;
        }
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        action.run();
                    }
                }
            }
        });
    }

    static JLabel createFeedbackBanner() {
        JLabel label = new JLabel("");
        label.setFont(Theme.small());
        label.setForeground(Theme.DANGER_FG);
        label.setOpaque(true);
        label.setVisible(false);
        return label;
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
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        target.setText("<html>" + escapeHtml(safeText(message)) + "</html>");
        target.setVisible(true);
    }

    static void hideBanner(JLabel target) {
        if (target == null) {
            return;
        }
        target.setText("");
        target.setVisible(false);
    }

    static void showDialogError(JDialog dialog, JLabel errorLabel, String message) {
        showBanner(errorLabel, message, true);
        if (dialog != null) {
            dialog.revalidate();
            dialog.repaint();
            SwingUtilities.invokeLater(() -> errorLabel.scrollRectToVisible(
                new Rectangle(0, 0, Math.max(1, errorLabel.getWidth()), Math.max(1, errorLabel.getHeight()))
            ));
        }
    }

    static JTextArea createEditableTextArea(int rows) {
        JTextArea area = new JTextArea(rows, 28);
        area.setEditable(true);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.body());
        area.setForeground(Theme.Admin.TEXT);
        area.setBackground(Theme.Admin.SURFACE);
        area.setCaretColor(Theme.Admin.TEXT);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return area;
    }

    static JTextArea createReadOnlyTextArea(int rows) {
        JTextArea area = new JTextArea(rows, 28);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.small());
        area.setForeground(Theme.Admin.TEXT);
        area.setBackground(Theme.Admin.SURFACE_ALT);
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return area;
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
        group.add(adminFormLabel(labelText), BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        container.add(group, gbc);
    }

    static void addScrollAreaGroup(
            JPanel container,
            GridBagConstraints gbc,
            int rowIndex,
            String labelText,
            JTextArea area,
            int preferredHeight) {
        JScrollPane scroll = adminScrollPane(area);
        scroll.setPreferredSize(new Dimension(380, preferredHeight));
        addFieldGroup(container, gbc, rowIndex, labelText, scroll);
    }

    static void finalizeFormDialog(
            JDialog dialog,
            AppWindow app,
            JComponent formCard,
            JButton cancelButton,
            JButton saveButton,
            int prefWidth,
            int minHeight) {
        JScrollPane formScroll = verticalScrollPane(formCard);
        formScroll.getViewport().setBackground(Theme.Admin.SURFACE);

        JPanel scrollCard = UiKit.adminCardPanel(new BorderLayout());
        scrollCard.setBorder(BorderFactory.createEmptyBorder(8, 8, 10, 10));
        scrollCard.add(formScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 0));
        footer.setOpaque(false);
        footer.add(cancelButton);
        footer.add(saveButton);

        JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_12));
        root.setBackground(Theme.Admin.BG);
        root.setBorder(BorderFactory.createEmptyBorder(
            Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16, Theme.SPACE_16
        ));
        root.add(scrollCard, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(saveButton);
        registerEscapeClose(dialog);

        dialog.pack();
        int targetHeight = Math.min(MAX_DIALOG_HEIGHT, Math.max(minHeight, dialog.getPreferredSize().height));
        dialog.setMinimumSize(new Dimension(Math.max(440, prefWidth - 40), Math.min(minHeight, 440)));
        dialog.setSize(new Dimension(prefWidth, targetHeight));
        dialog.setLocationRelativeTo(app);
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

    static String formatPlainDecimal(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
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
                setBackground(table != null ? table.getSelectionBackground() : Theme.Admin.BLUE_SOFT);
                setForeground(table != null ? table.getSelectionForeground() : Theme.Admin.TEXT);
            } else {
                setBackground(row % 2 == 0 ? Theme.Admin.SURFACE : Theme.Admin.SURFACE_ALT);
                setForeground(Theme.Admin.TEXT);
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

    static void selectStatusOption(JComboBox<StatusOption> combo, String rawCode) {
        if (combo == null || combo.getItemCount() == 0) {
            return;
        }
        String clean = rawCode == null ? "" : rawCode.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < combo.getItemCount(); i++) {
            StatusOption option = combo.getItemAt(i);
            if (option != null && option.code().equalsIgnoreCase(clean)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(0);
    }

    record StatusOption(String code, String label) implements java.io.Serializable {
        @Override
        public String toString() {
            return label + " (" + code + ")";
        }
    }

    static final class StatusPill extends JLabel {
        private static final long serialVersionUID = 1L;
        private Theme.StatusColors colors = Theme.statusColors("");

        StatusPill() {
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
}
