package pawpaw.dao;

import pawpaw.config.DatabaseConfig;
import pawpaw.model.AdoptionRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Data Access Object for the {@code adoption_request} table.
 */
public class AdoptionDao {

    private static final String BASE_SELECT =
        "SELECT ar.adoption_request_id, ar.request_date, ar.status, ar.user_id, ar.animal_id, "
            + "ar.applicant_name, ar.email, ar.phone, ar.address, ar.reason, ar.admin_note, "
            + "ar.created_at, ar.updated_at, "
            + "a.nama AS animal_nama, a.species AS animal_species, a.breed AS animal_breed, "
            + "a.image AS animal_image, a.status AS animal_status, "
            + "u.name AS user_name "
            + "FROM adoption_request ar "
            + "LEFT JOIN animal a ON a.animal_id = ar.animal_id "
            + "LEFT JOIN `user` u ON u.user_id = ar.user_id ";

    private static final String LOCK_SELECT =
        "SELECT adoption_request_id, request_date, status, user_id, animal_id, "
            + "applicant_name, email, phone, address, reason, admin_note, created_at, updated_at "
            + "FROM adoption_request WHERE adoption_request_id = ? LIMIT 1 FOR UPDATE";

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    public List<AdoptionRequest> findAllWithDetails() {
        String sql = BASE_SELECT + "ORDER BY ar.request_date DESC, ar.adoption_request_id DESC";
        return queryJoinedList(sql, null, "gagal memuat semua pengajuan adopsi");
    }

    public List<AdoptionRequest> findByUserIdWithDetails(int userId) {
        if (userId <= 0) {
            return List.of();
        }
        String sql = BASE_SELECT + "WHERE ar.user_id = ? ORDER BY ar.adoption_request_id DESC";
        return queryJoinedList(sql, ps -> ps.setInt(1, userId), "gagal memuat riwayat pengajuan adopsi");
    }

