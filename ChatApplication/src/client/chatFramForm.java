package client;


import client.ClientConnection;
import common.Message;
import common.MessageType;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import java.io.IOException;
import javax.swing.JOptionPane;
import java.io.File;
import java.nio.file.Files;
import javax.swing.JFileChooser;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author Admin
 */
public class chatFramForm extends javax.swing.JFrame {
    private String username;
    private ClientConnection connection;
    private String currentRoom = "";
    /**
     * Creates new form chatFramForm
     */
    private final DefaultListModel<String> roomModel =
        new DefaultListModel<>();

private final DefaultListModel<String> userModel =
        new DefaultListModel<>();
    public chatFramForm() {
        initComponents();
    }
public chatFramForm(String username, ClientConnection connection) {
    initComponents();

    this.username = username;
    this.connection = connection;
    lb_namelogin.setText("Dang dang nhap:" +username);

    // Gắn model cho danh sách phòng và user
    lstRoom.setModel(roomModel);
    lsUsers.setModel(userModel);

    setTitle("Chat - " + username);
    setLocationRelativeTo(null);

    connection.setListener(new ClientConnection.MessageListener() {

        @Override
        public void onMessage(Message message) {
            SwingUtilities.invokeLater(() -> {
                handleServerMessage(message);
            });
        }

        @Override
        public void onDisconnected() {
            SwingUtilities.invokeLater(() -> {
                append("* Disconnected from server.");

                txtMessage.setEnabled(false);
                btnSend.setEnabled(false);
                btnCreateRoom.setEnabled(false);
                btnPrivateMessage.setEnabled(false);
                lstRoom.setEnabled(false);
                lsUsers.setEnabled(false);
            });
        }
    });

    connection.startListening();
}

public void handleServerMessage(Message message) {

    switch (message.getType()) {

        case LOGIN_OK -> {
            currentRoom =
                    message.getRoom() == null ? "" : message.getRoom();

            setTitle("Chat - " + username + " @ " + currentRoom);

            append(message.getContent());
        }

        case CHAT -> {
            append(
                    message.getSender()
                    + ": "
                    + message.getContent()
            );
        }

        case HISTORY -> {
            append(
                    "[Lịch sử] "
                    + message.getSender()
                    + ": "
                    + message.getContent()
            );
        }

        case SYSTEM, JOIN, LEAVE -> {
            append("* " + message.getContent());
        }

        case ROOM_LIST -> {
            updateList(roomModel, message.getContent());
        }

        case USER_LIST -> {
            updateList(userModel, message.getContent());
}
        case PRIVATE_MESSAGE -> {
            append(
            "[Riêng] "
            + message.getSender()
            + " -> "
            + message.getReceiver()
            + ": "
            + message.getContent()
    );
}
        case FILE_MESSAGE -> {
            handleReceivedFile(message);
}
        default -> {
        }
    }
}
private void handleReceivedFile(Message message) {

    // Nếu đây là bản xác nhận gửi lại cho chính người gửi
    if (username.equals(message.getSender())) {

        append("[File] Đã gửi "
                + message.getFileName()
                + " cho "
                + message.getReceiver());

        return;
    }

    // Hiển thị trên khung chat
    append("[File] "
            + message.getSender()
            + " đã gửi cho bạn: "
            + message.getFileName());

    // Hỏi người nhận có muốn lưu không
    int choice = JOptionPane.showConfirmDialog(
            this,
            message.getSender()
                    + " đã gửi file:\n"
                    + message.getFileName()
                    + "\n\nBạn có muốn lưu file không?",
            "Nhận file",
            JOptionPane.YES_NO_OPTION
    );

    if (choice != JOptionPane.YES_OPTION) {
        return;
    }

    saveReceivedFile(message);
}
private void saveReceivedFile(Message message) {

    if (message.getFileData() == null) {
        JOptionPane.showMessageDialog(
                this,
                "Không có dữ liệu file!"
        );
        return;
    }

    // Chỉ lấy tên file, tránh đường dẫn không hợp lệ
    String safeFileName =
            new File(message.getFileName()).getName();

    JFileChooser fileChooser = new JFileChooser();

    // Đặt sẵn tên file
    fileChooser.setSelectedFile(
            new File(safeFileName)
    );

    int result = fileChooser.showSaveDialog(this);

    if (result != JFileChooser.APPROVE_OPTION) {
        return;
    }

    File saveFile = fileChooser.getSelectedFile();

    try {

        Files.write(
                saveFile.toPath(),
                message.getFileData()
        );

        JOptionPane.showMessageDialog(
                this,
                "Lưu file thành công!\n"
                + saveFile.getAbsolutePath()
        );

        append("[File] Đã lưu: "
                + saveFile.getName());

    } catch (IOException e) {

        JOptionPane.showMessageDialog(
                this,
                "Không thể lưu file: "
                + e.getMessage()
        );
    }
}
private void append(String text) {
    txtChatArea.append(text + System.lineSeparator());

    txtChatArea.setCaretPosition(
            txtChatArea.getDocument().getLength()
    );
}
private void updateList(
        DefaultListModel<String> model,
        String csv) {

    model.clear();

    if (csv == null || csv.isBlank()) {
        return;
    }

    for (String item : csv.split(",")) {
        if (!item.isBlank()) {
            model.addElement(item.trim());
        }
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

        label1 = new java.awt.Label();
        label2 = new java.awt.Label();
        label3 = new java.awt.Label();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtChatArea = new javax.swing.JTextArea();
        txtMessage = new javax.swing.JTextField();
        btnSend = new javax.swing.JButton();
        btnCreateRoom = new javax.swing.JButton();
        btnPrivateMessage = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lsUsers = new javax.swing.JList<>();
        label4 = new java.awt.Label();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstRoom = new javax.swing.JList<>();
        lb_namelogin = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        btn_sendfile = new javax.swing.JButton();
        btn_call = new javax.swing.JButton();
        btn_callvideo = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setFont(new java.awt.Font("Times New Roman", 0, 10)); // NOI18N
        setSize(new java.awt.Dimension(900, 600));

        label1.setText("phòng chat ");
        label1.setVisible(false);

        label2.setText("phòng chat ");

        label3.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N
        label3.setText("CHAT APP");

        txtChatArea.setEditable(false);
        txtChatArea.setColumns(20);
        txtChatArea.setRows(5);
        txtChatArea.setName(""); // NOI18N
        jScrollPane1.setViewportView(txtChatArea);

        txtMessage.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N
        txtMessage.setToolTipText("");
        txtMessage.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        txtMessage.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));

