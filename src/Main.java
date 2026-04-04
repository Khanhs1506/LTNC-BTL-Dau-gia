public class Main {

    public static void main(String[] args) throws Exception {

        SocketClient socketClient = new SocketClient();
        socketClient.connect();

        socketClient.send("Hello");

        String response = socketClient.receive();
        System.out.println(response);
    }
}