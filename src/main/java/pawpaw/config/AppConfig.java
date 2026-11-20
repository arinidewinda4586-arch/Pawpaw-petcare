package pawpaw.config;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application-wide constants and runtime storage directory helpers.
 */
public final class AppConfig {

    public static final String APP_NAME = "PawPaw Pet Care";
    public static final BigDecimal SHIPPING_FEE_DELIVERY = new BigDecimal("15000.00");
    public static final String DEFAULT_ANIMAL_IMAGE = "assets/img/meng.png";
    public static final String DEFAULT_PRODUCT_IMAGE = "assets/img/shop/whiskas.png";

    private AppConfig() {
    }

    /**
     * Returns the persistent runtime uploads root directory relative to the working directory.
     */
    public static Path getUploadsRoot() {
        return Paths.get("uploads");
    }

    /**
     * Returns the runtime upload directory for a specific subfolder (e.g. animals, products, avatars).
     */
    public static Path getUploadDir(String subfolder) {
        if (subfolder == null || subfolder.isBlank()) {
            throw new IllegalArgumentException("Subfolder upload tidak boleh kosong.");
        }
        String cleaned = subfolder.trim().replace('\\', '/');
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.isEmpty() || cleaned.contains("..")) {
            throw new IllegalArgumentException("Subfolder upload tidak valid: " + subfolder);
        }
        return getUploadsRoot().resolve(cleaned).normalize();
    }
}
