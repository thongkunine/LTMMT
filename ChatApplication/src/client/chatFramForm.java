package client;

import common.Message;
import common.MessageType;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * Giao diện Chat Client chính (NetBeans GUI Form - Light Theme)
 */
public class chatFramForm extends javax.swing.JFrame {

    private String username;
    private ClientConnection connection;
    private String currentRoom = "";
    private VoiceCallFrame voiceCallFrame = null;
    private String currentCallUser = null;
    private VideoCallFrame videoCallFrame = null;
    private String currentVideoCallUser = null;

    private final DefaultListModel<String> roomModel = new DefaultListModel<>();
    private final DefaultListModel<String> userModel = new DefaultListModel<>();

    // Lưu trữ danh sách các cửa sổ chat riêng theo đối phương
    private final Map<String, PrivateChatFrame> privateChatFrames = new ConcurrentHashMap<>();

    // Lưu trữ bộ nhớ đệm lịch sử chat theo từng phòng
    private final Map<String, StringBuilder> roomChatBuffers = new ConcurrentHashMap<>();

    // Lưu trữ bộ nhớ đệm lịch sử nhắn tin riêng theo đối phương
    private final Map<String, StringBuilder> privateChatBuffers = new ConcurrentHashMap<>();

    public chatFramForm() {
        initComponents();
        setupCustomUI();
    }

