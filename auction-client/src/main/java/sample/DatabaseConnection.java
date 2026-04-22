package sample;

import java.sql.Connection;
import java.sql.DriverManager;

// Kết nối MySQL và trả về đối tượng Connection để các phần khác CỦa chương trình sử dụng
public class DatabaseConnection {
    public Connection databaseLink; // Biến để lưu kết nối tới DB

    public Connection getConnection() {
        String databaseName = "demo_db";
        String databaseUser = "root";
        String databasePassword = "";
        String url = "jdbc:mysql://localhost/" + databaseName; // Kết nối tới MySQL chạy trên máy local

        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            databaseLink = DriverManager.getConnection(url, databaseUser, databasePassword);
        } catch (Exception e){
            e.printStackTrace();
            e.getCause();
        }

        return databaseLink;
    }
}
