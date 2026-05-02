package sample;

public class UserSession {

    private static UserSession instance;
    private String username;
    private boolean loggedIn = false;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    public void login(String username) {
        this.username  = username;
        this.loggedIn  = true;
    }

    public void logout() {
        this.username = null;
        this.loggedIn = false;
    }

    public String getUsername()  { return username; }
    public boolean isLoggedIn()  { return loggedIn; }
}