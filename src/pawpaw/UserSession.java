package pawpaw;

/** Data akun yang sedang masuk ke aplikasi. */
public final class UserSession {
    private String email;
    private String name;
    private String role;
    private int userId;

    public void login(Database.Account account) {
        if (account == null) throw new IllegalArgumentException("Akun tidak valid.");
        this.userId = account.id;
        this.name = account.name;
        this.email = account.email;
        this.role = account.role;
    }

    /** Kompatibilitas untuk pemanggilan sesi lama. */
    public void login(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email belum diisi.");
        this.email = email.trim();
        this.name = email.trim();
        this.role = "user";
        this.userId = 0;
    }

    public boolean isLoggedIn() { return email != null; }
    public String getEmail() { return email == null ? "" : email; }
    public String getName() { return name == null ? "" : name; }
    public String getRole() { return role == null ? "" : role; }
    public int getUserId() { return userId; }

    public void logout() {
        email = null; name = null; role = null; userId = 0;
    }
}
