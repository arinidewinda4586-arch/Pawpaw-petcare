package pawpaw.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

/**
 * Dual-palette design system ({@code app.css} Customer/Auth root tokens and {@link Admin} tokens),
 * bundled OFL {@code Poppins} typography loader, spacing/radius scale, and semantic badge tokens.
 */
public final class Theme {

    // Canvas & Surfaces (app.css Customer / Auth)
    public static final Color BG = new Color(0xEE, 0xF6, 0xFD);
    public static final Color SURFACE = new Color(0xFF, 0xFF, 0xFF);
    public static final Color SURFACE_ALT = new Color(0xF8, 0xFB, 0xFF);
    public static final Color STATUS_BAR_BG = new Color(0xE4, 0xF0, 0xFB);
    public static final Color BORDER = new Color(0xD6, 0xE8, 0xFF);
    public static final Color BORDER_SUBTLE = new Color(0xD9, 0xE7, 0xF4);

    // Brand & Navigation (PawPaw Identity)
    public static final Color NAVY = new Color(0x0F, 0x28, 0x54);
    public static final Color NAVY_HOVER = new Color(0x16, 0x36, 0x6B);
    public static final Color NAVY_DIVIDER = new Color(0x23, 0x41, 0x73);
    public static final Color NAVY_MUTED = new Color(0xB8, 0xC8, 0xE4);
    public static final Color BLUE = new Color(0x1C, 0x4D, 0x8D);
    public static final Color BLUE_HOVER = new Color(0x16, 0x3E, 0x73);
    public static final Color BANNER_BLUE = new Color(0x49, 0x88, 0xC4);
    public static final Color GOLD = new Color(0xFF, 0xC9, 0x3C);
    public static final Color CYAN_BTN = new Color(0xBD, 0xE8, 0xF5);
    public static final Color CYAN_BTN_HOVER = new Color(0xA8, 0xDE, 0xEF);

    // Text, Pills & Selection
    public static final Color INK = new Color(0x0F, 0x28, 0x54);
    public static final Color INK_BODY = new Color(0x15, 0x22, 0x32);
    public static final Color MUTED = new Color(0x49, 0x6A, 0x96);
    public static final Color TEXT_GRAY = new Color(0x4A, 0x55, 0x68);
    public static final Color PILL_IDLE_BG = new Color(0xDC, 0xEA, 0xFA);
    public static final Color PILL_IDLE_FG = new Color(0x52, 0x63, 0x9A);
    public static final Color SELECTION_BG = new Color(0xE0, 0xEC, 0xFF);

    // Auth Backdrop & Input Surfaces
    public static final Color AUTH_CREAM = new Color(0xEE, 0xF7, 0xFF);
    public static final Color AUTH_PEACH = new Color(0xD6, 0xE8, 0xFF);
    public static final Color AUTH_PINK = new Color(0xBD, 0xE8, 0xF5);
    public static final Color AUTH_BORDER = new Color(0xD6, 0xE8, 0xFF);
    public static final Color AUTH_INPUT_BG = new Color(0xF8, 0xFB, 0xFF);

    // Pastel Card Accents (app.css)
    public static final Color PASTEL_PINK = new Color(0xFF, 0xD6, 0xD6);
    public static final Color PASTEL_LIGHT_PINK = new Color(0xFF, 0xE2, 0xE2);
    public static final Color PASTEL_BLUE = new Color(0xD6, 0xE8, 0xFF);
    public static final Color PASTEL_GREEN = new Color(0xD8, 0xF5, 0xD0);
    public static final Color PASTEL_LAVENDER = new Color(0xE8, 0xD9, 0xFF);
    public static final Color PASTEL_ORANGE = new Color(0xFF, 0xED, 0xD5);

    // Semantic Status Pills
    public static final Color SUCCESS_BG = new Color(0xE6, 0xF6, 0xEF);
    public static final Color SUCCESS_FG = new Color(0x14, 0x78, 0x4E);
    public static final Color WARNING_BG = new Color(0xFF, 0xF3, 0xDC);
    public static final Color WARNING_FG = new Color(0xB2, 0x72, 0x00);
    public static final Color INFO_BG = new Color(0xED, 0xF4, 0xFF);
    public static final Color INFO_FG = new Color(0x24, 0x58, 0xB7);
    public static final Color DANGER_BG = new Color(0xFD, 0xE8, 0xE8);
    public static final Color DANGER_FG = new Color(0xC5, 0x3C, 0x38);
    public static final Color NEUTRAL_BG = new Color(0xE2, 0xE8, 0xF0);
    public static final Color NEUTRAL_FG = new Color(0x47, 0x55, 0x69);

