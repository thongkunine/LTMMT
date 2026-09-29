package client;

import common.Protocol;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.imageio.ImageIO;

public class VideoClient {

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    private volatile boolean running = false;

    private final String username;
    private final String targetUser;

    private Thread receiveThread;

    // Hàm callback để gửi hình nhận được về VideoCallFrame
    public interface VideoFrameListener {
        void onFrameReceived(BufferedImage image);
    }

    private VideoFrameListener listener;

    public VideoClient(String username, String targetUser) {
        this.username = username;
        this.targetUser = targetUser;
    }

    public void setVideoFrameListener(VideoFrameListener listener) {
        this.listener = listener;
    }

    // Kết nối tới VideoServer
    public boolean connect() {

        try {

            socket = new Socket(
                    Protocol.HOST,
                    Protocol.VIDEO_PORT
            );

            input = new DataInputStream(
                    socket.getInputStream()
            );

            output = new DataOutputStream(
                    socket.getOutputStream()
            );

            // Cho server biết tôi là ai và đang gọi ai
            output.writeUTF(username);
            output.writeUTF(targetUser);
            output.flush();

            running = true;

            startReceiving();

            System.out.println(
                    "[VideoClient] Connected: "
                    + username
                    + " -> "
                    + targetUser
            );

            return true;

        } catch (IOException e) {

            System.out.println(
                    "[VideoClient] Connect error: "
                    + e.getMessage()
            );

            return false;
        }
    }

    // Gửi một frame webcam
    public synchronized void sendFrame(BufferedImage image) {

        if (!running || image == null || output == null) {
            return;
        }

        try {

            ByteArrayOutputStream baos
                    = new ByteArrayOutputStream();

            // Nén BufferedImage thành JPEG
            ImageIO.write(image, "jpg", baos);

            byte[] imageData = baos.toByteArray();

            output.writeInt(imageData.length);
            output.write(imageData);
            output.flush();

        } catch (IOException e) {

            System.out.println(
                    "[VideoClient] Send frame error: "
                    + e.getMessage()
            );
        }
    }

    // Luồng nhận video từ người bên kia
    private void startReceiving() {

        receiveThread = new Thread(() -> {

            try {

                while (running) {

                    int length = input.readInt();

                    if (length <= 0
                            || length > 5 * 1024 * 1024) {

                        System.out.println(
                                "[VideoClient] Invalid frame: "
                                + length
                        );

                        break;
                    }

                    byte[] imageData = new byte[length];

                    input.readFully(imageData);

                    BufferedImage image = ImageIO.read(
                            new ByteArrayInputStream(imageData)
                    );

                    if (image != null && listener != null) {
                        listener.onFrameReceived(image);
                    }
                }

            } catch (IOException e) {

                if (running) {
                    System.out.println(
                            "[VideoClient] Receive error: "
                            + e.getMessage()
                    );
                }

            } finally {
                running = false;
            }
        });

        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    public void close() {

        running = false;

        try {

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException ignored) {
        }

        System.out.println(
                "[VideoClient] Disconnected: " + username
        );
    }

    public boolean isRunning() {
        return running;
    }
}