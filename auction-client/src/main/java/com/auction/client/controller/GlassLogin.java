package com.auction.client.controller;

import javax.swing.*;
import java.awt.*;
import java.awt.RadialGradientPaint;
import java.io.IOException;

/**
 * Lớp chính để tạo Giao diện Đăng nhập Phong cách Glassmorphism
 */
public class GlassLogin extends JFrame {

    // Tạo các biến --> thành phương thức của class --> Instance variable
    // Mục đích:
    // - Controller truy cập được
    // - View có thể cung cấp dữ liệu
    // - Đúng mô hình MVC

// Khi ko dùng các biến là các method: !!!!
// Nếu chỉ tồn tại trong method setupLoginContents().
// Sau khi method chạy xong:
// LoginController
// hoặc class khác
// không thể truy cập được userField, passField, loginButton.

    private RoundedTextField userField;
    private RoundedPasswordField passField;
    private RoundedButton loginButton;

    public GlassLogin() {
        setTitle("Giao diện Đăng nhập Phong cách Glassmorphism");
        setSize(1024, 768); // Kích thước cửa sổ chính
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Đặt giữa màn hình
        setResizable(false); // Giữ bố cục cố định không thay đổi

        // 1. Bảng Nền (Background Panel) - Vẽ gradient và trang trí
        BackgroundPanel backgroundPanel = new BackgroundPanel();
        setContentPane(backgroundPanel);
        backgroundPanel.setLayout(null); // Sử dụng bố cục tuyệt đối để đặt lớp kính

        // 2. Lớp Kính (Glass Login Panel) - Bảng chứa các thành phần đăng nhập
        GlassPanel loginPanel = new GlassPanel();
        loginPanel.setBounds(212, 134, 600, 500); // Đặt vị trí và kích thước khung kính
        backgroundPanel.add(loginPanel);

        // 3. Thiết lập nội dung bên trong lớp kính
        setupLoginContents(loginPanel);
    }

    /**
     * Sắp xếp và thêm các thành phần (nhãn, ô nhập liệu, nút) vào khung kính
     * @param panel Khung chứa dạng lớp kính
     */
    private void setupLoginContents(JPanel panel) {
        panel.setLayout(null); // Bố cục tuyệt đối cho các thành phần bên trong lớp kính

        // Phông chữ mặc định hiện đại hơn
        String fontName = "Segoe UI";

        // Tiêu đề chính: Login
        JLabel titleLabel = new JLabel("Đăng nhập");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(fontName, Font.BOLD, 48));
        titleLabel.setBounds(185, 40, 300, 60);
        panel.add(titleLabel);

        // Nhãn "Username or email"
        JLabel userLabel = new JLabel("Tên đăng nhập hoặc emal");
        userLabel.setForeground(new Color(255, 255, 255, 220)); 
        userLabel.setFont(new Font(fontName, Font.PLAIN, 18));
        userLabel.setBounds(75, 120, 250, 25);
        panel.add(userLabel);

        // Ô nhập Username tùy chỉnh bo góc
        userField = new RoundedTextField();
        userField.setBounds(75, 155, 450, 50);
        panel.add(userField);

        // Nhãn "Password"
        JLabel passLabel = new JLabel("Mật khẩu");
        passLabel.setForeground(new Color(255, 255, 255, 220));
        passLabel.setFont(new Font(fontName, Font.PLAIN, 18));
        passLabel.setBounds(75, 220, 100, 25);
        panel.add(passLabel);

        // Liên kết "Forgot password?"
        JLabel forgotPass = new JLabel("Quên mật khẩu?");
        forgotPass.setForeground(new Color(255, 255, 255, 220));
        forgotPass.setFont(new Font(fontName, Font.PLAIN, 16));
        forgotPass.setBounds(390, 220, 150, 25);
        forgotPass.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Đổi con trỏ thành bàn tay
        panel.add(forgotPass);

        // Ô nhập Password tùy chỉnh bo góc
        passField = new RoundedPasswordField();
        passField.setBounds(75, 255, 450, 50);
        panel.add(passField);

