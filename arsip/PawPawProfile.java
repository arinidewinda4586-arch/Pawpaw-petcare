import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class PawPawProfile extends JFrame {

    // =====================================================
    //  PENGATURAN GAMBAR  --> GANTI PATH & NAMA FILE DI SINI
    // =====================================================
    static final String IMG_CAT_FACE   = "images/cat_face.png";   // ikon kucing di banner kanan atas
    static final String IMG_PET_LUMI   = "images/lumi.png";       // foto Lumi (kucing)
    static final String IMG_PET_MILO   = "images/milo.png";       // foto Milo (anjing)

    // ikon judul section
    static final String ICON_INFO      = "images/icon_info.png";
    static final String ICON_PAW       = "images/icon_paw.png";
    static final String ICON_CHART     = "images/icon_chart.png";
    static final String ICON_CLOCK     = "images/icon_clock.png";
    static final String ICON_SETTING   = "images/icon_setting.png";

    // ikon informasi pribadi
    static final String ICON_NAME      = "images/icon_name.png";
    static final String ICON_USERNAME  = "images/icon_username.png";
    static final String ICON_EMAIL     = "images/icon_email.png";
    static final String ICON_PHONE     = "images/icon_phone.png";
    static final String ICON_ADDRESS   = "images/icon_address.png";
    static final String ICON_DATE      = "images/icon_date.png";

    // ikon ringkasan & aktivitas
    static final String ICON_ORDER     = "images/icon_order.png";
    static final String ICON_HEALTH    = "images/icon_health.png";
    static final String ICON_ADOPT     = "images/icon_adopt.png";

    // ikon pengaturan akun
    static final String ICON_EDIT      = "images/icon_edit.png";
    static final String ICON_PASSWORD  = "images/icon_password.png";
    static final String ICON_NOTIF     = "images/icon_notif.png";
    static final String ICON_PRIVACY   = "images/icon_privacy.png";
    static final String ICON_LOGOUT    = "images/icon_logout.png";

    // ====== WARNA ======
    static final Color NAVY      = new Color(15, 35, 80);
    static final Color BLUE_TXT  = new Color(52, 96, 160);
    static final Color BTN_BLUE  = new Color(31, 78, 140);
    static final Color BANNER_BG = new Color(214, 230, 251);
    static final Color SOFT_BG   = new Color(245, 249, 252);
    static final Color CARD_LINE = new Color(225, 235, 245);
    static final Color GREEN_BG  = new Color(209, 243, 214);
    static final Color GREEN_TXT = new Color(30, 90, 50);
    static final String FONT     = pickFont();

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

    static JLabel lbl(String text, int style, float size, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f(style, size));
        l.setForeground(c);
        return l;
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

    // gambar proporsional di tengah area
    static boolean drawFit(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
        if (img == null) return false;
        double s = Math.min((double) w / img.getWidth(), (double) h / img.getHeight());
        int dw = (int) (img.getWidth() * s), dh = (int) (img.getHeight() * s);
        g2.drawImage(img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
        return true;
    }

    // gambar memenuhi area (di-crop), untuk foto bulat
    static void drawCover(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
        double s = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
        int dw = (int) (img.getWidth() * s), dh = (int) (img.getHeight() * s);
        g2.drawImage(img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
    }

    static void click(JComponent c, Runnable r) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { r.run(); }
        });
    }

    // ====== KOMPONEN CUSTOM ======
    static class Card extends JPanel {
        int arc; Color bg, border; boolean dashed;
        Card(int arc, Color bg, Color border) {
            this.arc = arc; this.bg = bg; this.border = border;
            setOpaque(false);
        }
        @Override public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (border != null) {
                g2.setColor(border);
                g2.setStroke(dashed
                        ? new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f)
                        : new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // gambar/ikon biasa atau foto bulat
    static class Img extends JComponent {
        String path; boolean circle;
        Img(String path, int w, int h, boolean circle) {
            this.path = path; this.circle = circle;
            Dimension d = new Dimension(w, h);
            setPreferredSize(d); setMinimumSize(d); setMaximumSize(d);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            BufferedImage img = loadImage(path);
            if (circle) {
                g2.setColor(new Color(0, 0, 0, 25)); g2.fill(new Ellipse2D.Double(0, 1, w - 1, h - 1));
                g2.setColor(Color.WHITE); g2.fill(new Ellipse2D.Double(0, 0, w - 1, h - 1));
                Shape old = g2.getClip();
                g2.clip(new Ellipse2D.Double(3, 3, w - 7, h - 7));
                if (img != null) drawCover(g2, img, 3, 3, w - 6, h - 6);
                else {
                    g2.setColor(new Color(200, 215, 230)); g2.fillRect(0, 0, w, h);
                    g2.setColor(new Color(110, 130, 160)); g2.setFont(f(Font.PLAIN, 8f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("foto", (w - fm.stringWidth("foto")) / 2, h / 2 + 3);
                }
                g2.setClip(old);
            } else if (!drawFit(g2, img, 0, 0, w, h)) {
                g2.setColor(new Color(190, 200, 215));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 4, 4);
            }
            g2.dispose();
        }
    }

    // kotak kecil berisi ikon
    static class IconBox extends JComponent {
        String path; Color bg; int inset;
        IconBox(String path, int size, Color bg, int inset) {
            this.path = path; this.bg = bg; this.inset = inset;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d); setMinimumSize(d); setMaximumSize(d);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(bg); g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            if (!drawFit(g2, loadImage(path), inset, inset, w - inset * 2, h - inset * 2)) {
                g2.setColor(new Color(190, 200, 215));
                g2.drawRoundRect(inset, inset, w - inset * 2 - 1, h - inset * 2 - 1, 4, 4);
            }
            g2.dispose();
        }
    }

    static class Tag extends JComponent {
        String text; Color bg, fg; Font font;
        Tag(String text, Color bg, Color fg, float size) {
            this.text = text; this.bg = bg; this.fg = fg; this.font = f(Font.BOLD, size);
            FontMetrics fm = getFontMetrics(font);
            Dimension d = new Dimension(fm.stringWidth(text) + 18, 17);
            setPreferredSize(d); setMaximumSize(d); setMinimumSize(d);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(fg); g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent()) / 2 - 2);
            g2.dispose();
        }
    }

    static class Pill extends JButton {
        Pill(String text, int w, int h) {
            super(text);
            setPreferredSize(new Dimension(w, h));
            setMaximumSize(new Dimension(w, h));
            setFont(f(Font.BOLD, 9f));
            setFocusPainted(false); setBorderPainted(false); setContentAreaFilled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(BTN_BLUE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.setColor(Color.WHITE); g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent()) / 2 - 2);
            g2.dispose();
        }
    }

    // avatar inisial + badge pensil
    static class Avatar extends JComponent {
        String