package pawpaw.gui.common;

import pawpaw.util.ImageStore;
import pawpaw.util.Theme;
import pawpaw.util.VectorIcons;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Rounded or circular image preview component rendering cover-fitted photos or a vector paw fallback,
 * with optional colored ring border support.
 */
public final class PhotoPanel extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final Color PLACEHOLDER_BG = new Color(0xEE, 0xF2, 0xF8);

    private String imagePath;
    private transient BufferedImage sourceImage;
    private transient BufferedImage scaledCache;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private int cornerRadius;
    private boolean circular;
    private Color ringColor;
    private int ringThickness;

    public PhotoPanel() {
        this(140, 140, Theme.CARD_RADIUS);
    }

    public PhotoPanel(int width, int height) {
        this(width, height, Theme.CARD_RADIUS);
    }

    public PhotoPanel(int width, int height, int cornerRadius) {
        this.cornerRadius = Math.max(0, cornerRadius);
        setOpaque(false);
        Dimension size = new Dimension(Math.max(24, width), Math.max(24, height));
        setPreferredSize(size);
        setMinimumSize(new Dimension(24, 24));
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        String clean = (imagePath == null || imagePath.isBlank()) ? null : imagePath.trim();
        this.imagePath = clean;
        this.scaledCache = null;
        this.cachedWidth = -1;
        this.cachedHeight = -1;
        if (clean == null) {
            this.sourceImage = null;
        } else {
            try {
                this.sourceImage = ImageStore.load(clean);
            } catch (RuntimeException ignored) {
                this.sourceImage = null;
            }
        }
        revalidate();
        repaint();
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = Math.max(0, cornerRadius);
        repaint();
    }

    public boolean isCircular() {
        return circular;
    }

    public void setCircular(boolean circular) {
        this.circular = circular;
        repaint();
    }

    public void setRingBorder(Color color, int thickness) {
        this.ringColor = color;
        this.ringThickness = Math.max(0, thickness);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;
        int iw = getWidth() - insets.left - insets.right;
        int ih = getHeight() - insets.top - insets.bottom;
        if (iw <= 1 || ih <= 1) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            boolean hasCustomRing = ringColor != null && ringThickness > 0;
            float strokeWidth = hasCustomRing ? Math.min(ringThickness, Math.min(iw, ih) / 4.0f) : 1.0f;
            double inset = strokeWidth / 2.0;

            Shape clipShape;
            Shape borderShape;
            int drawX;
            int drawY;
            int drawW;
            int drawH;

            if (circular) {
                int diameter = Math.min(iw, ih);
                int cx = x + (iw - diameter) / 2;
                int cy = y + (ih - diameter) / 2;
                double innerSize = Math.max(1.0, diameter - strokeWidth);
                clipShape = new Ellipse2D.Double(cx + inset, cy + inset, innerSize, innerSize);
                borderShape = clipShape;
                drawX = cx;
                drawY = cy;
                drawW = diameter;
                drawH = diameter;
            } else {
                double arc = Math.min(cornerRadius * 2.0, Math.min(iw, ih));
                double innerW = Math.max(1.0, iw - strokeWidth);
                double innerH = Math.max(1.0, ih - strokeWidth);
                clipShape = new RoundRectangle2D.Double(x + inset, y + inset, innerW, innerH, arc, arc);
                borderShape = clipShape;
                drawX = x;
                drawY = y;
                drawW = iw;
                drawH = ih;
            }

            if (sourceImage != null) {
                g2.setColor(Theme.SURFACE_ALT);
                g2.fill(clipShape);

                BufferedImage cover = getOrBuildCover(drawW, drawH);
                Shape prevClip = g2.getClip();
                g2.clip(clipShape);
                g2.drawImage(cover, drawX, drawY, null);
                g2.setClip(prevClip);
            } else {
                g2.setColor(PLACEHOLDER_BG);
                g2.fill(clipShape);

                int iconSize = Math.max(20, Math.min(48, Math.min(drawW, drawH) / 2));
                Icon paw = VectorIcons.of(VectorIcons.Kind.PAW, iconSize, Theme.MUTED);
                int ix = drawX + (drawW - paw.getIconWidth()) / 2;
                int iy = drawY + (drawH - paw.getIconHeight()) / 2;
                paw.paintIcon(this, g2, ix, iy);
            }

            g2.setStroke(new BasicStroke(strokeWidth));
            g2.setColor(hasCustomRing ? ringColor : Theme.BORDER);
            g2.draw(borderShape);
        } finally {
            g2.dispose();
        }
    }

    private BufferedImage getOrBuildCover(int targetW, int targetH) {
        if (scaledCache != null && cachedWidth == targetW && cachedHeight == targetH) {
            return scaledCache;
        }
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double scale = Math.max(
                (double) targetW / Math.max(1, sourceImage.getWidth()),
                (double) targetH / Math.max(1, sourceImage.getHeight())
            );
            int dw = Math.max(1, (int) Math.ceil(sourceImage.getWidth() * scale));
            int dh = Math.max(1, (int) Math.ceil(sourceImage.getHeight() * scale));
            g.drawImage(sourceImage, (targetW - dw) / 2, (targetH - dh) / 2, dw, dh, null);
        } finally {
            g.dispose();
        }
        scaledCache = out;
        cachedWidth = targetW;
        cachedHeight = targetH;
        return out;
    }
}
