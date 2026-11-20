package pawpaw.gui.common;

import pawpaw.util.Theme;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Graphics;

/**
 * Table cell renderer displaying integer/string identifiers as soft blue {@code #ID} pills
 * while preserving underlying model types for numeric sorting and clipping safely in narrow columns.
 */
public final class IdPillRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private String pillText = "-";

    public IdPillRenderer() {
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

        String raw = value == null ? "" : value.toString().trim();
        if (raw.isEmpty()) {
            this.pillText = "-";
        } else if (raw.startsWith("#")) {
            this.pillText = raw;
        } else {
            this.pillText = "#" + raw;
        }

        setText(pillText);
        setToolTipText(pillText);
        setFont(Theme.badge());
        setBorder(null);

        if (isSelected) {
            setBackground(table != null ? table.getSelectionBackground() : Theme.Admin.BLUE_SOFT);
        } else {
            setBackground(row % 2 == 0 ? Theme.Admin.SURFACE : Theme.Admin.SURFACE_ALT);
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
            Theme.Admin.ID_PILL_BG,
            Theme.Admin.ID_PILL_FG,
            Theme.badge(),
            pillText,
            SwingConstants.CENTER,
            8,
            28
        );
    }
}
