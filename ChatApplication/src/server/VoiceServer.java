package server;

import common.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VoiceServer: Server chuyên trách chuyển tiếp dữ liệu âm thanh giữa 2 client qua TCP.
 */
public class VoiceServer {

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final Map<String, VoiceClientHandler> clients = new ConcurrentHashMap<>();

    public VoiceServer() {
        this(Protocol.VOICE_PORT);
    }

    public VoiceServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        new VoiceServer().start();
    }

    public void start() {
        running = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[VoiceServer] Started on port " + port);

            while (running && !serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                VoiceClientHandler handler = new VoiceClientHandler(socket);
                Thread thread = new Thread(handler, "VoiceHandler-" + socket.getRemoteSocketAddress());
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("[VoiceServer] Error: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }

        for (VoiceClientHandler handler : clients.values()) {
            handler.close();
        }
        clients.clear();
        System.out.println("[VoiceServer] Stopped.");
    }

    /**
     * Handler quản lý kết nối và luồng âm thanh cho từng client
     */
    public class VoiceClientHandler implements Runnable {

        private final Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private String username;
        private String targetUser;
        private volatile boolean active = false;

        public VoiceClientHandler(Socket socket) {
            this.socket = socket;
        }

        public boolean isActive() {
            return active;
        }

        @Override
        public void run() {
            try {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                // Bước bắt tay (Handshake): nhận username của client và người muốn đàm thoại
                username = in.readUTF();
                targetUser = in.readUTF();

                clients.put(username, this);
                active = true;

                System.out.println("[VoiceServer] Client connected: " + username + " -> target: " + targetUser);

                // Vòng lặp nhận dữ liệu âm thanh và chuyển tiếp cho targetUser
                byte[] buffer = new byte[2048];
                while (active && !socket.isClosed()) {
                    int length = in.readInt();
                    if (length <= 0) {
                        break;
                    }
                    if (length > buffer.length) {
                        buffer = new byte[length];
                    }

                    in.readFully(buffer, 0, length);

                    VoiceClientHandler targetHandler = clients.get(targetUser);
                    if (targetHandler != null && targetHandler.isActive()) {
                        targetHandler.sendAudio(buffer, length);
                    } else {
                        // Group voice call broadcast: send audio to all other clients in same target room
                        for (Map.Entry<String, VoiceClientHandler> entry : clients.entrySet()) {
                            String cUser = entry.getKey();
                            VoiceClientHandler h = entry.getValue();
                            if (!cUser.equals(username) && targetUser.equals(h.getTargetUser()) && h.isActive()) {
                                h.sendAudio(buffer, length);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // Client ngắt kết nối cuộc gọi
            } finally {
                close();
            }
        }

        public String getTargetUser() {
            return targetUser;
        }

        public synchronized void sendAudio(byte[] data, int length) {
            if (!active || out == null) {
                return;
            }
            try {
                out.writeInt(length);
                out.write(data, 0, length);
                out.flush();
            } catch (IOException e) {
                close();
            }
        }

        public void close() {
            active = false;
            if (username != null) {
                clients.remove(username, this);
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {
            }
            if (username != null) {
                System.out.println("[VoiceServer] Client disconnected: " + username);
            }
        }
    }
}
