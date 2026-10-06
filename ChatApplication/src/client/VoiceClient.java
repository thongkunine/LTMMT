package client;

import server.VoiceServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * VoiceClient: Lập trình UDP Socket 2 chiều (Vừa gửi vừa nhận):
 * - Sử dụng UDP DatagramSocket cho giao tiếp âm thanh trực tiếp thời gian thực, độ trễ cực thấp.
 * - Luồng 1 (Gửi - Recording/Sending Thread): Thu âm liên tục từ Microphone qua VoiceCallManager,
 *   đóng gói DatagramPacket và truyền lên VoiceServer.
 * - Luồng 2 (Nhận - Receiving/Buffering Thread): Nhận các gói DatagramPacket từ VoiceServer,
 *   đẩy ngay vào Bộ đệm (Jitter Buffer) của VoiceCallManager để phát ra Loa mượt mà không ngắt quãng.
 * - Luồng 3 (Heartbeat): Giữ mở cổng NAT UDP (Keep-alive) kể cả khi người dùng tắt micro.
 */
public class VoiceClient {

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private String username;
    private String targetUser;

    private volatile boolean running = false;
    private Thread recordingThread;
    private Thread receivingThread;
    private Thread heartbeatThread;

    private long sequenceNumber = 0;

    /**
     * Kết nối đến VoiceServer qua UDP và thực hiện bắt tay (gửi thông tin REGISTER)
     */
    public void connect(String host, int port, String username, String targetUser) throws IOException {
        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
        this.username = username;
        this.targetUser = targetUser;

        // Mở UDP Socket (Hệ điều hành tự động cấp phát cổng khả dụng)
        this.socket = new DatagramSocket();
        this.running = true;

        // Gửi gói tin REGISTER để VoiceServer ghi nhận IP và cổng UDP của client này
        sendRegisterPacket();
        System.out.println("[VoiceClient-UDP] Đã mở socket UDP cổng " + socket.getLocalPort() + ", kết nối đến " + host + ":" + port);
    }

    private void sendRegisterPacket() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(VoiceServer.TYPE_REGISTER);
            dos.writeUTF(username != null ? username : "");
            dos.writeUTF(targetUser != null ? targetUser : "");
            dos.flush();

            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("[VoiceClient-UDP] Lỗi gửi gói REGISTER: " + e.getMessage());
        }
    }

    /**
     * Bắt đầu 2 luồng UDP 2 chiều:
     * - Luồng Gửi: Thu âm Microphone -> UDP Socket.
     * - Luồng Nhận: UDP Socket -> Bộ đệm -> Loa.
     */
    public void startStreaming(VoiceCallManager voiceCallManager) {
        if (!running || voiceCallManager == null || socket == null || socket.isClosed()) {
            return;
        }

        // ===== LUỒNG GỬI (MICROPHONE -> UDP PACKET -> SERVER) =====
        recordingThread = new Thread(() -> {
            sequenceNumber = 0;
            while (running && !socket.isClosed() && voiceCallManager.isRunning()) {
                byte[] audioData = voiceCallManager.readMicrophone();
                if (audioData != null && audioData.length > 0 && running) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeByte(VoiceServer.TYPE_AUDIO);
                        dos.writeUTF(username != null ? username : "");
                        dos.writeUTF(targetUser != null ? targetUser : "");
                        dos.writeLong(++sequenceNumber);
                        dos.writeInt(audioData.length);
                        dos.write(audioData);
                        dos.flush();

                        byte[] data = baos.toByteArray();
                        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                        socket.send(packet);
                    } catch (IOException e) {
                        // Theo chuẩn UDP, nếu có lỗi gửi gói tin tạm thời thì bỏ qua không ngắt luồng
                    }
                } else {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }, "VoiceClient-UDPSender");
        recordingThread.setDaemon(true);
        recordingThread.start();

        // ===== LUỒNG NHẬN (SERVER -> UDP PACKET -> JITTER BUFFER -> SPEAKER) =====
        receivingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (running && !socket.isClosed() && voiceCallManager.isRunning()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    DataInputStream dis = new DataInputStream(
                        new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength())
                    );

                    byte type = dis.readByte();
                    if (type == VoiceServer.TYPE_AUDIO) {
                        String sender = dis.readUTF();
                        String target = dis.readUTF();
                        long seq = dis.readLong();
                        int length = dis.readInt();

                        // Không phát lại âm thanh của chính mình trong cuộc gọi thoại nhóm
                        if (username != null && username.equals(sender)) {
                            continue;
                        }

                        if (length > 0 && length <= 4096) {
                            byte[] audioChunk = new byte[length];
                            dis.readFully(audioChunk);

                            // Chuyển khối âm thanh vào bộ đệm (Jitter Buffer) để xử lý phát mượt mà
                            voiceCallManager.playAudio(audioChunk);
                        }
                    }
                } catch (SocketException e) {
                    // Socket đóng khi kết thúc cuộc gọi
                    break;
                } catch (IOException e) {
                    // Bỏ qua gói tin hỏng theo cơ chế UDP
                }
            }
        }, "VoiceClient-UDPReceiver");
        receivingThread.setDaemon(true);
        receivingThread.start();

        // ===== LUỒNG HEARTBEAT (DUY TRÌ KẾT NỐI NAT UDP) =====
        heartbeatThread = new Thread(() -> {
            while (running && !socket.isClosed() && voiceCallManager.isRunning()) {
                try {
                    Thread.sleep(3000);
                    if (running && socket != null && !socket.isClosed()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeByte(VoiceServer.TYPE_HEARTBEAT);
                        dos.writeUTF(username != null ? username : "");
                        dos.flush();
                        byte[] data = baos.toByteArray();
                        DatagramPacket hbPacket = new DatagramPacket(data, data.length, serverAddress, serverPort);
                        socket.send(hbPacket);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (IOException ignored) {
                }
            }
        }, "VoiceClient-UDPHeartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    /**
     * Ngắt kết nối cuộc gọi thoại và giải phóng UDP socket
     */
    public void disconnect() {
        running = false;

        // Gửi gói DISCONNECT lên server để xóa endpoint ngay lập tức
        if (socket != null && !socket.isClosed() && serverAddress != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeByte(VoiceServer.TYPE_DISCONNECT);
                dos.writeUTF(username != null ? username : "");
                dos.flush();
                byte[] data = baos.toByteArray();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
                socket.send(packet);
            } catch (Exception ignored) {
            }
        }

        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }

        if (receivingThread != null) {
            receivingThread.interrupt();
            receivingThread = null;
        }

        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
            heartbeatThread = null;
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("[VoiceClient-UDP] Đã ngắt kết nối UDP Voice.");
    }

    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }
}
