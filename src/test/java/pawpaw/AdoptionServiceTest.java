package pawpaw;

import pawpaw.config.AppConfig;
import pawpaw.dao.AdoptionDao;
import pawpaw.dao.AnimalDao;
import pawpaw.model.AdoptionRequest;
import pawpaw.model.Animal;
import pawpaw.service.AdoptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for AnimalDao, AdoptionDao, and AdoptionService against live MySQL pet_care.
 */
public class AdoptionServiceTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "AdoptionServiceTest",
            TestAssert.test("Animal CRUD validates required fields, applies default image, and cleans up cleanly",
                AdoptionServiceTest::testAnimalCrudAndValidation),
            TestAssert.test("submitRequest validates fields and rejects unavailable animals",
                AdoptionServiceTest::testSubmitRequestValidationAndAvailability),
            TestAssert.test("cancelUserRequest enforces ownership, sets note, and locks cancelled terminal state",
                AdoptionServiceTest::testCancelUserRequestAndTerminalLock),
            TestAssert.test("updateRequestStatus approval transitions animal to adopted, auto-rejects pending, and locks approved",
                AdoptionServiceTest::testTransactionalApprovalAndTerminalLocks),
            TestAssert.test("concurrent updateRequestStatus approvals serialize cleanly without AB-BA deadlock",
                AdoptionServiceTest::testConcurrentApprovalsSerializeWithoutDeadlock)
        );
    }

    private static void testAnimalCrudAndValidation() {
        AnimalDao animalDao = new AnimalDao();
        AdoptionDao adoptionDao = new AdoptionDao();
        AdoptionService adoptionService = new AdoptionService(animalDao, adoptionDao);
        List<Integer> cleanupAnimalIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            int initialCount = animalDao.countAll();
            TestAssert.assertTrue(initialCount >= 10, "Seed database must have at least 10 animals");

            Animal missingName = validTestAnimal("Temp-" + tag);
            missingName.setNama("   ");
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.createAnimal(missingName),
                "Blank animal nama must be rejected"
            );

            Animal missingSpecies = validTestAnimal("Temp-" + tag);
            missingSpecies.setSpecies("");
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.createAnimal(missingSpecies),
                "Blank animal species must be rejected"
            );

            Animal missingBreed = validTestAnimal("Temp-" + tag);
            missingBreed.setBreed(null);
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.createAnimal(missingBreed),
                "Blank animal breed must be rejected"
            );

            Animal missingGender = validTestAnimal("Temp-" + tag);
            missingGender.setGender(" ");
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.createAnimal(missingGender),
                "Blank animal gender must be rejected"
            );

            Animal missingAge = validTestAnimal("Temp-" + tag);
            missingAge.setAge("");
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.createAnimal(missingAge),
                "Blank animal age must be rejected"
            );

            Animal invalidStatus = validTestAnimal("Temp-" + tag);
            invalidStatus.setStatus("reserved");
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.createAnimal(invalidStatus),
                "Invalid animal status must be rejected"
            );

            Animal toCreate = validTestAnimal("  Mochi-" + tag + "  ");
            toCreate.setImage("   ");
            toCreate.setDescription("  Jinak dan ceria  ");
            Animal created = adoptionService.createAnimal(toCreate);
            cleanupAnimalIds.add(created.getAnimalId());

            TestAssert.assertTrue(created.getAnimalId() > 0, "Created animal must have generated animalId");
            TestAssert.assertEquals("Mochi-" + tag, created.getNama(), "Animal nama must be trimmed");
            TestAssert.assertEquals("available", created.getStatus(), "Created animal status must be available");
            TestAssert.assertEquals(
                AppConfig.DEFAULT_ANIMAL_IMAGE,
                created.getImage(),
                "Blank image must default to AppConfig.DEFAULT_ANIMAL_IMAGE"
            );
            TestAssert.assertEquals(initialCount + 1, animalDao.countAll(), "Animal count must increase by 1");
            TestAssert.assertTrue(
                adoptionService.listAnimals().stream().anyMatch(a -> a.getAnimalId() == created.getAnimalId()),
                "listAnimals must include created animal"
            );

            created.setNama("Mochi-Updated-" + tag);
            created.setBreed("Scottish Fold");
            created.setStatus("unavailable");
            created.setImage("");
            adoptionService.updateAnimal(created);

            Optional<Animal> reloaded = animalDao.findById(created.getAnimalId());
            TestAssert.assertTrue(reloaded.isPresent(), "Updated animal must exist in DB");
            TestAssert.assertEquals("Mochi-Updated-" + tag, reloaded.get().getNama(), "Updated nama must persist");
            TestAssert.assertEquals("Scottish Fold", reloaded.get().getBreed(), "Updated breed must persist");
            TestAssert.assertEquals("unavailable", reloaded.get().getStatus(), "Updated status must persist");
            TestAssert.assertEquals(
                AppConfig.DEFAULT_ANIMAL_IMAGE,
                reloaded.get().getImage(),
                "Blank image on update must default to AppConfig.DEFAULT_ANIMAL_IMAGE"
            );

            adoptionService.deleteAnimal(created.getAnimalId());
            cleanupAnimalIds.clear();
            TestAssert.assertTrue(animalDao.findById(created.getAnimalId()).isEmpty(), "Deleted animal must be removed");
            TestAssert.assertEquals(initialCount, animalDao.countAll(), "Animal count must return to initial count");

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.deleteAnimal(created.getAnimalId()),
                "Deleting missing animal must throw IllegalArgumentException"
            );
        } finally {
            cleanupAnimals(animalDao, cleanupAnimalIds);
        }
    }

    private static void testSubmitRequestValidationAndAvailability() {
        AnimalDao animalDao = new AnimalDao();
        AdoptionDao adoptionDao = new AdoptionDao();
        AdoptionService adoptionService = new AdoptionService(animalDao, adoptionDao);
        List<Integer> cleanupAnimalIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            Animal animal = adoptionService.createAnimal(validTestAnimal("Avail-" + tag));
            cleanupAnimalIds.add(animal.getAnimalId());

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.submitRequest(1, animal.getAnimalId(), "   ", "user@example.com", "0812", "Alamat", "Alasan"),
                "Blank applicantName must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.submitRequest(1, animal.getAnimalId(), "Pemohon", "not-an-email", "0812", "Alamat", "Alasan"),
                "Invalid email must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.submitRequest(1, animal.getAnimalId(), "Pemohon", "user@example.com", " ", "Alamat", "Alasan"),
                "Blank phone must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.submitRequest(1, animal.getAnimalId(), "Pemohon", "user@example.com", "0812", "", "Alasan"),
                "Blank address must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.submitRequest(1, animal.getAnimalId(), "Pemohon", "user@example.com", "0812", "Alamat", "   "),
                "Blank reason must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.submitRequest(1, 9999999, "Pemohon", "user@example.com", "0812", "Alamat", "Alasan"),
                "Non-existent animal must throw IllegalArgumentException"
            );

            Animal unavailable = validTestAnimal("Unavail-" + tag);
            unavailable.setStatus("unavailable");
            Animal savedUnavail = adoptionService.createAnimal(unavailable);
            cleanupAnimalIds.add(savedUnavail.getAnimalId());

            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> adoptionService.submitRequest(1, savedUnavail.getAnimalId(), "Pemohon", "user@example.com", "0812", "Alamat", "Alasan"),
                "Submitting request for unavailable animal must throw IllegalStateException"
            );
        } finally {
            cleanupAnimals(animalDao, cleanupAnimalIds);
        }
    }

    private static void testCancelUserRequestAndTerminalLock() {
        AnimalDao animalDao = new AnimalDao();
        AdoptionDao adoptionDao = new AdoptionDao();
        AdoptionService adoptionService = new AdoptionService(animalDao, adoptionDao);
        List<Integer> cleanupAnimalIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            Animal animal = adoptionService.createAnimal(validTestAnimal("Cancel-" + tag));
            cleanupAnimalIds.add(animal.getAnimalId());

            AdoptionRequest r3 = adoptionService.submitRequest(
                3,
                animal.getAnimalId(),
                "  Farras Akbar  ",
                "Farras." + tag + "@Example.COM",
                " 081233330003 ",
                " Jl. Garuda Sakti ",
                " Ingin merawat dengan baik "
            );
            TestAssert.assertTrue(r3.getAdoptionRequestId() > 0, "Submitted request must have generated ID");
            TestAssert.assertEquals("pending", r3.getStatus(), "New request status must be pending");
            TestAssert.assertEquals("farras." + tag + "@example.com", r3.getEmail(), "Request email must be normalized");
            TestAssert.assertEquals(animal.getNama(), r3.getAnimalNama(), "Joined animalNama must be populated");

            TestAssert.assertThrows(
                SecurityException.class,
                () -> adoptionService.cancelUserRequest(2, r3.getAdoptionRequestId()),
                "Cancelling another user's request must throw SecurityException"
            );

            adoptionService.cancelUserRequest(3, r3.getAdoptionRequestId());

            AdoptionRequest cancelled = findRequestById(
                adoptionService.listUserRequests(3),
                r3.getAdoptionRequestId()
            );
            TestAssert.assertEquals("cancelled", cancelled.getStatus(), "Cancelled request must have status 'cancelled'");
            TestAssert.assertEquals(
                "Dibatalkan sendiri oleh pemohon.",
                cancelled.getAdminNote(),
                "Self-cancelled request must store standard admin_note"
            );

            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> adoptionService.cancelUserRequest(3, r3.getAdoptionRequestId()),
                "Re-cancelling a cancelled request must throw IllegalStateException"
            );

            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> adoptionService.updateRequestStatus(r3.getAdoptionRequestId(), "approved", "Coba ubah"),
                "Admin updating a terminal cancelled request to approved must throw IllegalStateException"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> adoptionService.updateRequestStatus(r3.getAdoptionRequestId(), "pending", "Coba buka lagi"),
                "Admin updating a terminal cancelled request to pending must throw IllegalStateException"
            );
        } finally {
            cleanupAnimals(animalDao, cleanupAnimalIds);
        }
    }

    private static void testTransactionalApprovalAndTerminalLocks() {
        AnimalDao animalDao = new AnimalDao();
        AdoptionDao adoptionDao = new AdoptionDao();
        AdoptionService adoptionService = new AdoptionService(animalDao, adoptionDao);
        List<Integer> cleanupAnimalIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            Animal animal = adoptionService.createAnimal(validTestAnimal("AdoptFlow-" + tag));
            cleanupAnimalIds.add(animal.getAnimalId());

            AdoptionRequest r1 = adoptionService.submitRequest(
                1,
                animal.getAnimalId(),
                "Jesika Putri",
                "jesika." + tag + "@pawpaw.com",
                "081210000001",
                "Jl. Subrantas No. 10",
                "Ingin merawat dengan kasih sayang"
            );
            AdoptionRequest r2 = adoptionService.submitRequest(
                2,
                animal.getAnimalId(),
                "Arin Maharani",
                "arin." + tag + "@pawpaw.com",
                "081210000002",
                "Jl. Soebrantas No. 22",
                "Rumah luas dan ramah hewan"
            );
            AdoptionRequest r3 = adoptionService.submitRequest(
                3,
                animal.getAnimalId(),
                "Farras Akbar",
                "farras." + tag + "@pawpaw.com",
                "081210000003",
                "Jl. Garuda Sakti",
                "Berpengalaman merawat kucing"
            );
            adoptionService.cancelUserRequest(3, r3.getAdoptionRequestId());

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> adoptionService.updateRequestStatus(r1.getAdoptionRequestId(), "cancelled", "Invalid target"),
                "Admin updateRequestStatus must reject non-pending/approved/rejected target status"
            );

            adoptionService.updateRequestStatus(r1.getAdoptionRequestId(), "approved", "Disetujui test");

            List<AdoptionRequest> all = adoptionService.listAllRequests();
            AdoptionRequest reloadedR1 = findRequestById(all, r1.getAdoptionRequestId());
            AdoptionRequest reloadedR2 = findRequestById(all, r2.getAdoptionRequestId());
            AdoptionRequest reloadedR3 = findRequestById(all, r3.getAdoptionRequestId());

            TestAssert.assertEquals("approved", reloadedR1.getStatus(), "R1 must transition to approved");
            TestAssert.assertEquals("Disetujui test", reloadedR1.getAdminNote(), "R1 adminNote must persist");

            Animal reloadedAnimal = animalDao.findById(animal.getAnimalId())
                .orElseThrow(() -> new AssertionError("Animal must still exist"));
            TestAssert.assertEquals("adopted", reloadedAnimal.getStatus(), "Animal status must transition to adopted");

            TestAssert.assertEquals("rejected", reloadedR2.getStatus(), "Competing pending R2 must be auto-rejected");
            TestAssert.assertEquals(
                "Hewan telah disetujui untuk diadopsi oleh pemohon lain.",
                reloadedR2.getAdminNote(),
                "Auto-rejected R2 must have standard competing-applicant adminNote"
            );

            TestAssert.assertEquals("cancelled", reloadedR3.getStatus(), "Already-cancelled R3 must remain cancelled");
            TestAssert.assertEquals(
                "Dibatalkan sendiri oleh pemohon.",
                reloadedR3.getAdminNote(),
                "Already-cancelled R3 note must remain intact"
            );

            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> adoptionService.updateRequestStatus(r1.getAdoptionRequestId(), "rejected", "Ubah paksa"),
                "Terminal approved R1 must reject transition to rejected"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> adoptionService.updateRequestStatus(r1.getAdoptionRequestId(), "pending", "Rollback paksa"),
                "Terminal approved R1 must reject transition to pending"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> adoptionService.updateRequestStatus(r2.getAdoptionRequestId(), "approved", "Approve pada hewan adopted"),
                "Approving another request when animal is already adopted must throw IllegalStateException"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> adoptionService.submitRequest(
                    4,
                    animal.getAnimalId(),
                    "Fakhry Ramadhan",
                    "fakhry." + tag + "@pawpaw.com",
                    "081210000004",
                    "Jl. Bina Krida",
                    "Ingin mengadopsi"
                ),
                "submitRequest against adopted animal must throw IllegalStateException"
            );

            // Verify countAll and findRecent on AdoptionDao
            int totalRequests = adoptionDao.countAll();
            TestAssert.assertTrue(totalRequests >= 13, "countAll must include 10 seed rows plus R1, R2, R3");
            TestAssert.assertEquals(0, adoptionDao.findRecent(0).size(), "findRecent(0) must return empty list");
            List<AdoptionRequest> recentThree = adoptionDao.findRecent(3);
            TestAssert.assertEquals(3, recentThree.size(), "findRecent(3) must return 3 requests");
            TestAssert.assertTrue(
                recentThree.stream().anyMatch(r -> r.getAnimalId() == animal.getAnimalId()),
                "findRecent(3) must include latest temporary animal requests"
            );

            // Verify non-terminal rejected -> pending transition preserves note when adminNote is null
            Animal animal2 = adoptionService.createAnimal(validTestAnimal("Reopen-" + tag));
            cleanupAnimalIds.add(animal2.getAnimalId());
            AdoptionRequest r4 = adoptionService.submitRequest(
                4,
                animal2.getAnimalId(),
                "Fakhry Ramadhan",
                "fakhry2." + tag + "@pawpaw.com",
                "081210000004",
                "Jl. Bina Krida",
                "Alasan uji coba"
            );
            adoptionService.updateRequestStatus(r4.getAdoptionRequestId(), "rejected", "Dokumen kurang lengkap");
            AdoptionRequest rejectedR4 = findRequestById(adoptionService.listAllRequests(), r4.getAdoptionRequestId());
            TestAssert.assertEquals("rejected", rejectedR4.getStatus(), "R4 must become rejected");
            TestAssert.assertEquals("Dokumen kurang lengkap", rejectedR4.getAdminNote(), "R4 rejection note must persist");

            adoptionService.updateRequestStatus(r4.getAdoptionRequestId(), "pending", null);
            AdoptionRequest reopenedR4 = findRequestById(adoptionService.listAllRequests(), r4.getAdoptionRequestId());
            TestAssert.assertEquals("pending", reopenedR4.getStatus(), "Rejected R4 must be reopenable to pending");
            TestAssert.assertEquals("Dokumen kurang lengkap", reopenedR4.getAdminNote(), "Null adminNote must preserve existing note");

            // Make animal2 unavailable, verify approving R4 throws IllegalStateException and leaves R4 pending
            animal2.setStatus("unavailable");
            adoptionService.updateAnimal(animal2);
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> adoptionService.updateRequestStatus(r4.getAdoptionRequestId(), "approved", "Harus gagal"),
                "Approving request for unavailable animal must throw IllegalStateException"
            );
            AdoptionRequest afterRollbackR4 = findRequestById(adoptionService.listAllRequests(), r4.getAdoptionRequestId());
            TestAssert.assertEquals("pending", afterRollbackR4.getStatus(), "Failed approval must roll back R4 status to pending");
        } finally {
            cleanupAnimals(animalDao, cleanupAnimalIds);
        }
    }

    private static void testConcurrentApprovalsSerializeWithoutDeadlock() throws Exception {
        AnimalDao animalDao = new AnimalDao();
        AdoptionDao adoptionDao = new AdoptionDao();
        AdoptionService adoptionService = new AdoptionService(animalDao, adoptionDao);
        List<Integer> cleanupAnimalIds = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            for (int round = 1; round <= 3; round++) {
                String tag = uniqueTag();
                Animal animal = adoptionService.createAnimal(validTestAnimal("Concurrent-" + round + "-" + tag));
                cleanupAnimalIds.add(animal.getAnimalId());

                AdoptionRequest r1 = adoptionService.submitRequest(
                    1,
                    animal.getAnimalId(),
                    "Pemohon Satu",
                    "satu." + tag + "@pawpaw.com",
                    "081210000001",
                    "Jl. Satu",
                    "Alasan satu"
                );
                AdoptionRequest r2 = adoptionService.submitRequest(
                    2,
                    animal.getAnimalId(),
                    "Pemohon Dua",
                    "dua." + tag + "@pawpaw.com",
                    "081210000002",
                    "Jl. Dua",
                    "Alasan dua"
                );

                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);

                Future<Throwable> f1 = pool.submit(() -> runConcurrentApprove(adoptionService, r1.getAdoptionRequestId(), "Win-1", ready, start));
                Future<Throwable> f2 = pool.submit(() -> runConcurrentApprove(adoptionService, r2.getAdoptionRequestId(), "Win-2", ready, start));

                TestAssert.assertTrue(ready.await(5, TimeUnit.SECONDS), "Worker threads must reach start barrier");
                start.countDown();

                Throwable err1 = f1.get(15, TimeUnit.SECONDS);
                Throwable err2 = f2.get(15, TimeUnit.SECONDS);

                int successes = (err1 == null ? 1 : 0) + (err2 == null ? 1 : 0);
                TestAssert.assertEquals(1, successes, "Exactly one concurrent approval must succeed (round " + round + ")");

                Throwable failure = err1 != null ? err1 : err2;
                TestAssert.assertTrue(
                    failure instanceof IllegalStateException,
                    "Losing concurrent approval must fail with IllegalStateException (no deadlock), got: " + failure
                );

                Animal reloadedAnimal = animalDao.findById(animal.getAnimalId())
                    .orElseThrow(() -> new AssertionError("Animal must exist after concurrent test"));
                TestAssert.assertEquals("adopted", reloadedAnimal.getStatus(), "Animal must be adopted");

                List<AdoptionRequest> all = adoptionService.listAllRequests();
                AdoptionRequest reloaded1 = findRequestById(all, r1.getAdoptionRequestId());
                AdoptionRequest reloaded2 = findRequestById(all, r2.getAdoptionRequestId());

                AdoptionRequest winner = "approved".equals(reloaded1.getStatus()) ? reloaded1 : reloaded2;
                AdoptionRequest loser = winner == reloaded1 ? reloaded2 : reloaded1;
                TestAssert.assertEquals("approved", winner.getStatus(), "Winner must be approved");
                TestAssert.assertEquals("rejected", loser.getStatus(), "Loser must be auto-rejected");
                TestAssert.assertEquals(
                    "Hewan telah disetujui untuk diadopsi oleh pemohon lain.",
                    loser.getAdminNote(),
                    "Loser must carry standard competing-applicant rejection note"
                );
            }
        } finally {
            pool.shutdownNow();
            cleanupAnimals(animalDao, cleanupAnimalIds);
        }
    }

    private static Throwable runConcurrentApprove(
            AdoptionService service,
            int requestId,
            String note,
            CountDownLatch ready,
            CountDownLatch start) {
        ready.countDown();
        try {
            start.await(5, TimeUnit.SECONDS);
            service.updateRequestStatus(requestId, "approved", note);
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private static Animal validTestAnimal(String nama) {
        Animal animal = new Animal();
        animal.setNama(nama);
        animal.setSpecies("kucing");
        animal.setBreed("Persia");
        animal.setGender("Jantan");
        animal.setAge("1 Tahun");
        animal.setStatus("available");
        animal.setImage(AppConfig.DEFAULT_ANIMAL_IMAGE);
        animal.setDescription("Hewan pengujian sementara.");
        return animal;
    }

    private static AdoptionRequest findRequestById(List<AdoptionRequest> requests, int requestId) {
        return requests.stream()
            .filter(r -> r.getAdoptionRequestId() == requestId)
            .findFirst()
            .orElseThrow(() -> new AssertionError("AdoptionRequest #" + requestId + " not found"));
    }

    private static String uniqueTag() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static void cleanupAnimals(AnimalDao animalDao, List<Integer> animalIds) {
        for (int id : animalIds) {
            try {
                if (animalDao.findById(id).isPresent()) {
                    animalDao.delete(id);
                }
            } catch (Exception ex) {
                System.err.println("[CLEANUP WARN] Failed deleting animal_id=" + id + ": " + ex.getMessage());
            }
        }
    }
}
