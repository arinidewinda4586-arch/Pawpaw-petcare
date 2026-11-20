package pawpaw.service;

import pawpaw.config.DatabaseConfig;
import pawpaw.dao.HealthBookingDao;
import pawpaw.dao.HealthServiceDao;
import pawpaw.dao.PaymentDao;
import pawpaw.dao.PetDao;
import pawpaw.model.HealthBooking;
import pawpaw.model.HealthService;
import pawpaw.model.Payment;
import pawpaw.model.Pet;
import pawpaw.util.FormatUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Business service managing dynamic clinic health services, slot/quota availability,
 * transactional booking creation, auto-pet registration, and payment-synchronized status transitions.
 */
public class HealthBookingService {

    private static final DateTimeFormatter TIME_SLOT_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final BigDecimal MAX_DECIMAL_AMOUNT = new BigDecimal("9999999999.99");
    private static final Set<String> VALID_ADMIN_BOOKING_STATUSES =
        Set.of("pending", "approved", "completed", "rejected");
    private static final String DEFAULT_DOCTOR_NAME = "drh. Clara Wijaya";
    private static final String DEFAULT_PRACTICE_DAYS = "Senin - Sabtu";
    private static final String DEFAULT_TIME_SLOTS = "09:00, 13:00, 16:00";
    private static final String DEFAULT_ICON = "+";
    private static final String DEFAULT_CARD_COLOR = "card-green";
    private static final String USER_CANCEL_REASON = "Dibatalkan oleh pemilik hewan.";
    private static final String DEFAULT_REJECTION_REASON = "Jadwal penuh / tidak tersedia";

    private final HealthServiceDao healthServiceDao;
    private final HealthBookingDao healthBookingDao;
    private final PetDao petDao;
    private final PaymentDao paymentDao;

    public HealthBookingService() {
        this(new HealthServiceDao(), new HealthBookingDao(), new PetDao(), new PaymentDao());
    }

    public HealthBookingService(
            HealthServiceDao healthServiceDao,
            HealthBookingDao healthBookingDao,
            PetDao petDao,
            PaymentDao paymentDao) {
        this.healthServiceDao = Objects.requireNonNull(healthServiceDao, "HealthServiceDao tidak boleh null.");
        this.healthBookingDao = Objects.requireNonNull(healthBookingDao, "HealthBookingDao tidak boleh null.");
        this.petDao = Objects.requireNonNull(petDao, "PetDao tidak boleh null.");
        this.paymentDao = Objects.requireNonNull(paymentDao, "PaymentDao tidak boleh null.");
    }

    public List<HealthService> listServices() {
        return healthServiceDao.findAll();
    }

    public HealthService createService(HealthService s) {
        HealthService normalized = validateAndNormalizeService(
            s,
            "",
            DEFAULT_ICON,
            DEFAULT_CARD_COLOR
        );
        return healthServiceDao.insert(normalized);
    }

    public void updateService(HealthService s) {
        if (s == null || s.getServiceId() <= 0) {
            throw new IllegalArgumentException("ID layanan kesehatan tidak valid.");
        }
        HealthService existing = healthServiceDao.findById(s.getServiceId())
            .orElseThrow(() -> new IllegalArgumentException("Data layanan kesehatan tidak ditemukan."));
        HealthService normalized = validateAndNormalizeService(
            s,
            existing.getDescription(),
            nonBlankOrFallback(existing.getIcon(), DEFAULT_ICON),
            nonBlankOrFallback(existing.getCardColor(), DEFAULT_CARD_COLOR)
        );
        healthServiceDao.update(normalized);
    }

