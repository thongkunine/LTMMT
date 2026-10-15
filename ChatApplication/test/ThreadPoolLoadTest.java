
package test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ThreadPoolLoadTest {

    private static final String SERVER_IP = "100.74.164.86";
    private static final int SERVER_PORT = 5000;

    // Tổng số Client mô phỏng
    private static final int TOTAL_CLIENTS = 100;

    // Thời gian giữ kết nối: 15 giây
    private static final int HOLD_TIME = 15000;

    public static void main(String[] args) {

        List<Socket> sockets = new ArrayList<>();

        int connected = 0;
        int failed = 0;

        System.out.println("================================");
        System.out.println("THREAD POOL LOAD TEST");
        System.out.println("Server: " + SERVER_IP + ":" + SERVER_PORT);
        System.out.println("Total Clients: " + TOTAL_CLIENTS);
        System.out.println("================================");

        try {

            // Tạo 100 kết nối TCP
            for (int i = 1; i <= TOTAL_CLIENTS; i++) {

                try {

                    Socket socket = new Socket();

                    socket.connect(
                            new InetSocketAddress(
                                    SERVER_IP,
                                    SERVER_PORT
                            ),
                            2000
                    );

                    sockets.add(socket);
                    connected++;

                    System.out.println(
                            "Client " + i + " connected!"
                    );

                } catch (IOException e) {

                    failed++;

                    System.out.println(
                            "Client " + i
                            + " failed: " + e.getMessage()
                    );
                }
            }

            System.out.println("\n========== RESULT ==========");
            System.out.println("Connected TCP: " + connected);
            System.out.println("Failed TCP: " + failed);
            System.out.println("============================");

            // Giữ các Socket mở để quan sát Thread Pool
            System.out.println(
                    "Keeping connections open for 15 seconds..."
            );

            Thread.sleep(HOLD_TIME);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        } finally {

            for (Socket socket : sockets) {

                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println(
                            "Close error: " + e.getMessage()
                    );
                }
            }

            System.out.println(
                    "\nLoad test completed. All sockets closed."
            );
        }
    }
}
