package client;

import common.Protocol;

import java.awt.image.BufferedImage;
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

import javax.imageio.ImageIO;

public class VideoClient {

    // Phải giống VideoServer
    private static final byte TYPE_REGISTER = 1;
    private static final byte TYPE_VIDEO = 2;
    private static final byte TYPE_DISCONNECT = 3;
    private static final byte TYPE_HEARTBEAT = 4;

    /*
     * Dữ liệu video thực tế trong mỗi UDP packet.
     *
     * Giữ packet tương đối nhỏ để hạn chế IP fragmentation.
     */
    private static final int MAX_VIDEO_DATA = 1200;

    // Buffer nhận UDP
    private static final int RECEIVE_BUFFER_SIZE = 2048;

    /*
     * Nếu một frame không nhận đủ packet sau khoảng thời gian này
     * thì bỏ frame đó.
     */
    private static final long FRAME_TIMEOUT = 1000;

    private DatagramSocket socket;
    private InetAddress serverAddress;

    private volatile boolean running = false;

    private final String username;
    private final String targetUser;

    private Thread receiveThread;
    private Thread heartbeatThread;

    private long frameSequence = 0;

    /*
     * frameId -> FrameBuffer
     *
     * Dùng để ghép các UDP packet thành một JPEG hoàn chỉnh.
     */
    private final ConcurrentHashMap<Long, FrameBuffer> frameBuffers
            = new ConcurrentHashMap<>();

    // Callback gửi hình đã ghép về VideoCallFrame
    public interface VideoFrameListener {

        void onFrameReceived(BufferedImage image);
    }

    private VideoFrameListener listener;

    public VideoClient(
            String username,
            String targetUser
    ) {

        this.username = username;
        this.targetUser = targetUser;
    }

    public void setVideoFrameListener(
            VideoFrameListener listener
    ) {

        this.listener = listener;
    }

    // =====================================================
    // CONNECT UDP
    // =====================================================

    public boolean connect() {

        try {

            serverAddress =
                    InetAddress.getByName(
                            Protocol.HOST
                    );

            /*
             * UDP không cần kết nối như TCP.
             *
             * DatagramSocket() sẽ tự chọn một local UDP port.
             */
            socket = new DatagramSocket();

            running = true;

            sendRegister();

            startReceiving();

            startHeartbeat();

            System.out.println(
                    "[VideoClient-UDP] Registered: "
                    + username
                    + " -> "
                    + targetUser
                    + " | Server: "
                    + Protocol.HOST
                    + ":"
                    + Protocol.VIDEO_PORT
            );

            return true;

        } catch (IOException e) {

            System.out.println(
                    "[VideoClient-UDP] Start error: "
                    + e.getMessage()
            );

            running = false;

            return false;
        }
    }

    // =====================================================
    // REGISTER
    // =====================================================

    private void sendRegister()
            throws IOException {

        ByteArrayOutputStream baos =
                new ByteArrayOutputStream();

        DataOutputStream output =
                new DataOutputStream(baos);

        output.writeByte(TYPE_REGISTER);

        output.writeUTF(username);

        output.writeUTF(targetUser);

        output.flush();

        sendPacket(baos.toByteArray());
    }

    // =====================================================
    // SEND VIDEO FRAME
    // =====================================================

