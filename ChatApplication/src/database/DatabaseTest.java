package database;

import java.sql.Connection;

public class DatabaseTest {

    public static void main(String[] args) {

        try (Connection conn = DatabaseConnection.getConnection()) {

            System.out.println("KET NOI SQL SERVER THANH CONG!");
            System.out.println("Database: " + conn.getCatalog());

        } catch (Exception e) {

            System.out.println("KET NOI SQL SERVER THAT BAI!");

            // In chi tiết lỗi
            System.out.println("LOI: " + e.getMessage());

            // In toàn bộ lỗi
            e.printStackTrace();
        }
    }
}