import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.Principal;

public class SocketClient {
    protected Socket socket;
    protected PrintWriter out;
    protected BufferedReader in;

    public SocketClient(){}

    public SocketClient(Socket socket, PrintWriter out, BufferedReader in) {
        this.socket = socket;
        this.out = out;
        this.in = in;
    }

    // Phương thức kết nối với server
    public void connect() throws IOException{
        socket = new Socket("localhost", 9999);

        out = new PrintWriter(socket.getOutputStream(), true);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    }

    //  Phương thức gửi và nhập dữ liệu
    public void send(String message){
        System.out.println(message);
    }
    public String receive() throws IOException{
        return in.readLine();
    }
}
// out = new PrintWriter(new FileWriter(""));
