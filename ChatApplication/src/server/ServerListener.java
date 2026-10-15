package server;

/**
 * Interface lắng nghe các sự kiện từ ChatServer để cập nhật giao diện Server GUI theo thời gian thực.
 */
public interface ServerListener {

    /**
     * Nhận log mới từ server
     */
    void onLog(String timestamp, String level, String message);

    /**
     * Trạng thái server thay đổi (Đang chạy hoặc Đã dừng)
     */
    void onStatusChanged(boolean running, String message);

    /**
     * Có client mới kết nối socket TCP
     */
    void onClientConnected(String address);

    /**
     * Client hoàn tất xác thực đăng nhập
     */
    void onClientLoggedIn(ClientHandler client);

    /**
     * Client ngắt kết nối
     */
    void onClientDisconnected(String username, String address);

    /**
     * Danh sách phòng hoặc thành viên trong phòng có thay đổi
     */
    void onRoomUpdated();

    /**
     * Cập nhật số liệu thống kê luồng, queue và người dùng
     */
    void onStatsUpdated(int onlineUsers, int roomCount, int activeThreads, int queueSize);
}
