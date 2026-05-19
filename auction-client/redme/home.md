1. File MainHome.java (Người khởi động - The Entry Point)
Đây là cánh cửa đầu tiên để chạy ứng dụng của bạn. Nó kế thừa class Application của JavaFX.

FXMLLoader loader = new FXMLLoader(...): Dòng này có nhiệm vụ đọc file thiết kế home_demo.fxml và dịch nó thành giao diện hiển thị lên màn hình.

primaryStage.initStyle(StageStyle.UNDECORATED);: Đây là dòng code tạo ra hiệu ứng "tràn viền" rất đẹp. Nó xóa bỏ thanh tiêu đề mặc định của Windows (chứa nút X, phóng to, thu nhỏ), giúp app của bạn trông hiện đại hơn.

primaryStage.show();: Lệnh cuối cùng để mở cửa sổ ứng dụng lên.

2. File home_demo.fxml (Bộ khung giao diện - The View)
Đây là "bản vẽ kỹ thuật" của ứng dụng, được viết bằng ngôn ngữ đánh dấu XML. Thay vì chứa các thẻ tĩnh, file này giờ đây chỉ đóng vai trò làm khung chứa (Container):

fx:controller="sample.HomeController": Khai báo cực kỳ quan trọng ở dòng đầu, nó kết nối file giao diện này với "bộ não" HomeController.java. Bất cứ sự kiện nào (click nút) ở FXML đều sẽ được gửi sang Controller xử lý.

Thanh Navbar (<top>): Chứa logo và các nút bấm tĩnh. Bạn để ý các thuộc tính onAction="#handleDangKi", đây là cách FXML gọi hàm bên Java khi người dùng click chuột.

Khung tìm kiếm & Lưới thẻ (<center>):

fx:id="txtSearch": Đặt tên ID cho ô nhập liệu để bên Java có thể lấy được chữ người dùng gõ vào.

fx:id="flowPane": Đây là một chiếc hộp rỗng có tính năng tự động dàn hàng ngang. Bạn đã đặt ID cho nó để bên Java có thể "bơm" các thẻ tài sản vào đây.

3. File HomeController.java (Bộ não xử lý - The Controller)
Đây là trái tim của dự án, nơi chứa toàn bộ logic lập trình hướng đối tượng (OOP). File này làm 3 nhiệm vụ chính:

A. Quản lý dữ liệu (Model):

Bạn tạo ra class AuctionItem chứa các thuộc tính (tên, giá khởi điểm, thời gian...). Sau này, thay vì tự gõ dữ liệu bằng tay, bạn chỉ cần đọc dữ liệu từ Database (MySQL/SQL Server), tạo ra các object AuctionItem này và nhét vào List.

B. Khởi tạo và Bơm dữ liệu (initialize()):

Hàm @FXML public void initialize() là hàm đặc biệt của JavaFX. Khi file FXML vừa load xong giao diện lên màn hình, hàm này sẽ tự động chạy ngay lập tức.

Vòng lặp for (AuctionItem item : items) sẽ duyệt qua từng món hàng, gọi hàm createCard(item) để "vẽ" ra chiếc thẻ, rồi nhét nó vào flowPane.getChildren().add(...). Nhờ vậy, bạn có 100 món hàng thì nó sẽ tự vẽ ra 100 cái thẻ rải đều trên màn hình.

C. Chế tạo Thẻ Động (createCard(AuctionItem item)):

Đây là phần code "đỉnh" nhất. Thay vì dùng Scene Builder kéo thả, bạn đang dùng Java thuần để tự tạo ra các VBox, HBox, Label, Button.

Code sẽ lấy dữ liệu từ item (ví dụ item.title) để gán vào Label.

Hiệu ứng Hover (Di chuột): Dòng btnDangKi.setOnMouseEntered(...) bắt sự kiện khi chuột di vào nút đỏ, nó dùng hàm replace đổi mã màu CSS sang đỏ sậm hơn, và trả lại màu cũ khi chuột đi ra (setOnMouseExited). Giao diện trông sẽ rất mượt và có sức sống.

D. Xử lý sự kiện (Event Handlers):

Các hàm có chữ @FXML đứng trước như handleSearch(), handleDangKi() chính là đích đến của các cú click chuột từ FXML truyền sang. Tại đây, bạn có thể viết code logic (ví dụ: mở cửa sổ mới, lọc thẻ...).

Tóm lại: Luồng chạy của app bạn bây giờ là: MainHome bật lên -> Load home_demo.fxml -> Tự động gọi HomeController -> Controller lấy dữ liệu sinh ra các thẻ Card -> Bơm ngược các thẻ đó vào khung FlowPane trên màn hình.