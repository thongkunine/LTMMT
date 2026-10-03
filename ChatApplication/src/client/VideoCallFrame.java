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
    private String roomName;
    private ClientConnection connection;
    private boolean incomingCall;
    private VideoCallManager videoCallManager;
    private volatile boolean cameraRunning = false;
    private VideoClient videoClient;
    private volatile boolean micMuted = false;
    private volatile boolean cameraOff = false;
    private Runnable onCloseCallback;
    private long callStartTime = 0;
    private boolean callConnected = false;
    private boolean historyLogged = false;

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
        String target = (roomName != null && !roomName.isBlank()) ? roomName : (otherUser != null ? otherUser : "");
        if (target.isBlank()) {
            return;
        }
        videoClient = new VideoClient(username, target);
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
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                lb_remoteVideo.setText("");
                lb_remoteVideo.setIcon(new ImageIcon(scaledImage));
            });
        });
        boolean connected = videoClient.connect();
        if (!connected) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối máy chủ Video!");
            videoClient = null;
        }
    }

    private void startLocalCamera() {
        videoCallManager = new VideoCallManager();
        boolean opened = videoCallManager.openCamera();
        if (!opened) {
            JOptionPane.showMessageDialog(this, "Không thể mở webcam!");
            return;
        }
        cameraRunning = true;
        Thread cameraThread = new Thread(() -> {
            while (cameraRunning) {
                if (!cameraOff) {
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
                            if (!cameraOff) {
                                lb_localvideo.setText("");
                                lb_localvideo.setIcon(icon);
                            }
                        });
                    }
                }
                try {
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

    private synchronized void logCallHistory() {
        if (historyLogged) {
            return;
        }
        historyLogged = true;

        String durationStr;
        if (callConnected && callStartTime > 0) {
            long durationSec = (System.currentTimeMillis() - callStartTime) / 1000;
            long minutes = durationSec / 60;
            long seconds = durationSec % 60;
            durationStr = String.format("%02d:%02d", minutes, seconds);
        } else {
            durationStr = "Không trả lời";
        }

        String logContent = "[Cuộc gọi video] - Thời lượng: " + durationStr;

        if (roomName != null && !roomName.isBlank()) {
            Message historyMsg = new Message(MessageType.CHAT, username, logContent, roomName);
            try {
                if (connection != null) {
                    connection.send(historyMsg);
                }
            } catch (IOException e) {
                System.out.println("Lỗi gửi lịch sử cuộc gọi video phòng: " + e.getMessage());
            }
        } else if (otherUser != null && !otherUser.isBlank()) {
            Message historyMsg = new Message(MessageType.PRIVATE_MESSAGE, username, logContent);
            historyMsg.setReceiver(otherUser);
            try {
                if (connection != null) {
                    connection.send(historyMsg);
                }
            } catch (IOException e) {
                System.out.println("Lỗi gửi lịch sử cuộc gọi video: " + e.getMessage());
            }
        }
    }

    private void handleWindowClosing() {
        if (incomingCall && !callConnected) {
            rejectVideoCall();
        } else {
            endVideoCall();
        }
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
        this(username, otherUser, null, connection, incomingCall, null);
    }

    public VideoCallFrame(
            String username,
            String otherUser,
            ClientConnection connection,
            boolean incomingCall,
            Runnable onCloseCallback) {
        this(username, otherUser, null, connection, incomingCall, onCloseCallback);
    }

    public VideoCallFrame(
            String username,
            String otherUser,
            String roomName,
            ClientConnection connection,
            boolean incomingCall,
            Runnable onCloseCallback) {

        initComponents();

        this.username = username;
        this.otherUser = otherUser;
        this.roomName = roomName;
        this.connection = connection;
        this.incomingCall = incomingCall;
        this.onCloseCallback = onCloseCallback;

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                handleWindowClosing();
            }
        });

        setLocationRelativeTo(null);
        if (roomName != null && !roomName.isBlank()) {
            setTitle("Cuộc gọi video nhóm - " + roomName);
            lb_username.setText("CUỘC GỌI VIDEO NHÓM: " + roomName);
        } else {
            setTitle("Video Call - " + (otherUser != null ? otherUser : ""));
            lb_username.setText("CUỘC GỌI VIDEO: " + (otherUser != null ? otherUser : ""));
        }

        setupUI();

        if (incomingCall) {
            lb_callStatus.setText("Đang gọi video đến...");
            btnAccept.setVisible(true);
            btnReject.setVisible(true);
            btnEndCall.setVisible(false);
        } else {
            lb_callStatus.setText("Đang kết nối video...");
            btnAccept.setVisible(false);
            btnReject.setVisible(false);
            btnEndCall.setVisible(true);

            startVideoConnection();
            startLocalCamera();
        }
    }

    private void setupUI() {
        btnAccept.setBackground(new java.awt.Color(22, 163, 74));
        btnAccept.setForeground(java.awt.Color.WHITE);
        btnAccept.setOpaque(true);
        btnAccept.setContentAreaFilled(true);
        btnAccept.setBorderPainted(false);
        btnAccept.setIcon(ClientIcons.getVideoIcon(16, java.awt.Color.WHITE));

        btnReject.setBackground(new java.awt.Color(220, 38, 38));
        btnReject.setForeground(java.awt.Color.WHITE);
        btnReject.setOpaque(true);
        btnReject.setContentAreaFilled(true);
        btnReject.setBorderPainted(false);

        btnEndCall.setBackground(new java.awt.Color(220, 38, 38));
        btnEndCall.setForeground(java.awt.Color.WHITE);
        btnEndCall.setOpaque(true);
        btnEndCall.setContentAreaFilled(true);
        btnEndCall.setBorderPainted(false);

        btnMuteMic.setText("Tắt Mic");
        btnMuteMic.setIcon(ClientIcons.getMicIcon(16, new java.awt.Color(30, 64, 175), false));
        btnMuteMic.setFocusPainted(false);

        btnToggleCamera.setText("Tắt Cam");
        btnToggleCamera.setIcon(ClientIcons.getCamOffIcon(16, new java.awt.Color(30, 64, 175), false));
        btnToggleCamera.setFocusPainted(false);

        lb_username.setIcon(ClientIcons.getVideoIcon(18, java.awt.Color.WHITE));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlHeader = new javax.swing.JPanel();
        lb_username = new javax.swing.JLabel();
        lb_callStatus = new javax.swing.JLabel();
        pnlVideoContainer = new javax.swing.JPanel();
        lb_remoteVideo = new javax.swing.JLabel();
        lb_localvideo = new javax.swing.JLabel();
        btnAccept = new javax.swing.JButton();
        btnReject = new javax.swing.JButton();
        btnMuteMic = new javax.swing.JButton();
        btnToggleCamera = new javax.swing.JButton();
        btnEndCall = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Cuộc gọi video");
        setMinimumSize(new java.awt.Dimension(680, 540));
        setPreferredSize(new java.awt.Dimension(680, 540));
        setResizable(false);

        pnlHeader.setBackground(new java.awt.Color(37, 99, 235));

        lb_username.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lb_username.setForeground(new java.awt.Color(255, 255, 255));
        lb_username.setText("CUỘC GỌI VIDEO");

        lb_callStatus.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lb_callStatus.setForeground(new java.awt.Color(209, 250, 209));
        lb_callStatus.setText("Đang gọi video...");

        javax.swing.GroupLayout pnlHeaderLayout = new javax.swing.GroupLayout(pnlHeader);
        pnlHeader.setLayout(pnlHeaderLayout);
        pnlHeaderLayout.setHorizontalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lb_username)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lb_callStatus)
                .addGap(20, 20, 20))
        );
        pnlHeaderLayout.setVerticalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lb_username)
                    .addComponent(lb_callStatus))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        pnlVideoContainer.setBackground(new java.awt.Color(30, 41, 59));

        lb_remoteVideo.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lb_remoteVideo.setForeground(new java.awt.Color(204, 204, 204));
        lb_remoteVideo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb_remoteVideo.setText("Video người bên kia");

        lb_localvideo.setBackground(new java.awt.Color(51, 65, 85));
        lb_localvideo.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lb_localvideo.setForeground(new java.awt.Color(204, 204, 204));
        lb_localvideo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb_localvideo.setText("Camera của tôi");
        lb_localvideo.setOpaque(true);

        javax.swing.GroupLayout pnlVideoContainerLayout = new javax.swing.GroupLayout(pnlVideoContainer);
        pnlVideoContainer.setLayout(pnlVideoContainerLayout);
        pnlVideoContainerLayout.setHorizontalGroup(
            pnlVideoContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlVideoContainerLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(lb_remoteVideo, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addGap(10, 10, 10)
                .addComponent(lb_localvideo, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10))
        );
        pnlVideoContainerLayout.setVerticalGroup(
            pnlVideoContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlVideoContainerLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(pnlVideoContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lb_remoteVideo, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addGroup(pnlVideoContainerLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 250, Short.MAX_VALUE)
                        .addComponent(lb_localvideo, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(10, 10, 10))
        );

        btnAccept.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnAccept.setText("Chấp nhận");
        btnAccept.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAcceptActionPerformed(evt);
            }
        });

        btnReject.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnReject.setText("Từ chối");
        btnReject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRejectActionPerformed(evt);
            }
        });

        btnMuteMic.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        btnMuteMic.setText("Tắt Mic");
        btnMuteMic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMuteMicActionPerformed(evt);
            }
        });

        btnToggleCamera.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        btnToggleCamera.setText("Tắt Cam");
        btnToggleCamera.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnToggleCameraActionPerformed(evt);
            }
        });

        btnEndCall.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnEndCall.setText("Kết thúc");
        btnEndCall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEndCallActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlVideoContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(btnAccept, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(btnReject, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(btnMuteMic, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(btnToggleCamera, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(btnEndCall, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(50, Short.MAX_VALUE)))
                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnlHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(pnlVideoContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAccept, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnReject, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnMuteMic, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnToggleCamera, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEndCall, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20))
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

    private void btnMuteMicActionPerformed(java.awt.event.ActionEvent evt) {
        micMuted = !micMuted;
        if (micMuted) {
            btnMuteMic.setText("Bật Mic");
            btnMuteMic.setIcon(ClientIcons.getMicIcon(16, new java.awt.Color(220, 38, 38), true));
            btnMuteMic.setForeground(new java.awt.Color(220, 38, 38));
        } else {
            btnMuteMic.setText("Tắt Mic");
            btnMuteMic.setIcon(ClientIcons.getMicIcon(16, new java.awt.Color(30, 64, 175), false));
            btnMuteMic.setForeground(new java.awt.Color(30, 64, 175));
        }
    }

    private void btnToggleCameraActionPerformed(java.awt.event.ActionEvent evt) {
        cameraOff = !cameraOff;
        if (cameraOff) {
            btnToggleCamera.setText("Bật Cam");
            btnToggleCamera.setIcon(ClientIcons.getCamOffIcon(16, new java.awt.Color(220, 38, 38), true));
            btnToggleCamera.setForeground(new java.awt.Color(220, 38, 38));
            lb_localvideo.setIcon(null);
            lb_localvideo.setText("Camera đã tắt");
        } else {
            btnToggleCamera.setText("Tắt Cam");
            btnToggleCamera.setIcon(ClientIcons.getCamOffIcon(16, new java.awt.Color(30, 64, 175), false));
            btnToggleCamera.setForeground(new java.awt.Color(30, 64, 175));
            lb_localvideo.setText("Camera của tôi");
        }
    }

    private void lb_callStatusFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_lb_callStatusFocusGained
        // TODO add your handling code here:
    }//GEN-LAST:event_lb_callStatusFocusGained

    /**
     * @param args the command line arguments
     */
   public void videoCallEnded() {
       logCallHistory();
       stopLocalCamera();
       stopVideoConnection();
       if (onCloseCallback != null) {
           onCloseCallback.run();
       }
       dispose();
   }

    public void videoCallAccepted() {
        callConnected = true;
        callStartTime = System.currentTimeMillis();
        lb_callStatus.setText(
                "Đang trong cuộc gọi video với " + otherUser
        );

        btnAccept.setVisible(false);
        btnReject.setVisible(false);
        btnEndCall.setVisible(true);
    }

    public void videoCallRejected() {
        logCallHistory();
        stopLocalCamera();
        stopVideoConnection();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        dispose();
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

            callConnected = true;
            callStartTime = System.currentTimeMillis();
            lb_callStatus.setText("Đang trong cuộc gọi video...");

            btnAccept.setVisible(false);
            btnReject.setVisible(false);
            btnEndCall.setVisible(true);

            startLocalCamera();
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
        logCallHistory();
        Message message = new Message(
                MessageType.VIDEO_CALL_REJECT,
                username,
                "Rejected"
        );
        message.setReceiver(otherUser);

        try {
            connection.send(message);
            lb_callStatus.setText("Đã từ chối cuộc gọi video.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể từ chối cuộc gọi video: "
                    + e.getMessage()
            );
        }
        stopLocalCamera();
        stopVideoConnection();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        dispose();
    }

    private void endVideoCall() {
        logCallHistory();
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
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
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
    private javax.swing.JButton btnAccept;
    private javax.swing.JButton btnEndCall;
    private javax.swing.JButton btnMuteMic;
    private javax.swing.JButton btnReject;
    private javax.swing.JButton btnToggleCamera;
    private javax.swing.JLabel lb_callStatus;
    private javax.swing.JLabel lb_localvideo;
    private javax.swing.JLabel lb_remoteVideo;
    private javax.swing.JLabel lb_username;
    private javax.swing.JPanel pnlHeader;
    private javax.swing.JPanel pnlVideoContainer;
    // End of variables declaration//GEN-END:variables
}
