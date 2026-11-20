package pawpaw;

import java.util.Objects;

/**
 * Lightweight zero-dependency test assertion and runner utility.
 */
public final class TestAssert {

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    public static final class NamedTest {
        public final String name;
        public final ThrowingRunnable body;

        public NamedTest(String name, ThrowingRunnable body) {
            this.name = name;
            this.body = body;
        }
    }

    private TestAssert() {
    }

    public static NamedTest test(String name, ThrowingRunnable body) {
        return new NamedTest(name, body);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(
                message + " | expected: <" + expected + "> but was: <" + actual + ">"
            );
        }
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message + " | expected: <true> but was: <false>");
        }
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message + " | expected: <false> but was: <true>");
        }
    }

    public static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message + " | expected non-null value");
        }
    }

    public static void assertNull(Object value, String message) {
        if (value != null) {
            throw new AssertionError(message + " | expected null but was: <" + value + ">");
        }
    }

    public static <T extends Throwable> T assertThrows(
            Class<T> expectedType,
            ThrowingRunnable action,
            String message) {
        try {
            action.run();
        } catch (Throwable thrown) {
            if (expectedType.isInstance(thrown)) {
                return expectedType.cast(thrown);
            }
            throw new AssertionError(
                message + " | expected " + expectedType.getName()
                    + " but threw " + thrown.getClass().getName() + ": " + thrown.getMessage(),
                thrown
            );
        }
        throw new AssertionError(
            message + " | expected " + expectedType.getName() + " to be thrown, but nothing was thrown"
        );
    }

    public static void runSuite(String suiteName, NamedTest... tests) {
        int passed = 0;
        int failed = 0;
        System.out.println("=== Running Suite: " + suiteName + " ===");
        for (NamedTest test : tests) {
            try {
                test.body.run();
                passed++;
                System.out.println("[PASS] " + test.name);
            } catch (Throwable t) {
                failed++;
                System.err.println("[FAIL] " + test.name + " -> " + t.getMessage());
            }
        }
        System.out.println(
            "=== " + suiteName + ": " + passed + " passed, " + failed + " failed (total " + tests.length + ") ==="
        );
        if (failed > 0) {
            System.exit(1);
        }
    }
}
