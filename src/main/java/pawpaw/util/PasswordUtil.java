package pawpaw.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt password hashing and Laravel-compatible ($2y$/$2b$) verification helper.
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * Verifies a plaintext password against a BCrypt hash, normalizing {@code $2y$} and {@code $2b$}
     * prefixes to {@code $2a$} for jBCrypt 0.4 compatibility. Returns {@code false} on null, blank,
     * or malformed hashes without throwing.
     */
    public static boolean verify(String plain, String hash) {
        if (plain == null || hash == null || hash.isBlank()) {
            return false;
        }
        String normalized = hash.trim();
        if (normalized.startsWith("$2y$") || normalized.startsWith("$2b$")) {
            normalized = "$2a$" + normalized.substring(4);
        }
        if (!normalized.startsWith("$2a$") || normalized.length() < 29) {
            return false;
        }
        try {
            return BCrypt.checkpw(plain, normalized);
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            return false;
        }
    }

    /**
     * Hashes a plaintext password using BCrypt with cost factor 10.
     */
    public static String hash(String plain) {
        if (plain == null) {
            throw new IllegalArgumentException("Password tidak boleh null.");
        }
        return BCrypt.hashpw(plain, BCrypt.gensalt(10));
    }
}
