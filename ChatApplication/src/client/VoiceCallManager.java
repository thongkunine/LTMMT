package client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * VoiceCallManager: Quản lý thiết bị Microphone, Loa và Cơ chế Xử lý Bộ đệm (Audio Buffer / Jitter Buffer).
 * 
 * Mục tiêu: Âm thanh phát ra mượt mà, không bị ngắt quãng:
 * 1. Sử dụng Java Sound API với định dạng PCM 16kHz, 16-bit, Mono (Chuẩn VoIP âm thanh rõ, băng thông nhẹ).
 * 2. Bộ đệm phần cứng (Hardware Line Buffer):
 *    - Speaker: 8192 bytes (~256ms âm thanh) giúp card âm thanh luôn có dữ liệu dự phòng.
 *    - Microphone: 4096 bytes (~128ms âm thanh).
 * 3. Hàng đợi đệm mềm (Jitter Buffer Queue) kèm cơ chế Pre-buffering:
 *    - Khi bắt đầu hoặc khi bộ đệm bị cạn (Buffer Underrun do mạng trễ), hệ thống sẽ tích lũy đủ
 *      PREBUFFER_PACKETS (3 gói ~ 96ms) rồi mới bắt đầu xả ra loa. 96ms là độ trễ tai người không
 *      cảm nhận được nhưng đủ để hấp thụ hoàn toàn hiện tượng jitter/chập chờn của mạng UDP.
 *    - Giới hạn tối đa MAX_BUFFER_PACKETS (12 gói ~ 384ms): Nếu mạng bị nghẽn rồi dồn về quá nhiều gói,
 *      hệ thống chủ động loại bỏ gói cũ nhất để đảm bảo đàm thoại luôn bám sát thời gian thực (Real-time).
 * 4. Tách biệt hoàn toàn luồng nhận mạng và luồng phát loa:
 *    - Luồng nhận UDP chỉ việc đẩy (enqueue) vào bộ đệm không bị nghẽn.
 *    - Luồng phát (Playback Thread) độc lập lấy các khối từ bộ đệm ghi ra loa đều đặn.
 */
public class VoiceCallManager {

    private TargetDataLine microphone;
    private SourceDataLine speaker;

    private volatile boolean running = false;
    private volatile boolean micMuted = false;
    private volatile boolean speakerMuted = false;

    // Định dạng âm thanh: 16.000 Hz, 16 bit, Mono, Signed, Little-Endian
    private final AudioFormat format = new AudioFormat(
            16000.0f, // 16 kHz
            16,       // 16 bit
            1,        // Mono
            true,     // Signed
            false     // Little Endian
    );

    // Kích thước 1 khối âm thanh thu từ Mic: 1024 bytes (tương đương ~32ms âm thanh)
    public static final int CHUNK_SIZE = 1024;

    // Kích thước bộ đệm phần cứng Loa: 8192 bytes (~256ms)
    public static final int SPEAKER_HARDWARE_BUFFER = 8192;

    // Kích thước bộ đệm phần cứng Micro: 4096 bytes (~128ms)
    public static final int MIC_HARDWARE_BUFFER = 4096;

    // --- CẤU HÌNH BỘ ĐỆM JITTER BUFFER (CHỐNG NGẮT QUÃNG) ---
    // Số gói tối thiểu cần gom đủ trước khi bắt đầu xả ra loa (Pre-buffering)
    private static final int PREBUFFER_PACKETS = 3;

    // Số gói tối đa trong bộ đệm (tránh tích tụ độ trễ cuộc gọi khi mạng lag)
    private static final int MAX_BUFFER_PACKETS = 12;

    // Hàng đợi bộ đệm chứa các gói âm thanh nhận được từ mạng UDP
    private final BlockingQueue<byte[]> jitterBuffer = new LinkedBlockingQueue<>(MAX_BUFFER_PACKETS + 4);
    private final Object bufferLock = new Object();
    private volatile boolean isPrebuffering = true;

    private Thread playbackThread;

    /**
     * Mở microphone và loa với bộ đệm được tối ưu hoá
     */
    public synchronized void start() throws LineUnavailableException {
        try {
            // ===== MICROPHONE =====
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
            microphone.open(format, MIC_HARDWARE_BUFFER);
            microphone.start();

            // ===== SPEAKER =====
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
            speaker = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            speaker.open(format, SPEAKER_HARDWARE_BUFFER);
            speaker.start();

            running = true;
            isPrebuffering = true;
            jitterBuffer.clear();

            // Khởi động luồng phát âm thanh độc lập từ bộ đệm
            startPlaybackWorker();

            System.out.println("[VoiceCallManager] Bắt đầu gọi thoại. Đệm phần cứng: " 
                + SPEAKER_HARDWARE_BUFFER + " bytes, Pre-buffering: " + PREBUFFER_PACKETS + " gói.");
        } catch (LineUnavailableException e) {
            stop();
            throw e;
        }
    }

