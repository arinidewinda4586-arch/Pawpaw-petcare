import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public class PawPawShop extends JFrame {

    
    static final String LOGO_PATH   = "img/logo.png";      
    static final String HEADER_PATH = "img/cuking.png";    

    // ====== WARNA ======
    static final Color BANNER   = new Color(255, 167, 38);
    static final Color SIDEBAR  = new Color(14, 40, 98);
    static final Color HEAD_L   = new Color(16, 44, 106);
    static final Color HEAD_R   = new Color(40, 104, 170);
    static final Color BG       = new Color(234, 243, 252);
    static final Color NAVY     = new Color(15, 35, 80);
    static final Color BLUE_TXT = new Color(52, 96, 160);
    static final Color CHIP_BG  = new Color(219, 232, 248);
    static final Color CHIP_ON  = new Color(31, 78, 140);
    static final Color CART_BG  = new Color(176, 226, 240);
    static final String FONT    = pickFont();

    static String pickFont() {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String n : names) if (n.equals("Poppins")) return "Poppins";
        return "Segoe UI";
    }

    static Font f(int style, float size) { return new Font(FONT, style, 1).deriveFont(size); }

    static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g2;
    }

    // ====== LOADER GAMBAR ======
    static final Map<String, BufferedImage> CACHE = new HashMap<>();

    static BufferedImage loadImage(String path) {
        if (CACHE.containsKey(path)) return CACHE.get(path);
        BufferedImage img = null;
        try {
            File file = new File(path);
            if (file.exists()) img = ImageIO.read(file);
        } catch (Exception ignored) { }
        CACHE.put(path, img);
        return img;
    }

    // gambar digambar proporsional (tidak gepeng) dan di tengah area
    static boolean drawFit(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
        if (img == null) return false;
        double scale = Math.min((double) w / img.getWidth(), (double) h / img.getHeight());
        int dw = (int) (img.getWidth() * scale), dh = (int) (img.getHeight() * scale);
        g2.drawImage(img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
        return true;
    }

    // ====== DATA PRODUK ======
    static class Product {
        String name, cat, target, imagePath; int price;
        Product(String n, String c, String t, int p, String imagePath) {
            name = n; cat = c; target = t; price = p; this.imagePath = imagePath;
        }
    }

    final List<Product> products = new ArrayList<>();
    final JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
    final Chip[] chips = new Chip[4];
    final PillButton cartBtn = new PillButton("Keranjang", 1, true);
    JTextField search;
    String activeCat = "Semua";
    int cartCount = 0;

    public PawPawShop() {
        super("Shop | PawPaw Pet Care");

        
        products.add(new Product("Whiskas Adult",      "Makanan Kucing", "Untuk Kucing Dewasa", 75000,  "img/shop/whiskas.png"));
        products.add(new Product("Pedigree Adult",     "Makanan Anjing", "Untuk Anjing Dewasa", 100000, "img/shop/pedigree.png"));
        products.add(new Product("Shampoo Anjing",     "Perawatan",      "Untuk Anjing",        90000,  "img/shop/shampoo-anjing.png"));
        products.add(new Product("Shampoo Kucing", "Perawatan", "Untuk Kucing",   85000, "img/shop/shampoo-kucing.png"));
        products.add(new Product("Sisir Grooming",  "Perawatan",         "Untuk Anjing dan kucing",        60000,  "img/shop/sisir-grooming.png"));
        products.add(new Product("Mainan Tulang Karet",  "Mainan",      "Untuk Kucing dan anjing",        35000,  "img/shop/mainan-tulang.png"));

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1357, 726);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildBanner(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.add(new HeaderPanel(), BorderLayout.NORTH);
        main.add(buildContent(), BorderLayout.CENTER);
        JScrollPane sp = new JScrollPane(main);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        add(sp, BorderLayout.CENTER);

        refreshGrid();
    }

    // ====== BANNER ATAS ======
    JComponent buildBanner() {
        JLabel l = new JLabel("\uD83D\uDC3E 50+ Adopsi Setiap Bulan  -  Jadwalkan Vaksin Kapan Saja  -  Semua Item Toko Diskon 25% \uD83D\uDC3E", SwingConstants.CENTER);
        l.setFont(f(Font.BOLD, 10f));
        l.setForeground(new Color(70, 30, 0));
        l.setOpaque(true);
        l.setBackground(BANNER);
        l.setPreferredSize(new Dimension(10, 20));
        return l;
    }

    // ====== SIDEBAR ======
    JComponent buildSidebar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SIDEBAR);
        p.setPreferredSize(new Dimension(147, 10));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(Box.createVerticalStrut(20));
        JComponent logo = new LogoComp();
        logo.setAlignmentX(0.5f);
        top.add(logo);
        top.add(Box.createVerticalStrut(24));
        String[] menu = {"Beranda", "Adopsi", "Toko", "Kesehatan", "Profil", "Login"};
        for (String m : menu) top.add(menuItem(m, m.equals("Toko")));
        p.add(top, BorderLayout.NORTH);

        JLabel out = new JLabel("\u2726 Log Out", SwingConstants.CENTER);
        out.setFont(f(Font.BOLD, 10.5f));
        out.setForeground(Color.WHITE);
        out.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        out.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.add(out, BorderLayout.SOUTH);
        return p;
    }

    JComponent menuItem(String text, boolean active) {
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(255, 255, 255, 45));
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        l.setFont(f(Font.BOLD, 10.5f));
        l.setForeground(active ? new Color(255, 225, 40) : Color.WHITE);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 11, 0, 11));
        wrap.add(l);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        wrap.setPreferredSize(new Dimension(147, 35));
        return wrap;
    }

    // logo: pakai gambar dari LOGO_PATH, kalau tidak ada tampil logo sederhana
    static class LogoComp extends JComponent {
        LogoComp() { setPreferredSize(new Dimension(54, 54)); setMaximumSize(new Dimension(54, 54)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            if (!drawFit(g2, loadImage(LOGO_PATH), 0, 0, 54, 54)) {
                g2.setColor(Color.WHITE); g2.fillOval(0, 0, 53, 53);
                g2.setColor(SIDEBAR); g2.fillOval(4, 4, 45, 45);
                g2.setColor(Color.WHITE); g2.fillOval(6, 6, 41, 41);
                g2.setColor(SIDEBAR); g2.fillOval(9, 9, 35, 35);
                g2.setColor(Color.WHITE);
                g2.setFont(f(Font.BOLD, 8f));
                g2.drawString("PET CARE", 10, 17);
                g2.fillOval(19, 22, 15, 12);
                g2.fillOval(14, 20, 5, 6); g2.fillOval(34, 20, 5, 6);
                g2.fillOval(17, 16, 5, 6); g2.fillOval(31, 16, 5, 6);
            }
            g2.dispose();
        }
    }

    // ====== HEADER BIRU ======
    static class HeaderPanel extends JPanel {
        HeaderPanel() { setPreferredSize(new Dimension(10, 145)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setPaint(new GradientPaint(0, 0, HEAD_L, getWidth(), getHeight(), HEAD_R));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // ilustrasi header dari HEADER_PATH (area 36,8 ukuran 110x130)
            if (!drawFit(g2, loadImage(HEADER_PATH), 36, 8, 110, 130)) {
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(36, 18, 100, 110, 16, 16);
                g2.setColor(new Color(255, 255, 255, 180));
                g2.setFont(f(Font.PLAIN, 9f));
                g2.drawString("header.png", 62, 76);
            }

            g2.setColor(Color.WHITE);
            g2.setFont(f(Font.BOLD, 26f));
            g2.drawString("PAWPAW PET CARE", 144, 62);
            g2.setFont(f(Font.BOLD, 9.5f));
            g2.drawString("Pet Care No. 1 Se-Panam \uD83D\uDC3E", 144, 82);
            g2.drawString("Sahabat Terbaik Hewan Peliharaan Anda \uD83D\uDC3E", 144, 96);
            g2.dispose();
        }
    }

    // ====== KONTEN ======
    JComponent buildContent() {
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(BG);

        JPanel c = new JPanel();
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setPreferredSize(new Dimension(786, 760));

        // judul + tombol
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel k = new JLabel("PAWPAW SHOP");
        Map<TextAttribute, Object> at = new HashMap<>();
        at.put(TextAttribute.TRACKING, 0.15);
        k.setFont(f(Font.BOLD, 8.5f).deriveFont(at));
        k.setForeground(BLUE_TXT);
        JLabel t = new JLabel("Katalog Produk");
        t.setFont(f(Font.BOLD, 28f));
        t.setForeground(NAVY);
        JLabel s = new JLabel("Temukan makanan, mainan, dan kebutuhan perawatan untuk hewan peliharaan.");
        s.setFont(f(Font.PLAIN, 10.5f));
        s.setForeground(BLUE_TXT);
        titles.add(k); titles.add(t); titles.add(Box.createVerticalStrut(2)); titles.add(s);

        PillButton hist = new PillButton("Riwayat Pesanan", 0, false);
        hist.setPreferredSize(new Dimension(137, 34));
        cartBtn.setPreferredSize(new Dimension(122, 34));
        cartBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Isi keranjang: " + cartCount + " item"));
        hist.addActionListener(e -> JOptionPane.showMessageDialog(this, "Belum ada riwayat pesanan."));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        btns.setOpaque(false);
        btns.add(hist); btns.add(cartBtn);
        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setOpaque(false);
        btnWrap.add(btns, BorderLayout.SOUTH);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(titles, BorderLayout.WEST);
        row.add(btnWrap, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(786, 80));
        row.setAlignmentX(0f);
        c.add(row);
        c.add(Box.createVerticalStrut(18));

        // search
        Round sb = new Round(22, Color.WHITE, new Color(200, 215, 235));
        sb.setLayout(new BorderLayout(6, 0));
        sb.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
        JLabel ic = new JLabel("\u26B2");
        ic.setFont(f(Font.BOLD, 12f)); ic.setForeground(NAVY);
        search = new HintField("Cari produk, kategori, atau kebutuhan...");
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshGrid(); }
            public void removeUpdate(DocumentEvent e) { refreshGrid(); }
            public void changedUpdate(DocumentEvent e) { refreshGrid(); }
        });
        JButton x = new JButton("\u00D7") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(new Color(215, 228, 247)); g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(NAVY); g2.setFont(f(Font.BOLD, 11f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("\u00D7", (getWidth() - fm.stringWidth("\u00D7")) / 2, (getHeight() + fm.getAscent()) / 2 - 2);
                g2.dispose();
            }
        };
        x.setPreferredSize(new Dimension(20, 20));
        x.setBorderPainted(false); x.setContentAreaFilled(false); x.setFocusPainted(false);
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.addActionListener(e -> search.setText(""));
        JPanel xw = new JPanel(new GridBagLayout()); xw.setOpaque(false); xw.add(x);
        sb.add(ic, BorderLayout.WEST); sb.add(search, BorderLayout.CENTER); sb.add(xw, BorderLayout.EAST);
        sb.setMaximumSize(new Dimension(413, 34));
        sb.setPreferredSize(new Dimension(413, 34));
        sb.setAlignmentX(0f);
        c.add(sb);
        c.add(Box.createVerticalStrut(11));

        // chips
        String[] cats = {"Semua", "Makanan", "Mainan", "Perawatan"};
        JPanel cp = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        cp.setOpaque(false);
        for (int i = 0; i < cats.length; i++) {
            chips[i] = new Chip(cats[i]);
            final String cat = cats[i];
            chips[i].addActionListener(e -> {
                activeCat = cat;
                for (Chip ch : chips) ch.setSelectedChip(ch.getText().equals(cat));
                refreshGrid();
            });
            cp.add(chips[i]);
        }
        chips[0].setSelectedChip(true);
        cp.setMaximumSize(new Dimension(786, 32));
        cp.setAlignmentX(0f);
        cp.setBorder(BorderFactory.createEmptyBorder(0, -7, 0, 0));
        c.add(cp);
        c.add(Box.createVerticalStrut(18));

        // grid produk
        grid.setOpaque(false);
        JPanel gw = new JPanel(new BorderLayout());
        gw.setOpaque(false);
        gw.add(grid, BorderLayout.NORTH);
        gw.setAlignmentX(0f);
        c.add(gw);

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTH;
        gc.weighty = 1; gc.insets = new Insets(25, 0, 30, 0);
        bg.add(c, gc);
        return bg;
    }

    void refreshGrid() {
        grid.removeAll();
        String q = search == null ? "" : search.getText().trim().toLowerCase();
        for (Product p : products) {
            boolean catOk = activeCat.equals("Semua") || p.cat.contains(activeCat);
            boolean qOk = q.isEmpty() || (p.name + " " + p.cat + " " + p.target).toLowerCase().contains(q);
            if (catOk && qOk) grid.add(card(p));
        }
        grid.revalidate();
        grid.repaint();
    }

    JComponent card(Product p) {
        Round card = new Round(24, Color.WHITE, null);
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(250, 240));
        card.add(new ProductImage(p), BorderLayout.CENTER);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(0, 13, 12, 13));
        JLabel n = new JLabel(p.name); n.setFont(f(Font.BOLD, 12.5f)); n.setForeground(NAVY);
        JLabel c = new JLabel(p.cat); c.setFont(f(Font.PLAIN, 10.5f)); c.setForeground(new Color(30, 60, 130));
        JLabel t = new JLabel(p.target); t.setFont(f(Font.PLAIN, 9.5f)); t.setForeground(new Color(60, 110, 190));
        info.add(n); info.add(Box.createVerticalStrut(4));
        info.add(c); info.add(Box.createVerticalStrut(3));
        info.add(t); info.add(Box.createVerticalStrut(14));

        JLabel price = new JLabel("Rp " + NumberFormat.getInstance(new Locale("id", "ID")).format(p.price));
        price.setFont(f(Font.BOLD, 13f)); price.setForeground(NAVY);
        CartCircle cc = new CartCircle();
        cc.addActionListener(e -> { cartCount++; cartBtn.badge = cartCount; cartBtn.repaint(); });
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(price, BorderLayout.WEST);
        bottom.add(cc, BorderLayout.EAST);
        info.add(bottom);
        card.add(info, BorderLayout.SOUTH);
        return card;
    }

    // ====== KOMPONEN CUSTOM ======
    static class Round extends JPanel {
        int arc; Color bg, border;
        Round(int arc, Color bg, Color border) { this.arc = arc; this.bg = bg; this.border = border; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (border != null) { g2.setColor(border); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class HintField extends JTextField {
        String hint;
        HintField(String hint) {
            this.hint = hint;
            setBorder(null); setOpaque(false);
            setFont(f(Font.PLAIN, 10.5f));
            setForeground(NAVY);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = aa(g);
                g2.setColor(new Color(130, 150, 180));
                g2.setFont(getFont());
                g2.drawString(hint, 2, (getHeight() + g2.getFontMetrics().getAscent()) / 2 - 2);
                g2.dispose();
            }
        }
    }

    static class Chip extends JButton {
        boolean on;
        Chip(String t) {
            super(t);
            setFont(f(Font.BOLD, 10f));
            setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            int w = t.equals("Semua") ? 63 : t.equals("Perawatan") ? 82 : t.equals("Makanan") ? 75 : 66;
            setPreferredSize(new Dimension(w, 28));
        }
        void setSelectedChip(boolean b) { on = b; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(on ? CHIP_ON : CHIP_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setColor(on ? Color.WHITE : new Color(40, 75, 130));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent()) / 2 - 2);
            g2.dispose();
        }
    }

    static void drawCart(Graphics2D g2, int x, int y, Color c) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x, y, x + 3, y);
        g2.drawLine(x + 3, y, x + 5, y + 8);
        g2.drawLine(x + 5, y + 8, x + 13, y + 8);
        g2.drawLine(x + 13, y + 8, x + 14, y + 2);
        g2.drawLine(x + 4, y + 2, x + 14, y + 2);
        g2.fillOval(x + 5, y + 10, 3, 3);
        g2.fillOval(x + 11, y + 10, 3, 3);
    }

    // tipe ikon: 0 = kotak pesanan, 1 = keranjang
    static class PillButton extends JButton {
        int icon; boolean filled; int badge = 0; String text;
        PillButton(String text, int icon, boolean filled) {
            this.text = text; this.icon = icon; this.filled = filled;
            setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(filled ? NAVY : Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            if (!filled) { g2.setColor(NAVY); g2.setStroke(new BasicStroke(1.2f)); g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16); }
            Color fg = filled ? Color.WHITE : NAVY;
            if (icon == 0) {
                g2.setColor(new Color(190, 130, 70)); g2.fillRoundRect(12, 11, 12, 12, 3, 3);
                g2.setColor(new Color(120, 70, 30)); g2.drawRoundRect(12, 11, 12, 12, 3, 3);
            } else drawCart(g2, 13, 10, fg);
            g2.setColor(fg); g2.setFont(f(Font.BOLD, 10f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, icon == 0 ? 29 : 31, (h + fm.getAscent()) / 2 - 2);
            if (filled) {
                g2.setColor(new Color(225, 242, 250));
                g2.fillOval(w - 34, (h - 18) / 2, 20, 18);
                g2.setColor(NAVY); g2.setFont(f(Font.BOLD, 9.5f));
                String b = String.valueOf(badge);
                FontMetrics fb = g2.getFontMetrics();
                g2.drawString(b, w - 34 + (20 - fb.stringWidth(b)) / 2, (h + fb.getAscent()) / 2 - 2);
            }
            g2.dispose();
        }
    }

    static class CartCircle extends JButton {
        CartCircle() {
            setPreferredSize(new Dimension(30, 30));
            setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(CART_BG); g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            drawCart(g2, 8, 8, NAVY);
            g2.dispose();
        }
    }

    // gambar produk: dibaca dari p.imagePath, kalau file tidak ada tampil placeholder
    static class ProductImage extends JComponent {
        Product p;
        ProductImage(Product p) { this.p = p; setPreferredSize(new Dimension(250, 130)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            if (!drawFit(g2, loadImage(p.imagePath), 20, 12, w - 40, h - 14)) {
                g2.setColor(new Color(236, 242, 250));
                g2.fillRoundRect(w / 2 - 45, 22, 90, 100, 14, 14);
                g2.setColor(new Color(150, 170, 200));
                g2.setFont(f(Font.PLAIN, 9f));
                FontMetrics fm = g2.getFontMetrics();
                String s1 = "Gambar tidak ditemukan";
                g2.drawString(s1, (w - fm.stringWidth(s1)) / 2, 66);
                String s2 = p.imagePath;
                g2.drawString(s2, (w - fm.stringWidth(s2)) / 2, 80);
            }
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PawPawShop().setVisible(true));
    }
}