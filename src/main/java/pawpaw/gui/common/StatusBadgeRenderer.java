package pawpaw.gui.common;

import pawpaw.util.FormatUtil;
import pawpaw.util.Theme;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

/**
 * Table cell renderer displaying database status values as high-contrast semantic pill badges
 * with safe clipping and ellipsizing for narrow table columns.
 */
public final class StatusBadgeRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private Theme.StatusColors pillColors = Theme.statusColors("");
    private String badgeText = "-";
    private final int horizontalAlignment;

    public StatusBadgeRenderer() {
        this(SwingConstants.CENTER);
    }

    public StatusBadgeRenderer(int horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
        setOpaque(true);
        setFont(Theme.badge());
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

        String raw = value == null ? "" : value.toString();
        this.badgeText = FormatUtil.statusLabel(raw);
        this.pillColors = Theme.statusColors(raw);

        setText(badgeText);
        setToolTipText(badgeText);
        setFont(Theme.badge());
        setBorder(null);

        if (isSelected) {
            setBackground(table != null ? table.getSelectionBackground() : Theme.SELECTION_BG);
        } else {
            Color alt = (table != null && Theme.Admin.LINE.equals(table.getGridColor()))
                ? Theme.Admin.SURFACE_ALT
                : Theme.SURFACE_ALT;
            setBackground(row % 2 == 0 ? Theme.SURFACE : alt);
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        UiKit.paintPillBadge(
            g,
            getWidth(),
            getHeight(),
            getBackground(),
            pillColors.bg(),
            pillColors.fg(),
            Theme.badge(),
            badgeText,
            horizontalAlignment,
            10,
            28
        );
    }
}
