package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {

    // =========================
    // ĐĂNG KÝ
    // =========================
    public boolean register(String fullName, String username, String password) {

        String sql = "INSERT INTO Users (full_name, username, password) "
                   + "VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Băm mật khẩu trước khi lưu vào Database
            String hashedPassword = BCrypt.hashpw(
                    password,
                    BCrypt.gensalt(12)
            );

            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, hashedPassword);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Lỗi đăng ký: " + e.getMessage());
            return false;
        }
    }


    // =========================
    // KIỂM TRA USERNAME ĐÃ TỒN TẠI
    // =========================
    public boolean usernameExists(String username) {

        String sql = "SELECT id FROM Users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.out.println("Lỗi kiểm tra username: " + e.getMessage());
            return false;
        }
    }


    // =========================
    // ĐĂNG NHẬP
    // =========================
    public boolean login(String username, String password) {

        // Chỉ tìm theo username
        String sql = "SELECT password FROM Users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {

                // Không tìm thấy username
                if (!rs.next()) {
                    return false;
                }

                // Lấy mật khẩu đã băm trong Database
                String hashedPassword =
                        rs.getString("password");

                // So sánh password người dùng nhập với hash
                return BCrypt.checkpw(
                        password,
                        hashedPassword
                );
            }

        } catch (SQLException e) {
            System.out.println("Lỗi đăng nhập: " + e.getMessage());
            return false;
        }
    }
}