    /**
     * Luồng tiêu thụ bộ đệm (Playback Worker): Đọc từ jitterBuffer và ghi ra SourceDataLine
     */
    private void startPlaybackWorker() {
        playbackThread = new Thread(() -> {
            while (running) {
                try {
                    // Nếu đang Pre-buffering, chờ gom đủ ít nhất PREBUFFER_PACKETS gói
                    if (isPrebuffering) {
                        synchronized (bufferLock) {
                            while (running && jitterBuffer.size() < PREBUFFER_PACKETS) {
                                bufferLock.wait(100);
                            }
                            isPrebuffering = false;
                        }
                    }

                    // Lấy gói dữ liệu âm thanh từ hàng đợi bộ đệm (chờ tối đa 40ms)
                    byte[] audioData = jitterBuffer.poll(40, TimeUnit.MILLISECONDS);

                    if (audioData != null) {
                        if (!speakerMuted && speaker != null && speaker.isOpen()) {
                            speaker.write(audioData, 0, audioData.length);
                        }
                    } else {
                        // Không có dữ liệu (Buffer Underrun do mạng bị trễ):
                        // Tự động kích hoạt lại Pre-buffering để nạp đệm, tránh tiếng lách tách ngắt quãng
                        isPrebuffering = true;
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    if (!running) {
                        break;
                    }
                }
            }
        }, "VoiceCallManager-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    public void setMicMuted(boolean muted) {
        this.micMuted = muted;
    }

    public boolean isMicMuted() {
        return micMuted;
    }

    public void setSpeakerMuted(boolean muted) {
        this.speakerMuted = muted;
    }

    public boolean isSpeakerMuted() {
        return speakerMuted;
    }

    /**
     * Đọc dữ liệu từ Microphone (1024 bytes ~ 32ms)
     */
    public byte[] readMicrophone() {
        if (!running || microphone == null || micMuted) {
            return null;
        }

        byte[] buffer = new byte[CHUNK_SIZE];
        int bytesRead = microphone.read(buffer, 0, buffer.length);

        if (bytesRead <= 0) {
            return null;
        }

        if (bytesRead == buffer.length) {
            return buffer;
        }

        byte[] audioData = new byte[bytesRead];
        System.arraycopy(buffer, 0, audioData, 0, bytesRead);
        return audioData;
    }

    /**
     * Đẩy gói dữ liệu âm thanh nhận từ UDP vào Bộ đệm (Jitter Buffer).
     * Xử lý chống ngắt quãng âm thanh:
     * - Lưu trữ vào hàng đợi.
     * - Nếu hàng đợi quá đầy do mạng dồn gói, loại bỏ gói cũ nhất.
     * - Đánh thức luồng phát loa.
     */
    public void playAudio(byte[] audioData) {
        if (!running || speaker == null || audioData == null || speakerMuted) {
            return;
        }

        // Kiểm soát kích thước bộ đệm: Nếu vượt quá MAX_BUFFER_PACKETS, drop gói cũ nhất
        while (jitterBuffer.size() >= MAX_BUFFER_PACKETS) {
            jitterBuffer.poll();
        }

        jitterBuffer.offer(audioData);

        // Đánh thức luồng phát nếu đang ở trạng thái pre-buffering
        synchronized (bufferLock) {
            bufferLock.notifyAll();
        }
    }

    /**
     * Dừng cuộc gọi và giải phóng toàn bộ tài nguyên âm thanh
     */
    public synchronized void stop() {
        running = false;

        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }

        synchronized (bufferLock) {
            bufferLock.notifyAll();
        }
        jitterBuffer.clear();

        if (microphone != null) {
            try {
                microphone.stop();
                microphone.flush();
                microphone.close();
            } catch (Exception ignored) {
            }
            microphone = null;
        }

        if (speaker != null) {
            try {
                speaker.stop();
                speaker.flush();
                speaker.close();
            } catch (Exception ignored) {
            }
            speaker = null;
        }

        System.out.println("[VoiceCallManager] Đã dừng cuộc gọi và giải phóng bộ đệm.");
    }

    public boolean isRunning() {
        return running;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getBufferSize() {
        return jitterBuffer.size();
    }
}