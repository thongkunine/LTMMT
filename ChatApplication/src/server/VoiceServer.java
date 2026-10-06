package server;

import common.Protocol;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VoiceServer: UDP Relay Server xử lý truyền và chuyển tiếp âm thanh 2 chiều thời gian thực giữa các client.
 * Lập trình UDP Socket 2 chiều:
 * - Nhận các gói tin DatagramPacket từ client (vừa nhận).
 * - Chuyển tiếp ngay lập tức đến đích (vừa gửi) mà không chờ xác nhận, chấp nhận mất gói tin (UDP Packet Loss)
 *   để đảm bảo độ trễ thấp nhất (Real-time Latency).
 * Hỗ trợ cả cuộc gọi riêng 1-1 và cuộc gọi thoại nhóm (Room).
 */
public class VoiceServer {

    // Mã định danh loại gói tin UDP
    public static final byte TYPE_REGISTER = 1;
    public static final byte TYPE_AUDIO = 2;
    public static final byte TYPE_DISCONNECT = 3;
    public static final byte TYPE_HEARTBEAT = 4;

    private final int port;
    private DatagramSocket socket;
    private volatile boolean running = false;
    private final ConcurrentHashMap<String, VoiceClientEndpoint> clients = new ConcurrentHashMap<>();
    private Thread cleanupThread;

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
            socket = new DatagramSocket(port);
            System.out.println("[VoiceServer-UDP] Started on UDP port " + port);

            // Luồng dọn dẹp các endpoint không hoạt động quá 30 giây
            cleanupThread = new Thread(this::cleanupStaleEndpoints, "VoiceServer-UDP-Cleanup");
            cleanupThread.setDaemon(true);
            cleanupThread.start();

            byte[] buffer = new byte[4096];
            while (running && !socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Xử lý gói tin nhận được từ UDP socket
                handleIncomingPacket(packet);
            }
        } catch (SocketException e) {
            if (running) {
                System.out.println("[VoiceServer-UDP] Socket error: " + e.getMessage());
            }
        } catch (IOException e) {
            if (running) {
                System.out.println("[VoiceServer-UDP] IO error: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    private void handleIncomingPacket(DatagramPacket packet) {
        try {
            DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength())
            );

            byte type = in.readByte();
            switch (type) {
                case TYPE_REGISTER -> {
                    String username = in.readUTF();
                    String targetUser = in.readUTF();
                    VoiceClientEndpoint endpoint = new VoiceClientEndpoint(
                        username, packet.getAddress(), packet.getPort(), targetUser, System.currentTimeMillis()
                    );
                    clients.put(username, endpoint);
                    System.out.println("[VoiceServer-UDP] Client da dang ky: " + username
                        + " (IP: " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ") -> Đích: " + targetUser);

                    // Gửi gói tin ACK phản hồi cho client
                    sendAck(endpoint);
                }
                case TYPE_AUDIO -> {
                    String sender = in.readUTF();
                    String target = in.readUTF();
                    long seq = in.readLong();
                    int audioLen = in.readInt();

                    // Cập nhật lại IP, port và thời gian hoạt động của người gửi (NAT keep-alive)
                    VoiceClientEndpoint senderEndpoint = clients.get(sender);
                    if (senderEndpoint != null) {
                        senderEndpoint.address = packet.getAddress();
                        senderEndpoint.port = packet.getPort();
                        senderEndpoint.lastActiveTime = System.currentTimeMillis();
                    } else {
                        senderEndpoint = new VoiceClientEndpoint(
                            sender, packet.getAddress(), packet.getPort(), target, System.currentTimeMillis()
                        );
                        clients.put(sender, senderEndpoint);
                    }

                    // Chuyển tiếp gói tin âm thanh sang cho đối phương (UDP Forwarding)
                    forwardAudio(packet.getData(), packet.getOffset(), packet.getLength(), sender, target);
                }
                case TYPE_HEARTBEAT -> {
                    String username = in.readUTF();
                    VoiceClientEndpoint ep = clients.get(username);
                    if (ep != null) {
                        ep.address = packet.getAddress();
                        ep.port = packet.getPort();
                        ep.lastActiveTime = System.currentTimeMillis();
                    }
                }
                case TYPE_DISCONNECT -> {
                    String username = in.readUTF();
                    if (username != null) {
                        clients.remove(username);
                        System.out.println("[VoiceServer-UDP] Client disconected: " + username);
                    }
                }
                default -> {
                }
            }
        } catch (IOException e) {
            // Gói tin bị lỗi cấu trúc, bỏ qua theo cơ chế UDP
        }
    }

    private void forwardAudio(byte[] data, int offset, int length, String sender, String target) {
        if (socket == null || socket.isClosed()) {
            return;
        }

        // 1. Nếu đích đến là 1 user cụ thể (Cuộc gọi thoại 1-1)
        VoiceClientEndpoint targetEndpoint = clients.get(target);
        if (targetEndpoint != null) {
            try {
                DatagramPacket outPacket = new DatagramPacket(
                    data, offset, length, targetEndpoint.address, targetEndpoint.port
                );
                socket.send(outPacket);
            } catch (IOException ignored) {
            }
            return;
        }

        // 2. Nếu đích đến là phòng chat nhóm (Group Voice Call)
        for (Map.Entry<String, VoiceClientEndpoint> entry : clients.entrySet()) {
            String clientUser = entry.getKey();
            VoiceClientEndpoint ep = entry.getValue();
            // Chuyển tiếp âm thanh đến tất cả thành viên trong cùng phòng ngoại trừ người đang nói
            if (!clientUser.equals(sender) && target.equals(ep.targetUser)) {
                try {
                    DatagramPacket outPacket = new DatagramPacket(
                        data, offset, length, ep.address, ep.port
                    );
                    socket.send(outPacket);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void sendAck(VoiceClientEndpoint endpoint) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(TYPE_HEARTBEAT);
            dos.writeUTF("SERVER");
            dos.flush();
            byte[] bytes = baos.toByteArray();
            DatagramPacket ack = new DatagramPacket(bytes, bytes.length, endpoint.address, endpoint.port);
            socket.send(ack);
        } catch (IOException ignored) {
        }
    }

    private void cleanupStaleEndpoints() {
        while (running) {
            try {
                Thread.sleep(10000);
                long now = System.currentTimeMillis();
                clients.entrySet().removeIf(entry -> {
                    boolean stale = (now - entry.getValue().lastActiveTime) > 30000;
                    if (stale) {
                        System.out.println("[VoiceServer-UDP] Xoa endpoint het han: " + entry.getKey());
                    }
                    return stale;
                });
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void stop() {
        running = false;
        if (cleanupThread != null) {
            cleanupThread.interrupt();
            cleanupThread = null;
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        clients.clear();
        System.out.println("[VoiceServer-UDP] Stop server.");
    }

    /**
     * Thông tin endpoint của mỗi client kết nối qua UDP
     */
    public static class VoiceClientEndpoint {
        public final String username;
        public volatile InetAddress address;
        public volatile int port;
        public final String targetUser;
        public volatile long lastActiveTime;

        public VoiceClientEndpoint(String username, InetAddress address, int port, String targetUser, long lastActiveTime) {
            this.username = username;
            this.address = address;
            this.port = port;
            this.targetUser = targetUser;
            this.lastActiveTime = lastActiveTime;
        }
    }
}