    public Optional<Integer> findAnimalIdByRequestId(Connection c, int requestId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (requestId <= 0) {
            return Optional.empty();
        }
        String sql = "SELECT animal_id FROM adoption_request WHERE adoption_request_id = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt("animal_id")) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw dbError("gagal membaca ID hewan pengajuan adopsi", ex);
        }
    }

    public Optional<AdoptionRequest> findByIdForUpdate(Connection c, int requestId) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (requestId <= 0) {
            return Optional.empty();
        }
        try (PreparedStatement ps = c.prepareStatement(LOCK_SELECT)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapBaseRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw dbError("gagal mengunci data pengajuan adopsi", ex);
        }
    }

    public AdoptionRequest insert(Connection c, AdoptionRequest req) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (req == null) {
            throw new IllegalArgumentException("Data pengajuan adopsi tidak boleh null.");
        }

        LocalDateTime requestDate = req.getRequestDate() != null ? req.getRequestDate() : LocalDateTime.now();
        req.setRequestDate(requestDate);
        String status = (req.getStatus() == null || req.getStatus().isBlank()) ? "pending" : req.getStatus();
        req.setStatus(status);

        String sql = "INSERT INTO adoption_request "
            + "(request_date, status, user_id, animal_id, applicant_name, email, phone, address, reason, admin_note) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(requestDate));
            ps.setString(2, status);
            ps.setInt(3, req.getUserId());
            ps.setInt(4, req.getAnimalId());
            ps.setString(5, req.getApplicantName());
            ps.setString(6, req.getEmail());
            ps.setString(7, req.getPhone());
            ps.setString(8, req.getAddress());
            ps.setString(9, req.getReason());
            ps.setString(10, req.getAdminNote());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal menyimpan pengajuan adopsi baru.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Gagal memperoleh ID pengajuan adopsi baru.");
                }
                int generatedId = keys.getInt(1);
                req.setAdoptionRequestId(generatedId);
                return findByIdWithDetails(c, generatedId).orElse(req);
            }
        } catch (SQLException ex) {
            throw dbError("gagal menyimpan pengajuan adopsi", ex);
        }
    }

    public void updateStatusAndNote(Connection c, int requestId, String status, String adminNote) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (requestId <= 0 || status == null || status.isBlank()) {
            throw new IllegalArgumentException("Data pembaruan status pengajuan adopsi tidak valid.");
        }
        String sql = "UPDATE adoption_request SET status = ?, admin_note = ? WHERE adoption_request_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminNote);
            ps.setInt(3, requestId);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Pengajuan adopsi tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui status pengajuan adopsi", ex);
        }
    }

    public int rejectOtherPendingForAnimal(Connection c, int animalId, int excludeRequestId, String adminNote) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (animalId <= 0 || excludeRequestId <= 0) {
            throw new IllegalArgumentException("Parameter penolakan pengajuan adopsi lain tidak valid.");
        }
        String sql = "UPDATE adoption_request SET status = 'rejected', admin_note = ? "
            + "WHERE animal_id = ? AND adoption_request_id <> ? AND status = 'pending'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, adminNote);
            ps.setInt(2, animalId);
            ps.setInt(3, excludeRequestId);
            return ps.executeUpdate();
        } catch (SQLException ex) {
            throw dbError("gagal menolak pengajuan adopsi lainnya", ex);
        }
    }

    public List<AdoptionRequest> findRecent(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String sql = BASE_SELECT
            + "ORDER BY COALESCE(ar.request_date, ar.created_at) DESC, ar.adoption_request_id DESC LIMIT ?";
        return queryJoinedList(sql, ps -> ps.setInt(1, limit), "gagal memuat pengajuan adopsi terbaru");
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM adoption_request";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw dbError("gagal menghitung jumlah pengajuan adopsi", ex);
        }
    }

    private Optional<AdoptionRequest> findByIdWithDetails(Connection c, int requestId) throws SQLException {
        String sql = BASE_SELECT + "WHERE ar.adoption_request_id = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapJoinedRow(rs)) : Optional.empty();
            }
        }
    }

    private List<AdoptionRequest> queryJoinedList(String sql, StatementBinder binder, String errorAction) {
        List<AdoptionRequest> results = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapJoinedRow(rs));
                }
            }
            return results;
        } catch (SQLException ex) {
            throw dbError(errorAction, ex);
        }
    }

    private static AdoptionRequest mapBaseRow(ResultSet rs) throws SQLException {
        AdoptionRequest req = new AdoptionRequest();
        req.setAdoptionRequestId(rs.getInt("adoption_request_id"));
        req.setRequestDate(toLocalDateTime(rs.getTimestamp("request_date")));
        req.setStatus(rs.getString("status"));
        req.setUserId(rs.getInt("user_id"));
        req.setAnimalId(rs.getInt("animal_id"));
        req.setApplicantName(rs.getString("applicant_name"));
        req.setEmail(rs.getString("email"));
        req.setPhone(rs.getString("phone"));
        req.setAddress(rs.getString("address"));
        req.setReason(rs.getString("reason"));
        req.setAdminNote(rs.getString("admin_note"));
        req.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        req.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return req;
    }

    private static AdoptionRequest mapJoinedRow(ResultSet rs) throws SQLException {
        AdoptionRequest req = mapBaseRow(rs);
        req.setAnimalNama(rs.getString("animal_nama"));
        req.setAnimalSpecies(rs.getString("animal_species"));
        req.setAnimalBreed(rs.getString("animal_breed"));
        req.setAnimalImage(rs.getString("animal_image"));
        req.setAnimalStatus(rs.getString("animal_status"));
        req.setUserName(rs.getString("user_name"));
        return req;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static IllegalStateException dbError(String action, SQLException ex) {
        return new IllegalStateException("Kesalahan database (" + action + "): " + ex.getMessage(), ex);
    }
}
