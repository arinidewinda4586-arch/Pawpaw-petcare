package pawpaw.model;

import java.math.BigDecimal;

/**
 * Aggregate counters and revenue displayed on the Admin dashboard.
 */
public class DashboardStats {

    private int productsCount;
    private int ordersCount;
    private int animalsCount;
    private int adoptionsCount;
    private int bookingsCount;
    private int usersCount;
    private BigDecimal revenue = BigDecimal.ZERO;

    public DashboardStats() {
    }

    public DashboardStats(
            int productsCount,
            int ordersCount,
            int animalsCount,
            int adoptionsCount,
            int bookingsCount,
            int usersCount,
            BigDecimal revenue) {
        this.productsCount = productsCount;
        this.ordersCount = ordersCount;
        this.animalsCount = animalsCount;
        this.adoptionsCount = adoptionsCount;
        this.bookingsCount = bookingsCount;
        this.usersCount = usersCount;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }

    public int getProductsCount() {
        return productsCount;
    }

    public void setProductsCount(int productsCount) {
        this.productsCount = productsCount;
    }

    public int getOrdersCount() {
        return ordersCount;
    }

    public void setOrdersCount(int ordersCount) {
        this.ordersCount = ordersCount;
    }

    public int getAnimalsCount() {
        return animalsCount;
    }

    public void setAnimalsCount(int animalsCount) {
        this.animalsCount = animalsCount;
    }

    public int getAdoptionsCount() {
        return adoptionsCount;
    }

    public void setAdoptionsCount(int adoptionsCount) {
        this.adoptionsCount = adoptionsCount;
    }

    public int getBookingsCount() {
        return bookingsCount;
    }

    public void setBookingsCount(int bookingsCount) {
        this.bookingsCount = bookingsCount;
    }

    public int getUsersCount() {
        return usersCount;
    }

    public void setUsersCount(int usersCount) {
        this.usersCount = usersCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }
}
