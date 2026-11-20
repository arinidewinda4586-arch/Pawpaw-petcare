package pawpaw.service;

import pawpaw.dao.UserDao;
import pawpaw.model.User;
import pawpaw.util.PasswordUtil;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Authentication and registration business service.
 */
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    private final UserDao userDao;

    public AuthService() {
        this(new UserDao());
    }

    public AuthService(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "UserDao tidak boleh null.");
    }

    public User login(String identity, String password) {
        String cleanIdentity = identity == null ? "" : identity.trim();
        if (cleanIdentity.isEmpty()) {
            throw new IllegalArgumentException("Email atau username wajib diisi.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Kata sandi wajib diisi.");
        }

        Optional<User> userOpt = userDao.findByEmailOrUsername(cleanIdentity);
        if (userOpt.isEmpty() || !PasswordUtil.verify(password, userOpt.get().getPassword())) {
            throw new IllegalArgumentException("Email/username atau kata sandi tidak sesuai.");
        }
        return userOpt.get();
    }

    public User register(String name, String email, String password, String confirmPassword, String phone) {
        String cleanName = requireNonBlank(name, "Nama lengkap wajib diisi.", 150, "Nama lengkap maksimal 150 karakter.");
        String cleanEmail = normalizeAndValidateEmail(email);
        validateNewPassword(password, confirmPassword, "Kata sandi minimal 6 karakter.", "Kata sandi dan konfirmasi tidak sama.");

        String cleanPhone = phone == null ? "" : phone.trim();
        if (cleanPhone.length() > 30) {
            throw new IllegalArgumentException("Nomor telepon maksimal 30 karakter.");
        }
        if (cleanPhone.isEmpty()) {
            cleanPhone = "Belum diisi";
        }

        if (userDao.existsByEmail(cleanEmail, null)) {
            throw new IllegalArgumentException("Email sudah terdaftar.");
        }

        String username = generateUniqueUsername(cleanEmail);

        User user = new User();
        user.setName(cleanName);
        user.setUsername(username);
        user.setEmail(cleanEmail);
        user.setPassword(PasswordUtil.hash(password));
        user.setRole("user");
        user.setPhone(cleanPhone);
        user.setAddress("Belum diisi");
        user.setBio("Pet lover & proud pet parent");

        return userDao.insert(user);
    }

    static String requireNonBlank(String value, String emptyMsg, int maxLen, String tooLongMsg) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException(emptyMsg);
        }
        if (clean.length() > maxLen) {
            throw new IllegalArgumentException(tooLongMsg);
        }
        return clean;
    }

    static String normalizeAndValidateEmail(String email) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (cleanEmail.isEmpty()) {
            throw new IllegalArgumentException("Email wajib diisi.");
        }
        if (cleanEmail.length() > 150) {
            throw new IllegalArgumentException("Email maksimal 150 karakter.");
        }
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Format email tidak valid.");
        }
        return cleanEmail;
    }

    static void validateNewPassword(
            String password,
            String confirmPassword,
            String shortMsg,
            String mismatchMsg) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(shortMsg);
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException(mismatchMsg);
        }
    }

    private String generateUniqueUsername(String cleanEmail) {
        int atIdx = cleanEmail.indexOf('@');
        String localPart = atIdx > 0 ? cleanEmail.substring(0, atIdx) : "user";
        String baseUsername = localPart.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (baseUsername.isEmpty()) {
            baseUsername = "user";
        }
        if (baseUsername.length() > 40) {
            baseUsername = baseUsername.substring(0, 40);
        }

        String username = baseUsername;
        int counter = 1;
        while (userDao.existsByUsername(username, null)) {
            String prefix = baseUsername.length() > 35 ? baseUsername.substring(0, 35) : baseUsername;
            username = prefix + counter;
            counter++;
        }
        return username;
    }
}
