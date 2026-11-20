package pawpaw.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model mapped to the {@code health_service} table.
 */
public class HealthService {

    private int serviceId;
    private String serviceName;
    private BigDecimal price = BigDecimal.ZERO;
    private String doctorName = "drh. Clara Wijaya";
    private String practiceDays = "Senin - Sabtu";
    private String timeSlots = "09:00, 13:00, 16:00";
    private int dailyQuota = 5;
    private String description;
    private String icon = "+";
    private String cardColor = "card-green";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HealthService() {
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPracticeDays() {
        return practiceDays;
    }

    public void setPracticeDays(String practiceDays) {
        this.practiceDays = practiceDays;
    }

    public String getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(String timeSlots) {
        this.timeSlots = timeSlots;
    }

    public int getDailyQuota() {
        return dailyQuota;
    }

    public void setDailyQuota(int dailyQuota) {
        this.dailyQuota = dailyQuota;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCardColor() {
        return cardColor;
    }

    public void setCardColor(String cardColor) {
        this.cardColor = cardColor;
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
}
