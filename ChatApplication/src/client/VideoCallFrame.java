/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package client;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import java.awt.Image;
import com.github.sarxos.webcam.Webcam;
import common.Message;
import common.MessageType;
import java.io.IOException;
import javax.swing.JOptionPane;
/**
 *
 * @author Admin
 */
public class VideoCallFrame extends javax.swing.JFrame {
    private String username;
    private String otherUser;
    private ClientConnection connection;
    private boolean incomingCall;
    private VideoCallManager videoCallManager;
    private volatile boolean cameraRunning = false;
    private VideoClient videoClient;
    // hàm 
    private void stopVideoConnection() {

    if (videoClient != null) {
        videoClient.close();
        videoClient = null;
    }

    lb_remoteVideo.setIcon(null);
    lb_remoteVideo.setText("video người bên kia");
}
    private void startVideoConnection() {

    if (videoClient != null && videoClient.isRunning()) {
        return;
    }

    videoClient = new VideoClient(
            username,
            otherUser
    );

    videoClient.setVideoFrameListener(image -> {

        if (image == null) {
            return;
        }

        javax.swing.SwingUtilities.invokeLater(() -> {

            int width = lb_remoteVideo.getWidth();
            int height = lb_remoteVideo.getHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            Image scaledImage = image.getScaledInstance(
                    width,
                    height,
                    Image.SCALE_SMOOTH
            );

            lb_remoteVideo.setText("");
            lb_remoteVideo.setIcon(
                    new ImageIcon(scaledImage)
            );
        });
    });

    boolean connected = videoClient.connect();

    if (!connected) {

        JOptionPane.showMessageDialog(
                this,
                "not conected Server!"
        );

        videoClient = null;
    }
}
    private void startLocalCamera() {

    videoCallManager = new VideoCallManager();

    boolean opened = videoCallManager.openCamera();

    if (!opened) {
        JOptionPane.showMessageDialog(
                this,
                "Không thể mở webcam!"
        );
        return;
    }

    cameraRunning = true;

    Thread cameraThread = new Thread(() -> {

        while (cameraRunning) {

            BufferedImage image = videoCallManager.getFrame();

            if (image != null) {
                if (videoClient != null && videoClient.isRunning()) {
                    videoClient.sendFrame(image);
}

                Image scaledImage = image.getScaledInstance(
                        lb_localvideo.getWidth(),
                        lb_localvideo.getHeight(),
                        Image.SCALE_SMOOTH
                );

                ImageIcon icon = new ImageIcon(scaledImage);

                javax.swing.SwingUtilities.invokeLater(() -> {
                    lb_localvideo.setText("");
                    lb_localvideo.setIcon(icon);
                });
            }

            try {
                // Khoảng 15 FPS
                Thread.sleep(66);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    });

    cameraThread.setDaemon(true);
    cameraThread.start();
}
   private void stopLocalCamera() {

    cameraRunning = false;

    if (videoCallManager != null) {
        videoCallManager.closeCamera();
    }

    if (videoClient != null) {
        videoClient.close();
        videoClient = null;
    }

    lb_localvideo.setIcon(null);
    lb_localvideo.setText("Camera của tôi");

    lb_remoteVideo.setIcon(null);
    lb_remoteVideo.setText("Video người bên kia");
}
    /**
     * Creates new form VideoCallFrame
     */
    public VideoCallFrame() {
        initComponents();
    }
public VideoCallFrame(
        String username,
        String otherUser,
        ClientConnection connection,
        boolean incomingCall) {

    initComponents();

    this.username = username;
    this.otherUser = otherUser;
    this.connection = connection;
    this.incomingCall = incomingCall;

    setLocationRelativeTo(null);
    setTitle("Video Call - " + otherUser);

    lb_username.setText(otherUser);

    if (incomingCall) {

        lb_callStatus.setText("Đang gọi video đến...");

        btnAccept.setVisible(true);
        btnReject.setVisible(true);
        btnEndCall.setVisible(false);

    } else {

        lb_callStatus.setText("Đang gọi video...");

        btnAccept.setVisible(false);
        btnReject.setVisible(false);
        btnEndCall.setVisible(true);
        
        startVideoConnection();
        startLocalCamera();
    }
    
    
    
    
}
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToggleButton1 = new javax.swing.JToggleButton();
        lb_username = new javax.swing.JLabel();
        lb_callStatus = new javax.swing.JLabel();
        panel1 = new java.awt.Panel();
        label1 = new java.awt.Label();
        lb_localvideo = new javax.swing.JLabel();
        lb_remoteVideo = new javax.swing.JLabel();
        btnAccept = new java.awt.Button();
        btnReject = new java.awt.Button();
        btnEndCall = new java.awt.Button();

        jToggleButton1.setText("jToggleButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        lb_username.setText("VIDEO CALL--- ---");

        lb_callStatus.setText("đang gọi đến.......");
        lb_callStatus.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                lb_callStatusFocusGained(evt);
            }
        });

        label1.setText("video của tôi ");
        label1.setVisible(false);

        lb_localvideo.setText("camera của tôi");

        lb_remoteVideo.setText("video người bên kia");