    public void deleteService(int serviceId) {
        if (serviceId <= 0) {
            throw new IllegalArgumentException("ID layanan kesehatan tidak valid.");
        }
        runInTransaction(c -> {
            healthServiceDao.findByIdForUpdate(c, serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Data layanan kesehatan tidak ditemukan."));
            if (healthServiceDao.hasBookings(c, serviceId)) {
                throw new IllegalStateException(
                    "Layanan kesehatan tidak dapat dihapus karena masih memiliki jadwal booking pelanggan."
                );
            }
            healthServiceDao.delete(c, serviceId);
            return null;
        });
    }

    public List<String> parseTimeSlots(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        Set<String> validSlots = new LinkedHashSet<>();
        for (String rawToken : csv.split(",")) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }
            try {
                LocalTime parsed = LocalTime.parse(token, TIME_SLOT_FORMATTER);
                validSlots.add(TIME_SLOT_FORMATTER.format(parsed));
            } catch (DateTimeParseException ignored) {
                // Ignore malformed tokens so dirty DB CSVs never crash slot selection.
            }
        }
        return List.copyOf(validSlots);
    }

    public Map<String, Integer> getRemainingQuotaBySlot(int serviceId, LocalDate date) {
        if (serviceId <= 0) {
            throw new IllegalArgumentException("ID layanan kesehatan tidak valid.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Tanggal booking wajib diisi.");
        }
        HealthService service = healthServiceDao.findById(serviceId)
            .orElseThrow(() -> new IllegalArgumentException("Layanan kesehatan tidak ditemukan."));

        int quota = Math.max(1, service.getDailyQuota());
        List<String> slots = parseTimeSlots(service.getTimeSlots());
        Map<String, Integer> activeBySlot = healthBookingDao.countActiveSlotsForDate(serviceId, date);
        Map<String, Integer> remainingBySlot = new LinkedHashMap<>();
        for (String slot : slots) {
            int used = activeBySlot.getOrDefault(slot, 0);
            remainingBySlot.put(slot, Math.max(0, quota - used));
        }
        return Collections.unmodifiableMap(remainingBySlot);
    }

    public HealthBooking createBooking(
            int userId,
            int serviceId,
            Integer petIdOrNull,
            String ownerName,
            String petName,
            String petSpecies,
            LocalDate date,
            String timeSlot) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID pengguna tidak valid.");
        }
        if (serviceId <= 0) {
            throw new IllegalArgumentException("ID layanan kesehatan tidak valid.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Tanggal booking wajib diisi.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Tanggal booking tidak boleh di masa lalu.");
        }

        LocalTime slotTime = parseStrictTimeSlot(timeSlot);
        String normalizedSlot = TIME_SLOT_FORMATTER.format(slotTime);
        LocalDateTime bookingDateTime = LocalDateTime.of(date, slotTime);

        String cleanOwnerName = AuthService.requireNonBlank(
            ownerName,
            "Nama pemilik hewan wajib diisi.",
            150,
            "Nama pemilik maksimal 150 karakter."
        );

        String cleanPetName;
        String cleanPetSpecies;
        if (petIdOrNull != null) {
            if (petIdOrNull <= 0) {
                throw new IllegalArgumentException("ID hewan peliharaan tidak valid.");
            }
            Pet ownedPet = petDao.findById(petIdOrNull)
                .orElseThrow(() -> new IllegalArgumentException("Data hewan peliharaan tidak ditemukan."));
            if (ownedPet.getUserId() != userId) {
                throw new SecurityException("Anda tidak memiliki izin menggunakan hewan peliharaan milik pengguna lain.");
            }
            cleanPetName = isBlank(petName)
                ? ownedPet.getPetName()
                : AuthService.requireNonBlank(petName, "Nama hewan wajib diisi.", 100, "Nama hewan maksimal 100 karakter.");
            cleanPetSpecies = isBlank(petSpecies)
                ? ownedPet.getSpecies()
                : AuthService.requireNonBlank(petSpecies, "Jenis hewan wajib diisi.", 50, "Jenis hewan maksimal 50 karakter.");
        } else {
            cleanPetName = AuthService.requireNonBlank(
                petName,
                "Nama hewan wajib diisi.",
                100,
                "Nama hewan maksimal 100 karakter."
            );
            cleanPetSpecies = AuthService.requireNonBlank(
                petSpecies,
                "Jenis hewan wajib diisi.",
                50,
                "Jenis hewan maksimal 50 karakter."
            );
        }

        return runInTransaction(c -> {
            HealthService service = healthServiceDao.findByIdForUpdate(c, serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Layanan kesehatan tidak ditemukan."));

            List<String> validServiceSlots = parseTimeSlots(service.getTimeSlots());
            if (!validServiceSlots.contains(normalizedSlot)) {
                throw new IllegalArgumentException(
                    "Jam booking " + normalizedSlot + " tidak tersedia pada jadwal layanan " + service.getServiceName() + "."
                );
            }

            int quota = Math.max(1, service.getDailyQuota());
            int activeCount = healthBookingDao.countActiveInSlot(c, serviceId, bookingDateTime);
            if (activeCount >= quota) {
                throw new IllegalStateException(
                    "Maaf, kuota untuk layanan " + service.getServiceName()
                        + " pada sesi " + normalizedSlot + " sudah penuh (" + quota + " pasien). Silakan pilih jam atau tanggal lain."
                );
            }

            int resolvedPetId = resolveOrCreatePetId(c, userId, petIdOrNull, cleanPetName, cleanPetSpecies);

            HealthBooking booking = new HealthBooking();
            booking.setBookingDate(bookingDateTime);
            booking.setStatus("pending");
            booking.setUserId(userId);
            booking.setServiceId(serviceId);
            booking.setPetId(resolvedPetId);
            booking.setOwnerName(cleanOwnerName);
            booking.setPetName(cleanPetName);
            booking.setPetSpecies(cleanPetSpecies);
            booking.setRejectionReason(null);

            HealthBooking savedBooking = healthBookingDao.insert(c, booking);

            Payment payment = new Payment();
            payment.setBookingId(savedBooking.getBookingId());
            payment.setOrderId(null);
            payment.setAmount(service.getPrice() != null ? service.getPrice() : BigDecimal.ZERO);
            payment.setMethod("TUNAI");
            payment.setStatus("MENUNGGU");
            payment.setPaidAt(null);
            payment.setReferenceNumber("PAY-BKG-" + savedBooking.getBookingId());
            payment.setPaymentMethod("Klinik / COD");
            payment.setPaymentStatus("pending");
            payment.setPaymentDate(LocalDateTime.now());
            paymentDao.insert(c, payment);

            return healthBookingDao.findByIdWithDetails(c, savedBooking.getBookingId()).orElse(savedBooking);
        });
    }

    public List<HealthBooking> listUserBookings(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID pengguna tidak valid.");
        }
        return healthBookingDao.findByUserIdWithDetails(userId);
    }

    public List<HealthBooking> listAllBookings() {
        return healthBookingDao.findAllWithDetails();
    }

    public HealthBooking getBookingCard(int bookingId, int requesterUserId, boolean isAdmin) {
        if (bookingId <= 0) {
            throw new IllegalArgumentException("ID booking kesehatan tidak valid.");
        }
        HealthBooking booking = healthBookingDao.findByIdWithDetails(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Data booking kesehatan tidak ditemukan."));
        if (!isAdmin && booking.getUserId() != requesterUserId) {
            throw new SecurityException("Anda tidak memiliki izin melihat kartu booking milik pengguna lain.");
        }
        return booking;
    }

    public void cancelUserBooking(int userId, int bookingId) {
        if (userId <= 0 || bookingId <= 0) {
            throw new IllegalArgumentException("ID pengguna atau booking kesehatan tidak valid.");
        }

        runInTransaction(c -> {
            HealthBooking booking = healthBookingDao.findByIdForUpdate(c, bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Data booking kesehatan tidak ditemukan."));
            if (booking.getUserId() != userId) {
                throw new SecurityException("Anda tidak memiliki izin membatalkan booking milik pengguna lain.");
            }
            String currentStatus = normalizeToken(booking.getStatus());
            if (!"pending".equals(currentStatus)) {
                throw new IllegalStateException(
                    "Hanya jadwal booking berstatus Menunggu Konfirmasi (pending) yang dapat dibatalkan."
                );
            }

            healthBookingDao.updateStatus(c, bookingId, "cancelled", USER_CANCEL_REASON);
            paymentDao.updateByBookingId(c, bookingId, "GAGAL", "failed", false, false);
            return null;
        });
    }

    public void updateBookingStatus(int bookingId, String targetStatus, String rejectionReason) {
        if (bookingId <= 0) {
            throw new IllegalArgumentException("ID booking kesehatan tidak valid.");
        }
        String cleanTarget = normalizeToken(targetStatus);
        if (!VALID_ADMIN_BOOKING_STATUSES.contains(cleanTarget)) {
            throw new IllegalArgumentException(
                "Status booking harus salah satu dari: pending, approved, completed, rejected."
            );
        }

        if ("pending".equals(cleanTarget) || "approved".equals(cleanTarget)) {
            // Resolve serviceId before opening the transaction so findByIdForUpdate(c, serviceId)
            // is the first statement on the transactional connection (avoids a stale MVCC Read View).
            int preResolvedServiceId = healthBookingDao.findServiceIdByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Data booking kesehatan tidak ditemukan."));
            runInTransaction(c -> {
                activateOrReopenBookingLocked(c, bookingId, preResolvedServiceId, cleanTarget);
                return null;
            });
        } else {
            runInTransaction(c -> {
                HealthBooking booking = lockAndCheckNonTerminalBooking(c, bookingId);
                if ("completed".equals(cleanTarget)) {
                    healthBookingDao.updateStatus(c, booking.getBookingId(), "completed", null);
                    paymentDao.updateByBookingId(c, booking.getBookingId(), "BERHASIL", "paid", true, false);
                } else {
                    String cleanReason = isBlank(rejectionReason) ? DEFAULT_REJECTION_REASON : rejectionReason.trim();
                    healthBookingDao.updateStatus(c, booking.getBookingId(), "rejected", cleanReason);
                    paymentDao.updateByBookingId(c, booking.getBookingId(), "GAGAL", "failed", false, false);
                }
                return null;
            });
        }
    }

    public void deleteBooking(int bookingId) {
        if (bookingId <= 0) {
            throw new IllegalArgumentException("ID booking kesehatan tidak valid.");
        }
        healthBookingDao.delete(bookingId);
    }

    private void activateOrReopenBookingLocked(Connection c, int bookingId, int serviceId, String cleanTarget) {
        // Lock health_service before health_booking to match createBooking's lock order and avoid AB-BA deadlocks.
        HealthService service = healthServiceDao.findByIdForUpdate(c, serviceId)
            .orElseThrow(() -> new IllegalStateException("Data layanan kesehatan terkait tidak ditemukan."));

        HealthBooking booking = lockAndCheckNonTerminalBooking(c, bookingId);
        if (booking.getServiceId() != serviceId) {
            throw new IllegalStateException("Data layanan pada booking kesehatan berubah, silakan ulangi.");
        }

        String currentStatus = normalizeToken(booking.getStatus());
        if ("rejected".equals(currentStatus)) {
            int quota = Math.max(1, service.getDailyQuota());
            int activeInSlot = healthBookingDao.countActiveInSlot(c, serviceId, booking.getBookingDate());
            if (activeInSlot >= quota) {
                throw new IllegalStateException(
                    "Kuota sesi pada " + FormatUtil.dateTimeId(booking.getBookingDate())
                        + " sudah penuh (" + quota + " pasien), sehingga booking tidak dapat diaktifkan kembali."
                );
            }
        }

        healthBookingDao.updateStatus(c, bookingId, cleanTarget, null);
        paymentDao.updateByBookingId(c, bookingId, "MENUNGGU", "pending", false, true);
    }

    private HealthBooking lockAndCheckNonTerminalBooking(Connection c, int bookingId) {
        HealthBooking booking = healthBookingDao.findByIdForUpdate(c, bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Data booking kesehatan tidak ditemukan."));
        String currentStatus = normalizeToken(booking.getStatus());
        if ("completed".equals(currentStatus) || "cancelled".equals(currentStatus)) {
            throw new IllegalStateException(
                "Booking berstatus final (" + currentStatus + ") sudah terkunci dan tidak dapat diubah."
            );
        }
        return booking;
    }

    private int resolveOrCreatePetId(
            Connection c,
            int userId,
            Integer petIdOrNull,
            String cleanPetName,
            String cleanPetSpecies) {
        if (petIdOrNull != null) {
            return petIdOrNull;
        }
        Optional<Pet> existingPet = petDao.findByUserAndName(c, userId, cleanPetName);
        if (existingPet.isPresent()) {
            return existingPet.get().getPetId();
        }
        Pet newPet = new Pet();
        newPet.setUserId(userId);
        newPet.setPetName(cleanPetName);
        newPet.setSpecies(cleanPetSpecies);
        newPet.setBreed("Tidak diketahui");
        newPet.setGender("Tidak diketahui");
        newPet.setAge("1 Tahun");
        newPet.setHealthStatus("Pemeriksaan");
        return petDao.insert(c, newPet).getPetId();
    }

    private LocalTime parseStrictTimeSlot(String timeSlot) {
        if (isBlank(timeSlot)) {
            throw new IllegalArgumentException("Slot jam booking wajib dipilih.");
        }
        try {
            return LocalTime.parse(timeSlot.trim(), TIME_SLOT_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Format jam booking harus HH:mm (contoh: 09:00).", ex);
        }
    }

    private HealthService validateAndNormalizeService(
            HealthService input,
            String fallbackDescription,
            String fallbackIcon,
            String fallbackCardColor) {
        if (input == null) {
            throw new IllegalArgumentException("Data layanan kesehatan tidak boleh null.");
        }

        String serviceName = AuthService.requireNonBlank(
            input.getServiceName(),
            "Nama layanan kesehatan wajib diisi.",
            150,
            "Nama layanan kesehatan maksimal 150 karakter."
        );

        if (input.getPrice() == null) {
            throw new IllegalArgumentException("Harga layanan kesehatan wajib diisi.");
        }
        if (input.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Harga layanan kesehatan tidak boleh negatif.");
        }
        if (input.getPrice().compareTo(MAX_DECIMAL_AMOUNT) > 0) {
            throw new IllegalArgumentException("Harga layanan kesehatan melebihi batas maksimal.");
        }
        BigDecimal price = input.getPrice().setScale(2, RoundingMode.HALF_UP);

        String doctorName = trimOrFallback(
            input.getDoctorName(),
            DEFAULT_DOCTOR_NAME,
            100,
            "Nama dokter maksimal 100 karakter."
        );
        String practiceDays = trimOrFallback(
            input.getPracticeDays(),
            DEFAULT_PRACTICE_DAYS,
            100,
            "Hari praktik maksimal 100 karakter."
        );
        String timeSlots = trimOrFallback(
            input.getTimeSlots(),
            DEFAULT_TIME_SLOTS,
            150,
            "Slot jam praktik maksimal 150 karakter."
        );
        if (parseTimeSlots(timeSlots).isEmpty()) {
            throw new IllegalArgumentException(
                "Slot jam praktik harus berisi minimal satu jam valid berformat HH:mm (contoh: 09:00, 13:00)."
            );
        }

        if (input.getDailyQuota() <= 0) {
            throw new IllegalArgumentException("Kuota harian per sesi harus minimal 1.");
        }

        String description = input.getDescription() == null
            ? (fallbackDescription == null ? "" : fallbackDescription)
            : input.getDescription().trim();

        String icon = trimOrFallback(
            input.getIcon(),
            fallbackIcon,
            30,
            "Ikon layanan maksimal 30 karakter."
        );
        String cardColor = trimOrFallback(
            input.getCardColor(),
            fallbackCardColor,
            50,
            "Warna kartu layanan maksimal 50 karakter."
        );

        HealthService normalized = new HealthService();
        normalized.setServiceId(input.getServiceId());
        normalized.setServiceName(serviceName);
        normalized.setPrice(price);
        normalized.setDoctorName(doctorName);
        normalized.setPracticeDays(practiceDays);
        normalized.setTimeSlots(timeSlots);
        normalized.setDailyQuota(input.getDailyQuota());
        normalized.setDescription(description);
        normalized.setIcon(icon);
        normalized.setCardColor(cardColor);
        return normalized;
    }

    private static String trimOrFallback(String value, String fallback, int maxLen, String tooLongMsg) {
        String clean = isBlank(value) ? fallback : value.trim();
        if (clean.length() > maxLen) {
            throw new IllegalArgumentException(tooLongMsg);
        }
        return clean;
    }

    private static String nonBlankOrFallback(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static <T> T runInTransaction(DatabaseConfig.SqlWork<T> work) {
        try {
            return DatabaseConfig.inTransaction(work);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Kesalahan transaksi layanan kesehatan: " + ex.getMessage(), ex);
        }
    }
}
