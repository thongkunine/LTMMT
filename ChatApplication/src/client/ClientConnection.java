package client;

import common.Message;
import common.Protocol;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientConnection {

    public interface MessageListener {
        void onMessage(Message message);

        void onDisconnected();
    }

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private MessageListener listener;

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public void connect() throws IOException {
        socket = new Socket(Protocol.HOST, Protocol.PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void send(Message message) throws IOException {
        out.writeObject(message);
        out.flush();
    }

    public Message read() throws IOException, ClassNotFoundException {
        return (Message) in.readObject();
    }

    public void startListening() {
        Thread listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void close() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void listen() {
        try {
            Object incoming;
            while ((incoming = in.readObject()) != null) {
                if (incoming instanceof Message message && listener != null) {
                    listener.onMessage(message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (listener != null) {
                listener.onDisconnected();
            }
        }
    }
}
