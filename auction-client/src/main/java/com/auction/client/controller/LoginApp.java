package com.auction.client.controller;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginApp extends JFrame {
    
    // Khai báo các thành phần giao diện
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    public LoginApp() {
        // 1. Cài đặt cơ bản cho Cửa sổ (Frame)
        setTitle("Đăng nhập Hệ thống");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Hiển thị ở giữa màn hình
        setLayout(new BorderLayout());

        // 2. Tạo phần nhập liệu (Panel trung tâm)
        JPanel panelCenter = new JPanel(new GridLayout(2, 2, 10, 10));
        panelCenter.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panelCenter.add(new JLabel("Tài khoản:"));
        txtUsername = new JTextField();
        panelCenter.add(txtUsername);

        panelCenter.add(new JLabel("Mật khẩu:"));
        txtPassword = new JPasswordField();
        panelCenter.add(txtPassword);

        // 3. Tạo nút bấm (Panel phía dưới)
        JPanel panelBottom = new JPanel();
        btnLogin = new JButton("Đăng nhập");
        panelBottom.add(btnLogin);

        // Thêm vào Frame chính
        add(panelCenter, BorderLayout.CENTER);
        add(panelBottom, BorderLayout.SOUTH);

        // 4. Viết CODE EVENT (Sự kiện) cho nút Đăng nhập
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                xulyDangNhap();
            }
        });
    }

    // Hàm xử lý logic khi có Event click
    private void xulyDangNhap() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // KẾT NỐI DATABASE MYSQL
        // Lưu ý: Nhớ add thư viện mysql-connector-java.jar vào Eclipse
        String dbURL = "jdbc:mysql://localhost:3306/javat4";
        String dbUser = "root"; // Tên đăng nhập MySQL của bạn
        String dbPass = "123456"; // Đổi thành mật khẩu MySQL của bạn

        try {
            Connection conn = DriverManager.getConnection(dbURL, dbUser, dbPass);
            
            // Giả sử bạn có bảng 'tai_khoan' với cột 'username' và 'password' trong DB javat4
            String sql = "SELECT * FROM tai_khoan WHERE username = ? AND password = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);
            
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                // Mở form chính ở đây (Ví dụ: new MainForm().setVisible(true); )
                // this.dispose(); // Tắt form login
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi kết nối CSDL: " + ex.getMessage(), "Lỗi Hệ thống", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Hàm Main để chạy App
    public static void main(String[] args) {
        // Tạo giao diện với Look and Feel mặc định của hệ điều hành
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginApp().setVisible(true);
            }
        });
    }
}