package client;

import com.github.sarxos.webcam.Webcam;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class VideoCallManager  {

    private Webcam webcam;

    // Mở webcam
  public boolean openCamera() {
    try {

        webcam = Webcam.getDefault();

        if (webcam == null) {
            System.out.println("[VIDEO] Không tìm thấy webcam.");
            return false;
        }

        // Nếu camera đang mở thì đóng trước
        if (webcam.isOpen()) {
            webcam.close();
        }

        // Đặt độ phân giải trước khi mở
        webcam.setViewSize(new Dimension(320, 240));

        // Sau đó mới mở camera
        webcam.open();

        System.out.println(
                "[VIDEO] turn on webcam: "
                + webcam.getName()
        );

        return true;

    } catch (Exception e) {

        System.out.println(
                "[VIDEO] erro webcam: "
                + e.getMessage()
        );

        return false;
    }
}

    // Lấy một hình ảnh từ webcam
    public BufferedImage getFrame() {

        if (webcam != null && webcam.isOpen()) {
            return webcam.getImage();
        }

        return null;
    }

    // Đóng webcam
    public void closeCamera() {

        if (webcam != null && webcam.isOpen()) {

            webcam.close();

            System.out.println("[VIDEO] Đã đóng webcam.");
        }
    }

    // Kiểm tra webcam có đang mở hay không
    public boolean isCameraOpen() {

        return webcam != null && webcam.isOpen();
    }
}