package pawpaw.model;

import java.time.LocalDateTime;

/**
 * Domain model mapped to the {@code adoption_request} table, including joined animal and user display fields.
 */
public class AdoptionRequest {

    private int adoptionRequestId;
    private LocalDateTime requestDate;
    private String status = "pending";
    private int userId;
    private int animalId;
    private String applicantName;
    private String email;
    private String phone;
    private String address;
    private String reason;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Join display fields
    private String animalNama;
    private String animalSpecies;
    private String animalBreed;
    private String animalImage;
    private String animalStatus;
    private String userName;

    public AdoptionRequest() {
    }

    public int getAdoptionRequestId() {
        return adoptionRequestId;
    }

    public void setAdoptionRequestId(int adoptionRequestId) {
        this.adoptionRequestId = adoptionRequestId;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getAnimalId() {
        return animalId;
    }

    public void setAnimalId(int animalId) {
        this.animalId = animalId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAnimalNama() {
        return animalNama;
    }

    public void setAnimalNama(String animalNama) {
        this.animalNama = animalNama;
    }

    public String getAnimalSpecies() {
        return animalSpecies;
    }

    public void setAnimalSpecies(String animalSpecies) {
        this.animalSpecies = animalSpecies;
    }

    public String getAnimalBreed() {
        return animalBreed;
    }

    public void setAnimalBreed(String animalBreed) {
        this.animalBreed = animalBreed;
    }

    public String getAnimalImage() {
        return animalImage;
    }

    public void setAnimalImage(String animalImage) {
        this.animalImage = animalImage;
    }

    public String getAnimalStatus() {
        return animalStatus;
    }

    public void setAnimalStatus(String animalStatus) {
        this.animalStatus = animalStatus;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