    public synchronized void sendFrame(
            BufferedImage image
    ) {

        if (!running
                || socket == null
                || socket.isClosed()
                || image == null) {

            return;
        }

        try {

            // BufferedImage -> JPEG
            ByteArrayOutputStream imageStream =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    image,
                    "jpg",
                    imageStream
            );

            byte[] imageData =
                    imageStream.toByteArray();

            if (imageData.length == 0) {
                return;
            }

            long frameId =
                    frameSequence++;

            /*
             * Tính số UDP packet cần thiết
             * cho frame JPEG hiện tại.
             */
            int totalPackets =
                    (int) Math.ceil(
                            (double) imageData.length
                            / MAX_VIDEO_DATA
                    );

            for (int packetIndex = 0;
                    packetIndex < totalPackets;
                    packetIndex++) {

                int offset =
                        packetIndex
                        * MAX_VIDEO_DATA;

                int dataLength =
                        Math.min(
                                MAX_VIDEO_DATA,
                                imageData.length
                                - offset
                        );

                sendVideoPacket(
                        frameId,
                        packetIndex,
                        totalPackets,
                        imageData,
                        offset,
                        dataLength
                );
            }

        } catch (IOException e) {

            if (running) {

                System.out.println(
                        "[VideoClient-UDP] Send frame error: "
                        + e.getMessage()
                );
            }
        }
    }

    private void sendVideoPacket(
            long frameId,
            int packetIndex,
            int totalPackets,
            byte[] imageData,
            int offset,
            int dataLength
    ) throws IOException {

        ByteArrayOutputStream baos =
                new ByteArrayOutputStream();

        DataOutputStream output =
                new DataOutputStream(baos);

        /*
         * Packet format:
         *
         * byte TYPE_VIDEO
         * UTF sender
         * UTF target
         * long frameId
         * int packetIndex
         * int totalPackets
         * int dataLength
         * byte[] JPEG-part
         */

        output.writeByte(TYPE_VIDEO);

        output.writeUTF(username);

        output.writeUTF(targetUser);

        output.writeLong(frameId);

        output.writeInt(packetIndex);

        output.writeInt(totalPackets);

        output.writeInt(dataLength);

        output.write(
                imageData,
                offset,
                dataLength
        );

        output.flush();

        sendPacket(
                baos.toByteArray()
        );
    }

    // =====================================================
    // RECEIVE UDP
    // =====================================================

    private void startReceiving() {

        receiveThread =
                new Thread(() -> {

                    while (running) {

                        try {

                            byte[] buffer =
                                    new byte[
                                            RECEIVE_BUFFER_SIZE
                                    ];

                            DatagramPacket packet =
                                    new DatagramPacket(
                                            buffer,
                                            buffer.length
                                    );

                            socket.receive(packet);

                            handleIncomingPacket(
                                    packet
                            );

                        } catch (SocketException e) {

                            if (!running) {
                                break;
                            }

                            System.out.println(
                                    "[VideoClient-UDP] Socket error: "
                                    + e.getMessage()
                            );

                        } catch (IOException e) {

                            if (running) {

                                System.out.println(
                                        "[VideoClient-UDP] Receive error: "
                                        + e.getMessage()
                                );
                            }
                        }
                    }

                }, "VideoClient-UDP-Receive");

        receiveThread.setDaemon(true);

        receiveThread.start();
    }

    private void handleIncomingPacket(
            DatagramPacket packet
    ) throws IOException {

        DataInputStream input =
                new DataInputStream(
                        new ByteArrayInputStream(
                                packet.getData(),
                                packet.getOffset(),
                                packet.getLength()
                        )
                );

        byte type =
                input.readByte();

        if (type != TYPE_VIDEO) {
            return;
        }

        String sender =
                input.readUTF();

        String target =
                input.readUTF();

        long frameId =
                input.readLong();

        int packetIndex =
                input.readInt();

        int totalPackets =
                input.readInt();

        int dataLength =
                input.readInt();

        /*
         * Kiểm tra packet để tránh dữ liệu lỗi.
         */
        if (packetIndex < 0
                || totalPackets <= 0
                || packetIndex >= totalPackets
                || dataLength <= 0
                || dataLength > MAX_VIDEO_DATA
                || dataLength > input.available()) {

            return;
        }

        byte[] videoData =
                new byte[dataLength];

        input.readFully(videoData);

        /*
         * Lấy hoặc tạo buffer dành cho frame này.
         */
        FrameBuffer frameBuffer =
                frameBuffers.computeIfAbsent(
                        frameId,
                        id -> new FrameBuffer(
                                totalPackets
                        )
                );

        frameBuffer.addPacket(
                packetIndex,
                videoData
        );

        /*
         * Nếu nhận đủ packet
         * -> ghép JPEG
         * -> BufferedImage
         * -> gửi cho VideoCallFrame.
         */
        if (frameBuffer.isComplete()) {

            frameBuffers.remove(
                    frameId
            );

            byte[] completeFrame =
                    frameBuffer.buildFrame();

            BufferedImage image =
                    ImageIO.read(
                            new ByteArrayInputStream(
                                    completeFrame
                            )
                    );

            if (image != null
                    && listener != null) {

                listener.onFrameReceived(
                        image
                );
            }
        }

        cleanupOldFrames();
    }

    // =====================================================
    // HEARTBEAT
    // =====================================================

    private void startHeartbeat() {

        heartbeatThread =
                new Thread(() -> {

                    while (running) {

                        try {

                            Thread.sleep(10000);

                            if (!running) {
                                break;
                            }

                            ByteArrayOutputStream baos =
                                    new ByteArrayOutputStream();

                            DataOutputStream output =
                                    new DataOutputStream(
                                            baos
                                    );

                            output.writeByte(
                                    TYPE_HEARTBEAT
                            );

                            output.writeUTF(
                                    username
                            );

                            output.flush();

                            sendPacket(
                                    baos.toByteArray()
                            );

                        } catch (InterruptedException e) {

                            break;

                        } catch (IOException e) {

                            if (running) {

                                System.out.println(
                                        "[VideoClient-UDP] Heartbeat error: "
                                        + e.getMessage()
                                );
                            }
                        }
                    }

                }, "VideoClient-UDP-Heartbeat");

        heartbeatThread.setDaemon(true);

        heartbeatThread.start();
    }

    // =====================================================
    // CLEANUP FRAME BỊ THIẾU PACKET
    // =====================================================

    private void cleanupOldFrames() {

        long now =
                System.currentTimeMillis();

        for (Map.Entry<Long, FrameBuffer> entry
                : frameBuffers.entrySet()) {

            FrameBuffer frame =
                    entry.getValue();

            if (now - frame.createdAt
                    > FRAME_TIMEOUT) {

                frameBuffers.remove(
                        entry.getKey()
                );
            }
        }
    }

    // =====================================================
    // SEND UDP PACKET
    // =====================================================

    private void sendPacket(
            byte[] data
    ) throws IOException {

        DatagramPacket packet =
                new DatagramPacket(
                        data,
                        data.length,
                        serverAddress,
                        Protocol.VIDEO_PORT
                );

        socket.send(packet);
    }

    // =====================================================
    // CLOSE
    // =====================================================

    public void close() {

        if (!running) {
            return;
        }

        try {

            ByteArrayOutputStream baos =
                    new ByteArrayOutputStream();

            DataOutputStream output =
                    new DataOutputStream(
                            baos
                    );

            output.writeByte(
                    TYPE_DISCONNECT
            );

            output.writeUTF(
                    username
            );

            output.flush();

            sendPacket(
                    baos.toByteArray()
            );

        } catch (Exception ignored) {
        }

        running = false;

        frameBuffers.clear();

        if (receiveThread != null) {

            receiveThread.interrupt();

            receiveThread = null;
        }

        if (heartbeatThread != null) {

            heartbeatThread.interrupt();

            heartbeatThread = null;
        }

        if (socket != null
                && !socket.isClosed()) {

            socket.close();
        }

        socket = null;

        System.out.println(
                "[VideoClient-UDP] Disconnected: "
                + username
        );
    }

    public boolean isRunning() {

        return running
                && socket != null
                && !socket.isClosed();
    }

    // =====================================================
    // FRAME BUFFER
    // =====================================================

    private static class FrameBuffer {

        private final byte[][] packets;

        private int receivedPackets = 0;

        private final long createdAt =
                System.currentTimeMillis();

        public FrameBuffer(
                int totalPackets
        ) {

            packets =
                    new byte[
                            totalPackets
                    ][];
        }

        public synchronized void addPacket(
                int index,
                byte[] data
        ) {

            if (index < 0
                    || index >= packets.length) {

                return;
            }

            /*
             * Tránh đếm packet trùng hai lần.
             */
            if (packets[index] == null) {

                packets[index] = data;

                receivedPackets++;
            }
        }

        public synchronized boolean isComplete() {

            return receivedPackets
                    == packets.length;
        }

        public synchronized byte[] buildFrame()
                throws IOException {

            ByteArrayOutputStream baos =
                    new ByteArrayOutputStream();

            for (byte[] packet : packets) {

                if (packet == null) {

                    throw new IOException(
                            "Frame chưa đủ packet."
                    );
                }

                baos.write(packet);
            }

            return baos.toByteArray();
        }
    }
}