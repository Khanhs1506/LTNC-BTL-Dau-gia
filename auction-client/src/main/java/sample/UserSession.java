package sample;

public class UserSession {

    private static UserSession instance;
    private String username;
    private String role; // SELLER, BIDDER, ADMIN
    private boolean loggedIn = false;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    // Dùng khi biết role (sau login từ server)
    public void login(String username, String role) {
        this.username = username;
        this.role     = role;
        this.loggedIn = true;
    }

    // Giữ lại để tương thích code cũ
    public void login(String username) {
        this.username = username;
        this.loggedIn = true;
    }

    public void logout() {
        this.username = null;
        this.role     = null;
        this.loggedIn = false;
    }

    public String  getUsername() { return username; }
    public String  getRole()     { return role; }
    public boolean isLoggedIn()  { return loggedIn; }
}