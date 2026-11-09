package pawpaw;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Jendela utama Swing: sesi login, navigasi antarhalaman, helper tampilan, dan pemuat gambar. */
public final class MainApp extends JFrame {
    public static final Color NAVY = new Color(15, 40, 84);
    public static final Color BLUE = new Color(28, 77, 141);
    public static final Color GOLD = new Color(255, 201, 60);
    public static final Color INK = new Color(23, 35, 45);
    public static final Color MUTED = new Color(100, 128, 156);
    public static final Color LINE = new Color(214, 228, 242);
    public static final Color PIL = new Color(220, 233, 249);
    public static final Color SKY = new Color(234, 243, 252);
    public static final Color SUCCESS = new Color(22, 128, 60);
    public static final Color SUCCESS_BG = new Color(220, 245, 230);
    public static final Color DANGER = new Color(165, 56, 49);
    public static final Color DANGER_BG = new Color(253, 226, 224);

    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    /** Foto yang ditampilkan bulat (avatar testimoni). */
    private static final java.util.Set<String> ROUND = java.util.Set.of("jesika.png", "puncak-lukisem.png",
            "farras.png", "fakhry.png");
    private final UserSession session = new UserSession();

    public MainApp() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1280, (int) (screen.width * .92)), Math.min(820, (int) (screen.height * .90)));
        setMinimumSize(new Dimension(Math.min(420, screen.width), Math.min(480, screen.height)));
        showLogin();
        setLocationRelativeTo(null);
    }

    public UserSession session() {
        return session;
    }

    // ===================== Navigasi =====================

    public void showLogin() {
        session.logout();
        setTitle("PawPaw Pet Care — Login");
        setContentPane(new LoginView(this));
        revalidate();
        repaint();
    }

    public void completeLogin(String email) {
        session.login(email);
        JOptionPane.showMessageDialog(this, "Selamat datang kembali! Yuk lanjut ke beranda.", "Berhasil Masuk!",
                JOptionPane.INFORMATION_MESSAGE);
        showHome();
    }

    public void showHome() {
        setPage("Beranda", () -> new HomeView(this));
    }

    public void showAdopsi() {
        setPage("Adopsi", () -> new AdoptionPage(this));
    }

    public void showDetail(String[] hewan) {
        setPage("Detail Adopsi", () -> new AdoptionDetailPage(this, hewan));
    }

    public void showToko() {
        setPage("Toko", () -> new TokoPage(this));
    }

    public void showKeranjang() {
        setPage("Keranjang", () -> new KeranjangPage(this));
    }

    public void showCheckout() {
        setPage("Checkout", () -> new CheckoutPage(this));
    }

    public void showKesehatan() {
        setPage("Kesehatan", () -> new KesehatanPage(this));
    }

    /** Semua halaman setelah login lewat sini; tanpa sesi, pengguna dikembalikan ke Login. */
    private void setPage(String title, java.util.function.Supplier<? extends HalamanDasar> factory) {
        if (!session.isLoggedIn()) {
            showLogin();
            return;
        }
        HalamanDasar view = factory.get();
        setTitle("PawPaw Pet Care — " + title);
        setContentPane(view);
        getRootPane().setDefaultButton(null);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(view::resetScroll);
    }

    public void unavailable(String feature) {
        JOptionPane.showMessageDialog(this, "Fitur " + feature + " belum tersedia pada tahap awal.", "Belum tersedia",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ===================== Helper tampilan =====================

    public static Font font(int size, boolean bold) {
        return new Font("Nunito", bold ? Font.BOLD : Font.PLAIN, size);
    }

    public static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(color);
        return label;
    }

    public static JTextArea paragraph(String text, int size, Color color) {
        JTextArea area = new JTextArea(text);
        area.setFont(font(size, false));
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        return area;
    }

    /** Menyesuaikan tinggi paragraf dengan lebar yang tersedia (supaya teks tidak terpotong di BoxLayout). */
    public static void fit(JTextArea area, int width) {
        int w = Math.max(80, width);
        area.setPreferredSize(null);
        area.setMaximumSize(null);
        area.setSize(new Dimension(w, Short.MAX_VALUE));
        int height = area.getPreferredSize().height;
        area.setPreferredSize(new Dimension(w, height));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    public static JButton button(String text, Color background, Color foreground) {
        return button(text, background, foreground, 16, 12, null);
    }

    /** Tombol membulat. Bila outline tidak null, tombol digambar dengan garis tepi. */
    public static JButton button(String text, Color background, Color foreground, int size, int vPad, Color outline) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getBackground();
                if (!isEnabled())
                    g2.setColor(new Color(225, 230, 238));
                else if (getModel().isRollover() || getModel().isPressed())
                    g2.setColor(new Color(Math.max(0, base.getRed() - 10), Math.max(0, base.getGreen() - 10),
                            Math.max(0, base.getBlue() - 10)));
                else
                    g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                if (outline != null && isEnabled()) {
                    g2.setColor(outline);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
                }
                if (isFocusOwner()) {
                    g2.setColor(GOLD);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 20, 20);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(font(size, true));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setRolloverEnabled(true);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(vPad, 16, vPad, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JPanel roundedPanel(Color background) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D q = (Graphics2D) g.create();
                q.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                q.setColor(getBackground());
                q.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                q.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBackground(background);
        return panel;
    }

    /** Label kecil membulat, misalnya status "Tersedia". */
    public static JPanel chip(String text, Color background, Color foreground) {
        JPanel chip = roundedPanel(background);
        chip.setLayout(new BorderLayout());
        chip.setBorder(new EmptyBorder(4, 14, 4, 14));
        chip.add(label(text, 13, foreground, true), BorderLayout.CENTER);
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        chip.setMaximumSize(chip.getPreferredSize());
        return chip;
    }

    /** Kolom isian bergaya sama dengan form login PawPaw-GUI (garis bulat dan petunjuk abu-abu). */
    public static JTextField textField(String hint) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus() && hint != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(font(15, false));
                    g2.setColor(new Color(129, 148, 168));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, getInsets().left, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        styleInput(field);
        return field;
    }

    public static void styleInput(JComponent input) {
        input.setFont(font(15, false));
        input.setForeground(INK);
        if (input instanceof JComboBox)
            input.setBackground(Color.WHITE);
        else
            input.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(LINE, 2, true), new EmptyBorder(10, 12, 10, 12)));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setPreferredSize(new Dimension(200, 48));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
    }

    /** Menambahkan "caption + isian" ke panel BoxLayout vertikal. Mengembalikan caption-nya. */
    public static JLabel addField(JPanel parent, String caption, JComponent input) {
        JLabel label = label(caption, 14, NAVY, true);
        label.setLabelFor(input);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(label);
        parent.add(Box.createVerticalStrut(7));
        parent.add(input);
        parent.add(Box.createVerticalStrut(14));
        return label;
    }

    // ===================== Gambar =====================

    /** Subfolder tempat gambar dicari (relatif terhadap tiap folder dasar). */
    private static final String[] DIRS = { "", "images/", "img/", "assets/img/", "resources/images/",
            "src/pawpaw/img/", "src/img/", "src/assets/img/", "bin/assets/img/", "pawpaw/img/" };

    /** Folder dasar pencarian: folder kerja, folder tempat class berada, dan induk-induknya (akar proyek). */
    private static java.util.List<File> roots() {
        java.util.List<File> roots = new java.util.ArrayList<>();
        java.util.List<File> starts = new java.util.ArrayList<>();
        starts.add(new File(System.getProperty("user.dir")));
        try {
            File loc = new File(MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            starts.add(loc.isDirectory() ? loc : loc.getParentFile());
        } catch (Exception e) {
            // folder kerja saja
        }
        for (File start : starts) {
            File f = start;
            for (int up = 0; up < 3 && f != null; up++, f = f.getParentFile())
                if (!roots.contains(f))
                    roots.add(f);
        }
        return roots;
    }

    private static BufferedImage read(File f) {
        try {
            BufferedImage img = ImageIO.read(f);
            if (img != null)
                return img;
        } catch (IOException e) {
            // coba cara lain di bawah
        }
        // Cadangan: Toolkit bisa membaca beberapa JPEG yang gagal dibaca ImageIO.
        Image raw = new ImageIcon(f.getPath()).getImage();
        if (raw.getWidth(null) > 0 && raw.getHeight(null) > 0) {
            BufferedImage copy = new BufferedImage(raw.getWidth(null), raw.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = copy.createGraphics();
            g.drawImage(raw, 0, 0, null);
            g.dispose();
            return copy;
        }
        return null;
    }

    /**
     * Mencari gambar: (1) classpath, (2) folder-folder proyek, (3) ekstensi lain (.png/.jpg/...),
     * (4) berdasarkan nama file di mana saja dalam folder proyek. Null bila tidak ketemu.
     */
    private static synchronized BufferedImage source(String file) {
        if (IMAGES.containsKey(file))
            return IMAGES.get(file);
        BufferedImage img = null;
        String base = file.contains(".") ? file.substring(0, file.lastIndexOf('.')) : file;
        String[] names = { file, base + ".png", base + ".jpg", base + ".jpeg", base + ".jfif" };
        java.util.List<File> roots = roots();
        search:
        for (String name : names) {
            for (String dir : DIRS) {
                String cp = "/" + (dir.equals("src/pawpaw/img/") ? "img/" : dir) + name;
                URL url = MainApp.class.getResource(cp);
                if (url != null) {
                    try {
                        if ((img = ImageIO.read(url)) != null)
                            break search;
                    } catch (IOException e) {
                        // lanjut ke folder lain
                    }
                }
                for (File root : roots) {
                    File f = new File(root, dir + name);
                    if (f.isFile() && (img = read(f)) != null)
                        break search;
                }
            }
        }
        if (img == null) {
            String plain = file.substring(file.lastIndexOf('/') + 1);
            for (File root : roots) {
                File found = find(root, plain, 0);
                if (found != null && (img = read(found)) != null)
                    break;
            }
        }
        if (img == null)
            System.out.println("[Peringatan] Gambar tidak ditemukan: " + file + " (folder kerja: "
                    + System.getProperty("user.dir") + ")");
        if (img != null) {
            int[] crop = switch (file) {
                case "logo.png" -> new int[] { 409, 160, 576, 563 };
                case "cuking.png" -> new int[] { 396, 332, 2071, 2214 };
                case "meng.png" -> new int[] { 389, 123, 1296, 1717 };
                case "food.png" -> new int[] { 1137, 700, 1879, 2654 };
                default -> null;
            };
            if (crop != null && crop[0] + crop[2] <= img.getWidth() && crop[1] + crop[3] <= img.getHeight())
                img = img.getSubimage(crop[0], crop[1], crop[2], crop[3]);
        }
        IMAGES.put(file, img);
        return img;
    }

    /** Gambar muat-utuh dalam kotak width x height (untuk logo dan maskot). */
    public static ImageIcon image(String file, int width, int height) {
        BufferedImage source = source(file);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (source == null) {
            drawPlaceholder(g, width, height, 0);
        } else {
            if (ROUND.contains(file))
                g.setClip(new Ellipse2D.Double(0, 0, width, height));
            double ratio = Math.min((double) width / source.getWidth(), (double) height / source.getHeight());
            int w = (int) (source.getWidth() * ratio), h = (int) (source.getHeight() * ratio);
            g.drawImage(source, (width - w) / 2, (height - h) / 2, w, h, null);
        }
        g.dispose();
        return new ImageIcon(result);
    }

    /** Foto yang mengikuti ukuran komponen (gaya cover, sudut membulat). Bila file tidak ada, tampil kotak cadangan. */
    public static final class PhotoView extends JComponent {
        private final String file;
        private final int radius;
        private BufferedImage cache;
        private int cacheW, cacheH;

        public PhotoView(String file, int width, int height, int radius) {
            this.file = file;
            this.radius = radius;
            setPreferredSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0)
                return;
            if (cache == null || cacheW != w || cacheH != h) {
                cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D c = cache.createGraphics();
                c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                c.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                BufferedImage src = source(file);
                if (src == null) {
                    drawPlaceholder(c, w, h, radius);
                } else {
                    c.setClip(new RoundRectangle2D.Double(0, 0, w, h, radius, radius));
                    double scale = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
                    int dw = (int) Math.ceil(src.getWidth() * scale), dh = (int) Math.ceil(src.getHeight() * scale);
                    c.drawImage(src, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
                }
                c.dispose();
                cacheW = w;
                cacheH = h;
            }
            g.drawImage(cache, 0, 0, null);
        }
    }

    private static void drawPlaceholder(Graphics2D g, int w, int h, int radius) {
        g.setColor(new Color(205, 224, 245));
        g.fill(new RoundRectangle2D.Double(0, 0, w, h, Math.max(radius, 1), Math.max(radius, 1)));
        double size = Math.min(w, h) * 0.42;
        g.setColor(new Color(120, 152, 192));
        paw(g, (w - size) / 2, (h - size) / 2, size);
    }

    /** Menggambar telapak kaki hewan dalam kotak size x size di posisi (x, y). */
    public static void paw(Graphics2D g, double x, double y, double size) {
        Graphics2D q = (Graphics2D) g.create();
        q.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        q.translate(x, y);
        q.scale(size / 24.0, size / 24.0);
        q.fill(new Ellipse2D.Double(2, 6, 5, 7));
        q.fill(new Ellipse2D.Double(8, 2, 5, 7));
        q.fill(new Ellipse2D.Double(14, 2, 5, 7));
        q.fill(new Ellipse2D.Double(19, 7, 5, 7));
        q.fill(new RoundRectangle2D.Double(6, 12, 13, 10, 13, 10));
        q.dispose();
    }

    private static File find(File folder, String name, int depth) {
        File[] items = folder.listFiles();
        if (items == null)
            return null;
        for (File f : items)
            if (f.isFile() && f.getName().equalsIgnoreCase(name))
                return f;
        if (depth >= 6)
            return null;
        for (File f : items)
            if (f.isDirectory() && !f.getName().startsWith(".")) {
                File hit = find(f, name, depth + 1);
                if (hit != null)
                    return hit;
            }
        return null;
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new MainApp().setVisible(true));
    }
}
