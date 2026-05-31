package com.auction.server.service;

import com.auction.server.exception.DuplicateUsernameException;
import com.auction.server.exception.InvalidCredentialsException;
import com.auction.server.exception.InvalidUserDataException;
import com.auction.server.exception.UserNotFoundException;
import com.auction.server.model.User;

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
    public synchronized boolean register(User user)
            throws InvalidUserDataException, DuplicateUsernameException {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new InvalidUserDataException("Thông tin người dùng không hợp lệ (username null hoặc rỗng)!");
        }

        // Kiểm tra xem tên đăng nhập đã tồn tại chưa
        if (users.containsKey(user.getUsername())) {
            throw new DuplicateUsernameException(user.getUsername());
        }

        // Lưu vào hệ thống
        users.put(user.getUsername(), user);
        System.out.println("[UserManager] Đăng ký thành công tài khoản: " + user.getUsername());

        return true;
    }

    // đăng nhập
    public User login(String username, String password)
            throws UserNotFoundException, InvalidCredentialsException {

        User user = users.get(username);

        // Báo lỗi nếu không thấy username
        if (user == null) {
            throw new UserNotFoundException("Không tìm thấy tài khoản '" + username + "'!");
        }

        // Kiểm tra mật khẩu
        if (!user.getPassword().equals(password)) {
            throw new InvalidCredentialsException("Sai mật khẩu cho tài khoản '" + username + "'!");
        }

        System.out.println("[UserManager] Đăng nhập thành công: " + username);
        return user;
    }

    // lấy thông tin để kiểm tra quyền như admin hay seller ,.......
    public User getUser(String username) {
        return users.get(username);
    }
}