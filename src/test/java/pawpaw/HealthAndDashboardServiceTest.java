package pawpaw;

import pawpaw.config.DatabaseConfig;
import pawpaw.dao.DashboardDao;
import pawpaw.dao.HealthBookingDao;
import pawpaw.dao.HealthServiceDao;
import pawpaw.dao.OrderDao;
import pawpaw.dao.PaymentDao;
import pawpaw.dao.PetDao;
import pawpaw.model.DashboardStats;
import pawpaw.model.HealthBooking;
import pawpaw.model.HealthService;
import pawpaw.model.Payment;
import pawpaw.model.Pet;
import pawpaw.service.AdminDashboardService;
import pawpaw.service.HealthBookingService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for HealthServiceDao, HealthBookingDao, DashboardDao,
 * HealthBookingService, and AdminDashboardService against live MySQL pet_care.
 */
public class HealthAndDashboardServiceTest {

    public static void main(String[] args) {
        TestAssert.runSuite(
            "HealthAndDashboardServiceTest",
            TestAssert.test("parseTimeSlots trims whitespace, drops blank/invalid/duplicate tokens, and never throws",
                HealthAndDashboardServiceTest::testParseTimeSlots),
            TestAssert.test("HealthService CRUD validates inputs, applies defaults, and cleans up cleanly",
                HealthAndDashboardServiceTest::testHealthServiceCrudAndValidation),
            TestAssert.test("createBooking validates inputs, auto-creates pet, creates MENUNGGU payment, enforces quota & FK guard",
                HealthAndDashboardServiceTest::testCreateBookingAutoPetQuotaAndFkGuard),
            TestAssert.test("booking lifecycle: reject, quota-checked reactivation, user cancel, completed payment & terminal locks",
                HealthAndDashboardServiceTest::testBookingLifecycleReactivationCancelAndCompletedLock),
            TestAssert.test("concurrent createBooking and reactivation on quota=1 serialize cleanly in both lock orders without MVCC oversell",
                HealthAndDashboardServiceTest::testConcurrentBookingAndReactivationNoDeadlock),
            TestAssert.test("concurrent deleteService and createBooking serialize on health_service lock without cascade data loss",
                HealthAndDashboardServiceTest::testConcurrentDeleteServiceAndCreateBookingNoCascadeLoss),
            TestAssert.test("AdminDashboardService.loadOverview returns accurate seed counts, revenue, and recent joined lists",
                HealthAndDashboardServiceTest::testAdminDashboardOverview)
        );
    }

    private static void testParseTimeSlots() {
        HealthBookingService service = new HealthBookingService();

        List<String> parsed = service.parseTimeSlots(" 09:00 , , 14:00, 09:00, invalid ");
        TestAssert.assertEquals(
            List.of("09:00", "14:00"),
            parsed,
            "parseTimeSlots must trim, drop blank/invalid/duplicate tokens, and preserve order"
        );

        TestAssert.assertEquals(
            List.of(),
            service.parseTimeSlots(null),
            "parseTimeSlots(null) must return empty list"
        );
        TestAssert.assertEquals(
            List.of(),
            service.parseTimeSlots("   "),
            "parseTimeSlots(blank) must return empty list"
        );
        TestAssert.assertEquals(
            List.of(),
            service.parseTimeSlots("abc, 25:00, 9:00, 09:00:00, 12:60, -01:00"),
            "parseTimeSlots must ignore invalid/non-HH:mm tokens without throwing DateTimeParseException"
        );
        TestAssert.assertEquals(
            List.of("16:00", "08:00", "12:00"),
            service.parseTimeSlots("16:00, 08:00, 16:00, 12:00"),
            "parseTimeSlots must preserve insertion order of first occurrences"
        );
    }

    private static void testHealthServiceCrudAndValidation() {
        HealthServiceDao serviceDao = new HealthServiceDao();
        HealthBookingDao bookingDao = new HealthBookingDao();
        PetDao petDao = new PetDao();
        PaymentDao paymentDao = new PaymentDao();
        HealthBookingService bookingService = new HealthBookingService(serviceDao, bookingDao, petDao, paymentDao);
        List<Integer> cleanupServiceIds = new ArrayList<>();
        String tag = uniqueTag();

        try {
            int initialCount = bookingService.listServices().size();
            TestAssert.assertTrue(initialCount >= 10, "Seed database must have at least 10 health services");

            HealthService blankName = validTestService("Svc-" + tag);
            blankName.setServiceName("   ");
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createService(blankName),
                "Blank serviceName must be rejected"
            );

            HealthService negPrice = validTestService("Svc-" + tag);
            negPrice.setPrice(new BigDecimal("-500.00"));
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createService(negPrice),
                "Negative service price must be rejected"
            );

