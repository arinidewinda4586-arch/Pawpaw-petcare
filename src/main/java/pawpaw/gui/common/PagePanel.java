package pawpaw.gui.common;

import pawpaw.util.Theme;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.plaf.UIResource;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;

/**
 * Base class for navigable User and Admin workspace panels hosted inside {@link DesktopShell}.
 */
public abstract class PagePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final boolean constructed;
    private boolean explicitBackgroundSet;

    protected PagePanel() {
        super(new BorderLayout(Theme.SPACE_16, Theme.SPACE_16), true);
        this.constructed = true;
    }

    /**
     * Returns the default canvas background color for this workspace page.
     * Admin pages override this to return {@link Theme.Admin#BG}.
     */
    protected Color defaultBackground() {
        return Theme.BG;
    }

    @Override
    public void setBackground(Color bg) {
        if (constructed) {
            this.explicitBackgroundSet = (bg != null && !(bg instanceof UIResource));
        }
        super.setBackground(bg);
    }

    @Override
    public Color getBackground() {
        return explicitBackgroundSet ? super.getBackground() : defaultBackground();
    }

    @Override
    public Insets getInsets() {
        if (getBorder() == null) {
            return new Insets(Theme.SPACE_16, Theme.SPACE_24, Theme.SPACE_16, Theme.SPACE_24);
        }
        return super.getInsets();
    }

    @Override
    public Insets getInsets(Insets insets) {
        if (getBorder() == null) {
            if (insets == null) {
                return getInsets();
            }
            insets.set(Theme.SPACE_16, Theme.SPACE_24, Theme.SPACE_16, Theme.SPACE_24);
            return insets;
        }
        return super.getInsets(insets);
    }

    public abstract String getPageKey();

    public abstract String getPageTitle();

    public abstract String getPageSubtitle();

    public abstract void refreshData();

    public JComponent getToolbarAccessory() {
        return null;
    }

    public String getStatusSummary() {
        return "";
    }
}
