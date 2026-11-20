package pawpaw.dao;

import pawpaw.model.AdoptionRequest;
import pawpaw.model.DashboardStats;
import pawpaw.model.HealthBooking;
import pawpaw.model.Order;

import java.util.List;
import java.util.Objects;

/**
 * Aggregate Data Access Object providing Admin dashboard KPI metrics and recent activity lists.
 */
public class DashboardDao {

    private final ProductDao productDao;
    private final OrderDao orderDao;
    private final AnimalDao animalDao;
    private final AdoptionDao adoptionDao;
    private final HealthBookingDao healthBookingDao;
    private final UserDao userDao;

    public DashboardDao() {
        this(
            new ProductDao(),
            new OrderDao(),
            new AnimalDao(),
            new AdoptionDao(),
            new HealthBookingDao(),
            new UserDao()
        );
    }

    public DashboardDao(
            ProductDao productDao,
            OrderDao orderDao,
            AnimalDao animalDao,
            AdoptionDao adoptionDao,
            HealthBookingDao healthBookingDao,
            UserDao userDao) {
        this.productDao = Objects.requireNonNull(productDao, "ProductDao tidak boleh null.");
        this.orderDao = Objects.requireNonNull(orderDao, "OrderDao tidak boleh null.");
        this.animalDao = Objects.requireNonNull(animalDao, "AnimalDao tidak boleh null.");
        this.adoptionDao = Objects.requireNonNull(adoptionDao, "AdoptionDao tidak boleh null.");
        this.healthBookingDao = Objects.requireNonNull(healthBookingDao, "HealthBookingDao tidak boleh null.");
        this.userDao = Objects.requireNonNull(userDao, "UserDao tidak boleh null.");
    }

    public DashboardStats loadStats() {
        return new DashboardStats(
            productDao.countAll(),
            orderDao.countAll(),
            animalDao.countAll(),
            adoptionDao.countAll(),
            healthBookingDao.countAll(),
            userDao.countCustomers(),
            orderDao.sumRevenue()
        );
    }

    public List<Order> findRecentOrders(int limit) {
        return orderDao.findRecent(limit);
    }

    public List<AdoptionRequest> findRecentAdoptions(int limit) {
        return adoptionDao.findRecent(limit);
    }

    public List<HealthBooking> findRecentBookings(int limit) {
        return healthBookingDao.findRecent(limit);
    }
}