    // Solid Interactive Accents
    public static final Color DANGER_SOLID = new Color(0xDC, 0x26, 0x26);
    public static final Color DANGER_HOVER = new Color(0xB9, 0x1C, 0x1C);
    public static final Color SUCCESS_DOT = new Color(0x16, 0xA3, 0x4A);
    public static final Color DANGER_DOT = new Color(0xDC, 0x26, 0x26);

    // Spacing Scale (4, 8, 12, 16, 24)
    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_24 = 24;

    // Layout & Radius Constants
    public static final int SIDEBAR_WIDTH = 220;
    public static final int HEADER_HEIGHT = 56;
    public static final int STATUS_BAR_HEIGHT = 28;
    public static final int RADIUS = 12;
    public static final int INPUT_RADIUS = 16;
    public static final int CARD_RADIUS = 20;
    public static final int AUTH_CARD_RADIUS = 28;
    public static final int PILL_RADIUS = 999;
    public static final int TABLE_ROW_HEIGHT = 34;

    /**
     * Admin workspace palette matching {@code admin.css}.
     */
    public static final class Admin {
        public static final Color NAVY = new Color(0x0F, 0x27, 0x47);
        public static final Color NAVY_HOVER = new Color(0x19, 0x36, 0x5E);
        public static final Color NAVY_TEXT = new Color(0xB8, 0xC7, 0xE3);
        public static final int SIDEBAR_WIDTH = 245;
        public static final int SIDEBAR_COLLAPSED = 84;

        public static final Color BLUE = new Color(0x2F, 0x6F, 0xED);
        public static final Color BLUE_DARK = new Color(0x1F, 0x56, 0xC4);
        public static final Color BLUE_SOFT = new Color(0xDC, 0xEB, 0xFF);

        public static final Color BG = new Color(0xF2, 0xF7, 0xFF);
        public static final Color SURFACE = new Color(0xFF, 0xFF, 0xFF);
        public static final Color SURFACE_ALT = new Color(0xF7, 0xFA, 0xFF);
        public static final Color STATUS_BAR_BG = new Color(0xE8, 0xF0, 0xFC);
        public static final Color LINE = new Color(0xE3, 0xEC, 0xFA);
        public static final Color TEXT = new Color(0x14, 0x21, 0x3D);
        public static final Color TEXT_SOFT = new Color(0x6B, 0x7A, 0x99);

        public static final Color TABLE_HEADER_BG = new Color(0xF1, 0xF6, 0xFF);
        public static final Color TABLE_HEADER_FG = new Color(0x31, 0x49, 0x6B);
        public static final Color ROW_HOVER = new Color(0xF7, 0xFA, 0xFF);
        public static final Color ID_PILL_BG = new Color(0xED, 0xF4, 0xFF);
        public static final Color ID_PILL_FG = new Color(0x24, 0x58, 0xB7);

        public static final Color BTN_LIGHT_BG = new Color(0xED, 0xF4, 0xFF);
        public static final Color BTN_LIGHT_HOVER = new Color(0xDC, 0xEB, 0xFF);
        public static final Color BTN_LIGHT_FG = new Color(0x24, 0x58, 0xB7);

        public static final Color BTN_EDIT_BG = new Color(0xE6, 0xF6, 0xEF);
        public static final Color BTN_EDIT_HOVER = new Color(0xD1, 0xEF, 0xE2);
        public static final Color BTN_EDIT_FG = new Color(0x14, 0x78, 0x4E);

        public static final Color BTN_DELETE_BG = new Color(0xFF, 0xF0, 0xEF);
        public static final Color BTN_DELETE_HOVER = new Color(0xFC, 0xDC, 0xDC);
        public static final Color BTN_DELETE_FG = new Color(0xC5, 0x3C, 0x38);

        public static final int CARD_RADIUS = 18;
        public static final int BUTTON_RADIUS = 12;
        public static final int ICON_BOX_SIZE = 42;

        private Admin() {
        }
    }

    private static final Font BASE_REGULAR;
    private static final Font BASE_MEDIUM;
    private static final Font BASE_SEMIBOLD;
    private static final Font BASE_BOLD;
    private static final boolean POPPINS_LOADED;
    private static final String FONT_FAMILY;

    static {
        Font reg = loadBundledFont("Poppins-Regular.ttf");
        Font med = loadBundledFont("Poppins-Medium.ttf");
        Font semi = loadBundledFont("Poppins-SemiBold.ttf");
        Font bold = loadBundledFont("Poppins-Bold.ttf");

        BASE_REGULAR = reg;
        BASE_MEDIUM = med;
        BASE_SEMIBOLD = semi;
        BASE_BOLD = bold;
        POPPINS_LOADED = reg != null && med != null && semi != null && bold != null;
        FONT_FAMILY = POPPINS_LOADED ? "Poppins" : resolveFallbackFontFamily();
    }

