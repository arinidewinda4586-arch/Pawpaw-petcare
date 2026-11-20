package pawpaw.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the active desktop session user, admin preview mode flag, and in-memory shopping cart.
 */
public class SessionState {

    private User user;
    private boolean adminPreviewAsUser;
    private final List<CartItem> cart = new ArrayList<>();

    public void login(User user) {
        this.user = user;
        this.adminPreviewAsUser = false;
        clearCart();
    }

    public void logout() {
        this.user = null;
        this.adminPreviewAsUser = false;
        clearCart();
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public boolean isAdmin() {
        return user != null && user.isAdmin();
    }

    public boolean isAdminPreviewAsUser() {
        return isAdmin() && adminPreviewAsUser;
    }

    public void setAdminPreviewAsUser(boolean adminPreviewAsUser) {
        this.adminPreviewAsUser = isAdmin() && adminPreviewAsUser;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<CartItem> getCart() {
        return Collections.unmodifiableList(new ArrayList<>(cart));
    }

    public int getCartCount() {
        int total = 0;
        for (CartItem item : cart) {
            total += item.getQuantity();
        }
        return total;
    }

    public BigDecimal getCartSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    public void addToCart(Product product, int qty) {
        if (product == null || qty <= 0) {
            return;
        }
        for (CartItem item : cart) {
            if (item.getProduct() != null && item.getProduct().getProductId() == product.getProductId()) {
                item.setProduct(product);
                item.setQuantity(item.getQuantity() + qty);
                return;
            }
        }
        cart.add(new CartItem(product, qty));
    }

    public void updateCartQty(int productId, int qty) {
        if (qty <= 0) {
            removeFromCart(productId);
            return;
        }
        for (CartItem item : cart) {
            if (item.getProduct() != null && item.getProduct().getProductId() == productId) {
                item.setQuantity(qty);
                return;
            }
        }
    }

    public void removeFromCart(int productId) {
        cart.removeIf(item -> item.getProduct() != null && item.getProduct().getProductId() == productId);
    }

    public void clearCart() {
        cart.clear();
    }
}
