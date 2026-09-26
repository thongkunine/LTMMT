package server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {

    private final ConcurrentHashMap<String, ClientHandler> users = new ConcurrentHashMap<>();

    public boolean login(String username, ClientHandler handler) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return users.putIfAbsent(username.trim(), handler) == null;
    }

    public void logout(String username) {
        if (username != null) {
            users.remove(username);
        }
    }
    public List<String> getUsernames() {
        return new ArrayList<>(users.keySet());
    }

    public Collection<ClientHandler> getClients() {
        return users.values();
    }
    public ClientHandler getClient(String username){
         if(username == null|| username.isBlank()){
             return null;
         }
         return users.get(username.trim());
    }
}

 