            HealthService zeroQuota = validTestService("Svc-" + tag);
            zeroQuota.setDailyQuota(0);
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createService(zeroQuota),
                "dailyQuota <= 0 must be rejected"
            );

            HealthService badSlots = validTestService("Svc-" + tag);
            badSlots.setTimeSlots("invalid, 99:99");
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createService(badSlots),
                "Service timeSlots with zero valid HH:mm slots must be rejected"
            );

            HealthService toCreate = new HealthService();
            toCreate.setServiceName("  Vaksin Uji " + tag + "  ");
            toCreate.setPrice(new BigDecimal("125000"));
            toCreate.setDoctorName("   ");
            toCreate.setPracticeDays("");
            toCreate.setTimeSlots(" 09:00 , , 14:00, 09:00, invalid ");
            toCreate.setDailyQuota(2);
            toCreate.setDescription("  Layanan uji sementara  ");
            toCreate.setIcon(" ");
            toCreate.setCardColor("");

            HealthService created = bookingService.createService(toCreate);
            cleanupServiceIds.add(created.getServiceId());

            TestAssert.assertTrue(created.getServiceId() > 0, "Created service must have generated serviceId");
            TestAssert.assertEquals("Vaksin Uji " + tag, created.getServiceName(), "serviceName must be trimmed");
            assertMoneyEquals(new BigDecimal("125000.00"), created.getPrice(), "price must persist at scale 2");
            TestAssert.assertEquals("drh. Clara Wijaya", created.getDoctorName(), "Blank doctorName must use default");
            TestAssert.assertEquals("Senin - Sabtu", created.getPracticeDays(), "Blank practiceDays must use default");
            TestAssert.assertEquals("card-green", created.getCardColor(), "Blank cardColor must use default");
            TestAssert.assertEquals(
                List.of("09:00", "14:00"),
                bookingService.parseTimeSlots(created.getTimeSlots()),
                "Created service timeSlots must parse cleanly to [09:00, 14:00]"
            );

            created.setServiceName("Vaksin Update " + tag);
            created.setPrice(new BigDecimal("140000.00"));
            created.setDoctorName("drh. Budi Santoso");
            created.setDailyQuota(3);
            created.setDescription(null);
            bookingService.updateService(created);

            HealthService reloaded = serviceDao.findById(created.getServiceId()).orElseThrow();
            TestAssert.assertEquals("Vaksin Update " + tag, reloaded.getServiceName(), "Updated serviceName must persist");
            assertMoneyEquals(new BigDecimal("140000.00"), reloaded.getPrice(), "Updated price must persist");
            TestAssert.assertEquals("drh. Budi Santoso", reloaded.getDoctorName(), "Updated doctorName must persist");
            TestAssert.assertEquals(3, reloaded.getDailyQuota(), "Updated dailyQuota must persist");
            TestAssert.assertEquals("Layanan uji sementara", reloaded.getDescription(), "Null description on update must preserve existing");

            bookingService.deleteService(created.getServiceId());
            cleanupServiceIds.clear();
            TestAssert.assertTrue(serviceDao.findById(created.getServiceId()).isEmpty(), "Deleted service must be gone");
            TestAssert.assertEquals(initialCount, bookingService.listServices().size(), "Service count must return to initial");

            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.deleteService(created.getServiceId()),
                "Deleting missing service must throw IllegalArgumentException"
            );
        } finally {
            cleanupData(serviceDao, bookingDao, petDao, cleanupServiceIds, List.of(), Set.of(), 1);
        }
    }

    private static void testCreateBookingAutoPetQuotaAndFkGuard() {
        HealthServiceDao serviceDao = new HealthServiceDao();
        HealthBookingDao bookingDao = new HealthBookingDao();
        PetDao petDao = new PetDao();
        PaymentDao paymentDao = new PaymentDao();
        HealthBookingService bookingService = new HealthBookingService(serviceDao, bookingDao, petDao, paymentDao);

        List<Integer> cleanupServiceIds = new ArrayList<>();
        List<Integer> cleanupBookingIds = new ArrayList<>();
        Set<Integer> cleanupPetIds = new LinkedHashSet<>();
        String tag = uniqueTag();

        try {
            HealthService svcInput = validTestService("Checkup-" + tag);
            svcInput.setDailyQuota(1);
            svcInput.setTimeSlots(" 09:00 , , 14:00, 09:00, invalid ");
            svcInput.setPrice(new BigDecimal("95000.00"));
            HealthService svc = bookingService.createService(svcInput);
            cleanupServiceIds.add(svc.getServiceId());

            LocalDate tomorrow = LocalDate.now().plusDays(1);
            String tempPetName = "TestPet-" + tag;

            // Validation checks before DB booking creation
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1, svc.getServiceId(), null, "Jesika Putri", tempPetName, "Kucing", LocalDate.now().minusDays(1), "09:00"),
                "Booking in the past must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1, svc.getServiceId(), null, "Jesika Putri", tempPetName, "Kucing", tomorrow, "invalid"),
                "Invalid timeSlot format must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1, svc.getServiceId(), null, "Jesika Putri", tempPetName, "Kucing", tomorrow, "9:00"),
                "Non-HH:mm timeSlot must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1, svc.getServiceId(), null, "Jesika Putri", tempPetName, "Kucing", tomorrow, "16:00"),
                "TimeSlot not offered by service must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1, svc.getServiceId(), null, " ", tempPetName, "Kucing", tomorrow, "09:00"),
                "Blank ownerName must be rejected"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(1, svc.getServiceId(), null, "Jesika Putri", "  ", "Kucing", tomorrow, "09:00"),
                "Blank petName with petId=null must be rejected"
            );
            TestAssert.assertThrows(
                SecurityException.class,
                () -> bookingService.createBooking(1, svc.getServiceId(), 2, "Jesika Putri", "Luna", "Anjing", tomorrow, "09:00"),
                "Booking with another user's pet_id must throw SecurityException"
            );

            Map<String, Integer> quotaBefore = bookingService.getRemainingQuotaBySlot(svc.getServiceId(), tomorrow);
            TestAssert.assertEquals(1, quotaBefore.get("09:00"), "Remaining quota for 09:00 before booking must be 1");
            TestAssert.assertEquals(1, quotaBefore.get("14:00"), "Remaining quota for 14:00 before booking must be 1");

            HealthBooking b1 = bookingService.createBooking(
                1,
                svc.getServiceId(),
                null,
                "  Jesika Putri  ",
                "  " + tempPetName + "  ",
                "  Kucing  ",
                tomorrow,
                " 09:00 "
            );
            cleanupBookingIds.add(b1.getBookingId());
            if (b1.getPetId() != null) {
                cleanupPetIds.add(b1.getPetId());
            }

            TestAssert.assertTrue(b1.getBookingId() > 0, "Booking must have generated bookingId");
            TestAssert.assertEquals("pending", b1.getStatus(), "New booking status must be pending");
            TestAssert.assertEquals(LocalDateTime.of(tomorrow, LocalTime.of(9, 0)), b1.getBookingDate(), "Booking date/time must match tomorrow 09:00");
            TestAssert.assertEquals("Jesika Putri", b1.getOwnerName(), "ownerName must be trimmed");
            TestAssert.assertEquals(tempPetName, b1.getPetName(), "petName must be trimmed");
            TestAssert.assertEquals("Kucing", b1.getPetSpecies(), "petSpecies must be trimmed");
            TestAssert.assertEquals(svc.getServiceName(), b1.getServiceName(), "Joined serviceName must be populated");
            TestAssert.assertEquals(svc.getDoctorName(), b1.getDoctorName(), "Joined doctorName must be populated");
            assertMoneyEquals(new BigDecimal("95000.00"), b1.getServicePrice(), "Joined servicePrice must be populated");
            TestAssert.assertEquals("Jesika Putri", b1.getUserName(), "Joined userName must be populated");

            // Auto-created pet verification
            TestAssert.assertNotNull(b1.getPetId(), "createBooking with petId=null must auto-create pet and set petId");
            Pet createdPet = petDao.findById(b1.getPetId()).orElseThrow();
            TestAssert.assertEquals(1, createdPet.getUserId(), "Auto-created pet must belong to userId");
            TestAssert.assertEquals(tempPetName, createdPet.getPetName(), "Auto-created pet name must match");
            TestAssert.assertEquals("Kucing", createdPet.getSpecies(), "Auto-created pet species must match");
            TestAssert.assertEquals("Tidak diketahui", createdPet.getBreed(), "Auto-created pet breed must default to 'Tidak diketahui'");
            TestAssert.assertEquals("Tidak diketahui", createdPet.getGender(), "Auto-created pet gender must default to 'Tidak diketahui'");
            TestAssert.assertEquals("1 Tahun", createdPet.getAge(), "Auto-created pet age must default to '1 Tahun'");
            TestAssert.assertEquals("Pemeriksaan", createdPet.getHealthStatus(), "Auto-created pet healthStatus must default to 'Pemeriksaan'");

            // Linked payment verification
            Payment p1 = b1.getPayment();
            TestAssert.assertNotNull(p1, "Booking must have joined Payment");
            TestAssert.assertEquals(b1.getBookingId(), p1.getBookingId(), "Payment bookingId must match");
            TestAssert.assertNull(p1.getOrderId(), "Booking payment orderId must be null");
            assertMoneyEquals(new BigDecimal("95000.00"), p1.getAmount(), "Payment amount must equal service price");
            TestAssert.assertEquals("TUNAI", p1.getMethod(), "Booking payment method code must be TUNAI");
            TestAssert.assertEquals("MENUNGGU", p1.getStatus(), "Booking payment status must be MENUNGGU");
            TestAssert.assertEquals("pending", p1.getPaymentStatus(), "Booking paymentStatus must be pending");
            TestAssert.assertEquals("Klinik / COD", p1.getPaymentMethod(), "Booking paymentMethod must be 'Klinik / COD'");
            TestAssert.assertEquals("PAY-BKG-" + b1.getBookingId(), p1.getReferenceNumber(), "Reference number must be PAY-BKG-<bookingId>");
            TestAssert.assertNull(p1.getPaidAt(), "Initial booking payment paidAt must be null");

            // Quota check & second booking rejection
            Map<String, Integer> quotaAfter = bookingService.getRemainingQuotaBySlot(svc.getServiceId(), tomorrow);
            TestAssert.assertEquals(0, quotaAfter.get("09:00"), "Remaining quota for 09:00 after booking must be 0");
            TestAssert.assertEquals(1, quotaAfter.get("14:00"), "Remaining quota for 14:00 must stay 1");

            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(2, svc.getServiceId(), 2, "Arin Maharani", "Luna", "Anjing", tomorrow, "09:00"),
                "Second booking on full slot (daily_quota=1) must fail with IllegalStateException"
            );

            // FK delete guard on service with bookings
            TestAssert.assertTrue(serviceDao.hasBookings(svc.getServiceId()), "hasBookings must return true");
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> bookingService.deleteService(svc.getServiceId()),
                "deleteService on service with bookings must throw IllegalStateException"
            );

            // Ownership checks on getBookingCard and listing helpers
            TestAssert.assertThrows(
                SecurityException.class,
                () -> bookingService.getBookingCard(b1.getBookingId(), 2, false),
                "Non-owner non-admin requesting booking card must throw SecurityException"
            );
            HealthBooking ownerCard = bookingService.getBookingCard(b1.getBookingId(), 1, false);
            TestAssert.assertNotNull(ownerCard.getPayment(), "getBookingCard must populate payment");
            HealthBooking adminCard = bookingService.getBookingCard(b1.getBookingId(), 11, true);
            TestAssert.assertEquals(b1.getBookingId(), adminCard.getBookingId(), "Admin must view any booking card");

            TestAssert.assertTrue(
                bookingService.listUserBookings(1).stream().anyMatch(b -> b.getBookingId() == b1.getBookingId() && b.getPayment() != null),
                "listUserBookings must include b1 with payment populated"
            );
            TestAssert.assertTrue(
                bookingService.listAllBookings().stream().anyMatch(b -> b.getBookingId() == b1.getBookingId() && b.getPayment() != null),
                "listAllBookings must include b1 with payment populated"
            );
        } finally {
            cleanupData(serviceDao, bookingDao, petDao, cleanupServiceIds, cleanupBookingIds, cleanupPetIds, 1);
        }
    }

    private static void testBookingLifecycleReactivationCancelAndCompletedLock() {
        HealthServiceDao serviceDao = new HealthServiceDao();
        HealthBookingDao bookingDao = new HealthBookingDao();
        PetDao petDao = new PetDao();
        PaymentDao paymentDao = new PaymentDao();
        HealthBookingService bookingService = new HealthBookingService(serviceDao, bookingDao, petDao, paymentDao);

        List<Integer> cleanupServiceIds = new ArrayList<>();
        List<Integer> cleanupBookingIds = new ArrayList<>();
        Set<Integer> cleanupPetIds = new LinkedHashSet<>();
        String tag = uniqueTag();

        try {
            HealthService svcInput = validTestService("Lifecycle-" + tag);
            svcInput.setDailyQuota(1);
            svcInput.setTimeSlots(" 09:00 , , 14:00, 09:00, invalid ");
            svcInput.setPrice(new BigDecimal("110000.00"));
            HealthService svc = bookingService.createService(svcInput);
            cleanupServiceIds.add(svc.getServiceId());

            LocalDate tomorrow = LocalDate.now().plusDays(1);
            String tempPetName = "LifePet-" + tag;

            // b1 books tomorrow 09:00
            HealthBooking b1 = bookingService.createBooking(
                1, svc.getServiceId(), null, "Jesika Putri", tempPetName, "Kucing", tomorrow, "09:00"
            );
            cleanupBookingIds.add(b1.getBookingId());
            if (b1.getPetId() != null) {
                cleanupPetIds.add(b1.getPetId());
            }

            // Admin rejects b1 with blank reason -> defaults to "Jadwal penuh / tidak tersedia" & payment GAGAL/failed
            bookingService.updateBookingStatus(b1.getBookingId(), "rejected", "");
            HealthBooking b1Rejected = bookingDao.findByIdWithDetails(b1.getBookingId()).orElseThrow();
            TestAssert.assertEquals("rejected", b1Rejected.getStatus(), "b1 status must become rejected");
            TestAssert.assertEquals(
                "Jadwal penuh / tidak tersedia",
                b1Rejected.getRejectionReason(),
                "Blank rejectionReason must default to 'Jadwal penuh / tidak tersedia'"
            );
            TestAssert.assertNotNull(b1Rejected.getPayment(), "b1 must retain payment");
            TestAssert.assertEquals("GAGAL", b1Rejected.getPayment().getStatus(), "Rejected b1 payment status must be GAGAL");
            TestAssert.assertEquals("failed", b1Rejected.getPayment().getPaymentStatus(), "Rejected b1 paymentStatus must be failed");

            // While b1 is rejected, slot 09:00 quota is free again -> b2 takes 09:00
            TestAssert.assertEquals(1, bookingService.getRemainingQuotaBySlot(svc.getServiceId(), tomorrow).get("09:00"), "Slot 09:00 quota must reopen when b1 is rejected");
            HealthBooking b2 = bookingService.createBooking(
                2, svc.getServiceId(), 2, "Arin Maharani", "Luna", "Anjing", tomorrow, "09:00"
            );
            cleanupBookingIds.add(b2.getBookingId());

            // Reactivating b1 to approved (or pending) MUST fail while b2 holds the slot!
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> bookingService.updateBookingStatus(b1.getBookingId(), "approved", null),
                "Reactivating rejected b1 to approved when quota is full must throw IllegalStateException"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> bookingService.updateBookingStatus(b1.getBookingId(), "pending", null),
                "Reactivating rejected b1 to pending when quota is full must throw IllegalStateException"
            );
            HealthBooking b1StillRejected = bookingDao.findByIdWithDetails(b1.getBookingId()).orElseThrow();
            TestAssert.assertEquals("rejected", b1StillRejected.getStatus(), "Failed reactivation must leave b1 rejected");
            TestAssert.assertEquals("GAGAL", b1StillRejected.getPayment().getStatus(), "Failed reactivation must leave payment GAGAL");

            // Wrong user cannot cancel b2
            TestAssert.assertThrows(
                SecurityException.class,
                () -> bookingService.cancelUserBooking(1, b2.getBookingId()),
                "Cancelling another user's booking must throw SecurityException"
            );

            // Owner cancels b2 -> cancelled, reason set, payment GAGAL/failed, terminal locked
            bookingService.cancelUserBooking(2, b2.getBookingId());
            HealthBooking b2Cancelled = bookingDao.findByIdWithDetails(b2.getBookingId()).orElseThrow();
            TestAssert.assertEquals("cancelled", b2Cancelled.getStatus(), "Cancelled b2 status must be cancelled");
            TestAssert.assertEquals(
                "Dibatalkan oleh pemilik hewan.",
                b2Cancelled.getRejectionReason(),
                "User-cancelled b2 must set standard rejectionReason"
            );
            TestAssert.assertEquals("GAGAL", b2Cancelled.getPayment().getStatus(), "Cancelled b2 payment status must be GAGAL");
            TestAssert.assertEquals("failed", b2Cancelled.getPayment().getPaymentStatus(), "Cancelled b2 paymentStatus must be failed");

            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> bookingService.cancelUserBooking(2, b2.getBookingId()),
                "Re-cancelling cancelled b2 must throw IllegalStateException"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> bookingService.updateBookingStatus(b2.getBookingId(), "approved", null),
                "Admin updating terminal cancelled b2 must throw IllegalStateException"
            );

            // Now b1 can be reactivated to approved -> rejectionReason cleared, payment reset to MENUNGGU/pending/paidAt=null
            bookingService.updateBookingStatus(b1.getBookingId(), "approved", "Diabaikan saat approve");
            HealthBooking b1Approved = bookingDao.findByIdWithDetails(b1.getBookingId()).orElseThrow();
            TestAssert.assertEquals("approved", b1Approved.getStatus(), "Reactivated b1 must become approved");
            TestAssert.assertNull(b1Approved.getRejectionReason(), "Approved b1 rejectionReason must be cleared to null");
            TestAssert.assertEquals("MENUNGGU", b1Approved.getPayment().getStatus(), "Reactivated b1 payment status must reset to MENUNGGU");
            TestAssert.assertEquals("pending", b1Approved.getPayment().getPaymentStatus(), "Reactivated b1 paymentStatus must reset to pending");
            TestAssert.assertNull(b1Approved.getPayment().getPaidAt(), "Reactivated b1 payment paidAt must be null");

            // User cannot cancel approved booking
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> bookingService.cancelUserBooking(1, b1.getBookingId()),
                "User cannot cancel non-pending (approved) booking"
            );

            // Admin marks b1 completed -> payment BERHASIL/paid/paidAt!=null, terminal locked
            bookingService.updateBookingStatus(b1.getBookingId(), "completed", null);
            HealthBooking b1Completed = bookingDao.findByIdWithDetails(b1.getBookingId()).orElseThrow();
            TestAssert.assertEquals("completed", b1Completed.getStatus(), "b1 status must become completed");
            TestAssert.assertNull(b1Completed.getRejectionReason(), "Completed b1 rejectionReason must be null");
            TestAssert.assertEquals("BERHASIL", b1Completed.getPayment().getStatus(), "Completed b1 payment status must be BERHASIL");
            TestAssert.assertEquals("paid", b1Completed.getPayment().getPaymentStatus(), "Completed b1 paymentStatus must be paid");
            TestAssert.assertNotNull(b1Completed.getPayment().getPaidAt(), "Completed b1 payment paidAt must be populated");

            // Terminal completed lock & invalid target status
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> bookingService.updateBookingStatus(b1.getBookingId(), "rejected", "Ubah paksa"),
                "Terminal completed b1 must reject transition to rejected"
            );
            TestAssert.assertThrows(
                IllegalStateException.class,
                () -> bookingService.updateBookingStatus(b1.getBookingId(), "pending", null),
                "Terminal completed b1 must reject transition to pending"
            );
            TestAssert.assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.updateBookingStatus(b1.getBookingId(), "cancelled", null),
                "Admin updateBookingStatus must reject invalid target 'cancelled'"
            );

            // Test deleteBooking removes booking + cascaded payment
            bookingService.deleteBooking(b2.getBookingId());
            cleanupBookingIds.remove(Integer.valueOf(b2.getBookingId()));
            TestAssert.assertTrue(bookingDao.findByIdWithDetails(b2.getBookingId()).isEmpty(), "Deleted booking b2 must be gone");
            try (Connection c = DatabaseConfig.getConnection()) {
                TestAssert.assertTrue(paymentDao.findByBookingId(c, b2.getBookingId()).isEmpty(), "Deleting booking must cascade delete payment");
            } catch (Exception ex) {
                throw new AssertionError("Unexpected DB error verifying payment cascade: " + ex.getMessage(), ex);
            }
        } finally {
            cleanupData(serviceDao, bookingDao, petDao, cleanupServiceIds, cleanupBookingIds, cleanupPetIds, 1);
        }
    }

    private static void testConcurrentBookingAndReactivationNoDeadlock() throws Exception {
        HealthServiceDao serviceDao = new HealthServiceDao();
        HealthBookingDao bookingDao = new HealthBookingDao();
        PetDao petDao = new PetDao();
        PaymentDao paymentDao = new PaymentDao();
        HealthBookingService bookingService = new HealthBookingService(serviceDao, bookingDao, petDao, paymentDao);

        List<Integer> cleanupServiceIds = new ArrayList<>();
        List<Integer> cleanupBookingIds = new ArrayList<>();
        Set<Integer> cleanupUser2PetIds = new LinkedHashSet<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            // Part A: Uninstrumented concurrent races with petIdOrNull = null (both threads start transactions immediately).
            for (int round = 1; round <= 3; round++) {
                String tag = uniqueTag();
                HealthService svcInput = validTestService("Conc-" + round + "-" + tag);
                svcInput.setDailyQuota(1);
                svcInput.setTimeSlots("09:00");
                HealthService svc = bookingService.createService(svcInput);
                cleanupServiceIds.add(svc.getServiceId());

                LocalDate tomorrow = LocalDate.now().plusDays(1);
                HealthBooking b1 = bookingService.createBooking(
                    1, svc.getServiceId(), 1, "Jesika Putri", "Milo", "Kucing", tomorrow, "09:00"
                );
                cleanupBookingIds.add(b1.getBookingId());
                bookingService.updateBookingStatus(b1.getBookingId(), "rejected", "Sementara ditolak");

                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                String petName2 = "ConcPet-" + round + "-" + tag;

                Future<Object> reactivateFuture = pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await(5, TimeUnit.SECONDS);
                        bookingService.updateBookingStatus(b1.getBookingId(), "approved", null);
                        return "REACTIVATED";
                    } catch (Throwable t) {
                        return t;
                    }
                });

                Future<Object> createFuture = pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await(5, TimeUnit.SECONDS);
                        return bookingService.createBooking(
                            2, svc.getServiceId(), null, "Arin Maharani", petName2, "Anjing", tomorrow, "09:00"
                        );
                    } catch (Throwable t) {
                        return t;
                    }
                });

                TestAssert.assertTrue(ready.await(5, TimeUnit.SECONDS), "Concurrent threads must reach start barrier");
                start.countDown();

                Object r1 = reactivateFuture.get(15, TimeUnit.SECONDS);
                Object r2 = createFuture.get(15, TimeUnit.SECONDS);

                int successes = 0;
                Throwable failure = null;
                if ("REACTIVATED".equals(r1)) {
                    successes++;
                } else if (r1 instanceof Throwable t) {
                    failure = t;
                }
                if (r2 instanceof HealthBooking b2) {
                    successes++;
                    cleanupBookingIds.add(b2.getBookingId());
                    if (b2.getPetId() != null) {
                        cleanupUser2PetIds.add(b2.getPetId());
                    }
                } else if (r2 instanceof Throwable t) {
                    failure = t;
                }

                TestAssert.assertEquals(1, successes, "Exactly one of concurrent reactivation/create must succeed (round " + round + ")");
                TestAssert.assertTrue(
                    failure instanceof IllegalStateException,
                    "Losing concurrent thread must throw IllegalStateException (no deadlock), got: " + failure
                );

                try (Connection c = DatabaseConfig.getConnection()) {
                    int activeCount = bookingDao.countActiveInSlot(
                        c, svc.getServiceId(), LocalDateTime.of(tomorrow, LocalTime.of(9, 0))
                    );
                    TestAssert.assertEquals(1, activeCount, "Active bookings in quota=1 slot must remain 1 (round " + round + ")");
                }
            }

            // Part B: Deterministic Ordering 1 — createBooking acquires health_service FOR UPDATE first while
            // updateBookingStatus waits behind the row lock (verifies no stale MVCC Read View oversell).
            runDeterministicCreateThenReactivateOrder(
                pool, bookingDao, petDao, paymentDao, bookingService,
                cleanupServiceIds, cleanupBookingIds, cleanupUser2PetIds, true
            );

            // Part C: Deterministic Ordering 2 — updateBookingStatus acquires health_service FOR UPDATE first while
            // createBooking (petIdOrNull = null) waits behind the row lock.
            runDeterministicCreateThenReactivateOrder(
                pool, bookingDao, petDao, paymentDao, bookingService,
                cleanupServiceIds, cleanupBookingIds, cleanupUser2PetIds, false
            );
        } finally {
            pool.shutdownNow();
            cleanupData(serviceDao, bookingDao, petDao, cleanupServiceIds, cleanupBookingIds, cleanupUser2PetIds, 2);
        }
    }

    private record LockOrderedDaos(HealthServiceDao leader, HealthServiceDao follower) {
    }

    private static LockOrderedDaos createLockOrderedDaos() {
        CountDownLatch leaderHoldingLock = new CountDownLatch(1);
        CountDownLatch followerAboutToLock = new CountDownLatch(1);

        HealthServiceDao leaderDao = new HealthServiceDao() {
            @Override
            public Optional<HealthService> findByIdForUpdate(Connection c, int id) {
                Optional<HealthService> res = super.findByIdForUpdate(c, id);
                leaderHoldingLock.countDown();
                try {
                    followerAboutToLock.await(5, TimeUnit.SECONDS);
                    // Allow follower thread to enter InnoDB row-lock wait before leader commits.
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                return res;
            }
        };

        HealthServiceDao followerDao = new HealthServiceDao() {
            @Override
            public Optional<HealthService> findByIdForUpdate(Connection c, int id) {
                try {
                    leaderHoldingLock.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                followerAboutToLock.countDown();
                return super.findByIdForUpdate(c, id);
            }
        };

        return new LockOrderedDaos(leaderDao, followerDao);
    }

    private static void runDeterministicCreateThenReactivateOrder(
            ExecutorService pool,
            HealthBookingDao bookingDao,
            PetDao petDao,
            PaymentDao paymentDao,
            HealthBookingService baseService,
            List<Integer> cleanupServiceIds,
            List<Integer> cleanupBookingIds,
            Set<Integer> cleanupUser2PetIds,
            boolean createLocksFirst) throws Exception {
        String tag = uniqueTag();
        HealthService svcInput = validTestService("Det-" + (createLocksFirst ? "C1-" : "R1-") + tag);
        svcInput.setDailyQuota(1);
        svcInput.setTimeSlots("09:00");
        HealthService svc = baseService.createService(svcInput);
        cleanupServiceIds.add(svc.getServiceId());

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        HealthBooking b1 = baseService.createBooking(
            1, svc.getServiceId(), 1, "Jesika Putri", "Milo", "Kucing", tomorrow, "09:00"
        );
        cleanupBookingIds.add(b1.getBookingId());
        baseService.updateBookingStatus(b1.getBookingId(), "rejected", "Sementara ditolak");

        LockOrderedDaos daos = createLockOrderedDaos();
        HealthBookingService createSvc = new HealthBookingService(
            createLocksFirst ? daos.leader() : daos.follower(), bookingDao, petDao, paymentDao
        );
        HealthBookingService reactivateSvc = new HealthBookingService(
            createLocksFirst ? daos.follower() : daos.leader(), bookingDao, petDao, paymentDao
        );

        String tempPetName = "DetPet-" + tag;
        Future<Object> createFuture = pool.submit(() -> {
            try {
                return createSvc.createBooking(
                    2, svc.getServiceId(), null, "Arin Maharani", tempPetName, "Anjing", tomorrow, "09:00"
                );
            } catch (Throwable t) {
                return t;
            }
        });
        Future<Object> reactivateFuture = pool.submit(() -> {
            try {
                reactivateSvc.updateBookingStatus(b1.getBookingId(), "approved", null);
                return "REACTIVATED";
            } catch (Throwable t) {
                return t;
            }
        });

        Object createResult = createFuture.get(15, TimeUnit.SECONDS);
        Object reactivateResult = reactivateFuture.get(15, TimeUnit.SECONDS);

        if (createLocksFirst) {
            TestAssert.assertTrue(
                createResult instanceof HealthBooking,
                "Leader createBooking must succeed when locking first, got: " + createResult
            );
            HealthBooking b2 = (HealthBooking) createResult;
            cleanupBookingIds.add(b2.getBookingId());
            if (b2.getPetId() != null) {
                cleanupUser2PetIds.add(b2.getPetId());
            }
            TestAssert.assertTrue(
                reactivateResult instanceof IllegalStateException,
                "Waiting reactivate must observe committed b2 (no stale MVCC Read View) and throw IllegalStateException, got: " + reactivateResult
            );
            TestAssert.assertEquals("rejected", bookingDao.findByIdWithDetails(b1.getBookingId()).orElseThrow().getStatus(), "b1 must remain rejected");
        } else {
            TestAssert.assertEquals("REACTIVATED", reactivateResult, "Leader reactivate must succeed when locking first");
            TestAssert.assertTrue(
                createResult instanceof IllegalStateException,
                "Waiting createBooking must observe reactivated b1 and throw IllegalStateException, got: " + createResult
            );
            TestAssert.assertEquals("approved", bookingDao.findByIdWithDetails(b1.getBookingId()).orElseThrow().getStatus(), "b1 must be approved");
        }

        try (Connection c = DatabaseConfig.getConnection()) {
            int activeCount = bookingDao.countActiveInSlot(
                c, svc.getServiceId(), LocalDateTime.of(tomorrow, LocalTime.of(9, 0))
            );
            TestAssert.assertEquals(1, activeCount, "Active bookings in quota=1 slot must never exceed daily_quota");
        }
    }

    private static void testConcurrentDeleteServiceAndCreateBookingNoCascadeLoss() throws Exception {
        HealthServiceDao baseServiceDao = new HealthServiceDao();
        HealthBookingDao bookingDao = new HealthBookingDao();
        PetDao petDao = new PetDao();
        PaymentDao paymentDao = new PaymentDao();
        HealthBookingService baseService = new HealthBookingService(baseServiceDao, bookingDao, petDao, paymentDao);

        List<Integer> cleanupServiceIds = new ArrayList<>();
        List<Integer> cleanupBookingIds = new ArrayList<>();
        Set<Integer> cleanupUser2PetIds = new LinkedHashSet<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            // Case 1: createBooking locks health_service first; deleteService must fail with IllegalStateException and NEVER cascade-delete the new booking.
            runDeterministicDeleteVsCreateOrder(
                pool, baseServiceDao, bookingDao, petDao, paymentDao, baseService,
                cleanupServiceIds, cleanupBookingIds, cleanupUser2PetIds, true
            );

            // Case 2: deleteService locks health_service first; createBooking must fail cleanly with IllegalArgumentException and leave zero orphan rows.
            runDeterministicDeleteVsCreateOrder(
                pool, baseServiceDao, bookingDao, petDao, paymentDao, baseService,
                cleanupServiceIds, cleanupBookingIds, cleanupUser2PetIds, false
            );
        } finally {
            pool.shutdownNow();
            cleanupData(baseServiceDao, bookingDao, petDao, cleanupServiceIds, cleanupBookingIds, cleanupUser2PetIds, 2);
        }
    }

    private static void runDeterministicDeleteVsCreateOrder(
            ExecutorService pool,
            HealthServiceDao baseServiceDao,
            HealthBookingDao bookingDao,
            PetDao petDao,
            PaymentDao paymentDao,
            HealthBookingService baseService,
            List<Integer> cleanupServiceIds,
            List<Integer> cleanupBookingIds,
            Set<Integer> cleanupUser2PetIds,
            boolean createLocksFirst) throws Exception {
        String tag = uniqueTag();
        HealthService svc = baseService.createService(validTestService("DelRace-" + tag));
        cleanupServiceIds.add(svc.getServiceId());

        LockOrderedDaos daos = createLockOrderedDaos();
        HealthBookingService createSvc = new HealthBookingService(
            createLocksFirst ? daos.leader() : daos.follower(), bookingDao, petDao, paymentDao
        );
        HealthBookingService deleteSvc = new HealthBookingService(
            createLocksFirst ? daos.follower() : daos.leader(), bookingDao, petDao, paymentDao
        );

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String tempPetName = "DelRacePet-" + tag;

        Future<Object> createFuture = pool.submit(() -> {
            try {
                return createSvc.createBooking(
                    2, svc.getServiceId(), null, "Arin Maharani", tempPetName, "Anjing", tomorrow, "09:00"
                );
            } catch (Throwable t) {
                return t;
            }
        });
        Future<Object> deleteFuture = pool.submit(() -> {
            try {
                deleteSvc.deleteService(svc.getServiceId());
                return "DELETED";
            } catch (Throwable t) {
                return t;
            }
        });

        Object createResult = createFuture.get(15, TimeUnit.SECONDS);
        Object deleteResult = deleteFuture.get(15, TimeUnit.SECONDS);

        if (createLocksFirst) {
            TestAssert.assertTrue(
                createResult instanceof HealthBooking,
                "Leader createBooking must succeed, got: " + createResult
            );
            HealthBooking booking = (HealthBooking) createResult;
            cleanupBookingIds.add(booking.getBookingId());
            if (booking.getPetId() != null) {
                cleanupUser2PetIds.add(booking.getPetId());
            }
            TestAssert.assertTrue(
                deleteResult instanceof IllegalStateException,
                "Waiting deleteService must observe committed booking and throw IllegalStateException, got: " + deleteResult
            );
            TestAssert.assertTrue(baseServiceDao.findById(svc.getServiceId()).isPresent(), "Service must survive when deleteService fails");
            TestAssert.assertTrue(bookingDao.findByIdWithDetails(booking.getBookingId()).isPresent(), "Committed booking must NOT be cascade-deleted");
        } else {
            TestAssert.assertEquals("DELETED", deleteResult, "Leader deleteService must succeed");
            cleanupServiceIds.remove(Integer.valueOf(svc.getServiceId()));
            TestAssert.assertTrue(
                createResult instanceof IllegalArgumentException,
                "Waiting createBooking on deleted service must throw IllegalArgumentException, got: " + createResult
            );
            TestAssert.assertTrue(baseServiceDao.findById(svc.getServiceId()).isEmpty(), "Deleted service must remain deleted");
        }
    }

    private static void testAdminDashboardOverview() {
        OrderDao orderDao = new OrderDao();
        DashboardDao dashboardDao = new DashboardDao();
        AdminDashboardService dashboardService = new AdminDashboardService(dashboardDao);

        AdminDashboardService.DashboardOverview overview = dashboardService.loadOverview();
        TestAssert.assertNotNull(overview, "DashboardOverview must not be null");

        DashboardStats stats = overview.stats();
        TestAssert.assertNotNull(stats, "DashboardStats must not be null");
        TestAssert.assertTrue(stats.getProductsCount() >= 10, "Seed productsCount must be >= 10");
        TestAssert.assertTrue(stats.getOrdersCount() >= 10, "Seed ordersCount must be >= 10");
        TestAssert.assertTrue(stats.getAnimalsCount() >= 10, "Seed animalsCount must be >= 10");
        TestAssert.assertTrue(stats.getAdoptionsCount() >= 10, "Seed adoptionsCount must be >= 10");
        TestAssert.assertTrue(stats.getBookingsCount() >= 10, "Seed bookingsCount must be >= 10");
        TestAssert.assertTrue(stats.getUsersCount() >= 11, "Seed customer usersCount (role='user') must be >= 11");
        TestAssert.assertNotNull(stats.getRevenue(), "Revenue must never be null");
        assertMoneyEquals(orderDao.sumRevenue(), stats.getRevenue(), "Dashboard revenue must match OrderDao.sumRevenue()");

        TestAssert.assertEquals(5, overview.recentOrders().size(), "recentOrders must return top 5 seed orders");
        TestAssert.assertEquals(5, overview.recentAdoptions().size(), "recentAdoptions must return top 5 seed adoptions");
        TestAssert.assertEquals(5, overview.recentBookings().size(), "recentBookings must return top 5 seed bookings");

        HealthBooking firstBooking = overview.recentBookings().get(0);
        TestAssert.assertNotNull(firstBooking.getServiceName(), "Recent booking must include joined serviceName");
        TestAssert.assertNotNull(firstBooking.getUserName(), "Recent booking must include joined userName");
        TestAssert.assertNotNull(firstBooking.getPayment(), "Recent booking must include joined payment");

        TestAssert.assertEquals(0, dashboardDao.findRecentBookings(0).size(), "findRecentBookings(0) must return empty list");
    }

    private static HealthService validTestService(String name) {
        HealthService s = new HealthService();
        s.setServiceName(name);
        s.setPrice(new BigDecimal("100000.00"));
        s.setDoctorName("drh. Clara Wijaya");
        s.setPracticeDays("Senin - Sabtu");
        s.setTimeSlots("09:00, 14:00");
        s.setDailyQuota(2);
        s.setDescription("Layanan pengujian sementara.");
        s.setIcon("+");
        s.setCardColor("card-green");
        return s;
    }

    private static void assertMoneyEquals(BigDecimal expected, BigDecimal actual, String message) {
        if (expected == null || actual == null || expected.compareTo(actual) != 0) {
            throw new AssertionError(message + " | expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    private static String uniqueTag() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static void cleanupData(
            HealthServiceDao serviceDao,
            HealthBookingDao bookingDao,
            PetDao petDao,
            List<Integer> serviceIds,
            List<Integer> bookingIds,
            Set<Integer> petIds,
            int petOwnerUserId) {
        for (int bookingId : bookingIds) {
            try {
                if (bookingDao.findByIdWithDetails(bookingId).isPresent()) {
                    bookingDao.delete(bookingId);
                }
            } catch (Exception ex) {
                System.err.println("[CLEANUP WARN] Failed deleting booking_id=" + bookingId + ": " + ex.getMessage());
            }
        }
        for (int serviceId : serviceIds) {
            try {
                if (serviceDao.findById(serviceId).isPresent()) {
                    serviceDao.delete(serviceId);
                }
            } catch (Exception ex) {
                System.err.println("[CLEANUP WARN] Failed deleting service_id=" + serviceId + ": " + ex.getMessage());
            }
        }
        for (int petId : petIds) {
            try {
                Optional<Pet> petOpt = petDao.findById(petId);
                if (petOpt.isPresent() && petOpt.get().getUserId() == petOwnerUserId) {
                    petDao.deleteOwnedByUser(petId, petOwnerUserId);
                }
            } catch (Exception ex) {
                System.err.println("[CLEANUP WARN] Failed deleting pet_id=" + petId + ": " + ex.getMessage());
            }
        }
    }
}
