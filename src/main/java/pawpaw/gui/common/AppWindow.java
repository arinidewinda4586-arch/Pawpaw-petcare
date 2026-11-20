package pawpaw.gui.common;

import pawpaw.config.AppConfig;
import pawpaw.config.DatabaseConfig;
import pawpaw.gui.admin.AdminAdoptionsPanel;
import pawpaw.gui.admin.AdminAnimalsPanel;
import pawpaw.gui.admin.AdminBookingsPanel;
import pawpaw.gui.admin.AdminDashboardPanel;
import pawpaw.gui.admin.AdminOrdersPanel;
import pawpaw.gui.admin.AdminProductsPanel;
import pawpaw.gui.admin.AdminServicesPanel;
import pawpaw.gui.auth.AuthPanel;
import pawpaw.gui.user.AdoptionWorkspacePanel;
import pawpaw.gui.user.HealthWorkspacePanel;
import pawpaw.gui.user.ProfileWorkspacePanel;
import pawpaw.gui.user.ShopWorkspacePanel;
import pawpaw.gui.user.UserDashboardPanel;
import pawpaw.model.SessionState;
import pawpaw.model.User;
import pawpaw.service.AdminDashboardService;
import pawpaw.service.AdoptionService;
import pawpaw.service.AuthService;
import pawpaw.service.HealthBookingService;
import pawpaw.service.ProfileService;
import pawpaw.service.ShopService;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Central application JFrame owning service singletons, active session state,
 * authentication routing, and the Admin/User desktop workspace shell.
 */
public final class AppWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String CARD_AUTH = "AUTH";
    private static final String CARD_SHELL = "SHELL";

    private final transient SessionState session = new SessionState();
    private final transient AuthService authService = new AuthService();
    private final transient ProfileService profileService = new ProfileService();
    private final transient AdoptionService adoptionService = new AdoptionService();
    private final transient ShopService shopService = new ShopService();
    private final transient HealthBookingService healthBookingService = new HealthBookingService();
    private final transient AdminDashboardService adminDashboardService = new AdminDashboardService();

    private final CardLayout rootCards = new CardLayout();
    private final JPanel rootContainer = new JPanel(rootCards);
    private final AuthPanel authPanel;
    private final DesktopShell shell;

    public AppWindow() {
        super(AppConfig.APP_NAME + " — Desktop");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1040, 680));
        setLocationRelativeTo(null);
        setIconImage(buildWindowIcon());

        authPanel = new AuthPanel(authService, this::onLoginSuccess);
        shell = new DesktopShell(
            this::toggleWorkspaceMode,
            this::showAuth,
            () -> navigate("user.shop"),
            this::refreshDbStatus
        );

        registerUserPages();
        registerAdminPages();

        rootContainer.setBackground(Theme.BG);
        rootContainer.add(authPanel, CARD_AUTH);
        rootContainer.add(shell, CARD_SHELL);
        setContentPane(rootContainer);

        rootCards.show(rootContainer, CARD_AUTH);
    }

    private void registerUserPages() {
        registerPage(new UserDashboardPanel(this));
        registerPage(new AdoptionWorkspacePanel(this));
        registerPage(new ShopWorkspacePanel(this));
        registerPage(new HealthWorkspacePanel(this));
        registerPage(new ProfileWorkspacePanel(this));
    }

    private void registerAdminPages() {
        registerPage(new AdminDashboardPanel(this));
        registerPage(new AdminProductsPanel(this));
        registerPage(new AdminOrdersPanel(this));
        registerPage(new AdminAnimalsPanel(this));
        registerPage(new AdminAdoptionsPanel(this));
        registerPage(new AdminServicesPanel(this));
        registerPage(new AdminBookingsPanel(this));
    }

    public void registerPage(PagePanel page) {
        shell.registerPage(page);
    }

    public void showAuth() {
        session.logout();
        rootCards.show(rootContainer, CARD_AUTH);
        authPanel.resetForm();
        revalidate();
        repaint();
    }

    public void onLoginSuccess(User user) {
        if (user == null) {
            showAuth();
            return;
        }
        session.login(user);
        if (user.isAdmin()) {
            openAdminWorkspace();
        } else {
            openUserWorkspace();
        }
    }

    public void openAdminWorkspace() {
        if (!session.isLoggedIn()) {
            showAuth();
            return;
        }
        if (!session.isAdmin()) {
            openUserWorkspace();
            return;
        }
        session.setAdminPreviewAsUser(false);
        shell.applySession(session.getUser(), true, false);
        rootCards.show(rootContainer, CARD_SHELL);
        clearDefaultButton();
        navigate("admin.dashboard");
        refreshDbStatus();
    }

    public void openUserWorkspace() {
        if (!session.isLoggedIn()) {
            showAuth();
            return;
        }
        boolean canReturnToAdmin = session.isAdmin();
        session.setAdminPreviewAsUser(canReturnToAdmin);
        shell.applySession(session.getUser(), false, canReturnToAdmin);
        refreshCartBadge();
        rootCards.show(rootContainer, CARD_SHELL);
        clearDefaultButton();
        navigate("user.dashboard");
        refreshDbStatus();
    }

    private void clearDefaultButton() {
        JRootPane rootPane = getRootPane();
        if (rootPane != null) {
            rootPane.setDefaultButton(null);
        }
    }

    private void toggleWorkspaceMode() {
        if (!session.isLoggedIn()) {
            showAuth();
            return;
        }
        if (session.isAdmin()) {
            if (session.isAdminPreviewAsUser()) {
                openAdminWorkspace();
            } else {
                openUserWorkspace();
            }
        }
    }

    public void navigate(String pageKey) {
        shell.navigateTo(pageKey);
    }

    public void refreshCartBadge() {
        shell.setCartCount(session.getCartCount());
    }

    public void refreshSessionHeader() {
        if (!session.isLoggedIn()) {
            return;
        }
        boolean adminWorkspace = session.isAdmin() && !session.isAdminPreviewAsUser();
        boolean canReturnToAdmin = !adminWorkspace && session.isAdmin();
        shell.applySession(session.getUser(), adminWorkspace, canReturnToAdmin);
    }

    public void updateStatus(String summary) {
        shell.updateStatusSummary(summary);
    }

    public void refreshDbStatus() {
        boolean connected = DatabaseConfig.testConnection();
        String err = DatabaseConfig.getLastError();
        shell.updateDbStatus(connected, connected ? "pet_care" : err);
        authPanel.setDbStatus(connected, err);
    }

    public DesktopShell getShell() {
        return shell;
    }

    public SessionState getSession() {
        return session;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public ProfileService getProfileService() {
        return profileService;
    }

    public AdoptionService getAdoptionService() {
        return adoptionService;
    }

    public ShopService getShopService() {
        return shopService;
    }

    public HealthBookingService getHealthBookingService() {
        return healthBookingService;
    }

    public AdminDashboardService getAdminDashboardService() {
        return adminDashboardService;
    }

    private static BufferedImage buildWindowIcon() {
        BufferedImage iconImg = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = iconImg.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.NAVY);
            g2.fill(new RoundRectangle2D.Double(0, 0, 32, 32, 8, 8));
            Icon paw = VectorIcons.of(VectorIcons.Kind.PAW, 22, Theme.GOLD);
            paw.paintIcon(null, g2, 5, 5);
        } finally {
            g2.dispose();
        }
        return iconImg;
    }
}
