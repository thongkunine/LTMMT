package client;

import common.Message;
import common.MessageType;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(16);
    private final JButton loginButton = new JButton("Connect");

    public LoginFrame() {
        super("Chat Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(360, 160);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(usernameField, gbc);

        add(form, BorderLayout.CENTER);
        add(loginButton, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> connect());
        usernameField.addActionListener(e -> connect());
    }

    private void connect() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username.");
            return;
        }

        loginButton.setEnabled(false);
        new Thread(() -> {
            ClientConnection connection = new ClientConnection();
            try {
                connection.connect();
                connection.send(new Message(MessageType.LOGIN, username, username));
                Message response = connection.read();
                SwingUtilities.invokeLater(() -> {
                    if (response != null && response.getType() == MessageType.LOGIN_OK) {
                        ChatFrame chatFrame = new ChatFrame(username, connection);
                        chatFrame.setVisible(true);
                        dispose();
                        chatFrame.handleServerMessage(response);
                    } else {
                        connection.close();
                        String reason = response == null ? "Login failed" : response.getContent();
                        JOptionPane.showMessageDialog(this, reason);
                        loginButton.setEnabled(true);
                    }
                });
            } catch (IOException | ClassNotFoundException e) {
                connection.close();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Cannot connect: " + e.getMessage());
                    loginButton.setEnabled(true);
                });
            }
        }).start();
    }
}
