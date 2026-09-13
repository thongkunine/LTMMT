package server;

import common.Protocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {

    private final UserManager userManager = new UserManager();
    private final RoomManager roomManager = new RoomManager();

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
            System.out.println("Chat server started on port " + Protocol.PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket, this);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
