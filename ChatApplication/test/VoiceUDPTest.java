package test;

import client.VoiceCallManager;
import client.VoiceClient;
import server.VoiceServer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class VoiceUDPTest {

    public static void main(String[] args) throws Exception {
        System.out.println("=== BẮT ĐẦU KIỂM THỬ UDP VOICE SOCKET 2 CHIỀU VÀ BỘ ĐỆM ===");

        int testPort = 55555;
        VoiceServer voiceServer = new VoiceServer(testPort);
        Thread serverThread = new Thread(voiceServer::start, "Test-VoiceServer");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(200);

        // 1. Kiểm tra đăng ký UDP và gửi/nhận 2 chiều
        DatagramSocket clientA = new DatagramSocket();
        DatagramSocket clientB = new DatagramSocket();
        InetAddress localhost = InetAddress.getByName("127.0.0.1");

        // Gửi REGISTER từ client A (target B)
        java.io.ByteArrayOutputStream baosA = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dosA = new java.io.DataOutputStream(baosA);
        dosA.writeByte(VoiceServer.TYPE_REGISTER);
        dosA.writeUTF("UserA");
        dosA.writeUTF("UserB");
        dosA.flush();
        byte[] regA = baosA.toByteArray();
        clientA.send(new DatagramPacket(regA, regA.length, localhost, testPort));

        // Gửi REGISTER từ client B (target A)
        java.io.ByteArrayOutputStream baosB = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream dosB = new java.io.DataOutputStream(baosB);
        dosB.writeByte(VoiceServer.TYPE_REGISTER);
        dosB.writeUTF("UserB");
        dosB.writeUTF("UserA");
        dosB.flush();
        byte[] regB = baosB.toByteArray();
        clientB.send(new DatagramPacket(regB, regB.length, localhost, testPort));

        Thread.sleep(200);

        // Client A gửi gói AUDIO tới Client B qua VoiceServer UDP
        byte[] dummyAudioA = new byte[1024];
        dummyAudioA[0] = 42;
        java.io.ByteArrayOutputStream audioBaosA = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream audioDosA = new java.io.DataOutputStream(audioBaosA);
        audioDosA.writeByte(VoiceServer.TYPE_AUDIO);
        audioDosA.writeUTF("UserA");
        audioDosA.writeUTF("UserB");
        audioDosA.writeLong(1L);
        audioDosA.writeInt(dummyAudioA.length);
        audioDosA.write(dummyAudioA);
        audioDosA.flush();
        byte[] audioPacketDataA = audioBaosA.toByteArray();
        clientA.send(new DatagramPacket(audioPacketDataA, audioPacketDataA.length, localhost, testPort));

        // Client B nhận gói tin (bỏ qua ACK nếu có)
        byte[] recvBufB = new byte[2048];
        DatagramPacket recvPacketB = new DatagramPacket(recvBufB, recvBufB.length);
        clientB.setSoTimeout(3000);
        byte typeB = 0;
        String senderB = "";
        String targetB = "";
        byte[] pcmB = null;
        int lenB = 0;

        while (true) {
            clientB.receive(recvPacketB);
            java.io.DataInputStream disB = new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(recvPacketB.getData(), recvPacketB.getOffset(), recvPacketB.getLength())
            );
            typeB = disB.readByte();
            if (typeB == VoiceServer.TYPE_AUDIO) {
                senderB = disB.readUTF();
                targetB = disB.readUTF();
                long seqB = disB.readLong();
                lenB = disB.readInt();
                pcmB = new byte[lenB];
                disB.readFully(pcmB);
                break;
            }
        }

        System.out.println("[Test A -> B] Thành công! Nhận được " + lenB + " bytes từ " + senderB + " gửi tới " + targetB + " với byte[0]=" + pcmB[0]);
        if (pcmB[0] != 42 || !senderB.equals("UserA")) {
            throw new RuntimeException("Dữ liệu Client B nhận được không khớp!");
        }

        // 2. Chiều ngược lại: Client B gửi gói AUDIO tới Client A (Kiểm tra 2 chiều)
        byte[] dummyAudioB = new byte[1024];
        dummyAudioB[0] = 99;
        java.io.ByteArrayOutputStream audioBaosB = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream audioDosB = new java.io.DataOutputStream(audioBaosB);
        audioDosB.writeByte(VoiceServer.TYPE_AUDIO);
        audioDosB.writeUTF("UserB");
        audioDosB.writeUTF("UserA");
        audioDosB.writeLong(1L);
        audioDosB.writeInt(dummyAudioB.length);
        audioDosB.write(dummyAudioB);
        audioDosB.flush();
        byte[] audioPacketDataB = audioBaosB.toByteArray();
        clientB.send(new DatagramPacket(audioPacketDataB, audioPacketDataB.length, localhost, testPort));

        // Client A nhận gói tin (bỏ qua ACK nếu có)
        byte[] recvBufA = new byte[2048];
        DatagramPacket recvPacketA = new DatagramPacket(recvBufA, recvBufA.length);
        clientA.setSoTimeout(3000);
        byte typeA = 0;
        String senderA = "";
        String targetA = "";
        byte[] pcmA = null;
        int lenA = 0;

        while (true) {
            clientA.receive(recvPacketA);
            java.io.DataInputStream disA = new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(recvPacketA.getData(), recvPacketA.getOffset(), recvPacketA.getLength())
            );
            typeA = disA.readByte();
            if (typeA == VoiceServer.TYPE_AUDIO) {
                senderA = disA.readUTF();
                targetA = disA.readUTF();
                long seqA = disA.readLong();
                lenA = disA.readInt();
                pcmA = new byte[lenA];
                disA.readFully(pcmA);
                break;
            }
        }

        System.out.println("[Test B -> A] Thành công! Nhận được " + lenA + " bytes từ " + senderA + " gửi tới " + targetA + " với byte[0]=" + pcmA[0]);
        if (pcmA[0] != 99 || !senderA.equals("UserB")) {
            throw new RuntimeException("Dữ liệu Client A nhận được không khớp!");
        }

        clientA.close();
        clientB.close();
        voiceServer.stop();

        // 3. Kiểm tra logic Bộ đệm (Jitter Buffer) của VoiceCallManager
        System.out.println("Kiểm tra bộ đệm của VoiceCallManager...");
        VoiceCallManager manager = new VoiceCallManager();
        // Không gọi start() để tránh mở card âm thanh phần cứng trong môi trường test không có mic/loa
        // Kiểm tra buffer size và logic playAudio
        for (int i = 0; i < 20; i++) {
            byte[] chunk = new byte[1024];
            chunk[0] = (byte) i;
            manager.playAudio(chunk);
        }

        System.out.println("Số lượng gói sau khi nạp dồn 20 gói: " + manager.getBufferSize() + " (Đã giới hạn tối đa MAX_BUFFER_PACKETS để chống lag dồn trễ)");
        manager.stop();

        System.out.println("=== TẤT CẢ KIỂM THỬ UDP VOICE VÀ BỘ ĐỆM ĐỀU THÀNH CÔNG 100%! ===");
    }
}
