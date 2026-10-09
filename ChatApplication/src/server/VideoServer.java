package server;

import common.Protocol;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VideoServer {

    // Loại packet
    private static final byte TYPE_REGISTER = 1;
    private static final byte TYPE_VIDEO = 2;
    private static final byte TYPE_DISCONNECT = 3;
    private static final byte TYPE_HEARTBEAT = 4;

    // username -> endpoint UDP
    private final ConcurrentHashMap<String, VideoEndpoint> clients
            = new ConcurrentHashMap<>();

    private DatagramSocket socket;
    private volatile boolean running = false;

    public void start() {

        try {

            socket = new DatagramSocket(Protocol.VIDEO_PORT);

            running = true;

            System.out.println(
                    "[VideoServer-UDP] Started on UDP port "
                    + Protocol.VIDEO_PORT
            );

            /*
             * Mỗi UDP packet của video chỉ khoảng ~1400 bytes.
             * Dùng buffer lớn hơn để đọc packet.
             */
            byte[] buffer = new byte[2048];

            while (running) {

                DatagramPacket packet =
                        new DatagramPacket(
                                buffer,
                                buffer.length
                        );

                socket.receive(packet);

                handlePacket(packet);
            }

        } catch (IOException e) {

            if (running) {
                System.out.println(
                        "[VideoServer-UDP] Error: "
                        + e.getMessage()
                );
            }
        }
    }

    private void handlePacket(DatagramPacket packet) {

        try {

            ByteArrayInputStream bais =
                    new ByteArrayInputStream(
                            packet.getData(),
                            packet.getOffset(),
                            packet.getLength()
                    );

            DataInputStream input =
                    new DataInputStream(bais);

            byte type = input.readByte();

            switch (type) {

                case TYPE_REGISTER:
                    handleRegister(
                            input,
                            packet.getAddress(),
                            packet.getPort()
                    );
                    break;

                case TYPE_VIDEO:
                    handleVideo(packet, input);
                    break;

                case TYPE_DISCONNECT:
                    handleDisconnect(input);
                    break;

                case TYPE_HEARTBEAT:
                    handleHeartbeat(
                            input,
                            packet.getAddress(),
                            packet.getPort()
                    );
                    break;

                default:
                    System.out.println(
                            "[VideoServer-UDP] Unknown packet type: "
                            + type
                    );
            }

        } catch (IOException e) {

            System.out.println(
                    "[VideoServer-UDP] Packet error: "
                    + e.getMessage()
            );
        }
    }

    private void handleRegister(
            DataInputStream input,
            InetAddress address,
            int port
    ) throws IOException {

        String username = input.readUTF();
        String targetUser = input.readUTF();

        VideoEndpoint endpoint =
                new VideoEndpoint(
                        username,
                        targetUser,
                        address,
                        port
                );

        clients.put(username, endpoint);

        System.out.println(
                "[VideoServer-UDP] Registered: "
                + username
                + " -> "
                + targetUser
                + " | "
                + address.getHostAddress()
                + ":"
                + port
        );
    }

    /*
     * Server không cần ghép frame.
     *
     * Server chỉ relay từng packet UDP sang người nhận.
     * Việc ghép lại thành JPEG sẽ do VideoClient thực hiện.
     */
    private void handleVideo(
            DatagramPacket originalPacket,
            DataInputStream input
    ) throws IOException {

        /*
         * Header:
         *
         * byte   type
         * UTF    sender
         * UTF    target
         * long   frameId
         * int    packetIndex
         * int    totalPackets
         * int    dataLength
         * byte[] data
         */

        String sender = input.readUTF();
        String targetUser = input.readUTF();

        long frameId = input.readLong();
        int packetIndex = input.readInt();
        int totalPackets = input.readInt();
        int dataLength = input.readInt();

        // Cập nhật endpoint người gửi
        VideoEndpoint senderEndpoint = clients.get(sender);

        if (senderEndpoint != null) {
            senderEndpoint.lastActiveTime =
                    System.currentTimeMillis();
        }

        // Gọi 1-1
        VideoEndpoint target =
                clients.get(targetUser);

        if (target != null) {

            forwardPacket(
                    originalPacket,
                    target
            );

            return;
        }

        /*
         * Nếu target không phải username
         * thì coi target là room.
         *
         * Broadcast cho những client cùng room.
         */
        for (Map.Entry<String, VideoEndpoint> entry
                : clients.entrySet()) {

            String clientUsername =
                    entry.getKey();

            VideoEndpoint endpoint =
                    entry.getValue();

            if (!clientUsername.equals(sender)
                    && targetUser.equals(
                            endpoint.targetUser
                    )) {

                forwardPacket(
                        originalPacket,
                        endpoint
                );
            }
        }
    }

    private void forwardPacket(
            DatagramPacket originalPacket,
            VideoEndpoint target
    ) throws IOException {

        DatagramPacket forward =
                new DatagramPacket(
                        originalPacket.getData(),
                        originalPacket.getOffset(),
                        originalPacket.getLength(),
                        target.address,
                        target.port
                );

        socket.send(forward);
    }

    private void handleDisconnect(
            DataInputStream input
    ) throws IOException {

        String username = input.readUTF();

        clients.remove(username);

        System.out.println(
                "[VideoServer-UDP] Disconnected: "
                + username
        );
    }

    private void handleHeartbeat(
            DataInputStream input,
            InetAddress address,
            int port
    ) throws IOException {

        String username = input.readUTF();

        VideoEndpoint endpoint =
                clients.get(username);

        if (endpoint != null) {

            endpoint.address = address;
            endpoint.port = port;
            endpoint.lastActiveTime =
                    System.currentTimeMillis();
        }
    }

    public void stop() {

        running = false;

        clients.clear();

        if (socket != null
                && !socket.isClosed()) {

            socket.close();
        }

        System.out.println(
                "[VideoServer-UDP] Stopped."
        );
    }

    private static class VideoEndpoint {

        private final String username;
        private final String targetUser;

        private InetAddress address;
        private int port;

        private volatile long lastActiveTime;

        public VideoEndpoint(
                String username,
                String targetUser,
                InetAddress address,
                int port
        ) {

            this.username = username;
            this.targetUser = targetUser;
            this.address = address;
            this.port = port;

            this.lastActiveTime =
                    System.currentTimeMillis();
        }
    }
}