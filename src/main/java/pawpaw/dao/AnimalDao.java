package pawpaw.dao;

import pawpaw.config.AppConfig;
import pawpaw.config.DatabaseConfig;
import pawpaw.model.Animal;

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
 * JDBC Data Access Object for the {@code animal} table (adoption catalog).
 */
public class AnimalDao {

    private static final String BASE_SELECT =
        "SELECT animal_id, nama, species, breed, gender, age, status, image, description, created_at, updated_at "
            + "FROM animal ";

    public List<Animal> findAll() {
        String sql = BASE_SELECT + "ORDER BY animal_id DESC";
        List<Animal> animals = new ArrayList<>();
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                animals.add(mapRow(rs));
            }
            return animals;
        } catch (SQLException ex) {
            throw dbError("gagal memuat daftar hewan adopsi", ex);
        }
    }

    public Optional<Animal> findById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }
        try (Connection c = DatabaseConfig.getConnection()) {
            return findById(c, id, false);
        } catch (SQLException ex) {
            throw dbError("gagal mencari hewan adopsi", ex);
        }
    }

    public Optional<Animal> findByIdForUpdate(Connection c, int id) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (id <= 0) {
            return Optional.empty();
        }
        try {
            return findById(c, id, true);
        } catch (SQLException ex) {
            throw dbError("gagal mengunci data hewan adopsi", ex);
        }
    }

    public Animal insert(Animal animal) {
        if (animal == null) {
            throw new IllegalArgumentException("Data hewan adopsi tidak boleh null.");
        }
        String sql = "INSERT INTO animal (nama, species, breed, gender, age, status, image, description) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String status = (animal.getStatus() == null || animal.getStatus().isBlank())
            ? "available"
            : animal.getStatus();
        String image = (animal.getImage() == null || animal.getImage().isBlank())
            ? AppConfig.DEFAULT_ANIMAL_IMAGE
            : animal.getImage();

        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, animal.getNama());
            ps.setString(2, animal.getSpecies());
            ps.setString(3, animal.getBreed());
            ps.setString(4, animal.getGender());
            ps.setString(5, animal.getAge());
            ps.setString(6, status);
            ps.setString(7, image);
            ps.setString(8, animal.getDescription());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Gagal menyimpan data hewan adopsi baru.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Gagal memperoleh ID hewan adopsi baru.");
                }
                int generatedId = keys.getInt(1);
                animal.setAnimalId(generatedId);
                return findById(c, generatedId, false).orElse(animal);
            }
        } catch (SQLException ex) {
            throw dbError("gagal menambahkan hewan adopsi", ex);
        }
    }

    public void update(Animal animal) {
        if (animal == null || animal.getAnimalId() <= 0) {
            throw new IllegalArgumentException("Data pembaruan hewan adopsi tidak valid.");
        }
        String sql = "UPDATE animal SET nama = ?, species = ?, breed = ?, gender = ?, age = ?, "
            + "status = ?, image = ?, description = ? WHERE animal_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, animal.getNama());
            ps.setString(2, animal.getSpecies());
            ps.setString(3, animal.getBreed());
            ps.setString(4, animal.getGender());
            ps.setString(5, animal.getAge());
            ps.setString(6, animal.getStatus());
            ps.setString(7, animal.getImage());
            ps.setString(8, animal.getDescription());
            ps.setInt(9, animal.getAnimalId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data hewan adopsi tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui hewan adopsi", ex);
        }
    }

    public void updateStatus(Connection c, int animalId, String status) {
        if (c == null) {
            throw new IllegalArgumentException("Koneksi database tidak boleh null.");
        }
        if (animalId <= 0 || status == null || status.isBlank()) {
            throw new IllegalArgumentException("Data pembaruan status hewan adopsi tidak valid.");
        }
        String sql = "UPDATE animal SET status = ? WHERE animal_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, animalId);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Data hewan adopsi tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal memperbarui status hewan adopsi", ex);
        }
    }

    public void delete(int animalId) {
        if (animalId <= 0) {
            throw new IllegalArgumentException("ID hewan adopsi tidak valid.");
        }
        String sql = "DELETE FROM animal WHERE animal_id = ?";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, animalId);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data hewan adopsi tidak ditemukan.");
            }
        } catch (SQLException ex) {
            throw dbError("gagal menghapus hewan adopsi", ex);
        }
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM animal";
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException ex) {
            throw dbError("gagal menghitung jumlah hewan adopsi", ex);
        }
    }

    private Optional<Animal> findById(Connection c, int id, boolean forUpdate) throws SQLException {
        String sql = BASE_SELECT + "WHERE animal_id = ? LIMIT 1" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private static Animal mapRow(ResultSet rs) throws SQLException {
        Animal animal = new Animal();
        animal.setAnimalId(rs.getInt("animal_id"));
        animal.setNama(rs.getString("nama"));
        animal.setSpecies(rs.getString("species"));
        animal.setBreed(rs.getString("breed"));
        animal.setGender(rs.getString("gender"));
        animal.setAge(rs.getString("age"));
        animal.setStatus(rs.getString("status"));
        animal.setImage(rs.getString("image"));
        animal.setDescription(rs.getString("description"));
        animal.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        animal.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return animal;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static IllegalStateException dbError(String action, SQLException ex) {
        return new IllegalStateException("Kesalahan database (" + action + "): " + ex.getMessage(), ex);
    }
}
