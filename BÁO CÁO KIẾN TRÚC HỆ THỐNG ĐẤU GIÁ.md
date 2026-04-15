**BÁO CÁO KIẾN TRÚC HỆ THỐNG ĐẤU GIÁ (CLASS DIAGRAM)**

Sơ đồ lớp mô tả cấu trúc tổng thể của hệ thống đấu giá trực tuyến, được chia thành hai thực thể chính là Client và Server, giao tiếp với nhau qua giao thức TCP/IP.



**1. Phân hệ Client (com.auction.client)**

Client được thiết kế theo mô hình phân lớp nhằm tách biệt giao diện, xử lý logic và kết nối mạng:



**Gói ui (View):** Chứa các lớp giao diện người dùng.



*GlassLogin:* Giao diện đăng nhập sử dụng phong cách Glassmorphism.



*AuctionView:* Giao diện chính hiển thị thông tin sản phẩm và thực hiện đặt giá (Bid).



**Gói controller:** \* *LoginController:* Đóng vai trò trung gian, nhận dữ liệu từ *GlassLogin*, điều phối qua *SocketClient* để gửi lên Server và nhận phản hồi để cập nhật giao diện.



**Gói network \& utils:**



*SocketClient:* Thành phần cốt lõi xử lý kết nối Socket, luồng vào/ra *(BufferedReader/PrintWriter)* để truyền tải dữ liệu.



*SessionManager:* Áp dụng Design Pattern Singleton để lưu trữ thông tin phiên làm việc của người dùng (Username, Role) duy nhất trong suốt quá trình ứng dụng chạy.



**2. Phân hệ Server (ServerPkg)**

Server tập trung vào quản lý dữ liệu và logic nghiệp vụ, áp dụng nhiều kỹ thuật OOP nâng cao:



Gói model (Thực thể):



**Interface** *Entity*: Định nghĩa chuẩn nhận diện (ID) cho tất cả các đối tượng trong hệ thống.



**Lớp trừu tượng** *Item:* Lớp cha cho các loại tài sản đấu giá, chứa các thuộc tính chung như giá khởi điểm và giá cao nhất hiện tại.



**Tính đa hình (Polymorphism)**: Các lớp *ArtItem (Nghệ thuật)*, *ElectronicsItem (Điện tử)*, *VehicleItem (Phương tiện)* kế thừa từ Item và triển khai hàm *printInfo()* riêng biệt.



*User \& Bid:* Lưu trữ thông tin người dùng và lịch sử các lần trả giá.



**Gói factory:**



Áp dụng **Design Pattern Factory** *(ItemFactory \& ItemFactoryImpl)*. Điều này giúp Server khởi tạo các loại sản phẩm đấu giá một cách linh hoạt mà không cần can thiệp trực tiếp vào code khởi tạo của từng lớp con, giúp hệ thống dễ dàng mở rộng thêm các loại mặt hàng mới trong tương lai.



**3. Cơ chế tương tác (Interaction)**

**Giao tiếp Client-Server:** Được thể hiện qua đường nét đứt màu đỏ cam. *SocketClient* từ phía người dùng gửi các gói tin định dạng chuỗi (Ví dụ: *LOGIN|user|pass)* đến Server để xác thực thông tin dựa trên model User.



**Mối quan hệ điều khiển:** *LoginController* giữ tham chiếu đến cả View và Network (quan hệ Aggregation) để điều phối luồng dữ liệu.



\-Minh-

