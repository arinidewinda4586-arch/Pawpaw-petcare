package pawpaw;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Verifies the restructured project layout, synced SQL schema, resources, fonts,
 * GUI layer boundaries, and libraries.
 */
public class BuildLayoutTest {

    private static final Pattern SQL_IMPORT_PATTERN =
        Pattern.compile("^\\s*import\\s+(?:static\\s+)?java\\.sql\\..*");

    private static final List<String> REQUIRED_FONTS = List.of(
        "Poppins-Regular.ttf",
        "Poppins-Medium.ttf",
        "Poppins-SemiBold.ttf",
        "Poppins-Bold.ttf",
        "OFL.txt"
    );

    private static final List<String> REQUIRED_IMAGES = List.of(
        "logo.png",
        "cuking.png",
        "meng.png",
        "food.png",
        "vet.png",
        "vaksin.png",
        "anggora.jpg",
        "bengal.jpg",
        "goat.jpg",
        "rabbit.jpg",
        "dog.png",
        "kucing.png",
        "kelinci.png",
        "other.png",
        "all.png",
        "shop/whiskas.png",
        "shop/pedigree.png",
        "shop/shampoo-anjing.png",
        "shop/shampoo-kucing.png",
        "shop/sisir-grooming.png",
        "shop/mainan-tulang.png"
    );

    public static void main(String[] args) {
        TestAssert.runSuite(
            "BuildLayoutTest",
            TestAssert.test("db.properties exists with required keys", BuildLayoutTest::testDbProperties),
            TestAssert.test("all required image assets exist", BuildLayoutTest::testRequiredImages),
            TestAssert.test("bundled Poppins fonts exist and are packaged into bin/fonts", BuildLayoutTest::testBundledFontsPackaged),
            TestAssert.test("GUI package has zero java.sql imports", BuildLayoutTest::testZeroSqlImportsInGui),
            TestAssert.test("pet_care.sql has payment table and admin_pawpaw seed", BuildLayoutTest::testSqlSchema),
            TestAssert.test("legacy directories removed and required jars present", BuildLayoutTest::testLegacyRemovedAndJarsPresent)
        );
    }

    private static void testDbProperties() throws IOException {
        Path dbPropsPath = Paths.get("src/main/resources/db.properties");
        TestAssert.assertTrue(Files.isRegularFile(dbPropsPath), "src/main/resources/db.properties must exist");

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(dbPropsPath)) {
            props.load(in);
        }
        TestAssert.assertTrue(props.containsKey("db.url"), "db.properties must contain db.url");
        TestAssert.assertTrue(props.containsKey("db.user"), "db.properties must contain db.user");
        TestAssert.assertTrue(props.containsKey("db.password"), "db.properties must contain db.password");
        TestAssert.assertTrue(
            props.getProperty("db.url", "").contains("jdbc:mysql://"),
            "db.url must be a jdbc:mysql URL"
        );
    }

    private static void testRequiredImages() {
        Path imagesDir = Paths.get("src/main/resources/images");
        TestAssert.assertTrue(Files.isDirectory(imagesDir), "src/main/resources/images directory must exist");
        for (String rel : REQUIRED_IMAGES) {
            Path imgPath = imagesDir.resolve(rel);
            TestAssert.assertTrue(Files.isRegularFile(imgPath), "Required image must exist: " + rel);
        }
    }

    private static void testBundledFontsPackaged() throws IOException {
        Path srcFontsDir = Paths.get("src/main/resources/fonts");
        Path binFontsDir = Paths.get("bin/fonts");
        TestAssert.assertTrue(Files.isDirectory(srcFontsDir), "src/main/resources/fonts directory must exist");
        TestAssert.assertTrue(Files.isDirectory(binFontsDir), "bin/fonts directory must exist after Build-Windows.bat");

        for (String fileName : REQUIRED_FONTS) {
            Path srcFile = srcFontsDir.resolve(fileName);
            TestAssert.assertTrue(Files.isRegularFile(srcFile), "Bundled font asset must exist: " + srcFile);
            long srcSize = Files.size(srcFile);
            TestAssert.assertTrue(srcSize > 0, "Bundled font asset must be non-empty: " + srcFile);

            Path binFile = binFontsDir.resolve(fileName);
            TestAssert.assertTrue(
                Files.isRegularFile(binFile),
                "Packaged font asset must exist in bin/fonts after Build-Windows.bat: " + binFile
            );
            long binSize = Files.size(binFile);
            TestAssert.assertTrue(binSize > 0, "Packaged font asset must be non-empty: " + binFile);
            TestAssert.assertEquals(
                srcSize,
                binSize,
                "Packaged font asset size must match source: " + fileName
            );
            TestAssert.assertNotNull(
                BuildLayoutTest.class.getResource("/fonts/" + fileName),
                "Bundled font resource must be available on classpath: /fonts/" + fileName
            );
        }
    }

    private static void testZeroSqlImportsInGui() throws IOException {
        Path guiDir = Paths.get("src/main/java/pawpaw/gui");
        TestAssert.assertTrue(Files.isDirectory(guiDir), "src/main/java/pawpaw/gui directory must exist");

        List<Path> javaFiles;
        try (Stream<Path> walk = Files.walk(guiDir)) {
            javaFiles = walk
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .sorted()
                .toList();
        }
        TestAssert.assertTrue(!javaFiles.isEmpty(), "src/main/java/pawpaw/gui must contain Java source files");

        for (Path file : javaFiles) {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                TestAssert.assertFalse(
                    SQL_IMPORT_PATTERN.matcher(line).matches(),
                    "GUI class must not import java.sql.*: " + file + ":" + (i + 1) + " -> " + line.trim()
                );
            }
        }
    }

    private static void testSqlSchema() throws IOException {
        Path sqlPath = Paths.get("database/pet_care.sql");
        TestAssert.assertTrue(Files.isRegularFile(sqlPath), "database/pet_care.sql must exist");
        String sql = Files.readString(sqlPath, StandardCharsets.UTF_8);
        TestAssert.assertTrue(
            sql.contains("CREATE TABLE IF NOT EXISTS `payment`"),
            "pet_care.sql must define payment table"
        );
        TestAssert.assertTrue(
            sql.contains("admin_pawpaw"),
            "pet_care.sql must include admin_pawpaw seed user"
        );
    }

    private static void testLegacyRemovedAndJarsPresent() {
        TestAssert.assertFalse(Files.exists(Paths.get("arsip")), "legacy directory arsip/ must be removed");
        TestAssert.assertFalse(Files.exists(Paths.get("resources")), "legacy directory resources/ must be removed");
        TestAssert.assertFalse(Files.exists(Paths.get("src/pawpaw")), "legacy directory src/pawpaw/ must be removed");
        TestAssert.assertFalse(
            Files.exists(Paths.get("lib/mysql-connector-j-26.10.0")),
            "legacy extracted folder lib/mysql-connector-j-26.10.0/ must be removed"
        );
        TestAssert.assertFalse(
            Files.exists(Paths.get("lib/desktop.ini")),
            "lib/desktop.ini must be removed"
        );
        TestAssert.assertTrue(
            Files.isRegularFile(Paths.get("lib/mysql-connector-j-26.10.0.jar")),
            "lib/mysql-connector-j-26.10.0.jar must exist"
        );
        TestAssert.assertTrue(
            Files.isRegularFile(Paths.get("lib/jbcrypt-0.4.jar")),
            "lib/jbcrypt-0.4.jar must exist"
        );
    }
}
