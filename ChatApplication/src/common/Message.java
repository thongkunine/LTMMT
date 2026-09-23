package common;

import java.io.Serializable;
import java.time.LocalDateTime;
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String content;
    private String room;
    private LocalDateTime sentAt;
    public Message() {
    }

    public Message(MessageType type, String sender, String content) {
        this(type, sender, content, null);
    }

    public Message(MessageType type, String sender, String content, String room) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.room = room;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
    public LocalDateTime getSentAt() {
    return sentAt;
}

public void setSentAt(LocalDateTime sentAt) {
    this.sentAt = sentAt;
}

    @Override
    public String toString() {
        String prefix = room == null || room.isBlank() ? "" : "(" + room + ") ";
        return prefix + "[" + type + "] " + sender + ": " + content;
    }
}
