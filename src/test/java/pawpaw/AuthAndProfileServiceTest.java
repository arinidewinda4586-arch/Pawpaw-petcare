package pawpaw;

import pawpaw.config.DatabaseConfig;
import pawpaw.dao.PetDao;
import pawpaw.dao.UserDao;
import pawpaw.model.Pet;
import pawpaw.model.User;
import pawpaw.service.AuthService;
import pawpaw.service.ProfileService;
import pawpaw.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Integration tests for UserDao, PetDao, AuthService, and ProfileService against live MySQL pet_care.
 */
public class AuthAndProfileServiceTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "AuthAndProfileServiceTest",
            TestAssert.test("AuthService.register generates unique lowercase username, hashes password, and validates inputs",
                AuthAndProfileServiceTest::testRegisterAndValidation),
            TestAssert.test("AuthService.login authenticates by email/username and seed admin@pawpaw.com exists",
                AuthAndProfileServiceTest::testLoginAndSeedAdmin),
            TestAssert.test("ProfileService.updateProfile updates user fields and rejects collisions with existing users",
                AuthAndProfileServiceTest::testUpdateProfileAndCollisions),
            TestAssert.test("ProfileService.changePassword validates current password and updates login credential",
                AuthAndProfileServiceTest::testChangePassword),
            TestAssert.test("ProfileService pet CRUD enforces defaults, ownership, and delete guards",
                AuthAndProfileServiceTest::testPetCrudAndOwnership),
            TestAssert.test("ProfileService.loadSummary aggregates counts and latest 5 activities descending",
                AuthAndProfileServiceTest::testLoadSummaryAndActivities)
        );
    }

    private static void testRegisterAndValidation() {
        UserDao userDao = new UserDao();
        AuthService authService = new AuthService(userDao);
        List<Integer> cleanupIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("   ", "valid." + tag + "@example.com", "secret123", "secret123", "0812"),
                "Empty name must fail registration"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("Test", "not-an-email", "secret123", "secret123", "0812"),
                "Invalid email format must fail registration"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("Test", "short." + tag + "@example.com", "12345", "12345", "0812"),
                "Password shorter than 6 characters must fail registration"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("Test", "mismatch." + tag + "@example.com", "secret123", "secret999", "0812"),
                "Mismatched password confirmation must fail registration"
            );

            User first = authService.register(
                "  Budi Santoso  ",
                "  Budi+Paw." + tag + "@Example.COM  ",
                "rahasia123",
                "rahasia123",
                " 081234567890 "
            );
            cleanupIds.add(first.getUserId());

            TestAssert.assertTrue(first.getUserId() > 0, "Registered user must have generated userId");
            TestAssert.assertEquals("Budi Santoso", first.getName(), "Name must be trimmed");
            TestAssert.assertEquals("budi+paw." + tag + "@example.com", first.getEmail(), "Email must be trimmed and lowercased");
            TestAssert.assertEquals("budipaw." + tag, first.getUsername(), "Username must strip [^a-z0-9._-] from local-part");
            TestAssert.assertEquals("user", first.getRole(), "Registered user role must be 'user'");
            TestAssert.assertEquals("081234567890", first.getPhone(), "Phone must be trimmed");
            TestAssert.assertTrue(
                PasswordUtil.verify("rahasia123", first.getPassword()),
                "Password must be stored as verifiable bcrypt hash"
            );

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> authService.register("Budi Duplikat", "BUDI+PAW." + tag + "@example.com", "rahasia123", "rahasia123", ""),
                "Duplicate email must be rejected"
            );

            User second = authService.register(
                "Budi Kedua",
                "budi+paw." + tag + "@other.com",
                "rahasia123",
                "rahasia123",
                null
            );
            cleanupIds.add(second.getUserId());
            TestAssert.assertEquals(
                "budipaw." + tag + "1",
                second.getUsername(),
                "Colliding generated username must append numeric suffix"
            );

            User fallback = authService.register(
                "Fallback User",
                "+++@fallback-" + tag + ".test",
                "rahasia123",
                "rahasia123",
                ""
            );
            cleanupIds.add(fallback.getUserId());
            TestAssert.assertTrue(
                fallback.getUsername().startsWith("user"),
                "Empty sanitized local-part must fallback to 'user' prefix"
            );

            String longLocal = "verylongusername_" + tag + "_abcdefghijklmnopqrstuvwxyz012345";
            User longFirst = authService.register(
                "Long Local",
                longLocal + "@example.com",
                "rahasia123",
                "rahasia123",
                "0815"
            );
            cleanupIds.add(longFirst.getUserId());
            TestAssert.assertEquals(40, longFirst.getUsername().length(), "Generated username must truncate to max 40 chars");

            User longSecond = authService.register(
                "Long Local Two",
                longLocal + "@other.com",
                "rahasia123",
                "rahasia123",
                "0815"
            );
            cleanupIds.add(longSecond.getUserId());
            TestAssert.assertEquals(
                longLocal.substring(0, 35) + "1",
                longSecond.getUsername(),
                "Colliding long username must truncate base to 35 chars before numeric suffix"
            );
        } finally {
            cleanupUsers(userDao, cleanupIds);
        }
    }

    private static void testLoginAndSeedAdmin() {
        UserDao userDao = new UserDao();
        AuthService authService = new AuthService(userDao);
        List<Integer> cleanupIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            User created = authService.register(
                "Login Tester",
                "login." + tag + "@example.com",
                "sandiKu99",
                "sandiKu99",
                "0811112222"
            );
            cleanupIds.add(created.getUserId());

            User byEmail = authService.login("  LOGIN." + tag + "@example.com ", "sandiKu99");
            TestAssert.assertEquals(created.getUserId(), byEmail.getUserId(), "Login by email must return created user");

            User byUsername = authService.login("  " + created.getUsername() + "  ", "sandiKu99");
            TestAssert.assertEquals(created.getUserId(), byUsername.getUserId(), "Login by username must return created user");

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(created.getEmail(), "wrong-pass"),
                "Login with wrong password must throw IllegalArgumentException"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> authService.login("", "sandiKu99"),
                "Login with blank identity must throw IllegalArgumentException"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(created.getEmail(), ""),
                "Login with blank password must throw IllegalArgumentException"
            );

            Optional<User> adminOpt = userDao.findByEmailOrUsername("admin@pawpaw.com");
            TestAssert.assertTrue(adminOpt.isPresent(), "Seed admin@pawpaw.com row must exist in DB");
            TestAssert.assertEquals("admin", adminOpt.get().getRole(), "Seed admin role must be 'admin'");
            TestAssert.assertTrue(adminOpt.get().isAdmin(), "Seed admin must report isAdmin() == true");
            TestAssert.assertTrue(userDao.countCustomers() >= 11, "Seed database must have at least 11 customer users");
        } finally {
            cleanupUsers(userDao, cleanupIds);
        }
    }

    private static void testUpdateProfileAndCollisions() {
        UserDao userDao = new UserDao();
        PetDao petDao = new PetDao();
        AuthService authService = new AuthService(userDao);
        ProfileService profileService = new ProfileService(userDao, petDao);
        List<Integer> cleanupIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            User created = authService.register(
                "Profil Awal",
                "profil." + tag + "@example.com",
                "pass12345",
                "pass12345",
                "081200001111"
            );
            cleanupIds.add(created.getUserId());

            User updated = profileService.updateProfile(
                created.getUserId(),
                "  Profil Baru  ",
                "  profil_baru_" + tag + " ",
                "  Profil.Baru." + tag + "@Example.COM ",
                " 089988776655 ",
                " Jl. Sudirman No. 10 ",
                " Suka hewan peliharaan ",
                "uploads/avatars/avatar-" + tag + ".png"
            );

            TestAssert.assertEquals("Profil Baru", updated.getName(), "Updated name must match");
            TestAssert.assertEquals("profil_baru_" + tag, updated.getUsername(), "Updated username must be lowercased/trimmed");
            TestAssert.assertEquals("profil.baru." + tag + "@example.com", updated.getEmail(), "Updated email must be lowercased/trimmed");
            TestAssert.assertEquals("089988776655", updated.getPhone(), "Updated phone must match");
            TestAssert.assertEquals("Jl. Sudirman No. 10", updated.getAddress(), "Updated address must match");
            TestAssert.assertEquals("Suka hewan peliharaan", updated.getBio(), "Updated bio must match");
            TestAssert.assertEquals("uploads/avatars/avatar-" + tag + ".png", updated.getAvatar(), "Updated avatar path must match");

            // Updating keeping same username and email must succeed (exclude current userId)
            User sameIdentity = profileService.updateProfile(
                created.getUserId(),
                "Profil Tetap",
                updated.getUsername(),
                updated.getEmail(),
                updated.getPhone(),
                updated.getAddress(),
                updated.getBio(),
                null
            );
            TestAssert.assertEquals("Profil Tetap", sameIdentity.getName(), "Self-update keeping email/username must succeed");
            TestAssert.assertEquals("uploads/avatars/avatar-" + tag + ".png", sameIdentity.getAvatar(), "Null avatarPath must preserve existing avatar");

            // Collisions against seed admin must be rejected
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> profileService.updateProfile(
                    created.getUserId(),
                    "Profil Konflik Email",
                    updated.getUsername(),
                    "admin@pawpaw.com",
                    "08123",
                    "Alamat",
                    "Bio",
                    null
                ),
                "Colliding email with admin@pawpaw.com must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> profileService.updateProfile(
                    created.getUserId(),
                    "Profil Konflik Username",
                    "admin_pawpaw",
                    updated.getEmail(),
                    "08123",
                    "Alamat",
                    "Bio",
                    null
                ),
                "Colliding username with admin_pawpaw must be rejected"
            );
        } finally {
            cleanupUsers(userDao, cleanupIds);
        }
    }

    private static void testChangePassword() {
        UserDao userDao = new UserDao();
        PetDao petDao = new PetDao();
        AuthService authService = new AuthService(userDao);
        ProfileService profileService = new ProfileService(userDao, petDao);
        List<Integer> cleanupIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            User created = authService.register(
                "Password Tester",
                "pw." + tag + "@example.com",
                "oldPass123",
                "oldPass123",
                "081233334444"
            );
            cleanupIds.add(created.getUserId());

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(created.getUserId(), "wrongOld", "newPass123", "newPass123"),
                "Wrong current password must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(created.getUserId(), "oldPass123", "123", "123"),
                "New password shorter than 6 characters must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(created.getUserId(), "oldPass123", "newPass123", "different"),
                "Mismatched new password confirmation must be rejected"
            );

            profileService.changePassword(created.getUserId(), "oldPass123", "newPass456", "newPass456");

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(created.getEmail(), "oldPass123"),
                "Old password must no longer authenticate after password change"
            );
            User loggedIn = authService.login(created.getEmail(), "newPass456");
            TestAssert.assertEquals(created.getUserId(), loggedIn.getUserId(), "New password must authenticate cleanly");
        } finally {
            cleanupUsers(userDao, cleanupIds);
        }
    }

    private static void testPetCrudAndOwnership() throws Exception {
        UserDao userDao = new UserDao();
        PetDao petDao = new PetDao();
        AuthService authService = new AuthService(userDao);
        ProfileService profileService = new ProfileService(userDao, petDao);
        List<Integer> cleanupIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            User owner = authService.register("Owner", "owner." + tag + "@example.com", "pass1234", "pass1234", "0812");
            cleanupIds.add(owner.getUserId());
            User outsider = authService.register("Outsider", "outsider." + tag + "@example.com", "pass1234", "pass1234", "0813");
            cleanupIds.add(outsider.getUserId());

            Pet input = new Pet();
            input.setPetName("  Kiko  ");
            input.setSpecies("  Kucing  ");
            input.setBreed("   ");
            input.setGender(null);
            input.setAge("");
            input.setHealthStatus(null);

            Pet created = profileService.savePet(owner.getUserId(), input);
            TestAssert.assertTrue(created.getPetId() > 0, "Saved pet must have generated petId");
            TestAssert.assertEquals("Kiko", created.getPetName(), "Pet name must be trimmed");
            TestAssert.assertEquals("Kucing", created.getSpecies(), "Pet species must be trimmed");
            TestAssert.assertEquals("Campuran", created.getBreed(), "Blank breed must default to 'Campuran'");
            TestAssert.assertEquals("Jantan", created.getGender(), "Null gender must default to 'Jantan'");
            TestAssert.assertEquals("1 Tahun", created.getAge(), "Empty age must default to '1 Tahun'");
            TestAssert.assertEquals("Sehat", created.getHealthStatus(), "Null healthStatus must default to 'Sehat'");

            created.setPetName("Kiko Junior");
            created.setBreed("Persia");
            created.setAge("2 Tahun");
            Pet updated = profileService.savePet(owner.getUserId(), created);
            TestAssert.assertEquals("Kiko Junior", updated.getPetName(), "Updated pet name must persist");
            TestAssert.assertEquals("Persia", updated.getBreed(), "Updated pet breed must persist");
            TestAssert.assertEquals("2 Tahun", updated.getAge(), "Updated pet age must persist");

            try (Connection c = DatabaseConfig.getConnection()) {
                Optional<Pet> foundByName = petDao.findByUserAndName(c, owner.getUserId(), "  kiko junior ");
                TestAssert.assertTrue(foundByName.isPresent(), "PetDao.findByUserAndName must match case-insensitively");
                TestAssert.assertEquals(updated.getPetId(), foundByName.get().getPetId(), "Matched petId must equal updated pet");

                Pet txPet = new Pet();
                txPet.setUserId(owner.getUserId());
                txPet.setPetName("Momo");
                txPet.setSpecies("Kelinci");
                txPet.setBreed("Rex");
                txPet.setGender("Betina");
                txPet.setAge("1 Tahun");
                txPet.setHealthStatus("Sehat");
                Pet insertedInConn = petDao.insert(c, txPet);
                TestAssert.assertTrue(insertedInConn.getPetId() > 0, "PetDao.insert(Connection, Pet) must return generated ID");
                TestAssert.assertTrue(
                    petDao.deleteOwnedByUser(insertedInConn.getPetId(), owner.getUserId()),
                    "deleteOwnedByUser must remove inserted pet"
                );
            }

            TestAssert.assertThrows(
                SecurityException.class,
                () -> profileService.savePet(outsider.getUserId(), updated),
                "Updating another user's pet must throw SecurityException"
            );
            TestAssert.assertThrows(
                SecurityException.class,
                () -> profileService.deletePet(outsider.getUserId(), updated.getPetId()),
                "Deleting another user's pet must throw SecurityException"
            );
            TestAssert.assertEquals(1, profileService.listPets(owner.getUserId()).size(), "Owner pet must remain intact after rejected delete");

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> profileService.deletePet(owner.getUserId(), 9999999),
                "Deleting non-existent pet must throw IllegalArgumentException"
            );

            profileService.deletePet(owner.getUserId(), updated.getPetId());
            TestAssert.assertEquals(0, profileService.listPets(owner.getUserId()).size(), "Pet list must be empty after owner deletion");
        } finally {
            cleanupUsers(userDao, cleanupIds);
        }
    }

    private static void testLoadSummaryAndActivities() throws Exception {
        UserDao userDao = new UserDao();
        PetDao petDao = new PetDao();
        AuthService authService = new AuthService(userDao);
        ProfileService profileService = new ProfileService(userDao, petDao);
        List<Integer> cleanupIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            User user = authService.register("Summary User", "summary." + tag + "@example.com", "pass1234", "pass1234", "0814");
            cleanupIds.add(user.getUserId());

            Pet pet = new Pet();
            pet.setPetName("Boba");
            pet.setSpecies("Kucing");
            Pet savedPet = profileService.savePet(user.getUserId(), pet);

            LocalDateTime base = LocalDateTime.of(2026, 11, 10, 8, 0);
            try (Connection c = DatabaseConfig.getConnection()) {
                insertTestOrder(c, user.getUserId(), "ORD-TST-1-" + tag, 50000, "pending", base.plusHours(1));
                insertTestOrder(c, user.getUserId(), "ORD-TST-2-" + tag, 75000, "completed", base.plusHours(6));
                insertTestBooking(c, user.getUserId(), savedPet.getPetId(), "Boba", "Kucing", "pending", base.plusHours(2));
                insertTestBooking(c, user.getUserId(), savedPet.getPetId(), "Boba", "Kucing", "approved", base.plusHours(5));
                insertTestAdoption(c, user.getUserId(), "pending", base.plusHours(3));
                insertTestAdoption(c, user.getUserId(), "rejected", base.plusHours(4));
            }

            ProfileService.ProfileSummary summary = profileService.loadSummary(user.getUserId());
            TestAssert.assertEquals(user.getUserId(), summary.user().getUserId(), "Summary userId must match");
            TestAssert.assertEquals(1, summary.pets().size(), "Summary pets count must match");
            TestAssert.assertEquals(2, summary.ordersCount(), "Summary ordersCount must be 2");
            TestAssert.assertEquals(2, summary.bookingsCount(), "Summary bookingsCount must be 2");
            TestAssert.assertEquals(2, summary.adoptionsCount(), "Summary adoptionsCount must be 2");
            TestAssert.assertEquals(5, summary.recentActivities().size(), "Recent activities must be capped at 5");

            // Expected descending order: +6h (Pesanan), +5h (Kesehatan), +4h (Adopsi), +3h (Adopsi), +2h (Kesehatan)
            List<ProfileService.ActivityItem> items = summary.recentActivities();
            TestAssert.assertEquals("Pesanan", items.get(0).category(), "Latest activity category must be Pesanan");
            TestAssert.assertEquals("Kesehatan", items.get(1).category(), "2nd latest activity category must be Kesehatan");
            TestAssert.assertEquals("Adopsi", items.get(2).category(), "3rd latest activity category must be Adopsi");
            TestAssert.assertEquals("Adopsi", items.get(3).category(), "4th latest activity category must be Adopsi");
            TestAssert.assertEquals("Kesehatan", items.get(4).category(), "5th latest activity category must be Kesehatan");
            TestAssert.assertTrue(
                !items.get(0).date().isBefore(items.get(1).date())
                    && !items.get(1).date().isBefore(items.get(2).date())
                    && !items.get(2).date().isBefore(items.get(3).date())
                    && !items.get(3).date().isBefore(items.get(4).date()),
                "Recent activities must be sorted descending by date"
            );

            // Seed user #1 sanity check
            ProfileService.ProfileSummary seedSummary = profileService.loadSummary(1);
            TestAssert.assertTrue(seedSummary.ordersCount() >= 1, "Seed user 1 must have orders");
            TestAssert.assertFalse(seedSummary.recentActivities().isEmpty(), "Seed user 1 must have recent activities");
        } finally {
            cleanupUsers(userDao, cleanupIds);
        }
    }

    private static void insertTestOrder(
            Connection c,
            int userId,
            String orderCode,
            long total,
            String status,
            LocalDateTime orderDate) throws Exception {
        String sql = "INSERT INTO orders (order_date, total_price, status, user_id, order_code, recipient_name, recipient_phone, delivery_method) "
            + "VALUES (?, ?, ?, ?, ?, 'Tester', '0812', 'pickup')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(orderDate));
            ps.setLong(2, total);
            ps.setString(3, status);
            ps.setInt(4, userId);
            ps.setString(5, orderCode);
            ps.executeUpdate();
        }
    }

    private static void insertTestBooking(
            Connection c,
            int userId,
            int petId,
            String petName,
            String petSpecies,
            String status,
            LocalDateTime bookingDate) throws Exception {
        String sql = "INSERT INTO health_booking (booking_date, status, user_id, service_id, pet_id, owner_name, pet_name, pet_species) "
            + "VALUES (?, ?, ?, 1, ?, 'Tester', ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(bookingDate));
            ps.setString(2, status);
            ps.setInt(3, userId);
            ps.setInt(4, petId);
            ps.setString(5, petName);
            ps.setString(6, petSpecies);
            ps.executeUpdate();
        }
    }

    private static void insertTestAdoption(
            Connection c,
            int userId,
            String status,
            LocalDateTime requestDate) throws Exception {
        String sql = "INSERT INTO adoption_request (request_date, status, user_id, animal_id, applicant_name, email, phone, address, reason) "
            + "VALUES (?, ?, ?, 1, 'Tester', 'tester@example.com', '0812', 'Alamat Test', 'Alasan Test')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(requestDate));
            ps.setString(2, status);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    private static String uniqueTag() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static void cleanupUsers(UserDao userDao, List<Integer> userIds) {
        for (int id : userIds) {
            try {
                userDao.deleteById(id);
            } catch (Exception ex) {
                System.err.println("[CLEANUP WARN] Failed deleting user_id=" + id + ": " + ex.getMessage());
            }
        }
    }
}
