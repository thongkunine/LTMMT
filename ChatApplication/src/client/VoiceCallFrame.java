/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package client;

/**
 *
 * @author Admin
 */
import common.Message;
import common.MessageType;
import common.Protocol;
import java.io.IOException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.JOptionPane;

public class VoiceCallFrame extends javax.swing.JFrame {

    /**
     * Creates new form VoiceCallFrame
     */
    private String username;
    private String otherUser;
    private String roomName;
    private ClientConnection connection;
    private VoiceCallManager voiceCallManager;
    private VoiceClient voiceClient;
    private boolean incomingCall;
    private Runnable onCloseCallback;
    private volatile boolean inCall = false;
    private long callStartTime = 0;
    private boolean callConnected = false;
    private boolean historyLogged = false;

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

        String logContent = "[Cuộc gọi thoại] - Thời lượng: " + durationStr;

        if (roomName != null && !roomName.isBlank()) {
            Message historyMsg = new Message(MessageType.CHAT, username, logContent, roomName);
            try {
                if (connection != null) {
                    connection.send(historyMsg);
                }
            } catch (IOException e) {
                System.out.println("Lỗi gửi lịch sử cuộc gọi thoại phòng: " + e.getMessage());
            }
        } else if (otherUser != null && !otherUser.isBlank()) {
            Message historyMsg = new Message(MessageType.PRIVATE_MESSAGE, username, logContent);
            historyMsg.setReceiver(otherUser);
            try {
                if (connection != null) {
                    connection.send(historyMsg);
                }
            } catch (IOException e) {
                System.out.println("Lỗi gửi lịch sử cuộc gọi thoại: " + e.getMessage());
            }
        }
    }

    // Constructor mặc định cho NetBeans
    public VoiceCallFrame() {
        initComponents();
    }

    // Constructor dùng cho cuộc gọi (không có callback)
    public VoiceCallFrame(
            String username,
            String otherUser,
            ClientConnection connection,
            boolean incomingCall) {
        this(username, otherUser, null, connection, incomingCall, null);
    }

    // Constructor đầy đủ có callback khi đóng cuộc gọi
    public VoiceCallFrame(
            String username,
            String otherUser,
            ClientConnection connection,
            boolean incomingCall,
            Runnable onCloseCallback) {
        this(username, otherUser, null, connection, incomingCall, onCloseCallback);
    }

    // Constructor có thêm phòng chat (dùng cho gọi nhóm)
    public VoiceCallFrame(
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

        this.voiceCallManager = new VoiceCallManager();
        this.voiceClient = new VoiceClient();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                handleWindowClosing();
            }
        });

        setLocationRelativeTo(null);
        if (roomName != null && !roomName.isBlank()) {
            setTitle("Cuộc gọi thoại nhóm - " + roomName);
            lb_callername.setText(incomingCall ? (otherUser != null ? otherUser : "Nhóm " + roomName) : "Nhóm: " + roomName);
        } else {
            setTitle("Cuộc gọi thoại - " + (otherUser != null ? otherUser : ""));
            lb_callername.setText(otherUser != null ? otherUser : "");
        }

        setupUI();

        if (incomingCall) {
            lb_callstatus.setText("Đang gọi thoại đến...");
            btnAccept.setVisible(true);
            btnReject.setVisible(true);
            btnEndCall.setVisible(false);
        } else {
            lb_callstatus.setText("Đang đổ chuông...");
            btnAccept.setVisible(false);
            btnReject.setVisible(false);
            btnEndCall.setVisible(true);
        }
    }

    private void setupUI() {
        btnAccept.setBackground(new java.awt.Color(22, 163, 74));
        btnAccept.setForeground(java.awt.Color.WHITE);
        btnAccept.setOpaque(true);
        btnAccept.setContentAreaFilled(true);
        btnAccept.setBorderPainted(false);
        btnAccept.setIcon(ClientIcons.getPhoneIcon(16, java.awt.Color.WHITE));

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

        btnToggleSpeaker.setText("Loa ngoài");
        btnToggleSpeaker.setIcon(ClientIcons.getSpeakerIcon(16, new java.awt.Color(30, 64, 175), false));
        btnToggleSpeaker.setFocusPainted(false);

        lb_call.setIcon(ClientIcons.getPhoneIcon(18, java.awt.Color.WHITE));
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
        lb_call = new javax.swing.JLabel();
        lb_callername = new javax.swing.JLabel();
        lb_callstatus = new javax.swing.JLabel();
        btnAccept = new javax.swing.JButton();
        btnReject = new javax.swing.JButton();
        btnMuteMic = new javax.swing.JButton();
        btnToggleSpeaker = new javax.swing.JButton();
        btnEndCall = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Cuộc gọi thoại");
        setMinimumSize(new java.awt.Dimension(420, 360));
        setPreferredSize(new java.awt.Dimension(420, 360));
        setResizable(false);

        pnlHeader.setBackground(new java.awt.Color(37, 99, 235));

        lb_call.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lb_call.setForeground(new java.awt.Color(255, 255, 255));
        lb_call.setText("CUỘC GỌI THOẠI");

        javax.swing.GroupLayout pnlHeaderLayout = new javax.swing.GroupLayout(pnlHeader);
        pnlHeader.setLayout(pnlHeaderLayout);
        pnlHeaderLayout.setHorizontalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lb_call)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlHeaderLayout.setVerticalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(lb_call)
                .addContainerGap(15, Short.MAX_VALUE))
        );

        lb_callername.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lb_callername.setForeground(new java.awt.Color(0, 51, 153));
        lb_callername.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb_callername.setText("User Name");

        lb_callstatus.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lb_callstatus.setForeground(new java.awt.Color(102, 102, 102));
        lb_callstatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb_callstatus.setText("Đang gọi đến...");

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

        btnToggleSpeaker.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        btnToggleSpeaker.setText("Loa ngòai");
        btnToggleSpeaker.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnToggleSpeakerActionPerformed(evt);
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
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lb_callername, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lb_callstatus, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnAccept, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(40, 40, 40)
                                .addComponent(btnReject, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnMuteMic, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(40, 40, 40)
                                .addComponent(btnToggleSpeaker, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(30, 30, 30))
            .addGroup(layout.createSequentialGroup()
                .addGap(120, 120, 120)
                .addComponent(btnEndCall, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnlHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25)
                .addComponent(lb_callername)
                .addGap(10, 10, 10)
                .addComponent(lb_callstatus)
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAccept, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnReject, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnMuteMic, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnToggleSpeaker, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(btnEndCall, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnAcceptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAcceptActionPerformed
        // TODO add your handling code here:
        acceptCall();
    }//GEN-LAST:event_btnAcceptActionPerformed

    private void btnRejectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRejectActionPerformed
        // TODO add your handling code here:
        rejectCall();
    }//GEN-LAST:event_btnRejectActionPerformed

    private void btnEndCallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEndCallActionPerformed
        // TODO add your handling code here:
        endCall();
    }//GEN-LAST:event_btnEndCallActionPerformed

    private void btnMuteMicActionPerformed(java.awt.event.ActionEvent evt) {
        if (voiceCallManager == null) return;
        boolean muted = !voiceCallManager.isMicMuted();
        voiceCallManager.setMicMuted(muted);
        if (muted) {
            btnMuteMic.setText("Bật Mic");
            btnMuteMic.setIcon(ClientIcons.getMicIcon(16, new java.awt.Color(220, 38, 38), true));
            btnMuteMic.setForeground(new java.awt.Color(220, 38, 38));
        } else {
            btnMuteMic.setText("Tắt Mic");
            btnMuteMic.setIcon(ClientIcons.getMicIcon(16, new java.awt.Color(30, 64, 175), false));
            btnMuteMic.setForeground(new java.awt.Color(30, 64, 175));
        }
    }

    private void btnToggleSpeakerActionPerformed(java.awt.event.ActionEvent evt) {
        if (voiceCallManager == null) return;
        boolean muted = !voiceCallManager.isSpeakerMuted();
        voiceCallManager.setSpeakerMuted(muted);
        if (muted) {
            btnToggleSpeaker.setText("Tắt Loa");
            btnToggleSpeaker.setIcon(ClientIcons.getSpeakerIcon(16, new java.awt.Color(220, 38, 38), true));
            btnToggleSpeaker.setForeground(new java.awt.Color(220, 38, 38));
        } else {
            btnToggleSpeaker.setText("Loa ngoài");
            btnToggleSpeaker.setIcon(ClientIcons.getSpeakerIcon(16, new java.awt.Color(30, 64, 175), false));
            btnToggleSpeaker.setForeground(new java.awt.Color(30, 64, 175));
        }
    }
    private synchronized void startVoiceStreams() {
        if (inCall) {
            return;
        }
        try {
            voiceCallManager.start();
            String target = (roomName != null && !roomName.isBlank()) ? roomName : (otherUser != null ? otherUser : "");
            voiceClient.connect(Protocol.HOST, Protocol.VOICE_PORT, username, target);
            voiceClient.startStreaming(voiceCallManager);
            inCall = true;
        } catch (LineUnavailableException e) {
            JOptionPane.showMessageDialog(this, "Không thể mở microphone hoặc loa: " + e.getMessage());
            endCall();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối đến VoiceServer: " + e.getMessage());
            endCall();
        }
    }

    public void receiveAudio(byte[] data) {
        if (voiceCallManager != null && inCall && data != null) {
            voiceCallManager.playAudio(data);
        }
    }

    private synchronized void stopVoiceStreams() {
        inCall = false;
        if (voiceClient != null) {
            voiceClient.disconnect();
        }
        if (voiceCallManager != null) {
            voiceCallManager.stop();
        }
    }

    private void handleWindowClosing() {
        if (incomingCall && !inCall) {
            rejectCall();
        } else {
            endCall();
        }
    }

    private void endCall() {
        logCallHistory();
        Message message = new Message(
                MessageType.CALL_END,
                username,
                "Ended"
        );
        message.setReceiver(otherUser);

        try {
            connection.send(message);
        } catch (IOException e) {
            System.out.println(
                    "Không thể gửi CALL_END: " + e.getMessage()
            );
        }

        stopVoiceStreams();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        dispose();
    }

    private void rejectCall() {
        logCallHistory();
        Message message = new Message(
                MessageType.CALL_REJECT,
                username,
                "Rejected"
        );
        message.setReceiver(otherUser);

        try {
            connection.send(message);
            lb_callstatus.setText("Đã từ chối cuộc gọi.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể từ chối cuộc gọi: " + e.getMessage()
            );
        }

        stopVoiceStreams();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        dispose();
    }

    private void acceptCall() {
        Message message = new Message(
                MessageType.CALL_ACCEPT,
                username,
                "Accepted"
        );
        message.setReceiver(otherUser);

        try {
            connection.send(message);

            callConnected = true;
            callStartTime = System.currentTimeMillis();
            lb_callstatus.setText("Đang trong cuộc gọi...");
            btnAccept.setVisible(false);
            btnReject.setVisible(false);
            btnEndCall.setVisible(true);

            startVoiceStreams();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể chấp nhận cuộc gọi: " + e.getMessage()
            );
        }
    }

    public void callAccepted() {
        callConnected = true;
        callStartTime = System.currentTimeMillis();
        lb_callstatus.setText("Đang trong cuộc gọi...");
        btnAccept.setVisible(false);
        btnReject.setVisible(false);
        btnEndCall.setVisible(true);

        startVoiceStreams();
    }

    public void callRejected() {
        logCallHistory();
        lb_callstatus.setText("Cuộc gọi bị từ chối.");
        stopVoiceStreams();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        dispose();
    }

    public void callEnded() {
        logCallHistory();
        lb_callstatus.setText("Cuộc gọi đã kết thúc.");
        stopVoiceStreams();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        dispose();
    }
    /**
     * @param args the command line arguments
     */
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
            java.util.logging.Logger.getLogger(VoiceCallFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(VoiceCallFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(VoiceCallFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VoiceCallFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VoiceCallFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAccept;
    private javax.swing.JButton btnEndCall;
    private javax.swing.JButton btnMuteMic;
    private javax.swing.JButton btnReject;
    private javax.swing.JButton btnToggleSpeaker;
    private javax.swing.JLabel lb_call;
    private javax.swing.JLabel lb_callername;
    private javax.swing.JLabel lb_callstatus;
    private javax.swing.JPanel pnlHeader;
    // End of variables declaration//GEN-END:variables
}
