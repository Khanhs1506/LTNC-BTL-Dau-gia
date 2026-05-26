package sample;

public class UserSession {

    private static UserSession instance;
    private String username;
    private String role; // SELLER, BIDDER, ADMIN
    private boolean loggedIn = false;
    private double balance = 0.0;

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
        this.balance = 0.0; // <-- Lưu số dư sau khi đăng nhập ở đây !!! (Tạm thời cho bằng 0)
    }

    // ── Getter / Setter balance ────────────────────────────
    public double getBalance() { return balance; }

    public void setBalance(double balance) { this.balance = balance; }

    /** Cộng thêm tiền (nạp ví) */
    public void addBalance(double amount) { this.balance += amount; }

    /** Trừ tiền (đặt cọc / đấu giá) — trả false nếu không đủ số dư */
    public boolean deductBalance(double amount) {
        if (this.balance < amount) return false;
        this.balance -= amount;
        return true;
    }

    public String  getUsername() { return username; }
    public String  getRole()     { return role; }
    public boolean isLoggedIn()  { return loggedIn; }
}