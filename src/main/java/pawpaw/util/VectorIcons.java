package pawpaw.util;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

/**
 * Pure Java2D antialiased vector icons replacing all emoji glyphs across the desktop UI.
 */
public final class VectorIcons {

    public enum Kind {
        PAW,
        DASHBOARD,
        SHOP,
        CART,
        HEALTH,
        PROFILE,
        ORDERS,
        SERVICES,
        BOOKINGS,
        ADOPTION,
        SHIELD,
        SWITCH_MODE,
        REFRESH,
        SEARCH,
        PLUS,
        EDIT,
        TRASH,
        LOGOUT,
        CHECK,
        CLOSE,
        IMAGE
    }

    private VectorIcons() {
    }

    public static Icon of(Kind kind, int size, Color color) {
        Objects.requireNonNull(kind, "VectorIcons.Kind tidak boleh null.");
        int safeSize = Math.max(8, size);
        Color safeColor = color != null ? color : Theme.INK;
        return new VectorIcon(kind, safeSize, safeColor);
    }

    public static Icon of(Kind kind, int size) {
        return of(kind, size, Theme.INK);
    }

    private static final class VectorIcon implements Icon {
        private final Kind kind;
        private final int size;
        private final Color color;

        VectorIcon(Kind kind, int size, Color color) {
            this.kind = kind;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double scale = size / 24.0;
                g2.scale(scale, scale);

                // Keep visual stroke weight at ~1.75px at standard 18px-24px icon sizes.
                float strokeWidth = Math.max(1.6f, Math.min(2.15f, (float) (1.75 * 24.0 / Math.max(16, size))));
                g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(color);

                drawKind(g2, kind);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    private static void drawKind(Graphics2D g, Kind kind) {
        switch (kind) {
            case PAW -> drawPaw(g);
            case DASHBOARD -> drawDashboard(g);
            case SHOP -> drawShop(g);
            case CART -> drawCart(g);
            case HEALTH -> drawHealth(g);
            case PROFILE -> drawProfile(g);
            case ORDERS -> drawOrders(g);
            case SERVICES -> drawServices(g);
            case BOOKINGS -> drawBookings(g);
            case ADOPTION -> drawAdoption(g);
            case SHIELD -> drawShield(g);
            case SWITCH_MODE -> drawSwitchMode(g);
            case REFRESH -> drawRefresh(g);
            case SEARCH -> drawSearch(g);
            case PLUS -> drawPlus(g);
            case EDIT -> drawEdit(g);
            case TRASH -> drawTrash(g);
            case LOGOUT -> drawLogout(g);
            case CHECK -> drawCheck(g);
            case CLOSE -> drawClose(g);
            case IMAGE -> drawImage(g);
        }
    }

    private static void drawPaw(Graphics2D g) {
        // Four toes + rounded central pad
        g.fill(new Ellipse2D.Double(4.0, 8.2, 3.6, 4.6));
        g.fill(new Ellipse2D.Double(8.4, 4.2, 3.5, 4.6));
        g.fill(new Ellipse2D.Double(12.3, 4.2, 3.5, 4.6));
        g.fill(new Ellipse2D.Double(16.4, 8.2, 3.6, 4.6));

        Path2D pad = new Path2D.Double();
        pad.moveTo(12.0, 10.6);
        pad.curveTo(15.2, 10.6, 17.6, 13.6, 17.6, 16.5);
        pad.curveTo(17.6, 18.8, 15.6, 19.8, 13.8, 19.2);
        pad.curveTo(12.6, 18.8, 11.4, 18.8, 10.2, 19.2);
        pad.curveTo(8.4, 19.8, 6.4, 18.8, 6.4, 16.5);
        pad.curveTo(6.4, 13.6, 8.8, 10.6, 12.0, 10.6);
        pad.closePath();
        g.fill(pad);
    }

    private static void drawDashboard(Graphics2D g) {
        g.draw(new RoundRectangle2D.Double(3.5, 3.5, 7.0, 7.0, 2.0, 2.0));
        g.draw(new RoundRectangle2D.Double(13.5, 3.5, 7.0, 7.0, 2.0, 2.0));
        g.draw(new RoundRectangle2D.Double(3.5, 13.5, 7.0, 7.0, 2.0, 2.0));
        g.draw(new RoundRectangle2D.Double(13.5, 13.5, 7.0, 7.0, 2.0, 2.0));
    }

    private static void drawShop(Graphics2D g) {
        Path2D bag = new Path2D.Double();
        bag.moveTo(5.0, 8.5);
        bag.lineTo(19.0, 8.5);
        bag.lineTo(20.0, 19.5);
        bag.curveTo(20.1, 20.3, 19.5, 21.0, 18.7, 21.0);
        bag.lineTo(5.3, 21.0);
        bag.curveTo(4.5, 21.0, 3.9, 20.3, 4.0, 19.5);
        bag.closePath();
        g.draw(bag);

        g.draw(new Arc2D.Double(8.5, 3.5, 7.0, 9.5, 0, 180, Arc2D.OPEN));
    }

    private static void drawCart(Graphics2D g) {
        Path2D cart = new Path2D.Double();
        cart.moveTo(2.5, 4.5);
        cart.lineTo(5.5, 4.5);
        cart.lineTo(7.8, 15.0);
        cart.lineTo(18.5, 15.0);
        cart.lineTo(20.5, 7.5);
        cart.lineTo(6.3, 7.5);
        g.draw(cart);

        g.fill(new Ellipse2D.Double(8.0, 17.8, 2.7, 2.7));
        g.fill(new Ellipse2D.Double(16.2, 17.8, 2.7, 2.7));
    }

    private static void drawHealth(Graphics2D g) {
        Path2D cross = new Path2D.Double();
        cross.moveTo(9.5, 4.0);
        cross.lineTo(14.5, 4.0);
        cross.lineTo(14.5, 9.5);
        cross.lineTo(20.0, 9.5);
        cross.lineTo(20.0, 14.5);
        cross.lineTo(14.5, 14.5);
        cross.lineTo(14.5, 20.0);
        cross.lineTo(9.5, 20.0);
        cross.lineTo(9.5, 14.5);
        cross.lineTo(4.0, 14.5);
        cross.lineTo(4.0, 9.5);
        cross.lineTo(9.5, 9.5);
        cross.closePath();
        g.draw(cross);
    }

    private static void drawProfile(Graphics2D g) {
        g.draw(new Ellipse2D.Double(8.2, 4.0, 7.6, 7.6));
        Path2D shoulders = new Path2D.Double();
        shoulders.moveTo(4.5, 20.0);
        shoulders.curveTo(4.5, 15.5, 8.0, 14.0, 12.0, 14.0);
        shoulders.curveTo(16.0, 14.0, 19.5, 15.5, 19.5, 20.0);
        g.draw(shoulders);
    }

    private static void drawOrders(Graphics2D g) {
        Path2D doc = new Path2D.Double();
        doc.moveTo(6.0, 3.5);
        doc.lineTo(15.0, 3.5);
        doc.lineTo(18.5, 7.0);
        doc.lineTo(18.5, 20.5);
        doc.lineTo(6.0, 20.5);
        doc.closePath();
        g.draw(doc);

        g.draw(new Line2D.Double(9.0, 10.5, 15.5, 10.5));
        g.draw(new Line2D.Double(9.0, 14.0, 15.5, 14.0));
        g.draw(new Line2D.Double(9.0, 17.5, 13.0, 17.5));
    }

    private static void drawServices(Graphics2D g) {
        g.draw(new RoundRectangle2D.Double(3.5, 5.0, 17.0, 15.5, 2.5, 2.5));
        g.draw(new Line2D.Double(3.5, 9.8, 20.5, 9.8));
        g.draw(new Line2D.Double(8.0, 3.2, 8.0, 6.4));
        g.draw(new Line2D.Double(16.0, 3.2, 16.0, 6.4));

        g.fill(new Ellipse2D.Double(7.3, 12.6, 2.2, 2.2));
        g.fill(new Ellipse2D.Double(11.0, 12.6, 2.2, 2.2));
        g.fill(new Ellipse2D.Double(14.7, 12.6, 2.2, 2.2));
        g.fill(new Ellipse2D.Double(7.3, 16.0, 2.2, 2.2));
        g.fill(new Ellipse2D.Double(11.0, 16.0, 2.2, 2.2));
    }

    private static void drawBookings(Graphics2D g) {
        g.draw(new RoundRectangle2D.Double(4.5, 4.5, 15.0, 16.5, 2.5, 2.5));
        g.draw(new RoundRectangle2D.Double(8.5, 2.8, 7.0, 3.2, 1.5, 1.5));
        Path2D check = new Path2D.Double();
        check.moveTo(8.3, 13.5);
        check.lineTo(10.8, 16.0);
        check.lineTo(15.8, 10.6);
        g.draw(check);
    }

    private static void drawAdoption(Graphics2D g) {
        Path2D heart = new Path2D.Double();
        heart.moveTo(12.0, 20.0);
        heart.curveTo(5.0, 15.2, 3.3, 11.4, 3.5, 8.2);
        heart.curveTo(3.7, 5.3, 6.0, 3.8, 8.4, 4.0);
        heart.curveTo(10.2, 4.1, 11.3, 5.2, 12.0, 6.4);
        heart.curveTo(12.7, 5.2, 13.8, 4.1, 15.6, 4.0);
        heart.curveTo(18.0, 3.8, 20.3, 5.3, 20.5, 8.2);
        heart.curveTo(20.7, 11.4, 19.0, 15.2, 12.0, 20.0);
        heart.closePath();
        g.draw(heart);
    }

    private static void drawShield(Graphics2D g) {
        Path2D shield = new Path2D.Double();
        shield.moveTo(12.0, 3.3);
        shield.lineTo(19.2, 6.2);
        shield.lineTo(19.2, 11.8);
        shield.curveTo(19.2, 16.4, 15.8, 19.4, 12.0, 20.8);
        shield.curveTo(8.2, 19.4, 4.8, 16.4, 4.8, 11.8);
        shield.lineTo(4.8, 6.2);
        shield.closePath();
        g.draw(shield);

        Path2D innerCheck = new Path2D.Double();
        innerCheck.moveTo(8.8, 12.0);
        innerCheck.lineTo(11.1, 14.3);
        innerCheck.lineTo(15.4, 9.6);
        g.draw(innerCheck);
    }

    private static void drawSwitchMode(Graphics2D g) {
        g.draw(new Line2D.Double(4.5, 8.5, 19.5, 8.5));
        Path2D topArrow = new Path2D.Double();
        topArrow.moveTo(15.8, 5.0);
        topArrow.lineTo(19.5, 8.5);
        topArrow.lineTo(15.8, 12.0);
        g.draw(topArrow);

        g.draw(new Line2D.Double(19.5, 15.5, 4.5, 15.5));
        Path2D bottomArrow = new Path2D.Double();
        bottomArrow.moveTo(8.2, 12.0);
        bottomArrow.lineTo(4.5, 15.5);
        bottomArrow.lineTo(8.2, 19.0);
        g.draw(bottomArrow);
    }

    private static void drawRefresh(Graphics2D g) {
        g.draw(new Arc2D.Double(4.5, 4.5, 15.0, 15.0, 35, 290, Arc2D.OPEN));
        Path2D head = new Path2D.Double();
        head.moveTo(15.5, 4.0);
        head.lineTo(19.5, 6.2);
        head.lineTo(17.5, 10.2);
        g.draw(head);
    }

    private static void drawSearch(Graphics2D g) {
        g.draw(new Ellipse2D.Double(4.2, 4.2, 11.6, 11.6));
        g.draw(new Line2D.Double(14.3, 14.3, 19.8, 19.8));
    }

    private static void drawPlus(Graphics2D g) {
        g.draw(new Line2D.Double(12.0, 4.8, 12.0, 19.2));
        g.draw(new Line2D.Double(4.8, 12.0, 19.2, 12.0));
    }

    private static void drawEdit(Graphics2D g) {
        Path2D pencil = new Path2D.Double();
        pencil.moveTo(15.8, 4.2);
        pencil.lineTo(19.8, 8.2);
        pencil.lineTo(8.5, 19.5);
        pencil.lineTo(4.0, 20.0);
        pencil.lineTo(4.5, 15.5);
        pencil.closePath();
        g.draw(pencil);
        g.draw(new Line2D.Double(13.4, 6.6, 17.4, 10.6));
    }

    private static void drawTrash(Graphics2D g) {
        g.draw(new Line2D.Double(4.5, 6.8, 19.5, 6.8));
        g.draw(new RoundRectangle2D.Double(9.2, 3.6, 5.6, 3.2, 1.2, 1.2));
        Path2D bin = new Path2D.Double();
        bin.moveTo(6.5, 6.8);
        bin.lineTo(7.4, 19.5);
        bin.curveTo(7.5, 20.2, 8.1, 20.6, 8.8, 20.6);
        bin.lineTo(15.2, 20.6);
        bin.curveTo(15.9, 20.6, 16.5, 20.2, 16.6, 19.5);
        bin.lineTo(17.5, 6.8);
        g.draw(bin);
        g.draw(new Line2D.Double(10.3, 10.2, 10.3, 17.0));
        g.draw(new Line2D.Double(13.7, 10.2, 13.7, 17.0));
    }

    private static void drawLogout(Graphics2D g) {
        Path2D door = new Path2D.Double();
        door.moveTo(11.0, 4.2);
        door.lineTo(5.2, 4.2);
        door.lineTo(5.2, 19.8);
        door.lineTo(11.0, 19.8);
        g.draw(door);

        g.draw(new Line2D.Double(9.5, 12.0, 19.5, 12.0));
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(15.8, 8.2);
        arrow.lineTo(19.5, 12.0);
        arrow.lineTo(15.8, 15.8);
        g.draw(arrow);
    }

    private static void drawCheck(Graphics2D g) {
        Path2D check = new Path2D.Double();
        check.moveTo(4.8, 12.6);
        check.lineTo(9.6, 17.4);
        check.lineTo(19.2, 6.8);
        g.draw(check);
    }

    private static void drawClose(Graphics2D g) {
        g.draw(new Line2D.Double(5.8, 5.8, 18.2, 18.2));
        g.draw(new Line2D.Double(18.2, 5.8, 5.8, 18.2));
    }

    private static void drawImage(Graphics2D g) {
        g.draw(new RoundRectangle2D.Double(3.5, 4.5, 17.0, 15.0, 2.5, 2.5));
        g.fill(new Ellipse2D.Double(7.2, 7.8, 2.6, 2.6));
        Path2D mountain = new Path2D.Double();
        mountain.moveTo(4.5, 17.8);
        mountain.lineTo(9.4, 12.8);
        mountain.lineTo(12.6, 15.6);
        mountain.lineTo(15.8, 11.4);
        mountain.lineTo(19.5, 17.8);
        g.draw(mountain);
    }
}
