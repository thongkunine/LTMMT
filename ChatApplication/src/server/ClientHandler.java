package server;

import common.Message;
import common.MessageType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username = "Anonymous";
    private Room currentRoom;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public void send(Message message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Cannot send to " + username + ": " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            if (!handleLogin()) {
                return;
            }

            Object incoming;
            while ((incoming = in.readObject()) != null) {
                if (incoming instanceof Message message) {
                    handle(message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(username + " disconnected: " + e.getMessage());
        } finally {
            close();
        }
    }

    private boolean handleLogin() throws IOException, ClassNotFoundException {
        Message login = (Message) in.readObject();
        if (login == null || login.getType() != MessageType.LOGIN) {
            send(new Message(MessageType.LOGIN_FAIL, "server", "Invalid login request"));
            return false;
        }

        String name = login.getSender();
        if (!server.getUserManager().login(name, this)) {
            send(new Message(MessageType.LOGIN_FAIL, "server", "Username is empty or already taken"));
            return false;
        }

        username = name.trim();
        currentRoom = server.getRoomManager().getDefaultRoom();
        currentRoom.join(this);

        send(new Message(MessageType.LOGIN_OK, "server", "Welcome " + username, currentRoom.getName()));
        broadcastRoomList();
        sendUserList();
        currentRoom.broadcast(new Message(
                MessageType.SYSTEM, "server", username + " joined the room", currentRoom.getName()));
        return true;
    }

    private void handle(Message message) {
        if (message.getSender() == null || message.getSender().isBlank()) {
            message.setSender(username);
        }

        switch (message.getType()) {
            case CHAT -> handleChat(message);
            case CREATE_ROOM -> joinRoom(server.getRoomManager().getOrCreate(message.getContent()).getName());
            case JOIN_ROOM -> joinRoom(message.getContent());
            default -> {
            }
        }
    }

    private void handleChat(Message message) {
        if (currentRoom == null) {
            return;
        }
        message.setType(MessageType.CHAT);
        message.setRoom(currentRoom.getName());
        currentRoom.broadcast(message);
    }

    private void joinRoom(String roomName) {
        Room next = server.getRoomManager().getOrCreate(roomName);
        if (currentRoom == next) {
            return;
        }

        if (currentRoom != null) {
            currentRoom.leave(this);
            currentRoom.broadcast(new Message(
                    MessageType.SYSTEM, "server", username + " left the room", currentRoom.getName()));
            sendUserListToRoom(currentRoom);
            server.getRoomManager().removeIfEmpty(currentRoom.getName());
        }

        currentRoom = next;
        currentRoom.join(this);
        broadcastRoomList();
        sendUserList();
        currentRoom.broadcast(new Message(
                MessageType.SYSTEM, "server", username + " joined the room", currentRoom.getName()));
    }

    private void broadcastRoomList() {
        String rooms = String.join(",", server.getRoomManager().getRoomNames());
        Message list = new Message(MessageType.ROOM_LIST, "server", rooms);
        for (ClientHandler client : server.getUserManager().getClients()) {
            client.send(list);
        }
    }

    private void sendUserList() {
        sendUserListToRoom(currentRoom);
    }

    private void sendUserListToRoom(Room room) {
        if (room == null) {
            return;
        }
        String users = room.getMemberNames().stream().collect(Collectors.joining(","));
        Message list = new Message(MessageType.USER_LIST, "server", users, room.getName());
        room.broadcast(list);
    }

    private void close() {
        if (currentRoom != null) {
            currentRoom.leave(this);
            currentRoom.broadcast(new Message(
                    MessageType.LEAVE, "server", username + " left the chat", currentRoom.getName()));
            sendUserListToRoom(currentRoom);
            server.getRoomManager().removeIfEmpty(currentRoom.getName());
        }
        server.getUserManager().logout(username);

        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing " + username + ": " + e.getMessage());
        }
    }
}
