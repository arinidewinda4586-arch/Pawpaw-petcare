package pawpaw;

import pawpaw.gui.common.AppWindow;
import pawpaw.util.Theme;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;

/**
 * Desktop application entry point initializing antialiased font hints, Swing defaults, and {@link AppWindow}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            applyUiDefaults();
            AppWindow window = new AppWindow();
            window.setVisible(true);
        });
    }

    public static void applyUiDefaults() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        UIManager.put("Panel.background", new ColorUIResource(Theme.BG));
        UIManager.put("OptionPane.background", Theme.SURFACE);
        UIManager.put("OptionPane.messageForeground", Theme.INK);
        UIManager.put("OptionPane.messageFont", Theme.body());
        UIManager.put("OptionPane.buttonFont", Theme.bodyBold());
        UIManager.put("OptionPane.yesButtonText", "Ya");
        UIManager.put("OptionPane.noButtonText", "Batal");
        UIManager.put("Label.font", Theme.body());
        UIManager.put("Button.font", Theme.bodyBold());
        UIManager.put("TextField.font", Theme.body());
        UIManager.put("PasswordField.font", Theme.body());
        UIManager.put("ComboBox.font", Theme.body());
        UIManager.put("Table.font", Theme.body());
        UIManager.put("TableHeader.font", Theme.smallBold());
        UIManager.put("ToolTip.background", Theme.NAVY);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font", Theme.small());
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(5, 8, 5, 8));
        UIManager.put("TabbedPane.font", Theme.bodyBold());
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(Theme.BORDER));
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
    }
}
