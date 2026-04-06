import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerBase {
    public static void main(String[] args) {

        try {

            ServerSocket server = new ServerSocket(9999);
            System.out.println("Server dang chay...");
            
            // Giải pháp xử lý mỗi khi đăng nhập lại chạy ServerBase 1 lần nữa
            // Bọc vòng lặp vô hạn ở đây để Server không bao giờ tắt
            while (true){
                Socket socket = server.accept();
                System.out.println("Client da ket noi!");

                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String message;
            }

            while ((message = in.readLine()) != null) {

                System.out.println("Client gui: " + message);

                // xử lý login
                if (message.startsWith("LOGIN")) {

                    String[] data = message.split("\\|");

                    String username = data[1];
                    String password = data[2];

                    System.out.println("Username: " + username);
                    System.out.println("Password: " + password);

                    // kiểm tra login (demo)
                    if (username.equals("admin") && password.equals("123")) {

                        out.println("LOGIN SUCCESS");
                        System.out.println("Login thanh cong");

                    } else {

                        out.println("FAIL");
                        System.out.println("Login that bai");

                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
// sendLogin