        // Biểu tượng con mắt 
        JLabel eyeIcon = new JLabel("\uD83D\uDC41"); 
        eyeIcon.setForeground(new Color(255, 255, 255, 200));
        eyeIcon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 24)); 
        eyeIcon.setBounds(480, 265, 30, 30);
        eyeIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.add(eyeIcon);

        // Checkbox "Remember me"
        JCheckBox rememberMe = new JCheckBox("Ghi nhớ");
        rememberMe.setForeground(Color.WHITE);
        rememberMe.setFont(new Font(fontName, Font.PLAIN, 16));
        rememberMe.setOpaque(false); // Làm nền checkbox trong suốt
        rememberMe.setFocusPainted(false); // Xóa viền khi checkbox được chọn
        rememberMe.setBounds(75, 315, 200, 30);
        panel.add(rememberMe);

        // Nút "Login" màu vàng tùy chỉnh bo góc
        loginButton = new RoundedButton("Đăng nhập");
        loginButton.setBounds(75, 360, 450, 60);
        panel.add(loginButton);

        // Văn bản chân trang
        JLabel footer = new JLabel("Chưa có tài khoản? Đăng ký.");
        footer.setForeground(Color.WHITE);
        footer.setFont(new Font(fontName, Font.PLAIN, 16));
        footer.setBounds(200, 440, 300, 25);
        panel.add(footer);
    }

    /**
     * Bảng Nền tùy chỉnh: Vẽ gradient nền và trang trí các hình khối
     */
    class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Vẽ gradient nền
            GradientPaint gp = new GradientPaint(0, 0, new Color(130, 0, 150), 
                                                 0, getHeight(), new Color(50, 0, 180));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Blob lớn bên trái
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f)); 
            g2.setColor(new Color(100, 150, 255));
            g2.fillOval(-150, 100, 500, 400);

            // Quả cầu lớn bên phải
            float[] distSphereL = {0.0f, 1.0f};
            Color[] colorsSphereL = {new Color(0, 150, 255, 200), new Color(0, 50, 200, 50)};
            RadialGradientPaint rgpL = new RadialGradientPaint(getWidth() - 100, getHeight() - 100, 250, distSphereL, colorsSphereL);
            g2.setPaint(rgpL);
            g2.fillOval(getWidth() - 250, getHeight() - 250, 300, 300);

            // Quả cầu nhỏ bên phải
            float[] distSphereS = {0.0f, 1.0f};
            Color[] colorsSphereS = {new Color(50, 200, 255, 150), new Color(0, 100, 255, 50)};
            RadialGradientPaint rgpS = new RadialGradientPaint(getWidth() - 325, 225, 100, distSphereS, colorsSphereS);
            g2.setPaint(rgpS);
            g2.fillOval(getWidth() - 400, 150, 150, 150);

            // Quả cầu ở giữa phía trên
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            g2.setColor(new Color(150, 0, 255));
            g2.fillOval(getWidth() / 2 - 100, 50, 200, 200);
            
            // Trả lại composite bình thường
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }

    /**
     * Bảng Kính mờ tùy chỉnh
     */
    class GlassPanel extends JPanel {
        public GlassPanel() {
            // QUAN TRỌNG: Lệnh này giúp loại bỏ nền đặc mặc định của Panel
            setOpaque(false); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Vẽ lớp nền kính mờ (Trắng, độ trong suốt 80/255)
            // Bạn có thể tăng/giảm số 80 để điều chỉnh độ mờ
            g2.setColor(new Color(255, 255, 255, 80)); 
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40); 

            // 2. Viền lớp kính
            g2.setColor(new Color(255, 255, 255, 120)); 
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 40, 40); 
        }
    }

    /**
     * Ô nhập Username tùy chỉnh bo góc
     */
    class RoundedTextField extends JTextField {
        public RoundedTextField() {
            setOpaque(false); // Làm nền trong suốt
            setForeground(Color.WHITE); // Màu chữ
            setFont(new Font("Segoe UI", Font.PLAIN, 18));
            setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15)); 
            setCaretColor(Color.WHITE); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 60)); // Nền input trong suốt màu trắng
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15); 
            super.paintComponent(g); 
        }
    }

    /**
     * Ô nhập Password tùy chỉnh bo góc
     */
    class RoundedPasswordField extends JPasswordField {
        public RoundedPasswordField() {
            setOpaque(false); // Làm nền trong suốt
            setForeground(Color.WHITE); // Màu chữ
            setFont(new Font("Segoe UI", Font.PLAIN, 18));
            setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15)); 
            setCaretColor(Color.WHITE); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 60)); 
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15); 
            super.paintComponent(g); 
        }
    }

    /**
     * Nút Đăng nhập tùy chỉnh màu vàng bo góc
     */
    class RoundedButton extends JButton {
        public RoundedButton(String text) {
            super(text);
            setOpaque(false); 
            setContentAreaFilled(false); 
            setBorderPainted(false); 
            setFocusPainted(false); 
            setForeground(new Color(50, 50, 50)); 
            setFont(new Font("Segoe UI", Font.BOLD, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (getModel().isPressed()) {
                g2.setColor(new Color(230, 180, 0)); 
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 210, 80)); 
            } else {
                g2.setColor(new Color(255, 200, 50)); 
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); 
            super.paintComponent(g); 
        }
    }

    public String getUsername(){
        return this.userField.getText();
    }
    // public String getPassword(){
    //     return this.passField.getPassword();
    // }
    /*Hàm passField.getPassword() của Java trả về một mảng ký tự (char[]) chứ không phải chuỗi (String). 
    Do đó, bạn cần ép kiểu nó về String. */
    public String getPassword(){
        return new String(this.passField.getPassword()); // Ép sang String;
    }
    public RoundedButton getLoginButton(){
        return this.loginButton;
    }
    public void showError(String message){
        JOptionPane.showMessageDialog(this, message);
    }
    public void showMessage(String message){
        JOptionPane.showConfirmDialog(this, message);
    }

    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> { // Cú pháp (Tự nhớ);
        try {
            GlassLogin view = new GlassLogin(); // Tạo giao diện đăng nhập
            view.setVisible(true); // Cửa số hiển thị trên màn hình;

            SocketClient socketClient = new SocketClient();
            socketClient.connect();

            new LoginController(socketClient, view); // Xử lý logic login;
        
        } catch (IOException e){
            e.printStackTrace();
            }
        });
    }
}