package pawpaw.dao;

import pawpaw.config.DatabaseConfig;
import pawpaw.model.Pet;

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
 * JDBC Data Access Object for the {@code pet} table.
 */
public class PetDao {

    private static final String BASE_SELECT =
        "SELECT pet_id, pet_name, species, breed, gender, age, user_id, health_status, created_at, updated_at "
            + "FROM pet ";

    public List<Pet> findByUserId(int userId) {
        if (userId <= 0) {
            return List.of();
        }
        String sql = BASE_SELECT + "WHERE user_id = ? ORDER BY pet_id DESC";
        List<Pet> pets = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pets.add(mapRow(rs));
                }
            }
            return pets;
        } catch (SQLException ex) {
            throw dbError("gagal memuat daftar hewan peliharaan", ex);
        }
    }

    public Optional<Pet> findById(int petId) {
        if (petId <= 0) {
            return Optional.empty();
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            return findById(c, petId);
        } catch (SQLException ex) {
            throw dbError("gagal mencari hewan peliharaan", ex);
        }
    }

    public Optional<Pet> findByUserAndName(Connection c, int userId, String petName) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (userId <= 0 || petName == null || petName.isBlank()) {
            return Optional.empty();
        }
        String sql = BASE_SELECT + "WHERE user_id = ? AND LOWER(pet_name) = LOWER(?) ORDER BY pet_id DESC LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, petName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw dbError("gagal mencari hewan peliharaan berdasarkan nama", ex);
        }
    }

    public Pet insert(Pet pet) {
        try (Connection c = DatabaseConfig.getConnection()) {
            return insert(c, pet);
        } catch (SQLException ex) {
            throw dbError("gagal menambahkan hewan peliharaan", ex);
        }
    }

    public Pet insert(Connection c, Pet pet) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (pet == null) {
            throw new IllegalArgumentException("Data hewan peliharaan tidak boleh null.");
        }
        String sql = "INSERT INTO pet (pet_name, species, breed, gender, age, user_id, health_status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, pet.getPetName());
            ps.setString(2, pet.getSpecies());
            ps.setString(3, pet.getBreed());
            ps.setString(4, pet.getGender());
            ps.setString(5, pet.getAge());
            ps.setInt(6, pet.getUserId());
            ps.setString(7, pet.getHealthStatus());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal menyimpan data hewan peliharaan.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Gagal memperoleh ID hewan peliharaan baru.");
                }
                int generatedId = keys.getInt(1);
                pet.setPetId(generatedId);
                return findById(c, generatedId).orElse(pet);
            }
        } catch (SQLException ex) {
            throw dbError("gagal menyimpan hewan peliharaan baru", ex);
        }
    }

    public void update(Pet pet) {
        if (pet == null || pet.getPetId() <= 0 || pet.getUserId() <= 0) {
            throw new IllegalArgumentException("Data pembaruan hewan peliharaan tidak valid.");
        }
        String sql = "UPDATE pet SET pet_name = ?, species = ?, breed = ?, gender = ?, age = ?, health_status = ? "
            + "WHERE pet_id = ? AND user_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pet.getPetName());
            ps.setString(2, pet.getSpecies());
            ps.setString(3, pet.getBreed());
            ps.setString(4, pet.getGender());
            ps.setString(5, pet.getAge());
            ps.setString(6, pet.getHealthStatus());
            ps.setInt(7, pet.getPetId());
            ps.setInt(8, pet.getUserId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data hewan peliharaan tidak ditemukan atau bukan milik Anda.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui hewan peliharaan", ex);
        }
    }

    public boolean deleteOwnedByUser(int petId, int userId) {
        if (petId <= 0 || userId <= 0) {
            return false;
        }
        String sql = "DELETE FROM pet WHERE pet_id = ? AND user_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw dbError("gagal menghapus hewan peliharaan", ex);
        }
    }

    private Optional<Pet> findById(Connection c, int petId) throws SQLException {
        String sql = BASE_SELECT + "WHERE pet_id = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, petId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private static Pet mapRow(ResultSet rs) throws SQLException {
        Pet pet = new Pet();
        pet.setPetId(rs.getInt("pet_id"));
        pet.setPetName(rs.getString("pet_name"));
        pet.setSpecies(rs.getString("species"));
        pet.setBreed(rs.getString("breed"));
        pet.setGender(rs.getString("gender"));
        pet.setAge(rs.getString("age"));
        pet.setUserId(rs.getInt("user_id"));
        pet.setHealthStatus(rs.getString("health_status"));
        pet.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        pet.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return pet;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static IllegalStateException dbError(String action, SQLException ex) {
        return new IllegalStateException("Kesalahan database (" + action + "): " + ex.getMessage(), ex);
    }
}
