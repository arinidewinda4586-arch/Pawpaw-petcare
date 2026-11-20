package pawpaw.service;

import pawpaw.dao.DashboardDao;
import pawpaw.model.AdoptionRequest;
import pawpaw.model.DashboardStats;
import pawpaw.model.HealthBooking;
import pawpaw.model.Order;

import java.util.List;
import java.util.Objects;

/**
 * Business service aggregating live KPI counters, revenue, and recent activity tables for the Admin dashboard.
 */
public class AdminDashboardService {

    private static final int RECENT_LIMIT = 5;

    public record DashboardOverview(
        DashboardStats stats,
        List<Order> recentOrders,
        List<AdoptionRequest> recentAdoptions,
        List<HealthBooking> recentBookings
    ) {
    }

    private final DashboardDao dashboardDao;

    public AdminDashboardService() {
        this(new DashboardDao());
    }

    public AdminDashboardService(DashboardDao dashboardDao) {
        this.dashboardDao = Objects.requireNonNull(dashboardDao, "DashboardDao tidak boleh null.");
    }

    public DashboardOverview loadOverview() {
        DashboardStats stats = dashboardDao.loadStats();
        List<Order> recentOrders = List.copyOf(dashboardDao.findRecentOrders(RECENT_LIMIT));
        List<AdoptionRequest> recentAdoptions = List.copyOf(dashboardDao.findRecentAdoptions(RECENT_LIMIT));
        List<HealthBooking> recentBookings = List.copyOf(dashboardDao.findRecentBookings(RECENT_LIMIT));
        return new DashboardOverview(stats, recentOrders, recentAdoptions, recentBookings);
    }
}
