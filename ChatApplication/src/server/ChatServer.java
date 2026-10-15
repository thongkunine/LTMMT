package server;

import common.Message;
import common.MessageType;
import common.Protocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

/**
 * ChatServer: Quản lý toàn bộ hệ thống Socket TCP (Chat, File) và UDP (Voice, Video).
 * Hỗ trợ giao diện Server GUI, theo dõi sự kiện thời gian thực và điều khiển máy chủ.
 */
public class ChatServer {

    private final UserManager userManager = new UserManager();
    private final RoomManager roomManager = new RoomManager();

    // Giới hạn tối đa 20 luồng xử lý Client
    public static final int MAX_THREADS = 20;

    // Tối đa 50 kết nối chờ được xử lý
    public static final int MAX_QUEUE = 50;

    private ExecutorService clientPool;
    private ServerSocket serverSocket;
    private VoiceServer voiceServer;
    private VideoServer videoServer;

    private Thread acceptThread;
    private Thread voiceThread;
    private Thread videoThread;

    private volatile boolean running = false;
    private int tcpPort = Protocol.PORT;
    private int voicePort = Protocol.VOICE_PORT;
    private int videoPort = Protocol.VIDEO_PORT;

    private final List<ServerListener> listeners = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatServer() {
    }

    public static void main(String[] args) {
        boolean cli = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "--nogui".equalsIgnoreCase(arg) || "--console".equalsIgnoreCase(arg)) {
                cli = true;
                break;
            }
        }
        if (cli) {
            ChatServer server = new ChatServer();
            server.startServer(Protocol.PORT, Protocol.VOICE_PORT, Protocol.VIDEO_PORT);
        } else {
            // Mở giao diện Server GUI
            javax.swing.SwingUtilities.invokeLater(() -> {
                ServerGUI.main(args);
            });
        }
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public boolean isRunning() {
        return running;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public int getVoicePort() {
        return voicePort;
    }

    public int getVideoPort() {
        return videoPort;
    }

    public int getActiveThreadCount() {
        if (clientPool instanceof ThreadPoolExecutor executor) {
            return executor.getActiveCount();
        }
        return 0;
    }

    public int getQueueSize() {
        if (clientPool instanceof ThreadPoolExecutor executor) {
            return executor.getQueue().size();
        }
        return 0;
    }

    public void addListener(ServerListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ServerListener listener) {
        listeners.remove(listener);
    }

    public void log(String level, String message) {
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        System.out.println("[" + time + "] [" + level + "] " + message);
        for (ServerListener listener : listeners) {
            try {
                listener.onLog(time, level, message);
            } catch (Exception ignored) {
            }
        }
    }

    public void notifyStatusChanged(boolean runningState, String msg) {
        for (ServerListener listener : listeners) {
            try {
                listener.onStatusChanged(runningState, msg);
            } catch (Exception ignored) {
            }
        }
    }

    public void notifyClientConnected(String address) {
        for (ServerListener listener : listeners) {
            try {
                listener.onClientConnected(address);
            } catch (Exception ignored) {
            }
        }
        updateStats();
    }

    public void notifyClientLoggedIn(ClientHandler client) {
        for (ServerListener listener : listeners) {
            try {
                listener.onClientLoggedIn(client);
            } catch (Exception ignored) {
            }
        }
        updateStats();
    }

    public void notifyClientDisconnected(String username, String address) {
        for (ServerListener listener : listeners) {
            try {
                listener.onClientDisconnected(username, address);
            } catch (Exception ignored) {
            }
        }
        updateStats();
    }

    public void notifyRoomUpdated() {
        for (ServerListener listener : listeners) {
            try {
                listener.onRoomUpdated();
            } catch (Exception ignored) {
            }
        }
        updateStats();
    }

    public void updateStats() {
        int online = userManager.getOnlineCount();
        int rooms = roomManager.getRoomNames().size();
        int activeThreads = getActiveThreadCount();
        int queueSize = getQueueSize();
        for (ServerListener listener : listeners) {
            try {
                listener.onStatsUpdated(online, rooms, activeThreads, queueSize);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Tương thích ngược với phương thức start cũ
     */
    public void start() {
        startServer(Protocol.PORT, Protocol.VOICE_PORT, Protocol.VIDEO_PORT);
    }

    /**
     * Khởi động máy chủ với các cổng cấu hình
     */
    public synchronized boolean startServer(int tcpPort, int voicePort, int videoPort) {
        if (running) {
            log("WARN", "Máy chủ đã đang chạy!");
            return false;
        }

        this.tcpPort = tcpPort;
        this.voicePort = voicePort;
        this.videoPort = videoPort;

        clientPool = new ThreadPoolExecutor(
                MAX_THREADS,
                MAX_THREADS,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(MAX_QUEUE),
                new ThreadPoolExecutor.AbortPolicy()
        );

        // 1. Khởi động VoiceServer (UDP)
        try {
            voiceServer = new VoiceServer(voicePort);
            voiceThread = new Thread(voiceServer::start, "VoiceServer-Thread");
            voiceThread.setDaemon(true);
            voiceThread.start();
            log("INFO", "Voice Server (UDP) đã khởi động trên cổng " + voicePort);
        } catch (Exception e) {
            log("ERROR", "Lỗi khởi động Voice Server: " + e.getMessage());
        }

        // 2. Khởi động VideoServer (UDP)
        try {
            videoServer = new VideoServer(videoPort);
            videoThread = new Thread(videoServer::start, "VideoServer-Thread");
            videoThread.setDaemon(true);
            videoThread.start();
            log("INFO", "Video Server (UDP) đã khởi động trên cổng " + videoPort);
        } catch (Exception e) {
            log("ERROR", "Lỗi khởi động Video Server: " + e.getMessage());
        }

        // 3. Khởi động TCP Chat Server
        try {
            serverSocket = new ServerSocket(tcpPort);
            running = true;
            log("INFO", "Chat Server (TCP) đã khởi động thành công trên cổng " + tcpPort);
            log("INFO", "Thread Pool: " + MAX_THREADS + " luồng, tối đa " + MAX_QUEUE + " kết nối chờ");

            notifyStatusChanged(true, "Server đang chạy trên cổng TCP " + tcpPort);

            acceptThread = new Thread(this::listenLoop, "ChatServer-AcceptThread");
            acceptThread.setDaemon(true);
            acceptThread.start();
            updateStats();
            return true;

        } catch (IOException e) {
            log("ERROR", "Không thể lắng nghe trên cổng TCP " + tcpPort + ": " + e.getMessage());
            stopServer();
            return false;
        }
    }

    private void listenLoop() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                String remoteAddr = socket.getRemoteSocketAddress().toString().replace("/", "");
                log("INFO", "Client kết nối từ địa chỉ: " + remoteAddr);
                notifyClientConnected(remoteAddr);

                try {
                    ClientHandler handler = new ClientHandler(socket, this);
                    clientPool.execute(handler);

                    if (clientPool instanceof ThreadPoolExecutor executor) {
                        log("DEBUG", "Luồng hoạt động: " + executor.getActiveCount() + "/" + MAX_THREADS
                                + " | Đang chờ: " + executor.getQueue().size());
                    }
                    updateStats();

                } catch (RejectedExecutionException e) {
                    log("WARN", "Server quá tải (hàng đợi vượt " + MAX_QUEUE + ")! Từ chối client: " + remoteAddr);
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            } catch (IOException e) {
                if (running) {
                    log("ERROR", "Lỗi chấp nhận kết nối: " + e.getMessage());
                }
                break;
            }
        }
    }

    /**
     * Dừng máy chủ an toàn
     */
    public synchronized void stopServer() {
        if (!running && (serverSocket == null || serverSocket.isClosed())) {
            return;
        }
        running = false;
        log("INFO", "Đang tiến hành dừng máy chủ...");

        // Đóng ServerSocket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }

        // Dừng VoiceServer & VideoServer
        if (voiceServer != null) {
            try {
                voiceServer.stop();
            } catch (Exception ignored) {
            }
        }
        if (videoServer != null) {
            try {
                videoServer.stop();
            } catch (Exception ignored) {
            }
        }

        // Ngắt kết nối tất cả các Client hiện có
        for (ClientHandler client : userManager.getClients()) {
            try {
                client.kick("Máy chủ đã dừng hoạt động.");
            } catch (Exception ignored) {
            }
        }

        // Dừng Thread Pool
        if (clientPool != null && !clientPool.isShutdown()) {
            clientPool.shutdownNow();
        }

        log("INFO", "Máy chủ đã dừng hoàn toàn.");
        notifyStatusChanged(false, "Server đã dừng.");
        updateStats();
    }

    /**
     * Khởi động lại máy chủ
     */
    public synchronized void restartServer(int tcpPort, int voicePort, int videoPort) {
        log("INFO", "Đang khởi động lại máy chủ...");
        stopServer();
        try {
            Thread.sleep(600);
        } catch (InterruptedException ignored) {
        }
        startServer(tcpPort, voicePort, videoPort);
    }

    /**
     * Phát thông báo tới toàn bộ người dùng đang online trên server
     */
    public void broadcast(Message message) {
        for (ClientHandler client : userManager.getClients()) {
            try {
                client.send(message);
            } catch (Exception ignored) {
            }
        }
        log("SYSTEM", "[Broadcast Toàn Hệ Thống]: " + message.getContent());
    }

    /**
     * Phát thông báo tới một phòng chat cụ thể
     */
    public void broadcastToRoom(String roomName, Message message) {
        Room room = roomManager.get(roomName);
        if (room != null) {
            room.broadcast(message);
            log("SYSTEM", "[Broadcast Phòng " + roomName + "]: " + message.getContent());
        }
    }

    /**
     * Quản trị viên kick một người dùng khỏi server
     */
    public void kickUser(String username, String reason) {
        ClientHandler client = userManager.getClient(username);
        if (client != null) {
            log("WARN", "Quản trị viên ngắt kết nối người dùng: " + username + " (Lý do: " + reason + ")");
            client.kick(reason);
        }
    }

    /**
     * Ngắt kết nối toàn bộ người dùng
     */
    public void disconnectAll(String reason) {
        for (ClientHandler client : userManager.getClients()) {
            try {
                client.kick(reason);
            } catch (Exception ignored) {
            }
        }
        log("WARN", "Đã ngắt kết nối toàn bộ người dùng. Lý do: " + reason);
    }
}
