package pawpaw.gui.common;

import pawpaw.model.User;
import pawpaw.util.ImageStore;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared desktop application shell with collapsible dual-palette navigation rail
 * (Customer {@code app.css} and Admin {@code admin.css}), top workspace toolbar,
 * CardLayout center host, and bottom database/session status bar.
 */
public final class DesktopShell extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color CUSTOMER_ACTIVE_PILL_BG = new Color(255, 255, 255, 31);
    private static final Color CUSTOMER_HOVER_PILL_BG = new Color(255, 255, 255, 20);
    private static final Color CUSTOMER_NAV_IDLE_FG = new Color(0xE8, 0xF0, 0xFA);
    private static final Color ADMIN_DIVIDER = new Color(255, 255, 255, 31);

    private record NavItem(String pageKey, String label, VectorIcons.Kind icon) {
    }

    private static final List<NavItem> ADMIN_NAV_ITEMS = List.of(
        new NavItem("admin.dashboard", "Dashboard", VectorIcons.Kind.DASHBOARD),
        new NavItem("admin.products", "Produk", VectorIcons.Kind.SHOP),
        new NavItem("admin.orders", "Pesanan", VectorIcons.Kind.ORDERS),
        new NavItem("admin.animals", "Hewan Adopsi", VectorIcons.Kind.PAW),
        new NavItem("admin.adoptions", "Verifikasi Adopsi", VectorIcons.Kind.ADOPTION),
        new NavItem("admin.services", "Layanan Kesehatan", VectorIcons.Kind.SERVICES),
        new NavItem("admin.bookings", "Booking Kesehatan", VectorIcons.Kind.BOOKINGS)
    );

    private static final List<NavItem> USER_NAV_ITEMS = List.of(
        new NavItem("user.dashboard", "Beranda", VectorIcons.Kind.DASHBOARD),
        new NavItem("user.adoption", "Adopsi", VectorIcons.Kind.PAW),
        new NavItem("user.shop", "Toko", VectorIcons.Kind.SHOP),
        new NavItem("user.health", "Kesehatan", VectorIcons.Kind.HEALTH),
        new NavItem("user.profile", "Profil", VectorIcons.Kind.PROFILE)
    );

    private final transient Runnable onSwitchMode;
    private final transient Runnable onLogout;
    private final transient Runnable onCartClick;
    private final transient Runnable onRefresh;

    private final transient Map<String, PagePanel> pages = new LinkedHashMap<>();
    private final transient List<NavButton> navButtons = new ArrayList<>();
    private String activePageKey;
    private boolean sidebarCollapsed;
    private boolean adminPaletteActive;
    private boolean dbConnected = true;

    // Sidebar widgets
    private final JPanel sidebar;
    private final JPanel brandHeader;
    private final JPanel brandRow;
    private final FlowLayout brandRowLayout;
    private final BrandLogoButton logoButton;
    private final JPanel brandTextPanel;
    private final JLabel brandTitleLabel;
    private final JPanel roleRow;
    private final RolePill rolePill;
    private final JLabel brandSubLabel;
    private final JPanel navContainer;
    private final JPanel sidebarBottom;
    private final SidebarActionButton switchModeButton;
    private final SidebarActionButton logoutButton;
    private final RoundedNavyCard userCard;
    private final AvatarBadge avatarBadge;
    private final JPanel userTextPanel;
    private final JLabel sidebarUserNameLabel;
    private final JLabel sidebarUserEmailLabel;

    // Workspace & Top bar widgets
    private final JPanel workspace;
    private final JPanel topBar;
    private final JLabel pageTitleLabel;
    private final JLabel pageSubtitleLabel;
    private final JPanel accessorySlot;
    private final ToolbarPillButton cartButton;
    private final ToolbarPillButton refreshButton;

    // Center host
    private final CardLayout cardLayout;
    private final JPanel cardContainer;

    // Bottom status bar widgets
    private final JPanel statusBar;
    private final StatusDot dbDot;
    private final JLabel dbStatusLabel;
    private final JLabel statusSummaryLabel;
    private final JLabel statusUserLabel;

    public DesktopShell() {
        this(null, null, null, null);
    }

    public DesktopShell(
            Runnable onSwitchMode,
            Runnable onLogout,
            Runnable onCartClick,
            Runnable onRefresh) {
        super(new BorderLayout(0, 0));
        this.onSwitchMode = onSwitchMode;
        this.onLogout = onLogout;
        this.onCartClick = onCartClick;
        this.onRefresh = onRefresh;

        setBackground(Theme.BG);
        setOpaque(true);

        // --- Left Collapsible Dual-Palette Navigation Rail ---
        sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.NAVY);
        sidebar.setOpaque(true);

        brandHeader = new JPanel(new BorderLayout(0, Theme.SPACE_8));
        brandHeader.setOpaque(false);

        brandRowLayout = new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0);
        brandRow = new JPanel(brandRowLayout);
        brandRow.setOpaque(false);

        logoButton = new BrandLogoButton();
        logoButton.addActionListener(e -> toggleSidebar());

        brandTextPanel = new JPanel();
        brandTextPanel.setLayout(new BoxLayout(brandTextPanel, BoxLayout.Y_AXIS));
        brandTextPanel.setOpaque(false);
        brandTitleLabel = UiKit.label("PawPaw", Theme.heading(), Color.WHITE);
        brandTextPanel.add(brandTitleLabel);

        brandRow.add(logoButton);
        brandRow.add(brandTextPanel);

        roleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_8, 0));
        roleRow.setOpaque(false);
        rolePill = new RolePill();
        brandSubLabel = UiKit.label("Pet Care Desk", Theme.badge(), Theme.NAVY_MUTED);
        roleRow.add(rolePill);
        roleRow.add(brandSubLabel);

        brandHeader.add(brandRow, BorderLayout.NORTH);
        brandHeader.add(roleRow, BorderLayout.SOUTH);

        navContainer = new JPanel();
        navContainer.setLayout(new BoxLayout(navContainer, BoxLayout.Y_AXIS));
        navContainer.setOpaque(false);
        navContainer.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_12, 10, Theme.SPACE_12, 10));

        JScrollPane navScroll = new JScrollPane(navContainer);
        navScroll.setBorder(BorderFactory.createEmptyBorder());
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        navScroll.getVerticalScrollBar().setUnitIncrement(14);

        sidebarBottom = new JPanel();
        sidebarBottom.setLayout(new BoxLayout(sidebarBottom, BoxLayout.Y_AXIS));
        sidebarBottom.setOpaque(false);

        switchModeButton = new SidebarActionButton("Mode Pelanggan", VectorIcons.Kind.SWITCH_MODE, Theme.GOLD);
        switchModeButton.addActionListener(e -> {
            if (this.onSwitchMode != null) {
                this.onSwitchMode.run();
            }
        });
        switchModeButton.setVisible(false);

        logoutButton = new SidebarActionButton("Keluar", VectorIcons.Kind.LOGOUT, Theme.NAVY_MUTED);
        logoutButton.addActionListener(e -> {
            if (this.onLogout != null) {
                this.onLogout.run();
            }
        });

        userCard = new RoundedNavyCard();
        userCard.setLayout(new BorderLayout(10, 0));
        userCard.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        userCard.setAlignmentX(LEFT_ALIGNMENT);

        avatarBadge = new AvatarBadge();
        sidebarUserNameLabel = UiKit.label("Tamu", Theme.smallBold(), Color.WHITE);
        sidebarUserEmailLabel = UiKit.label("-", Theme.badge(), Theme.NAVY_MUTED);

        userTextPanel = new JPanel();
        userTextPanel.setLayout(new BoxLayout(userTextPanel, BoxLayout.Y_AXIS));
        userTextPanel.setOpaque(false);
        userTextPanel.add(sidebarUserNameLabel);
        userTextPanel.add(sidebarUserEmailLabel);

        userCard.add(avatarBadge, BorderLayout.WEST);
        userCard.add(userTextPanel, BorderLayout.CENTER);

        sidebarBottom.add(switchModeButton);
        sidebarBottom.add(Box.createVerticalStrut(6));
        sidebarBottom.add(logoutButton);
        sidebarBottom.add(Box.createVerticalStrut(10));
        sidebarBottom.add(userCard);

        sidebar.add(brandHeader, BorderLayout.NORTH);
        sidebar.add(navScroll, BorderLayout.CENTER);
        sidebar.add(sidebarBottom, BorderLayout.SOUTH);

        // --- Workspace Area (Header + Center CardLayout + Status Bar) ---
        workspace = new JPanel(new BorderLayout(0, 0));
        workspace.setBackground(Theme.BG);
        workspace.setOpaque(true);

        topBar = new HeaderBarPanel();
        topBar.setBackground(Theme.SURFACE);
        topBar.setOpaque(true);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        pageTitleLabel = UiKit.label("Workspace", Theme.title(), Theme.INK);
        pageSubtitleLabel = UiKit.label("Siap digunakan", Theme.small(), Theme.MUTED);
        titleBox.add(pageTitleLabel);
        titleBox.add(pageSubtitleLabel);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_8, 2));
        topActions.setOpaque(false);

        accessorySlot = new JPanel(new BorderLayout());
        accessorySlot.setOpaque(false);

        cartButton = new ToolbarPillButton(
            "Keranjang (0)",
            VectorIcons.Kind.CART,
            Theme.CYAN_BTN,
            Theme.CYAN_BTN_HOVER,
            Theme.INK,
            null,
            14
        );
        cartButton.setVisible(false);
        cartButton.addActionListener(e -> {
            if (this.onCartClick != null) {
                this.onCartClick.run();
            } else if (pages.containsKey("user.shop")) {
                navigateTo("user.shop");
            }
        });

        refreshButton = new ToolbarPillButton(
            "Refresh",
            VectorIcons.Kind.REFRESH,
            Theme.SURFACE,
            Theme.SURFACE_ALT,
            Theme.INK,
            Theme.BORDER,
            14
        );
        refreshButton.setToolTipText("Muat ulang halaman aktif (F5)");
        refreshButton.addActionListener(e -> triggerRefresh());

        topActions.add(accessorySlot);
        topActions.add(cartButton);
        topActions.add(refreshButton);

        topBar.add(titleBox, BorderLayout.WEST);
        topBar.add(topActions, BorderLayout.EAST);

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setBackground(Theme.BG);
        cardContainer.setOpaque(true);

        statusBar = new JPanel(new BorderLayout(Theme.SPACE_16, 0));
        statusBar.setBackground(Theme.STATUS_BAR_BG);
        statusBar.setOpaque(true);
        statusBar.setPreferredSize(new Dimension(0, Theme.STATUS_BAR_HEIGHT));

        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        dbPanel.setOpaque(false);
        dbDot = new StatusDot();
        dbStatusLabel = UiKit.label("MySQL Terhubung: pet_care", Theme.badge(), Theme.INK);
        dbPanel.add(dbDot);
        dbPanel.add(dbStatusLabel);

        statusSummaryLabel = UiKit.label("Siap.", Theme.small(), Theme.MUTED);
        statusSummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);

        statusUserLabel = UiKit.label("Belum masuk", Theme.badge(), Theme.MUTED);
        statusUserLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        statusBar.add(dbPanel, BorderLayout.WEST);
        statusBar.add(statusSummaryLabel, BorderLayout.CENTER);
        statusBar.add(statusUserLabel, BorderLayout.EAST);

        workspace.add(topBar, BorderLayout.NORTH);
        workspace.add(cardContainer, BorderLayout.CENTER);
        workspace.add(statusBar, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);
        add(workspace, BorderLayout.CENTER);

        applyPaletteAndSidebarState();
        registerKeyboardRefresh();
    }

    private void registerKeyboardRefresh() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
            "pawpaw.refreshActivePage"
        );
        getActionMap().put("pawpaw.refreshActivePage", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                triggerRefresh();
            }
        });
    }

    private void triggerRefresh() {
        refreshActivePage();
        if (onRefresh != null) {
            onRefresh.run();
        }
    }

    public void registerPage(PagePanel page) {
        if (page == null) {
            return;
        }
        String key = page.getPageKey();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("PagePanel key tidak boleh kosong.");
        }
        PagePanel existing = pages.put(key, page);
        if (existing != null) {
            cardContainer.remove(existing);
        }
        cardContainer.add(page, key);

        if (key.equals(activePageKey)) {
            applyHeaderForPage(page);
            cardLayout.show(cardContainer, key);
        }
        cardContainer.revalidate();
        cardContainer.repaint();
    }

    public void applySession(User user, boolean adminWorkspace, boolean canReturnToAdmin) {
        String name = (user != null && user.getName() != null && !user.getName().isBlank())
            ? user.getName()
            : "Pengguna";
        String email = (user != null && user.getEmail() != null && !user.getEmail().isBlank())
            ? user.getEmail()
            : "-";
        String role = (user != null && user.getRole() != null && !user.getRole().isBlank())
            ? user.getRole().toLowerCase(Locale.ROOT)
            : "user";
        String avatar = user != null ? user.getAvatar() : null;

        this.adminPaletteActive = adminWorkspace;

        sidebarUserNameLabel.setText(name);
        sidebarUserEmailLabel.setText(email);
        avatarBadge.update(name, avatar);
        userCard.setToolTipText(name + " (" + email + ")");
        statusUserLabel.setText(name + " [" + role + "]");

        rolePill.setAdminMode(adminWorkspace);
        cartButton.setVisible(!adminWorkspace);

        if (adminWorkspace) {
            switchModeButton.setFullText("Mode Pelanggan");
            switchModeButton.setVisible(true);
            rebuildNavigation(ADMIN_NAV_ITEMS);
        } else {
            if (canReturnToAdmin) {
                switchModeButton.setFullText("Kembali ke Panel Admin");
                switchModeButton.setVisible(true);
            } else {
                switchModeButton.setVisible(false);
            }
            rebuildNavigation(USER_NAV_ITEMS);
        }

        applyPaletteAndSidebarState();
    }

    public boolean isSidebarCollapsed() {
        return sidebarCollapsed;
    }

    public void toggleSidebar() {
        setSidebarCollapsed(!sidebarCollapsed);
    }

    public void setSidebarCollapsed(boolean collapsed) {
        if (this.sidebarCollapsed == collapsed) {
            return;
        }
        this.sidebarCollapsed = collapsed;
        applyPaletteAndSidebarState();
    }

    public boolean isAdminPaletteActive() {
        return adminPaletteActive;
    }

    public int getSidebarWidth() {
        if (sidebarCollapsed) {
            return Theme.Admin.SIDEBAR_COLLAPSED;
        }
        return adminPaletteActive ? Theme.Admin.SIDEBAR_WIDTH : Theme.SIDEBAR_WIDTH;
    }

    private void applyPaletteAndSidebarState() {
        int targetWidth = getSidebarWidth();
        Color sidebarBg = adminPaletteActive ? Theme.Admin.NAVY : Theme.NAVY;
        Color dividerColor = adminPaletteActive ? ADMIN_DIVIDER : Theme.NAVY_DIVIDER;
        Color mutedSidebarText = adminPaletteActive ? Theme.Admin.NAVY_TEXT : Theme.NAVY_MUTED;
        Color hoverBg = adminPaletteActive ? Theme.Admin.NAVY_HOVER : Theme.NAVY_HOVER;
        Color workspaceBg = adminPaletteActive ? Theme.Admin.BG : Theme.BG;
        Color topBarLine = adminPaletteActive ? Theme.Admin.LINE : Theme.BORDER_SUBTLE;
        Color statusBg = adminPaletteActive ? Theme.Admin.STATUS_BAR_BG : Theme.STATUS_BAR_BG;
        Color statusLine = adminPaletteActive ? Theme.Admin.LINE : Theme.BORDER;
        Color titleInk = adminPaletteActive ? Theme.Admin.TEXT : Theme.INK;
        Color softInk = adminPaletteActive ? Theme.Admin.TEXT_SOFT : Theme.MUTED;

        sidebar.setBackground(sidebarBg);
        sidebar.setPreferredSize(new Dimension(targetWidth, 0));
        sidebar.setMinimumSize(new Dimension(targetWidth, 0));
        sidebar.setMaximumSize(new Dimension(targetWidth, Integer.MAX_VALUE));

        int padX = sidebarCollapsed ? 10 : 14;
        brandHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, dividerColor),
            BorderFactory.createEmptyBorder(14, padX, 12, padX)
        ));
        brandRowLayout.setAlignment(sidebarCollapsed ? FlowLayout.CENTER : FlowLayout.LEFT);
        brandRowLayout.setHgap(sidebarCollapsed ? 0 : Theme.SPACE_8);
        brandTitleLabel.setText(adminPaletteActive ? "PawPaw Admin" : "PawPaw");
        brandSubLabel.setText(adminPaletteActive ? "Panel Admin" : "Pet Care Desk");
        brandSubLabel.setForeground(mutedSidebarText);
        brandTextPanel.setVisible(!sidebarCollapsed);
        roleRow.setVisible(!sidebarCollapsed);
        logoButton.setToolTipText(sidebarCollapsed ? "Perluas Sidebar" : "Ciutkan Sidebar");

        navContainer.setBorder(BorderFactory.createEmptyBorder(
            Theme.SPACE_12,
            sidebarCollapsed ? 8 : 10,
            Theme.SPACE_12,
            sidebarCollapsed ? 8 : 10
        ));
        for (NavButton btn : navButtons) {
            btn.applyState(adminPaletteActive, sidebarCollapsed, targetWidth);
        }

        sidebarBottom.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, dividerColor),
            BorderFactory.createEmptyBorder(Theme.SPACE_12, sidebarCollapsed ? 8 : 12, Theme.SPACE_12, sidebarCollapsed ? 8 : 12)
        ));
        switchModeButton.applyState(hoverBg, sidebarCollapsed);
        logoutButton.applyState(hoverBg, sidebarCollapsed);
        logoutButton.setForeground(mutedSidebarText);
        logoutButton.setIconColor(mutedSidebarText);

        userCard.setCardFill(hoverBg);
        userTextPanel.setVisible(!sidebarCollapsed);
        sidebarUserEmailLabel.setForeground(mutedSidebarText);
        userCard.remove(avatarBadge);
        userCard.remove(userTextPanel);
        if (sidebarCollapsed) {
            userCard.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
            userCard.add(avatarBadge, BorderLayout.CENTER);
        } else {
            userCard.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            userCard.add(avatarBadge, BorderLayout.WEST);
            userCard.add(userTextPanel, BorderLayout.CENTER);
        }

        setBackground(workspaceBg);
        workspace.setBackground(workspaceBg);
        cardContainer.setBackground(workspaceBg);

        topBar.setBackground(Theme.SURFACE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, topBarLine),
            BorderFactory.createEmptyBorder(4, Theme.SPACE_24, 4, Theme.SPACE_24)
        ));
        pageTitleLabel.setForeground(titleInk);
        pageSubtitleLabel.setForeground(softInk);

        if (adminPaletteActive) {
            refreshButton.applyPalette(
                Theme.Admin.BTN_LIGHT_BG,
                Theme.Admin.BTN_LIGHT_HOVER,
                Theme.Admin.BTN_LIGHT_FG,
                null,
                Theme.Admin.BUTTON_RADIUS
            );
        } else {
            refreshButton.applyPalette(
                Theme.SURFACE,
                Theme.SURFACE_ALT,
                Theme.INK,
                Theme.BORDER,
                14
            );
        }

        statusBar.setBackground(statusBg);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, statusLine),
            BorderFactory.createEmptyBorder(0, Theme.SPACE_16, 0, Theme.SPACE_16)
        ));
        if (dbConnected) {
            dbStatusLabel.setForeground(titleInk);
        } else {
            dbStatusLabel.setForeground(Theme.DANGER_FG);
        }
        statusSummaryLabel.setForeground(softInk);
        statusUserLabel.setForeground(softInk);

        sidebar.revalidate();
        sidebar.repaint();
        revalidate();
        repaint();
    }

    private void rebuildNavigation(List<NavItem> items) {
        navContainer.removeAll();
        navButtons.clear();
        for (NavItem item : items) {
            NavButton btn = new NavButton(item.pageKey(), item.label(), item.icon());
            btn.addActionListener(e -> navigateTo(item.pageKey()));
            navButtons.add(btn);
            navContainer.add(btn);
            navContainer.add(Box.createVerticalStrut(4));
        }
        updateActiveNavHighlight();
        navContainer.revalidate();
        navContainer.repaint();
    }

    public void navigateTo(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        PagePanel page = pages.get(key);
        if (page == null) {
            updateStatusSummary("Halaman belum terdaftar: " + key);
            return;
        }
        this.activePageKey = key;
        cardLayout.show(cardContainer, key);
        applyHeaderForPage(page);
        updateActiveNavHighlight();
        refreshActivePage();
    }

    private void applyHeaderForPage(PagePanel page) {
        pageTitleLabel.setText(page.getPageTitle() != null ? page.getPageTitle() : "");
        pageSubtitleLabel.setText(page.getPageSubtitle() != null ? page.getPageSubtitle() : "");
        accessorySlot.removeAll();
        JComponent accessory = page.getToolbarAccessory();
        if (accessory != null) {
            accessorySlot.add(accessory, BorderLayout.CENTER);
        }
        accessorySlot.revalidate();
        accessorySlot.repaint();
    }

    private void updateActiveNavHighlight() {
        for (NavButton btn : navButtons) {
            btn.setSelectedPage(btn.pageKey.equals(activePageKey));
        }
    }

    public void refreshActivePage() {
        if (activePageKey == null) {
            return;
        }
        PagePanel page = pages.get(activePageKey);
        if (page == null) {
            return;
        }
        try {
            page.refreshData();
            String summary = page.getStatusSummary();
            if (summary == null || summary.isBlank()) {
                updateStatusSummary(page.getPageTitle() + " dimuat.");
            } else {
                updateStatusSummary(summary);
            }
        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            updateStatusSummary("Gagal memuat data: " + (msg != null && !msg.isBlank() ? msg : ex.getClass().getSimpleName()));
        }
    }

    public void updateStatusSummary(String summary) {
        statusSummaryLabel.setText((summary == null || summary.isBlank()) ? "Siap." : summary);
    }

    public void updateDbStatus(boolean connected, String detail) {
        this.dbConnected = connected;
        dbDot.setConnected(connected);
        if (connected) {
            String dbName = (detail == null || detail.isBlank()) ? "pet_care" : detail.trim();
            dbStatusLabel.setText("MySQL Terhubung: " + dbName);
            dbStatusLabel.setForeground(adminPaletteActive ? Theme.Admin.TEXT : Theme.INK);
            dbStatusLabel.setToolTipText("Koneksi database aktif (" + dbName + ")");
        } else {
            dbStatusLabel.setText("MySQL Terputus");
            dbStatusLabel.setForeground(Theme.DANGER_FG);
            dbStatusLabel.setToolTipText(detail != null && !detail.isBlank() ? detail : "Tidak dapat terhubung ke MySQL.");
        }
    }

    public void setCartCount(int count) {
        int safeCount = Math.max(0, count);
        cartButton.setText("Keranjang (" + safeCount + ")");
    }

    public String getActivePageKey() {
        return activePageKey;
    }

    public boolean isSwitchModeButtonVisible() {
        return switchModeButton.isVisible();
    }

    public String getSwitchModeButtonText() {
        return switchModeButton.getFullText();
    }

    public String getDbStatusText() {
        return dbStatusLabel.getText();
    }

    public String getStatusSummaryText() {
        return statusSummaryLabel.getText();
    }

    private static final class BrandLogoButton extends JButton {
        private static final long serialVersionUID = 1L;

        BrandLogoButton() {
            BufferedImage logo = ImageStore.loadScaledFit("assets/img/logo.png", 36, 36, false);
            setIcon(logo != null ? new ImageIcon(logo) : VectorIcons.of(VectorIcons.Kind.PAW, 24, Theme.GOLD));
            Dimension size = new Dimension(44, 44);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder());
            setToolTipText("Ciutkan Sidebar");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, 24.0, 24.0);
                Color fill = getModel().isPressed() || getModel().isRollover()
                    ? new Color(255, 255, 255, 42)
                    : new Color(255, 255, 255, 24);
                g2.setColor(fill);
                g2.fill(shape);
                g2.setColor(new Color(255, 255, 255, isFocusOwner() ? 160 : 38));
                g2.setStroke(new BasicStroke(isFocusOwner() ? 1.5f : 1.0f));
                g2.draw(shape);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class NavButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final String pageKey;
        private final String fullLabel;
        private final transient Icon customerActiveIcon;
        private final transient Icon customerIdleIcon;
        private final transient Icon adminActiveIcon;
        private final transient Icon adminIdleIcon;
        private boolean selectedPage;
        private boolean adminMode;

        NavButton(String pageKey, String text, VectorIcons.Kind kind) {
            super(text);
            this.pageKey = pageKey;
            this.fullLabel = text;
            this.customerActiveIcon = kind != null ? VectorIcons.of(kind, 17, Theme.GOLD) : null;
            this.customerIdleIcon = kind != null ? VectorIcons.of(kind, 17, CUSTOMER_NAV_IDLE_FG) : null;
            this.adminActiveIcon = kind != null ? VectorIcons.of(kind, 17, Color.WHITE) : null;
            this.adminIdleIcon = kind != null ? VectorIcons.of(kind, 17, Theme.Admin.NAVY_TEXT) : null;

            setToolTipText(fullLabel);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            setAlignmentX(LEFT_ALIGNMENT);
            getModel().addChangeListener(e -> syncAppearance());
            applyState(false, false, Theme.SIDEBAR_WIDTH);
        }

        void setSelectedPage(boolean selectedPage) {
            this.selectedPage = selectedPage;
            syncAppearance();
        }

        void applyState(boolean adminMode, boolean collapsed, int sidebarWidth) {
            this.adminMode = adminMode;
            int innerW = Math.max(48, sidebarWidth - 20);
            setPreferredSize(new Dimension(innerW, 38));
            if (collapsed) {
                setText("");
                setHorizontalAlignment(SwingConstants.CENTER);
                setIconTextGap(0);
                setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            } else {
                setText(fullLabel);
                setHorizontalAlignment(SwingConstants.LEFT);
                setIconTextGap(12);
                setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            }
            syncAppearance();
        }

        private void syncAppearance() {
            boolean hover = !selectedPage && getModel().isRollover();
            if (adminMode) {
                if (selectedPage) {
                    setFont(Theme.bodyBold());
                    setForeground(Color.WHITE);
                    setIcon(adminActiveIcon);
                } else if (hover) {
                    setFont(Theme.body());
                    setForeground(Color.WHITE);
                    setIcon(adminActiveIcon);
                } else {
                    setFont(Theme.body());
                    setForeground(Theme.Admin.NAVY_TEXT);
                    setIcon(adminIdleIcon);
                }
            } else {
                if (selectedPage) {
                    setFont(Theme.bodyBold());
                    setForeground(Theme.GOLD);
                    setIcon(customerActiveIcon);
                } else if (hover) {
                    setFont(Theme.body());
                    setForeground(Theme.GOLD);
                    setIcon(customerActiveIcon);
                } else {
                    setFont(Theme.body());
                    setForeground(CUSTOMER_NAV_IDLE_FG);
                    setIcon(customerIdleIcon);
                }
            }
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
                    RoundRectangle2D pill = new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, 24.0, 24.0);
                    if (selectedPage) {
                        g2.setColor(adminMode ? Theme.Admin.BLUE : CUSTOMER_ACTIVE_PILL_BG);
                        g2.fill(pill);
                    } else if (getModel().isRollover() || getModel().isPressed()) {
                        g2.setColor(adminMode ? Theme.Admin.NAVY_HOVER : CUSTOMER_HOVER_PILL_BG);
                        g2.fill(pill);
                    }
                    if (isFocusOwner()) {
                        g2.setColor(adminMode ? Color.WHITE : Theme.GOLD);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.draw(pill);
                    }
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class SidebarActionButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final VectorIcons.Kind kind;
        private String fullText;
        private boolean collapsed;
        private Color hoverBg = Theme.NAVY_HOVER;
        private Color iconColor;

        SidebarActionButton(String text, VectorIcons.Kind kind, Color iconColor) {
            super(text);
            this.kind = kind;
            this.fullText = text;
            this.iconColor = iconColor;
            setIcon(VectorIcons.of(kind, 16, iconColor));
            setToolTipText(text);
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(10);
            setFont(Theme.smallBold());
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            setAlignmentX(LEFT_ALIGNMENT);
        }

        void setFullText(String text) {
            this.fullText = text != null ? text : "";
            setToolTipText(this.fullText);
            if (!collapsed) {
                super.setText(this.fullText);
            }
        }

        String getFullText() {
            return fullText;
        }

        void setIconColor(Color iconColor) {
            this.iconColor = iconColor;
            setIcon(VectorIcons.of(kind, 16, iconColor));
        }

        void applyState(Color hoverBg, boolean collapsed) {
            this.collapsed = collapsed;
            this.hoverBg = hoverBg != null ? hoverBg : Theme.NAVY_HOVER;
            if (collapsed) {
                super.setText("");
                setHorizontalAlignment(SwingConstants.CENTER);
                setIconTextGap(0);
                setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
            } else {
                super.setText(fullText);
                setHorizontalAlignment(SwingConstants.LEFT);
                setIconTextGap(10);
                setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            }
            if (getIcon() == null) {
                setIcon(VectorIcons.of(kind, 16, iconColor));
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover() || getModel().isPressed()) {
                    double arc = Theme.RADIUS * 2.0;
                    g2.setColor(hoverBg);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class ToolbarPillButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final VectorIcons.Kind kind;
        private Color bg;
        private Color hoverBg;
        private Color fg;
        private Color borderColor;
        private int radius;

        ToolbarPillButton(
                String text,
                VectorIcons.Kind kind,
                Color bg,
                Color hoverBg,
                Color fg,
                Color borderColor,
                int radius) {
            super(text);
            this.kind = kind;
            setFont(Theme.bodyBold());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setIconTextGap(8);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            applyPalette(bg, hoverBg, fg, borderColor, radius);
        }

        void applyPalette(Color bg, Color hoverBg, Color fg, Color borderColor, int radius) {
            this.bg = bg != null ? bg : Theme.SURFACE;
            this.hoverBg = hoverBg != null ? hoverBg : this.bg;
            this.fg = fg != null ? fg : Theme.INK;
            this.borderColor = borderColor;
            this.radius = Math.max(8, radius);
            setForeground(this.fg);
            setIcon(kind != null ? VectorIcons.of(kind, 16, this.fg) : null);
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
                    double arc = radius * 2.0;
                    RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, arc, arc);
                    Color fill = (getModel().isRollover() || getModel().isPressed()) ? hoverBg : bg;
                    g2.setColor(fill);
                    g2.fill(shape);
                    if (borderColor != null || isFocusOwner()) {
                        g2.setStroke(new BasicStroke(isFocusOwner() ? 1.8f : 1.5f));
                        g2.setColor(isFocusOwner() ? Theme.BLUE : borderColor);
                        g2.draw(new RoundRectangle2D.Double(0.75, 0.75, w - 1.5, h - 1.5, arc, arc));
                    }
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class RolePill extends JLabel {
        private static final long serialVersionUID = 1L;
        private boolean adminMode;

        RolePill() {
            super("USER", SwingConstants.CENTER);
            setFont(Theme.badge());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            setAdminMode(false);
        }

        void setAdminMode(boolean adminMode) {
            this.adminMode = adminMode;
            setText(adminMode ? "ADMIN" : "USER");
            setForeground(adminMode ? Theme.Admin.NAVY : Theme.INK);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(adminMode ? Theme.GOLD : Theme.CYAN_BTN);
                int h = getHeight();
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), h, h, h));
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class RoundedNavyCard extends JPanel {
        private static final long serialVersionUID = 1L;
        private Color fill = Theme.NAVY_HOVER;

        RoundedNavyCard() {
            setOpaque(false);
        }

        void setCardFill(Color fill) {
            this.fill = fill != null ? fill : Theme.NAVY_HOVER;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double arc = Theme.RADIUS * 2.0;
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class AvatarBadge extends JComponent {
        private static final long serialVersionUID = 1L;
        private String initials = "PP";
        private transient BufferedImage avatarImage;

        AvatarBadge() {
            Dimension fixed = new Dimension(32, 32);
            setPreferredSize(fixed);
            setMinimumSize(fixed);
            setMaximumSize(fixed);
            setOpaque(false);
        }

        void update(String name, String avatarPath) {
            this.initials = computeInitials(name);
            this.avatarImage = null;
            if (avatarPath != null && !avatarPath.isBlank()) {
                try {
                    this.avatarImage = ImageStore.loadScaledFit(avatarPath, 32, 32, true);
                } catch (RuntimeException ignored) {
                    this.avatarImage = null;
                }
            }
            repaint();
        }

        private static String computeInitials(String name) {
            if (name == null || name.isBlank()) {
                return "PP";
            }
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
            }
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int ox = Math.max(0, (getWidth() - 32) / 2);
                int oy = Math.max(0, (getHeight() - 32) / 2);
                if (avatarImage != null) {
                    g2.drawImage(avatarImage, ox, oy, 32, 32, null);
                } else {
                    g2.setColor(Theme.BLUE);
                    g2.fill(new Ellipse2D.Double(ox, oy, 32, 32));
                    g2.setColor(Theme.GOLD);
                    g2.setFont(Theme.smallBold());
                    var fm = g2.getFontMetrics();
                    int x = ox + (32 - fm.stringWidth(initials)) / 2;
                    int y = oy + ((32 - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(initials, x, y);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class HeaderBarPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        HeaderBarPanel() {
            super(new BorderLayout(Theme.SPACE_16, 0));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, Math.max(Theme.HEADER_HEIGHT, d.height));
        }
    }

    private static final class StatusDot extends JComponent {
        private static final long serialVersionUID = 1L;
        private boolean connected = true;

        StatusDot() {
            setPreferredSize(new Dimension(10, 10));
            setOpaque(false);
        }

        void setConnected(boolean connected) {
            this.connected = connected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(connected ? Theme.SUCCESS_DOT : Theme.DANGER_DOT);
                g2.fill(new Ellipse2D.Double(1, 1, 8, 8));
            } finally {
                g2.dispose();
            }
        }
    }
}
