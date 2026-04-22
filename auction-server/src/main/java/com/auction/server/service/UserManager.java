package com.auction.server.service;

import com.auction.server.model.User; // Giả định bạn đã có lớp User trong model
import com.auction.server.exception.UserNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class UserManager {

    // 1. Singleton Instance
    private static class Holder {
        private static final UserManager INSTANCE = new UserManager();
    }


    // Dùng ConcurrentHashMap để tránh lỗi khi nhiều Client đăng ký/đăng nhập cùng lúc
    private final Map<String, User> users;

    private UserManager() {
        users = new ConcurrentHashMap<>();

        // Sau này khi làm phần Lưu trữ File (File I/O), load dữ liệu từ file vào biến 'users' ở đây.
        // Tạm thời có thể tạo một tài khoản admin mặc định để test:
        // users.put("admin", new User("admin", "123456", "ADMIN"));
    }

    // 3. Lấy instance duy nhất
    public static UserManager getInstance() {
        return Holder.INSTANCE;
    }

// chức năng đăng ký tài khoản mới
    public synchronized boolean register(User user) throws Exception {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new Exception("Lỗi: Thông tin người dùng không hợp lệ!");
        }

        // Kiểm tra xem tên đăng nhập đã tồn tại chưa
        if (users.containsKey(user.getUsername())) {
            throw new Exception("Lỗi: Tên đăng nhập '" + user.getUsername() + "' đã tồn tại!");
        }

        // Lưu vào hệ thống
        users.put(user.getUsername(), user);
        System.out.println("[UserManager] Đăng ký thành công tài khoản: " + user.getUsername());

        return true;
    }

// đăng nhập
    public User login(String username, String password) throws UserNotFoundException {

        User user = users.get(username);

        // Báo lỗi nếu không thấy username
        if (user == null) {
            throw new UserNotFoundException("Lỗi: Không tìm thấy tài khoản '" + username + "'!");
        }

        // Kiểm tra mật khẩu
        if (!user.getPassword().equals(password)) {
            throw new UserNotFoundException("Lỗi: Sai mật khẩu cho tài khoản '" + username + "'!");
        }

        System.out.println("[UserManager] Đăng nhập thành công: " + username);
        return user;
    }

// lấy thông tin để kiểm tra quyền như admin hay seller ,.......
    public User getUser(String username) {
        return users.get(username);
    }
}