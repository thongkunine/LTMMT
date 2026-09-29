package server;

import common.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class VideoServer {

    // username -> kết nối video của user
    private final ConcurrentHashMap<String, VideoConnection> clients
            = new ConcurrentHashMap<>();

    public void start() {

        try (ServerSocket serverSocket
                = new ServerSocket(Protocol.VIDEO_PORT)) {

            System.out.println(
                    "[VideoServer] Started on port "
                    + Protocol.VIDEO_PORT
            );

            while (true) {

                Socket socket = serverSocket.accept();

                Thread thread = new Thread(() -> {
                    handleClient(socket);
                });

                thread.setDaemon(true);
                thread.start();
            }

        } catch (IOException e) {
            System.out.println(
                    "[VideoServer] Error: " + e.getMessage()
            );
        }
    }

    private void handleClient(Socket socket) {

        String username = null;

        try {

            DataInputStream input
                    = new DataInputStream(socket.getInputStream());

            DataOutputStream output
                    = new DataOutputStream(socket.getOutputStream());

            // Client gửi username và người đang gọi
            username = input.readUTF();
            String targetUser = input.readUTF();

            VideoConnection connection
                    = new VideoConnection(socket, output, targetUser);

            clients.put(username, connection);

            System.out.println(
                    "[VideoServer] Client connected: "
                    + username
                    + " -> target: "
                    + targetUser
            );

            while (true) {

                // Đọc kích thước frame
                int frameLength = input.readInt();

                // Kiểm tra dữ liệu bất thường
                if (frameLength <= 0
                        || frameLength > 5 * 1024 * 1024) {

                    System.out.println(
                            "[VideoServer] Invalid frame size: "
                            + frameLength
                    );

                    break;
                }

                // Đọc JPEG
                byte[] frameData = new byte[frameLength];
                input.readFully(frameData);
                // Tìm người nhận
                VideoConnection target
                    = clients.get(targetUser);

               if (target != null) {
                 target.sendFrame(frameData);
                }
                    
            }

        } catch (IOException e) {

            System.out.println(
                    "[VideoServer] Client disconnected: "
                    + username
            );

        } finally {

            if (username != null) {
                clients.remove(username);
            }

            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static class VideoConnection {

        private final Socket socket;
        private final DataOutputStream output;
        private final String targetUser;

        public VideoConnection(
                Socket socket,
                DataOutputStream output,
                String targetUser) {

            this.socket = socket;
            this.output = output;
            this.targetUser = targetUser;
        }

        public synchronized void sendFrame(byte[] frameData) {

            try {

                output.writeInt(frameData.length);
                output.write(frameData);
                output.flush();

            } catch (IOException e) {

                System.out.println(
                        "[VideoServer] Send frame error: "
                        + e.getMessage()
                );
            }
        }
    }
}