package pawpaw.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatting helpers for Indonesian currency, dates, URL slugs, and database status labels.
 */
public final class FormatUtil {

    private static final Locale ID_LOCALE = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", ID_LOCALE);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", ID_LOCALE);

    private FormatUtil() {
    }

    public static String rupiah(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.setScale(0, RoundingMode.HALF_UP);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(ID_LOCALE);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        if (value.signum() < 0) {
            return "-Rp " + formatter.format(value.abs());
        }
        return "Rp " + formatter.format(value);
    }

    public static String rupiah(long amount) {
        return rupiah(BigDecimal.valueOf(amount));
    }

    public static String dateId(LocalDate d) {
        return d == null ? "-" : DATE_FORMAT.format(d);
    }

    public static String dateTimeId(LocalDateTime dt) {
        return dt == null ? "-" : DATE_TIME_FORMAT.format(dt);
    }

    public static String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        return normalized;
    }

    public static String statusLabel(String dbStatus) {
        if (dbStatus == null || dbStatus.isBlank()) {
            return "-";
        }
        String key = dbStatus.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "available" -> "Tersedia";
            case "adopted" -> "Sudah Diadopsi";
            case "unavailable" -> "Tidak Tersedia";
            case "pending" -> "Menunggu";
            case "approved" -> "Disetujui";
            case "rejected" -> "Ditolak";
            case "cancelled" -> "Dibatalkan";
            case "paid" -> "Dibayar";
            case "processing" -> "Diproses";
            case "shipped" -> "Dikirim";
            case "completed" -> "Selesai";
            case "failed", "gagal" -> "Gagal";
            case "berhasil" -> "Berhasil";
            case "menunggu" -> "Menunggu";
            default -> key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
        };
    }
}
