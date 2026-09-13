package server;

import common.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public RoomManager() {
        rooms.put(Protocol.DEFAULT_ROOM, new Room(Protocol.DEFAULT_ROOM));
    }

    public Room getDefaultRoom() {
        return rooms.get(Protocol.DEFAULT_ROOM);
    }

    public Room get(String name) {
        return rooms.get(name);
    }

    public Room getOrCreate(String name) {
        String roomName = name == null ? "" : name.trim();
        if (roomName.isEmpty()) {
            return getDefaultRoom();
        }
        return rooms.computeIfAbsent(roomName, Room::new);
    }

    public void removeIfEmpty(String name) {
        if (name == null || Protocol.DEFAULT_ROOM.equals(name)) {
            return;
        }
        rooms.computeIfPresent(name, (key, room) -> room.isEmpty() ? null : room);
    }

    public List<String> getRoomNames() {
        return new ArrayList<>(rooms.keySet());
    }
}
