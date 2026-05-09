[Client] Nhập Họ tên + Username + Mật khẩu + Vai trò
    ↓
RegisterController.handleRegister()
    ↓ gửi username (không phải họ tên)
ServerConnection.register(username, password, role)
    ↓ "REGISTER==={"username":..., "password":..., "role":...}"
ClientHandler.handlerRegister()
    ↓ tạo User object đúng loại (Admin/Seller/Bidder)
UserDaoImpl.registerUser() → INSERT vào DB ✅
    ↓ "REGISTER SUCCESS"
[Client] navigateToLogin() → mở màn hình Login
    ↓
LoginController → ServerConnection.login(username, password)
    ↓ "LOGIN==={"username":..., "password":...}"
ClientHandler.handlerLogin() → parse JsonObject ✅
UserDaoImpl.getUserByUsername() → SELECT từ DB
    ↓ "LOGIN SUCCESS"
[Client] UserSession.login(username) ✅