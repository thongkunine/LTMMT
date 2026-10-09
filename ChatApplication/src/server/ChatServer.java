
package server;

import common.Protocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

public class ChatServer {

    private final UserManager userManager = new UserManager();
    private final RoomManager roomManager = new RoomManager();

    // Giới hạn tối đa 20 luồng xử lý Client
    private static final int MAX_THREADS = 20;

    // Tối đa 50 kết nối chờ được xử lý
    private static final int MAX_QUEUE = 50;

    private final ExecutorService clientPool =
            new ThreadPoolExecutor(
                    MAX_THREADS,
                    MAX_THREADS,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(MAX_QUEUE),
                    new ThreadPoolExecutor.AbortPolicy()
            );

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public void start() {

        // Khởi động VoiceServer
        VoiceServer voiceServer =
                new VoiceServer(Protocol.VOICE_PORT);

        Thread voiceThread = new Thread(
                voiceServer::start,
                "VoiceServer-Thread"
        );

        voiceThread.setDaemon(true);
        voiceThread.start();

        // Khởi động VideoServer
        VideoServer videoServer = new VideoServer();

        Thread videoThread = new Thread(
                videoServer::start,
                "VideoServer-Thread"
        );

        videoThread.setDaemon(true);
        videoThread.start();

        // Khởi động TCP ChatServer
        try (ServerSocket serverSocket =
                new ServerSocket(Protocol.PORT)) {

            System.out.println(
                    "Chat server started on port "
                    + Protocol.PORT
            );

            System.out.println(
                    "Thread Pool: " + MAX_THREADS
                    + " threads, queue: " + MAX_QUEUE
            );

            while (!Thread.currentThread().isInterrupted()) {

                Socket socket = serverSocket.accept();

                System.out.println(
                        "Client connected: "
                        + socket.getRemoteSocketAddress()
                );

                try {

                    ClientHandler handler =
                            new ClientHandler(socket, this);

                    // Đưa ClientHandler vào Thread Pool
                    clientPool.execute(handler);

                    ThreadPoolExecutor executor =
                            (ThreadPoolExecutor) clientPool;

                    System.out.println(
                            "Active Threads: "
                            + executor.getActiveCount()
                            + "/" + MAX_THREADS
                            + " | Waiting: "
                            + executor.getQueue().size()
                    );

                } catch (RejectedExecutionException e) {

                    System.out.println(
                            "Server overloaded! Rejecting: "
                            + socket.getRemoteSocketAddress()
                    );

                    try {
                        socket.close();
                    } catch (IOException ex) {
                        System.out.println(
                                "Socket close error: "
                                + ex.getMessage()
                        );
                    }
                }
            }

        } catch (IOException e) {

            System.out.println(
                    "Server error: " + e.getMessage()
            );

        } finally {

            clientPool.shutdown();

            System.out.println(
                    "Client Thread Pool shutdown."
            );
        }
    }
}