        javax.swing.GroupLayout panel1Layout = new javax.swing.GroupLayout(panel1);
        panel1.setLayout(panel1Layout);
        panel1Layout.setHorizontalGroup(
            panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel1Layout.createSequentialGroup()
                .addGroup(panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panel1Layout.createSequentialGroup()
                        .addGap(221, 221, 221)
                        .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 25, Short.MAX_VALUE))
                    .addGroup(panel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lb_remoteVideo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addComponent(lb_localvideo, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panel1Layout.setVerticalGroup(
            panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel1Layout.createSequentialGroup()
                .addGroup(panel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panel1Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(lb_remoteVideo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(panel1Layout.createSequentialGroup()
                        .addContainerGap(151, Short.MAX_VALUE)
                        .addComponent(lb_localvideo, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(57, 57, 57))
        );

        btnAccept.setLabel("chấp nhận ");
        btnAccept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAcceptActionPerformed(evt);
            }
        });

        btnReject.setLabel("từ chối");
        btnReject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRejectActionPerformed(evt);
            }
        });

        btnEndCall.setLabel("kết thúc");
        btnEndCall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEndCallActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap(20, Short.MAX_VALUE)
                        .addComponent(panel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(142, 142, 142)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lb_username, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lb_callStatus)))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(btnAccept, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(63, 63, 63)
                                .addComponent(btnReject, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(82, 82, 82)
                                .addComponent(btnEndCall, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb_username)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lb_callStatus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnAccept, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnReject, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEndCall, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAcceptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAcceptActionPerformed
        // TODO add your handling code here:
        acceptVideoCall();
    }//GEN-LAST:event_btnAcceptActionPerformed

    private void btnRejectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRejectActionPerformed
        // TODO add your handling code here:
        rejectVideoCall();
    }//GEN-LAST:event_btnRejectActionPerformed

    private void btnEndCallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEndCallActionPerformed
          endVideoCall();
        // TODO add your handling code here:
    }//GEN-LAST:event_btnEndCallActionPerformed

    private void lb_callStatusFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_lb_callStatusFocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_lb_callStatusFocusGained

    /**
     * @param args the command line arguments
     */
   public void videoCallEnded() {

    lb_callStatus.setText(
            otherUser + " đã kết thúc cuộc gọi."
    );

    btnAccept.setVisible(false);
    btnReject.setVisible(false);
    btnEndCall.setVisible(false);
    stopLocalCamera();
}
    public void videoCallAccepted() {

    lb_callStatus.setText(
            "Đang trong cuộc gọi video với " + otherUser
    );

    btnAccept.setVisible(false);
    btnReject.setVisible(false);
    btnEndCall.setVisible(true);
}

    public void videoCallRejected() {
        stopLocalCamera();
        stopVideoConnection();

    lb_callStatus.setText(
            otherUser + " đã từ chối cuộc gọi."
    );

    btnAccept.setVisible(false);
    btnReject.setVisible(false);
    btnEndCall.setVisible(false);
}
    private void acceptVideoCall() {

    Message message = new Message(
            MessageType.VIDEO_CALL_ACCEPT,
            username,
            "Accepted"
    );

    message.setReceiver(otherUser);

    try {
        connection.send(message);

        lb_callStatus.setText("Đang trong cuộc gọi video...");

        btnAccept.setVisible(false);
        btnReject.setVisible(false);
        btnEndCall.setVisible(true);
        
       // startLocalCamera();
        startVideoConnection();                   

    } catch (IOException e) {
        JOptionPane.showMessageDialog(
                this,
                "Không thể chấp nhận cuộc gọi video: "
                + e.getMessage()
        );
    }
}
    private void rejectVideoCall() {

    Message message = new Message(
            MessageType.VIDEO_CALL_REJECT,
            username,
            "Rejected"
    );

    message.setReceiver(otherUser);

    try {
        connection.send(message);

        lb_callStatus.setText("Đã từ chối cuộc gọi video.");

        dispose();

    } catch (IOException e) {
        JOptionPane.showMessageDialog(
                this,
                "Không thể từ chối cuộc gọi video: "
                + e.getMessage()
        );
    }
}
    private void endVideoCall() {

    Message message = new Message(
            MessageType.VIDEO_CALL_END,
            username,
            "Ended"
    );

    message.setReceiver(otherUser);

    try {
        connection.send(message);

    } catch (IOException e) {
        System.out.println(
                "Không thể gửi VIDEO_CALL_END: "
                + e.getMessage()
        );
    }
        stopLocalCamera();  
        stopVideoConnection();
    dispose();
}
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(VideoCallFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(VideoCallFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(VideoCallFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VideoCallFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VideoCallFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.Button btnAccept;
    private java.awt.Button btnEndCall;
    private java.awt.Button btnReject;
    private javax.swing.JToggleButton jToggleButton1;
    private java.awt.Label label1;
    private javax.swing.JLabel lb_callStatus;
    private javax.swing.JLabel lb_localvideo;
    private javax.swing.JLabel lb_remoteVideo;
    private javax.swing.JLabel lb_username;
    private java.awt.Panel panel1;
    // End of variables declaration//GEN-END:variables
}
