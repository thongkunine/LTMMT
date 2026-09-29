package client;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class VoiceCallManager {

    private TargetDataLine microphone;
    private SourceDataLine speaker;

    private boolean running = false;

    // Định dạng âm thanh dùng cho cuộc gọi
    private final AudioFormat format = new AudioFormat(
            16000.0f, // 16 kHz
            16,       // 16 bit
            1,        // Mono
            true,     // Signed
            false     // Little Endian
    );

    // Mở microphone và loa
    public void start() throws LineUnavailableException {
        try {
            // ===== MICROPHONE =====
            DataLine.Info micInfo =
                    new DataLine.Info(
                            TargetDataLine.class,
                            format
                    );

            microphone =
                    (TargetDataLine) AudioSystem.getLine(micInfo);

            microphone.open(format);
            microphone.start();

            // ===== SPEAKER =====
            DataLine.Info speakerInfo =
                    new DataLine.Info(
                            SourceDataLine.class,
                            format
                    );

            speaker =
                    (SourceDataLine) AudioSystem.getLine(speakerInfo);

            speaker.open(format);
            speaker.start();

            running = true;

            System.out.println("Voice call started.");
        } catch (LineUnavailableException e) {
            stop();
            throw e;
        }
    }

    // Đọc dữ liệu từ microphone
    public byte[] readMicrophone() {

        if (!running || microphone == null) {
            return null;
        }

        byte[] buffer = new byte[1024];

        int bytesRead = microphone.read(
                buffer,
                0,
                buffer.length
        );

        if (bytesRead <= 0) {
            return null;
        }

        if (bytesRead == buffer.length) {
            return buffer;
        }

        byte[] audioData = new byte[bytesRead];

        System.arraycopy(
                buffer,
                0,
                audioData,
                0,
                bytesRead
        );

        return audioData;
    }

    // Phát dữ liệu âm thanh ra loa
    public void playAudio(byte[] audioData) {

        if (!running
                || speaker == null
                || audioData == null) {
            return;
        }

        speaker.write(
                audioData,
                0,
                audioData.length
        );
    }

    // Dừng cuộc gọi
    public void stop() {

        running = false;

        if (microphone != null) {
            try {
                microphone.stop();
                microphone.close();
            } catch (Exception ignored) {
            }
            microphone = null;
        }

        if (speaker != null) {
            try {
                speaker.stop();
                speaker.close();
            } catch (Exception ignored) {
            }
            speaker = null;
        }

        System.out.println("Voice call stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public AudioFormat getFormat() {
        return format;
    }
}