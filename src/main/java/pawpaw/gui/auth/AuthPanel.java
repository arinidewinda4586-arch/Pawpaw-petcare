package pawpaw.gui.auth;

import pawpaw.config.DatabaseConfig;
import pawpaw.gui.common.UiKit;
import pawpaw.model.User;
import pawpaw.service.AuthService;
import pawpaw.util.ImageStore;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ViewportLayout;
import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.MultipleGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/**
 * Website-matched authentication experience with radial sky-blue backdrop,
 * peeking cursor-tracking cat mascot, single rounded 28px flip card (Login/Register),
 * identity-only Remember Me persistence, and live MySQL connection pill.
 */
public final class AuthPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String CARD_LOGIN = "LOGIN";
    private static final String CARD_REGISTER = "REGISTER";
    private static final int CARD_WIDTH = 430;
    private static final long FLIP_DURATION_NANOS = 240_000_000L;

    /**
     * Persistence abstraction for the "Ingat saya" identity-only remember option.
     */
    public interface RememberStore {
        boolean isRememberEnabled();

        String getRememberedIdentity();

        void save(boolean enabled, String identity);
    }

    private final transient AuthService authService;
    private final transient Consumer<User> onAuthSuccess;
    private final transient RememberStore rememberStore;

    private final MascotView mascotView;
    private final FlipCardPanel flipCardPanel;
    private final CardLayout faceLayout;
    private final JPanel faceHost;
    private final JPanel loginFace;
    private final JPanel registerFace;
    private final RoundedPillPanel demoHintPill;

    private final JTextField identityField;
    private final JPasswordField loginPasswordField;
    private final JCheckBox rememberCheckBox;
    private final JLabel loginErrorLabel;
    private final JButton loginButton;

    private final JTextField regNameField;
    private final JTextField regEmailField;
    private final JTextField regPhoneField;
    private final JPasswordField regPasswordField;
    private final JPasswordField regConfirmField;
    private final JLabel registerErrorLabel;
    private final JButton registerButton;

    private final ConnectionDot dbDot;
    private final JLabel dbStatusLabel;

    private boolean registerMode;
    private final Timer flipTimer;
    private long flipStartNanos;
    private boolean flipSwapped;
    private boolean flipTargetRegister;

    public AuthPanel() {
        this(new AuthService(), user -> {
        }, new PreferencesRememberStore());
    }

    public AuthPanel(AuthService authService, Consumer<User> onAuthSuccess) {
        this(authService, onAuthSuccess, new PreferencesRememberStore());
    }

    public AuthPanel(AuthService authService, Consumer<User> onAuthSuccess, RememberStore rememberStore) {
        super(new BorderLayout(0, 0));
        this.authService = Objects.requireNonNull(authService, "AuthService tidak boleh null.");
        this.onAuthSuccess = onAuthSuccess != null ? onAuthSuccess : user -> {
        };
        this.rememberStore = rememberStore != null ? rememberStore : new PreferencesRememberStore();

        setOpaque(true);
        setBackground(Theme.AUTH_CREAM);

        // --- Top Transparent Overlay Bar (Brand Left, MySQL Pill Right) ---
        JPanel topBar = new JPanel(new BorderLayout(Theme.SPACE_16, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(16, 24, 4, 24));

        JPanel brandLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandLeft.setOpaque(false);
        BufferedImage logoImg = ImageStore.loadScaledFit("assets/img/logo.png", 28, 28, false);
        JLabel logoLabel = new JLabel(
            logoImg != null ? new ImageIcon(logoImg) : VectorIcons.of(VectorIcons.Kind.PAW, 22, Theme.NAVY)
        );
        JLabel brandTitle = UiKit.label("PawPaw Pet Care", Theme.heading(), Theme.INK);
        brandLeft.add(logoLabel);
        brandLeft.add(brandTitle);

        dbDot = new ConnectionDot();
        dbStatusLabel = UiKit.label("Memeriksa koneksi...", Theme.badge(), Theme.INK);
        JButton retryDbBtn = new JButton(VectorIcons.of(VectorIcons.Kind.REFRESH, 14, Theme.BLUE));
        retryDbBtn.setToolTipText("Periksa ulang koneksi MySQL");
        retryDbBtn.setContentAreaFilled(false);
        retryDbBtn.setBorderPainted(false);
        retryDbBtn.setFocusPainted(false);
        retryDbBtn.setOpaque(false);
        retryDbBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        retryDbBtn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
        retryDbBtn.addActionListener(e -> refreshDbStatus());

        RoundedPillPanel statusPill = new RoundedPillPanel(
            new FlowLayout(FlowLayout.CENTER, 8, 5),
            Theme.SURFACE,
            Theme.BORDER,
            -1
        );
        statusPill.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 8));
        statusPill.add(dbDot);
        statusPill.add(dbStatusLabel);
        statusPill.add(retryDbBtn);

        JPanel statusRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        statusRight.setOpaque(false);
        statusRight.add(statusPill);

        topBar.add(brandLeft, BorderLayout.WEST);
        topBar.add(statusRight, BorderLayout.EAST);

        // --- Login Face Controls ---
        identityField = UiKit.textField("admin@pawpaw.com atau arini");
        loginPasswordField = UiKit.passwordField("Masukkan kata sandi");
        rememberCheckBox = new JCheckBox("Ingat saya");
        rememberCheckBox.setFont(Theme.smallBold());
        rememberCheckBox.setForeground(Theme.INK);
        rememberCheckBox.setOpaque(false);
        rememberCheckBox.setFocusPainted(false);
        rememberCheckBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginErrorLabel = createInlineErrorLabel();
        loginButton = UiKit.primaryButton("Masuk", VectorIcons.Kind.CHECK);
        loginButton.addActionListener(e -> submitLogin());

        identityField.addActionListener(e -> submitLogin());
        loginPasswordField.addActionListener(e -> submitLogin());

        // --- Register Face Controls ---
        regNameField = UiKit.textField("Masukkan nama lengkap");
        regEmailField = UiKit.textField("nama@email.com");
        regPhoneField = UiKit.textField("08xxxxxxxxxx (opsional)");
        regPasswordField = UiKit.passwordField("Minimal 6 karakter");
        regConfirmField = UiKit.passwordField("Ulangi kata sandi");
        registerErrorLabel = createInlineErrorLabel();
        registerButton = UiKit.primaryButton("Daftar & Masuk", VectorIcons.Kind.PLUS);
        registerButton.addActionListener(e -> submitRegister());

        regNameField.addActionListener(e -> submitRegister());
        regEmailField.addActionListener(e -> submitRegister());
        regPhoneField.addActionListener(e -> submitRegister());
        regPasswordField.addActionListener(e -> submitRegister());
        regConfirmField.addActionListener(e -> submitRegister());

        loginFace = buildLoginFace();
        registerFace = buildRegisterFace();

        faceLayout = new CardLayout();
        faceHost = new JPanel(faceLayout) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                Component active = registerMode ? registerFace : loginFace;
                if (active != null) {
                    Dimension d = active.getPreferredSize();
                    Insets insets = getInsets();
                    return new Dimension(
                        d.width + insets.left + insets.right,
                        d.height + insets.top + insets.bottom
                    );
                }
                return super.getPreferredSize();
            }
        };
        faceHost.setOpaque(false);
        faceHost.add(loginFace, CARD_LOGIN);
        faceHost.add(registerFace, CARD_REGISTER);

        flipCardPanel = new FlipCardPanel(faceHost);
        mascotView = new MascotView();
        demoHintPill = new RoundedPillPanel(
            new GridLayout(2, 1, 0, 2),
            new Color(0xF0, 0xF7, 0xFF),
            Theme.BORDER,
            12
        );
        demoHintPill.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        JLabel demoLine1 = UiKit.label(
            "Akun demo: admin@pawpaw.com / admin123",
            Theme.badge(),
            Theme.MUTED
        );
        demoLine1.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel demoLine2 = UiKit.label(
            "Pelanggan: jesika@pawpaw.com / password123",
            Theme.badge(),
            Theme.MUTED
        );
        demoLine2.setHorizontalAlignment(SwingConstants.CENTER);
        demoHintPill.add(demoLine1);
        demoHintPill.add(demoLine2);

        AuthStackPanel authStack = new AuthStackPanel(mascotView, flipCardPanel, demoHintPill);

        ScrollableCenterHost centerHost = new ScrollableCenterHost(authStack);
        JScrollPane centerScroll = createTransparentScrollPane(centerHost);

        add(topBar, BorderLayout.NORTH);
        add(centerScroll, BorderLayout.CENTER);

        flipTimer = new Timer(16, e -> stepFlipAnimation());
        flipTimer.setRepeats(true);

        addHierarchyListener(e -> syncDefaultButton());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                syncDefaultButton();
            }
        });

        applyRememberedIdentity();
        showLoginFaceImmediate();
        refreshDbStatus();
    }

    private JPanel buildLoginFace() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = createFormConstraints();
        int row = 0;

        JLabel heading = UiKit.label("Selamat Datang Kembali!", Theme.title(), Theme.INK);
        JLabel subtitle = UiKit.label("Masuk untuk lanjut rawat si bulu", Theme.small(), Theme.MUTED);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 2, 0);
        panel.add(heading, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 16, 0);
        panel.add(subtitle, gbc);

        addFormField(panel, gbc, row++, "Email atau Username", identityField);
        addFormField(panel, gbc, row++, "Kata Sandi", loginPasswordField);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(rememberCheckBox, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(loginErrorLabel, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(2, 0, 14, 0);
        panel.add(loginButton, gbc);

        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.insets = new Insets(4, 0, 0, 0);
        panel.add(buildSwitchRow("Belum punya akun?", "Daftar di sini", this::flipToRegister), gbc);

        return panel;
    }

    private JPanel buildRegisterFace() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        GridBagConstraints gbc = createFormConstraints();
        int row = 0;

        JLabel heading = UiKit.label("Gabung Yuk!", Theme.title(), Theme.INK);
        JLabel subtitle = UiKit.label("Daftar buat mulai adopsi & rawat hewan", Theme.small(), Theme.MUTED);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 2, 0);
        panel.add(heading, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 14, 0);
        panel.add(subtitle, gbc);

        addFormField(panel, gbc, row++, "Nama Lengkap", regNameField);
        addFormField(panel, gbc, row++, "Email", regEmailField);
        addFormField(panel, gbc, row++, "Nomor Telepon (opsional)", regPhoneField);
        addFormField(panel, gbc, row++, "Kata Sandi", regPasswordField);
        addFormField(panel, gbc, row++, "Konfirmasi Kata Sandi", regConfirmField);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(registerErrorLabel, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(2, 0, 12, 0);
        panel.add(registerButton, gbc);

        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.insets = new Insets(2, 0, 0, 0);
        panel.add(buildSwitchRow("Sudah punya akun?", "Masuk di sini", this::flipToLogin), gbc);

        return panel;
    }

    private static JPanel buildSwitchRow(String promptText, String actionText, Runnable onSwitch) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        row.setOpaque(false);
        JLabel prompt = UiKit.label(promptText, Theme.small(), Theme.MUTED);
        JButton linkBtn = createLinkButton(actionText, onSwitch);
        row.add(prompt);
        row.add(linkBtn);
        return row;
    }

    private static JButton createLinkButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.smallBold());
        btn.setForeground(Theme.BLUE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setMargin(new Insets(0, 2, 0, 2));
        btn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (action != null) {
                action.run();
            }
        });
        return btn;
    }

    private static JScrollPane createTransparentScrollPane(ScrollableCenterHost host) {
        JScrollPane scroll = new JScrollPane();
        JViewport viewport = new JViewport();
        viewport.setOpaque(false);
        viewport.setLayout(new ExpandingCenterViewportLayout());
        scroll.setViewport(viewport);
        scroll.setViewportView(host);
        scroll.setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private static GridBagConstraints createFormConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        return gbc;
    }

    private static void addFormField(
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
        group.add(UiKit.formLabel(labelText), BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        container.add(group, gbc);
    }

    private static JLabel createInlineErrorLabel() {
        JLabel label = UiKit.label("", Theme.small(), Theme.DANGER_FG);
        label.setOpaque(true);
        label.setBackground(Theme.DANGER_BG);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xFC, 0xA5, 0xA5), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        label.setVisible(false);
        return label;
    }

    private void flipToRegister() {
        startFlipAnimation(true);
    }

    private void flipToLogin() {
        startFlipAnimation(false);
    }

    private void startFlipAnimation(boolean targetRegister) {
        if (flipTimer.isRunning()) {
            flipTimer.stop();
        }
        if (this.registerMode == targetRegister && flipCardPanel.getScaleX() >= 0.999) {
            return;
        }
        this.flipTargetRegister = targetRegister;
        this.flipStartNanos = System.nanoTime();
        this.flipSwapped = false;
        flipTimer.start();
    }

    private void stepFlipAnimation() {
        long elapsed = System.nanoTime() - flipStartNanos;
        double progress = Math.min(1.0, elapsed / (double) FLIP_DURATION_NANOS);

        if (progress < 0.5) {
            double phase = progress * 2.0;
            double eased = 0.5 - 0.5 * Math.cos(phase * Math.PI);
            flipCardPanel.setScaleX(Math.max(0.08, 1.0 - 0.92 * eased));
        } else {
            if (!flipSwapped) {
                flipSwapped = true;
                applyFaceImmediate(flipTargetRegister);
            }
            double phase = (progress - 0.5) * 2.0;
            double eased = 0.5 - 0.5 * Math.cos(phase * Math.PI);
            flipCardPanel.setScaleX(Math.max(0.08, 0.08 + 0.92 * eased));
        }

        if (progress >= 1.0) {
            flipTimer.stop();
            flipCardPanel.setScaleX(1.0);
            JComponent focusTarget = registerMode ? regNameField : identityField;
            focusTarget.requestFocusInWindow();
        }
    }

    public boolean isRegisterMode() {
        return registerMode;
    }

    public boolean isFlipAnimating() {
        return flipTimer.isRunning();
    }

    public void showLoginFaceImmediate() {
        if (flipTimer.isRunning()) {
            flipTimer.stop();
        }
        flipCardPanel.setScaleX(1.0);
        applyFaceImmediate(false);
    }

    public void showRegisterFaceImmediate() {
        if (flipTimer.isRunning()) {
            flipTimer.stop();
        }
        flipCardPanel.setScaleX(1.0);
        applyFaceImmediate(true);
    }

    private void applyFaceImmediate(boolean showRegister) {
        this.registerMode = showRegister;
        faceLayout.show(faceHost, showRegister ? CARD_REGISTER : CARD_LOGIN);
        demoHintPill.setVisible(!showRegister);
        syncDefaultButton();
        revalidateAncestorChain(faceHost);
    }

    private void syncDefaultButton() {
        if (!isVisible()) {
            return;
        }
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane != null) {
            rootPane.setDefaultButton(registerMode ? registerButton : loginButton);
        }
    }

    private void submitLogin() {
        clearError(loginErrorLabel);
        String identity = identityField.getText();
        String password = new String(loginPasswordField.getPassword());

        try {
            User authenticated = authService.login(identity, password);
            if (rememberCheckBox.isSelected()) {
                String cleanIdentity = identity != null ? identity.trim() : "";
                rememberStore.save(true, cleanIdentity);
            } else {
                rememberStore.save(false, "");
            }
            loginPasswordField.setText("");
            clearError(loginErrorLabel);
            onAuthSuccess.accept(authenticated);
        } catch (RuntimeException ex) {
            setInlineError(loginErrorLabel, formatAuthError(ex));
            refreshDbStatus();
        }
    }

    private void submitRegister() {
        clearError(registerErrorLabel);
        String name = regNameField.getText();
        String email = regEmailField.getText();
        String phone = regPhoneField.getText();
        String password = new String(regPasswordField.getPassword());
        String confirm = new String(regConfirmField.getPassword());

        try {
            User registered = authService.register(name, email, password, confirm, phone);
            regPasswordField.setText("");
            regConfirmField.setText("");
            clearError(registerErrorLabel);
            onAuthSuccess.accept(registered);
        } catch (RuntimeException ex) {
            setInlineError(registerErrorLabel, formatAuthError(ex));
            refreshDbStatus();
        }
    }

    private void applyRememberedIdentity() {
        boolean enabled = rememberStore.isRememberEnabled();
        String savedIdentity = rememberStore.getRememberedIdentity();
        rememberCheckBox.setSelected(enabled);
        if (enabled && savedIdentity != null && !savedIdentity.isBlank()) {
            identityField.setText(savedIdentity.trim());
        } else {
            identityField.setText("");
        }
    }

    private static String formatAuthError(RuntimeException ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "Gagal memproses autentikasi (" + ex.getClass().getSimpleName() + ").";
        }
        return "<html>" + escapeHtml(msg) + "</html>";
    }

    private static String escapeHtml(String raw) {
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void setInlineError(JLabel target, String htmlMessage) {
        target.setText(htmlMessage);
        target.setVisible(true);
        revalidateAncestorChain(target);
    }

    private static void clearError(JLabel target) {
        target.setText("");
        target.setVisible(false);
        revalidateAncestorChain(target);
    }

    private static void revalidateAncestorChain(JComponent component) {
        Container parent = component.getParent();
        while (parent != null) {
            if (parent instanceof JComponent jc) {
                jc.revalidate();
                jc.repaint();
            }
            parent = parent.getParent();
        }
    }

    public void refreshDbStatus() {
        boolean ok = DatabaseConfig.testConnection();
        setDbStatus(ok, DatabaseConfig.getLastError());
    }

    public void setDbStatus(boolean connected, String detail) {
        dbDot.setConnected(connected);
        if (connected) {
            dbStatusLabel.setText("MySQL Terhubung: pet_care");
            dbStatusLabel.setForeground(Theme.INK);
            dbStatusLabel.setToolTipText("Koneksi MySQL aktif");
        } else {
            dbStatusLabel.setText("MySQL Terputus");
            dbStatusLabel.setForeground(Theme.DANGER_FG);
            dbStatusLabel.setToolTipText(detail != null && !detail.isBlank() ? detail : "MySQL tidak dapat dijangkau.");
        }
    }

    public void resetForm() {
        applyRememberedIdentity();
        loginPasswordField.setText("");
        regNameField.setText("");
        regEmailField.setText("");
        regPhoneField.setText("");
        regPasswordField.setText("");
        regConfirmField.setText("");
        clearError(loginErrorLabel);
        clearError(registerErrorLabel);
        showLoginFaceImmediate();
        refreshDbStatus();
    }

    public boolean isRememberMeSelected() {
        return rememberCheckBox.isSelected();
    }

    public void setRememberMeSelected(boolean selected) {
        rememberCheckBox.setSelected(selected);
    }

    public String getIdentityFieldText() {
        return identityField.getText();
    }

    public int getPasswordLength() {
        return loginPasswordField.getPassword().length;
    }

    public String getDbStatusText() {
        return dbStatusLabel.getText();
    }

    public void attemptLoginForTest(String identity, String password) {
        showLoginFaceImmediate();
        identityField.setText(identity != null ? identity : "");
        loginPasswordField.setText(password != null ? password : "");
        submitLogin();
    }

    public void attemptRegisterForTest(
            String name,
            String email,
            String phone,
            String password,
            String confirmPassword) {
        showRegisterFaceImmediate();
        regNameField.setText(name != null ? name : "");
        regEmailField.setText(email != null ? email : "");
        regPhoneField.setText(phone != null ? phone : "");
        regPasswordField.setText(password != null ? password : "");
        regConfirmField.setText(confirmPassword != null ? confirmPassword : "");
        submitRegister();
    }

    public String getInlineErrorText() {
        JLabel active = registerMode ? registerErrorLabel : loginErrorLabel;
        return active.isVisible() ? active.getText() : "";
    }

    @Override
    public void removeNotify() {
        if (flipTimer.isRunning()) {
            flipTimer.stop();
        }
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setColor(Theme.AUTH_CREAM);
            g2.fillRect(0, 0, w, h);

            float maxDim = Math.max(w, h);
            float r1 = Math.max(1f, 0.45f * maxDim);
            g2.setPaint(new RadialGradientPaint(
                0.12f * w,
                0.18f * h,
                r1,
                new float[] { 0f, 1f },
                new Color[] { Theme.AUTH_PEACH, Theme.tint(Theme.AUTH_PEACH, 0) },
                MultipleGradientPaint.CycleMethod.NO_CYCLE
            ));
            g2.fillRect(0, 0, w, h);

            float r2 = Math.max(1f, 0.48f * maxDim);
            g2.setPaint(new RadialGradientPaint(
                0.88f * w,
                0.82f * h,
                r2,
                new float[] { 0f, 1f },
                new Color[] { Theme.AUTH_PINK, Theme.tint(Theme.AUTH_PINK, 0) },
                MultipleGradientPaint.CycleMethod.NO_CYCLE
            ));
            g2.fillRect(0, 0, w, h);
        } finally {
            g2.dispose();
        }
    }

    private static final class PreferencesRememberStore implements RememberStore {
        private static final String KEY_ENABLED = "remember_enabled";
        private static final String KEY_IDENTITY = "remember_identity";

        @Override
        public boolean isRememberEnabled() {
            try {
                Preferences prefs = Preferences.userNodeForPackage(AuthPanel.class);
                return prefs.getBoolean(KEY_ENABLED, false);
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        @Override
        public String getRememberedIdentity() {
            try {
                Preferences prefs = Preferences.userNodeForPackage(AuthPanel.class);
                return prefs.get(KEY_IDENTITY, "");
            } catch (RuntimeException ignored) {
                return "";
            }
        }

        @Override
        public void save(boolean enabled, String identity) {
            try {
                Preferences prefs = Preferences.userNodeForPackage(AuthPanel.class);
                prefs.putBoolean(KEY_ENABLED, enabled);
                if (enabled && identity != null && !identity.isBlank()) {
                    prefs.put(KEY_IDENTITY, identity.trim());
                } else {
                    prefs.remove(KEY_IDENTITY);
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static final class RoundedPillPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Color fillColor;
        private final Color borderColor;
        private final int radius;

        RoundedPillPanel(LayoutManager layout, Color fillColor, Color borderColor, int radius) {
            super(layout);
            this.fillColor = fillColor != null ? fillColor : Theme.SURFACE;
            this.borderColor = borderColor != null ? borderColor : Theme.BORDER;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                if (w > 2 && h > 2) {
                    double arc = radius < 0 ? (h - 1.0) : (radius * 2.0);
                    RoundRectangle2D pill = new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, arc, arc);
                    g2.setColor(fillColor);
                    g2.fill(pill);
                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.draw(pill);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class AuthStackPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int MASCOT_OVERLAP = 30;
        private static final int HINT_GAP = 12;

        AuthStackPanel(MascotView mascotView, FlipCardPanel cardPanel, RoundedPillPanel hintPill) {
            setOpaque(false);
            setLayout(new StackLayout(mascotView, cardPanel, hintPill));
            // Index 0 paints last in AWT/Swing Container, placing MascotView above card top edge.
            add(mascotView);
            add(cardPanel);
            add(hintPill);
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return false;
        }

        private static final class StackLayout implements LayoutManager {
            private final MascotView mascotView;
            private final FlipCardPanel cardPanel;
            private final RoundedPillPanel hintPill;

            StackLayout(MascotView mascotView, FlipCardPanel cardPanel, RoundedPillPanel hintPill) {
                this.mascotView = mascotView;
                this.cardPanel = cardPanel;
                this.hintPill = hintPill;
            }

            @Override
            public void addLayoutComponent(String name, Component comp) {
            }

            @Override
            public void removeLayoutComponent(Component comp) {
            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                Dimension mascotPref = mascotView.getPreferredSize();
                Dimension cardPref = cardPanel.getPreferredSize();
                int w = Math.max(cardPref.width, mascotPref.width);
                int h = (mascotPref.height - MASCOT_OVERLAP) + cardPref.height;
                if (hintPill.isVisible()) {
                    Dimension hintPref = hintPill.getPreferredSize();
                    w = Math.max(w, hintPref.width);
                    h += HINT_GAP + hintPref.height;
                }
                return new Dimension(w, h);
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return preferredLayoutSize(parent);
            }

            @Override
            public void layoutContainer(Container parent) {
                int w = parent.getWidth();
                if (w <= 0) {
                    w = CARD_WIDTH;
                }
                Dimension mascotPref = mascotView.getPreferredSize();
                Dimension cardPref = cardPanel.getPreferredSize();
                int cardW = Math.min(CARD_WIDTH, w);
                int cardX = Math.max(0, (w - cardW) / 2);
                int mascotX = Math.max(0, (w - mascotPref.width) / 2);
                int cardY = Math.max(0, mascotPref.height - MASCOT_OVERLAP);

                mascotView.setBounds(mascotX, 0, mascotPref.width, mascotPref.height);
                cardPanel.setBounds(cardX, cardY, cardW, cardPref.height);

                if (hintPill.isVisible()) {
                    Dimension hintPref = hintPill.getPreferredSize();
                    int hintW = Math.max(cardW, Math.min(w, hintPref.width));
                    int hintX = Math.max(0, (w - hintW) / 2);
                    hintPill.setBounds(hintX, cardY + cardPref.height + HINT_GAP, hintW, hintPref.height);
                } else {
                    hintPill.setBounds(cardX, cardY + cardPref.height, 0, 0);
                }
            }
        }
    }

    private static final class FlipCardPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private static final int SHADOW_OFFSET_Y = 6;
        private static final int SHADOW_PAD_X = 4;
        private double scaleX = 1.0;

        FlipCardPanel(JPanel faceHost) {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(42, 32, 28 + SHADOW_OFFSET_Y, 32));
            add(faceHost, BorderLayout.CENTER);
        }

        double getScaleX() {
            return scaleX;
        }

        void setScaleX(double scaleX) {
            this.scaleX = Math.max(0.08, Math.min(1.0, scaleX));
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(CARD_WIDTH, d.height);
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension pref = getPreferredSize();
            return new Dimension(320, pref.height);
        }

        @Override
        public void paint(Graphics g) {
            if (scaleX >= 0.999 || getWidth() <= 0 || getHeight() <= 0) {
                super.paint(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                double cx = getWidth() / 2.0;
                g2.translate(cx, 0.0);
                g2.scale(scaleX, 1.0);
                g2.translate(-cx, 0.0);
                super.paint(g2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 8 || h <= 10) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double arc = Theme.AUTH_CARD_RADIUS * 2.0;
                double cardW = w - SHADOW_PAD_X * 2.0;
                double cardH = h - SHADOW_OFFSET_Y - 2.0;
                double cardX = SHADOW_PAD_X;
                double cardY = 1.0;

                // Soft layered blue shadow matching 0 20px 45px rgba(15, 40, 84, 0.16)
                g2.setColor(new Color(15, 40, 84, 18));
                g2.fill(new RoundRectangle2D.Double(cardX - 2, cardY + 5, cardW + 4, cardH + 2, arc, arc));
                g2.setColor(new Color(15, 40, 84, 24));
                g2.fill(new RoundRectangle2D.Double(cardX, cardY + 3, cardW, cardH, arc, arc));

                RoundRectangle2D cardShape = new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, arc, arc);
                g2.setColor(Theme.SURFACE);
                g2.fill(cardShape);

                g2.setColor(Theme.AUTH_BORDER);
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(cardShape);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final class MascotView extends JComponent {
        private static final long serialVersionUID = 1L;
        private static final int FACE_WIDTH = 96;
        private static final int FACE_HEIGHT = 78;
        private static final int EAR_OVERHANG = 14;
        private static final double MAX_PUPIL_OFFSET = 5.0;

        private double pupilOffsetX;
        private double pupilOffsetY;
        private transient boolean listenerRegistered;
        private final transient AWTEventListener mouseMotionListener = event -> {
            if (event instanceof MouseEvent me) {
                int id = me.getID();
                if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED) {
                    handleGlobalMouseMotion(me);
                }
            }
        };

        MascotView() {
            Dimension fixed = new Dimension(FACE_WIDTH, FACE_HEIGHT + EAR_OVERHANG);
            setPreferredSize(fixed);
            setMinimumSize(fixed);
            setMaximumSize(fixed);
            setOpaque(false);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            if (!listenerRegistered) {
                try {
                    Toolkit.getDefaultToolkit().addAWTEventListener(
                        mouseMotionListener,
                        AWTEvent.MOUSE_MOTION_EVENT_MASK
                    );
                    listenerRegistered = true;
                } catch (SecurityException | HeadlessException ignored) {
                }
            }
        }

        @Override
        public void removeNotify() {
            if (listenerRegistered) {
                try {
                    Toolkit.getDefaultToolkit().removeAWTEventListener(mouseMotionListener);
                } catch (SecurityException | HeadlessException ignored) {
                } finally {
                    listenerRegistered = false;
                }
            }
            super.removeNotify();
        }

        private void handleGlobalMouseMotion(MouseEvent me) {
            if (!isShowing() || getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            Point screenPoint;
            try {
                screenPoint = me.getLocationOnScreen();
            } catch (IllegalComponentStateException | HeadlessException ignored) {
                return;
            }
            if (screenPoint == null) {
                return;
            }
            Point local = new Point(screenPoint);
            try {
                SwingUtilities.convertPointFromScreen(local, this);
            } catch (IllegalComponentStateException ignored) {
                return;
            }
            updatePupilsForLocalPoint(local.x, local.y);
        }

        private void updatePupilsForLocalPoint(int x, int y) {
            if (getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            double cx = getWidth() / 2.0;
            double cy = EAR_OVERHANG + FACE_HEIGHT / 2.0;
            double dx = x - cx;
            double dy = y - cy;
            double dist = Math.hypot(dx, dy);

            double nextX;
            double nextY;
            if (dist < 0.001) {
                nextX = 0.0;
                nextY = 0.0;
            } else {
                double angle = Math.atan2(dy, dx);
                double mag = Math.min(MAX_PUPIL_OFFSET, dist / 20.0);
                nextX = Math.cos(angle) * mag;
                nextY = Math.sin(angle) * mag;
            }

            if (Math.abs(nextX - pupilOffsetX) > 0.05 || Math.abs(nextY - pupilOffsetY) > 0.05) {
                this.pupilOffsetX = nextX;
                this.pupilOffsetY = nextY;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int faceY = EAR_OVERHANG;

                // Soft shadow under peeking mascot face
                g2.setColor(new Color(15, 40, 84, 34));
                g2.fill(new RoundRectangle2D.Double(2, faceY + 4, FACE_WIDTH - 4, FACE_HEIGHT - 2, 72, 68));

                // Rotated cat ears (#0F2854, ±12 deg)
                drawEar(g2, 24.0, faceY + 2.0, -12.0);
                drawEar(g2, FACE_WIDTH - 24.0, faceY + 2.0, 12.0);

                // Face gradient (#1C4D8D -> #0F2854) with 2px #4988C4 border
                RoundRectangle2D faceShape = new RoundRectangle2D.Double(
                    1.0,
                    faceY + 1.0,
                    FACE_WIDTH - 2.0,
                    FACE_HEIGHT - 2.0,
                    78.0,
                    70.0
                );
                g2.setPaint(new GradientPaint(0, faceY, Theme.BLUE, FACE_WIDTH, faceY + FACE_HEIGHT, Theme.NAVY));
                g2.fill(faceShape);
                g2.setColor(Theme.BANNER_BLUE);
                g2.setStroke(new BasicStroke(2.0f));
                g2.draw(faceShape);

                // Eyes (22x22 white circles at x=14 and x=60, y=faceY+26)
                double eyeY = faceY + 26.0;
                drawEyeWithPupil(g2, 14.0, eyeY);
                drawEyeWithPupil(g2, FACE_WIDTH - 14.0 - 22.0, eyeY);

                // Nose (#FFC93C oval 10x8 at x=43, y=faceY+46)
                g2.setColor(Theme.GOLD);
                g2.fill(new Ellipse2D.Double(43.0, faceY + 46.0, 10.0, 8.0));
            } finally {
                g2.dispose();
            }
        }

        private static void drawEar(Graphics2D g2, double baseCenterX, double baseY, double angleDegrees) {
            AffineTransform prev = g2.getTransform();
            g2.rotate(Math.toRadians(angleDegrees), baseCenterX, baseY);
            Path2D ear = new Path2D.Double();
            ear.moveTo(baseCenterX - 18.0, baseY + 12.0);
            ear.lineTo(baseCenterX, baseY - 16.0);
            ear.lineTo(baseCenterX + 18.0, baseY + 12.0);
            ear.closePath();
            g2.setColor(Theme.NAVY);
            g2.fill(ear);
            g2.setTransform(prev);
        }

        private void drawEyeWithPupil(Graphics2D g2, double eyeX, double eyeY) {
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(eyeX, eyeY, 22.0, 22.0));

            double pupilX = eyeX + (22.0 - 9.0) / 2.0 + pupilOffsetX;
            double pupilY = eyeY + (22.0 - 9.0) / 2.0 + pupilOffsetY;
            g2.setColor(Theme.NAVY);
            g2.fill(new Ellipse2D.Double(pupilX, pupilY, 9.0, 9.0));
        }
    }

    private static final class ScrollableCenterHost extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        ScrollableCenterHost(JComponent content) {
            super(new GridBagLayout());
            setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(8, 16, 24, 16);
            add(content, gbc);
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

    private static final class ExpandingCenterViewportLayout extends ViewportLayout {
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

    private static final class ConnectionDot extends JComponent {
        private static final long serialVersionUID = 1L;
        private boolean connected;

        ConnectionDot() {
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
