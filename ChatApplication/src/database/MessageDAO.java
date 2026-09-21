package database;

import common.Message;
import common.MessageType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    // Lưu tin nhắn vào SQL Server
    public void saveMessage(Message message) {

        String sql = """
                INSERT INTO Messages
                (username, room_name, content)
                VALUES (?, ?, ?)
                """;

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, message.getSender());
            ps.setString(2, message.getRoom());
            ps.setString(3, message.getContent());

            ps.executeUpdate();

            System.out.println(
                    "Da luu tin nhan: "
                    + message.getSender()
                    + " -> "
                    + message.getRoom()
            );

        } catch (SQLException e) {

            System.out.println(
                    "Loi luu tin nhan: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }


    // Lấy lịch sử của một phòng
    public List<Message> getMessagesByRoom(String roomName) {

        List<Message> messages = new ArrayList<>();

        String sql = """
                SELECT username, room_name, content
                FROM Messages
                WHERE room_name = ?
                ORDER BY sent_at ASC
                """;

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, roomName);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                String username = rs.getString("username");
                String room = rs.getString("room_name");
                String content = rs.getString("content");

                Message message =
                        new Message(
                                MessageType.CHAT,
                                username,
                                content,
                                room
                        );

                messages.add(message);
            }

        } catch (SQLException e) {

            System.out.println(
                    "Loi doc lich su: " + e.getMessage()
            );

            e.printStackTrace();
        }

        return messages;
    }
}