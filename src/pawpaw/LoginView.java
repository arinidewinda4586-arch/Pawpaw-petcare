package pawpaw;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Login dummy Swing; hanya memeriksa input kosong. */
public final class LoginView extends JPanel {
    private final JTextField email = new JTextField() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !hasFocus())
                paintHint(g, "nama@email.com");
        }
    };
    private final JPasswordField password = new JPasswordField() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getPassword().length == 0 && !hasFocus())
                paintHint(g, "Masukkan kata sandi");
        }
    };
    private final JPanel card;
    private final JPanel center;
    private final JLabel error;

    public LoginView(MainApp app) {
        setLayout(new BorderLayout());
        JLabel brand = MainApp.label("✦  PawPaw Pet Care", 21, Color.WHITE, true);
        brand.setBorder(new EmptyBorder(22, 26, 12, 26));
        add(brand, BorderLayout.NORTH);
        card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(34, 30, 30, 30));
        JLabel title = MainApp.label("Selamat Datang Kembali!", 22, MainApp.NAVY, true);
        title.setAlignmentX(LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));
        JTextArea subtitle = MainApp.paragraph("Masuk untuk lanjut rawat si bulu", 15, new Color(100, 128, 156));
        subtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));
        error = MainApp.label(" ", 13, new Color(165, 56, 49), false);
        error.setName("loginError");
        error.setAlignmentX(LEFT_ALIGNMENT);
        card.add(error);
        card.add(Box.createVerticalStrut(10));
        email.setName("loginEmail");
        password.setName("loginPassword");
        field("Email", email);
        card.add(Box.createVerticalStrut(16));
        field("Kata Sandi", password);
        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        options.setAlignmentX(LEFT_ALIGNMENT);
        options.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JCheckBox remember = new JCheckBox("Ingat saya");
        remember.setOpaque(false);
        remember.setEnabled(false);
        remember.setToolTipText("Penyimpanan sesi belum tersedia.");
        JButton forgot = link("Lupa kata sandi?", () -> app.unavailable("pemulihan kata sandi"));
        options.add(remember, BorderLayout.WEST);
        options.add(forgot, BorderLayout.EAST);
        card.add(Box.createVerticalStrut(16));
        card.add(options);
        card.add(Box.createVerticalStrut(18));
        JButton submit = MainApp.button("Masuk", MainApp.BLUE, Color.WHITE);
        submit.setName("loginSubmit");
        submit.setAlignmentX(LEFT_ALIGNMENT);
        submit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        submit.addActionListener(e -> {
            char[] value = password.getPassword();
            String secret = new String(value);
            boolean missingPassword = value.length == 0 || secret.isBlank();
            Arrays.fill(value, '\0');
            if (email.getText().isBlank() || missingPassword) {
                error.setText("Lengkapi email/username dan kata sandi.");
                if (email.getText().isBlank())
                    email.requestFocusInWindow();
                else
                    password.requestFocusInWindow();
                return;
            }
            String entered = email.getText().trim();
            password.setText("");
            if (!app.login(entered, secret)) {
                error.setText("Email/username atau kata sandi salah.");
                password.requestFocusInWindow();
            }
        });
        app.getRootPane().setDefaultButton(submit);
        card.add(submit);
        card.add(Box.createVerticalStrut(32));
        JPanel signup = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        signup.setOpaque(false);
        signup.add(MainApp.label("Belum punya akun?", 13, MainApp.NAVY, false));
        signup.add(link("Daftar di sini", () -> app.unavailable("registrasi")));
        signup.setAlignmentX(LEFT_ALIGNMENT);
        signup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        card.add(signup);
        center = new JPanel(null);
        center.setOpaque(false);
        JPanel mascot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D q = (Graphics2D) g.create();
                q.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                q.setColor(MainApp.NAVY);
                q.fillPolygon(new int[] { 8, 20, 42 }, new int[] { 33, 0, 25 }, 3);
                q.fillPolygon(new int[] { 54, 78, 88 }, new int[] { 25, 0, 33 }, 3);
                q.setColor(new Color(44, 111, 176));
                q.fillOval(0, 16, 96, 78);
                q.setColor(Color.WHITE);
                q.fillOval(17, 36, 22, 22);
                q.fillOval(57, 36, 22, 22);
                q.setColor(Color.BLACK);
                q.fillOval(23, 43, 9, 9);
                q.fillOval(63, 43, 9, 9);
                q.fillOval(43, 63, 10, 8);
                q.dispose();
            }
        };
        mascot.setOpaque(false);
        center.add(mascot);
        center.add(card);
        center.setComponentZOrder(mascot, 0);
        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = scroll.getViewport().getWidth(), height = scroll.getViewport().getHeight();
                int cw = Math.min(420, Math.max(320, width - 40));
                int ch = 510;
                title.setFont(MainApp.font(cw < 400 ? 18 : 22, true));
                center.setPreferredSize(new Dimension(width, Math.max(640, height)));
                int y = Math.max(80, (Math.max(640, height) - ch) / 2);
                card.setBounds((width - cw) / 2, y, cw, ch);
                mascot.setBounds((width - 96) / 2, y - 64, 96, 94);
                center.revalidate();
                center.repaint();
            }
        });
    }

    private void paintHint(Graphics g, String text) {
        g.setFont(MainApp.font(15, false));
        g.setColor(new Color(129, 148, 168));
        g.drawString(text, 14, 29);
    }

    private void field(String text, JTextField input) {
        JLabel caption = MainApp.label(text, 14, MainApp.NAVY, true);
        caption.setLabelFor(input);
        caption.setAlignmentX(LEFT_ALIGNMENT);
        card.add(caption);
        card.add(Box.createVerticalStrut(7));
        input.setFont(MainApp.font(15, false));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 228, 242), 2, true), new EmptyBorder(10, 12, 10, 12)));
        input.setAlignmentX(LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        card.add(input);
    }

    private JButton link(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(MainApp.font(13, true));
        b.setForeground(MainApp.BLUE);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(4, 2, 4, 2));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D q = (Graphics2D) g.create();
        q.setPaint(new GradientPaint(0, 0, MainApp.NAVY, getWidth(), getHeight(), new Color(44, 111, 176)));
        q.fillRect(0, 0, getWidth(), getHeight());
        q.dispose();
    }
}
