package pawpaw;

import pawpaw.config.AppConfig;
import pawpaw.config.DatabaseConfig;
import pawpaw.model.CartItem;
import pawpaw.model.Product;
import pawpaw.model.SessionState;
import pawpaw.model.User;
import pawpaw.util.FormatUtil;
import pawpaw.util.ImageStore;
import pawpaw.util.PasswordUtil;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Unit tests for configuration, core utilities, and domain models.
 */
public class CoreUtilAndConfigTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "CoreUtilAndConfigTest",
            TestAssert.test("PasswordUtil verifies $2y$ Laravel hashes and handles invalid input safely",
                CoreUtilAndConfigTest::testPasswordUtil),
            TestAssert.test("FormatUtil formats Rupiah, dates, slugs, and status labels",
                CoreUtilAndConfigTest::testFormatUtil),
            TestAssert.test("ImageStore loads resources, handles missing files, and copies uploads cleanly",
                CoreUtilAndConfigTest::testImageStore),
            TestAssert.test("SessionState manages cart quantities, subtotal, and login/logout reset",
                CoreUtilAndConfigTest::testSessionState),
            TestAssert.test("DatabaseConfig reports unreachable error cleanly and connects after reset",
                CoreUtilAndConfigTest::testDatabaseConfig)
        );
    }

    private static void testPasswordUtil() {
        String bcrypt2a = PasswordUtil.hash("rahasia123");
        TestAssert.assertNotNull(bcrypt2a, "Generated bcrypt hash must not be null");
        TestAssert.assertTrue(bcrypt2a.startsWith("$2a$10$"), "Generated hash must use $2a$10$ prefix");

        String laravel2y = "$2y$" + bcrypt2a.substring(4);
        TestAssert.assertTrue(
            PasswordUtil.verify("rahasia123", laravel2y),
            "PasswordUtil.verify must accept $2y$ Laravel hashes"
        );
        TestAssert.assertFalse(
            PasswordUtil.verify("salah123", laravel2y),
            "PasswordUtil.verify must return false for wrong password"
        );
        TestAssert.assertFalse(
            PasswordUtil.verify(null, laravel2y),
            "PasswordUtil.verify must return false for null plaintext"
        );
        TestAssert.assertFalse(
            PasswordUtil.verify("rahasia123", null),
            "PasswordUtil.verify must return false for null hash"
        );
        TestAssert.assertFalse(
            PasswordUtil.verify("rahasia123", ""),
            "PasswordUtil.verify must return false for empty hash"
        );
        TestAssert.assertFalse(
            PasswordUtil.verify("rahasia123", "not-a-bcrypt-hash"),
            "PasswordUtil.verify must return false for malformed hash without throwing"
        );
    }

    private static void testFormatUtil() {
        TestAssert.assertEquals(
            "Rp 125.000",
            FormatUtil.rupiah(new BigDecimal("125000.00")),
            "FormatUtil.rupiah(BigDecimal) should format Indonesian currency"
        );
        TestAssert.assertEquals(
            "Rp 15.000",
            FormatUtil.rupiah(15000L),
            "FormatUtil.rupiah(long) should format Indonesian currency"
        );
        TestAssert.assertEquals(
            "shampoo-kucing-anti-kutu-250-ml",
            FormatUtil.slugify("Shampoo Kucing Anti Kutu 250 ml!"),
            "FormatUtil.slugify should produce clean lowercase hyphenated slug"
        );
        TestAssert.assertFalse(
            FormatUtil.dateId(LocalDate.of(2026, 11, 19)).isBlank(),
            "FormatUtil.dateId should format LocalDate"
        );
        TestAssert.assertFalse(
            FormatUtil.dateTimeId(LocalDateTime.of(2026, 11, 19, 14, 30)).isBlank(),
            "FormatUtil.dateTimeId should format LocalDateTime"
        );
        TestAssert.assertEquals(
            "Tersedia",
            FormatUtil.statusLabel("available"),
            "FormatUtil.statusLabel should translate known statuses"
        );
    }

    private static void testImageStore() throws IOException {
        ImageStore.clearCache();

        BufferedImage whiskas = ImageStore.load("assets/img/shop/whiskas.png");
        TestAssert.assertNotNull(whiskas, "ImageStore should load assets/img/shop/whiskas.png");

        BufferedImage anggora = ImageStore.load("assets/img/anggora.jpg");
        TestAssert.assertNotNull(anggora, "ImageStore should load assets/img/anggora.jpg");

        BufferedImage missing = ImageStore.load("uploads/missing-file-xyz.png");
        TestAssert.assertNull(missing, "ImageStore should return null on missing image without throwing");

        BufferedImage scaledCover = ImageStore.loadScaledCover("assets/img/meng.png", 80, 80);
        TestAssert.assertNotNull(scaledCover, "loadScaledCover should render meng.png");
        TestAssert.assertEquals(80, scaledCover.getWidth(), "Scaled cover width should match");
        TestAssert.assertEquals(80, scaledCover.getHeight(), "Scaled cover height should match");

        BufferedImage scaledFit = ImageStore.loadScaledFit("assets/img/logo.png", 64, 64, true);
        TestAssert.assertNotNull(scaledFit, "loadScaledFit should render logo.png");
        TestAssert.assertEquals(64, scaledFit.getWidth(), "Scaled fit width should match");

        Path tempSource = Files.createTempFile("pawpaw-upload-src-", ".png");
        String relativeUpload = null;
        try {
            BufferedImage sample = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sample.createGraphics();
            g.setColor(Color.ORANGE);
            g.fillRect(0, 0, 24, 24);
            g.dispose();
            ImageIO.write(sample, "png", tempSource.toFile());

            relativeUpload = ImageStore.copyUploadedImage(tempSource.toFile(), "test-tmp", "sample");
            TestAssert.assertTrue(
                relativeUpload.startsWith("uploads/test-tmp/"),
                "copyUploadedImage must return normalized uploads/test-tmp/ path"
            );

            BufferedImage loadedUpload = ImageStore.load(relativeUpload);
            TestAssert.assertNotNull(loadedUpload, "ImageStore should load copied upload image");
            TestAssert.assertEquals(24, loadedUpload.getWidth(), "Uploaded image width should match");
        } finally {
            Files.deleteIfExists(tempSource);
            if (relativeUpload != null) {
                Files.deleteIfExists(Paths.get(relativeUpload));
            }
            Path testTmpDir = AppConfig.getUploadDir("test-tmp");
            if (Files.isDirectory(testTmpDir)) {
                try (var stream = Files.list(testTmpDir)) {
                    if (stream.findAny().isEmpty()) {
                        Files.deleteIfExists(testTmpDir);
                    }
                }
            }
            Path uploadsRoot = AppConfig.getUploadsRoot();
            if (Files.isDirectory(uploadsRoot)) {
                try (var stream = Files.list(uploadsRoot)) {
                    if (stream.findAny().isEmpty()) {
                        Files.deleteIfExists(uploadsRoot);
                    }
                }
            }
            ImageStore.clearCache();
        }
    }

    private static void testSessionState() {
        SessionState session = new SessionState();
        TestAssert.assertFalse(session.isLoggedIn(), "New session should not be logged in");

        User admin = new User();
        admin.setUserId(11);
        admin.setName("Admin PawPaw");
        admin.setRole("admin");
        session.login(admin);

        TestAssert.assertTrue(session.isLoggedIn(), "Session should be logged in after login");
        TestAssert.assertTrue(session.isAdmin(), "Admin user should report isAdmin() == true");
        session.setAdminPreviewAsUser(true);
        TestAssert.assertTrue(session.isAdminPreviewAsUser(), "Admin preview flag should toggle");

        Product p1 = new Product();
        p1.setProductId(1);
        p1.setProductName("Whiskas");
        p1.setPrice(new BigDecimal("65000.00"));

        Product p2 = new Product();
        p2.setProductId(2);
        p2.setProductName("Pedigree");
        p2.setPrice(new BigDecimal("75000.00"));

        session.addToCart(p1, 2);
        session.addToCart(p1, 1);
        session.addToCart(p2, 1);

        TestAssert.assertEquals(4, session.getCartCount(), "Cart count should accumulate quantities");
        TestAssert.assertEquals(
            0,
            new BigDecimal("270000.00").compareTo(session.getCartSubtotal()),
            "Cart subtotal should equal 3*65000 + 1*75000"
        );

        session.updateCartQty(2, 0);
        List<CartItem> items = session.getCart();
        TestAssert.assertEquals(1, items.size(), "Updating qty <= 0 should remove item from cart");
        TestAssert.assertEquals(3, session.getCartCount(), "Remaining cart count should be 3");

        session.login(admin);
        TestAssert.assertEquals(0, session.getCartCount(), "Login must clear existing cart");

        session.addToCart(p1, 1);
        session.logout();
        TestAssert.assertFalse(session.isLoggedIn(), "Logout must clear user");
        TestAssert.assertEquals(0, session.getCartCount(), "Logout must clear cart");
    }

    private static void testDatabaseConfig() {
        try {
            DatabaseConfig.overrideForTest(
                "jdbc:mysql://127.0.0.1:33999/pet_care?connectTimeout=500",
                "root",
                ""
            );
            boolean ok = DatabaseConfig.testConnection();
            TestAssert.assertFalse(ok, "Unreachable MySQL port must return false");
            String err = DatabaseConfig.getLastError();
            TestAssert.assertTrue(
                err != null && !err.isBlank(),
                "DatabaseConfig.getLastError() must be non-blank on failure"
            );
        } finally {
            DatabaseConfig.resetConfig();
        }

        boolean localOk = DatabaseConfig.testConnection();
        TestAssert.assertTrue(
            localOk,
            "Local MySQL pet_care should connect cleanly after resetConfig(): " + DatabaseConfig.getLastError()
        );
    }
}