        btnSend.setText("send");
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        btnCreateRoom.setText("TẠO PHÒNG");
        btnCreateRoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateRoomActionPerformed(evt);
            }
        });

        btnPrivateMessage.setText("NHẮN TIN RIÊNG");
        btnPrivateMessage.setToolTipText("");
        btnPrivateMessage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrivateMessageActionPerformed(evt);
            }
        });

        jLabel1.setText("nguoi online");

        lsUsers.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                lsUsersAncestorAdded(evt);
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        jScrollPane2.setViewportView(lsUsers);

        label4.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N
        label4.setText("danh sách phong ");

        lstRoom.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstRoomValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(lstRoom);

        lb_namelogin.setText("long in :");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        btn_sendfile.setText("gữi file ");
        btn_sendfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_sendfileActionPerformed(evt);
            }
        });

        btn_call.setText("gọi thoại ");

        btn_callvideo.setText("gọi video");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addComponent(lb_namelogin, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGap(48, 48, 48)
                                        .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(294, 294, 294))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(100, 100, 100)
                                        .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(89, 89, 89))))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(label2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btn_sendfile, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(68, 68, 68)
                                .addComponent(btnPrivateMessage)
                                .addGap(93, 93, 93)
                                .addComponent(btn_call)
                                .addGap(88, 88, 88)
                                .addComponent(btn_callvideo)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 458, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 458, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(57, 57, 57)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnCreateRoom)
                            .addComponent(jLabel1)))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(btnSend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(label4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(90, 90, 90))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(label3, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(label1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(63, 63, 63)
                        .addComponent(lb_namelogin))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(label2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(label4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnCreateRoom)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnSend, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 76, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnPrivateMessage)
                    .addComponent(btn_sendfile)
                    .addComponent(btn_call)
                    .addComponent(btn_callvideo))
                .addGap(16, 16, 16))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnPrivateMessageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrivateMessageActionPerformed
        // TODO add your handling code here:
        sendPrivateMessage();
    }//GEN-LAST:event_btnPrivateMessageActionPerformed
