package pawpaw.service;

import pawpaw.config.AppConfig;
import pawpaw.config.DatabaseConfig;
import pawpaw.dao.AdoptionDao;
import pawpaw.dao.AnimalDao;
import pawpaw.model.AdoptionRequest;
import pawpaw.model.Animal;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Business service managing adoption catalog animals and transactional adoption verification.
 */
public class AdoptionService {

    private static final Set<String> VALID_ANIMAL_STATUSES = Set.of("available", "adopted", "unavailable");
    private static final Set<String> VALID_ADMIN_REQUEST_STATUSES = Set.of("pending", "approved", "rejected");
    private static final String SELF_CANCEL_NOTE = "Dibatalkan sendiri oleh pemohon.";
    private static final String AUTO_REJECT_NOTE = "Hewan telah disetujui untuk diadopsi oleh pemohon lain.";

    private final AnimalDao animalDao;
    private final AdoptionDao adoptionDao;

    public AdoptionService() {
        this(new AnimalDao(), new AdoptionDao());
    }

    public AdoptionService(AnimalDao animalDao, AdoptionDao adoptionDao) {
        this.animalDao = Objects.requireNonNull(animalDao, "AnimalDao tidak boleh null.");
        this.adoptionDao = Objects.requireNonNull(adoptionDao, "AdoptionDao tidak boleh null.");
    }

    public List<Animal> listAnimals() {
        return animalDao.findAll();
    }

    public Animal createAnimal(Animal a) {
        Animal target = validateAndNormalizeAnimal(a, "");
        return animalDao.insert(target);
    }

    public void updateAnimal(Animal a) {
        if (a == null || a.getAnimalId() <= 0) {
            throw new IllegalArgumentException("ID hewan adopsi tidak valid.");
        }
        Animal existing = animalDao.findById(a.getAnimalId())
            .orElseThrow(() -> new IllegalArgumentException("Data hewan adopsi tidak ditemukan."));
        Animal target = validateAndNormalizeAnimal(a, existing.getDescription());
        animalDao.update(target);
    }

    public void deleteAnimal(int animalId) {
        if (animalId <= 0) {
            throw new IllegalArgumentException("ID hewan adopsi tidak valid.");
        }
        animalDao.delete(animalId);
    }

    public List<AdoptionRequest> listAllRequests() {
        return adoptionDao.findAllWithDetails();
    }

