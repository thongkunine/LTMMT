package client;

import common.Message;
import common.MessageType;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.time.format.DateTimeFormatter;
public class ChatFrame extends JFrame {

    private final String username;
    private final ClientConnection connection;
    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final DefaultListModel<String> roomModel = new DefaultListModel<>();
    private final DefaultListModel<String> userModel = new DefaultListModel<>();
    private final JList<String> roomList = new JList<>(roomModel);
    private final JList<String> userList = new JList<>(userModel);
    private String currentRoom = "";

    public ChatFrame(String username, ClientConnection connection) {
        super("Chat - " + username);
        this.username = username;
        this.connection = connection;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 520);
        setLocationRelativeTo(null);

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);

        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setPreferredSize(new Dimension(160, 0));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(160, 0));

        JSplitPane sideSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, roomScroll, userScroll);
        sideSplit.setResizeWeight(0.5);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        JButton sendButton = new JButton("Send");
        JButton createRoomButton = new JButton("New room");
        bottom.add(createRoomButton, BorderLayout.WEST);
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(sideSplit, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendChat());
        inputField.addActionListener(e -> sendChat());
        createRoomButton.addActionListener(e -> createRoom());
        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                joinSelectedRoom();
            }
        });

        connection.setListener(new ClientConnection.MessageListener() {
            @Override
            public void onMessage(Message message) {
                SwingUtilities.invokeLater(() -> handleServerMessage(message));
            }

            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(() -> {
                    append("Disconnected from server.");
                    inputField.setEnabled(false);
                });
            }
        });
        connection.startListening();
    }

public void handleServerMessage(Message message) {
    switch (message.getType()) {
        case LOGIN_OK -> {
            currentRoom = message.getRoom() == null ? "" : message.getRoom();

            setTitle(
                    "Chat - " + username + " @ " + currentRoom
            );

            append(message.getContent());
        }

        case CHAT ->
            append(
                    formatTime(message)
                    +message.getSender()
                    + ": "
                    + message.getContent()
            );

        case HISTORY ->
            append(
                    formatTime(message)
                    +"[Lịch sử] "
                    + message.getSender()
                    + ": "
                    + message.getContent()
            );

        case SYSTEM, JOIN, LEAVE ->
            append("* " + message.getContent());

        case ROOM_LIST ->
            updateList(roomModel, message.getContent());

        case USER_LIST -> {
            if (currentRoom.isEmpty()
                    || currentRoom.equals(message.getRoom())) {

                updateList(userModel, message.getContent());
            }
        }

        default -> {
        }
    }
}
    private void sendChat() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            connection.send(new Message(MessageType.CHAT, username, text, currentRoom));
            inputField.setText("");
        } catch (IOException e) {
            append("Cannot send message: " + e.getMessage());
        }
    }

    private void createRoom() {
        String name = JOptionPane.showInputDialog(this, "Room name:");
        if (name == null || name.isBlank()) {
            return;
        }
        try {

    chatArea.setText("");

    connection.send(
            new Message(
                    MessageType.CREATE_ROOM,
                    username,
                    name.trim()
            )
    );

    currentRoom = name.trim();

    setTitle(
            "Chat - "
            + username
            + " @ "
            + currentRoom
    );

} catch (IOException e) {

    append(
            "Cannot create room: "
            + e.getMessage()
    );
}
    }

   private void joinSelectedRoom() {

    // Lấy phòng đang được chọn
    String selected = roomList.getSelectedValue();

    // Không chọn phòng hoặc đang ở chính phòng đó
    if (selected == null || selected.equals(currentRoom)) {
        return;
    }

    try {

        // Xóa tin nhắn của phòng cũ
        chatArea.setText("");

        // Gửi yêu cầu JOIN_ROOM lên Server
        connection.send(
                new Message(
                        MessageType.JOIN_ROOM,
                        username,
                        selected
                )
        );

        // Cập nhật phòng hiện tại
        currentRoom = selected;

        // Cập nhật tiêu đề
        setTitle(
                "Chat - "
                + username
                + " @ "
                + currentRoom
        );

    } catch (IOException e) {

        append(
                "Cannot join room: "
                + e.getMessage()
        );
    }
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
private String formatTime(Message message) {
    if (message.getSentAt() == null) {
        return "";
    }

    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("HH:mm");

    return "[" + message.getSentAt().format(formatter) + "] ";
}
    private void append(String line) {
        chatArea.append(line + System.lineSeparator());
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}
