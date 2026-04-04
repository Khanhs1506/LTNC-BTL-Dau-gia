import java.net.*;
import java.io.*;

public class Server {

    public static void main(String[] args) {

        try {
            ServerSocket server = new ServerSocket(9999);
            System.out.println("Server dang chay...");

            Socket socket = server.accept();
            System.out.println("Client da ket noi!");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String message;

            while ((message = in.readLine()) != null) {
                System.out.println("Client gui: " + message);

                out.println("Server da nhan: " + message);
            }

            socket.close();
            server.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}