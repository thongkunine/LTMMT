package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * VoiceClient: Chịu trách nhiệm kết nối đến VoiceServer, gửi và nhận audio qua TCP.
 */
public class VoiceClient {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private volatile boolean running = false;

    private Thread recordingThread;
    private Thread playbackThread;

    /**
     * Kết nối đến VoiceServer và thực hiện bắt tay (gửi username và targetUser)
     */
    public void connect(String host, int port, String username, String targetUser) throws IOException {
        socket = new Socket(host, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        // Gửi thông tin định danh cho VoiceServer
        out.writeUTF(username);
        out.writeUTF(targetUser);
        out.flush();

        running = true;
    }

    /**
     * Bắt đầu 2 luồng: 1 luồng thu âm gửi đi, 1 luồng nhận âm thanh phát ra loa
     */
    public void startStreaming(VoiceCallManager voiceCallManager) {
        if (!running || voiceCallManager == null) {
            return;
        }

        // Luồng 1: Thu âm từ Microphone và gửi đến VoiceServer
        recordingThread = new Thread(() -> {
            while (running && !socket.isClosed() && voiceCallManager.isRunning()) {
                byte[] audioData = voiceCallManager.readMicrophone();
                if (audioData != null && audioData.length > 0 && running) {
                    try {
                        synchronized (this) {
                            if (out != null) {
                                out.writeInt(audioData.length);
                                out.write(audioData);
                                out.flush();
                            }
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        }, "VoiceClient-Record");
        recordingThread.setDaemon(true);
        recordingThread.start();

        // Luồng 2: Nhận âm thanh từ VoiceServer và phát ra loa
        playbackThread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            while (running && !socket.isClosed() && voiceCallManager.isRunning()) {
                try {
                    int length = in.readInt();
                    if (length <= 0) {
                        break;
                    }
                    if (length > buffer.length) {
                        buffer = new byte[length];
                    }

                    in.readFully(buffer, 0, length);

                    byte[] audioChunk = new byte[length];
                    System.arraycopy(buffer, 0, audioChunk, 0, length);

                    voiceCallManager.playAudio(audioChunk);
                } catch (IOException e) {
                    break;
                }
            }
        }, "VoiceClient-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    /**
     * Ngắt kết nối và giải phóng tài nguyên
     */
    public void disconnect() {
        running = false;

        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }

        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }

        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isConnected() {
        return running && socket != null && !socket.isClosed();
    }
}
