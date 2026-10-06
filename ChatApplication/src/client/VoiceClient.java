package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class VoiceClient {

    private static final byte TYPE_REGISTER = 1;
    private static final byte TYPE_AUDIO = 2;
    private static final byte TYPE_DISCONNECT = 3;
    private static final byte TYPE_HEARTBEAT = 4;

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
     * Kết nối logic đến VoiceServer UDP.
     * UDP không tạo connection như TCP.
     * Client mở DatagramSocket và gửi gói REGISTER.
     */
    public void connect(
            String host,
            int port,
            String username,
            String targetUser
    ) throws IOException {

        this.serverAddress = InetAddress.getByName(host);
        this.serverPort = port;
        this.username = username;
        this.targetUser = targetUser;

        socket = new DatagramSocket();

        // Timeout không bắt buộc vì receive chạy trên thread riêng
        running = true;

        sendRegister();

        System.out.println(
                "[VoiceClient-UDP] " + username
                + " đăng ký VoiceServer "
                + host + ":" + port
                + " -> " + targetUser
        );
    }

    /**
     * Gửi thông tin đăng ký endpoint UDP cho server.
     */
    private void sendRegister() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(TYPE_REGISTER);
        dos.writeUTF(username);
        dos.writeUTF(targetUser);
        dos.flush();

        byte[] data = baos.toByteArray();

        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                serverAddress,
                serverPort
        );

        socket.send(packet);
    }

    /**
     * Bắt đầu truyền âm thanh 2 chiều.
     */
    public void startStreaming(VoiceCallManager voiceCallManager) {

        if (!running || voiceCallManager == null) {
            return;
        }

        startReceiving(voiceCallManager);
        startRecording(voiceCallManager);
        startHeartbeat();
    }

    /**
     * THREAD 1:
     * Microphone -> UDP -> VoiceServer
     */
    private void startRecording(VoiceCallManager voiceCallManager) {

        recordingThread = new Thread(() -> {

            while (running && voiceCallManager.isRunning()) {

                byte[] audioData = voiceCallManager.readMicrophone();

                if (audioData == null || audioData.length == 0) {
                    continue;
                }

                try {

                    ByteArrayOutputStream baos =
                            new ByteArrayOutputStream();

                    DataOutputStream dos =
                            new DataOutputStream(baos);

                    dos.writeByte(TYPE_AUDIO);

                    dos.writeUTF(username);
                    dos.writeUTF(targetUser);

                    // Sequence number dùng để xác định thứ tự packet
                    dos.writeLong(sequenceNumber++);

                    dos.writeInt(audioData.length);
                    dos.write(audioData);

                    dos.flush();

                    byte[] packetData = baos.toByteArray();

                    DatagramPacket packet =
                            new DatagramPacket(
                                    packetData,
                                    packetData.length,
                                    serverAddress,
                                    serverPort
                            );

                    socket.send(packet);

                } catch (IOException e) {

                    if (running) {
                        System.out.println(
                                "[VoiceClient-UDP] Lỗi gửi audio: "
                                + e.getMessage()
                        );
                    }

                    break;
                }
            }

        }, "VoiceClient-UDP-Send");

        recordingThread.setDaemon(true);
        recordingThread.start();
    }

    /**
     * THREAD 2:
     * UDP -> Jitter Buffer -> Speaker
     */
    private void startReceiving(
            VoiceCallManager voiceCallManager
    ) {

        receivingThread = new Thread(() -> {

            byte[] receiveBuffer = new byte[4096];

            while (running && voiceCallManager.isRunning()) {

                try {

                    DatagramPacket packet =
                            new DatagramPacket(
                                    receiveBuffer,
                                    receiveBuffer.length
                            );

                    socket.receive(packet);

                    DataInputStream in =
                            new DataInputStream(
                                    new ByteArrayInputStream(
                                            packet.getData(),
                                            packet.getOffset(),
                                            packet.getLength()
                                    )
                            );

                    byte type = in.readByte();

                    if (type == TYPE_AUDIO) {

                        // Server forward nguyên packet AUDIO
                        String sender = in.readUTF();
                        String target = in.readUTF();

                        long sequence = in.readLong();

                        int audioLength = in.readInt();

                        if (audioLength <= 0
                                || audioLength > receiveBuffer.length) {
                            continue;
                        }

                        byte[] audioData =
                                new byte[audioLength];

                        in.readFully(
                                audioData,
                                0,
                                audioLength
                        );

                        /*
                         * Không phát trực tiếp ở đây.
                         *
                         * playAudio() sẽ đưa packet vào
                         * jitterBuffer của VoiceCallManager.
                         */
                        voiceCallManager.playAudio(audioData);

                    } else if (type == TYPE_HEARTBEAT) {

                        // ACK/Heartbeat từ server
                        if (in.available() > 0) {
                            in.readUTF();
                        }
                    }

                } catch (SocketException e) {

                    if (!running) {
                        break;
                    }

                } catch (IOException e) {

                    if (running) {
                        System.out.println(
                                "[VoiceClient-UDP] Loi nhan audio: "
                                + e.getMessage()
                        );
                    }
                }
            }

        }, "VoiceClient-UDP-Receive");

        receivingThread.setDaemon(true);
        receivingThread.start();
    }

    /**
     * Heartbeat giúp VoiceServer biết endpoint UDP
     * của client vẫn đang hoạt động.
     */
    private void startHeartbeat() {

        heartbeatThread = new Thread(() -> {

            while (running) {

                try {

                    Thread.sleep(10000);

                    if (!running) {
                        break;
                    }

                    ByteArrayOutputStream baos =
                            new ByteArrayOutputStream();

                    DataOutputStream dos =
                            new DataOutputStream(baos);

                    dos.writeByte(TYPE_HEARTBEAT);
                    dos.writeUTF(username);
                    dos.flush();

                    byte[] data = baos.toByteArray();

                    DatagramPacket packet =
                            new DatagramPacket(
                                    data,
                                    data.length,
                                    serverAddress,
                                    serverPort
                            );

                    socket.send(packet);

                } catch (InterruptedException e) {

                    break;

                } catch (IOException e) {

                    if (!running) {
                        break;
                    }
                }
            }

        }, "VoiceClient-UDP-Heartbeat");

        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    /**
     * Gửi DISCONNECT và đóng UDP socket.
     */
    public void disconnect() {

        if (!running) {
            return;
        }

        try {

            ByteArrayOutputStream baos =
                    new ByteArrayOutputStream();

            DataOutputStream dos =
                    new DataOutputStream(baos);

            dos.writeByte(TYPE_DISCONNECT);
            dos.writeUTF(username);
            dos.flush();

            byte[] data = baos.toByteArray();

            DatagramPacket packet =
                    new DatagramPacket(
                            data,
                            data.length,
                            serverAddress,
                            serverPort
                    );

            socket.send(packet);

        } catch (Exception ignored) {
        }

        running = false;

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

        socket = null;

        System.out.println(
                "[VoiceClient-UDP] disconected."
        );
    }

    public boolean isConnected() {
        return running
                && socket != null
                && !socket.isClosed();
    }
}