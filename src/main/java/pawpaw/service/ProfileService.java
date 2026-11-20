package pawpaw.service;

import pawpaw.config.DatabaseConfig;
import pawpaw.dao.PetDao;
import pawpaw.dao.UserDao;
import pawpaw.model.Pet;
import pawpaw.model.User;
import pawpaw.util.FormatUtil;
import pawpaw.util.PasswordUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * User profile, security credential, personal activity summary, and pet management service.
 */
public class ProfileService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]+$");
    private static final int ACTIVITY_LIMIT = 5;

    public record ActivityItem(String category, String title, String description, LocalDateTime date) {
    }

    public record ProfileSummary(
        User user,
        List<Pet> pets,
        int ordersCount,
        int bookingsCount,
        int adoptionsCount,
        List<ActivityItem> recentActivities
    ) {
    }

    private final UserDao userDao;
    private final PetDao petDao;

    public ProfileService() {
        this(new UserDao(), new PetDao());
    }

    public ProfileService(UserDao userDao, PetDao petDao) {
        this.userDao = Objects.requireNonNull(userDao, "UserDao tidak boleh null.");
        this.petDao = Objects.requireNonNull(petDao, "PetDao tidak boleh null.");
    }

    public ProfileSummary loadSummary(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID pengguna tidak valid.");
        }
        User user = requireUser(userId);
        List<Pet> pets = petDao.findByUserId(userId);

        try (Connection c = DatabaseConfig.getConnection()) {
            int ordersCount = queryCount(c, "SELECT COUNT(*) FROM orders WHERE user_id = ?", userId);
            int bookingsCount = queryCount(c, "SELECT COUNT(*) FROM health_booking WHERE user_id = ?", userId);
            int adoptionsCount = queryCount(c, "SELECT COUNT(*) FROM adoption_request WHERE user_id = ?", userId);

            List<ActivityItem> combined = new ArrayList<>();
            combined.addAll(loadRecentOrderActivities(c, userId));
            combined.addAll(loadRecentBookingActivities(c, userId));
            combined.addAll(loadRecentAdoptionActivities(c, userId));
            combined.sort(Comparator.comparing(ActivityItem::date, Comparator.nullsLast(Comparator.reverseOrder())));

            List<ActivityItem> topFive = combined.size() > ACTIVITY_LIMIT
                ? List.copyOf(combined.subList(0, ACTIVITY_LIMIT))
                : List.copyOf(combined);

            return new ProfileSummary(
                user,
                List.copyOf(pets),
                ordersCount,
                bookingsCount,
                adoptionsCount,
                topFive
            );
        } catch (SQLException ex) {
            throw new IllegalStateException("Gagal memuat ringkasan profil pengguna: " + ex.getMessage(), ex);
        }
    }

    public User updateProfile(
            int userId,
            String name,
            String username,
            String email,
            String phone,
            String address,
            String bio,
            String avatarPath) {
        User existing = requireUser(userId);

        String cleanName = AuthService.requireNonBlank(
            name,
            "Nama lengkap wajib diisi.",
            150,
            "Nama lengkap maksimal 150 karakter."
        );
        String cleanUsername = AuthService.requireNonBlank(
            username == null ? "" : username.trim().toLowerCase(Locale.ROOT),
            "Username wajib diisi.",
            100,
            "Username maksimal 100 karakter."
        );
        if (!USERNAME_PATTERN.matcher(cleanUsername).matches()) {
            throw new IllegalArgumentException(
                "Username hanya boleh berisi huruf kecil, angka, titik, garis bawah, atau tanda hubung."
            );
        }
        String cleanEmail = AuthService.normalizeAndValidateEmail(email);

        String cleanPhone = phone == null ? "" : phone.trim();
        if (cleanPhone.length() > 30) {
            throw new IllegalArgumentException("Nomor telepon maksimal 30 karakter.");
        }
        if (cleanPhone.isEmpty()) {
            cleanPhone = "-";
        }

        if (userDao.existsByUsername(cleanUsername, userId)) {
            throw new IllegalArgumentException("Username sudah digunakan pengguna lain.");
        }
        if (userDao.existsByEmail(cleanEmail, userId)) {
            throw new IllegalArgumentException("Email sudah terdaftar pada akun lain.");
        }

        String cleanAddress = address == null ? null : address.trim();
        String cleanBio = bio == null ? null : bio.trim();
        String cleanAvatar = (avatarPath == null || avatarPath.isBlank())
            ? existing.getAvatar()
            : avatarPath.trim().replace('\\', '/');
        if (cleanAvatar != null && cleanAvatar.length() > 255) {
            throw new IllegalArgumentException("Path avatar maksimal 255 karakter.");
        }

        existing.setName(cleanName);
        existing.setUsername(cleanUsername);
        existing.setEmail(cleanEmail);
        existing.setPhone(cleanPhone);
        existing.setAddress(cleanAddress);
        existing.setBio(cleanBio);
        existing.setAvatar(cleanAvatar);

        userDao.updateProfile(existing);
        return userDao.findById(userId).orElse(existing);
    }

    public void changePassword(int userId, String currentPassword, String newPassword, String confirmPassword) {
        User existing = requireUser(userId);
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new IllegalArgumentException("Kata sandi saat ini wajib diisi.");
        }
        if (!PasswordUtil.verify(currentPassword, existing.getPassword())) {
            throw new IllegalArgumentException("Kata sandi saat ini tidak sesuai.");
        }
        AuthService.validateNewPassword(
            newPassword,
            confirmPassword,
            "Kata sandi baru minimal 6 karakter.",
            "Konfirmasi kata sandi baru tidak cocok."
        );

        userDao.updatePassword(userId, PasswordUtil.hash(newPassword));
    }

    public List<Pet> listPets(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID pengguna tidak valid.");
        }
        return petDao.findByUserId(userId);
    }

    public Pet savePet(int userId, Pet pet) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID pengguna tidak valid.");
        }
        if (pet == null) {
            throw new IllegalArgumentException("Data hewan peliharaan tidak boleh null.");
        }

        String petName = AuthService.requireNonBlank(
            pet.getPetName(),
            "Nama hewan peliharaan wajib diisi.",
            100,
            "Nama hewan peliharaan maksimal 100 karakter."
        );
        String species = AuthService.requireNonBlank(
            pet.getSpecies(),
            "Jenis hewan peliharaan wajib diisi.",
            50,
            "Jenis hewan peliharaan maksimal 50 karakter."
        );

        Pet target = new Pet();
        target.setPetId(pet.getPetId());
        target.setUserId(userId);
        target.setPetName(petName);
        target.setSpecies(species);
        target.setBreed(trimOrDefault(pet.getBreed(), "Campuran", 100));
        target.setGender(trimOrDefault(pet.getGender(), "Jantan", 20));
        target.setAge(trimOrDefault(pet.getAge(), "1 Tahun", 50));
        target.setHealthStatus(trimOrDefault(pet.getHealthStatus(), "Sehat", 255));

        if (target.getPetId() <= 0) {
            return petDao.insert(target);
        }

        Pet existing = petDao.findById(target.getPetId())
            .orElseThrow(() -> new IllegalArgumentException("Data hewan peliharaan tidak ditemukan."));
        if (existing.getUserId() != userId) {
            throw new SecurityException("Anda tidak memiliki izin mengubah data hewan milik pengguna lain.");
        }

        petDao.update(target);
        return petDao.findById(target.getPetId()).orElse(target);
    }

    public void deletePet(int userId, int petId) {
        if (userId <= 0 || petId <= 0) {
            throw new IllegalArgumentException("ID hewan peliharaan tidak valid.");
        }
        Pet existing = petDao.findById(petId)
            .orElseThrow(() -> new IllegalArgumentException("Data hewan peliharaan tidak ditemukan."));
        if (existing.getUserId() != userId) {
            throw new SecurityException("Anda tidak memiliki izin menghapus data hewan milik pengguna lain.");
        }
        if (!petDao.deleteOwnedByUser(petId, userId)) {
            throw new IllegalStateException("Gagal menghapus data hewan peliharaan.");
        }
    }

    private User requireUser(int userId) {
        return userDao.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));
    }

    private static String trimOrDefault(String value, String fallback, int maxLen) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            return fallback;
        }
        return clean.length() > maxLen ? clean.substring(0, maxLen) : clean;
    }

    private static int queryCount(Connection c, String sql, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static List<ActivityItem> loadRecentOrderActivities(Connection c, int userId) throws SQLException {
        String sql = "SELECT order_id, order_code, total_price, status, order_date, created_at "
            + "FROM orders WHERE user_id = ? "
            + "ORDER BY COALESCE(order_date, created_at) DESC, order_id DESC LIMIT ?";
        List<ActivityItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, ACTIVITY_LIMIT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String orderCode = rs.getString("order_code");
                    String title = "Pesanan " + ((orderCode != null && !orderCode.isBlank())
                        ? orderCode
                        : "#" + rs.getInt("order_id"));
                    BigDecimal total = rs.getBigDecimal("total_price");
                    String statusLabel = FormatUtil.statusLabel(rs.getString("status"));
                    String desc = "Total " + FormatUtil.rupiah(total) + " - Status: " + statusLabel;
                    LocalDateTime date = firstNonNullDate(rs.getTimestamp("order_date"), rs.getTimestamp("created_at"));
                    items.add(new ActivityItem("Pesanan", title, desc, date));
                }
            }
        }
        return items;
    }

    private static List<ActivityItem> loadRecentBookingActivities(Connection c, int userId) throws SQLException {
        String sql = "SELECT hb.booking_date, hb.created_at, hb.pet_name, hb.pet_species, hb.status, hs.service_name "
            + "FROM health_booking hb "
            + "LEFT JOIN health_service hs ON hs.service_id = hb.service_id "
            + "WHERE hb.user_id = ? "
            + "ORDER BY COALESCE(hb.booking_date, hb.created_at) DESC, hb.booking_id DESC LIMIT ?";
        List<ActivityItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, ACTIVITY_LIMIT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String serviceName = rs.getString("service_name");
                    String title = (serviceName != null && !serviceName.isBlank()) ? serviceName : "Janji Kesehatan";
                    String petName = rs.getString("pet_name");
                    String petSpecies = rs.getString("pet_species");
                    String speciesPart = (petSpecies != null && !petSpecies.isBlank()) ? " (" + petSpecies + ")" : "";
                    String statusLabel = FormatUtil.statusLabel(rs.getString("status"));
                    String desc = "Untuk " + (petName != null && !petName.isBlank() ? petName : "-")
                        + speciesPart + " - Status: " + statusLabel;
                    LocalDateTime date = firstNonNullDate(rs.getTimestamp("booking_date"), rs.getTimestamp("created_at"));
                    items.add(new ActivityItem("Kesehatan", title, desc, date));
                }
            }
        }
        return items;
    }

    private static List<ActivityItem> loadRecentAdoptionActivities(Connection c, int userId) throws SQLException {
        String sql = "SELECT ar.request_date, ar.created_at, ar.status, a.nama AS animal_name "
            + "FROM adoption_request ar "
            + "LEFT JOIN animal a ON a.animal_id = ar.animal_id "
            + "WHERE ar.user_id = ? "
            + "ORDER BY COALESCE(ar.request_date, ar.created_at) DESC, ar.adoption_request_id DESC LIMIT ?";
        List<ActivityItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, ACTIVITY_LIMIT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String animalName = rs.getString("animal_name");
                    String title = "Pengajuan Adopsi "
                        + ((animalName != null && !animalName.isBlank()) ? animalName : "Hewan");
                    String desc = "Status: " + FormatUtil.statusLabel(rs.getString("status"));
                    LocalDateTime date = firstNonNullDate(rs.getTimestamp("request_date"), rs.getTimestamp("created_at"));
                    items.add(new ActivityItem("Adopsi", title, desc, date));
                }
            }
        }
        return items;
    }

    private static LocalDateTime firstNonNullDate(Timestamp primary, Timestamp fallback) {
        if (primary != null) {
            return primary.toLocalDateTime();
        }
        return fallback != null ? fallback.toLocalDateTime() : null;
    }
}