    public chatFramForm(String username, ClientConnection connection) {
        initComponents();
        this.username = username;
        this.connection = connection;

        lb_namelogin.setText("● " + username);
        lstRoom.setModel(roomModel);
        lsUsers.setModel(userModel);

        setTitle("Chat App - " + username);
        setLocationRelativeTo(null);
        setupCustomUI();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                if (voiceCallFrame != null) {
                    voiceCallFrame.dispose();
                }
                if (videoCallFrame != null) {
                    videoCallFrame.dispose();
                }
                for (PrivateChatFrame pFrame : privateChatFrames.values()) {
                    pFrame.dispose();
                }
                connection.close();
                dispose();
                System.exit(0);
            }
        });

        connection.setListener(new ClientConnection.MessageListener() {
            @Override
            public void onMessage(Message message) {
                if (message.getType() == MessageType.VOICE_DATA) {
                    if (voiceCallFrame != null) {
                        voiceCallFrame.receiveAudio(message.getVoiceData());
                    }
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    handleServerMessage(message);
                });
            }

            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(() -> {
                    append("* Đã mất kết nối tới máy chủ.");
                    txtMessage.setEnabled(false);
                    btnSend.setEnabled(false);
                    btnCreateRoom.setEnabled(false);
                    btnPrivateMessage.setEnabled(false);
                    btn_sendfile.setEnabled(false);
                    btn_voicecall.setEnabled(false);
                    btn_callvideo.setEnabled(false);
                    lstRoom.setEnabled(false);
                    lsUsers.setEnabled(false);
                });
            }
        });

        connection.startListening();
    }

    private void setupCustomUI() {
        // Gắn icon vector 2D sắc nét cho tiêu đề và các nút bấm
        lblAppTitle.setText("HỆ THỐNG CHAT TRỰC TUYẾN");
        lblAppTitle.setIcon(ClientIcons.getChatAppIcon(22, java.awt.Color.WHITE));

        lblRoomTitle.setText("Danh sách phòng:");
        lblRoomTitle.setIcon(ClientIcons.getRoomListIcon(18, new java.awt.Color(37, 99, 235)));

        lblOnlineTitle.setText("Người dùng Online:");
        lblOnlineTitle.setIcon(ClientIcons.getOnlineUserIcon(14, new java.awt.Color(34, 197, 94)));

        btnPrivateMessage.setText("Nhắn tin riêng");
        btnPrivateMessage.setIcon(ClientIcons.getLightningIcon(18, new java.awt.Color(245, 158, 11)));
        btnPrivateMessage.setVisible(false);

        btn_voicecall.setText("Gọi thoại");
        btn_voicecall.setIcon(ClientIcons.getPhoneIcon(16, new java.awt.Color(22, 163, 74)));

        btn_callvideo.setText("Gọi video");
        btn_callvideo.setIcon(ClientIcons.getVideoIcon(16, new java.awt.Color(220, 38, 38)));

        btn_sendfile.setText("Gửi File");
        btn_sendfile.setIcon(ClientIcons.getFileIcon(16, new java.awt.Color(37, 99, 235)));

        btnCreateRoom.setText("Tạo phòng mới");
        btnCreateRoom.setIcon(ClientIcons.getAddIcon(14, new java.awt.Color(37, 99, 235)));

        btnLeaveRoom.setText("Rời phòng");
        btnLeaveRoom.setIcon(ClientIcons.getLeaveIcon(14, new java.awt.Color(220, 38, 38)));

        // Nút Gửi màu xanh dương đậm nổi bật, không bị màu trắng che phủ
        btnSend.setBackground(new java.awt.Color(37, 99, 235));
        btnSend.setForeground(java.awt.Color.WHITE);
        btnSend.setOpaque(true);
        btnSend.setContentAreaFilled(true);
        btnSend.setBorderPainted(false);
        btnSend.setFocusPainted(false);

        // Double click listener cho danh sách người dùng online
        lsUsers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = lsUsers.getSelectedValue();
                    if (selected != null && !selected.isBlank()) {
                        openPrivateChat(selected.trim());
                    }
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = lsUsers.locationToIndex(e.getPoint());
                    if (index != -1) {
                        lsUsers.setSelectedIndex(index);
                        String selected = lsUsers.getSelectedValue();
                        if (selected != null && !selected.isBlank() && !selected.equals(username)) {
                            showUserContextMenu(e, selected.trim());
                        }
                    }
                }
            }
        });

        // Mouse double click listener cho danh sách phòng (lstRoom)
        lstRoom.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = lstRoom.locationToIndex(e.getPoint());
                    if (index != -1) {
                        lstRoom.setSelectedIndex(index);
                        String selectedRoom = lstRoom.getSelectedValue();
                        if (selectedRoom != null && !selectedRoom.isBlank()) {
                            switchRoom(selectedRoom.trim());
                        }
                    }
                }
            }
        });

        // Mouse double click listener cho txtChatArea để lưu file
        txtChatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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

    private void showUserContextMenu(MouseEvent e, String targetUser) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem itemChat = new JMenuItem("Nhắn tin riêng");
        itemChat.setIcon(ClientIcons.getLightningIcon(16, new java.awt.Color(245, 158, 11)));
        itemChat.addActionListener(evt -> openPrivateChat(targetUser));

        JMenuItem itemVoice = new JMenuItem("Gọi thoại");
        itemVoice.setIcon(ClientIcons.getPhoneIcon(16, new java.awt.Color(22, 163, 74)));
        itemVoice.addActionListener(evt -> startVoiceCallWith(targetUser));

        JMenuItem itemVideo = new JMenuItem("Gọi video");
        itemVideo.setIcon(ClientIcons.getVideoIcon(16, new java.awt.Color(220, 38, 38)));
        itemVideo.addActionListener(evt -> startVideoCallWith(targetUser));

        popup.add(itemChat);
        popup.addSeparator();
        popup.add(itemVoice);
        popup.add(itemVideo);

        popup.show(lsUsers, e.getX(), e.getY());
    }

    public void recordPrivateMessageHistory(String targetUser, String formattedText) {
        if (targetUser == null || formattedText == null) return;
        StringBuilder buffer = privateChatBuffers.computeIfAbsent(targetUser, k -> new StringBuilder());
        if (!buffer.toString().contains(formattedText)) {
            if (buffer.length() > 0 && !buffer.toString().endsWith("\n") && !buffer.toString().endsWith(System.lineSeparator())) {
                buffer.append(System.lineSeparator());
            }
            buffer.append(formattedText);
        }
    }

    public void openPrivateChat(String targetUser) {
        if (targetUser == null || targetUser.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng để nhắn tin riêng!");
            return;
        }
        if (targetUser.equals(username)) {
            JOptionPane.showMessageDialog(this, "Bạn không thể tự nhắn tin riêng cho chính mình!");
            return;
        }

        PrivateChatFrame frame = privateChatFrames.computeIfAbsent(targetUser, u -> {
            PrivateChatFrame p = new PrivateChatFrame(username, u, connection, this);
            StringBuilder buf = privateChatBuffers.get(u);
            if (buf != null) {
                p.setInitialHistory(buf.toString());
            }
            return p;
        });

        StringBuilder buf = privateChatBuffers.get(targetUser);
        if (buf != null) {
            frame.setInitialHistory(buf.toString());
        }

        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();

        // Tải lịch sử nhắn tin riêng lưu trong cơ sở dữ liệu
        String u1 = username;
        String u2 = targetUser;
        String roomKey = "PRIVATE:" + (u1.compareTo(u2) < 0 ? u1 + "_" + u2 : u2 + "_" + u1);
        sendSafe(new Message(MessageType.JOIN_ROOM, username, roomKey));
    }

    public void onPrivateChatClosed(String targetUser) {
        if (targetUser != null) {
            privateChatFrames.remove(targetUser);
        }
    }

    private void sendSafe(Message msg) {
        if (connection == null || msg == null) {
            return;
        }
        try {
            connection.send(msg);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi dữ liệu tới máy chủ: " + e.getMessage(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void startVoiceCallWith(String targetUser) {
        if (targetUser == null || targetUser.isBlank() || targetUser.equals(username)) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng hợp lệ để gọi thoại!");
            return;
        }
        currentCallUser = targetUser;
        voiceCallFrame = new VoiceCallFrame(username, targetUser, connection, false, () -> {
            voiceCallFrame = null;
            currentCallUser = null;
        });
        voiceCallFrame.setVisible(true);

        Message request = new Message(MessageType.CALL_REQUEST, username, "Bắt đầu cuộc gọi thoại");
        request.setReceiver(targetUser);
        sendSafe(request);
    }

    public void startVideoCallWith(String targetUser) {
        if (targetUser == null || targetUser.isBlank() || targetUser.equals(username)) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng hợp lệ để gọi video!");
            return;
        }
        currentVideoCallUser = targetUser;
        videoCallFrame = new VideoCallFrame(username, targetUser, connection, false, () -> {
            videoCallFrame = null;
            currentVideoCallUser = null;
        });
        videoCallFrame.setVisible(true);

        Message request = new Message(MessageType.VIDEO_CALL_REQUEST, username, "Bắt đầu cuộc gọi video");
        request.setReceiver(targetUser);
        sendSafe(request);
    }

    public void startGroupVoiceCall(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Bạn cần tham gia một phòng chat để thực hiện gọi thoại nhóm!");
            return;
        }
        voiceCallFrame = new VoiceCallFrame(username, null, roomName, connection, false, () -> {
            voiceCallFrame = null;
        });
        voiceCallFrame.setVisible(true);

        Message request = new Message(MessageType.CALL_REQUEST, username, "Bắt đầu cuộc gọi thoại nhóm");
        request.setRoom(roomName);
        sendSafe(request);
    }

    public void startGroupVideoCall(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Bạn cần tham gia một phòng chat để thực hiện gọi video nhóm!");
            return;
        }
        videoCallFrame = new VideoCallFrame(username, null, roomName, connection, false, () -> {
            videoCallFrame = null;
        });
        videoCallFrame.setVisible(true);

        Message request = new Message(MessageType.VIDEO_CALL_REQUEST, username, "Bắt đầu cuộc gọi video nhóm");
        request.setRoom(roomName);
        sendSafe(request);
    }

    public void handleServerMessage(Message message) {
        switch (message.getType()) {
            case LOGIN_OK -> {
                currentRoom = message.getRoom() == null ? "" : message.getRoom();
                setTitle("Chat App - " + username + " @ Phòng: " + currentRoom);
                StringBuilder buffer = roomChatBuffers.get(currentRoom);
                txtChatArea.setText(buffer != null ? buffer.toString() : "");
                append("* " + message.getContent());
            }

            case LEAVE -> {
                if (message.getRoom() != null && !message.getRoom().isBlank()) {
                    currentRoom = message.getRoom();
                    setTitle("Chat App - " + username + " @ Phòng: " + currentRoom);
                    StringBuilder buffer = roomChatBuffers.get(currentRoom);
                    txtChatArea.setText(buffer != null ? buffer.toString() : "");
                }
                JOptionPane.showMessageDialog(this, message.getContent(), "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }

            case CHAT -> {
                String rName = (message.getRoom() == null || message.getRoom().isBlank()) ? currentRoom : message.getRoom();
                String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                String formatted = "[" + timeStr + "] " + message.getSender() + ": " + message.getContent();

                StringBuilder buffer = roomChatBuffers.computeIfAbsent(rName, k -> new StringBuilder());
                if (buffer.length() > 0 && !buffer.toString().endsWith("\n") && !buffer.toString().endsWith(System.lineSeparator())) {
                    buffer.append(System.lineSeparator());
                }
                buffer.append(formatted);

                if (rName.equals(currentRoom)) {
                    append(formatted);
                }
            }

            case HISTORY -> {
                String rName = message.getRoom();
                if (rName != null && rName.startsWith("PRIVATE:")) {
                    String pair = rName.substring("PRIVATE:".length());
                    String[] parts = pair.split("_");
                    if (parts.length == 2) {
                        String other = parts[0].equals(username) ? parts[1] : parts[0];
                        String timeStr = message.getSentAt() != null ? message.getSentAt().format(DateTimeFormatter.ofPattern("HH:mm")) : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                        String senderName = username.equals(message.getSender()) ? "Bạn" : message.getSender();
                        String formatted = "[" + timeStr + "] " + senderName + ": " + message.getContent();

                        recordPrivateMessageHistory(other, formatted);

                        PrivateChatFrame pFrame = privateChatFrames.get(other);
                        if (pFrame != null) {
                            pFrame.appendHistory(formatted);
                        }
                    }
                    return;
                }

                String targetRoomName = (rName == null || rName.isBlank()) ? currentRoom : rName;
                String timeStr = message.getSentAt() != null ? message.getSentAt().format(DateTimeFormatter.ofPattern("HH:mm")) : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                String formatted = "[Lịch sử " + timeStr + "] " + message.getSender() + ": " + message.getContent();

                StringBuilder buffer = roomChatBuffers.computeIfAbsent(targetRoomName, k -> new StringBuilder());
                if (!buffer.toString().contains(formatted)) {
                    if (buffer.length() > 0 && !buffer.toString().endsWith("\n") && !buffer.toString().endsWith(System.lineSeparator())) {
                        buffer.append(System.lineSeparator());
                    }
                    buffer.append(formatted);
                    if (targetRoomName.equals(currentRoom)) {
                        append(formatted);
                    }
                }
            }

            case SYSTEM, JOIN -> {
                String rName = (message.getRoom() == null || message.getRoom().isBlank()) ? currentRoom : message.getRoom();
                String formatted = "* " + message.getContent();

                StringBuilder buffer = roomChatBuffers.computeIfAbsent(rName, k -> new StringBuilder());
                if (buffer.length() > 0 && !buffer.toString().endsWith("\n") && !buffer.toString().endsWith(System.lineSeparator())) {
                    buffer.append(System.lineSeparator());
                }
                buffer.append(formatted);

                if (rName.equals(currentRoom)) {
                    append(formatted);
                }
            }

            case ROOM_LIST -> {
                updateList(roomModel, message.getContent());
            }

            case USER_LIST -> {
                updateList(userModel, message.getContent());
            }

            case PRIVATE_MESSAGE -> {
                String sender = message.getSender();

                if (username != null && username.equals(sender)) {
                    return;
                }

                if (sender != null && !sender.isBlank()) {
                    String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                    String formatted = "[" + timeStr + "] " + sender + ": " + message.getContent();
                    recordPrivateMessageHistory(sender, formatted);

                    PrivateChatFrame pFrame = privateChatFrames.computeIfAbsent(sender, u -> {
                        PrivateChatFrame p = new PrivateChatFrame(username, u, connection, this);
                        StringBuilder buf = privateChatBuffers.get(u);
                        if (buf != null) {
                            p.setInitialHistory(buf.toString());
                        }
                        return p;
                    });
                    pFrame.receiveMessage(message);
                }
            }

            case FILE_MESSAGE -> {
                if (message.getReceiver() != null && !message.getReceiver().isBlank()) {
                    String sender = message.getSender();
                    String targetUser = (username != null && username.equals(sender)) ? message.getReceiver() : sender;
                    if (targetUser != null && !targetUser.isBlank()) {
                        PrivateChatFrame pFrame = privateChatFrames.computeIfAbsent(targetUser, u -> {
                            PrivateChatFrame p = new PrivateChatFrame(username, u, connection, this);
                            StringBuilder buf = privateChatBuffers.get(u);
                            if (buf != null) {
                                p.setInitialHistory(buf.toString());
                            }
                            return p;
                        });
                        pFrame.receiveMessage(message);
                    }
                } else {
                    handleReceivedFile(message);
                }
            }

            case CALL_REQUEST -> handleIncomingCall(message);

            case CALL_ACCEPT -> {
                currentCallUser = message.getSender();
                if (voiceCallFrame != null) {
                    voiceCallFrame.callAccepted();
                }
            }

            case CALL_REJECT -> {
                if (voiceCallFrame != null) {
                    voiceCallFrame.callRejected();
                    voiceCallFrame = null;
                }
                currentCallUser = null;
            }

            case CALL_END -> {
                if (voiceCallFrame != null) {
                    voiceCallFrame.callEnded();
                    voiceCallFrame = null;
                }
                currentCallUser = null;
            }

            case VIDEO_CALL_REQUEST -> handleIncomingVideoCall(message);

            case VIDEO_CALL_ACCEPT -> {
                if (videoCallFrame != null) {
                    videoCallFrame.videoCallAccepted();
                }
            }

            case VIDEO_CALL_REJECT -> {
                if (videoCallFrame != null) {
                    videoCallFrame.videoCallRejected();
                }
                currentVideoCallUser = null;
            }

            case VIDEO_CALL_END -> {
                if (videoCallFrame != null) {
                    videoCallFrame.videoCallEnded();
                }
                currentVideoCallUser = null;
            }

            default -> {
            }
        }
    }

    private void handleIncomingCall(Message message) {
        String caller = message.getSender();
        String room = message.getRoom();
        if (room != null && !room.isBlank()) {
            if (voiceCallFrame == null) {
                voiceCallFrame = new VoiceCallFrame(username, caller, room, connection, true, () -> {
                    voiceCallFrame = null;
                });
                voiceCallFrame.setVisible(true);
            }
        } else {
            currentCallUser = caller;
            if (voiceCallFrame == null) {
                voiceCallFrame = new VoiceCallFrame(username, caller, connection, true, () -> {
                    voiceCallFrame = null;
                    currentCallUser = null;
                });
                voiceCallFrame.setVisible(true);
            }
        }
    }

    private java.util.List<Message> fileMessageList = new java.util.ArrayList<>();

    private void handleIncomingVideoCall(Message message) {
        String caller = message.getSender();
        String room = message.getRoom();
        if (room != null && !room.isBlank()) {
            if (videoCallFrame == null) {
                videoCallFrame = new VideoCallFrame(username, caller, room, connection, true, () -> {
                    videoCallFrame = null;
                });
                videoCallFrame.setVisible(true);
            }
        } else {
            currentVideoCallUser = caller;
            if (videoCallFrame == null) {
                videoCallFrame = new VideoCallFrame(username, caller, connection, true, () -> {
                    videoCallFrame = null;
                    currentVideoCallUser = null;
                });
                videoCallFrame.setVisible(true);
            }
        }
    }

    private void handleReceivedFile(Message message) {
        if (username.equals(message.getSender())) {
            fileMessageList.add(message);
            append("[File] Bạn đã gửi file: " + message.getFileName() + " (Nhấp đúp để lưu file)");
            return;
        }

        fileMessageList.add(message);
        append("[File] " + message.getSender() + " đã gửi file: " + message.getFileName() + " (Nhấp đúp để lưu file)");

        int choice = JOptionPane.showConfirmDialog(this,
                message.getSender() + " đã gửi file:\n" + message.getFileName() + "\n\nBạn có muốn lưu file ngay không?",
                "Nhận file", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            saveReceivedFile(message);
        }
    }

    private void saveReceivedFile(Message message) {
        if (message.getFileData() == null) {
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

    private void append(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String current = txtChatArea.getText();
        if (!current.isEmpty() && !current.endsWith("\n") && !current.endsWith(System.lineSeparator())) {
            txtChatArea.append(System.lineSeparator());
        }
        txtChatArea.append(text + System.lineSeparator());
        txtChatArea.setCaretPosition(txtChatArea.getDocument().getLength());
    }

    private void updateList(DefaultListModel<String> model, String csv) {
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

        pnlHeader = new javax.swing.JPanel();
        lblAppTitle = new javax.swing.JLabel();
        lb_namelogin = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtChatArea = new javax.swing.JTextArea();
        txtMessage = new javax.swing.JTextField();
        btnSend = new javax.swing.JButton();
        btn_sendfile = new javax.swing.JButton();
        btnPrivateMessage = new javax.swing.JButton();
        btn_voicecall = new javax.swing.JButton();
        btn_callvideo = new javax.swing.JButton();
        lblRoomTitle = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstRoom = new javax.swing.JList<>();
        btnCreateRoom = new javax.swing.JButton();
        btnLeaveRoom = new javax.swing.JButton();
        lblOnlineTitle = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lsUsers = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Chat Application");
        setMinimumSize(new java.awt.Dimension(960, 640));

        pnlHeader.setBackground(new java.awt.Color(30, 64, 171));

        lblAppTitle.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblAppTitle.setForeground(new java.awt.Color(255, 255, 255));
        lblAppTitle.setText("HỆ THỐNG CHAT TRỰC TUYẾN");

        lb_namelogin.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lb_namelogin.setForeground(new java.awt.Color(209, 250, 209));
        lb_namelogin.setText("Đăng nhập:");

        javax.swing.GroupLayout pnlHeaderLayout = new javax.swing.GroupLayout(pnlHeader);
        pnlHeader.setLayout(pnlHeaderLayout);
        pnlHeaderLayout.setHorizontalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblAppTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lb_namelogin)
                .addGap(20, 20, 20))
        );
        pnlHeaderLayout.setVerticalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblAppTitle)
                    .addComponent(lb_namelogin))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        txtChatArea.setEditable(false);
        txtChatArea.setColumns(20);
        txtChatArea.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtChatArea.setLineWrap(true);
        txtChatArea.setRows(5);
        txtChatArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(txtChatArea);

        txtMessage.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        txtMessage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
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

        btn_sendfile.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        btn_sendfile.setText("Gửi File");
        btn_sendfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_sendfileActionPerformed(evt);
            }
        });

        btnPrivateMessage.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        btnPrivateMessage.setText("Nhắn tin riêng");
        btnPrivateMessage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrivateMessageActionPerformed(evt);
            }
        });

        btn_voicecall.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        btn_voicecall.setText("Gọi thoại");
        btn_voicecall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_voicecallActionPerformed(evt);
            }
        });

        btn_callvideo.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        btn_callvideo.setText("Gọi video");
        btn_callvideo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_callvideoActionPerformed(evt);
            }
        });

        lblRoomTitle.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblRoomTitle.setText("Danh sách phòng:");

        lstRoom.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jScrollPane3.setViewportView(lstRoom);

        btnCreateRoom.setText("Tạo phòng mới");
        btnCreateRoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateRoomActionPerformed(evt);
            }
        });

        btnLeaveRoom.setText("Rời phòng");
        btnLeaveRoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLeaveRoomActionPerformed(evt);
            }
        });

        lblOnlineTitle.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        lblOnlineTitle.setText("Người dùng Online:");

        lsUsers.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jScrollPane2.setViewportView(lsUsers);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txtMessage, javax.swing.GroupLayout.DEFAULT_SIZE, 457, Short.MAX_VALUE)
                        .addGap(10, 10, 10)
                        .addComponent(btnSend)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btn_sendfile, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnPrivateMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(btn_voicecall, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12)
                        .addComponent(btn_callvideo, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblRoomTitle)
                    .addComponent(jScrollPane3)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnCreateRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnLeaveRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblOnlineTitle)
                    .addComponent(jScrollPane2))
                .addGap(15, 15, 15))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnlHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE)
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnSend, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_sendfile, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnPrivateMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_voicecall, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btn_callvideo, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblRoomTitle)
                        .addGap(6, 6, 6)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCreateRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnLeaveRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(15, 15, 15)
                        .addComponent(lblOnlineTitle)
                        .addGap(6, 6, 6)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
        String content = txtMessage.getText().trim();
        if (content.isEmpty() || connection == null) {
            return;
        }

        Message msg = new Message(MessageType.CHAT, username, content);
        msg.setRoom(currentRoom);
        sendSafe(msg);

        txtMessage.setText("");
        txtMessage.requestFocus();
    }//GEN-LAST:event_btnSendActionPerformed

    private void btn_sendfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_sendfileActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        try {
            byte[] fileData = Files.readAllBytes(selectedFile.toPath());
            Message message = new Message(MessageType.FILE_MESSAGE, username, "Gửi file nhóm");
            message.setRoom(currentRoom);
            message.setFileName(selectedFile.getName());
            message.setFileData(fileData);

            sendSafe(message);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi đọc file: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btn_sendfileActionPerformed

    private void btnPrivateMessageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrivateMessageActionPerformed
        String selectedUser = lsUsers.getSelectedValue();
        if (selectedUser == null || selectedUser.isBlank()) {
            selectedUser = JOptionPane.showInputDialog(this, "Nhập username người muốn nhắn tin riêng:", "Nhắn tin riêng", JOptionPane.QUESTION_MESSAGE);
        }
        if (selectedUser != null && !selectedUser.isBlank()) {
            openPrivateChat(selectedUser.trim());
        }
    }//GEN-LAST:event_btnPrivateMessageActionPerformed

    private void btn_voicecallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_voicecallActionPerformed
        if (currentRoom == null || currentRoom.isBlank()) {
            JOptionPane.showMessageDialog(this, "Bạn cần tham gia một phòng chat để gọi thoại nhóm!");
            return;
        }
        startGroupVoiceCall(currentRoom);
    }//GEN-LAST:event_btn_voicecallActionPerformed

    private void btn_callvideoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_callvideoActionPerformed
        if (currentRoom == null || currentRoom.isBlank()) {
            JOptionPane.showMessageDialog(this, "Bạn cần tham gia một phòng chat để gọi video nhóm!");
            return;
        }
        startGroupVideoCall(currentRoom);
    }//GEN-LAST:event_btn_callvideoActionPerformed

    private void btnCreateRoomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateRoomActionPerformed
        String roomName = JOptionPane.showInputDialog(this, "Nhập tên phòng mới:", "Tạo phòng", JOptionPane.QUESTION_MESSAGE);
        if (roomName != null && !roomName.isBlank()) {
            sendSafe(new Message(MessageType.CREATE_ROOM, username, roomName.trim()));
        }
    }//GEN-LAST:event_btnCreateRoomActionPerformed

    private void btnLeaveRoomActionPerformed(java.awt.event.ActionEvent evt) {
        if (currentRoom == null || currentRoom.isBlank()) {
            JOptionPane.showMessageDialog(this, "Bạn chưa ở trong phòng nào!");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn rời nhóm chat '" + currentRoom + "'?",
                "Rời nhóm chat",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            sendSafe(new Message(MessageType.LEAVE, username, currentRoom));
        }
    }

    public void switchRoom(String targetRoom) {
        if (targetRoom == null || targetRoom.isBlank() || targetRoom.equals(currentRoom)) {
            return;
        }
        currentRoom = targetRoom.trim();
        setTitle("Chat App - " + username + " @ Phòng: " + currentRoom);

        StringBuilder buffer = roomChatBuffers.get(currentRoom);
        txtChatArea.setText(buffer != null ? buffer.toString() : "");
        txtChatArea.setCaretPosition(txtChatArea.getDocument().getLength());

        sendSafe(new Message(MessageType.JOIN_ROOM, username, currentRoom));
    }

    private void lstRoomValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstRoomValueChanged
        if (!evt.getValueIsAdjusting()) {
            String selectedRoom = lstRoom.getSelectedValue();
            if (selectedRoom != null && !selectedRoom.isBlank() && !selectedRoom.equals(currentRoom)) {
                switchRoom(selectedRoom.trim());
            }
        }
    }//GEN-LAST:event_lstRoomValueChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCreateRoom;
    private javax.swing.JButton btnLeaveRoom;
    private javax.swing.JButton btnPrivateMessage;
    private javax.swing.JButton btnSend;
    private javax.swing.JButton btn_callvideo;
    private javax.swing.JButton btn_sendfile;
    private javax.swing.JButton btn_voicecall;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lb_namelogin;
    private javax.swing.JLabel lblAppTitle;
    private javax.swing.JLabel lblOnlineTitle;
    private javax.swing.JLabel lblRoomTitle;
    private javax.swing.JList<String> lsUsers;
    private javax.swing.JList<String> lstRoom;
    private javax.swing.JPanel pnlHeader;
    private javax.swing.JTextArea txtChatArea;
    private javax.swing.JTextField txtMessage;
    // End of variables declaration//GEN-END:variables
}
