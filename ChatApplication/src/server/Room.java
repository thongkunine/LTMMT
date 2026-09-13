package server;

import common.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {

    private final String name;
    private final List<ClientHandler> members = new CopyOnWriteArrayList<>();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void join(ClientHandler client) {
        if (!members.contains(client)) {
            members.add(client);
        }
    }

    public void leave(ClientHandler client) {
        members.remove(client);
    }

    public void broadcast(Message message) {
        for (ClientHandler member : members) {
            member.send(message);
        }
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public List<String> getMemberNames() {
        List<String> names = new ArrayList<>();
        for (ClientHandler member : members) {
            names.add(member.getUsername());
        }
        return names;
    }
}
