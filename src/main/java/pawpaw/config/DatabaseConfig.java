package pawpaw.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centralized JDBC configuration and transaction management for MySQL {@code pet_care}.
 */
public final class DatabaseConfig {

    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection c) throws Exception;
    }

    private static final String DEFAULT_URL =
        "jdbc:mysql://127.0.0.1:3306/pet_care?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta&characterEncoding=UTF-8";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static volatile String activeUrl;
    private static volatile String activeUser;
    private static volatile String activePassword;
    private static volatile String lastError = "";

    static {
        resetConfig();
    }

    private DatabaseConfig() {
    }

    public static synchronized void resetConfig() {
        Properties props = new Properties();
        props.setProperty("db.url", DEFAULT_URL);
        props.setProperty("db.user", DEFAULT_USER);
        props.setProperty("db.password", DEFAULT_PASSWORD);

        try (InputStream cpStream = DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (cpStream != null) {
                props.load(cpStream);
            }
        } catch (IOException ignored) {
        }

        Path externalProps = Paths.get("db.properties");
        if (Files.isRegularFile(externalProps)) {
            try (InputStream fileStream = Files.newInputStream(externalProps)) {
                props.load(fileStream);
            } catch (IOException ignored) {
            }
        }

        activeUrl = props.getProperty("db.url", DEFAULT_URL).trim();
        activeUser = props.getProperty("db.user", DEFAULT_USER).trim();
        activePassword = props.getProperty("db.password", DEFAULT_PASSWORD);
        lastError = "";
    }

    public static synchronized void overrideForTest(String url, String user, String pass) {
        activeUrl = url != null ? url : DEFAULT_URL;
        activeUser = user != null ? user : DEFAULT_USER;
        activePassword = pass != null ? pass : "";
        lastError = "";
    }

    public static Connection getConnection() throws SQLException {
        return getConnection(activeUrl, activeUser, activePassword);
    }

    public static Connection getConnection(String url, String user, String pass) throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && conn.isValid(2)) {
                lastError = "";
                return true;
            }
            lastError = "Koneksi database tidak valid.";
            return false;
        } catch (Exception ex) {
            String msg = ex.getMessage();
            lastError = (msg == null || msg.isBlank()) ? ex.getClass().getSimpleName() : msg;
            return false;
        }
    }

    public static String getLastError() {
        return lastError;
    }

    /**
     * Executes {@code work} inside an explicit JDBC transaction, committing on success,
     * rolling back on any failure, and restoring {@code autoCommit} before closing.
     */
    public static <T> T inTransaction(SqlWork<T> work) throws Exception {
        try (Connection conn = getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (Throwable ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                if (ex instanceof Exception e) {
                    throw e;
                }
                if (ex instanceof Error err) {
                    throw err;
                }
                throw new RuntimeException(ex);
            } finally {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
