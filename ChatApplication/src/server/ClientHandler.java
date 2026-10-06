package server;

import common.Message;
import common.MessageType;
import database.MessageDAO;
import database.UserDAO;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username = "Anonymous";
    private Room currentRoom;
    private final Set<Room> joinedRooms = ConcurrentHashMap.newKeySet();

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public synchronized void send(Message message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
                out.reset();
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
        Object incoming = in.readObject();

        if (!(incoming instanceof Message msg) || msg == null) {
            send(new Message(
                    MessageType.LOGIN_FAIL,
                    "server",
                    "Yêu cầu không hợp lệ!"
            ));
            return false;
        }

        // ==========================
        // ĐĂNG KÝ
        // ==========================
        if (msg.getType() == MessageType.REGISTER) {
            String uname = msg.getSender();
            String pwd = msg.getContent();
            String fullName = msg.getRoom();

            if (uname == null || uname.isBlank()
                    || pwd == null || pwd.isBlank()
                    || fullName == null || fullName.isBlank()) {

                send(new Message(
                        MessageType.REGISTER_FAIL,
                        "server",
                        "Vui lòng nhập đầy đủ thông tin!"
                ));
                return false;
            }

            uname = uname.trim();

            // Kiểm tra username đã tồn tại trong SQL Server chưa
            if (userDAO.usernameExists(uname)) {
                send(new Message(
                        MessageType.REGISTER_FAIL,
                        "server",
                        "Tên đăng nhập đã tồn tại!"
                ));
                return false;
            }

            // Lưu tài khoản vào database
            boolean ok = userDAO.register(fullName.trim(), uname, pwd);
            if (ok) {
                send(new Message(
                        MessageType.REGISTER_OK,
                        "server",
                        "Đăng ký thành công! Vui lòng đăng nhập."
                ));
            } else {
                send(new Message(
                        MessageType.REGISTER_FAIL,
                        "server",
                        "Đăng ký thất bại! Lỗi cơ sở dữ liệu."
                ));
            }
            return false;
        }

        // ==========================
        // ĐĂNG NHẬP
        // ==========================
        if (msg.getType() != MessageType.LOGIN) {
            send(new Message(
                    MessageType.LOGIN_FAIL,
                    "server",
                    "Yêu cầu không hợp lệ!"
            ));
            return false;
        }

        String name = msg.getSender();
        String password = msg.getContent();

        if (name == null || name.isBlank()
                || password == null || password.isBlank()) {

            send(new Message(
                    MessageType.LOGIN_FAIL,
                    "server",
                    "Tên đăng nhập và mật khẩu không được để trống!"
            ));
            return false;
        }

        name = name.trim();

        // Kiểm tra tài khoản bằng SQL Server
        if (!userDAO.login(name, password)) {
            send(new Message(
                    MessageType.LOGIN_FAIL,
                    "server",
                    "Sai tên đăng nhập hoặc mật khẩu!"
            ));
            return false;
        }

        // SQL đúng rồi -> kiểm tra user có đang online không
        if (!server.getUserManager().login(name, this)) {
            send(new Message(
                    MessageType.LOGIN_FAIL,
                    "server",
                    "Tài khoản này đang được đăng nhập!"
            ));
            return false;
        }

        // ==========================
        // LOGIN THÀNH CÔNG
        // ==========================
        username = name;
        currentRoom = server.getRoomManager().getDefaultRoom();
        currentRoom.join(this);
        joinedRooms.add(currentRoom);

        send(new Message(
                MessageType.LOGIN_OK,
                "server",
                "Đăng nhập thành công! Xin chào " + username,
                currentRoom.getName()
        ));

        sendChatHistory(currentRoom.getName());
        broadcastRoomList();
        broadcastOnlineUsers();

        currentRoom.broadcast(new Message(
                MessageType.SYSTEM,
                "server",
                username + " joined the room",
                currentRoom.getName()
        ));

        return true;
    }

    private void handle(Message message) {
        if (message.getSender() == null || message.getSender().isBlank()) {
            message.setSender(username);
        }

        switch (message.getType()) {
            case CHAT -> handleChat(message);
            case PRIVATE_MESSAGE -> handlePrivateMessage(message);
            case FILE_MESSAGE -> handleFileMessage(message);
            case CREATE_ROOM -> joinRoom(server.getRoomManager().getOrCreate(message.getContent()).getName());
            case JOIN_ROOM -> joinRoom(message.getContent());
            case LEAVE -> handleLeaveRoom(message);
            case CALL_REQUEST -> handleCallSignal(message);
            case CALL_ACCEPT -> handleCallSignal(message);
            case CALL_REJECT -> handleCallSignal(message);
            case CALL_END -> handleCallSignal(message);
            case VOICE_DATA -> handleVoiceData(message);
            case VIDEO_CALL_REQUEST -> handleVideoCallSignal(message);
            case VIDEO_CALL_ACCEPT -> handleVideoCallSignal(message);
            case VIDEO_CALL_REJECT -> handleVideoCallSignal(message);
            case VIDEO_CALL_END -> handleVideoCallSignal(message);
            default -> {
            }
        }
    }

    /**
     * Xử lý tín hiệu cuộc gọi thoại (CALL_REQUEST, CALL_ACCEPT, CALL_REJECT, CALL_END)
     */
    private void handleCallSignal(Message message) {
        message.setSender(username);
        message.setSentAt(LocalDateTime.now());

        String receiver = message.getReceiver();
        if (receiver == null || receiver.isBlank()) {
            String roomName = message.getRoom();
            if (roomName != null && !roomName.isBlank()) {
                Room room = server.getRoomManager().get(roomName);
                if (room != null) {
                    for (ClientHandler client : room.getMembers()) {
                        if (client != this) {
                            client.send(message);
                        }
                    }
                }
            }
            return;
        }

        ClientHandler target = server.getUserManager().getClient(receiver);
        if (target == null) {
            if (message.getType() == MessageType.CALL_REQUEST) {
                Message reject = new Message(MessageType.CALL_REJECT, receiver, "Offline");
                reject.setReceiver(username);
                send(reject);
            }
            return;
        }
        if (!receiver.equals(username)) {
            target.send(message);
        }
    }

    /**
     * Chuyển tiếp dữ liệu âm thanh qua TCP dự phòng (hỗ trợ cả 1-1 và phòng nhóm)
     */
    private void handleVoiceData(Message message) {
        message.setSender(username);
        String receiver = message.getReceiver();
        if (receiver != null && !receiver.isBlank()) {
            ClientHandler target = server.getUserManager().getClient(receiver);
            if (target != null && target != this) {
                target.send(message);
            }
            return;
        }

        // Nếu là cuộc gọi thoại nhóm trong phòng
        String roomName = message.getRoom();
        if (roomName != null && !roomName.isBlank()) {
            Room room = server.getRoomManager().get(roomName);
            if (room != null) {
                for (ClientHandler client : room.getMembers()) {
                    if (client != this) {
                        client.send(message);
                    }
                }
            }
        }
    }

    /**
     * Xử lý tín hiệu cuộc gọi video
     */
    private void handleVideoCallSignal(Message message) {
        message.setSender(username);
        message.setSentAt(LocalDateTime.now());

        String receiver = message.getReceiver();
        if (receiver == null || receiver.isBlank()) {
            String roomName = message.getRoom();
            if (roomName != null && !roomName.isBlank()) {
                Room room = server.getRoomManager().get(roomName);
                if (room != null) {
                    for (ClientHandler client : room.getMembers()) {
                        if (client != this) {
                            client.send(message);
                        }
                    }
                }
            }
            return;
        }

        ClientHandler target = server.getUserManager().getClient(receiver);
        if (target == null) {
            if (message.getType() == MessageType.VIDEO_CALL_REQUEST) {
                Message reject = new Message(MessageType.VIDEO_CALL_REJECT, receiver, "Offline");
                reject.setReceiver(username);
                send(reject);
            }
            return;
        }
        if (!receiver.equals(username)) {
            target.send(message);
        }
    }

    private void handleChat(Message message) {
        String rName = message.getRoom();
        Room targetRoom = (rName != null && !rName.isBlank()) ? server.getRoomManager().get(rName) : currentRoom;
        if (targetRoom == null) {
            return;
        }

        // Đảm bảo đây là tin nhắn CHAT
        message.setType(MessageType.CHAT);

        // Server tự xác định người gửi
        message.setSender(username);

        // Server tự xác định phòng
        message.setRoom(targetRoom.getName());

        message.setSentAt(LocalDateTime.now());
        // 1. Lưu tin nhắn vào SQL Server
        messageDAO.saveMessage(message);

        // 2. Gửi tin nhắn cho các thành viên trong phòng
        targetRoom.broadcast(message);
    }

    private void handlePrivateMessage(Message message) {
        String receiver = message.getReceiver();
        if (receiver == null || receiver.isBlank()) {
            return;
        }
        ClientHandler target = server.getUserManager().getClient(receiver);
        message.setSender(username);
        message.setSentAt(LocalDateTime.now());

        if (message.getRoom() == null || message.getRoom().isBlank()) {
            String u1 = username;
            String u2 = receiver;
            String roomKey = "PRIVATE:" + (u1.compareTo(u2) < 0 ? u1 + "_" + u2 : u2 + "_" + u1);
            message.setRoom(roomKey);
        }
        messageDAO.saveMessage(message);

        if (target != null) {
            target.send(message);
        }
        if (target != this) {
            send(message);
        }
    }

    private void handleFileMessage(Message message) {
        if (message.getFileData() == null
                || message.getFileName() == null
                || message.getFileName().isBlank()) {

            send(new Message(
                    MessageType.SYSTEM,
                    "server",
                    "File không hợp lệ."
            ));
            return;
        }

        message.setSender(username);
        message.setSentAt(LocalDateTime.now());

        String receiver = message.getReceiver();
        if (receiver == null || receiver.isBlank()) {
            String roomName = message.getRoom();
            if (roomName == null || roomName.isBlank()) {
                if (currentRoom != null) {
                    roomName = currentRoom.getName();
                    message.setRoom(roomName);
                }
            }
            Room room = roomName != null ? server.getRoomManager().get(roomName) : null;
            if (room != null) {
                room.broadcast(message);
                Message dbFileMsg = new Message(MessageType.CHAT, username, "[File] Đã gửi file: " + message.getFileName(), roomName);
                dbFileMsg.setSentAt(message.getSentAt());
                messageDAO.saveMessage(dbFileMsg);
            } else {
                send(new Message(
                        MessageType.SYSTEM,
                        "server",
                        "Không xác định được phòng nhận file."
                ));
            }
            return;
        }

        // Tìm client người nhận
        ClientHandler target = server.getUserManager().getClient(receiver);

        if (target == null) {
            send(new Message(
                    MessageType.SYSTEM,
                    "server",
                    "Người dùng " + receiver + " hiện không online."
            ));
            return;
        }

        if (message.getRoom() == null || message.getRoom().isBlank()) {
            String u1 = username;
            String u2 = receiver;
            String roomKey = "PRIVATE:" + (u1.compareTo(u2) < 0 ? u1 + "_" + u2 : u2 + "_" + u1);
            message.setRoom(roomKey);
        }
        Message dbFileMsg = new Message(MessageType.PRIVATE_MESSAGE, username, "[File] Đã gửi file: " + message.getFileName(), message.getRoom());
        dbFileMsg.setSentAt(message.getSentAt());
        messageDAO.saveMessage(dbFileMsg);

        // Gửi file cho người nhận
        target.send(message);

        // Gửi lại cho người gửi để xác nhận
        if (target != this) {
            send(message);
        }

        System.out.println(
                username
                + " gui file "
                + message.getFileName()
                + " cho "
                + receiver
        );
    }

    private void joinRoom(String roomName) {
        Room next = server.getRoomManager().getOrCreate(roomName);
        if (!joinedRooms.contains(next)) {
            next.join(this);
            joinedRooms.add(next);
            next.broadcast(new Message(
                    MessageType.SYSTEM, "server", username + " joined the room", next.getName()));
        }

        currentRoom = next;
        sendChatHistory(currentRoom.getName());
        broadcastRoomList();
        sendUserList();
    }

    private void handleLeaveRoom(Message message) {
        String roomName = message.getRoom();
        if (roomName == null || roomName.isBlank()) {
            roomName = message.getContent();
        }
        if (roomName == null || roomName.isBlank()) {
            if (currentRoom != null) {
                roomName = currentRoom.getName();
            }
        }
        if (roomName == null || roomName.isBlank()) {
            return;
        }

        Room room = server.getRoomManager().get(roomName);
        if (room != null && joinedRooms.contains(room)) {
            Message leaveSysMsg = new Message(
                    MessageType.SYSTEM, "server", username + " đã rời khỏi phòng " + roomName, roomName);
            leaveSysMsg.setSentAt(LocalDateTime.now());
            messageDAO.saveMessage(leaveSysMsg);
            room.broadcast(leaveSysMsg);

            room.leave(this);
            joinedRooms.remove(room);
            sendUserListToRoom(room);
            server.getRoomManager().removeIfEmpty(room.getName());

            if (currentRoom == room) {
                if (!joinedRooms.isEmpty()) {
                    currentRoom = joinedRooms.iterator().next();
                } else {
                    Room defaultRoom = server.getRoomManager().getDefaultRoom();
                    defaultRoom.join(this);
                    joinedRooms.add(defaultRoom);
                    currentRoom = defaultRoom;
                }
                send(new Message(MessageType.LEAVE, "server", "Bạn đã rời khỏi phòng " + roomName, currentRoom.getName()));
                sendChatHistory(currentRoom.getName());
                sendUserList();
            }
            broadcastRoomList();
        }
    }

    private void broadcastRoomList() {
        String rooms = String.join(",", server.getRoomManager().getRoomNames());
        Message list = new Message(MessageType.ROOM_LIST, "server", rooms);
        for (ClientHandler client : server.getUserManager().getClients()) {
            client.send(list);
        }
    }

    private void broadcastOnlineUsers() {
        String users = String.join(
                ",",
                server.getUserManager().getUsernames()
        );

        Message list = new Message(
                MessageType.USER_LIST,
                "server",
                users
        );

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
        for (Room r : new java.util.ArrayList<>(joinedRooms)) {
            r.leave(this);
            r.broadcast(new Message(
                    MessageType.LEAVE, "server", username + " left the chat", r.getName()));
            sendUserListToRoom(r);
            server.getRoomManager().removeIfEmpty(r.getName());
        }
        joinedRooms.clear();
        server.getUserManager().logout(username);
        broadcastOnlineUsers();

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

    private void sendChatHistory(String roomName) {
        System.out.println("=== BAT DAU LOAD LICH SU ===");
        System.out.println("Phong can load: " + roomName);

        List<Message> history = messageDAO.getMessagesByRoom(roomName);

        System.out.println(
                "So tin nhan tim thay: " + history.size()
        );

        for (Message oldMessage : history) {
            Message historyMessage = new Message(
                    MessageType.HISTORY,
                    oldMessage.getSender(),
                    oldMessage.getContent(),
                    (oldMessage.getRoom() != null && !oldMessage.getRoom().isBlank()) ? oldMessage.getRoom() : roomName
            );
            historyMessage.setSentAt(oldMessage.getSentAt());

            send(historyMessage);
        }

        System.out.println("=== KET THUC LOAD LICH SU ===");
    }
}
