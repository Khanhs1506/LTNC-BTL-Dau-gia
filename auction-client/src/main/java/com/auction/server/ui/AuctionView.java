package main.java.com.auction.server.ui;

/*
Hiện tại bạn đang đặt là AuctionView. 
Đây là một cái tên rất chuẩn theo mô hình MVC 
(Model-View-Controller) */

// Observer (Người quan sát)
// Chạy trên 2 IDE nó vẫn lỗi chưa fix được

import java.awt.Image;
import javax.swing.ImageIcon;

public class AuctionView extends javax.swing.JFrame {

    public AuctionView() {
        initComponents();
        loadImage();
    }

    private void loadImage() {
        try {

            String path = "/images/540d1e1ca48a10d4499b1_6d5891d672ce45ef8376cd50ff5a1ca3.jpg";

            java.net.URL imgURL = getClass().getResource(path);

            if (imgURL != null) {

                ImageIcon icon = new ImageIcon(imgURL);
                Image img = icon.getImage().getScaledInstance(200, 150, Image.SCALE_SMOOTH);

                jLabel3.setIcon(new ImageIcon(img));
                jLabel3.setText("");

            } else {

                System.out.println("Không tìm thấy ảnh: " + path);
                jLabel3.setText("Không tìm thấy ảnh");

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel6 = new javax.swing.JLabel();

        jPanel4 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();

        jPanel6 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Auction View");
        setSize(850, 700);
        setLocationRelativeTo(null);

        jPanel1.setBackground(new java.awt.Color(244, 246, 248));
        jPanel1.setLayout(null);

        jLabel1.setFont(new java.awt.Font("Arial", 1, 18));
        jLabel1.setText("Online đấu giá");
        jLabel1.setBounds(20, 10, 200, 30);

        jPanel1.add(jLabel1);

        // PANEL CHI TIẾT SẢN PHẨM

        jPanel3.setBackground(java.awt.Color.white);
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204,204,204)));
        jPanel3.setLayout(null);
        jPanel3.setBounds(20,50,800,220);

        jLabel2.setText("Chi tiết phiên đấu giá");
        jLabel2.setOpaque(true);
        jLabel2.setBounds(0,0,800,35);

        jPanel3.add(jLabel2);

        jLabel3.setBounds(20,50,200,150);
        jPanel3.add(jLabel3);

        jLabel5.setFont(new java.awt.Font("Arial",1,23));
        jLabel5.setText("Penaldo - Cry Baby");
        jLabel5.setBounds(240,50,500,40);

        jPanel3.add(jLabel5);

        jTextArea1.setEditable(false);
        jTextArea1.setLineWrap(true);
        jTextArea1.setWrapStyleWord(true);

        jTextArea1.setText("Lionel Messi (sinh 1987) là siêu sao bóng đá người Argentina, "
                + "được công nhận là một trong những cầu thủ vĩ đại nhất mọi thời đại.");

        jScrollPane1.setViewportView(jTextArea1);
        jScrollPane1.setBounds(240,90,520,80);

        jPanel3.add(jScrollPane1);

        jLabel6.setFont(new java.awt.Font("Arial",1,24));
        jLabel6.setForeground(java.awt.Color.red);
        jLabel6.setText("Thời gian còn lại: 05:00");
        jLabel6.setBounds(240,170,400,40);

        jPanel3.add(jLabel6);

        jPanel1.add(jPanel3);

        // PANEL GIÁ

        jPanel4.setBackground(java.awt.Color.white);
        jPanel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153,153,153)));
        jPanel4.setLayout(null);
        jPanel4.setBounds(20,290,800,110);

        jLabel7.setFont(new java.awt.Font("Arial",0,16));
        jLabel7.setText("Giá khởi điểm: 10,000,000 $");
        jLabel7.setBounds(20,15,400,30);

        jPanel4.add(jLabel7);

        jLabel8.setFont(new java.awt.Font("Arial",1,24));
        jLabel8.setForeground(new java.awt.Color(0,153,0));
        jLabel8.setText("Giá cao nhất: 12,500,000 $");
        jLabel8.setBounds(20,50,600,40);

        jPanel4.add(jLabel8);

        jPanel1.add(jPanel4);

        // PANEL ĐẶT GIÁ

        jPanel6.setBackground(java.awt.Color.white);
        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153,153,153)));
        jPanel6.setLayout(null);
        jPanel6.setBounds(20,420,800,80);

        jTextField1.setBounds(20,20,350,40);
        jTextField1.setToolTipText("Nhập số tiền");

        jPanel6.add(jTextField1);

        jButton1.setText("Đặt giá");
        jButton1.setBackground(new java.awt.Color(0,51,153));
        jButton1.setForeground(java.awt.Color.white);
        jButton1.setBounds(390,20,390,40);

        jButton1.addActionListener(e -> bidAction());

        jPanel6.add(jButton1);

        jPanel1.add(jPanel6);

        add(jPanel1);
    }

    private void bidAction() {

        String amount = jTextField1.getText();

        if(amount.isEmpty()){

            System.out.println("Vui lòng nhập số tiền!");

        }else{

            System.out.println("Đang gửi giá: " + amount);
            jTextField1.setText("");

        }

    }

// ===========================================================================
// UNIT TEST: Hàm main bên dưới dùng để chạy thử giao diện AuctionView độc lập.
// Vui lòng mở comment nếu muốn kiểm tra nhanh giao diện này.
// Khởi chạy chính thức của dự án nằm tại: com.auction.server.main.App.java
// ===========================================================================

    // public static void main(String args[]) {

    //     java.awt.EventQueue.invokeLater(() -> {

    //         new AuctionView().setVisible(true);

    //     });

    // }

    // VARIABLES

    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;

    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;

    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;

}