private void sendPrivateMessage() {

    // Lấy người đang được chọn trong danh sách online
    String receiver = lsUsers.getSelectedValue();

    if (receiver == null || receiver.isBlank()) {
        JOptionPane.showMessageDialog(
                this,
                "Vui lòng chọn người muốn nhắn tin riêng!"
        );
        return;
    }

    // Không cho nhắn cho chính mình
    if (receiver.equals(username)) {
        JOptionPane.showMessageDialog(
                this,
                "Bạn không thể nhắn tin riêng cho chính mình!"
        );
        return;
    }

    // Hiện hộp thoại nhập nội dung
    String content = JOptionPane.showInputDialog(
            this,
            "Nhắn tin riêng cho " + receiver + ":"
    );

    if (content == null || content.trim().isEmpty()) {
        return;
    }

    Message message = new Message(
            MessageType.PRIVATE_MESSAGE,
            username,
            content.trim()
    );

    message.setReceiver(receiver);

    try {
        connection.send(message);
    } catch (IOException e) {
        append("Không thể gửi tin nhắn riêng: " + e.getMessage());
    }
}
    private void lsUsersAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_lsUsersAncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_lsUsersAncestorAdded

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
    sendChat();        // TODO add your handling code here:
    }//GEN-LAST:event_btnSendActionPerformed

    private void btnCreateRoomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateRoomActionPerformed
        // TODO add your handling code here:
        createRoom();
    }//GEN-LAST:event_btnCreateRoomActionPerformed

    private void lstRoomValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstRoomValueChanged
        // TODO add your handling code here:
         if (!evt.getValueIsAdjusting()) {
        joinSelectedRoom();
    }
         
    }//GEN-LAST:event_lstRoomValueChanged

    private void btn_sendfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_sendfileActionPerformed
        // TODO add your handling code here:
        sendFile();
    }//GEN-LAST:event_btn_sendfileActionPerformed

    private void sendFile() {

    // 1. Lấy người nhận từ danh sách online
    String receiver = lsUsers.getSelectedValue();

    if (receiver == null || receiver.isBlank()) {
        JOptionPane.showMessageDialog(
                this,
                "Vui lòng chọn người nhận file!"
        );
        return;
    }

    // Không gửi file cho chính mình
    if (receiver.equals(username)) {
        JOptionPane.showMessageDialog(
                this,
                "Bạn không thể gửi file cho chính mình!"
        );
        return;
    }

    // 2. Mở cửa sổ chọn file
    JFileChooser fileChooser = new JFileChooser();

    int result = fileChooser.showOpenDialog(this);

    // Người dùng bấm Cancel
    if (result != JFileChooser.APPROVE_OPTION) {
        return;
    }

    // 3. Lấy file được chọn
    File file = fileChooser.getSelectedFile();

    long maxFileSize = 10 * 1024 * 1024;

    if (file.length() > maxFileSize) {
    JOptionPane.showMessageDialog(
            this,
            "File quá lớn! Vui lòng chọn file nhỏ hơn 10 MB.");
        return;
}
    

    try {

        // 4. Đọc toàn bộ file thành byte[]
        byte[] fileData = Files.readAllBytes(file.toPath());

        // 5. Tạo message
        Message message = new Message(
                MessageType.FILE_MESSAGE,
                username,
                "Gửi file"
        );

        message.setReceiver(receiver);
        message.setFileName(file.getName());
        message.setFileData(fileData);

        // 6. Gửi lên server
        connection.send(message);

        JOptionPane.showMessageDialog(
                this,
                "Đã gửi file "
                + file.getName()
                + " cho "
                + receiver
        );

    } catch (IOException e) {

        JOptionPane.showMessageDialog(
                this,
                "Không thể gửi file: " + e.getMessage()
        );
    }
}private void joinSelectedRoom() {

    String selectedRoom = lstRoom.getSelectedValue();

    if (selectedRoom == null || selectedRoom.isBlank()) {
        return;
    }

    // Đang ở phòng này rồi thì không JOIN lại
    if (selectedRoom.equals(currentRoom)) {
        return;
    }

    Message message = new Message(
            MessageType.JOIN_ROOM,
            username,
            selectedRoom
    );

    try {
        connection.send(message);
    } catch (IOException e) {
        append("Không thể tham gia phòng: " + e.getMessage());
    }
}
    private void createRoom() {

    String roomName = JOptionPane.showInputDialog(
            this,
            "Nhập tên phòng mới:"
    );

    if (roomName == null || roomName.trim().isEmpty()) {
        return;
    }

    Message message = new Message(
            MessageType.CREATE_ROOM,
            username,
            roomName.trim()
    );

    try {
        connection.send(message);
    } catch (IOException e) {
        append("Không thể tạo phòng: " + e.getMessage());
    }
}
    private void sendChat() {

    String text = txtMessage.getText().trim();

    if (text.isEmpty()) {
        return;
    }

    Message message = new Message(
            MessageType.CHAT,
            username,
            text
    );

    try {
        connection.send(message);

        // Xóa ô nhập sau khi gửi
        txtMessage.setText("");
        txtMessage.requestFocus();

    } catch (IOException e) {
        append("Không thể gửi tin nhắn: " + e.getMessage());
    }
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
            java.util.logging.Logger.getLogger(chatFramForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(chatFramForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(chatFramForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(chatFramForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new chatFramForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCreateRoom;
    private javax.swing.JButton btnPrivateMessage;
    private javax.swing.JButton btnSend;
    private javax.swing.JButton btn_call;
    private javax.swing.JButton btn_callvideo;
    private javax.swing.JButton btn_sendfile;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private java.awt.Label label1;
    private java.awt.Label label2;
    private java.awt.Label label3;
    private java.awt.Label label4;
    private javax.swing.JLabel lb_namelogin;
    private javax.swing.JList<String> lsUsers;
    private javax.swing.JList<String> lstRoom;
    private javax.swing.JTextArea txtChatArea;
    private javax.swing.JTextField txtMessage;
    // End of variables declaration//GEN-END:variables
}