    public List<AdoptionRequest> listUserRequests(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID pengguna tidak valid.");
        }
        return adoptionDao.findByUserIdWithDetails(userId);
    }

    public AdoptionRequest submitRequest(
            int userId,
            int animalId,
            String applicantName,
            String email,
            String phone,
            String address,
            String reason) {
        if (userId <= 0) {
            throw new IllegalArgumentException("ID pengguna tidak valid.");
        }
        if (animalId <= 0) {
            throw new IllegalArgumentException("ID hewan adopsi tidak valid.");
        }

        String cleanName = AuthService.requireNonBlank(
            applicantName,
            "Nama pemohon wajib diisi.",
            150,
            "Nama pemohon maksimal 150 karakter."
        );
        String cleanEmail = AuthService.normalizeAndValidateEmail(email);
        String cleanPhone = AuthService.requireNonBlank(
            phone,
            "Nomor telepon wajib diisi.",
            30,
            "Nomor telepon maksimal 30 karakter."
        );
        String cleanAddress = AuthService.requireNonBlank(
            address,
            "Alamat lengkap wajib diisi.",
            500,
            "Alamat lengkap maksimal 500 karakter."
        );
        String cleanReason = AuthService.requireNonBlank(
            reason,
            "Alasan adopsi wajib diisi.",
            1000,
            "Alasan adopsi maksimal 1000 karakter."
        );

        return runInTransaction(c -> {
            Animal animal = animalDao.findByIdForUpdate(c, animalId)
                .orElseThrow(() -> new IllegalArgumentException("Hewan adopsi tidak ditemukan."));
            if (!"available".equalsIgnoreCase(animal.getStatus())) {
                throw new IllegalStateException("Maaf, hewan ini sudah tidak tersedia untuk diadopsi.");
            }

            AdoptionRequest req = new AdoptionRequest();
            req.setUserId(userId);
            req.setAnimalId(animalId);
            req.setApplicantName(cleanName);
            req.setEmail(cleanEmail);
            req.setPhone(cleanPhone);
            req.setAddress(cleanAddress);
            req.setReason(cleanReason);
            req.setRequestDate(LocalDateTime.now());
            req.setStatus("pending");

            return adoptionDao.insert(c, req);
        });
    }

    public void cancelUserRequest(int userId, int requestId) {
        if (userId <= 0 || requestId <= 0) {
            throw new IllegalArgumentException("ID pengguna atau pengajuan adopsi tidak valid.");
        }

        runInTransaction(c -> {
            AdoptionRequest req = adoptionDao.findByIdForUpdate(c, requestId)
                .orElseThrow(() -> new IllegalArgumentException("Pengajuan adopsi tidak ditemukan."));
            if (req.getUserId() != userId) {
                throw new SecurityException("Anda tidak memiliki izin membatalkan pengajuan adopsi milik pengguna lain.");
            }
            if (!"pending".equalsIgnoreCase(req.getStatus())) {
                throw new IllegalStateException(
                    "Hanya pengajuan adopsi berstatus Menunggu Verifikasi (pending) yang dapat dibatalkan."
                );
            }

            adoptionDao.updateStatusAndNote(c, requestId, "cancelled", SELF_CANCEL_NOTE);
            return null;
        });
    }

    public void updateRequestStatus(int requestId, String targetStatus, String adminNote) {
        if (requestId <= 0) {
            throw new IllegalArgumentException("ID pengajuan adopsi tidak valid.");
        }
        String cleanTarget = targetStatus == null ? "" : targetStatus.trim().toLowerCase(Locale.ROOT);
        if (!VALID_ADMIN_REQUEST_STATUSES.contains(cleanTarget)) {
            throw new IllegalArgumentException("Status pengajuan adopsi harus salah satu dari: pending, approved, rejected.");
        }

        runInTransaction(c -> {
            if ("approved".equals(cleanTarget)) {
                approveRequestLocked(c, requestId, adminNote);
            } else {
                AdoptionRequest req = lockAndCheckNonTerminalRequest(c, requestId);
                String resolvedNote = resolveAdminNote(adminNote, req.getAdminNote());
                adoptionDao.updateStatusAndNote(c, requestId, cleanTarget, resolvedNote);
            }
            return null;
        });
    }

    private void approveRequestLocked(Connection c, int requestId, String adminNote) {
        int animalId = adoptionDao.findAnimalIdByRequestId(c, requestId)
            .orElseThrow(() -> new IllegalArgumentException("Pengajuan adopsi tidak ditemukan."));

        // Lock animal before adoption_request so concurrent approvals serialize on animal_id
        // and cannot AB-BA deadlock with rejectOtherPendingForAnimal.
        Animal animal = animalDao.findByIdForUpdate(c, animalId)
            .orElseThrow(() -> new IllegalStateException("Data hewan adopsi terkait tidak ditemukan."));

        AdoptionRequest req = lockAndCheckNonTerminalRequest(c, requestId);
        if (req.getAnimalId() != animalId) {
            throw new IllegalStateException("Data hewan pada pengajuan adopsi berubah, silakan ulangi.");
        }
        if (!"available".equalsIgnoreCase(animal.getStatus())) {
            throw new IllegalStateException(
                "Hewan adopsi sudah tidak tersedia (status saat ini: " + animal.getStatus() + ")."
            );
        }

        String resolvedNote = resolveAdminNote(adminNote, req.getAdminNote());
        adoptionDao.updateStatusAndNote(c, requestId, "approved", resolvedNote);
        animalDao.updateStatus(c, animalId, "adopted");
        adoptionDao.rejectOtherPendingForAnimal(c, animalId, requestId, AUTO_REJECT_NOTE);
    }

    private AdoptionRequest lockAndCheckNonTerminalRequest(Connection c, int requestId) {
        AdoptionRequest req = adoptionDao.findByIdForUpdate(c, requestId)
            .orElseThrow(() -> new IllegalArgumentException("Pengajuan adopsi tidak ditemukan."));

        String currentStatus = req.getStatus() == null ? "" : req.getStatus().trim().toLowerCase(Locale.ROOT);
        if ("approved".equals(currentStatus) || "cancelled".equals(currentStatus)) {
            throw new IllegalStateException(
                "Pengajuan adopsi berstatus final (" + currentStatus + ") sudah terkunci dan tidak dapat diubah."
            );
        }
        return req;
    }

    private static Animal validateAndNormalizeAnimal(Animal input, String fallbackDescription) {
        if (input == null) {
            throw new IllegalArgumentException("Data hewan adopsi tidak boleh null.");
        }
        String nama = AuthService.requireNonBlank(
            input.getNama(),
            "Nama hewan wajib diisi.",
            100,
            "Nama hewan maksimal 100 karakter."
        );
        String species = AuthService.requireNonBlank(
            input.getSpecies(),
            "Jenis hewan wajib diisi.",
            50,
            "Jenis hewan maksimal 50 karakter."
        );
        String breed = AuthService.requireNonBlank(
            input.getBreed(),
            "Ras hewan wajib diisi.",
            100,
            "Ras hewan maksimal 100 karakter."
        );
        String gender = AuthService.requireNonBlank(
            input.getGender(),
            "Jenis kelamin hewan wajib diisi.",
            20,
            "Jenis kelamin hewan maksimal 20 karakter."
        );
        String age = AuthService.requireNonBlank(
            input.getAge(),
            "Usia hewan wajib diisi.",
            50,
            "Usia hewan maksimal 50 karakter."
        );

        String status = input.getStatus() == null ? "" : input.getStatus().trim().toLowerCase(Locale.ROOT);
        if (!VALID_ANIMAL_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Status hewan harus salah satu dari: available, adopted, unavailable.");
        }

        String image = (input.getImage() == null || input.getImage().isBlank())
            ? AppConfig.DEFAULT_ANIMAL_IMAGE
            : input.getImage().trim().replace('\\', '/');
        if (image.length() > 255) {
            throw new IllegalArgumentException("Path gambar hewan maksimal 255 karakter.");
        }

        String description = input.getDescription() == null
            ? (fallbackDescription == null ? "" : fallbackDescription)
            : input.getDescription().trim();

        Animal normalized = new Animal();
        normalized.setAnimalId(input.getAnimalId());
        normalized.setNama(nama);
        normalized.setSpecies(species);
        normalized.setBreed(breed);
        normalized.setGender(gender);
        normalized.setAge(age);
        normalized.setStatus(status);
        normalized.setImage(image);
        normalized.setDescription(description);
        return normalized;
    }

    private static String resolveAdminNote(String adminNote, String existingNote) {
        if (adminNote == null) {
            return existingNote;
        }
        String trimmed = adminNote.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static <T> T runInTransaction(DatabaseConfig.SqlWork<T> work) {
        try {
            return DatabaseConfig.inTransaction(work);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Kesalahan transaksi adopsi: " + ex.getMessage(), ex);
        }
    }
}
