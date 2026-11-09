package pawpaw;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Kerangka halaman bergaya PawPaw-GUI: bar promo emas, sidebar navy yang bisa diperkecil,
 * banner gradasi, area konten putih yang bisa digulir, dan tata letak adaptif.
 * Semua halaman setelah login (Beranda, Adopsi, Toko, Keranjang, Checkout, Kesehatan, ...) mewarisi class ini (INHERITANCE).
 * Subclass cukup mengisi panel {@link #content} dan, bila perlu, meng-override {@link #onWidth(int)}.
 */
public abstract class HalamanDasar extends JPanel {
    private static final String[] NAMES = { "Beranda", "Adopsi", "Toko", "Kesehatan", "Profil", "Logout" };
    /** Status sidebar (ringkas/lebar) dipertahankan saat pindah halaman. */
    private static boolean compact;

    protected final MainApp app;
    /** Area konten putih; tambahkan komponen ke sini (BoxLayout vertikal). */
    protected final JPanel content = new JPanel();
    /** Area di bawah konten putih (di atas hak cipta), mis. footer beranda. Kosong secara bawaan. */
    protected final JPanel below = new JPanel();

    private final String active;
    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel sidebar = new JPanel();
    private final JPanel mobileBar = new JPanel(new BorderLayout());
    private final JScrollPane sideScroll;
    private final JButton[] nav = new JButton[NAMES.length];
    private final JButton logo = new JButton();
    private final JPanel hero;
    private final JLabel heroTitle;
    private final JTextArea heroText;
    private final JLabel heroPicture;
    private JScrollPane mainScroll;
    private JButton mobileMenu;
    private boolean narrowMode;
    private int viewportWidth = 1000;

    protected HalamanDasar(MainApp app, String active, String title, String subtitle) {
        this.app = app;
        this.active = active;
        setLayout(new BorderLayout());
        setBackground(MainApp.NAVY);

        JLabel promo = MainApp.label(
                "50+ Adopsi Setiap Bulan  •  Jadwalkan Vaksin Kapan Saja  •  Semua Item Toko Diskon 25%", 13,
                MainApp.NAVY, true);
        promo.setHorizontalAlignment(SwingConstants.CENTER);
        promo.setOpaque(true);
        promo.setBackground(MainApp.GOLD);
        promo.setBorder(new EmptyBorder(7, 12, 7, 12));
        add(promo, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        // ----- Sidebar -----
        sidebar.setBackground(MainApp.NAVY);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(22, 12, 22, 12));
        logo.setName("sidebarToggle");
        logo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logo.setBorder(new EmptyBorder(0, 0, 25, 0));
        logo.setContentAreaFilled(false);
        logo.setFocusPainted(false);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        logo.addActionListener(e -> {
            compact = !compact;
            setSidebarWidth();
        });
        sidebar.add(logo);
        for (int i = 0; i < NAMES.length; i++) {
            final String name = NAMES[i];
            JButton button = MainApp.button(name, MainApp.NAVY, name.equals(active) ? MainApp.GOLD : Color.WHITE);
            button.setIcon(new MenuIcon(i));
            button.setIconTextGap(14);
            button.getAccessibleContext().setAccessibleName(name);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            button.setAlignmentX(CENTER_ALIGNMENT);
            button.setToolTipText(name);
            button.addActionListener(e -> navigate(name));
            nav[i] = button;
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(4));
        }
        sidebar.add(Box.createVerticalGlue());
        sideScroll = new JScrollPane(sidebar);
        sideScroll.setBorder(null);
        sideScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.getVerticalScrollBar().setUnitIncrement(18);
        body.add(sideScroll, BorderLayout.WEST);
        setSidebarWidth();

        // ----- Bar atas untuk jendela sempit -----
        mobileBar.setBackground(MainApp.NAVY);
        mobileBar.setBorder(new EmptyBorder(8, 14, 8, 14));
        mobileBar.add(MainApp.label("PawPaw Pet Care", 18, Color.WHITE, true), BorderLayout.WEST);
        JButton menu = MainApp.button("☰", MainApp.NAVY, Color.WHITE);
        menu.setToolTipText("Buka menu");
        mobileMenu = menu;
        menu.addActionListener(e -> {
            JPopupMenu popup = new JPopupMenu();
            for (String name : NAMES) {
                JMenuItem item = new JMenuItem(name);
                item.addActionListener(a -> navigate(name));
                popup.add(item);
            }
            popup.show(menu, 0, menu.getHeight());
        });
        mobileBar.add(menu, BorderLayout.EAST);

        // ----- Halaman: banner + konten + hak cipta -----
        JPanel page = new PagePanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(MainApp.NAVY);

        hero = new JPanel(new BorderLayout(26, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D q = (Graphics2D) g.create();
                q.setPaint(new GradientPaint(0, 0, MainApp.NAVY, getWidth(), getHeight(), new Color(44, 111, 176)));
                q.fillRect(0, 0, getWidth(), getHeight());
                q.dispose();
            }
        };
        hero.setBorder(new EmptyBorder(22, 30, 22, 30));
        heroPicture = new JLabel(MainApp.image("cuking.png", 120, 128));
        hero.add(heroPicture, BorderLayout.WEST);
        JPanel heroCopy = new JPanel(new BorderLayout(0, 10));
        heroCopy.setOpaque(false);
        heroTitle = MainApp.label(title, 34, Color.WHITE, true);
        heroText = MainApp.paragraph(subtitle, 16, Color.WHITE);
        heroCopy.add(heroTitle, BorderLayout.NORTH);
        heroCopy.add(heroText, BorderLayout.CENTER);
        hero.add(heroCopy, BorderLayout.CENTER);
        hero.setPreferredSize(new Dimension(0, 178));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 178));
        hero.setAlignmentX(LEFT_ALIGNMENT);
        page.add(hero);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(28, 30, 34, 30));
        content.setAlignmentX(LEFT_ALIGNMENT);
        page.add(content);

        below.setLayout(new BoxLayout(below, BoxLayout.Y_AXIS));
        below.setBackground(MainApp.NAVY);
        below.setAlignmentX(LEFT_ALIGNMENT);
        page.add(below);

        JLabel copyright = MainApp.label("© 2026 PawPaw Pet Care. All rights reserved.", 12,
                new Color(152, 173, 201), false);
        copyright.setBorder(new EmptyBorder(14, 30, 18, 30));
        copyright.setAlignmentX(LEFT_ALIGNMENT);
        page.add(copyright);

        mainScroll = new JScrollPane(page);
        mainScroll.setBorder(null);
        mainScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainScroll.getVerticalScrollBar().setUnitIncrement(22);
        body.add(mainScroll, BorderLayout.CENTER);
        mainScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adaptContent(mainScroll.getViewport().getWidth());
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adaptWindow();
            }
        });
    }

    /** Dipanggil setiap lebar area konten berubah; subclass mengatur jumlah kolom, dsb. */
    protected void onWidth(int width) {
    }

    /** Halaman utama menu aktif? Halaman turunan (Detail, Keranjang, Checkout) menimpa ini menjadi false. */
    protected boolean isRoot() {
        return true;
    }

    /** Lebar area konten yang terakhir diketahui. */
    protected int viewportWidth() {
        return viewportWidth;
    }

    private void navigate(String name) {
        boolean sama = name.equals(active) && isRoot();
        switch (name) {
            case "Beranda" -> {
                if (!sama)
                    app.showHome();
            }
            case "Adopsi" -> {
                if (!sama)
                    app.showAdopsi();
            }
            case "Toko" -> {
                if (!sama)
                    app.showToko();
            }
            case "Kesehatan" -> {
                if (!sama)
                    app.showKesehatan();
            }
            case "Logout" -> app.showLogin();
            default -> app.unavailable(name);
        }
    }

    private int activeIndex() {
        for (int i = 0; i < NAMES.length; i++)
            if (NAMES[i].equals(active))
                return i;
        return 0;
    }

    public void resetScroll() {
        if (narrowMode)
            mobileMenu.requestFocusInWindow();
        else
            nav[activeIndex()].requestFocusInWindow();
        mainScroll.getViewport().setViewPosition(new Point(0, 0));
    }

    // ===================== Adaptif =====================

    private void adaptWindow() {
        boolean small = getWidth() < 800;
        sideScroll.setVisible(!small);
        if (small)
            body.add(mobileBar, BorderLayout.NORTH);
        else
            body.remove(mobileBar);
        if (small != narrowMode) {
            narrowMode = small;
            if (small)
                mobileMenu.requestFocusInWindow();
            else
                nav[activeIndex()].requestFocusInWindow();
        }
        body.revalidate();
    }

    private void adaptContent(int width) {
        if (width <= 0)
            return;
        viewportWidth = width;
        heroPicture.setVisible(width >= 650);
        heroTitle.setFont(MainApp.font(width < 600 ? 22 : 34, true));
        MainApp.fit(heroText, width - 60 - (width >= 650 ? 146 : 0));
        onWidth(width);
        content.revalidate();
        hero.revalidate();
    }

    private void setSidebarWidth() {
        sideScroll.setPreferredSize(new Dimension(compact ? 88 : 220, 0));
        int logoSize = compact ? 52 : 104;
        logo.setIcon(MainApp.image("logo.png", logoSize, logoSize));
        logo.setPreferredSize(new Dimension(compact ? 64 : 196, 129));
        logo.setMinimumSize(new Dimension(compact ? 64 : 196, 129));
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 129));
        logo.setToolTipText(compact ? "Perluas sidebar" : "Perkecil sidebar");
        logo.getAccessibleContext().setAccessibleName(logo.getToolTipText());
        for (int i = 0; i < nav.length; i++) {
            nav[i].setText(compact ? "" : NAMES[i]);
            nav[i].setHorizontalAlignment(compact ? SwingConstants.CENTER : SwingConstants.LEFT);
            nav[i].setIconTextGap(compact ? 0 : 14);
            nav[i].setBorder(new EmptyBorder(12, compact ? 8 : 16, 12, compact ? 8 : 16));
            nav[i].setPreferredSize(new Dimension(compact ? 64 : 196, 46));
            nav[i].setMinimumSize(new Dimension(compact ? 64 : 196, 46));
        }
        revalidate();
        repaint();
    }

    /** Panel isi halaman: lebarnya selalu mengikuti viewport, tingginya mengikuti isi. */
    private static final class PagePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 22;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(22, visible.height - 22);
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

    /** Ikon vektor lokal (sama dengan PawPaw-GUI): tidak bergantung pada font emoji atau library tambahan. */
    private static final class MenuIcon implements Icon {
        private final int type;

        MenuIcon(int type) {
            this.type = type;
        }

        @Override
        public int getIconWidth() {
            return 24;
        }

        @Override
        public int getIconHeight() {
            return 24;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D q = (Graphics2D) g.create();
            q.translate(x, y);
            q.setColor(c.getForeground());
            q.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            q.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (type) {
                case 0 -> { // Beranda: rumah.
                    q.drawPolyline(new int[] { 2, 12, 22 }, new int[] { 11, 3, 11 }, 3);
                    q.drawPolyline(new int[] { 5, 5, 19, 19 }, new int[] { 10, 21, 21, 10 }, 4);
                    q.drawRect(10, 14, 4, 7);
                }
                case 1 -> MainApp.paw(q, 0, 0, 24); // Adopsi: telapak kaki hewan.
                case 2 -> { // Toko: tas belanja.
                    q.drawRoundRect(4, 8, 16, 14, 2, 2);
                    q.drawArc(8, 2, 8, 12, 0, 180);
                    q.drawLine(8, 8, 8, 11);
                    q.drawLine(16, 8, 16, 11);
                }
                case 3 -> { // Kesehatan: tanda tambah medis.
                    q.fillRoundRect(9, 2, 6, 20, 2, 2);
                    q.fillRoundRect(2, 9, 20, 6, 2, 2);
                }
                case 4 -> { // Profil: kepala dan bahu.
                    q.fillOval(8, 2, 8, 8);
                    q.fillRoundRect(4, 13, 16, 9, 8, 8);
                }
                case 5 -> { // Logout: pintu dan panah keluar.
                    q.drawPolyline(new int[] { 9, 3, 3, 9 }, new int[] { 3, 3, 21, 21 }, 4);
                    q.drawLine(9, 12, 22, 12);
                    q.drawPolyline(new int[] { 17, 22, 17 }, new int[] { 7, 12, 17 }, 3);
                }
                default -> throw new IllegalStateException("Ikon menu tidak dikenal");
            }
            q.dispose();
        }
    }
}