    public record StatusColors(Color bg, Color fg) implements Serializable {
    }

    private Theme() {
    }

    private static Font loadBundledFont(String fileName) {
        Font font = loadFontFromResource("/fonts/" + fileName);
        if (font == null) {
            font = loadFontFromFile(Paths.get("src", "main", "resources", "fonts", fileName));
        }
        if (font != null) {
            try {
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            } catch (RuntimeException ignored) {
                // Direct Font.deriveFont(...) still functions cleanly even if registration is restricted.
            }
        }
        return font;
    }

    private static Font loadFontFromResource(String resourcePath) {
        try (InputStream stream = Theme.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                return Font.createFont(Font.TRUETYPE_FONT, stream);
            }
        } catch (FontFormatException | IOException ignored) {
        }
        return null;
    }

    private static Font loadFontFromFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (InputStream stream = Files.newInputStream(path)) {
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (FontFormatException | IOException ignored) {
        }
        return null;
    }

    private static String resolveFallbackFontFamily() {
        try {
            String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            boolean hasSegoe = Arrays.stream(families).anyMatch(f -> "Segoe UI".equalsIgnoreCase(f));
            return hasSegoe ? "Segoe UI" : Font.SANS_SERIF;
        } catch (Throwable ignored) {
            return Font.SANS_SERIF;
        }
    }

    private static Font deriveOrFallback(Font base, int fallbackStyle, float size) {
        if (POPPINS_LOADED && base != null) {
            return base.deriveFont(size);
        }
        return new Font(FONT_FAMILY, fallbackStyle, Math.round(size));
    }

    public static boolean isPoppinsLoaded() {
        return POPPINS_LOADED;
    }

    public static String fontFamily() {
        return FONT_FAMILY;
    }

    public static Font display() {
        return deriveOrFallback(BASE_BOLD, Font.BOLD, 24f);
    }

    public static Font title() {
        return deriveOrFallback(BASE_BOLD, Font.BOLD, 20f);
    }

    public static Font heading() {
        return deriveOrFallback(BASE_SEMIBOLD, Font.BOLD, 15f);
    }

    public static Font body() {
        return deriveOrFallback(BASE_REGULAR, Font.PLAIN, 13f);
    }

    public static Font bodyMedium() {
        return deriveOrFallback(BASE_MEDIUM, Font.PLAIN, 13f);
    }

    public static Font bodyBold() {
        return deriveOrFallback(BASE_SEMIBOLD, Font.BOLD, 13f);
    }

    public static Font small() {
        return deriveOrFallback(BASE_REGULAR, Font.PLAIN, 12f);
    }

    public static Font smallBold() {
        return deriveOrFallback(BASE_SEMIBOLD, Font.BOLD, 12f);
    }

    public static Font badge() {
        return deriveOrFallback(BASE_SEMIBOLD, Font.BOLD, 11f);
    }

    public static Font kpiValue() {
        return deriveOrFallback(BASE_BOLD, Font.BOLD, 22f);
    }

    public static Font mono() {
        return new Font(Font.MONOSPACED, Font.PLAIN, 12);
    }

    public static Color healthCardColor(String cardColorKey) {
        if (cardColorKey == null || cardColorKey.isBlank()) {
            return PASTEL_BLUE;
        }
        return switch (cardColorKey.trim().toLowerCase(Locale.ROOT)) {
            case "card-green" -> PASTEL_GREEN;
            case "card-blue" -> PASTEL_BLUE;
            case "card-purple" -> PASTEL_LAVENDER;
            case "card-pink" -> PASTEL_PINK;
            case "card-orange" -> PASTEL_ORANGE;
            default -> PASTEL_BLUE;
        };
    }

    public static StatusColors statusColors(String status) {
        if (status == null || status.isBlank()) {
            return new StatusColors(NEUTRAL_BG, NEUTRAL_FG);
        }
        String key = status.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "available", "approved", "completed", "paid", "berhasil", "tersedia",
                "disetujui", "selesai", "dibayar", "sehat" -> new StatusColors(SUCCESS_BG, SUCCESS_FG);
            case "pending", "processing", "menunggu", "diproses" -> new StatusColors(WARNING_BG, WARNING_FG);
            case "shipped", "adopted", "dikirim", "sudah diadopsi", "info", "admin", "user" ->
                new StatusColors(INFO_BG, INFO_FG);
            case "rejected", "cancelled", "unavailable", "failed", "gagal",
                "ditolak", "dibatalkan", "tidak tersedia", "habis" -> new StatusColors(DANGER_BG, DANGER_FG);
            default -> new StatusColors(NEUTRAL_BG, NEUTRAL_FG);
        };
    }

    public static Color tint(Color base, int alpha) {
        if (base == null) {
            return new Color(0xE2, 0xE8, 0xF0, alpha);
        }
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
