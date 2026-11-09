package pawpaw;

/** Sesi dummy di memori. Tidak menyimpan kata sandi atau akun permanen. */
public final class UserSession {
    private String email;

    public void login(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email belum diisi.");
        this.email = email.trim();
    }

    public boolean isLoggedIn() {
        return email != null;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public void logout() {
        email = null;
    }
}
