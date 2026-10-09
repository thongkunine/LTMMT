package client;

import common.Message;
import common.MessageType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Khung Chat Riêng 2 Người (NetBeans GUI Form)
 */
public class PrivateChatFrame extends javax.swing.JFrame {

    private String username;
    private String targetUser;
    private ClientConnection connection;
    private chatFramForm parentFrame;

    private java.util.List<Message> fileMessageList = new java.util.ArrayList<>();

    public PrivateChatFrame() {
        initComponents();
    }

    public PrivateChatFrame(String username, String targetUser, ClientConnection connection, chatFramForm parentFrame) {
        initComponents();
        this.username = username;
        this.targetUser = targetUser;
        this.connection = connection;
        this.parentFrame = parentFrame;

        setTitle("Chat riêng: " + targetUser);
        lbUserTitle.setText("Trò chuyện với: " + targetUser);
        setupUI();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                if (parentFrame != null) {
                    parentFrame.onPrivateChatClosed(targetUser);
                }
            }
        });
    }

    private void setupUI() {
        btnSend.setBackground(new java.awt.Color(37, 99, 235));
        btnSend.setForeground(java.awt.Color.WHITE);
        btnSend.setOpaque(true);
        btnSend.setContentAreaFilled(true);
        btnSend.setBorderPainted(false);
        btnSend.setFocusPainted(false);

        btnSendFile.setText("File");
        btnSendFile.setIcon(ClientIcons.getFileIcon(14, new java.awt.Color(37, 99, 235)));

        btnVoiceCall.setText("Thoại");
        btnVoiceCall.setIcon(ClientIcons.getPhoneIcon(14, new java.awt.Color(22, 163, 74)));

        btnVideoCall.setText("Video");
        btnVideoCall.setIcon(ClientIcons.getVideoIcon(14, new java.awt.Color(220, 38, 38)));

        lbStatus.setText("Đang hoạt động");
        lbStatus.setIcon(ClientIcons.getOnlineUserIcon(10, new java.awt.Color(34, 197, 94)));

        txtChatArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    try {
                        String text = txtChatArea.getText();
                        int caretPos = txtChatArea.getCaretPosition();
                        int lineNum = txtChatArea.getLineOfOffset(Math.min(caretPos, text.length()));
                        int start = txtChatArea.getLineStartOffset(lineNum);
                        int end = txtChatArea.getLineEndOffset(lineNum);
                        String lineText = text.substring(start, end).trim();

                        for (Message msg : fileMessageList) {
                            if (msg.getFileName() != null && lineText.contains(msg.getFileName())) {
                                saveReceivedFile(msg);
                                return;
                            }
                        }

                        for (Message msg : fileMessageList) {
                            if (msg.getFileName() != null && (lineText.toLowerCase().contains("file") || lineText.contains(msg.getFileName()))) {
                                saveReceivedFile(msg);
                                return;
                            }
                        }

                        if (!fileMessageList.isEmpty()) {
                            saveReceivedFile(fileMessageList.get(fileMessageList.size() - 1));
                        }
                    } catch (Exception ex) {
                        if (!fileMessageList.isEmpty()) {
                            saveReceivedFile(fileMessageList.get(fileMessageList.size() - 1));
                        }
                    }
                }
            }
        });
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setInitialHistory(String historyText) {
        if (historyText != null && !historyText.isBlank()) {
            txtChatArea.setText(historyText);
            txtChatArea.setCaretPosition(txtChatArea.getDocument().getLength());
        }
    }

    public void appendHistory(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String current = txtChatArea.getText();
        if (current.contains(text)) {
            return;
        }
        append(text);
    }

    public void receiveMessage(Message message) {
        if (message == null) {
            return;
        }
        // Bỏ qua bản ghi echo từ server nếu là chính mình gửi
        if (username != null && username.equals(message.getSender())) {
            return;
        }
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        if (message.getType() == MessageType.FILE_MESSAGE) {
            fileMessageList.add(message);
            String formatted = "[" + timeStr + "] " + message.getSender() + " đã gửi file: " + message.getFileName() + " (Nhấp đúp để lưu file)";
            append(formatted);
            if (parentFrame != null) {
                parentFrame.recordPrivateMessageHistory(targetUser, formatted);
            }

            int choice = JOptionPane.showConfirmDialog(this,
                    message.getSender() + " đã gửi file:\n" + message.getFileName() + "\n\nBạn có muốn lưu file ngay không?",
                    "Nhận file", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                saveReceivedFile(message);
            }
        } else {
            String formatted = "[" + timeStr + "] " + message.getSender() + ": " + message.getContent();
            append(formatted);
            if (parentFrame != null) {
                parentFrame.recordPrivateMessageHistory(targetUser, formatted);
            }
        }
        setVisible(true);
        toFront();
        requestFocus();
    }

    public void saveReceivedFile(Message message) {
        if (message == null || message.getFileData() == null) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu file!");
            return;
        }

        String safeFileName = new File(message.getFileName()).getName();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(safeFileName));

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File saveFile = fileChooser.getSelectedFile();
        try {
            Files.write(saveFile.toPath(), message.getFileData());
            JOptionPane.showMessageDialog(this, "Lưu file thành công!\n" + saveFile.getAbsolutePath());
            append("[File] Đã lưu: " + saveFile.getName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể lưu file: " + e.getMessage());
        }
    }

    public void append(String text) {
        String current = txtChatArea.getText();
        if (!current.isEmpty() && !current.endsWith("\n") && !current.endsWith(System.lineSeparator())) {
            txtChatArea.append(System.lineSeparator());
        }
        txtChatArea.append(text + System.lineSeparator());
        txtChatArea.setCaretPosition(txtChatArea.getDocument().getLength());
    }

    private void sendMessage() {
        String content = txtInput.getText().trim();
        if (content.isEmpty() || connection == null) {
            return;
        }

        Message msg = new Message(MessageType.PRIVATE_MESSAGE, username, content);
        msg.setReceiver(targetUser);

        try {
            connection.send(msg);
            String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String formatted = "[" + timeStr + "] Bạn: " + content;
            append(formatted);
            if (parentFrame != null) {
                parentFrame.recordPrivateMessageHistory(targetUser, formatted);
            }
            txtInput.setText("");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể gửi tin nhắn: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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

        pnlHeader = new javax.swing.JPanel();
        lbUserTitle = new javax.swing.JLabel();
        lbStatus = new javax.swing.JLabel();
        btnSendFile = new javax.swing.JButton();
        btnVoiceCall = new javax.swing.JButton();
        btnVideoCall = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtChatArea = new javax.swing.JTextArea();
        txtInput = new javax.swing.JTextField();
        btnSend = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Tin nhắn riêng");
        setMinimumSize(new java.awt.Dimension(550, 450));

        pnlHeader.setBackground(new java.awt.Color(37, 99, 235));

        lbUserTitle.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lbUserTitle.setForeground(new java.awt.Color(255, 255, 255));
        lbUserTitle.setText("Trò chuyện với...");

        lbStatus.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lbStatus.setForeground(new java.awt.Color(209, 250, 209));
        lbStatus.setText("● Đang hoạt động");

        btnSendFile.setText("📁 File");
        btnSendFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendFileActionPerformed(evt);
            }
        });

        btnVoiceCall.setText("📞 Thoại");
        btnVoiceCall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVoiceCallActionPerformed(evt);
            }
        });

        btnVideoCall.setText("📹 Video");
        btnVideoCall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVideoCallActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlHeaderLayout = new javax.swing.GroupLayout(pnlHeader);
        pnlHeader.setLayout(pnlHeaderLayout);
        pnlHeaderLayout.setHorizontalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbUserTitle)
                    .addComponent(lbStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnSendFile)
                .addGap(8, 8, 8)
                .addComponent(btnVoiceCall)
                .addGap(8, 8, 8)
                .addComponent(btnVideoCall)
                .addGap(12, 12, 12))
        );
        pnlHeaderLayout.setVerticalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlHeaderLayout.createSequentialGroup()
                        .addComponent(lbUserTitle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lbStatus))
                    .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnSendFile, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnVoiceCall, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnVideoCall, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        txtChatArea.setEditable(false);
        txtChatArea.setColumns(20);
        txtChatArea.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtChatArea.setLineWrap(true);
        txtChatArea.setRows(5);
        txtChatArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(txtChatArea);

        txtInput.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtInputActionPerformed(evt);
            }
        });

        btnSend.setBackground(new java.awt.Color(37, 99, 235));
        btnSend.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnSend.setForeground(new java.awt.Color(255, 255, 255));
        btnSend.setText("Gửi");
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnlHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(txtInput, javax.swing.GroupLayout.DEFAULT_SIZE, 410, Short.MAX_VALUE)
                        .addGap(10, 10, 10)
                        .addComponent(btnSend, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(15, 15, 15))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(pnlHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 290, Short.MAX_VALUE)
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtInput, javax.swing.GroupLayout.DEFAULT_SIZE, 38, Short.MAX_VALUE)
                    .addComponent(btnSend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(15, 15, 15))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
        sendMessage();
    }//GEN-LAST:event_btnSendActionPerformed

    private void txtInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtInputActionPerformed
        sendMessage();
    }//GEN-LAST:event_txtInputActionPerformed

    private void btnSendFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendFileActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        try {
            byte[] fileData = Files.readAllBytes(selectedFile.toPath());
            Message message = new Message(MessageType.FILE_MESSAGE, username, "Gửi file");
            message.setReceiver(targetUser);
            message.setFileName(selectedFile.getName());
            message.setFileData(fileData);

            fileMessageList.add(message);
            connection.send(message);

            String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            append("[" + timeStr + "] Bạn đã gửi file: " + selectedFile.getName() + " (Nhấp đúp để lưu file)");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi đọc file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSendFileActionPerformed

    private void btnVoiceCallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVoiceCallActionPerformed
        if (parentFrame != null) {
            parentFrame.startVoiceCallWith(targetUser);
        }
    }//GEN-LAST:event_btnVoiceCallActionPerformed

    private void btnVideoCallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVideoCallActionPerformed
        if (parentFrame != null) {
            parentFrame.startVideoCallWith(targetUser);
        }
    }//GEN-LAST:event_btnVideoCallActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSend;
    private javax.swing.JButton btnSendFile;
    private javax.swing.JButton btnVideoCall;
    private javax.swing.JButton btnVoiceCall;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbStatus;
    private javax.swing.JLabel lbUserTitle;
    private javax.swing.JPanel pnlHeader;
    private javax.swing.JTextArea txtChatArea;
    private javax.swing.JTextField txtInput;
    // End of variables declaration//GEN-END:variables
}
